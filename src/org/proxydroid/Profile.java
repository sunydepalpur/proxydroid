/**
 * 
 */
package org.proxydroid;

import java.io.Serializable;
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
	private String intranetAddr;
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

		isAutoConnect = settings.getBoolean("isAutoConnect", false);
		host = settings.getString("host", "");
		proxyType = settings.getString("proxyType", "http");
		user = settings.getString("user", "");
		password = settings.getString("password", "");
		ssid = settings.getString("ssid", "");
		intranetAddr = settings.getString("intranetAddr", "");
		intranetAddr = validateIntrnet(intranetAddr);
		domain = settings.getString("domain", "");
		isAuth = settings.getBoolean("isAuth", false);
		isNTLM = settings.getBoolean("isNTLM", false);
		isAutoSetProxy = settings.getBoolean("isAutoSetProxy", false);
		isDNSProxy = settings.getBoolean("isDNSProxy", false);

		String portText = settings.getString("port", "");

		if (name.equals("")) {
			name = host + ":" + port + "@" + proxyType;
		}

		try {
			port = Integer.valueOf(portText);
		} catch (Exception e) {
			port = 1984;
		}
	}

	public void setProfile(SharedPreferences settings) {
		Editor ed = settings.edit();
		ed.putString("profileName", name);
		ed.putString("host", host);
		ed.putString("port", Integer.toString(port));
		ed.putString("intranetAddr", intranetAddr);
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
		port = 1984;
		ssid = "";
		user = "";
		domain = "";
		password = "";
		isAuth = false;
		proxyType = "http";
		isAutoConnect = false;
		ssid = "";
		isNTLM = false;
		intranetAddr = "";
		isDNSProxy = false;
	}

	private String validateIntrnet(String ia) {

		boolean valid = Pattern.matches("[0-9]\\.[0-9]\\.[0-9]\\.[0-9]/[0-9]",
				ia);
		if (valid)
			return ia;
		else
			return "";
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
		obj.put("port", port);
		obj.put("proxyType", proxyType);
		obj.put("isAuth", isAuth);
		obj.put("user", user);
		obj.put("password", password);
		obj.put("domain", domain);
		obj.put("isNTLM", isNTLM);
		obj.put("isAutoConnect", isAutoConnect);
		obj.put("intranetAddr", intranetAddr);
		obj.put("isAutoSetProxy", isAutoSetProxy);
		obj.put("isDNSProxy", isDNSProxy);
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

		public boolean getBoolean(String key, boolean def) {
			Object tmp = obj.get(key);
			if (tmp != null)
				return (Boolean) def;
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
		intranetAddr = jd.getString("intranetAddr", "");
		
		port = jd.getInt("port", 1984);
		
		isAuth = jd.getBoolean("isAuth", false);
		isNTLM = jd.getBoolean("isNTLM", false);
		isAutoConnect = jd.getBoolean("isAutoConnect", false);
		isAutoSetProxy = jd.getBoolean("isAutoSetProxy", false);
		isDNSProxy = jd.getBoolean("isDNSProxy", false);

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
	 * @return the intranetAddr
	 */
	public String getIntranetAddr() {
		return intranetAddr;
	}

	/**
	 * @param intranetAddr
	 *            the intranetAddr to set
	 */
	public void setIntranetAddr(String intranetAddr) {
		this.intranetAddr = intranetAddr;
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
