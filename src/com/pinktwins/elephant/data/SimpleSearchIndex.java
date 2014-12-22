package com.pinktwins.elephant.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.eventbus.Subscribe;
import com.pinktwins.elephant.Elephant;

public class SimpleSearchIndex {

	private boolean isReady = false;

	private HashMap<String, Set<Note>> map = new HashMap<String, Set<Note>>();

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
		ArrayList<Note> found = new ArrayList<Note>();
		HashSet<Note> foundSet = new HashSet<Note>();

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

		digest(e.note, e.note.getMeta().title());
		digest(e.note, e.note.contents());
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
