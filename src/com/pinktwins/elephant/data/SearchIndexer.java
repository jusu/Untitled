package com.pinktwins.elephant.data;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.google.common.eventbus.Subscribe;
import com.pinktwins.elephant.Elephant;
import com.pinktwins.elephant.data.Note.Meta;
import com.pinktwins.elephant.eventbus.NoteChangedEvent;
import com.pinktwins.elephant.eventbus.NotebookEvent;
import com.pinktwins.elephant.eventbus.SearchIndexChangedEvent;
import com.pinktwins.elephant.util.Factory;

public class SearchIndexer {

	private boolean isReady = false;

	// tagId -> Set<Note>
	private HashMap<String, Set<Note>> tagMap = Factory.newHashMap();

	// note file -> lastModified() of notefile when note digested
	private HashMap<File, Long> digestTimes = Factory.newHashMap();

	private SearchIndexInterface memoryIndex = new MemorySearchIndex();
	private SearchIndexInterface luceneIndex;

	static boolean useLucene = true;

	public SearchIndexer() {
		Elephant.eventBus.register(this);
	}

	public void start() {
		if (useLucene) {
			luceneIndex = new LuceneSearchIndex();
		}
	}

	public boolean ready() {
		return isReady;
	}

	public void markReady() {
		isReady = true;
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
		found.addAll(memoryIndex.search(text));
		if (useLucene) {
			found.addAll(luceneIndex.search(text));
		}
		return found;
	}

	public void purgeNote(Note note) {
		memoryIndex.purgeNote(note);
		if (useLucene) {
			luceneIndex.purgeNote(note);
		}

		for (Set<Note> set : tagMap.values()) {
			set.remove(note);
		}
	}

	public void digestNote(Note note, Notebook nb) {
		// Dont index notes in Trash.
		if (nb != null && nb.isTrash()) {
			return;
		}

		Meta meta = note.getMeta();
		memoryIndex.digestText(note, meta.title());

		if (useLucene) {
			luceneIndex.digestText(note, null);
		} else {
			// Memory index
			String contents = note.contents();
			if (contents.startsWith("{\\rtf")) {
				contents = Note.plainTextContents(contents);
			}
			memoryIndex.digestText(note, contents);
		}

		List<String> tagIds = meta.tags();
		if (!tagIds.isEmpty()) {
			for (String s : tagIds) {
				digestTag(note, s);
			}

			List<String> tagNames = Vault.getInstance().resolveTagIds(tagIds);
			for (String s : tagNames) {
				memoryIndex.digestText(note, s + " tag:" + s + " t:" + s);
			}
		}

		if (nb != null) {
			memoryIndex.digestText(note, "notebook:" + nb.name() + " nb:" + nb.name());
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
		memoryIndex.debug();
	}

	public Set<Note> notesByTag(String tagId) {
		Set<Note> notes = tagMap.get(tagId);
		if (notes == null) {
			notes = Factory.newHashSet();
		}
		return notes;
	}

	@Subscribe
	public void handleNoteChanged(NoteChangedEvent e) throws Exception {
		try {
			purgeNote(e.note);
			digestNote(e.note, e.note.findContainingNotebook());
		} catch (Exception ex) {
			ex.printStackTrace();
			throw ex;
		}

		if (useLucene && ready()) {
			luceneIndex.commit();
		}

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
