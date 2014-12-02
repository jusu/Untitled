package com.pinktwins.elephant.data;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.pinktwins.elephant.IOUtil;

public class Note {
	private File file, meta;
	private String fileName = "";

	public interface Meta {
		public String title();
		public void title(String newTitle);
	}

	public Note(File f) {
		file = f;
		meta = new File(f.getParentFile().getAbsolutePath() + File.separator + "." + file.getName() + ".meta");

		try {
			if (!meta.exists()) {
				meta.createNewFile();
			}
		} catch (IOException e) {
		}

		readInfo();
	}

	private void readInfo() {
		fileName = file.getName();
	}

	public String name() {
		return fileName;
	}

	private byte[] contents;

	public String contents() {
		try {
			contents = IOUtil.readFile(file);
			return new String(contents, "UTF-8");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}

	public void save(String newText) {
		try {
			IOUtil.writeFile(file, newText);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	final private HashMap<String, String> emptyMap = new HashMap<String, String>();

	public Map<String, String> getMetaMap() {
		try {
			String json = new String(IOUtil.readFile(meta), "UTF-8");
			if (json == null || json.isEmpty()) {
				return emptyMap;
			}
			
			JSONObject o = new JSONObject(json);
			HashMap<String, String> map = new HashMap<String, String>();

			Iterator<?> i = o.keys();
			while (i.hasNext()) {
				String key = (String) i.next();
				String value = o.optString(key);
				map.put(key, value);
			}

			return map;
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return emptyMap;
	}

	private void setMeta(String key, String value) {
		try {
			String json = new String(IOUtil.readFile(meta), "UTF-8");
			if (json == null || json.isEmpty()) {
				json = "{}";
			}

			JSONObject o = new JSONObject(json);
			o.put(key, value);
			IOUtil.writeFile(meta, o.toString());
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Meta getMeta() {
		return new Metadata(getMetaMap());
	}

	private class Metadata implements Meta {

		private Map<String, String> map;

		private Metadata(Map<String, String> map) {
			this.map = map;
		}

		@Override
		public String title() {
			String s = map.get("title");
			if (s == null) {
				s = "Untitled";
			}
			return s;
		}

		@Override
		public void title(String newTitle) {
			setMeta("title", newTitle);
			reload();
		}

		private void reload() {
			map = getMetaMap();
		}
	}
}
