/* proxydroid - Global / Individual Proxy App for Android
 * Copyright (C) 2011 Max Lv <max.c.lv@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * 
 *                            ___====-_  _-====___
 *                      _--^^^#####//      \\#####^^^--_
 *                   _-^##########// (    ) \\##########^-_
 *                  -############//  |\^^/|  \\############-
 *                _/############//   (@::@)   \\############\_
 *               /#############((     \\//     ))#############\
 *              -###############\\    (oo)    //###############-
 *             -#################\\  / VV \  //#################-
 *            -###################\\/      \//###################-
 *           _#/|##########/\######(   /\   )######/\##########|\#_
 *           |/ |#/\#/\#/\/  \#/\##\  |  |  /##/\#/  \/\#/\#/\#| \|
 *           `  |/  V  V  `   V  \#\| |  | |/#/  V   '  V  V  \|  '
 *              `   `  `      `   / | |  | | \   '      '  '   '
 *                               (  | |  | |  )
 *                              __\ | |  | | /__
 *                             (vvv(VVV)(VVV)vvv)
 *
 *                              HERE BE DRAGONS
 *
 */

package org.proxydroid;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import com.flurry.android.FlurryAgent;
import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;
import com.ksmaze.android.preference.ListPreferenceMultiSelect;

import android.app.AlertDialog;
import android.app.ProgressDialog;
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
import android.os.Handler;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

public class ProxyDroid extends PreferenceActivity implements
		OnSharedPreferenceChangeListener {

	private static final String TAG = "ProxyDroid";

	private ProgressDialog pd = null;

	private String profile;

	private Profile mProfile = new Profile();

	private CheckBoxPreference isAutoConnectCheck;
	private CheckBoxPreference isAutoSetProxyCheck;
	private CheckBoxPreference isAuthCheck;
	private CheckBoxPreference isNTLMCheck;
	private CheckBoxPreference isDNSProxyCheck;
	private ListPreference profileList;

	private EditTextPreference hostText;
	private EditTextPreference portText;
	private EditTextPreference userText;
	private EditTextPreference passwordText;
	private EditTextPreference domainText;
	private EditTextPreference intranetAddrText;
	private ListPreferenceMultiSelect ssidList;
	private ListPreference proxyTypeList;
	private CheckBoxPreference isRunningCheck;
	private Preference proxyedApps;

	private AdView adView;

	private static final int MSG_UPDATE_FINISHED = 0;

	final Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_UPDATE_FINISHED:
				Toast.makeText(ProxyDroid.this,
						getString(R.string.update_finished), Toast.LENGTH_LONG)
						.show();
				break;
			}
			super.handleMessage(msg);
		}
	};

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

	@Override
	public void onStart() {
		super.onStart();
		FlurryAgent.onStartSession(this, "AV372I7R5YYD52NWPUPE");
	}

	@Override
	public void onStop() {
		super.onStop();
		FlurryAgent.onEndSession(this);
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);
		addPreferencesFromResource(R.xml.proxydroid_preference);
		// Create the adView
		adView = new AdView(this, AdSize.BANNER, "a14db2c016cb9b6");
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
		domainText = (EditTextPreference) findPreference("domain");
		intranetAddrText = (EditTextPreference) findPreference("intranetAddr");
		ssidList = (ListPreferenceMultiSelect) findPreference("ssid");
		proxyTypeList = (ListPreference) findPreference("proxyType");
		proxyedApps = (Preference) findPreference("proxyedApps");
		profileList = (ListPreference) findPreference("profile");

		isRunningCheck = (CheckBoxPreference) findPreference("isRunning");
		isAutoSetProxyCheck = (CheckBoxPreference) findPreference("isAutoSetProxy");
		isAuthCheck = (CheckBoxPreference) findPreference("isAuth");
		isNTLMCheck = (CheckBoxPreference) findPreference("isNTLM");
		isDNSProxyCheck = (CheckBoxPreference) findPreference("isDNSProxy");
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

		if (Utils.isWorked()) {
			edit.putBoolean("isRunning", true);
		} else {
			if (settings.getBoolean("isRunning", false)) {
				// showAToast(getString(R.string.crash_alert));
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

		if (!Utils.isRoot()) {

			isAutoSetProxyCheck.setChecked(false);
			isAutoSetProxyCheck.setEnabled(false);
			proxyedApps.setEnabled(false);
			showAToast(getString(R.string.require_root_alert));
		}

		String versionName;
		try {
			versionName = getPackageManager().getPackageInfo(getPackageName(),
					0).versionName;
		} catch (NameNotFoundException e) {
			versionName = "NONE";
		}

		if (!settings.getBoolean(versionName, false)) {

			new Thread() {
				public void run() {

					String version;
					try {
						version = getPackageManager().getPackageInfo(
								getPackageName(), 0).versionName;
					} catch (NameNotFoundException e) {
						version = "NONE";
					}

					SharedPreferences settings = PreferenceManager
							.getDefaultSharedPreferences(ProxyDroid.this);

					updateProfiles();

					CopyAssets();

					Utils.runCommand("chmod 755 /data/data/org.proxydroid/iptables");
					Utils.runCommand("chmod 755 /data/data/org.proxydroid/redsocks");
					Utils.runCommand("chmod 755 /data/data/org.proxydroid/proxy.sh");
					Utils.runCommand("chmod 755 /data/data/org.proxydroid/cntlm");
					Utils.runCommand("chmod 755 /data/data/org.proxydroid/tproxy");
					Editor edit = settings.edit();
					edit.putBoolean(version, true);
					edit.commit();

					handler.sendEmptyMessage(MSG_UPDATE_FINISHED);
				}
			}.start();

		}

	}

	private void updateProfiles() {
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(this);

		// Store current settings first
		String oldProfile = settings.getString("profile", "1");

		Profile mProfile = new Profile();
		mProfile.getProfile(settings);

		Editor ed = settings.edit();
		ed.putString(oldProfile, mProfile.toString());
		ed.commit();

		boolean mIsAutoConnect = mProfile.isAutoConnect();
		boolean mIsAutoSetProxy = mProfile.isAutoSetProxy();
		boolean mIsAuth = mProfile.isAuth();
		boolean mIsNTLM = mProfile.isNTLM();
		boolean mIsDNSProxy = mProfile.isDNSProxy();

		String mName = mProfile.getName();
		String mHost = mProfile.getHost();
		String mUser = mProfile.getUser();
		String mSsid = mProfile.getSsid();
		String mPassword = mProfile.getPassword();
		String mDomain = mProfile.getDomain();
		String mProxyType = mProfile.getProxyType();
		String mIntranetAddr = mProfile.getIntranetAddr();

		int mPort = mProfile.getPort();

		// Load all profiles
		String[] mProfileValues = settings.getString("profileValues", "")
				.split("\\|");

		// Test on each profile
		for (String p : mProfileValues) {

			if (p.equals("0"))
				continue;

			String profileString = settings.getString(p, "");
			String[] st = profileString.split("\\|");

			mName = getProfileName(p);

			if (st.length < 6)
				return;

			// tricks for old editions
			if (st.length < 11) {
				mHost = st[0];
				try {
					mPort = Integer.valueOf(st[1]);
				} catch (Exception e) {
					mPort = -1;
				}
				mIntranetAddr = "";
				mUser = st[2];
				mPassword = st[3];
				mIsAuth = st[4].equals("true") ? true : false;
				mProxyType = st[5];

				// tricks for old editions
				if (st.length < 8) {
					mIsAutoConnect = false;
					mSsid = "";
				} else {
					mSsid = st[6];
					mIsAutoConnect = st[7].equals("true") ? true : false;

				}

				// tricks for old editions
				if (st.length < 10) {
					mIsNTLM = false;
					mDomain = "";
				} else {
					mDomain = st[8];
					mIsNTLM = st[9].equals("true") ? true : false;
				}

			} else {
				mHost = st[0];
				try {
					mPort = Integer.valueOf(st[1]);
				} catch (Exception e) {
					mPort = -1;
				}
				mIntranetAddr = st[2];
				mUser = st[3];
				mPassword = st[4];
				mIsAuth = st[5].equals("true") ? true : false;
				mProxyType = st[6];
				mSsid = st[7];
				mIsAutoConnect = st[8].equals("true") ? true : false;
				mDomain = st[9];
				mIsNTLM = st[10].equals("true") ? true : false;
			}

			Profile tmpProfile = new Profile();

			tmpProfile.setAuth(mIsAuth);
			tmpProfile.setAutoConnect(mIsAutoConnect);
			tmpProfile.setAutoSetProxy(mIsAutoSetProxy);
			tmpProfile.setDNSProxy(mIsDNSProxy);
			tmpProfile.setNTLM(mIsNTLM);

			tmpProfile.setName(mName);
			tmpProfile.setHost(mHost);
			tmpProfile.setIntranetAddr(mIntranetAddr);
			tmpProfile.setUser(mUser);
			tmpProfile.setPassword(mPassword);
			tmpProfile.setDomain(mDomain);
			tmpProfile.setProxyType(mProxyType);
			tmpProfile.setSsid(mSsid);

			tmpProfile.setPort(mPort);

			ed = settings.edit();
			ed.putString(p, tmpProfile.toString());
			ed.commit();
		}
	}

	/** Called when the activity is closed. */
	@Override
	public void onDestroy() {

		adView.destroy();

		super.onDestroy();
	}

	/** Called when connect button is clicked. */
	public boolean serviceStart() {

		if (Utils.isWorked()) {

			try {
				stopService(new Intent(ProxyDroid.this, ProxyDroidService.class));
			} catch (Exception e) {
				// Nothing
			}

			return false;
		}

		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(this);

		mProfile.getProfile(settings);

		try {

			Intent it = new Intent(ProxyDroid.this, ProxyDroidService.class);
			Bundle bundle = new Bundle();
			bundle.putString("host", mProfile.getHost());
			bundle.putString("user", mProfile.getUser());
			bundle.putString("intranetAddr", mProfile.getIntranetAddr());
			bundle.putString("password", mProfile.getPassword());
			bundle.putString("domain", mProfile.getDomain());

			bundle.putString("proxyType", mProfile.getProxyType());
			bundle.putBoolean("isAutoSetProxy", mProfile.isAutoSetProxy());
			bundle.putBoolean("isAuth", mProfile.isAuth());
			bundle.putBoolean("isNTLM", mProfile.isNTLM());
			bundle.putBoolean("isDNSProxy", mProfile.isDNSProxy());

			bundle.putInt("port", mProfile.getPort());

			it.putExtras(bundle);
			startService(it);

		} catch (Exception ignore) {
			// Nothing
			return false;
		}

		return true;
	}

	private void onProfileChange(String oldProfileName) {

		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(this);

		mProfile.getProfile(settings);
		Editor ed = settings.edit();
		ed.putString(oldProfileName, mProfile.toString());
		ed.commit();

		String profileString = settings.getString(profile, "");

		if (profileString.equals("")) {
			mProfile.init();
			mProfile.setName(getProfileName(profile));
		} else {
			mProfile.decodeJson(profileString);
		}

		hostText.setText(mProfile.getHost());
		intranetAddrText.setText(mProfile.getIntranetAddr());
		userText.setText(mProfile.getUser());
		passwordText.setText(mProfile.getPassword());
		domainText.setText(mProfile.getDomain());
		proxyTypeList.setValue(mProfile.getProxyType());
		ssidList.setValue(mProfile.getSsid());

		isAuthCheck.setChecked(mProfile.isAuth());
		isNTLMCheck.setChecked(mProfile.isNTLM());
		isAutoConnectCheck.setChecked(mProfile.isAutoConnect());
		isAutoSetProxyCheck.setChecked(mProfile.isAutoSetProxy());
		isDNSProxyCheck.setChecked(mProfile.isDNSProxy());

		portText.setText(Integer.toString(mProfile.getPort()));

		Log.d(TAG, mProfile.toString());

		mProfile.setProfile(settings);

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
		domainText.setEnabled(false);
		ssidList.setEnabled(false);
		proxyTypeList.setEnabled(false);
		proxyedApps.setEnabled(false);
		profileList.setEnabled(false);
		intranetAddrText.setEnabled(false);

		isAuthCheck.setEnabled(false);
		isNTLMCheck.setEnabled(false);
		isDNSProxyCheck.setEnabled(false);
		isAutoSetProxyCheck.setEnabled(false);
		isAutoConnectCheck.setEnabled(false);
	}

	private void enableAll() {
		hostText.setEnabled(true);
		portText.setEnabled(true);
		intranetAddrText.setEnabled(true);

		proxyTypeList.setEnabled(true);
		if (isAuthCheck.isChecked()) {
			userText.setEnabled(true);
			passwordText.setEnabled(true);
			isNTLMCheck.setEnabled(true);
			if (isNTLMCheck.isChecked())
				domainText.setEnabled(true);
		}
		if (!isAutoSetProxyCheck.isChecked())
			proxyedApps.setEnabled(true);
		if (isAutoConnectCheck.isChecked())
			ssidList.setEnabled(true);

		isDNSProxyCheck.setEnabled(true);
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

	private String getProfileName(String profile) {
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(this);
		return settings.getString("profile" + profile,
				getString(R.string.profile_base) + " " + profile);
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
			isNTLMCheck.setEnabled(false);
		} else {
			userText.setEnabled(true);
			passwordText.setEnabled(true);
			isNTLMCheck.setEnabled(true);
		}

		if (!settings.getBoolean("isAuth", false)
				|| !settings.getBoolean("isNTLM", false)) {
			domainText.setEnabled(false);
		} else {
			domainText.setEnabled(true);
		}

		Editor edit = settings.edit();

		if (Utils.isWorked()) {
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

		profileList.setSummary(getProfileName(profile));

		if (!settings.getString("ssid", "").equals(""))
			ssidList.setSummary(settings.getString("ssid", ""));
		if (!settings.getString("user", "").equals(""))
			userText.setSummary(settings.getString("user",
					getString(R.string.user_summary)));
		if (!settings.getString("intranetAddr", "").equals(""))
			intranetAddrText.setSummary(settings.getString("intranetAddr",
					getString(R.string.set_intranet_summary)));
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
		if (!settings.getString("domain", "").equals(""))
			domainText.setSummary(settings.getString("domain", ""));

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
				profileEntriesBuffer.append(getProfileName(Integer
						.toString(newProfileValue)) + "|");
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
				profileList.setSummary(getProfileName(profileString));
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
				isNTLMCheck.setEnabled(false);
				domainText.setEnabled(false);
			} else {
				userText.setEnabled(true);
				passwordText.setEnabled(true);
				isNTLMCheck.setEnabled(true);
				if (isNTLMCheck.isChecked())
					domainText.setEnabled(true);
				else
					domainText.setEnabled(false);
			}
		}

		if (key.equals("isNTLM")) {
			if (!settings.getBoolean("isAuth", false)
					|| !settings.getBoolean("isNTLM", false)) {
				domainText.setEnabled(false);
			} else {
				domainText.setEnabled(true);
			}
		}

		if (key.equals("isAutoConnect")) {
			if (settings.getBoolean("isAutoConnect", false)) {
				loadNetworkList();
				ssidList.setEnabled(true);
			} else
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
		else if (key.equals("domain"))
			if (settings.getString("domain", "").equals(""))
				domainText.setSummary(getString(R.string.domain_summary));
			else
				domainText.setSummary(settings.getString("domain", ""));
		else if (key.equals("intranetAddr"))
			if (settings.getString("intranetAddr", "").equals(""))
				intranetAddrText
						.setSummary(getString(R.string.set_intranet_summary));
			else
				intranetAddrText.setSummary(settings.getString("intranetAddr",
						""));
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
		menu.add(Menu.NONE, Menu.FIRST + 1, 2, getString(R.string.recovery))
				.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
		menu.add(Menu.NONE, Menu.FIRST + 2, 3, getString(R.string.profile_del))
				.setIcon(android.R.drawable.ic_menu_delete);
		menu.add(Menu.NONE, Menu.FIRST + 3, 4, getString(R.string.about))
				.setIcon(android.R.drawable.ic_menu_info_details);
		menu.add(Menu.NONE, Menu.FIRST + 4, 5, getString(R.string.change_name))
				.setIcon(android.R.drawable.ic_menu_edit);
		menu.add(Menu.NONE, Menu.FIRST + 5, 1,
				getString(R.string.use_system_iptables)).setIcon(
				android.R.drawable.ic_menu_revert);

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
			delProfile(profile);
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
		case Menu.FIRST + 4:
			rename();
			break;
		case Menu.FIRST + 5:
			// Use system's instead
			File inFile = new File("/system/bin/iptables");
			if (inFile.exists()) {
				try {
					InputStream in = new FileInputStream(inFile);
					OutputStream out = new FileOutputStream(
							"/data/data/org.proxydroid/iptables");
					copyFile(in, out);
					in.close();
					in = null;
					out.flush();
					out.close();
					out = null;
				} catch (Exception e) {
					// Ignore
				}
			}
			break;
		}

		return true;
	}

	private void rename() {
		LayoutInflater factory = LayoutInflater.from(this);
		final View textEntryView = factory.inflate(
				R.layout.alert_dialog_text_entry, null);
		final EditText profileName = (EditText) textEntryView
				.findViewById(R.id.profile_name_edit);
		profileName.setText(getProfileName(profile));

		AlertDialog ad = new AlertDialog.Builder(this)
				.setTitle(R.string.change_name)
				.setView(textEntryView)
				.setPositiveButton(R.string.alert_dialog_ok,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								EditText profileName = (EditText) textEntryView
										.findViewById(R.id.profile_name_edit);
								SharedPreferences settings = PreferenceManager
										.getDefaultSharedPreferences(ProxyDroid.this);
								String name = profileName.getText().toString();
								if (name == null)
									return;
								name = name.replace("|", "");
								if (name.length() <= 0)
									return;
								Editor ed = settings.edit();
								ed.putString("profile" + profile, name);
								ed.commit();

								profileList.setSummary(getProfileName(profile));

								String[] profileEntries = settings.getString(
										"profileEntries", "").split("\\|");
								String[] profileValues = settings.getString(
										"profileValues", "").split("\\|");

								StringBuffer profileEntriesBuffer = new StringBuffer();
								StringBuffer profileValuesBuffer = new StringBuffer();

								for (int i = 0; i < profileValues.length - 1; i++) {
									if (profileValues[i].equals(profile))
										profileEntriesBuffer
												.append(getProfileName(profile)
														+ "|");
									else
										profileEntriesBuffer
												.append(profileEntries[i] + "|");
									profileValuesBuffer.append(profileValues[i]
											+ "|");
								}

								profileEntriesBuffer
										.append(getString(R.string.profile_new));
								profileValuesBuffer.append("0");

								ed = settings.edit();
								ed.putString("profileEntries",
										profileEntriesBuffer.toString());
								ed.putString("profileValues",
										profileValuesBuffer.toString());

								ed.commit();

								loadProfileList();
							}
						})
				.setNegativeButton(R.string.alert_dialog_cancel,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								/* User clicked cancel so do some stuff */
							}
						}).create();
		ad.show();
	}

	private void delProfile(String profile) {
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
		new Thread() {
			public void run() {
				try {
					stopService(new Intent(ProxyDroid.this,
							ProxyDroidService.class));
				} catch (Exception e) {
					// Nothing
				}

				try {
					File cache = new File(ProxyDroidService.BASE
							+ "cache/dnscache");
					if (cache.exists())
						cache.delete();
				} catch (Exception ignore) {
					// Nothing
				}

				Utils.runRootCommand(Utils.getIptables() + " -t nat -F OUTPUT");

				Utils.runRootCommand(ProxyDroidService.BASE + "proxy.sh stop");
				Utils.runRootCommand("kill -9 `cat /data/data/org.proxydroid/tproxy.pid`");

				CopyAssets();
				Utils.runCommand("chmod 755 /data/data/org.proxydroid/iptables");
				Utils.runCommand("chmod 755 /data/data/org.proxydroid/redsocks");
				Utils.runCommand("chmod 755 /data/data/org.proxydroid/proxy.sh");
				Utils.runCommand("chmod 755 /data/data/org.proxydroid/cntlm");
				Utils.runCommand("chmod 755 /data/data/org.proxydroid/tproxy");
			}
		}.start();
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