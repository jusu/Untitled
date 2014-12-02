package com.pinktwins.elephant.data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Notebook {
	private String name = "";
	private File folder;

	public ArrayList<Note> notes = new ArrayList<Note>();

	public Notebook(File folder) {
		name = folder.getName();
		this.folder = folder;

		populate();
	}

	private void populate() {
		notes.clear();
		for (File f : folder.listFiles()) {
			String name = f.getName();
			if (name.charAt(0) != '.' && !name.endsWith("~")) {
				notes.add(new Note(f));
			}
		}
	}

	public List<Note> getNotes() {
		return notes;
	}

	public String name() {
		return name;
	}
	
	public int count() {
		return notes.size();
	}

	public Note newNote() throws IOException {
		String fullPath = this.folder.getAbsolutePath() + File.separator + System.currentTimeMillis();
		File f = new File(fullPath);

		f.createNewFile();
		Note n = new Note(f);
		n.getMeta().title("Untitled");
		
		notes.add(0, n);

		return n;
	}
}
