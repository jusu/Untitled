package com.pinktwins.elephant.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Set;

import com.pinktwins.elephant.Elephant;
import com.pinktwins.elephant.eventbus.IndexProgressEvent;
import com.pinktwins.elephant.util.Factory;
import com.pinktwins.elephant.util.StringParser;

public class Search {

	private static final String encoderCharset = java.nio.charset.StandardCharsets.UTF_8.toString();
	public static final SearchIndexer ssi = new SearchIndexer();

	private Search() {
	}

	public static Object lockObject = new Object();
	
	synchronized public static Notebook search(String text) {
		text = text.toLowerCase();

		Notebook found = new Notebook();
		found.setName(Notebook.NAME_SEARCH);
		found.setToSearchResultNotebook();

		int totalNotes = 0;
		int progress = -1;

		if (!ssi.ready()) {
			ssi.start();

			Vault vault = Vault.getInstance();

			synchronized (lockObject) {
				int noteCount = vault.getNoteCount();

				for (Notebook nb : vault.getNotebooks()) {
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
			}

			ssi.markReady();
			ssi.commit();
		}

		List<Set<Note>> sets = Factory.newArrayList();
		List<Set<Note>> negativeSets = Factory.newArrayList();

		StringParser parser = new StringParser();
		StringParser.Buffer buf = new StringParser.Buffer(text);
		for (StringParser.Token token = parser.getNextNonWhitespaceToken(buf);
			 token.isString(); token = parser.getNextNonWhitespaceToken(buf)) {

			// At this point this can be:
			// * "title:", "!title:" or any other prefix.
			// * "some non-prefix text with spaces"
			// * "textwithoutspaces", "!textwithoutspaces"
			// * "!"
			String searchTerm = token.getValue();
			boolean negation = searchTerm.startsWith("!");
			if(negation) {
				searchTerm = searchTerm.substring(1);
			}

			// The searchTerm is a prefix eg. title: nb: tag:
			// It is not complete without the suffix. We must read the
			// suffix to complete the searchTerm.
			if(SearchIndexer.ALL_PREFIXES.contains(searchTerm)) {
				token = parser.getNextNonWhitespaceToken(buf);
				if(token.isString()) searchTerm = searchTerm + token.getValue();
			}

			Set<Note> notes = Factory.newHashSet();
			if (negation) {
				notes.addAll(ssi.search(searchTerm));
				negativeSets.add(notes);
			} else {
				notes.addAll(ssi.search(searchTerm));
				sets.add(notes);
			}
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

			for (Set<Note> negatives : negativeSets) {
				smallest.removeAll(negatives);
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

	private static String encode(String s) throws UnsupportedEncodingException {
		return URLEncoder.encode(s, encoderCharset).replaceAll("\\+", "%20");
	}

	public static void main(String[] args) {
		Elephant.args = args;

		String vaultPath = Elephant.settings.getString(Settings.Keys.VAULT_FOLDER);
		Vault.getInstance().setLocation(vaultPath);

		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		try {
			String s;
			while ((s = in.readLine()) != null) {
				if (s.length() > 0) {
					Notebook nb = Search.search(s);
					int i = 0, count = nb.count();
					System.out.print("{\"search\":\"" + s + "\",\"result\":[");
					for (Note n : nb.notes) {
						System.out.print("{\"file\":\"");
						System.out.print(encode(n.file().getParentFile().getName() + "/" + n.file().getName()));
						System.out.print("\",");
						System.out.print("\"title\":\"");
						System.out.print(encode(n.getMeta().title()));
						System.out.print("\",");
						System.out.print("\"updated\":");
						System.out.print(n.lastModified());
						System.out.print("}");
						if (++i < count) {
							System.out.print(",");
						}
					}
					System.out.println("]}");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
