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

import java.util.regex.Pattern;

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
	private String intranetAddr;
	private String user;
	private String password;
	private boolean isAutoConnect = false;
	private boolean isAutoSetProxy = false;
	private boolean isAuth = false;
	private boolean isNTLM = false;
	private boolean isDNSProxy = false;
	private String domain;
	
	private String validateIntrnet(String ia) {

		boolean valid = Pattern.matches("[0-9]\\.[0-9]\\.[0-9]\\.[0-9]/[0-9]",
				ia);
		if (valid)
			return ia;
		else
			return "";
	}

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
			intranetAddr = settings.getString("intranetAddr", "");
			intranetAddr = validateIntrnet(intranetAddr);
			domain = settings.getString("domain", "");
			isAuth = settings.getBoolean("isAuth", false);
			isNTLM = settings.getBoolean("isNTLM", false);
			isDNSProxy = settings.getBoolean("isDNSProxy", false);
			isAutoSetProxy = settings.getBoolean("isAutoSetProxy", false);
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
			bundle.putString("intranetAddr", intranetAddr);
			bundle.putString("user", user);
			bundle.putString("password", password);
			bundle.putString("domain", domain);
			bundle.putBoolean("isAuth", isAuth);
			bundle.putBoolean("isNTLM", isNTLM);
			bundle.putBoolean("isDNSProxy", isDNSProxy);
			bundle.putBoolean("isAutoSetProxy", isAutoSetProxy);

			it.putExtras(bundle);
			context.startService(it);
		}
	}

}
