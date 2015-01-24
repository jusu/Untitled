package com.pinktwins.elephant;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridLayout;
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
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingWorker;

import com.google.common.eventbus.Subscribe;
import com.pinktwins.elephant.NoteItem.NoteItemListener;
import com.pinktwins.elephant.data.Note;
import com.pinktwins.elephant.data.Notebook;
import com.pinktwins.elephant.eventbus.NotebookEvent;
import com.pinktwins.elephant.eventbus.UIEvent;
import com.pinktwins.elephant.util.CustomMouseListener;
import com.pinktwins.elephant.util.Factory;
import com.pinktwins.elephant.util.Images;
import com.pinktwins.elephant.util.ResizeListener;

public class NoteList extends BackgroundPanel implements NoteItemListener {

	private static final Logger LOG = Logger.getLogger(NoteList.class.getName());

	private static Image tile, iAllNotes;

	private ElephantWindow window;

	private Notebook notebook;
	private NoteItem selectedNote;
	private Notebook previousNotebook;
	private int initialScrollValue;

	private List<NoteItem> noteItems = Factory.newArrayList();

	private ListController<NoteItem> lc = ListController.newInstance();

	private static int separatorLineY = 41;

	private JScrollPane scroll;
	private JPanel main, allNotesPanel, fillerPanel;
	private JLabel currentName;

	private final Workers<Point> workers = new Workers<Point>();
	private boolean isWorking = false;
	private final Trigger loadCancelTriggers = new Trigger();

	static {
		Iterator<Image> i = Images.iterator(new String[] { "notelist", "allNotes" });
		tile = i.next();
		iAllNotes = i.next();
	}

	public NoteList(ElephantWindow w) {
		super(tile);
		window = w;

		Elephant.eventBus.register(this);

		createComponents();
	}

	private void createComponents() {
		// title bar
		final JPanel title = new JPanel(new BorderLayout());
		title.setBorder(ElephantWindow.emptyBorder);

		final JButton allNotes = new JButton("");
		allNotes.setIcon(new ImageIcon(iAllNotes));
		allNotes.setBorderPainted(false);
		allNotes.setContentAreaFilled(false);
		allNotes.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				window.showAllNotes();
			}
		});

		allNotesPanel = new JPanel(new GridLayout(1, 1));
		allNotesPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		allNotesPanel.add(allNotes);

		fillerPanel = new JPanel(new GridLayout(1, 1));

		currentName = new JLabel("", JLabel.CENTER);
		currentName.setBorder(BorderFactory.createEmptyBorder(13, 0, 9, 0));
		currentName.setFont(ElephantWindow.fontTitle);
		currentName.setForeground(ElephantWindow.colorTitle);

		final JPanel sep = new JPanel(null);
		sep.setBounds(0, 0, 1920, 1);
		sep.setBackground(Color.decode("#cccccc"));

		title.add(allNotesPanel, BorderLayout.WEST);
		title.add(currentName, BorderLayout.CENTER);
		title.add(fillerPanel, BorderLayout.EAST);
		title.add(sep, BorderLayout.SOUTH);

		// main notes area
		main = new JPanel();
		main.setLayout(null);

		scroll = new JScrollPane(main);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setBorder(ElephantWindow.emptyBorder);
		scroll.getVerticalScrollBar().setUnitIncrement(5);

		add(title, BorderLayout.NORTH);
		add(scroll, BorderLayout.CENTER);

		main.addMouseListener(new CustomMouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (!e.isPopupTrigger()) {
					new UIEvent(UIEvent.Kind.editorWillChangeNote).post();
					window.onNoteListClicked(e);
				}
			}
		});

		main.addComponentListener(new ResizeListener() {
			@Override
			public void componentResized(ComponentEvent e) {
				layoutItems();

				fillerPanel.setPreferredSize(new Dimension(allNotesPanel.getWidth(), 10));
				fillerPanel.revalidate();

				separatorLineY = sep.getBounds().y;
			}
		});

		scroll.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
			@Override
			public void adjustmentValueChanged(AdjustmentEvent e) {
				// If we have reached the bottom of list, work more thumbs to screen
				if (!isWorking && workers.size() > 0) {
					JScrollBar v = scroll.getVerticalScrollBar();
					float f = (v.getValue() + v.getModel().getExtent()) / (float) v.getMaximum();
					if (Float.valueOf(f).equals(Float.valueOf(1.0f))) {
						isWorking = true;
						workers.next();
					}
				}
			}
		});

		currentName.addMouseListener(new CustomMouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {
				window.jumpToNotebookAction.actionPerformed(null);
			}
		});
	}

	public void cache(Notebook notebook) {
		for (Note n : notebook.getNotes()) {
			NoteItem.itemOf(n);
		}
	}

	public void load(Notebook notebook) {
		this.notebook = notebook;

		loadCancelTriggers.triggerAll();
		workers.clear();

		currentName.setText(notebook.name());

		main.removeAll();
		noteItems.clear();

		main.repaint();

		final List<Note> list = notebook.getNotes();

		final Trigger cancelTrigger = loadCancelTriggers.get();
		final int uiStep = 50;

		// First batch to screen NOW.
		// This could come from SwingWorker too, but doing it here avoids some flickering.
		for (int start = 0, end = Math.min(list.size(), uiStep); start < end; start++) {
			NoteItem item = NoteItem.itemOf(list.get(start));
			main.add(item);
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
						NoteItem.itemOf(list.get(n));
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
								NoteItem item = NoteItem.itemOf(list.get(n));
								main.add(item);
								noteItems.add(item);
							}

							initialScrollValue = scroll.getVerticalScrollBar().getValue();
							layoutItems();
							scroll.getVerticalScrollBar().revalidate();
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

		allNotesPanel.setVisible(!notebook.isAllNotes());
		fillerPanel.setVisible(!notebook.isAllNotes());

		if (notebook.equals(previousNotebook)) {
			initialScrollValue = scroll.getVerticalScrollBar().getValue();
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
		Insets insets = main.getInsets();
		Dimension size = new Dimension(192, 192);
		int x = 2; // 6?
		int y = 12;

		Rectangle mainBounds = main.getBounds();

		int itemAtRow = 0;
		int lastOffset = 0;
		for (NoteItem item : noteItems) {
			size = item.getPreferredSize();

			lc.itemsPerRow = mainBounds.width / size.width;
			int extra = mainBounds.width - (size.width * lc.itemsPerRow);
			extra /= 2;

			int linedX = x + insets.left + (itemAtRow * size.width);
			if (lc.itemsPerRow > 0) {
				int add = extra / lc.itemsPerRow;
				linedX += (itemAtRow + 1) * add;
			}

			item.setBounds(linedX, y + insets.top, size.width, size.height);

			if (itemAtRow < lc.itemsPerRow - 1) {
				itemAtRow++;
				lastOffset = size.height;
			} else {
				y += size.height;
				itemAtRow = 0;
				lastOffset = 0;
			}
		}

		Dimension d = main.getPreferredSize();
		d.height = y + 12 + lastOffset;
		main.setPreferredSize(d);

		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				scroll.getVerticalScrollBar().setValue(initialScrollValue);
			}
		});
	}

	@Override
	public void noteClicked(NoteItem item, boolean doubleClick) {
		if (doubleClick) {
			window.openNoteWindow(item.note);
		} else {
			new UIEvent(UIEvent.Kind.editorWillChangeNote).post();
			selectNote(item.note);
			window.showNote(item.note);
		}
	}

	private void selectNote(NoteItem item) {
		selectedNote = item;
		item.setSelected(true);

		lc.updateVerticalScrollbar(item, scroll);
	}

	public void changeSelection(int delta, int keyCode) {
		boolean sideways = keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_DOWN;

		NoteItem item = lc.changeSelection(noteItems, selectedNote, delta, sideways);
		if (item != null) {
			deselectAll();
			selectNote(item);
			window.showNote(item.note);
		}
	}

	private void deselectAll() {
		for (NoteItem i : noteItems) {
			i.setSelected(false);
		}
		selectedNote = null;
	}

	public void selectNote(Note n) {
		deselectAll();
		for (NoteItem item : noteItems) {
			if (item.note.equals(n)) {
				selectNote(item);
				return;
			}
		}
	}

	public void unfocusEditor() {
		if (selectedNote != null) {
			this.requestFocusInWindow();
			selectedNote.requestFocusInWindow();
		}
	}

	public void newNote() {
		try {
			Note newNote = notebook.newNote();
			load(notebook);
			selectNote(newNote);
			window.showNote(newNote);
		} catch (IOException e) {
			LOG.severe("Fail: " + e);
		}
	}

	public void deleteSelected() {
		if (selectedNote != null) {
			int index = noteItems.indexOf(selectedNote);
			notebook.deleteNote(selectedNote.note);
			load(notebook);

			if (index >= 0 && index < noteItems.size()) {
				NoteItem item = noteItems.get(index);
				window.showNote(item.note);
				selectNote(item);
			} else {
				selectedNote = null;
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
				noteItems.add(index, NoteItem.itemOf(note));
				return;
			}
		}
	}

	public void updateLoad() {
		Note n = null;
		if (selectedNote != null) {
			n = selectedNote.note;
		}

		load(notebook);

		if (n != null) {
			selectNote(n);
			if (selectedNote != null && !window.isEditorDirty()) {
				window.showNote(n);
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
		Point p = currentName.getLocationOnScreen();
		int x = (p.x + currentName.getWidth() / 2) - NotebookChooser.fixedWidthJump / 2;
		nbc.setBounds(x, p.y + currentName.getHeight(), NotebookChooser.fixedWidthJump, NotebookChooser.fixedHeight);

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
		return notebook.isAllNotes() || notebook.isTrash() || notebook.isSearch() || notebook.isTagSearch();
	}

	public boolean isSearch() {
		return notebook.isSearch();
	}

	public boolean isShowingNotebook(Notebook nb) {
		return notebook.equals(nb);
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
