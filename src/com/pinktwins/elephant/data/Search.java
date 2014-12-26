package com.pinktwins.elephant.data;

import java.util.ArrayList;
import java.util.HashSet;

public class Search {

	public static final SimpleSearchIndex ssi = new SimpleSearchIndex();

	public static Notebook search(String text) {
		text = text.toLowerCase();

		Notebook found = new Notebook();
		found.setName(Notebook.NAME_SEARCH);

		if (!ssi.ready()) {
			for (Notebook nb : Vault.getInstance().getNotebooks()) {
				if (!nb.isTrash()) {
					for (Note n : nb.notes) {
						ssi.digestNote(n, nb);
					}
				}
			}
			ssi.markReady();
		}

		ArrayList<HashSet<Note>> sets = Factory.newArrayList();

		/*
		 * List<String> keys = new ArrayList<String>(); Matcher m =
		 * Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(text); while
		 * (m.find()) { keys.add(m.group(1).replace("\"", "")); }
		 */

		String[] a = text.split(" ");
		for (String q : a) {
			HashSet<Note> notes = Factory.newHashSet();
			notes.addAll(ssi.search(q));
			sets.add(notes);
		}

		HashSet<Note> smallest = null;
		int smallestSize = Integer.MAX_VALUE;

		for (HashSet<Note> notes : sets) {
			if (notes.size() < smallestSize) {
				smallest = notes;
			}
		}

		if (smallest != null) {
			for (HashSet<Note> notes : sets) {
				smallest.retainAll(notes);
			}

			for (Note n : smallest) {
				found.addNote(n);
			}
		}

		found.sortNotes();

		int len = found.count();
		String s = len + " note";
		if (len != 1) {
			s += "s";
		}
		s += " found";

		found.setName(s);

		return found;
	}
}
