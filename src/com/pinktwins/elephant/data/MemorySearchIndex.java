package com.pinktwins.elephant.data;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

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

			if (NumberUtils.isNumber(s.replace("#", ""))) {
				continue;
			}

			if (s.indexOf("##") > -1) {
				continue;
			}

			synchronized (wordMap) {
				Set<Note> set = wordMap.get(s);
				if (set == null) {
					set = Factory.newHashSet();
					wordMap.put(s, set);
				}
				set.add(n);
			}
		}
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
