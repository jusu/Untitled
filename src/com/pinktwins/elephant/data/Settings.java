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
						"confirmDeleteFromTrash"), WINDOW_MAXIMIZED("maximized"), FONT_SCALE("fontScale"), PASTE_PLAINTEXT("pastePlaintext"), SHOW_SIDEBAR(
								"showSidebar"), DEFAULT_FILETYPE("defaultFiletype"), CHARSET("charset"), INLINE_PREVIEW("inlinePreview"), SORT_BY(
										"sortBy"), SORT_RECENT_FIRST("sortRecentFirst"), MARKDOWN_STYLES("markdownStyles"), FONTS("fonts"), FONT_EDITOR(
												"fontEditor"), FONT_EDITORTITLE("fontEditorTitle"), FONT_CARDNAME("fontCardName"), FONT_SNIPPETNAME(
														"fontSnippetName"), FONT_CARDPREVIEW("fontCardPreview"), FONT_SNIPPETPREVIEW("fontSnippetPreview"), MARKDOWN_FULLPICTUREPATH("markdownFullPicturePath"), WORDWRAP("wordWrap");

		private final String str;

		private Keys(String s) {
			str = s;
		}

		@Override
		public String toString() {
			return str;
		}

		public String title() {
			switch (this) {
			case ALLOW_FILENAMECHARS:
				return "Allow characters in filenames";
			case AUTOBULLET:
				return "Autobullet";
			case CHARSET:
				return "Character set";
			case CONFIRM_DELETE_FROM_TRASH:
				return "Confirm delete from Trash";
			case DEFAULT_FILETYPE:
				return "Default filetype";
			case DEFAULT_NOTEBOOK:
				return "Default notebook";
			case FONT_SCALE:
				return "Font scale";
			case INLINE_PREVIEW:
				return "Inline preview";
			case MARKDOWN_STYLES:
				return "Markdown styles";
			case PASTE_PLAINTEXT:
				return "Paste plain text";
			case FONTS:
				return "Fonts";
			case MARKDOWN_FULLPICTUREPATH:
				return "Markdown: use full path for pictures";
			default:
				return "";
			}
		}

		public String description() {
			switch (this) {
			case ALLOW_FILENAMECHARS:
				return "Additional characters allowed in note's filename. The filename is based on note's title, and by default only a-z, A-Z, 0-9 and . - are allowed, other chars are converted to _";
			case AUTOBULLET:
				return "Whether to automatically create lists when starting a line with * - or +. Default is yes.";
			case CHARSET:
				return "Force a specific character set for notes. Default is to use your system's default character set. Example is \"UTF-8\" to force UTF-8. Note that Elephant doesn't convert your notes' character encodings - if you previously used non-ascii characters and change the character set, your notes might need to be converted to the new encoding to display correctly.";
			case CONFIRM_DELETE_FROM_TRASH:
				return "Confirm when deleting a note from Trash. Default is yes.";
			case DEFAULT_FILETYPE:
				return "Default file type when creating a new note. Default is to create note files with .txt extension, in plain text format. Change to \"md\" to create new notes in markdown format.";
			case DEFAULT_NOTEBOOK:
				return "Name of the default notebook to create notes in. Default is \"Inbox\".";
			case FONT_SCALE:
				return "Make all fonts small/bigger. Does not affect layouts so scaling too much will negatively affect appearance. Yet, scaling slightly can be helpful with high-res screens. Requires restart. Selecting custom fonts will reset this scale to 1.";
			case INLINE_PREVIEW:
				return "Display inline previews for attachments that support it. Currently PDFs are shown inline in note editor. Each attachment can be folded/expanded using the fold/expand button and the folding state is saved per attachment. This default applies when attachment has not been manually folded/expanded.";
			case MARKDOWN_STYLES:
				return "Additional styles for Markdown display.<br/>For example, \"body { font-size: 22px; color: red; }\"<br/>Can also be full path to a stylesheet file.";
			case PASTE_PLAINTEXT:
				return "Text is pasted in plain text. Default is no.";
			case FONTS:
				return "Fonts used in Elephant.";
			case MARKDOWN_FULLPICTUREPATH:
				return "Use full path for markdown pictures, as in: 'noteFilename.attachments/picture.jpg'. This might be required to support external markdown editors. This is a beta feature, is off by default, and filename without path is used.";
			default:
				return "";
			}
		}

		public String fontDefaults() {
			switch (this) {
			case FONT_EDITOR:
				return "Arial-13";
			case FONT_EDITORTITLE:
				return "Helvetica-15";
			case FONT_CARDNAME:
				return "Helvetica-BOLD-16";
			case FONT_CARDPREVIEW:
				return "Arial-12";
			case FONT_SNIPPETNAME:
				return "Helvetica-BOLD-14";
			case FONT_SNIPPETPREVIEW:
				return "Arial-13";
			default:
				return "";
			}
		}

		public static enum Kinds {
			String, Boolean, Float, Fonts, Other
		};

		public Kinds getKind() {
			switch (this) {
			case ALLOW_FILENAMECHARS:
				return Kinds.String;
			case AUTOBULLET:
				return Kinds.Boolean;
			case CHARSET:
				return Kinds.String;
			case CONFIRM_DELETE_FROM_TRASH:
				return Kinds.Boolean;
			case DEFAULT_FILETYPE:
				return Kinds.String;
			case DEFAULT_NOTEBOOK:
				return Kinds.String;
			case FONT_SCALE:
				return Kinds.Float;
			case INLINE_PREVIEW:
				return Kinds.Boolean;
			case MARKDOWN_STYLES:
				return Kinds.String;
			case PASTE_PLAINTEXT:
				return Kinds.Boolean;
			case FONTS:
				return Kinds.Fonts;
			case MARKDOWN_FULLPICTUREPATH:
				return Kinds.Boolean;
			default:
				return Kinds.Other;
			}
		}

	};

	public static Keys[] uiKeys = { Keys.FONTS, Keys.ALLOW_FILENAMECHARS, Keys.AUTOBULLET, Keys.CHARSET, Keys.CONFIRM_DELETE_FROM_TRASH, Keys.DEFAULT_FILETYPE,
			Keys.DEFAULT_NOTEBOOK, Keys.FONT_SCALE, Keys.INLINE_PREVIEW, Keys.MARKDOWN_STYLES, Keys.PASTE_PLAINTEXT, Keys.MARKDOWN_FULLPICTUREPATH };

	public static enum SortBy {
		TITLE, CREATED, UPDATED
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

	public float getFloat(Keys key) {
		return getFloat(key.toString());
	}

	public float getFloat(String keyStr) {
		return (float) map.optDouble(keyStr, 1.0);
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

		try {
			Object o = map.get(Keys.AUTOBULLET.toString());
			if (o instanceof Boolean) {
				return getBoolean(Keys.AUTOBULLET);
			}
			if (o instanceof Integer) {
				return getInt(Keys.AUTOBULLET) > 0;
			}
		} catch (JSONException e) {
		}

		return true;
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

	public boolean getInlinePreview() {
		if (!has(Keys.INLINE_PREVIEW)) {
			return true;
		}
		return getBoolean(Keys.INLINE_PREVIEW);
	}

	public String getDefaultFiletype() {
		if (!has(Keys.DEFAULT_FILETYPE)) {
			return "txt";
		}
		String s = getString(Keys.DEFAULT_FILETYPE);
		if (s.isEmpty()) {
			return "txt";
		}

		return s;
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

	public SortBy getSortBy() {
		if (!has(Keys.SORT_BY)) {
			return SortBy.UPDATED;
		}

		int n = getInt(Keys.SORT_BY);
		SortBy[] v = SortBy.values();
		if (n >= 0 && n < v.length) {
			return v[n];
		}

		return SortBy.UPDATED;
	}

	public boolean getSortRecentFirst() {
		if (!has(Keys.SORT_RECENT_FIRST)) {
			return true;
		}
		return getBoolean(Keys.SORT_RECENT_FIRST);
	}

	public void setSortBy(SortBy s) {
		set(Keys.SORT_BY, s.ordinal());
	}

	public void setSortMostRecent(boolean b) {
		set(Keys.SORT_RECENT_FIRST, b);
	}
	
	public boolean getMarkdownFullPicturePath() {
		if (!has(Keys.MARKDOWN_FULLPICTUREPATH)) {
			return false;
		}
		return getBoolean(Keys.MARKDOWN_FULLPICTUREPATH);
	}
	
	public boolean getWordWrap() {
		if (!has(Keys.WORDWRAP)) {
			return true;
		}
		return getBoolean(Keys.WORDWRAP);
	}
}
