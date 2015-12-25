package com.pinktwins.elephant;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import javax.swing.JScrollBar;
import javax.swing.SwingWorker;

import com.google.common.eventbus.Subscribe;
import com.pinktwins.elephant.NoteItem.NoteItemListener;
import com.pinktwins.elephant.data.Note;
import com.pinktwins.elephant.data.Notebook;
import com.pinktwins.elephant.data.Settings;
import com.pinktwins.elephant.eventbus.NotebookEvent;
import com.pinktwins.elephant.eventbus.UIEvent;
import com.pinktwins.elephant.util.CustomMouseListener;
import com.pinktwins.elephant.util.Factory;
import com.pinktwins.elephant.util.Images;
import com.pinktwins.elephant.util.ResizeListener;

public class NoteList extends BackgroundPanel implements NoteItemListener {

	private static final Logger LOG = Logger.getLogger(NoteList.class.getName());

	private static final int SNIPPETVIEW_ITEMHEIGHT = 86;

	private static Image tile;

	private ElephantWindow window;

	private Notebook notebook;
	private List<NoteItem> noteItems = Factory.newArrayList();
	private SortedSet<NoteItem> selectedNotes = Factory.newSortedSet();

	private ListController<NoteItem> lc = ListController.newInstance();

	private Notebook previousNotebook;
	private int initialScrollValue;
	private static int separatorLineY = 41;

	private final Workers<Point> workers = new Workers<Point>();
	private boolean isWorking = false;
	private final Trigger loadCancelTriggers = new Trigger();

	private NoteListUI ui;

	static {
		Iterator<Image> i = Images.iterator(new String[] { "notelist" });
		tile = i.next();
	}

	public static enum ListModes {
		CARDVIEW, SNIPPETVIEW
	};

	private ListModes listMode = ListModes.CARDVIEW;

	public NoteList(ElephantWindow w) {
		super(tile);
		window = w;

		Elephant.eventBus.register(this);

		listMode = Elephant.settings.getNoteListMode();

		createComponents();
	}

	private void createComponents() {

		ui = new NoteListUI(this);

		ui.allNotes.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				window.showAllNotes();
			}
		});

		ui.main.addMouseListener(new CustomMouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (!e.isPopupTrigger()) {
					new UIEvent(UIEvent.Kind.editorWillChangeNote).post();
					window.onNoteListClicked(e);
				}
			}
		});

		ui.main.addComponentListener(new ResizeListener() {
			@Override
			public void componentResized(ComponentEvent e) {
				// added in v17 - layout need not jump to initial value on resize?
				initialScrollValue = ui.scroll.getVerticalScrollBar().getValue();

				layoutItems();

				ui.fillerPanel.setPreferredSize(new Dimension(ui.allNotesPanel.getWidth(), 10));
				ui.fillerPanel.revalidate();

				separatorLineY = ui.sep.getBounds().y;
			}
		});

		ui.scroll.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
			@Override
			public void adjustmentValueChanged(AdjustmentEvent e) {
				// If we have reached the bottom of list, work more thumbs to screen
				if (!isWorking && !workers.isEmpty()) {
					JScrollBar v = ui.scroll.getVerticalScrollBar();
					float f = (v.getValue() + v.getModel().getExtent()) / (float) v.getMaximum();
					if (Float.valueOf(f).equals(Float.valueOf(1.0f))) {
						isWorking = true;
						workers.next();
					}
				}
			}
		});

		ui.currentName.addMouseListener(new CustomMouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {
				window.jumpToNotebookAction.actionPerformed(null);
			}
		});
	}

	public void cache(Notebook notebook) {
		for (Note n : notebook.getNotes()) {
			NoteItem.itemOf(n, listMode);
		}
	}

	public Notebook currentNotebook() {
		return notebook;
	}

	public void load(Notebook notebook) {
		this.notebook = notebook;

		loadCancelTriggers.triggerAll();
		workers.clear();

		ui.currentName.setText(notebook.name());

		ui.main.removeAll();
		noteItems.clear();

		ui.main.repaint();

		final List<Note> list = notebook.getNotes();

		final Trigger cancelTrigger = loadCancelTriggers.get();
		final int uiStep = 50;

		// First batch to screen NOW.
		// This could come from SwingWorker too, but doing it here avoids some flickering.
		for (int start = 0, end = Math.min(list.size(), uiStep); start < end; start++) {
			NoteItem item = NoteItem.itemOf(list.get(start), listMode);
			ui.main.add(item);
			noteItems.add(item);
		}

		// Paging for remaining notes.
		for (int start = uiStep, end = list.size(); start < end; start += uiStep) {

			final Point range = new Point(start, Math.min(start + uiStep, list.size()));

			workers.add(new SwingWorker<Point, Void>() {
				@Override
				protected Point doInBackground() throws Exception {
					for (int n = range.x, len = range.y; n < len; n++) {
						if (cancelTrigger.isDown) {
							return null;
						}
						NoteItem.itemOf(list.get(n), listMode);
					}
					return range;
				}

				@Override
				protected void done() {
					try {
						Point range = get();
						if (range != null) {
							for (int n = range.x, len = range.y; n < len; n++) {
								if (cancelTrigger.isDown) {
									return;
								}
								NoteItem item = NoteItem.itemOf(list.get(n), listMode);
								ui.main.add(item);
								noteItems.add(item);
							}

							initialScrollValue = ui.scroll.getVerticalScrollBar().getValue();
							layoutItems();
							ui.scroll.getVerticalScrollBar().revalidate();
						}
					} catch (ExecutionException e) {
						LOG.severe("Fail: " + e);
					} catch (InterruptedException e) {
						LOG.severe("Fail: " + e);
					} finally {
						isWorking = false;
					}
				}
			});
		}

		ui.allNotesPanel.setVisible(!notebook.isAllNotes());
		ui.fillerPanel.setVisible(!notebook.isAllNotes());

		if (notebook.equals(previousNotebook)) {
			initialScrollValue = ui.scroll.getVerticalScrollBar().getValue();
		} else {
			initialScrollValue = 0;
		}

		layoutItems();

		previousNotebook = notebook;
	}

	public static int separatorLineY() {
		return separatorLineY;
	}

	private void layoutItems() {
		Insets insets = ui.main.getInsets();
		Dimension size = new Dimension(192, 192);

		int x = 0, y = 0;

		switch (listMode) {
		case CARDVIEW:
			x = 2;
			y = 12;
			break;
		case SNIPPETVIEW:
			x = 0;
			y = 0;
			break;
		}

		Rectangle mainBounds = ui.main.getBounds();

		int itemAtRow = 0;
		int lastOffset = 0;
		for (NoteItem item : noteItems) {
			size = item.getPreferredSize();

			int linedX = 0;
			int itemWidth = size.width;
			int itemHeight = size.height;

			switch (listMode) {
			case CARDVIEW:
				lc.itemsPerRow = mainBounds.width / size.width;

				int extra = mainBounds.width - (size.width * lc.itemsPerRow);
				extra /= 2;

				linedX = x + insets.left + (itemAtRow * size.width);
				if (lc.itemsPerRow > 0) {
					int add = extra / lc.itemsPerRow;
					linedX += (itemAtRow + 1) * add;
				}
				break;
			case SNIPPETVIEW:
				lc.itemsPerRow = 1;
				itemWidth = mainBounds.width;
				itemHeight = SNIPPETVIEW_ITEMHEIGHT;
				break;
			}

			item.setBounds(linedX, y + insets.top, itemWidth, itemHeight);

			if (itemAtRow < lc.itemsPerRow - 1) {
				itemAtRow++;
				lastOffset = size.height;
			} else {
				y += itemHeight;
				itemAtRow = 0;
				lastOffset = 0;
			}
		}

		Dimension d = ui.main.getPreferredSize();
		d.height = y + 12 + lastOffset;
		ui.main.setPreferredSize(d);

		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				ui.scroll.getVerticalScrollBar().setValue(initialScrollValue);
				NoteList.this.repaint();
			}
		});
	}

	@Override
	public void noteClicked(final NoteItem item, boolean doubleClick, MouseEvent e) {
		if (e.isPopupTrigger()) {
			// JPopupMenu is buggy.
			return;
		}

		if (doubleClick) {
			window.openNoteWindow(item.note);
		} else {
			new UIEvent(UIEvent.Kind.editorWillChangeNote).post();

			boolean addToSelection = e.isMetaDown() || e.isControlDown() || e.isShiftDown();

			if (addToSelection && item.isSelected() && selectedNotes.size() > 1) {
				deselectNote(item);
			} else {
				selectNote(item.note, addToSelection);
			}

			if (e.isShiftDown()) {
				// Select range between clicked note and notes above/below
				int itemIndex = noteItems.indexOf(item);
				int index = -1;
				for (int n = itemIndex - 1; n >= 0; n--) {
					NoteItem ni = noteItems.get(n);
					if (ni.isSelected()) {
						index = n;
						break;
					}
				}
				if (index == -1) {
					for (int n = itemIndex + 1; n < noteItems.size(); n++) {
						NoteItem ni = noteItems.get(n);
						if (ni.isSelected()) {
							index = n;
							break;
						}
					}
				}
				if (index > -1) {
					int min = Math.min(index, itemIndex), max = Math.max(index, itemIndex);
					for (int n = Math.max(min, 0); n <= max; n++) {
						selectNote(noteItems.get(n), true);
					}
				}
			}

			showSelectedNote();
		}
	}

	private void selectNote(final NoteItem item, final boolean addToSelection) {
		if (!addToSelection) {
			deselectAll();
		}

		selectedNotes.add(item);
		item.setSelected(true);

		lc.updateVerticalScrollbar(item, ui.scroll);
	}

	private void deselectNote(final NoteItem item) {
		item.setSelected(false);
		selectedNotes.remove(item);
	}

	public void changeSelection(int delta, KeyEvent event) {
		int keyCode = 0;
		boolean addToSelection = false;

		if (event != null) {
			keyCode = event.getKeyCode();
			addToSelection = event.isShiftDown();

		}

		boolean sideways = keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_DOWN;

		// Get first or last selected note, depending on key pressed
		NoteItem selected = null;
		if (!selectedNotes.isEmpty()) {
			selected = (keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_LEFT) ? selectedNotes.first() : selectedNotes.last();
		}

		NoteItem item = lc.changeSelection(noteItems, selected, delta, sideways);
		if (item != null) {
			if (!addToSelection) {
				deselectAll();
			}

			selectNote(item, addToSelection);

			// Select all notes between previous and new selection when shift pressed
			if (sideways && addToSelection && selected != null) {
				int from = noteItems.indexOf(selected), to = noteItems.indexOf(item);
				int min = Math.min(from, to), max = Math.max(from, to);

				for (int n = min + 1; n < max; n++) {
					selectNote(noteItems.get(n), addToSelection);
				}
			}

			showSelectedNote();
		}
	}

	private void showSelectedNote() {
		if (selectedNotes.isEmpty()) {
			throw new AssertionError();
		}

		if (selectedNotes.size() == 1) {
			window.showNote(selectedNotes.first().note);
		} else {
			window.showMultipleNotes();
		}
	}

	private void deselectAll() {
		for (NoteItem i : noteItems) {
			i.setSelected(false);
		}
		selectedNotes.clear();
	}

	public void selectNote(final Note n, final boolean addToSelection) {
		for (NoteItem item : noteItems) {
			if (item.note.equals(n)) {
				selectNote(item, addToSelection);
				return;
			}
		}
	}

	public Note selectNote(int index) {
		if (index >= 0 && index < noteItems.size()) {
			NoteItem item = noteItems.get(index);
			selectNote(item, false);
			return item.note;
		}

		return null;
	}

	public void unfocusEditor() {
		if (!selectedNotes.isEmpty()) {
			this.requestFocusInWindow();
			selectedNotes.first().requestFocusInWindow();
		}
	}

	public void newNote() {
		try {
			Note newNote = notebook.newNote();
			load(notebook);
			selectNote(newNote, false);
			window.showNote(newNote);
		} catch (IOException e) {
			LOG.severe("Fail: " + e);
		}
	}

	public void deleteSelected() {
		if (!selectedNotes.isEmpty()) {
			int index = noteItems.indexOf(selectedNotes.first());

			Iterator<NoteItem> i = new TreeSet<NoteItem>(selectedNotes).iterator();
			while (i.hasNext()) {
				NoteItem item = i.next();
				notebook.deleteNote(item.note);
			}

			load(notebook);

			if (index >= 0 && index < noteItems.size()) {
				NoteItem item = noteItems.get(index);
				window.showNote(item.note);
				selectNote(item, false);
			} else {
				selectedNotes.clear();
				window.showNotebook(notebook);
			}
		}
	}

	public void updateThumb(Note note) {
		for (NoteItem item : noteItems) {
			if (item.note.equals(note)) {
				int index = noteItems.indexOf(item);
				noteItems.remove(item);

				NoteItem.removeCacheKey(note.file());
				noteItems.add(index, NoteItem.itemOf(note, listMode));
				return;
			}
		}
	}

	public void updateLoad() {
		Note n = null;
		if (!selectedNotes.isEmpty()) {
			n = selectedNotes.first().note;
		}

		load(notebook);

		if (n != null) {
			selectNote(n, false);
			if (!selectedNotes.isEmpty() && !window.isEditorDirty()) {
				if (window.uiMode == ElephantWindow.UiModes.notes) {
					window.showNote(n);
				} else {
					window.refreshNote(n);
				}
			}
		}
	}

	public void sortAndUpdate() {
		notebook.refresh();
		updateLoad();
	}

	public void openNotebookChooserForJumping() {
		NotebookChooser nbc = new NotebookChooser(window, "");

		// Center on window
		Point p = ui.currentName.getLocationOnScreen();
		int x = (p.x + ui.currentName.getWidth() / 2) - NotebookChooser.fixedWidthJump / 2;
		nbc.setBounds(x, p.y + ui.currentName.getHeight(), NotebookChooser.fixedWidthJump, NotebookChooser.fixedHeight);

		nbc.setVisible(true);

		nbc.setNotebookActionListener(new NotebookActionListener() {
			@Override
			public void didCancelSelection() {
			}

			@Override
			public void didSelect(Notebook nb) {
				window.showNotebook(nb);
			}
		});
	}

	public boolean isDynamicallyCreatedNotebook() {
		return notebook.isDynamicallyCreatedNotebook();
	}

	public boolean isSearch() {
		return notebook.isSearch();
	}

	public boolean isShowingNotebook(final Notebook nb) {
		return notebook.equals(nb);
	}

	public Set<Note> getSelection() {
		Set<Note> sel = Factory.newHashSet();
		for (NoteItem item : selectedNotes) {
			sel.add(item.note);
		}
		return sel;
	}

	public int getIndexOfFirstSelectedNote() {
		return selectedNotes.isEmpty() ? -1 : noteItems.indexOf(selectedNotes.first());
	}

	public void changeMode(ListModes newMode) {
		listMode = newMode;
		NoteItem.clearItemCache();
		updateLoad();

		Elephant.settings.set(Settings.Keys.NOTELIST_MODE, newMode.toString());
	}

	@Subscribe
	public void handleNotebookEvent(NotebookEvent event) {
		switch (event.kind) {
		case noteCreated:
			break;
		case noteMoved:
			NoteItem.removeCacheKey(event.source);
			break;
		case noteRenamed:
			NoteItem.removeCacheKey(event.source);
			if (isShowingNotebook(Note.findContainingNotebook(event.dest))) {
				sortAndUpdate();
			}
			break;
		default:
			break;
		}
	}
}
