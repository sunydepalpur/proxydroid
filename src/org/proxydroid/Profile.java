/* proxydroid - Global / Individual Proxy App for Android
 * Copyright (C) 2011 K's Maze <kafkasmaze@gmail.com>
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
 */
package org.proxydroid;

import java.io.Serializable;
import java.util.Vector;
import java.util.regex.Pattern;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

/**
 * @author KsMaze
 * 
 */
public class Profile implements Serializable {

	private String name;
	private String host;
	private String proxyType;
	private int port;
	private String bypassAddrs;
	private String user;
	private String password;
	private boolean isAutoConnect = false;
	private boolean isAutoSetProxy = false;
	private boolean isAuth = false;
	private boolean isNTLM = false;
	private boolean isDNSProxy = false;

	private String domain;
	private String ssid;

	public Profile() {
		init();
	}

	public void getProfile(SharedPreferences settings) {
		name = settings.getString("profileName", "");

		host = settings.getString("host", "");
		proxyType = settings.getString("proxyType", "http");
		user = settings.getString("user", "");
		password = settings.getString("password", "");
		ssid = settings.getString("ssid", "");
		bypassAddrs = settings.getString("bypassAddrs", "");
		domain = settings.getString("domain", "");

		isAuth = settings.getBoolean("isAuth", false);
		isNTLM = settings.getBoolean("isNTLM", false);
		isAutoSetProxy = settings.getBoolean("isAutoSetProxy", false);
		isDNSProxy = settings.getBoolean("isDNSProxy", false);
		isAutoConnect = settings.getBoolean("isAutoConnect", false);

		String portText = settings.getString("port", "");

		if (name.equals("")) {
			name = host + ":" + port + "@" + proxyType;
		}

		try {
			port = Integer.valueOf(portText);
		} catch (Exception e) {
			port = 3128;
		}
	}

	public void setProfile(SharedPreferences settings) {
		Editor ed = settings.edit();
		ed.putString("profileName", name);
		ed.putString("host", host);
		ed.putString("port", Integer.toString(port));
		ed.putString("bypassAddrs", bypassAddrs);
		ed.putString("user", user);
		ed.putString("password", password);
		ed.putBoolean("isAuth", isAuth);
		ed.putBoolean("isNTLM", isNTLM);
		ed.putString("domain", domain);
		ed.putString("proxyType", proxyType);
		ed.putBoolean("isAutoConnect", isAutoConnect);
		ed.putBoolean("isAutoSetProxyCheck", isAutoSetProxy);
		ed.putBoolean("isDNSProxy", isDNSProxy);
		ed.putString("ssid", ssid);
		ed.commit();
	}

	public void init() {
		host = "";
		port = 3128;
		ssid = "";
		user = "";
		domain = "";
		password = "";
		isAuth = false;
		proxyType = "http";
		isAutoConnect = false;
		ssid = "";
		isNTLM = false;
		bypassAddrs = "";
		isDNSProxy = false;
	}

	@Override
	public String toString() {
		return this.encodeJson().toJSONString();
	}

	@SuppressWarnings("unchecked")
	public JSONObject encodeJson() {
		JSONObject obj = new JSONObject();
		obj.put("name", name);
		obj.put("ssid", ssid);
		obj.put("host", host);
		obj.put("proxyType", proxyType);
		obj.put("user", user);
		obj.put("password", password);
		obj.put("domain", domain);
		obj.put("bypassAddrs", bypassAddrs);

		obj.put("isAuth", isAuth);
		obj.put("isNTLM", isNTLM);
		obj.put("isAutoConnect", isAutoConnect);
		obj.put("isAutoSetProxy", isAutoSetProxy);
		obj.put("isDNSProxy", isDNSProxy);

		obj.put("port", port);
		return obj;
	}

	class JSONDecoder {
		private JSONObject obj;

		public JSONDecoder(String values) throws ParseException {
			JSONParser parser = new JSONParser();
			obj = (JSONObject) parser.parse(values);
		}

		public String getString(String key, String def) {
			Object tmp = obj.get(key);
			if (tmp != null)
				return (String) tmp;
			else
				return def;
		}

		public int getInt(String key, int def) {
			Object tmp = obj.get(key);
			if (tmp != null) {
				try {
					return Integer.valueOf(tmp.toString());
				} catch (NumberFormatException e) {
					return def;
				}
			} else {
				return def;
			}
		}

		public Boolean getBoolean(String key, Boolean def) {
			Object tmp = obj.get(key);
			if (tmp != null)
				return (Boolean) tmp;
			else
				return def;
		}
	}

	public void decodeJson(String values) {
		JSONDecoder jd;

		try {
			jd = new JSONDecoder(values);
		} catch (ParseException e) {
			return;
		}

		name = jd.getString("name", "");
		ssid = jd.getString("ssid", "");
		host = jd.getString("host", "");
		proxyType = jd.getString("proxyType", "http");
		user = jd.getString("user", "");
		password = jd.getString("password", "");
		domain = jd.getString("domain", "");
		bypassAddrs = jd.getString("bypassAddrs", "");

		port = jd.getInt("port", 3128);

		isAuth = jd.getBoolean("isAuth", false);
		isNTLM = jd.getBoolean("isNTLM", false);
		isAutoConnect = jd.getBoolean("isAutoConnect", false);
		isAutoSetProxy = jd.getBoolean("isAutoSetProxy", false);
		isDNSProxy = jd.getBoolean("isDNSProxy", false);

	}
	
	public static boolean validateAddr(String ia) {

		boolean valid1 = Pattern.matches(
				"[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}/[0-9]{1,2}",
				ia);
		boolean valid2 = Pattern.matches(
				"[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}", ia);
		if (valid1 || valid2)
			return true;
		else
			return false;
	}

	public static String[] decodeAddrs(String addrs) {
		String[] list = addrs.split("\\|");
		Vector<String> ret = new Vector<String>();
		for (String addr : list)
			if (validateAddr(addr))
				ret.add(addr);
		return ret.toArray(new String[ret.size()]);
	}

	public static String encodeAddrs(String[] addrs) {
		
		if (addrs.length == 0)
			return "";
		
		StringBuffer sb = new StringBuffer();
		for (String addr : addrs)
			if (validateAddr(addr))
				sb.append(addr + "|");
		String ret = sb.substring(0, sb.length() - 1);
		return ret;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 *            the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the ssid
	 */
	public String getSsid() {
		return ssid;
	}

	/**
	 * @param ssid
	 *            the ssid to set
	 */
	public void setSsid(String ssid) {
		this.ssid = ssid;
	}

	/**
	 * @return the host
	 */
	public String getHost() {
		return host;
	}

	/**
	 * @param host
	 *            the host to set
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * @return the proxyType
	 */
	public String getProxyType() {
		return proxyType;
	}

	/**
	 * @param proxyType
	 *            the proxyType to set
	 */
	public void setProxyType(String proxyType) {
		this.proxyType = proxyType;
	}

	/**
	 * @return the port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * @param port
	 *            the port to set
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * @return the bypassAddrs
	 */
	public String getBypassAddrs() {
		return bypassAddrs;
	}

	/**
	 * @param bypassAddrs
	 *            the bypassAddrs to set
	 */
	public void setBypassAddrs(String bypassAddrs) {
		this.bypassAddrs = bypassAddrs;
	}

	/**
	 * @return the user
	 */
	public String getUser() {
		return user;
	}

	/**
	 * @param user
	 *            the user to set
	 */
	public void setUser(String user) {
		this.user = user;
	}

	/**
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @param password
	 *            the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * @return the isAutoConnect
	 */
	public Boolean isAutoConnect() {
		return isAutoConnect;
	}

	/**
	 * @param isAutoConnect
	 *            the isAutoConnect to set
	 */
	public void setAutoConnect(Boolean isAutoConnect) {
		this.isAutoConnect = isAutoConnect;
	}

	/**
	 * @return the isAutoSetProxy
	 */
	public Boolean isAutoSetProxy() {
		return isAutoSetProxy;
	}

	/**
	 * @param isAutoSetProxy
	 *            the isAutoSetProxy to set
	 */
	public void setAutoSetProxy(Boolean isAutoSetProxy) {
		this.isAutoSetProxy = isAutoSetProxy;
	}

	/**
	 * @return the isAuth
	 */
	public Boolean isAuth() {
		return isAuth;
	}

	/**
	 * @param isAuth
	 *            the isAuth to set
	 */
	public void setAuth(Boolean isAuth) {
		this.isAuth = isAuth;
	}

	/**
	 * @return the isNTLM
	 */
	public Boolean isNTLM() {
		return isNTLM;
	}

	/**
	 * @param isNTLM
	 *            the isNTLM to set
	 */
	public void setNTLM(Boolean isNTLM) {
		this.isNTLM = isNTLM;
	}

	/**
	 * @return the domain
	 */
	public String getDomain() {
		return domain;
	}

	/**
	 * @param domain
	 *            the domain to set
	 */
	public void setDomain(String domain) {
		this.domain = domain;
	}

	/**
	 * @return the isDNSProxy
	 */
	public boolean isDNSProxy() {
		return isDNSProxy;
	}

	/**
	 * @param isDNSProxy
	 *            the isDNSProxy to set
	 */
	public void setDNSProxy(boolean isDNSProxy) {
		this.isDNSProxy = isDNSProxy;
	}

}
