package com.pinktwins.elephant.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.eventbus.Subscribe;
import com.pinktwins.elephant.Elephant;
import com.pinktwins.elephant.data.Note.Meta;
import com.pinktwins.elephant.eventbus.NoteChangedEvent;
import com.pinktwins.elephant.eventbus.NotebookEvent;
import com.pinktwins.elephant.util.Factory;

public class SimpleSearchIndex {

	private boolean isReady = false;

	private HashMap<String, Set<Note>> map = Factory.newHashMap();

	public SimpleSearchIndex() {
		Elephant.eventBus.register(this);
	}

	public boolean ready() {
		return isReady;
	}

	public void markReady() {
		isReady = true;
	}

	public void digest(Note n, String text) {
		String[] a = text.split(" ");
		for (String s : a) {
			s = s.toLowerCase().trim();
			if (s.isEmpty()) {
				continue;
			}

			Set<Note> set = map.get(s);
			if (set == null) {
				set = new HashSet<Note>();
			}
			set.add(n);
			map.put(s, set);
		}
	}

	public List<Note> search(String text) {
		ArrayList<Note> found = Factory.newArrayList();
		HashSet<Note> foundSet = Factory.newHashSet();

		Set<String> strs = map.keySet();
		for (String s : strs) {
			if (s.indexOf(text) >= 0) {
				foundSet.addAll(map.get(s));
			}
		}

		found.addAll(foundSet);
		return found;
	}

	public void purgeNote(Note note) {
		for (String s : map.keySet()) {
			Set<Note> set = map.get(s);
			if (set != null) {
				set.remove(note);
			}
		}
	}

	public void digestNote(Note note, Notebook nb) {
		Meta meta = note.getMeta();
		digest(note, meta.title());

		String contents = note.contents();
		if (contents.startsWith("{\\rtf")) {
			contents = note.plainTextContents(contents);
		}
		digest(note, contents);

		List<String> tagIds = meta.tags();
		if (!tagIds.isEmpty()) {
			List<String> tagNames = Vault.getInstance().resolveTagIds(tagIds);
			for (String s : tagNames) {
				digest(note, s + " tag:" + s + " t:" + s);
			}
		}

		if (nb != null) {
			digest(note, "notebook:" + nb.name() + " nb:" + nb.name());
		}
	}

	public void debug() {
		System.out.println("SSI has " + map.keySet().size() + " strings.");
		long n = 0;
		for (String s : map.keySet()) {
			Set<Note> set = map.get(s);
			n += set.size();
		}
		System.out.println("total of " + n + " set items.");
	}

	@Subscribe
	public void handleNoteChanged(NoteChangedEvent e) {
		purgeNote(e.note);
		digestNote(e.note, e.note.findContainingNotebook());
	}

	@Subscribe
	public void handleNotebookEvent(NotebookEvent event) {
		switch (event.kind) {
		case noteMoved:
			if (event.source != null && event.dest != null) {
				Note oldNote = new Note(event.source);
				Note newNote = new Note(event.dest);

				purgeNote(oldNote);
				digestNote(newNote, newNote.findContainingNotebook());
			}
			break;
		case noteCreated:
			break;
		default:
			break;
		}
	}
}
