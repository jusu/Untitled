package com.pinktwins.elephant.data;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.io.FilenameUtils;

import com.pinktwins.elephant.data.Note.Meta;
import com.pinktwins.elephant.eventbus.NotebookEvent;
import com.pinktwins.elephant.util.Factory;
import com.pinktwins.elephant.util.IOUtil;

public class Notebook implements Comparable<Notebook> {

	private static final Logger LOG = Logger.getLogger(Notebook.class.getName());

	private static final String[] POPULATE_EXTENSIONS = { "txt", "rtf", "md", "html", "htm" };

	public static final String NAME_ALLNOTES = "All Notes";
	public static final String NAME_SEARCH = "Search";

	private String name = "";
	private File folder;
	private boolean isSearch, isTagSearch;
	private boolean isPreviewDisabled = false;

	public List<Note> notes = Factory.newArrayList();

	@Override
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		}

		if (o instanceof File) {
			return folder != null && folder.equals(o);
		}

		if (o instanceof Notebook) {
			Notebook onb = (Notebook) o;
			if (folder != null && onb.folder != null) {
				return folder.equals(onb.folder);
			} else {
				return name.equals(onb.name);
			}
		}

		return false;
	}

	@Override
	public int hashCode() {
		return folder.hashCode();
	}

	@Override
	public int compareTo(Notebook nb) {
		return name().toLowerCase().compareTo(nb.name().toLowerCase());
	}

	public File folder() {
		return folder;
	}

	public Notebook() {
	}

	public void populateFromNotebook(Notebook nb) {
		notes.addAll(nb.notes);
	}

	public Notebook(File folder) {
		name = folder.getName();
		this.folder = folder;

		File f = new File(Vault.getInstance().getHome() + File.separator + name + File.separator + ".disablePreview");
		isPreviewDisabled = f.exists();

		populate();
	}

	public void setToSearchResultNotebook() {
		isSearch = true;
	}

	public void setToTagResultNotebook() {
		isTagSearch = true;
	}

	public static Notebook createNotebook() throws IOException {
		String baseName = Vault.getInstance().getHome() + File.separator + "New notebook";
		File f = new File(baseName);
		int n = 2;
		while (f.exists()) {
			f = new File(baseName + " " + n);
			n++;
		}

		if (!f.mkdirs()) {
			throw new IOException();
		}

		return new Notebook(f);
	}

	public static Notebook getNotebookWithAllNotes() {
		Notebook all = new Notebook();
		all.name = NAME_ALLNOTES;

		for (Notebook nb : Vault.getInstance().getNotebooks()) {
			if (!nb.isTrash()) {
				all.populateFromNotebook(nb);
			}
		}
		all.sortNotes();

		return all;
	}

	public static Notebook getNotebookWithTag(String tagId, String tagName) {
		Notebook nb = new Notebook();
		nb.setToTagResultNotebook();
		nb.name = "Tag " + tagName;
		Set<Note> notes = Search.ssi.notesByTag(tagId);
		nb.notes.addAll(notes);
		return nb;
	}

	private boolean isNoteExtension(String ext) {
		for (String s : POPULATE_EXTENSIONS) {
			if (s.equalsIgnoreCase(ext)) {
				return true;
			}
		}
		return false;
	}

	private void populate() {
		boolean didDigest = false;

		if (folder != null) {
			notes.clear();
			for (File f : folder.listFiles()) {
				String name = f.getName();
				String ext = FilenameUtils.getExtension(f.getName()).toLowerCase();

				if (name.charAt(0) != '.' && !name.endsWith("~") && isNoteExtension(ext)) {
					try {
						if (f.isFile()) {
							Note note = new Note(f);
							note.setPreviewDisabled(isPreviewDisabled);

							notes.add(note);

							// Re-digest possibly modified notes.
							// Only for when all notes have been indexed already,
							// and notes have been modified externally, by sync.
							if (Search.ssi.ready()) {
								long digestTime = Search.ssi.getDigestTime(f);
								if (f.lastModified() != digestTime) {
									Search.ssi.digestNote(note, this);
									didDigest = true;
								}
							}

						}
					} catch (SecurityException e) {
						LOG.severe("Fail: " + e);
					}
				}
			}
		}

		sortNotes();

		if (didDigest) {
			Search.ssi.commit();
		}
	}

	public void addNote(Note n) {
		notes.add(n);
	}

	public void sortNotes() {
		Collections.sort(notes);
	}

	public void truncNotes(int limit) {
		if (notes.size() > limit) {
			notes = notes.subList(0, limit);
		}
	}

	public List<Note> getNotes() {
		return notes;
	}

	public String name() {
		return name;
	}

	public void setName(String s) {
		name = s;
	}

	public int count() {
		return notes.size();
	}

	public boolean isTrash() {
		return "Trash".equals(name);
	}

	public boolean isAllNotes() {
		return NAME_ALLNOTES.equals(name);
	}

	public boolean isSearch() {
		return isSearch;
	}

	public boolean isTagSearch() {
		return isTagSearch;
	}

	public boolean isDynamicallyCreatedNotebook() {
		return isAllNotes() || isTrash() || isSearch() || isTagSearch();
	}

	public Note newNote() throws IOException {
		if (folder == null) {
			throw new IllegalStateException();
		}

		String fullPath = this.folder.getAbsolutePath() + File.separator + Long.toString(System.currentTimeMillis(), 36) + ".txt";
		File f = new File(fullPath);

		f.createNewFile();
		Note n = new Note(f);

		Meta m = n.getMeta();
		m.title("Untitled");
		m.setCreatedTime();

		notes.add(0, n);

		new NotebookEvent(NotebookEvent.Kind.noteCreated, f, f).post();

		return n;
	}

	public void deleteNote(Note note) {
		if (isTrash()) {
			notes.remove(note);
			note.delete();
			return;
		}

		File trash = Vault.getInstance().getTrash();
		if (folder != null && folder.equals(trash)) {
			notes.remove(note);
			note.delete();
			return;
		}

		notes.remove(note);
		note.moveTo(trash);
	}

	@SuppressWarnings("unlikely-arg-type")
	public Note find(String name) {
		File note = new File(folder + File.separator + name);
		for (Note n : notes) {
			if (n.equals(note)) {
				return n;
			}
		}
		return null;
	}

	public void refresh() {
		populate();
	}

	public boolean rename(String s) {
		if (folder == null) {
			throw new IllegalStateException();
		}

		File newFile = new File(folder.getParentFile() + File.separator + s);
		try {
			if (folder.renameTo(newFile)) {
				folder = newFile;
				return true;
			}
		} catch (Exception e) {
			LOG.severe("Fail: " + e);
		}
		return false;
	}

	public void markNoteSavedTimestamp() {
		// write current timestamp to note folder, in file ".lastSaveTs"
		File f = new File(folder + File.separator + ".lastSaveTs");
		try {
			IOUtil.writeFile(f, String.valueOf(System.currentTimeMillis()));
		} catch (IOException e) {
			LOG.severe("Fail: " + e);
		}
	}
}
