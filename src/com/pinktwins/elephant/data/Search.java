package com.pinktwins.elephant.data;

import java.util.List;
import java.util.Set;

import com.pinktwins.elephant.eventbus.IndexProgressEvent;
import com.pinktwins.elephant.util.Factory;

public class Search {

	public static final SearchIndexer ssi = new SearchIndexer();

	private Search() {
	}

	public static Notebook search(String text) {
		text = text.toLowerCase();

		Notebook found = new Notebook();
		found.setName(Notebook.NAME_SEARCH);
		found.setToSearchResultNotebook();

		int totalNotes = 0;
		int progress = -1;

		if (!ssi.ready()) {
			ssi.start();

			int noteCount = Vault.getInstance().getNoteCount();

			for (Notebook nb : Vault.getInstance().getNotebooks()) {
				if (!nb.isTrash()) {
					for (Note n : nb.notes) {
						ssi.digestNote(n, nb);
						totalNotes++;

						int p = (int) (totalNotes / (float) noteCount * 100);
						if (progress != p / 10) {
							progress = p;
							new IndexProgressEvent(p).post();
						}
					}
				}
			}
			ssi.markReady();
		}

		List<Set<Note>> sets = Factory.newArrayList();

		/*
		 * List<String> keys = new ArrayList<String>(); Matcher m =
		 * Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(text); while (m.find()) {
		 * keys.add(m.group(1).replace("\"", "")); }
		 */

		String[] a = text.split(" ");
		for (String q : a) {
			q = q.trim();
			Set<Note> notes = Factory.newHashSet();
			notes.addAll(ssi.search(q));
			sets.add(notes);
		}

		Set<Note> smallest = null;
		int smallestSize = Integer.MAX_VALUE;

		for (Set<Note> notes : sets) {
			if (notes.size() < smallestSize) {
				smallest = notes;
			}
		}

		if (smallest != null) {
			for (Set<Note> notes : sets) {
				smallest.retainAll(notes);
			}

			for (Note n : smallest) {
				found.addNote(n);
			}
		}

		int len = found.count();

		found.sortNotes();

		String s = len + " note";
		if (len != 1) {
			s += "s";
		}
		s += " found";

		found.setName(s);

		return found;
	}
}
