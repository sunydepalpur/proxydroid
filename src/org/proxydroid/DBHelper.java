package org.proxydroid;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.PreferenceManager;
import android.util.Log;

public class DBHelper {
	public static final String TAG = "ProxyDroid.DBHelper";

	private static final String DATABASE_NAME = "proxydroid.data";
	private static final int DATABASE_VERSION = 5;
	private static final String PROXYS_TABLE = "proxys";

	public class Proxys {
		public static final String ENABLED = "enabled";
		public static final String AUTH = "auth";
		public static final String PROXY = "proxy";
		public static final String PORT = "port";
		public static final String USERNAME = "username";
		public static final String PASSWORD = "password";
		public static final String TYPE = "type";
	}

	private Context mContext;
	private SQLiteDatabase mDB;

	public DBHelper(Context context) {
		this.mContext = context;
		DBOpenHelper dbOpenHelper = new DBOpenHelper(context);
		this.mDB = dbOpenHelper.getWritableDatabase();
	}

	public AppDetails checkApp(int uid, int execUid, String execCmd) {
		int allow = AppDetails.ASK;
		long dateAccess = 0;
		Cursor c = this.mDB
				.rawQuery(
						"SELECT apps._id,apps.allow,logs.date FROM apps,logs "
								+ "WHERE (apps.uid=? AND apps.exec_uid=? AND apps.exec_cmd=?) "
								+ "AND (logs.app_id=apps._id AND (logs.type=1 OR logs.type=2)) "
								+ "ORDER BY logs.date LIMIT 1", new String[] {
								Long.toString(uid), Integer.toString(execUid),
								execCmd });
		if (c.moveToFirst()) {
			int id = c.getInt(c.getColumnIndex(Apps.ID));
			allow = c.getInt(c.getColumnIndex(Apps.ALLOW));
			dateAccess = c.getLong(c.getColumnIndex(Logs.DATE));

			addLog(id, 0, (allow == AppDetails.ALLOW) ? LogType.ALLOW
					: LogType.DENY);
		}
		c.close();
		return new AppDetails(uid, allow, dateAccess);
	}

	public void insert(int uid, int toUid, String cmd, int allow) {
		ContentValues values = new ContentValues();
		values.put(Apps.UID, uid);
		values.put(Apps.EXEC_UID, toUid);
		values.put(Apps.EXEC_CMD, cmd);
		values.put(Apps.ALLOW, allow);
		values.put(Apps.PACKAGE, Util.getAppPackage(mContext, uid));
		values.put(Apps.NAME, Util.getAppName(mContext, uid, false));
		long id = 0;
		try {
			id = this.mDB.insertOrThrow(PROXYS_TABLE, null, values);
		} catch (SQLException e) {
			// There was an old, probably stagnant, row in the table
			// Delete it and try again
			deleteByUid(uid);
			id = this.mDB.insert(PROXYS_TABLE, null, values);
		} finally {
			values.clear();

			if (id > 0) {
				addLog(id, System.currentTimeMillis(), LogType.CREATE);
				addLog(id, System.currentTimeMillis(),
						(allow == AppDetails.ALLOW) ? LogType.ALLOW
								: LogType.DENY);
			}
		}
	}

	public Cursor getAllApps() {
		return this.mDB.query(PROXYS_TABLE, new String[] { Apps.ID, Apps.UID,
				Apps.PACKAGE, Apps.NAME, Apps.ALLOW }, null, null, null, null,
				"allow DESC, name ASC");
	}

	public AppDetails getAppDetails(long id) {
		Cursor cursor = this.mDB
				.rawQuery(
						"SELECT apps._id AS _id,apps.uid AS uid,apps.package AS package,"
								+ "apps.name AS name,apps.exec_uid AS exec_uid,apps.exec_cmd AS exec_cmd,apps.allow AS allow,"
								+ "logs.date AS date,logs.type AS type "
								+ "FROM apps,logs "
								+ "WHERE apps._id=? AND logs.app_id=apps._id AND (logs.type=0 OR logs.type=1 OR logs.type=2)"
								+ "ORDER BY logs.date DESC ",
						new String[] { Long.toString(id) });
		AppDetails appDetails = new AppDetails();
		if (cursor.moveToFirst()) {
			appDetails.setUid(cursor.getInt(cursor.getColumnIndex(Apps.UID)));
			appDetails.setPackageName(cursor.getString(cursor
					.getColumnIndex(Apps.PACKAGE)));
			appDetails.setName(cursor.getString(cursor
					.getColumnIndex(Apps.NAME)));
			appDetails
					.setAllow(cursor.getInt(cursor.getColumnIndex(Apps.ALLOW)));
			appDetails.setExecUid(cursor.getInt(cursor
					.getColumnIndex(Apps.EXEC_UID)));
			appDetails.setCommand(cursor.getString(cursor
					.getColumnIndex(Apps.EXEC_CMD)));
			boolean accessFound = false;
			boolean createdFound = false;
			do {
				int logType = cursor.getInt(cursor.getColumnIndex(Logs.TYPE));
				if (logType == LogType.CREATE) {
					appDetails.setDateCreated(cursor.getLong(cursor
							.getColumnIndex(Logs.DATE)));
					createdFound = true;
				} else if (logType == LogType.ALLOW || logType == LogType.DENY) {
					appDetails.setAccessType(logType);
					appDetails.setDateAccess(cursor.getLong(cursor
							.getColumnIndex(Logs.DATE)));
					accessFound = true;
				}
				if (accessFound && createdFound) {
					break;
				}
			} while (cursor.moveToNext());
		}
		cursor.close();
		return appDetails;
	}

	public void changeState(long id) {
		Cursor c = this.mDB.query(PROXYS_TABLE, new String[] { Apps.ALLOW },
				"_id=?", new String[] { Long.toString(id) }, null, null, null);
		if (c.moveToFirst()) {
			int allow = c.getInt(0);
			ContentValues values = new ContentValues();
			values.put(Apps.ALLOW, (allow != 0) ? 0 : 1);
			this.mDB.update(PROXYS_TABLE, values, "_id=?",
					new String[] { Long.toString(id) });
			values.clear();

			addLog(id, 0, LogType.TOGGLE);
		} else {
			Log.d(TAG, "app matching uid " + id + " not found in database");
		}
		c.close();
	}

	public void deleteById(long id) {
		Log.d(TAG, "Deleting from logs table where app_id=" + id);
		this.mDB.delete(LOGS_TABLE, "app_id=?",
				new String[] { Long.toString(id) });
		Log.d(TAG, "Deleting from apps table where _id=" + id);
		this.mDB.delete(PROXYS_TABLE, "_id=?", new String[] { Long.toString(id) });
	}

	public int getDBVersion() {
		return this.mDB.getVersion();
	}

	public void close() {
		if (this.mDB.isOpen()) {
			this.mDB.close();
		}
	}

	private static class DBOpenHelper extends SQLiteOpenHelper {
		private static final String CREATE_PROXYS = "CREATE TABLE IF NOT EXISTS "
				+ PROXYS_TABLE
				+ " (_id INTEGER, uid INTEGER, package TEXT, name TEXT, exec_uid INTEGER, "
				+ "exec_cmd TEXT, allow INTEGER,"
				+ " PRIMARY KEY (_id), UNIQUE (uid,exec_uid,exec_cmd));";

		private Context mContext;

		DBOpenHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
			mContext = context;
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(CREATE_PROXYS);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// TODO Auto-generated method stub
			
		}
	}

}
