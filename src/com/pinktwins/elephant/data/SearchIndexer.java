package com.pinktwins.elephant.data;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.Subscribe;
import com.pinktwins.elephant.Elephant;
import com.pinktwins.elephant.data.Note.Meta;
import com.pinktwins.elephant.eventbus.NoteChangedEvent;
import com.pinktwins.elephant.eventbus.NotebookEvent;
import com.pinktwins.elephant.eventbus.SearchIndexChangedEvent;
import com.pinktwins.elephant.util.Factory;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class SearchIndexer {

	private static final Logger LOG = Logger.getLogger(SearchIndexer.class.getName());

	public static final List<String> PREFIX_NOTEBOOK = ImmutableList.of("notebook:", "nb:", "@");
    public static final List<String> PREFIX_TAG = ImmutableList.of("tag:", "t:", "#");
    public static final List<String> PREFIX_TITLE = ImmutableList.of("title:");
    public static final List<String> PREFIX_UUID = ImmutableList.of("uuid:");
    public static final List<String> ALL_PREFIXES = new ImmutableList.Builder()
			.addAll(PREFIX_NOTEBOOK)
			.addAll(PREFIX_TAG)
			.addAll(PREFIX_TITLE)
			.addAll(PREFIX_UUID)
			.build();

	private boolean isReady = false;

	// tagId -> Set<Note>
	private Map<String, Set<Note>> tagMap = Factory.newHashMap();

	// note file -> lastModified() of notefile when note digested
	private Map<File, Long> digestTimes = Factory.newHashMap();

	private MemorySearchIndex memoryIndex = new MemorySearchIndex();
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
		List<Note> found = Factory.newArrayList();

		boolean prefixedSearchTerm = ALL_PREFIXES
				.parallelStream()
				.anyMatch((prefix)->text.startsWith(prefix));

        // If the search term contains a prefix we only
        // use the memory index. These are not indexed in lucene anyway.
		if(prefixedSearchTerm){
            found.addAll(memoryIndex.search(text));
        }
		else {
			// Normal non-prefixes searches are performed in both
			// memory index and lucene index.
            found.addAll(memoryIndex.search(text));
            if (useLucene) {
                found.addAll(luceneIndex.search(text));
            }
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
		// Digest the individual words in the title.
		memoryIndex.digestText(note, meta.title());
		// Digest the entire title as well.
		for(String prefix: PREFIX_TITLE) {
			memoryIndex.digestEntirely(note, prefix + meta.title());
		}

		if (useLucene) {
			luceneIndex.digestText(note, null);
		} else {
			// Memory index
			String contents = note.contentsIncludingRawHtml();
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
				// Digest the tag label (the words in the label)
				memoryIndex.digestText(note, s);
				// Digest the entire labels, spaces and all.
				for(String prefix :PREFIX_TAG) {
					memoryIndex.digestEntirely(note, prefix + s);
				}
			}
		}

		if (nb != null) {
			String nbName = nb.name();
			// Digest the notebook name words, so the words in the notebook name can be found.
			memoryIndex.digestText(note, nbName);
			// Digest the entire notebook names, spaces and all.
			for(String prefix: PREFIX_NOTEBOOK) {
				memoryIndex.digestEntirely(note, prefix + nbName);
			}
		}

		// date to sort by creation and last modified date
		memoryIndex.digestDate(note, meta.created());
		
		if (meta.created() < note.lastModified())
			memoryIndex.digestDate(note, note.lastModified());

		// Add the UUID for the note
		for(String prefix: PREFIX_UUID) {
			memoryIndex.digestEntirely(note, prefix + meta.getUUID());
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

	// Release Lucene write lock.
	public void commit() {
		if (useLucene && ready()) {
			luceneIndex.commit();
		}
	}

	@Subscribe
	public void handleNoteChanged(NoteChangedEvent event) throws Exception {
		try {
			purgeNote(event.note);
			digestNote(event.note, event.note.findContainingNotebook());
		} catch (Exception e) {
			LOG.severe("Fail: " + e);
			throw e;
		}

		commit();

		new SearchIndexChangedEvent().post();
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
		case noteDeleted:
			if (event.source != null) {
				Note oldNote = new Note(event.source);

				purgeNote(oldNote);
			}
			break;
		default:
			break;
		}
	}
}
