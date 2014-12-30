package com.pinktwins.elephant.data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FilenameUtils;

import com.pinktwins.elephant.Elephant;
import com.pinktwins.elephant.data.Note.Meta;
import com.pinktwins.elephant.eventbus.NotebookEvent;
import com.pinktwins.elephant.eventbus.NotebookEvent.Kind;
import com.pinktwins.elephant.util.Factory;

public class Notebook implements Comparable<Notebook> {
	final static public String NAME_ALLNOTES = "All Notes";
	final static public String NAME_SEARCH = "Search";

	private String name = "";
	private File folder;
	private boolean isSearch;

	public ArrayList<Note> notes = Factory.newArrayList();

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
		populate();
	}

	public void setToSearchResultNotebook() {
		isSearch = true;
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

	private void populate() {
		notes.clear();
		for (File f : folder.listFiles()) {
			String name = f.getName();
			String ext = FilenameUtils.getExtension(f.getName()).toLowerCase();

			if (name.charAt(0) != '.' && !name.endsWith("~") && (ext.equals("txt") || ext.equals("rtf"))) {
				try {
					if (f.isFile()) {
						notes.add(new Note(f));
					}
				} catch (SecurityException e) {
					e.printStackTrace();
				}
			}
		}

		sortNotes();
	}

	public void addNote(Note n) {
		notes.add(n);
	}

	public void sortNotes() {
		Collections.sort(notes);
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

		Elephant.eventBus.post(new NotebookEvent(Kind.noteCreated));

		return n;
	}

	public void deleteNote(Note note) {
		if (isTrash()) {
			return;
		}

		File trash = Vault.getInstance().getTrash();
		if (folder != null && folder.equals(trash)) {
			return;
		}

		notes.remove(note);
		note.moveTo(trash);
	}

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
			e.printStackTrace();
		}
		return false;
	}
}
