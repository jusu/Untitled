package com.pinktwins.elephant;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.border.Border;

import com.google.common.eventbus.Subscribe;
import com.pinktwins.elephant.data.Note;
import com.pinktwins.elephant.data.NoteChangedEvent;
import com.pinktwins.elephant.data.Notebook;
import com.pinktwins.elephant.data.Vault;
import com.pinktwins.elephant.data.VaultEvent;

public class ElephantWindow extends JFrame {
	private static final long serialVersionUID = 1L;

	final public static Font fontTitle = Font.decode("Helvetica-BOLD-18");
	final public static Font fontH1 = Font.decode("Helvetica-BOLD-16");
	final public static Font fontSmall = Font.decode("Helvetica-10");
	final public static Font fontEditor = Font.decode("Arial-13");
	final public static Font fontBoldEditor = Font.decode("Arial-BOLD-13");
	final public static Font fontBoldNormal = Font.decode("Arial-BOLD-14");
	final public static Font fontNormal = Font.decode("Arial-14");
	final public static Font fontMedium = Font.decode("Arial-11");

	final public static Color colorTitle = Color.decode("#999999");
	final public static Color colorTitleButton = Color.decode("#666666");
	final public static Color colorGray5 = Color.decode("#555555");

	final public static Border emptyBorder = BorderFactory.createEmptyBorder(0, 0, 0, 0);

	CustomSplitPane splitLeft, splitRight;

	private Toolbar toolBar = new Toolbar(this);
	private Sidebar sideBar = new Sidebar(this);
	private NoteList noteList = new NoteList(this);
	private NoteEditor noteEditor = new NoteEditor(this);
	private Notebooks notebooks = new Notebooks(this);
	private Tags tags = new Tags(this);

	private boolean hasWindowFocus;

	enum UiModes {
		notebooks, notes, tags
	};

	UiModes uiMode;

	public ElephantWindow() {
		setTitle("Elephant Premium");

		setBounds(loadBounds());

		Elephant.eventBus.register(this);

		createMenu();
		createSplit();
		createToolbar();

		showNotebook(Vault.getInstance().getDefaultNotebook());

		KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
		manager.addKeyEventDispatcher(new KeyDispatcher());

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowActivated(WindowEvent e) {
				hasWindowFocus = true;
				toolBar.focusGained();
			}

			@Override
			public void windowDeactivated(WindowEvent e) {
				hasWindowFocus = false;
				toolBar.focusLost();
			}

			@Override
			public void windowClosed(WindowEvent e) {
				saveChanges();
			}
		});

		addComponentListener(new ComponentListener() {
			@Override
			public void componentResized(ComponentEvent e) {
				saveBounds(ElephantWindow.this.getBounds());
			}

			@Override
			public void componentMoved(ComponentEvent e) {
				saveBounds(ElephantWindow.this.getBounds());
			}

			@Override
			public void componentShown(ComponentEvent e) {
			}

			@Override
			public void componentHidden(ComponentEvent e) {
			}
		});
	}

	private Rectangle loadBounds() {
		Rectangle b = new Rectangle();
		int x = Elephant.settings.getInt("windowX");
		int y = Elephant.settings.getInt("windowY");
		int w = Elephant.settings.getInt("windowWidth");
		int h = Elephant.settings.getInt("windowHeight");
		b.x = x >= 0 ? x : 0;
		b.y = y >= 0 ? y : 22;
		b.width = w > 0 ? w : 1080;
		b.height = h > 0 ? h : 1050;
		return b;
	}

	private void saveBounds(Rectangle r) {
		Elephant.settings.setChain("windowX", r.x).setChain("windowY", r.y).setChain("windowWidth", r.width).set("windowHeight", r.height);
	}

	// XXX this will bite me eventually
	private class KeyDispatcher implements KeyEventDispatcher {
		@Override
		public boolean dispatchKeyEvent(KeyEvent e) {
			if (!hasWindowFocus) {
				return false;
			}

			switch (uiMode) {
			case notes:
				if (!noteEditor.hasFocus() && !toolBar.isEditing()) {
					switch (e.getID()) {
					case KeyEvent.KEY_PRESSED:
						switch (e.getKeyCode()) {
						case KeyEvent.VK_UP:
							noteList.changeSelection(-1, e.getKeyCode());
							break;
						case KeyEvent.VK_DOWN:
							noteList.changeSelection(1, e.getKeyCode());
							break;
						case KeyEvent.VK_LEFT:
							noteList.changeSelection(-1, e.getKeyCode());
							break;
						case KeyEvent.VK_RIGHT:
							noteList.changeSelection(1, e.getKeyCode());
							break;
						case KeyEvent.VK_BACK_SPACE:
							deleteSelectedNote();
						}
						break;
					}
				}
				break;
			case notebooks:
				if (!notebooks.isEditing() && !toolBar.isEditing()) {
					switch (e.getID()) {
					case KeyEvent.KEY_PRESSED:
						switch (e.getKeyCode()) {
						case KeyEvent.VK_UP:
						case KeyEvent.VK_LEFT:
							notebooks.changeSelection(-1, e.getKeyCode());
							break;
						case KeyEvent.VK_DOWN:
						case KeyEvent.VK_RIGHT:
							notebooks.changeSelection(1, e.getKeyCode());
							break;
						case KeyEvent.VK_ENTER:
							notebooks.openSelected();
							break;
						default:
							notebooks.handleKeyEvent(e);
						}
						break;
					}
				}
				break;
			}

			switch (e.getID()) {
			case KeyEvent.KEY_PRESSED:
				if (e.getKeyCode() >= KeyEvent.VK_1 && e.getKeyCode() <= KeyEvent.VK_9) {
					if ((e.getModifiers() & KeyEvent.META_MASK) == KeyEvent.META_MASK && (e.getModifiers() & KeyEvent.ALT_MASK) == 0) {
						String target = sideBar.shortcuts.getTarget(e.getKeyCode() - KeyEvent.VK_1);
						toolBar.clearSearch();
						openShortcut(target);
					}
				}
				break;
			}

			return false;
		}
	}

	public void openShortcut(String target) {
		if (Sidebar.ACTION_NOTES.equals(target)) {
			showNotes();
			return;
		}

		if (Sidebar.ACTION_NOTEBOOKS.equals(target)) {
			showNotebooks();
			return;
		}

		if (Sidebar.ACTION_TAGS.equals(target)) {
			showTags();
			return;
		}

		File f = new File(target);
		if (f.exists()) {

			saveChanges();

			if (f.isDirectory()) {
				Notebook notebook = Vault.getInstance().findNotebook(f);
				if (notebook != null) {
					showNotebook(notebook);
				}
			} else {
				File folder = f.getParentFile();
				Notebook notebook = Vault.getInstance().findNotebook(folder);
				if (notebook != null) {
					Note note = notebook.find(f.getName());
					if (note != null) {
						showNotebook(notebook);
						noteList.selectNote(note);
						showNote(note);
					}
				}
			}
		}

	}

	public void saveChanges() {
		noteEditor.saveChanges();
	}

	public void deleteSelectedNote() {
		noteEditor.clear();
		noteList.deleteSelected();
	}

	private void showNotes() {
		splitLeft.setRightComponent(splitRight);
		uiMode = UiModes.notes;
		sideBar.selectNavigation(0);
	}

	private void showNotebooks() {
		splitLeft.setRightComponent(notebooks);
		uiMode = UiModes.notebooks;
		sideBar.selectNavigation(1);
	}

	private void showTags() {
		splitLeft.setRightComponent(tags);
		uiMode = UiModes.tags;
		sideBar.selectNavigation(2);
	}

	public void showNotebook(Notebook notebook) {
		showNotes();
		noteEditor.clear();
		noteList.load(notebook);
		noteList.changeSelection(1, 0);

		// DO I NEED THIS OR NOT?
		// noteList.unfocusEditor();
	}

	public void showNote(Note note) {
		showNotes();
		noteEditor.clear();
		noteEditor.load(note);
	}

	public void showAllNotes() {
		Notebook nb = Notebook.getNotebookWithAllNotes();
		showNotebook(nb);
	}

	public void focusEditor() {
		noteEditor.focusTitle();
	}

	public void unfocusEditor() {
		noteList.unfocusEditor();
	}

	public void newNote() {
		noteList.newNote();
		focusEditor();
	}

	ActionListener newNoteAction = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			// XXX if showing notebooks, open new note in solo window
			showNotes();
			newNote();
		}
	};

	ActionListener saveNoteAction = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			unfocusEditor();
		}
	};

	ActionListener searchAction = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			toolBar.focusSearch();
		}
	};

	ActionListener newNotebookAction = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			showNotebooks();
			notebooks.newNotebook();
		}
	};

	ActionListener newWindowAction = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			newWindow();
		}
	};

	ActionListener showNotesAction = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			showNotes();
		}
	};

	ActionListener showNotebooksAction = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			showNotebooks();
		}
	};

	ActionListener showTagsAction = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			showTags();
		}
	};

	ActionListener showAllNotesAction = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			showAllNotes();
		}
	};

	private void createMenu() {
		JMenuBar mb = new JMenuBar();
		JMenu file = new JMenu("File");

		JMenuItem newNote = new JMenuItem("New Note");
		newNote.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.META_MASK));
		newNote.addActionListener(newNoteAction);
		file.add(newNote);

		JMenuItem newNotebook = new JMenuItem("New Notebook");
		newNotebook.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.META_MASK | ActionEvent.SHIFT_MASK));
		newNotebook.addActionListener(newNotebookAction);
		file.add(newNotebook);

		JMenuItem newWindow = new JMenuItem("New Elephant Window");
		newWindow.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.META_MASK | ActionEvent.ALT_MASK));
		newWindow.addActionListener(newWindowAction);
		file.add(newWindow);

		file.addSeparator();

		JMenuItem saveNote = new JMenuItem("Save");
		saveNote.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.META_MASK));
		saveNote.addActionListener(saveNoteAction);
		file.add(saveNote);

		JMenu edit = new JMenu("Edit");

		JMenuItem iSearch = new JMenuItem("Search Notes...");
		iSearch.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, ActionEvent.META_MASK | ActionEvent.ALT_MASK));
		iSearch.addActionListener(searchAction);
		edit.add(iSearch);

		JMenu view = new JMenu("View");

		JMenuItem iNotes = new JMenuItem("Notes");
		iNotes.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_2, ActionEvent.META_MASK | ActionEvent.ALT_MASK));
		iNotes.addActionListener(showNotesAction);
		view.add(iNotes);

		JMenuItem iNotebooks = new JMenuItem("Notebooks");
		iNotebooks.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_3, ActionEvent.META_MASK | ActionEvent.ALT_MASK));
		iNotebooks.addActionListener(showNotebooksAction);
		view.add(iNotebooks);

		JMenuItem iTags = new JMenuItem("Tags");
		iTags.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_4, ActionEvent.META_MASK | ActionEvent.ALT_MASK));
		iTags.addActionListener(showTagsAction);
		view.add(iTags);

		view.addSeparator();

		JMenuItem iAll = new JMenuItem("Show All Notes");
		iAll.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, ActionEvent.META_MASK | ActionEvent.SHIFT_MASK));
		iAll.addActionListener(showAllNotesAction);
		view.add(iAll);

		mb.add(file);
		mb.add(edit);
		mb.add(view);

		setJMenuBar(mb);
	}

	protected void newWindow() {
		ElephantWindow w = new ElephantWindow();
		w.setVisible(true);
	}

	private void createSplit() {
		splitLeft = new CustomSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		splitLeft.setResizeWeight(0.2);
		splitLeft.setContinuousLayout(true);
		splitLeft.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		splitLeft.initLocationWithKey("divider1");
		splitLeft.limitLocation(250);

		splitRight = new CustomSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		splitRight.setResizeWeight(0.5);
		splitRight.setContinuousLayout(true);
		splitRight.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		splitRight.initLocationWithKey("divider2");

		splitLeft.setLeftComponent(sideBar);
		splitLeft.setRightComponent(splitRight);

		splitRight.setLeftComponent(noteList);
		splitRight.setRightComponent(noteEditor);

		add(splitLeft, BorderLayout.CENTER);
	}

	private void createToolbar() {
		toolBar.setPreferredSize(new Dimension(1920, 40));
		add(toolBar, BorderLayout.NORTH);
	}

	public void onNoteListClicked(MouseEvent e) {
		noteEditor.unfocus();
	}

	@Subscribe
	public void handleNoteChanged(NoteChangedEvent event) {
		noteList.updateThumb(event.note);
	}

	public void sortAndUpdate() {
		noteList.sortAndUpdate();
	}

	@Subscribe
	public void handleVaultEvent(VaultEvent event) {
		if (event.kind == VaultEvent.Kind.notebookListChanged) {
			notebooks.refresh();
		}
	}

	public void search(String text) {
		if (text.length() == 0) {
			showNotebook(Vault.getInstance().getDefaultNotebook());
		} else {
			showNotebook(Vault.getInstance().search(text));
		}
	}
}

// laku 4ever ;,,,,(

