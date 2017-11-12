package com.pinktwins.elephant.data;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.logging.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.io.Files;
import com.pinktwins.elephant.NoteList.ListModes;
import com.pinktwins.elephant.Sidebar.RecentNotesModes;
import com.pinktwins.elephant.util.IOUtil;

public class Settings {

	private static final Logger LOG = Logger.getLogger(Settings.class.getName());

	public static enum Keys {
		DEFAULT_NOTEBOOK("defaultNotebook"), VAULT_FOLDER("noteFolder"), USE_LUCENE("useLucene"), NOTELIST_MODE("noteListMode"), AUTOBULLET(
				"autoBullet"), RECENT_SHOW("showRecent"), ALLOW_FILENAMECHARS("allowFilenameChars"), CONFIRM_DELETE_FROM_TRASH(
						"confirmDeleteFromTrash"), WINDOW_MAXIMIZED("maximized"), FONT_SCALE("fontScale"), PASTE_PLAINTEXT(
								"pastePlaintext"), SHOW_SIDEBAR("showSidebar"), DEFAULT_FILETYPE("defaultFiletype"), CHARSET("charset");

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

	public boolean getBoolean(Keys key) {
		return map.optBoolean(key.toString(), false);
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

	public void set(Keys key, boolean value) {
		set(key.toString(), value);
	}

	public void set(String key, boolean value) {
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

	public Settings setChain(Keys key, boolean value) {
		return setChain(key.toString(), value);
	}

	public Settings setChain(String key, boolean value) {
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
			Files.write(map.toString(4), settingsFile(), Charset.forName("UTF-8"));
		} catch (IOException e) {
			LOG.severe("Fail: " + e);
		} catch (JSONException e) {
			LOG.severe("Fail: " + e);
		}
	}

	public ListModes getNoteListMode() {
		String mode = getString(Keys.NOTELIST_MODE);

		if (mode.isEmpty()) {
			return ListModes.CARDVIEW;
		}

		if (ListModes.CARDVIEW.toString().equals(mode)) {
			return ListModes.CARDVIEW;
		}

		if (ListModes.SNIPPETVIEW.toString().equals(mode)) {
			return ListModes.SNIPPETVIEW;
		}

		LOG.severe("Unknown listmode: " + mode);
		return ListModes.CARDVIEW;
	}

	public RecentNotesModes getRecentNotesMode() {
		String mode = getString(Keys.RECENT_SHOW);

		if (mode.isEmpty()) {
			return RecentNotesModes.SHOW;
		}

		if (RecentNotesModes.SHOW.toString().equals(mode)) {
			return RecentNotesModes.SHOW;
		}

		if (RecentNotesModes.HIDE.toString().equals(mode)) {
			return RecentNotesModes.HIDE;
		}

		LOG.severe("Unknown recentNotesMode: " + mode);
		return RecentNotesModes.SHOW;
	}

	public boolean getAutoBullet() {
		if (!has(Keys.AUTOBULLET)) {
			return true;
		}
		return getInt(Keys.AUTOBULLET) > 0;
	}

	public String getAllowFilenameChars() {
		if (!has(Keys.ALLOW_FILENAMECHARS)) {
			return "";
		}
		return getString(Keys.ALLOW_FILENAMECHARS);
	}

	public boolean getConfirmDeleteFromTrash() {
		if (!has(Keys.CONFIRM_DELETE_FROM_TRASH)) {
			return true;
		}
		return getBoolean(Keys.CONFIRM_DELETE_FROM_TRASH);
	}

	public boolean getShowSidebar() {
		if (!has(Keys.SHOW_SIDEBAR)) {
			return true;
		}
		return getBoolean(Keys.SHOW_SIDEBAR);
	}

	public String getDefaultFiletype() {
		if (!has(Keys.DEFAULT_FILETYPE)) {
			return "txt";
		}
		return getString(Keys.DEFAULT_FILETYPE);
	}

	public boolean hasCharset() {
		return has(Keys.CHARSET);
	}

	public String getCharset() {
		if (hasCharset()) {
			return getString(Keys.CHARSET);
		} else {
			return null;
		}
	}
}
