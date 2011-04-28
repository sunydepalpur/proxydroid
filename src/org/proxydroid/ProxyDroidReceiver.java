package org.proxydroid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

public class ProxyDroidReceiver extends BroadcastReceiver {

	private String host;
	private String proxyType;
	private int port;
	private String user;
	private String password;
	private boolean isAutoConnect = false;
	private boolean isAuth = false;

	@Override
	public void onReceive(Context context, Intent intent) {

		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(context);

		isAutoConnect = settings.getBoolean("isAutoConnect", false);

		if (isAutoConnect) {
			host = settings.getString("host", "");
			proxyType = settings.getString("proxyType", "http");
			user = settings.getString("user", "");
			password = settings.getString("password", "");
			isAuth = settings.getBoolean("isAuth", false);
			String portText = settings.getString("port", "");
			try {
				port = Integer.valueOf(portText);
			} catch (Exception e) {
				port = 3128;
			}

			Intent it = new Intent(context, ProxyDroidService.class);
			Bundle bundle = new Bundle();
			bundle.putString("host", host);
			bundle.putString("proxyType", proxyType);
			bundle.putInt("port", port);
			bundle.putString("user", user);
			bundle.putString("password", password);
			bundle.putBoolean("isAuth", isAuth);

			it.putExtras(bundle);
			context.startService(it);
		}
	}

}
