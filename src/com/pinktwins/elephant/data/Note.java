package com.pinktwins.elephant.data;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONException;
import org.json.JSONObject;

import com.pinktwins.elephant.Elephant;
import com.pinktwins.elephant.data.NotebookEvent.Kind;

public class Note {
	private File file, meta;
	private String fileName = "";
	
	private boolean saveLocked = false;

	private BasicFileAttributes attr;
	static private DateTimeFormatter df = DateTimeFormat.forPattern("dd MMM yyyy").withLocale(Locale.getDefault());

	public interface Meta {
		public String title();

		public void title(String newTitle);

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

	private File metaFromFile(File f) {
		return new File(f.getParentFile().getAbsolutePath() + File.separator + "." + f.getName() + ".meta");
	}

	public Note(File f) {
		file = f;
		meta = metaFromFile(f);

		String ext = FilenameUtils.getExtension(f.getName());
		if (!"txt".equals(ext) && !"rtf".equals(ext)) {
			saveLocked = true;
		}

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
		if (saveLocked) {
			return "(binary)";
		}

		try {
			contents = IOUtil.readFile(file);
			return new String(contents, Charset.defaultCharset());
		} catch (IOException e) {
			e.printStackTrace();
		}

		return "";
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
				if (file.exists()) {
					s = file.getName();
					s = s.replace("." + FilenameUtils.getExtension(s),  "");
				} else {
					s = "Untitled";
				}
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
		File destMeta = new File(dest + File.separator + meta.getName());
		File destAtts = new File(attachmentFolderPath(destFile));

		if (destFile.exists() || destMeta.exists() || destAtts.exists()) {
			attemptSafeRename(file.getName());
			moveTo(dest);
			return;
		}

		try {
			FileUtils.moveFileToDirectory(file, dest, false);
			FileUtils.moveFileToDirectory(meta, dest, false);

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

			Elephant.eventBus.post(new NotebookEvent(Kind.noteMoved));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void attemptSafeRename(String newName) {

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

		try {
			FileUtils.moveFile(meta, newMeta);
			FileUtils.moveFile(file, newFile);

			file = newFile;
			meta = newMeta;

			if (newAtts != null) {
				FileUtils.moveDirectory(atts, newAtts);
			}
		} catch (IOException e) {
			e.printStackTrace();
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
			return null;
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
