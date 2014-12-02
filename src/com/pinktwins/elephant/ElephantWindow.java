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

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import com.pinktwins.elephant.data.Note;
import com.pinktwins.elephant.data.Notebook;
import com.pinktwins.elephant.data.Vault;

public class ElephantWindow extends JFrame {
	private static final long serialVersionUID = 1L;

	final public static Font fontH1 = Font.decode("Arial-BOLD-16");
	final public static Font fontSmall = Font.decode("Arial-10");
	
	JSplitPane splitLeft, splitRight;

	private Sidebar sideBar = new Sidebar();
	private NoteList noteList = new NoteList(this);
	private NoteEditor noteEditor = new NoteEditor(this);
	private Notebooks notebooks = new Notebooks(this);

	enum UiModes {
		notebooks, notes, tags
	};

	UiModes uiMode;

	public ElephantWindow() {
		setTitle("Elephant Premium");
		setSize(1080, 1050);

		createMenu();
		createSplit();
		createToolbar();

		// XXX show default notebook, create it if none exists
		showNotebook(Vault.getInstance().getNotebooks().get(1));

		KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
		manager.addKeyEventDispatcher(new KeyDispatcher());
	}

	private class KeyDispatcher implements KeyEventDispatcher {
		@Override
		public boolean dispatchKeyEvent(KeyEvent e) {
			switch (uiMode) {
			case notes:
				switch (e.getID()) {
				case KeyEvent.KEY_PRESSED:
					switch (e.getKeyCode()) {
					case KeyEvent.VK_UP:
						if (!noteEditor.hasFocus()) {
							noteList.changeSelection(-1);
						}
						break;
					case KeyEvent.VK_DOWN:
						if (!noteEditor.hasFocus()) {
							noteList.changeSelection(1);
						}
						break;
					}
					break;
				}
				break;
			}
			return false;
		}
	}

	private void showNotes() {
		splitLeft.setRightComponent(splitRight);
		uiMode = UiModes.notes;
	}

	private void showNotebooks() {
		splitLeft.setRightComponent(notebooks);
		uiMode = UiModes.notebooks;
	}

	private void showTags() {
		uiMode = UiModes.tags;
	}

	public void showNotebook(Notebook notebook) {
		showNotes();
		noteEditor.clear();
		noteList.load(notebook);
		noteList.changeSelection(1);
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

	ActionListener newNotebookAction = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			showNotebooks();
			// XXX NEW NOTEBOOK
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
		JPanel tools = new JPanel();
		tools.setBackground(Color.decode("#b6b6b6"));
		tools.setPreferredSize(new Dimension(1920, 40));

		add(tools, BorderLayout.NORTH);
	}

	public void onNoteListClicked(MouseEvent e) {
		noteEditor.unfocus();
	}

	public void updateThumb(Note note) {
		noteList.updateThumb(note);
	}

}
