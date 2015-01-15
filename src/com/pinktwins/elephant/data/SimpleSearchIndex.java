package com.pinktwins.elephant.data;

import java.io.File;
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
import com.pinktwins.elephant.eventbus.SearchIndexChangedEvent;
import com.pinktwins.elephant.util.Factory;

public class SimpleSearchIndex {

	private boolean isReady = false;

	// word -> Set<Note>
	private HashMap<String, Set<Note>> wordMap = Factory.newHashMap();
	// tagId -> Set<Note>
	private HashMap<String, Set<Note>> tagMap = Factory.newHashMap();

	// note file -> lastModified() of notefile when note digested
	private HashMap<File, Long> digestTimes = Factory.newHashMap();

	public SimpleSearchIndex() {
		Elephant.eventBus.register(this);
	}

	public boolean ready() {
		return isReady;
	}

	public void markReady() {
		isReady = true;
	}

	public void digestWord(Note n, String text) {
		String[] a = text.split(" ");
		for (String s : a) {
			s = s.toLowerCase().trim();
			if (s.isEmpty()) {
				continue;
			}

			Set<Note> set = wordMap.get(s);
			if (set == null) {
				set = Factory.newHashSet();
			}
			set.add(n);
			wordMap.put(s, set);
		}
	}

	public void digestTag(Note n, String tagId) {
		if (tagId.isEmpty()) {
			return;
		}

		Set<Note> set = tagMap.get(tagId);
		if (set == null) {
			set = Factory.newHashSet();
		}

		set.add(n);
		tagMap.put(tagId, set);
	}

	public List<Note> search(String text) {
		ArrayList<Note> found = Factory.newArrayList();
		HashSet<Note> foundSet = Factory.newHashSet();

		Set<String> strs = wordMap.keySet();
		for (String s : strs) {
			if (s.indexOf(text) >= 0) {
				foundSet.addAll(wordMap.get(s));
			}
		}

		found.addAll(foundSet);
		return found;
	}

	public void purgeNote(Note note) {
		for (Set<Note> set : wordMap.values()) {
			set.remove(note);
		}

		for (Set<Note> set : tagMap.values()) {
			set.remove(note);
		}
	}

	public void digestNote(Note note, Notebook nb) {
		Meta meta = note.getMeta();
		digestWord(note, meta.title());

		String contents = note.contents();
		if (contents.startsWith("{\\rtf")) {
			contents = note.plainTextContents(contents);
		}
		digestWord(note, contents);

		List<String> tagIds = meta.tags();
		if (!tagIds.isEmpty()) {
			for (String s : tagIds) {
				digestTag(note, s);
			}

			List<String> tagNames = Vault.getInstance().resolveTagIds(tagIds);
			for (String s : tagNames) {
				digestWord(note, s + " tag:" + s + " t:" + s);
			}
		}

		if (nb != null) {
			digestWord(note, "notebook:" + nb.name() + " nb:" + nb.name());
		}

		addDigestTimestamp(note);
	}

	private void addDigestTimestamp(Note note) {
		digestTimes.put(note.file(), note.lastModified());
	}

	public long getDigestTime(File f) {
		Long ts = digestTimes.get(f);
		if (ts == null) {
			return 0;
		}
		return ts.longValue();
	}

	public void debug() {
		System.out.println("SSI has " + wordMap.keySet().size() + " strings.");
		long n = 0;
		for (String s : wordMap.keySet()) {
			Set<Note> set = wordMap.get(s);
			n += set.size();
		}
		System.out.println("total of " + n + " set items.");
	}

	public Set<Note> notesByTag(String tagId) {
		Set<Note> notes = tagMap.get(tagId);
		if (notes == null) {
			notes = Factory.newHashSet();
		}
		return notes;
	}

	@Subscribe
	public void handleNoteChanged(NoteChangedEvent e) {
		purgeNote(e.note);
		digestNote(e.note, e.note.findContainingNotebook());

		Elephant.eventBus.post(new SearchIndexChangedEvent());
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
