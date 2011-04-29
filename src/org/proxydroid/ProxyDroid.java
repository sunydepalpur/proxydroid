package org.proxydroid;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;

public class ProxyDroid extends PreferenceActivity implements
		OnSharedPreferenceChangeListener {

	private static final String TAG = "ProxyDroid";
	public static final String SERVICE_NAME = "org.proxydroid.ProxyDroidService";

	private ProgressDialog pd = null;
	private String host = "";
	private int port = -1;
	private String user = "";
	private String password = "";
	private String ssid = "";
	private String profile;
	public static boolean isAutoConnect = false;
	public static boolean isAutoSetProxy = false;
	public static boolean isRoot = false;
	private boolean isAuth = false;
	private String proxyType = "http";

	private CheckBoxPreference isAutoConnectCheck;
	private CheckBoxPreference isAutoSetProxyCheck;
	private CheckBoxPreference isAuthCheck;
	private ListPreference profileList;

	private EditTextPreference hostText;
	private EditTextPreference portText;
	private EditTextPreference userText;
	private EditTextPreference passwordText;
	private ListPreference ssidList;
	private ListPreference proxyTypeList;
	private CheckBoxPreference isRunningCheck;
	private Preference proxyedApps;

	public static boolean runCommand(String command) {
		Process process = null;
		try {
			process = Runtime.getRuntime().exec(command);
			process.waitFor();
		} catch (Exception e) {
			Log.e(TAG, e.getMessage());
			return false;
		} finally {
			try {
				process.destroy();
			} catch (Exception e) {
				// nothing
			}
		}
		return true;
	}

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

	private void CopyAssets() {
		AssetManager assetManager = getAssets();
		String[] files = null;
		try {
			files = assetManager.list("");
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
		}
		for (int i = 0; i < files.length; i++) {
			InputStream in = null;
			OutputStream out = null;
			try {

				in = assetManager.open(files[i]);
				out = new FileOutputStream("/data/data/org.proxydroid/"
						+ files[i]);
				copyFile(in, out);
				in.close();
				in = null;
				out.flush();
				out.close();
				out = null;

			} catch (Exception e) {
				Log.e(TAG, e.getMessage());
			}
		}
	}

	private void copyFile(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[1024];
		int read;
		while ((read = in.read(buffer)) != -1) {
			out.write(buffer, 0, read);
		}
	}

	private boolean isTextEmpty(String s, String msg) {
		if (s == null || s.length() <= 0) {
			showAToast(msg);
			return true;
		}
		return false;
	}

	public boolean isWorked(String service) {
		ActivityManager myManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		ArrayList<RunningServiceInfo> runningService = (ArrayList<RunningServiceInfo>) myManager
				.getRunningServices(30);
		for (int i = 0; i < runningService.size(); i++) {
			if (runningService.get(i).service.getClassName().toString()
					.equals(service)) {
				return true;
			}
		}
		return false;
	}

	private void loadProfileList() {
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(this);
		String[] profileEntries = settings.getString("profileEntries", "")
				.split("\\|");
		String[] profileValues = settings.getString("profileValues", "").split(
				"\\|");

		profileList.setEntries(profileEntries);
		profileList.setEntryValues(profileValues);
	}

	private void loadNetworkList() {
		WifiManager wm = (WifiManager) this
				.getSystemService(Context.WIFI_SERVICE);
		List<WifiConfiguration> wcs = wm.getConfiguredNetworks();
		String[] ssidEntries = new String[wcs.size() + 1];
		ssidEntries[0] = "2G/3G";
		int n = 1;
		for (WifiConfiguration wc : wcs) {
			if (wc != null && wc.SSID != null)
				ssidEntries[n++] = wc.SSID.replace("\"", "");
			else
				ssidEntries[n++] = "unknown";
		}
		ssidList.setEntries(ssidEntries);
		ssidList.setEntryValues(ssidEntries);
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);
		addPreferencesFromResource(R.xml.proxydroid_preference);
		// Create the adView
		AdView adView = new AdView(this, AdSize.BANNER, "a14db2c016cb9b6");
		// Lookup your LinearLayout assuming it’s been given
		// the attribute android:id="@+id/mainLayout"
		LinearLayout layout = (LinearLayout) findViewById(R.id.ad);
		// Add the adView to it
		layout.addView(adView);
		// Initiate a generic request to load it with an ad
		AdRequest aq = new AdRequest();
		adView.loadAd(aq);

		hostText = (EditTextPreference) findPreference("host");
		portText = (EditTextPreference) findPreference("port");
		userText = (EditTextPreference) findPreference("user");
		passwordText = (EditTextPreference) findPreference("password");
		ssidList = (ListPreference) findPreference("ssid");
		proxyTypeList = (ListPreference) findPreference("proxyType");
		proxyedApps = (Preference) findPreference("proxyedApps");
		profileList = (ListPreference) findPreference("profile");

		isRunningCheck = (CheckBoxPreference) findPreference("isRunning");
		isAutoSetProxyCheck = (CheckBoxPreference) findPreference("isAutoSetProxy");
		isAuthCheck = (CheckBoxPreference) findPreference("isAuth");
		isAutoConnectCheck = (CheckBoxPreference) findPreference("isAutoConnect");

		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(this);

		String profileValuesString = settings.getString("profileValues", "");

		if (profileValuesString.equals("")) {
			Editor ed = settings.edit();
			profile = "1";
			ed.putString("profileValues", "1|0");
			ed.putString("profileEntries", getString(R.string.profile_default)
					+ "|" + getString(R.string.profile_new));
			ed.putString("profile", "1");
			ed.commit();

			profileList.setDefaultValue("1");
		}

		loadProfileList();

		loadNetworkList();

		Editor edit = settings.edit();

		if (this.isWorked(SERVICE_NAME)) {
			edit.putBoolean("isRunning", true);
		} else {
			if (settings.getBoolean("isRunning", false)) {
				showAToast(getString(R.string.crash_alert));
				recovery();
			}
			edit.putBoolean("isRunning", false);
		}

		edit.commit();

		if (settings.getBoolean("isRunning", false)) {
			isRunningCheck.setChecked(true);
			disableAll();
		} else {
			isRunningCheck.setChecked(false);
			enableAll();
		}

		if (!runRootCommand("")) {
			isRoot = false;
		} else {
			isRoot = true;
		}

		if (!isRoot) {

			isAutoSetProxyCheck.setChecked(false);
			isAutoSetProxyCheck.setEnabled(false);
		}

		if (!isWorked(SERVICE_NAME)) {
			CopyAssets();
			runCommand("chmod 777 /data/data/org.proxydroid/iptables_g1");
			runCommand("chmod 777 /data/data/org.proxydroid/iptables_n1");
			runCommand("chmod 777 /data/data/org.proxydroid/redsocks");
			runCommand("chmod 777 /data/data/org.proxydroid/proxy.sh");
		}

	}

	/** Called when the activity is closed. */
	@Override
	public void onDestroy() {

		super.onDestroy();
	}

	/** Called when connect button is clicked. */
	public boolean serviceStart() {

		if (isWorked(SERVICE_NAME)) {

			try {
				stopService(new Intent(ProxyDroid.this, ProxyDroidService.class));
			} catch (Exception e) {
				// Nothing
			}

			return false;
		}

		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(this);

		isAutoConnect = settings.getBoolean("isAutoConnect", false);
		isAutoSetProxy = settings.getBoolean("isAutoSetProxy", false);
		isAuth = settings.getBoolean("isAuth", false);

		host = settings.getString("host", "");
		if (isTextEmpty(host, getString(R.string.host_empty)))
			return false;

		proxyType = settings.getString("proxyType", "http");

		if (isAuth) {
			user = settings.getString("user", "");
			if (isTextEmpty(user, getString(R.string.user_empty)))
				return false;

			password = settings.getString("password", "");
		} else {
			user = "";
			password = "";
		}

		try {
			String portString = settings.getString("port", "");
			if (isTextEmpty(portString, getString(R.string.port_empty)))
				return false;
			port = Integer.valueOf(portString);
		} catch (NumberFormatException e) {
			showAToast(getString(R.string.number_alert));
			Log.e(TAG, "wrong number", e);
			return false;
		}

		try {

			Intent it = new Intent(ProxyDroid.this, ProxyDroidService.class);
			Bundle bundle = new Bundle();
			bundle.putString("host", host);
			bundle.putString("user", user);
			bundle.putString("password", password);
			bundle.putInt("port", port);
			bundle.putString("proxyType", proxyType);
			bundle.putBoolean("isAutoSetProxy", isAutoSetProxy);
			bundle.putBoolean("isAuth", isAuth);

			it.putExtras(bundle);
			startService(it);

		} catch (Exception ignore) {
			// Nothing
			return false;
		}

		return true;
	}

	private void onProfileChange(String oldProfile) {

		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(ProxyDroid.this);

		isAutoConnect = settings.getBoolean("isAutoConnect", false);
		isAutoSetProxy = settings.getBoolean("isAutoSetProxy", false);
		isAuth = settings.getBoolean("isAuth", false);

		host = settings.getString("host", "");

		user = settings.getString("user", "");

		ssid = settings.getString("ssid", "");

		password = settings.getString("password", "");

		String portString = settings.getString("port", "");
		try {
			port = Integer.valueOf(portString);
		} catch (NumberFormatException e) {
			port = -1;
		}

		String oldProfileSettings = host + "|" + (port != -1 ? port : "") + "|"
				+ user + "|" + password + "|" + (isAuth ? "true" : "false")
				+ "|" + proxyType + "|" + ssid + "|"
				+ (isAutoConnect ? "true" : "false");

		Editor ed = settings.edit();
		ed.putString(oldProfile, oldProfileSettings);
		ed.commit();

		String profileString = settings.getString(profile, "");

		if (profileString.equals("")) {

			host = "";
			port = -1;
			user = "";
			password = "";
			isAuth = false;
			proxyType = "http";
			isAutoConnect = false;
			ssid = "";

		} else {

			String[] st = profileString.split("\\|");
			Log.d(TAG, "Token size: " + st.length);

			host = st[0];
			try {
				port = Integer.valueOf(st[1]);
			} catch (Exception e) {
				port = -1;
			}
			user = st[2];
			password = st[3];
			isAuth = st[4].equals("true") ? true : false;
			proxyType = st[5];
			if (st.length < 7) {
				isAutoConnect = false;
				ssid = "";
			} else {
				ssid = st[6];
				isAutoConnect = st[7].equals("true") ? true : false;

			}

		}

		Log.d(TAG, host + "|" + port + "|" + user + "|" + password + "|"
				+ (isAuth ? "true" : "false") + "|" + proxyType + "|" + ssid
				+ "|" + (isAutoConnect ? "true" : "false"));

		hostText.setText(host);
		portText.setText(port != -1 ? Integer.toString(port) : "");
		userText.setText(user);
		passwordText.setText(password);
		isAuthCheck.setChecked(isAuth);
		proxyTypeList.setValue(proxyType);
		isAutoConnectCheck.setChecked(isAutoConnect);
		ssidList.setValue(ssid);

		ed = settings.edit();
		ed.putString("host", host.equals("null") ? "" : host);
		ed.putString("port", port != -1 ? Integer.toString(port) : "");
		ed.putString("user", user.equals("null") ? "" : user);
		ed.putString("password", password.equals("null") ? "" : password);
		ed.putBoolean("isSocks", isAuth);
		ed.putString("proxyType", proxyType);
		ed.putBoolean("isAutoConnect", isAutoConnect);
		ed.putString("ssid", ssid);
		ed.commit();

	}

	private void showAToast(String msg) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(msg)
				.setCancelable(false)
				.setNegativeButton(getString(R.string.ok_iknow),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						});
		AlertDialog alert = builder.create();
		alert.show();
	}

	private void disableAll() {
		hostText.setEnabled(false);
		portText.setEnabled(false);
		userText.setEnabled(false);
		passwordText.setEnabled(false);
		ssidList.setEnabled(false);
		proxyTypeList.setEnabled(false);
		proxyedApps.setEnabled(false);
		profileList.setEnabled(false);

		isAuthCheck.setEnabled(false);
		isAutoSetProxyCheck.setEnabled(false);
		isAutoConnectCheck.setEnabled(false);
	}

	private void enableAll() {
		hostText.setEnabled(true);
		portText.setEnabled(true);

		proxyTypeList.setEnabled(true);
		if (isAuthCheck.isChecked()) {
			userText.setEnabled(true);
			passwordText.setEnabled(true);
		}
		if (!isAutoSetProxyCheck.isChecked())
			proxyedApps.setEnabled(true);
		if (isAutoConnectCheck.isChecked())
			ssidList.setEnabled(true);

		profileList.setEnabled(true);
		isAutoSetProxyCheck.setEnabled(true);
		isAuthCheck.setEnabled(true);
		isAutoConnectCheck.setEnabled(true);
	}

	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
			Preference preference) {

		if (preference.getKey() != null
				&& preference.getKey().equals("proxyedApps")) {
			Intent intent = new Intent(this, AppManager.class);
			startActivity(intent);
		} else if (preference.getKey() != null
				&& preference.getKey().equals("isRunning")) {

			if (!serviceStart()) {

				SharedPreferences settings = PreferenceManager
						.getDefaultSharedPreferences(ProxyDroid.this);

				Editor edit = settings.edit();

				edit.putBoolean("isRunning", false);

				edit.commit();

				enableAll();
			}

		}
		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}

	@Override
	protected void onResume() {
		super.onResume();
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(this);

		if (settings.getBoolean("isAutoSetProxy", false))
			proxyedApps.setEnabled(false);
		else
			proxyedApps.setEnabled(true);

		if (settings.getBoolean("isAutoConnect", false))
			ssidList.setEnabled(true);
		else
			ssidList.setEnabled(false);

		if (!settings.getBoolean("isAuth", false)) {
			userText.setEnabled(false);
			passwordText.setEnabled(false);
		} else {
			userText.setEnabled(true);
			passwordText.setEnabled(true);
		}

		Editor edit = settings.edit();

		if (this.isWorked(SERVICE_NAME)) {
			if (settings.getBoolean("isConnecting", false))
				isRunningCheck.setEnabled(false);
			edit.putBoolean("isRunning", true);
		} else {
			if (settings.getBoolean("isRunning", false)) {
				showAToast(getString(R.string.crash_alert));
				recovery();
			}
			edit.putBoolean("isRunning", false);
		}

		edit.commit();

		if (settings.getBoolean("isRunning", false)) {
			isRunningCheck.setChecked(true);
			disableAll();
		} else {
			isRunningCheck.setChecked(false);
			enableAll();
		}

		// Setup the initial values
		profile = settings.getString("profile", "1");
		profileList.setValue(profile);

		profileList.setSummary(getString(R.string.profile_base) + " "
				+ settings.getString("profile", ""));

		if (!settings.getString("ssid", "").equals(""))
			ssidList.setSummary(settings.getString("ssid", ""));
		if (!settings.getString("user", "").equals(""))
			userText.setSummary(settings.getString("user",
					getString(R.string.user_summary)));
		if (!settings.getString("port", "-1").equals("-1")
				&& !settings.getString("port", "-1").equals(""))
			portText.setSummary(settings.getString("port",
					getString(R.string.port_summary)));
		if (!settings.getString("host", "").equals(""))
			hostText.setSummary(settings.getString("host",
					getString(R.string.host_summary)));
		if (!settings.getString("password", "").equals(""))
			passwordText.setSummary("*********");
		if (!settings.getString("proxyType", "").equals(""))
			proxyTypeList.setSummary(settings.getString("proxyType", "")
					.toUpperCase());

		// Set up a listener whenever a key changes
		getPreferenceScreen().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onPause() {
		super.onPause();

		// Unregister the listener whenever a key changes
		getPreferenceScreen().getSharedPreferences()
				.unregisterOnSharedPreferenceChangeListener(this);
	}

	public void onSharedPreferenceChanged(SharedPreferences settings, String key) {
		// Let's do something a preference value changes

		if (key.equals("profile")) {
			String profileString = settings.getString("profile", "");
			if (profileString.equals("0")) {
				String[] profileEntries = settings.getString("profileEntries",
						"").split("\\|");
				String[] profileValues = settings
						.getString("profileValues", "").split("\\|");
				int newProfileValue = Integer
						.valueOf(profileValues[profileValues.length - 2]) + 1;

				StringBuffer profileEntriesBuffer = new StringBuffer();
				StringBuffer profileValuesBuffer = new StringBuffer();

				for (int i = 0; i < profileValues.length - 1; i++) {
					profileEntriesBuffer.append(profileEntries[i] + "|");
					profileValuesBuffer.append(profileValues[i] + "|");
				}
				profileEntriesBuffer.append(getString(R.string.profile_base)
						+ " " + newProfileValue + "|");
				profileValuesBuffer.append(newProfileValue + "|");
				profileEntriesBuffer.append(getString(R.string.profile_new));
				profileValuesBuffer.append("0");

				Editor ed = settings.edit();
				ed.putString("profileEntries", profileEntriesBuffer.toString());
				ed.putString("profileValues", profileValuesBuffer.toString());
				ed.putString("profile", Integer.toString(newProfileValue));
				ed.commit();

				loadProfileList();

			} else {
				String oldProfile = profile;
				profile = profileString;
				profileList.setValue(profile);
				onProfileChange(oldProfile);
				profileList.setSummary(getString(R.string.profile_base) + " "
						+ profileString);
			}
		}

		if (key.equals("isConnecting")) {
			if (settings.getBoolean("isConnecting", false)) {
				Log.d(TAG, "Connecting start");
				isRunningCheck.setEnabled(false);
				pd = ProgressDialog.show(this, "",
						getString(R.string.connecting), true, true);
			} else {
				Log.d(TAG, "Connecting finish");
				if (pd != null) {
					pd.dismiss();
					pd = null;
				}
				isRunningCheck.setEnabled(true);
			}
		}

		if (key.equals("isAuth")) {
			if (!settings.getBoolean("isAuth", false)) {
				userText.setEnabled(false);
				passwordText.setEnabled(false);
			} else {
				userText.setEnabled(true);
				passwordText.setEnabled(true);
			}
		}

		if (key.equals("isAutoConnect")) {
			if (settings.getBoolean("isAutoConnect", false))
				ssidList.setEnabled(true);
			else
				ssidList.setEnabled(false);
		}

		if (key.equals("isAutoSetProxy")) {
			if (settings.getBoolean("isAutoSetProxy", false))
				proxyedApps.setEnabled(false);
			else
				proxyedApps.setEnabled(true);
		}

		if (key.equals("isRunning")) {
			if (settings.getBoolean("isRunning", false)) {
				disableAll();
				isRunningCheck.setChecked(true);
			} else {
				enableAll();
				isRunningCheck.setChecked(false);
			}
		}

		if (key.equals("ssid"))
			if (settings.getString("ssid", "").equals(""))
				ssidList.setSummary(getString(R.string.ssid_summary));
			else
				ssidList.setSummary(settings.getString("ssid", ""));
		else if (key.equals("user"))
			if (settings.getString("user", "").equals(""))
				userText.setSummary(getString(R.string.user_summary));
			else
				userText.setSummary(settings.getString("user", ""));
		else if (key.equals("port"))
			if (settings.getString("port", "-1").equals("-1")
					|| settings.getString("port", "-1").equals(""))
				portText.setSummary(getString(R.string.port_summary));
			else
				portText.setSummary(settings.getString("port", ""));
		else if (key.equals("host"))
			if (settings.getString("host", "").equals(""))
				hostText.setSummary(getString(R.string.host_summary));
			else
				hostText.setSummary(settings.getString("host", ""));
		else if (key.equals("proxyType"))
			if (settings.getString("proxyType", "").equals(""))
				proxyTypeList
						.setSummary(getString(R.string.proxy_type_summary));
			else
				proxyTypeList.setSummary(settings.getString("proxyType", "")
						.toUpperCase());
		else if (key.equals("password"))
			if (!settings.getString("password", "").equals(""))
				passwordText.setSummary("*********");
			else
				passwordText.setSummary(getString(R.string.password_summary));
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
				.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
		menu.add(Menu.NONE, Menu.FIRST + 2, 2, getString(R.string.profile_del))
				.setIcon(android.R.drawable.ic_menu_delete);
		menu.add(Menu.NONE, Menu.FIRST + 3, 3, getString(R.string.about))
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
			delProfile();
			break;
		case Menu.FIRST + 3:
			String versionName = "";
			try {
				versionName = getPackageManager().getPackageInfo(
						getPackageName(), 0).versionName;
			} catch (NameNotFoundException e) {
				versionName = "";
			}
			showAToast(getString(R.string.about) + " (" + versionName + ")"
					+ getString(R.string.copy_rights));
			break;
		}

		return true;
	}

	private void delProfile() {
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(this);
		String[] profileEntries = settings.getString("profileEntries", "")
				.split("\\|");
		String[] profileValues = settings.getString("profileValues", "").split(
				"\\|");

		Log.d(TAG, "Profile :" + profile);
		if (profileEntries.length > 2) {
			StringBuffer profileEntriesBuffer = new StringBuffer();
			StringBuffer profileValuesBuffer = new StringBuffer();

			String newProfileValue = "1";

			for (int i = 0; i < profileValues.length - 1; i++) {
				if (!profile.equals(profileValues[i])) {
					profileEntriesBuffer.append(profileEntries[i] + "|");
					profileValuesBuffer.append(profileValues[i] + "|");
					newProfileValue = profileValues[i];
				}
			}
			profileEntriesBuffer.append(getString(R.string.profile_new));
			profileValuesBuffer.append("0");

			Editor ed = settings.edit();
			ed.putString("profileEntries", profileEntriesBuffer.toString());
			ed.putString("profileValues", profileValuesBuffer.toString());
			ed.putString("profile", newProfileValue);
			ed.commit();

			loadProfileList();
		}
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

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) { // 按下的如果是BACK，同时没有重复
			try {
				finish();
			} catch (Exception ignore) {
				// Nothing
			}
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

}