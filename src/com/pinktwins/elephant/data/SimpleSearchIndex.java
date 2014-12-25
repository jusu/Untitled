package com.pinktwins.elephant.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.eventbus.Subscribe;
import com.pinktwins.elephant.Elephant;
import com.pinktwins.elephant.data.Note.Meta;

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
			s = s.toLowerCase();

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

	@Subscribe
	public void handleNoteChanged(NoteChangedEvent e) {
		for (String s : map.keySet()) {
			Set<Note> set = map.get(s);
			if (set != null) {
				set.remove(e.note);
			}
		}

		digestNote(e.note, Vault.getInstance().findNotebook(e.note.file().getParentFile()));
	}

	public void digestNote(Note note, Notebook nb) {
		Meta meta = note.getMeta();
		digest(note, meta.title());
		digest(note, note.contents());

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
}
