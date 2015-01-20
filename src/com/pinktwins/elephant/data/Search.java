package com.pinktwins.elephant.data;

import java.util.ArrayList;
import java.util.HashSet;

import com.pinktwins.elephant.eventbus.IndexProgressEvent;
import com.pinktwins.elephant.util.Factory;

public class Search {

	public static final SearchIndexer ssi = new SearchIndexer();

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
							IndexProgressEvent.post(p);
						}
					}
				}
			}
			ssi.markReady();
		}

		ArrayList<HashSet<Note>> sets = Factory.newArrayList();

		/*
		 * List<String> keys = new ArrayList<String>(); Matcher m =
		 * Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(text); while (m.find()) {
		 * keys.add(m.group(1).replace("\"", "")); }
		 */

		String[] a = text.split(" ");
		for (String q : a) {
			q = q.trim();
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

		int len = found.count();

		found.sortNotes();

		// No need to limit, new NoteList does a good job paging.
		// found.truncNotes(1000); // limit to 1000 results

		// if (SearchIndexer.useLucene && len >= 1000 && LuceneSearchIndex.lastSearchTotalHits > len) {
		// len = LuceneSearchIndex.lastSearchTotalHits;
		// }

		String s = len + " note";
		if (len != 1) {
			s += "s";
		}
		s += " found";

		found.setName(s);

		return found;
	}
}
