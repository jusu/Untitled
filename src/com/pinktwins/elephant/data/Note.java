package com.pinktwins.elephant.data;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONException;
import org.json.JSONObject;

import com.pinktwins.elephant.Elephant;
import com.pinktwins.elephant.data.NotebookEvent.Kind;

public class Note {
	private File file, meta;
	private String fileName = "";

	private BasicFileAttributes attr;
	static private DateTimeFormatter df = DateTimeFormat.forPattern("dd MMM yyyy").withLocale(Locale.getDefault());

	public interface Meta {
		public String title();

		public void title(String newTitle);
	}

	@Override
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		}

		if (o instanceof File) {
			return file.equals(o);
		}

		if (o instanceof Note) {
			return file.equals(((Note) o).file());
		}

		return false;
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

	public String createdStr() {
		if (attr == null) {
			try {
				attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
			} catch (IOException e) {
				e.printStackTrace();
				return "";
			}
		}
		return df.print(attr.creationTime().toMillis());
	}

	public String updatedStr() {
		if (attr == null) {
			try {
				attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return df.print(attr.lastModifiedTime().toMillis());
	}

	public void flushAttrs() {
		attr = null;
	}

	private void readInfo() {
		fileName = file.getName();
	}

	public File file() {
		return file;
	}

	public String name() {
		return fileName;
	}

	public long lastModified() {
		long l1 = file.lastModified();
		long l2 = meta.lastModified();
		return l1 > l2 ? l1 : l2;
	}

	private byte[] contents;

	public String contents() {
		try {
			contents = IOUtil.readFile(file);
			return new String(contents, Charset.defaultCharset());
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
			String json = new String(IOUtil.readFile(meta), Charset.defaultCharset());
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
			String json = new String(IOUtil.readFile(meta), Charset.defaultCharset());
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

	public void moveTo(File dest) {
		try {
			FileUtils.moveFileToDirectory(file, dest, false);
			FileUtils.moveFileToDirectory(meta, dest, false);

			Notebook nb = Vault.getInstance().findNotebook(dest);
			if (nb != null) {
				nb.refresh();
				Elephant.eventBus.post(new NotebookEvent(Kind.noteMoved));
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
