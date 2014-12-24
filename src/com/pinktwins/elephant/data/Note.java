package com.pinktwins.elephant.data;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONException;
import org.json.JSONObject;

import com.pinktwins.elephant.Elephant;
import com.pinktwins.elephant.data.NotebookEvent.Kind;

public class Note implements Comparable<Note> {
	private File file, meta;
	private String fileName = "";

	private boolean saveLocked = false;

	static private DateTimeFormatter df = DateTimeFormat.forPattern("dd MMM yyyy").withLocale(Locale.getDefault());

	static private File[] emptyFileList = new File[0];

	public interface Meta {
		public String title();

		public long created();

		public void title(String newTitle);

		public void setCreatedTime();

		public int getAttachmentPosition(File attachment);

		public void setAttachmentPosition(File attachment, int position);
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

	@Override
	public int hashCode() {
		return file.getAbsoluteFile().hashCode();
	}

	@Override
	public int compareTo(Note n) {
		long m1 = file.lastModified(), m2 = n.file().lastModified();

		if (m1 > m2) {
			return -1;
		}

		if (m1 < m2) {
			return 1;
		}

		return 0;
	}

	private File metaFromFile(File f) {
		String flatPath = f.getAbsolutePath().replace(Vault.getInstance().getHome().getAbsolutePath() + File.separator, "");
		flatPath = flatPath.replaceAll(Matcher.quoteReplacement(File.separator), "_");
		File m = new File(Vault.getInstance().getHome().getAbsolutePath() + File.separator + ".meta" + File.separator + flatPath);
		m.getParentFile().mkdirs();
		return m;

		// return new File(f.getParentFile().getAbsolutePath() + File.separator
		// + "." + f.getName() + ".elephant");
	}

	public Note(File f) {
		file = f;
		meta = metaFromFile(f);

		String ext = FilenameUtils.getExtension(f.getName());
		if (!"txt".equals(ext) && !"rtf".equals(ext)) {
			saveLocked = true;
		}

		readInfo();
	}

	public String createdStr() {
		return df.print(getMeta().created());
	}

	public String updatedStr() {
		return df.print(lastModified());
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
		return file.lastModified();
	}

	private byte[] contents;

	public String contents() {
		if (saveLocked) {
			return "(binary)";
		}

		contents = IOUtil.readFile(file);
		return new String(contents, Charset.defaultCharset());
	}

	public void save(String newText) {
		if (saveLocked) {
			return;
		}

		try {
			IOUtil.writeFile(file, newText);

			// XXX if I just wrote rtf rich text to .txt file, might want to
			// rename that file.

			// XXX after 'make plain text' command, should write .txt file, not
			// .rtf

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	final private HashMap<String, String> emptyMap = Factory.newHashMap();

	public Map<String, String> getMetaMap() {
		try {
			String json = new String(IOUtil.readFile(meta), Charset.defaultCharset());
			if (json == null || json.isEmpty()) {
				return emptyMap;
			}

			JSONObject o = new JSONObject(json);
			HashMap<String, String> map = Factory.newHashMap();

			@SuppressWarnings("unchecked")
			Iterator<String> i = o.keys();
			while (i.hasNext()) {
				String key = i.next();
				String value = o.optString(key);
				map.put(key, value);
			}

			return map;
		} catch (JSONException e) {
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
				if (file.exists()) {
					s = file.getName();
					s = s.replace("." + FilenameUtils.getExtension(s), "");
				} else {
					s = "Untitled";
				}
			}
			return s;
		}

		@Override
		public long created() {
			try {
				return Long.valueOf(map.get("created"));
			} catch (NumberFormatException e) {
				return new Date().getTime();
			}
		}

		@Override
		public void title(String newTitle) {
			setMeta("title", newTitle);
			reload();
		}

		@Override
		public void setCreatedTime() {
			setMeta("created", String.valueOf(new Date().getTime()));
			reload();
		}

		private void reload() {
			map = getMetaMap();
		}

		@Override
		public int getAttachmentPosition(File attachment) {
			String key = "attachment:" + attachment.getName() + ":position";
			String value = map.get(key);
			if (value == null) {
				return 0;
			}
			return Integer.parseInt(value);
		}

		@Override
		public void setAttachmentPosition(File attachment, int position) {
			setMeta("attachment:" + attachment.getName() + ":position", String.valueOf(position));
			reload();
		}

	}

	private String ts() {
		return Long.toString(System.currentTimeMillis(), 36);
	}

	public void moveTo(File dest) {

		File destFile = new File(dest + File.separator + file.getName());
		File destMeta = metaFromFile(destFile);
		File destAtts = new File(attachmentFolderPath(destFile));

		if (destFile.exists() || destMeta.exists() || destAtts.exists()) {
			try {
				attemptSafeRename(file.getName());
				moveTo(dest);
				return;
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
		}

		try {
			FileUtils.moveFileToDirectory(file, dest, false);
			if (meta.exists()) {
				FileUtils.moveFile(meta, destMeta);
			}

			File atts = new File(attachmentFolderPath(file));
			if (atts.exists() && atts.isDirectory()) {
				FileUtils.moveDirectoryToDirectory(atts, dest, true);
			}

			Notebook source = Vault.getInstance().findNotebook(file.getParentFile());
			if (source != null) {
				source.refresh();
			}

			Notebook nb = Vault.getInstance().findNotebook(dest);
			if (nb != null) {
				nb.refresh();
			}

			NotebookEvent event = new NotebookEvent(Kind.noteMoved);
			event.source = file;
			event.dest = destFile;
			Elephant.eventBus.post(event);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void attemptSafeRename(String newName) throws IOException {

		newName = newName.replaceAll("[^a-zA-Z0-9 \\.\\-]", "_");

		File newFile = new File(file.getParentFile().getAbsolutePath() + File.separator + newName);
		File newMeta = metaFromFile(newFile);

		File atts = new File(attachmentFolderPath(file));
		File newAtts = null;
		if (atts.exists() && atts.isDirectory()) {
			newAtts = new File(attachmentFolderPath(newFile));
		}

		if (newFile.exists() || newMeta.exists() || (newAtts != null && newAtts.exists())) {
			// fallback
			String base = FilenameUtils.getBaseName(newName);
			String ext = FilenameUtils.getExtension(newName);
			newName = base + "_" + ts() + "." + ext;
			attemptSafeRename(newName);
			return;
		}

		if (meta.exists()) {
			FileUtils.moveFile(meta, newMeta);
		}
		if (file.exists()) {
			FileUtils.moveFile(file, newFile);
		}

		file = newFile;
		meta = newMeta;

		if (newAtts != null) {
			FileUtils.moveDirectory(atts, newAtts);
		}
	}

	public File importAttachment(File f) throws IOException {
		File dest = new File(attachmentFolder().getAbsolutePath() + File.separator + f.getName());

		String orgDest = dest.getAbsolutePath();

		int n = 1;
		while (dest.exists()) {
			String ext = "." + FilenameUtils.getExtension(orgDest);
			dest = new File(orgDest.replace(ext, " " + n + ext));
			n++;
		}

		FileUtils.copyFile(f, dest);
		return dest;
	}

	private String attachmentFolderPath(File f) {
		return f.getAbsolutePath() + ".attachments";
	}

	private File attachmentFolder() throws IOException {
		String s = attachmentFolderPath(file);

		File f = new File(s);
		if (!f.exists()) {
			if (!f.mkdirs()) {
				throw new IOException();
			}
		}

		return f;
	}

	public File[] getAttachmentList() {
		File f = new File(attachmentFolderPath(file));
		if (f.exists()) {
			return f.listFiles();
		} else {
			return emptyFileList;
		}
	}

	public void removeAttachment(File f) {
		try {
			File deletedFolder = new File(attachmentFolder() + File.separator + "deleted");
			// XXX file may exist in deleted folder already, should rename to
			// unique
			FileUtils.moveFileToDirectory(f, deletedFolder, true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
