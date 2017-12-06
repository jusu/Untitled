package com.pinktwins.elephant;

import java.util.List;

import com.pinktwins.elephant.data.Note;
import com.pinktwins.elephant.eventbus.UIEvent;
import com.pinktwins.elephant.util.Factory;

public class History {

	private final List<HistoryItem> items = Factory.newArrayList();
	private int index = 0;
	private static final int MAX_HISTORY_SIZE = 20;

	// When going back/forward in history, prevent adding same items
	private boolean preventAdd = false;
	private boolean freezeState;

	private int indexMark = 0;

	private ElephantWindow window;

	public History(ElephantWindow w) {
		window = w;
	}

	public void cleanup() {
		window = null;
	}

	private static enum ItemType {
		Note, Notebooks, Tags, AllNotes, Search;
	};

	private class HistoryItem {
		private ItemType type;
		private Note note;
		private String search;

		public HistoryItem(ItemType t) {
			type = t;
		}

		public HistoryItem(Note n) {
			type = ItemType.Note;
			note = n;
		}

		public HistoryItem(String s) {
			type = ItemType.Search;
			this.search = s;
		}
	}

	private void addItem(final HistoryItem i) {
		if (!preventAdd) {
			while (items.size() - 1 > index) {
				items.remove(items.size() - 1);
			}

			items.add(i);

			while (items.size() > MAX_HISTORY_SIZE) {
				items.remove(0);
			}

			index = items.size() - 1;
		}
	}

	public void add(final Note n) {
		if (items.size() > 0) {
			Note last = items.get(items.size() - 1).note;
			if (n.equals(last)) {
				return;
			}
		}

		addItem(new HistoryItem(n));
	}

	public void addNotebooks() {
		addItem(new HistoryItem(ItemType.Notebooks));
	}

	public void addTags() {
		addItem(new HistoryItem(ItemType.Tags));
	}

	public void addAllNotes() {
		addItem(new HistoryItem(ItemType.AllNotes));
	}

	public void addSearch(String s) {
		addItem(new HistoryItem(s));
	}

	public void back() {
		if (index > 0) {
			index--;
			showIndex(true);
		}
	}

	public void forward() {
		if (index < items.size() - 1) {
			index++;
			showIndex(true);
		}
	}

	private void showIndex(final boolean shouldClearSearch) {
		HistoryItem i = items.get(index);

		preventAdd = true;

		new UIEvent(UIEvent.Kind.editorWillChangeNote).post();

		switch (i.type) {
		case AllNotes:
			window.showAllNotes();
			break;
		case Note:
			window.selectAndShowNote(i.note.findContainingNotebook(), i.note, shouldClearSearch);
			break;
		case Notebooks:
			window.showNotebooks();
			break;
		case Search:
			window.setSearchText(i.search);
			window.search(i.search);
			break;
		case Tags:
			window.showTags();
			break;
		default:
			break;
		}

		preventAdd = false;
	}

	public void rewindSearch() {
		for (; index > 0; index--) {
			HistoryItem i = items.get(index);
			if (i.type != ItemType.Search) {
				break;
			}
		}
		showIndex(false);
	}

	public void freeze() {
		freezeState = preventAdd;
		preventAdd = true;
	}

	public void unFreeze() {
		preventAdd = freezeState;
	}

	public void clear() {
		items.clear();
		index = 0;
	}

	public int size() {
		return items.size();
	}

	public void setMark() {
		indexMark = index;
	}

	public void rewindToMark() {
		index = indexMark;
		rewindSearch();
		//showIndex(false);
	}
}
