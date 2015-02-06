package com.pinktwins.elephant.data;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import com.pinktwins.elephant.util.IOUtil;

public class Settings {

	private static final Logger LOG = Logger.getLogger(Settings.class.getName());

	public static enum Keys {
		DEFAULT_NOTEBOOK("defaultNotebook"), VAULT_FOLDER("noteFolder"), USE_LUCENE("useLucene");

		private final String str;

		private Keys(String s) {
			str = s;
		}

		@Override
		public String toString() {
			return str;
		}
	};

	private String homeDir;
	private JSONObject map;

	public File settingsFile() {
		return new File(homeDir + File.separator + ".com.pinktwins.elephant.settings");
	}

	public Settings() {
		homeDir = System.getProperty("user.home");
		map = load();
	}

	public String userHomePath() {
		return homeDir;
	}

	private JSONObject load() {
		return IOUtil.loadJson(settingsFile());
	}

	public boolean has(Keys key) {
		return map.has(key.toString());
	}

	public int getInt(Keys key) {
		return getInt(key.toString());
	}

	public int getInt(String keyStr) {
		return map.optInt(keyStr);
	}

	public String getString(Keys key) {
		return map.optString(key.toString(), "");
	}

	public void set(Keys key, int value) {
		set(key.toString(), value);
	}

	public void set(String key, int value) {
		try {
			map.put(key, value);
			save();
		} catch (JSONException e) {
			LOG.severe("Fail: " + e);
		}
	}

	public void set(Keys key, String value) {
		set(key.toString(), value);
	}

	public void set(String key, String value) {
		try {
			map.put(key, value);
			save();
		} catch (JSONException e) {
			LOG.severe("Fail: " + e);
		}
	}

	public Settings setChain(Keys key, int value) {
		return setChain(key.toString(), value);
	}

	public Settings setChain(String key, int value) {
		try {
			map.put(key.toString(), value);
		} catch (JSONException e) {
			LOG.severe("Fail: " + e);
		}
		return this;
	}

	public Settings setChain(Keys key, String value) {
		return setChain(key.toString(), value);
	}

	public Settings setChain(String key, String value) {
		try {
			map.put(key.toString(), value);
		} catch (JSONException e) {
			LOG.severe("Fail: " + e);
		}
		return this;
	}

	private void save() {
		try {
			IOUtil.writeFile(settingsFile(), map.toString(4));
		} catch (IOException e) {
			LOG.severe("Fail: " + e);
		} catch (JSONException e) {
			LOG.severe("Fail: " + e);
		}
	}
}
