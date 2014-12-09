package com.pinktwins.elephant;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
	final public static Font fontBoldNormal = Font.decode("Helvetica-BOLD-14");

	final public static Color colorTitle = Color.decode("#999999");
	
	final public static Border emptyBorder = BorderFactory.createEmptyBorder(0, 0, 0, 0);

	JSplitPane splitLeft, splitRight;

	private Toolbar toolBar = new Toolbar(this);
	private Sidebar sideBar = new Sidebar(this, "SHORTCUTS");
	private NoteList noteList = new NoteList(this);
	private NoteEditor noteEditor = new NoteEditor(this);
	private Notebooks notebooks = new Notebooks(this);
	private Tags tags = new Tags(this);
	
	enum UiModes {
		notebooks, notes, tags
	};

	UiModes uiMode;

	public ElephantWindow() {
		setTitle("Elephant Premium");
		setSize(1080, 1050);

		Elephant.eventBus.register(this);

		createMenu();
		createSplit();
		createToolbar();

		// XXX show default notebook, create it if none exists
		showNotebook(Vault.getInstance().getNotebooks().get(1));

		KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
		manager.addKeyEventDispatcher(new KeyDispatcher());

		addWindowListener(new WindowAdapter() {
			public void windowActivated(WindowEvent e) {
				toolBar.focusGained();
			}

			public void windowDeactivated(WindowEvent e) {
				toolBar.focusLost();
			}
		});
	}

	// XXX this will bite me eventually
	private class KeyDispatcher implements KeyEventDispatcher {
		@Override
		public boolean dispatchKeyEvent(KeyEvent e) {
			switch (uiMode) {
			case notes:
				if (!noteEditor.hasFocus()) {
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
							noteEditor.clear();
							noteList.deleteSelected();
						}
						break;
					}
				}
				break;
			case notebooks:
				if (!notebooks.isEditing()) {
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

			noteEditor.saveChanges();

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
		noteList.unfocusEditor();
	}

	public void showNote(Note note) {
		showNotes();
		noteEditor.clear();
		noteEditor.load(note);
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

	ActionListener newNotebookAction = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			showNotebooks();
			notebooks.newNotebook();
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

		file.addSeparator();

		JMenuItem saveNote = new JMenuItem("Save");
		saveNote.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.META_MASK));
		saveNote.addActionListener(saveNoteAction);
		file.add(saveNote);

		JMenu edit = new JMenu("Edit");

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

		mb.add(file);
		mb.add(edit);
		mb.add(view);

		setJMenuBar(mb);
	}

	private void createSplit() {
		splitLeft = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		splitLeft.setResizeWeight(0.2);
		splitLeft.setContinuousLayout(true);
		splitLeft.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

		splitRight = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		splitRight.setResizeWeight(0.5);
		splitRight.setContinuousLayout(true);
		splitRight.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

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
}

// ;'''( laku
