package org.proxydroid;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import android.app.AlertDialog;
import android.app.TabActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TabHost;

public class ProxyDroid extends TabActivity {
	private static final String TAG = "ProxyDroid";

	private static final int PACKAGE_UNINSTALL = 1;
	private static final int SEND_REPORT = 2;

	private Context mContext;
	private String mMaliciousAppPackage = "";
	
	public static boolean runRootCommand(String command) {
		Process process = null;
		DataOutputStream os = null;
		try {
			process = Runtime.getRuntime().exec("su");
			os = new DataOutputStream(process.getOutputStream());
			os.writeBytes(command + "\n");
			os.writeBytes("exit\n");
			os.flush();
			process.waitFor();
		} catch (Exception e) {
			Log.e(TAG, e.getMessage());
			return false;
		} finally {
			try {
				if (os != null) {
					os.close();
				}
				process.destroy();
			} catch (Exception e) {
				// nothing
			}
		}
		return true;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mContext = this;

		setContentView(R.layout.main);

		Resources res = getResources();
		TabHost tabHost = getTabHost();
		TabHost.TabSpec spec;
		Intent intent;

		intent = new Intent().setClass(this, ProxyDroidService.class);
		spec = tabHost
				.newTabSpec("settings")
				.setIndicator(getString(R.string.tab_settings),
						res.getDrawable(android.R.drawable.ic_menu_preferences))
				.setContent(intent);
		tabHost.addTab(spec);

		tabHost.setCurrentTab(0);

		firstRun();

		new CheckForMaliciousApps().execute();
	}

	private void firstRun() {
		int versionCode = 0;
		try {
			versionCode = getPackageManager().getPackageInfo(
					"com.noshufou.android.su", PackageManager.GET_META_DATA).versionCode;
		} catch (NameNotFoundException e) {
			Log.e(TAG,
					"Package not found... Odd, since we're in that package...",
					e);
		}

		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		int lastFirstRun = prefs.getInt("last_run", 0);

		if (lastFirstRun >= versionCode) {
			Log.d(TAG, "Not first run");
			return;
		}
		Log.d(TAG, "First run for version " + versionCode);

		String suVer = getSuVersion(this);
		Log.d(TAG, "su version: " + suVer);
		new Updater(this, suVer).doUpdate();

		SharedPreferences.Editor editor = prefs.edit();
		editor.putInt("last_run", versionCode);
		editor.commit();
	}

	private void maliciousAppFound(final String packageName) {
		new AlertDialog.Builder(mContext)
				.setTitle(R.string.warning)
				.setMessage(
						getString(R.string.malicious_app_found, packageName))
				.setPositiveButton(R.string.uninstall,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								Uri packageUri = Uri.parse("package:"
										+ packageName);
								Intent intent = new Intent(
										Intent.ACTION_DELETE, packageUri);
								startActivityForResult(intent,
										PACKAGE_UNINSTALL);
							}
						})
				.setNegativeButton(android.R.string.cancel,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								new CheckForMaliciousApps().execute();
							}
						}).show();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			final Intent data) {
		switch (requestCode) {
		case PACKAGE_UNINSTALL:
			// We should check to see if the resultCode == 1, but it's always 0
			// perhaps it's a mistake in PackageInstaller.apk
			new AlertDialog.Builder(mContext)
					.setTitle(R.string.uninstall_successful)
					.setMessage(R.string.report_msg)
					.setPositiveButton(R.string.yes,
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									Intent email = new Intent(
											Intent.ACTION_SEND);
									email.setType("plain/text");
									email.putExtra(
											Intent.EXTRA_EMAIL,
											new String[] { "superuser.android@gmail.com" });
									email.putExtra(Intent.EXTRA_SUBJECT,
											getString(R.string.report_subject));
									email.putExtra(
											Intent.EXTRA_TEXT,
											getString(R.string.report_body,
													mMaliciousAppPackage));
									startActivityForResult(email, SEND_REPORT);
								}
							})
					.setNegativeButton(R.string.no,
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									new CheckForMaliciousApps().execute();
								}
							}).show();
			break;
		case SEND_REPORT:
			new CheckForMaliciousApps().execute();
			break;
		}
	}

	public static String getSuVersion(Context context) {
		Process process = null;
		try {
			process = Runtime.getRuntime().exec("su -v");
			InputStream processInputStream = process.getInputStream();
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(
					processInputStream));
			try {
				int counter = 0;
				while (counter < 20) {
					Thread.sleep(50);
					if (stdInput.ready()) {
						String suVersion = stdInput.readLine();
						return suVersion;
					}
					counter++;
				}
				return " " + context.getString(R.string.su_original);
			} finally {
				stdInput.close();
			}
		} catch (IOException e) {
			Log.e(TAG,
					"Call to su failed. Perhaps the wrong version of su is present",
					e);
			return " " + context.getString(R.string.su_original);
		} catch (InterruptedException e) {
			Log.e(TAG, "Call to su failed.", e);
			return " ...";
		}
	}

	public class CheckForMaliciousApps extends
			AsyncTask<String, Integer, String> {

		@Override
		protected String doInBackground(String... params) {
			PackageManager pm = mContext.getPackageManager();
			List<ApplicationInfo> apps = pm.getInstalledApplications(0);
			for (int i = 0; i < apps.size(); i++) {
				ApplicationInfo app = apps.get(i);
				if (!app.packageName.equals(mContext.getPackageName())
						&& pm.checkPermission(
								"com.noshufou.android.su.RESPOND",
								app.packageName) == PackageManager.PERMISSION_GRANTED
						&& !mMaliciousAppPackage.equals(app.packageName)) {
					mMaliciousAppPackage = app.packageName;
					return app.packageName;
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			if (result != null) {
				maliciousAppFound(result);
			}
		}
	}
	
	// 点击Menu时，系统调用当前Activity的onCreateOptionsMenu方法，并传一个实现了一个Menu接口的menu对象供你使用
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		/*
		 * add()方法的四个参数，依次是： 1、组别，如果不分组的话就写Menu.NONE,
		 * 2、Id，这个很重要，Android根据这个Id来确定不同的菜单 3、顺序，那个菜单现在在前面由这个参数的大小决定
		 * 4、文本，菜单的显示文本
		 */
		menu.add(Menu.NONE, Menu.FIRST + 1, 1, getString(R.string.recovery))
				.setIcon(android.R.drawable.ic_menu_delete);
		menu.add(Menu.NONE, Menu.FIRST + 2, 2, getString(R.string.about))
				.setIcon(android.R.drawable.ic_menu_info_details);
		// return true才会起作用
		return true;

	}

	// 菜单项被选择事件
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case Menu.FIRST + 1:
			recovery();
			break;
		case Menu.FIRST + 2:
			showAToast(getString(R.string.copy_rights));
			break;
		}

		return true;
	}

	private void recovery() {
		try {
			stopService(new Intent(this, ProxyDroidService.class));
		} catch (Exception e) {
			// Nothing
		}

		if (ProxyDroidService.isARMv6()) {
			runRootCommand(ProxyDroidService.BASE
					+ "iptables_g1 -t nat -F OUTPUT");
		} else {
			runRootCommand(ProxyDroidService.BASE
					+ "iptables_n1 -t nat -F OUTPUT");
		}

		runRootCommand(ProxyDroidService.BASE + "proxy.sh stop");
	}
}