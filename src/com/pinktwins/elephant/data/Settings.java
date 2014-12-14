package com.pinktwins.elephant.data;

import java.io.File;
import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

public class Settings {
	private String homeDir;
	private JSONObject map;

	private File settingsFile() {
		return new File(homeDir + File.separator + ".com.pinktwins.elephant.settings");
	}

	public Settings() {
		homeDir = System.getProperty("user.home");
		map = load();
	}

	private JSONObject load() {
		return IOUtil.loadJson(settingsFile());
	}

	public int getInt(String key) {
		return map.optInt(key);
	}

	public String getString(String key) {
		return map.optString(key, "");
	}

	public void set(String key, int value) {
		try {
			map.put(key, value);
			save();
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public void set(String key, String value) {
		try {
			map.put(key, value);
			save();
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public Settings setChain(String key, int value) {
		try {
			map.put(key, value);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return this;
	}

	public Settings setChain(String key, String value) {
		try {
			map.put(key, value);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return this;
	}

	private void save() {
		try {
			IOUtil.writeFile(settingsFile(), map.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
