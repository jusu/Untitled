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
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;

import org.pushingpixels.trident.Timeline;

import com.pinktwins.elephant.Notebooks.NotebookActionListener;
import com.pinktwins.elephant.data.Note;
import com.pinktwins.elephant.data.Notebook;

public class NoteList extends BackgroundPanel {

	private static final long serialVersionUID = 5649274177360148568L;
	private static Image tile, noteShadow, noteSelection;

	private ElephantWindow window;
	final private Color kColorNoteBorder = Color.decode("#cdcdcd");

	private Notebook notebook;
	private NoteItem selectedNote;
	private Notebook previousNotebook;
	private int initialScrollValue;

	private ArrayList<NoteItem> noteItems = new ArrayList<NoteItem>();

	static {
		try {
			tile = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/notelist.png"));
			noteShadow = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/noteShadow.png"));
			noteSelection = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/noteSelection.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public NoteList(ElephantWindow w) {
		super(tile);
		window = w;
		createComponents();
	}

	JScrollPane scroll;
	JPanel main, allNotesPanel, fillerPanel;
	JLabel currentName;

	private void createComponents() {
		// title bar
		final JPanel title = new JPanel(new BorderLayout());

		final JButton allNotes = new JButton("All Notes");
		allNotes.setForeground(ElephantWindow.colorTitleButton);
		allNotes.setBorderPainted(true);
		allNotes.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				window.showAllNotes();
			}
		});

		allNotesPanel = new JPanel();
		allNotesPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		allNotesPanel.add(allNotes);

		fillerPanel = new JPanel();
		JButton filler = new JButton("               ");
		filler.setContentAreaFilled(false);
		filler.setBorderPainted(false);
		fillerPanel.add(filler);

		currentName = new JLabel("", JLabel.CENTER);
		currentName.setBorder(BorderFactory.createEmptyBorder(13, 0, 9, 0));
		currentName.setFont(ElephantWindow.fontTitle);
		currentName.setForeground(ElephantWindow.colorTitle);

		JPanel sep = new JPanel(null);
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
				Elephant.eventBus.post(new UIEvent(UIEvent.Kind.editorWillChangeNote));
				window.onNoteListClicked(e);
			}
		});

		main.addComponentListener(new ResizeListener() {
			@Override
			public void componentResized(ComponentEvent e) {
				layoutItems();
			}
		});

		currentName.addMouseListener(new CustomMouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {
				window.jumpToNotebookAction.actionPerformed(null);
			}
		});
	}

	public void load(Notebook notebook) {
		this.notebook = notebook;

		currentName.setText(notebook.name());

		main.removeAll();
		noteItems.clear();

		main.repaint();

		List<Note> list = notebook.getNotes();
		for (Note n : list) {
			NoteItem item = new NoteItem(n);
			main.add(item);
			noteItems.add(item);
		}

		allNotesPanel.setVisible(!notebook.isAllNotes());
		fillerPanel.setVisible(!notebook.isAllNotes());

		if (notebook.equals(previousNotebook)) {
			initialScrollValue = scroll.getVerticalScrollBar().getValue();
		} else {
			initialScrollValue = 0;
		}

		layoutItems();
		// revalidate();

		previousNotebook = notebook;
	}

	int itemsPerRow;

	private void layoutItems() {
		Insets insets = main.getInsets();
		Dimension size = new Dimension(192, 192);
		int x = 6;
		int y = 12;

		Rectangle mainBounds = main.getBounds();

		int itemAtRow = 0;
		int lastOffset = 0;
		for (NoteItem item : noteItems) {
			size = item.getPreferredSize();

			itemsPerRow = mainBounds.width / size.width;
			int extra = mainBounds.width - (size.width * itemsPerRow);
			extra /= 2;

			int linedX = x + insets.left + (itemAtRow * size.width);
			if (itemsPerRow > 0) {
				int add = extra / itemsPerRow;
				linedX += (itemAtRow + 1) * add;
			}

			item.setBounds(linedX, y + insets.top, size.width, size.height);

			if (itemAtRow < itemsPerRow - 1) {
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

	private void selectNote(NoteItem item) {
		selectedNote = item;
		item.setSelected(true);

		Rectangle b = item.getBounds();
		int itemY = b.y;
		int y = scroll.getVerticalScrollBar().getValue();
		int scrollHeight = scroll.getBounds().height;

		if (itemY < y || itemY + b.height >= y + scrollHeight) {

			if (itemY < y) {
				itemY -= 12;
			} else {
				itemY -= scrollHeight - b.height - 12;
			}

			JScrollBar bar = scroll.getVerticalScrollBar();
			Timeline timeline = new Timeline(bar);
			timeline.addPropertyToInterpolate("value", bar.getValue(), itemY);
			timeline.setDuration(100);
			timeline.play();
		}
	}

	public void changeSelection(int delta, int keyCode) {
		int len = noteItems.size();
		int select = -1;

		if (keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_DOWN) {
			delta *= itemsPerRow;
		}

		if (selectedNote == null) {
			if (len > 0) {
				if (delta < 0) {
					select = len - 1;
				} else {
					select = 0;
				}
			}
		} else {
			int currentIndex = noteItems.indexOf(selectedNote);
			select = currentIndex + delta;
		}

		if (select >= 0 && select < len) {
			deselectAll();

			NoteItem ni = noteItems.get(select);
			selectNote(ni);
			window.showNote(ni.note);
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
			if (item.note == n) {
				selectNote(item);
				return;
			}
		}
	}

	class NoteItem extends JPanel implements MouseListener {

		private static final long serialVersionUID = -4080651728730225105L;
		private Note note;
		private Dimension size = new Dimension(196, 196);
		private JLabel name;
		private JTextArea preview;
		private BackgroundPanel root;

		public NoteItem(Note n) {
			super();
			note = n;

			setLayout(new BorderLayout());

			root = new BackgroundPanel(noteShadow, 2);
			root.setBorder(BorderFactory.createEmptyBorder(7, 7, 7, 7));
			root.setMinimumSize(size);
			root.setMaximumSize(size);

			JPanel p = new JPanel();
			p.setLayout(new BorderLayout());
			p.setBackground(Color.WHITE);
			p.setBorder(BorderFactory.createLineBorder(kColorNoteBorder, 1));

			name = new JLabel(n.getMeta().title());
			name.setFont(ElephantWindow.fontH1);
			name.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
			p.add(name, BorderLayout.NORTH);

			JPanel previewPane = new JPanel();
			previewPane.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));
			previewPane.setLayout(new GridLayout(1, 1));
			previewPane.setBackground(Color.WHITE);

			preview = new JTextArea();
			preview.setEditable(false);
			preview.setFont(ElephantWindow.fontSmall);
			preview.setText(getContentPreview());
			preview.setBackground(Color.WHITE);

			previewPane.add(preview);

			p.add(previewPane, BorderLayout.CENTER);

			root.addOpaque(p, BorderLayout.CENTER);
			add(root, BorderLayout.CENTER);

			p.addMouseListener(this);
			preview.addMouseListener(this);
		}

		private String getContentPreview() {
			String contents = note.contents();
			if (contents.length() > 200) {
				contents = contents.substring(0, 200) + "…";
			}
			return contents;
		}

		public void updateThumb() {
			name.setText(note.getMeta().title());
			preview.setText(getContentPreview());
		}

		public void setSelected(boolean b) {
			if (b) {
				root.setImage(noteSelection);
			} else {
				root.setImage(noteShadow);
			}
			repaint();
		}

		@Override
		public Dimension getPreferredSize() {
			return size;
		}

		@Override
		public Dimension getMinimumSize() {
			return size;
		}

		@Override
		public Dimension getMaximumSize() {
			return size;
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			if (e.getClickCount() == 2) {
				// XXX open note in new window
			}
		}

		@Override
		public void mousePressed(MouseEvent e) {
			Elephant.eventBus.post(new UIEvent(UIEvent.Kind.editorWillChangeNote));
			selectNote(NoteItem.this.note);
			window.showNote(note);
			unfocusEditor();
		}

		@Override
		public void mouseReleased(MouseEvent e) {
		}

		@Override
		public void mouseEntered(MouseEvent e) {
		}

		@Override
		public void mouseExited(MouseEvent e) {
		}
	}

	public void unfocusEditor() {
		if (selectedNote != null) {
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
			e.printStackTrace();
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
			if (item.note == note) {
				item.updateThumb();
				return;
			}
		}
	}

	public void sortAndUpdate() {
		notebook.sortNotes();

		// XXX animate position changes to notes

		Note n = null;
		if (selectedNote != null) {
			n = selectedNote.note;
		}

		load(notebook);

		if (n != null) {
			selectNote(n);
		}
	}

	public void openNotebookChooserForJumping() {
		NotebookChooser nbc = new NotebookChooser(window, "");

		// Center on window
		Point p = currentName.getLocationOnScreen();
		Rectangle r = window.getBounds();
		int x = (p.x + currentName.getWidth() / 2) - NotebookChooser.fixedWidth / 2;
		nbc.setBounds(x, p.y + currentName.getHeight(), NotebookChooser.fixedWidth, NotebookChooser.fixedHeight);

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

}
