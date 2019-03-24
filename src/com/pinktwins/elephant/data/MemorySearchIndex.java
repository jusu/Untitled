package com.pinktwins.elephant.data;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.pinktwins.elephant.util.Factory;

// Naive and memory-wasteful index for instant searching.
// Works well enough for up to 5,000 - 15,000 notes, after
// that something else should be used.

public class MemorySearchIndex implements SearchIndexInterface {

	// word -> Set<Note>
	private Map<String, Set<Note>> wordMap = Factory.newHashMap();

	// Escaped chars in regex patterns
	// <([{\^-=$!|]})?*+.>

	// Split by these chars.
	String[] splitChars = { " ", "\n", "\r", "\t" };

	Pattern splitter = Pattern.compile(StringUtils.join(splitChars, "|"));

	/**
	 * Do not split up the text in words, keep the complete text together.
	 *
	 * @param n The note that will be indexed.
	 * @param text The text to find the note.
	 */
	public void digestEntirely(Note n, String text) {
		addNoteToMap(n, text.toLowerCase());
	}

	private void addNoteToMap(Note note, String text) {
		synchronized (wordMap) {
			Set<Note> indexedNotes = wordMap.get(text);
			if (indexedNotes == null) {
				indexedNotes = Factory.newHashSet();
				wordMap.put(text, indexedNotes);
			}
			indexedNotes.add(note);
		}
	}

	@Override
	public void digestText(Note n, String text) {
		String[] a = splitter.split(text);
		for (String s : a) {
			s = s.toLowerCase().trim();
			if (s.isEmpty()) {
				continue;
			}

			if (s.length() > 100) {
				continue;
			}

			if (s.indexOf("\uFFFD") > -1) {
				continue;
			}

			if (s.indexOf("##") > -1) {
				continue;
			}

			addNoteToMap(n, s);
		}
	}

	@Override
	public void digestDate(Note note, long dateValue) {
		String formats = "EEE EEE, EEEE EEEE, MMM MMM, MMMM MMMM, a dd dd, yyyy dd/MM/yyyy dd/MM/yy";

		Date date = new Date(dateValue);
		// create date format for search
		SimpleDateFormat wordFormat = new SimpleDateFormat(formats);

		digestText(note, wordFormat.format(date));
	}

	public Set<Note> search(String text) {
		Set<Note> foundSet = Factory.newHashSet();

		synchronized (wordMap) {
			Set<String> strs = wordMap.keySet();
			for (String s : strs) {
				if (s.indexOf(text) >= 0) {
					foundSet.addAll(wordMap.get(s));
				}
			}
		}

		return foundSet;
	}

	@Override
	public synchronized void purgeNote(Note note) {
		synchronized (wordMap) {
			for (Set<Note> set : wordMap.values()) {
				set.remove(note);
			}
		}
	}

	@Override
	public void debug() {
		synchronized (wordMap) {
			System.out.println("SSI memoryIndex has " + wordMap.keySet().size() + " strings/sets");
			long n = 0;

			for (String s : wordMap.keySet()) {
				Set<Note> set = wordMap.get(s);
				n += set.size();
			}

			System.out.println("total of " + n + " set items.");
		}
	}

	@Override
	public void commit() {
	}
}
