package com.pinktwins.elephant;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import com.pinktwins.elephant.data.Notebook;
import com.pinktwins.elephant.data.Vault;
import com.sun.glass.events.KeyEvent;

public class ElephantWindow extends JFrame {
	private static final long serialVersionUID = 1L;

	JSplitPane splitLeft, splitRight;

	private Sidebar sideBar = new Sidebar();
	private NoteList noteList = new NoteList();
	private NoteEditor noteEditor = new NoteEditor();
	private Notebooks notebooks = new Notebooks(this);

	public ElephantWindow() {
		setTitle("Elephant Premium");
		setSize(1080, 1050);

		createMenu();

		splitLeft = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		splitLeft.setResizeWeight(0.2);
		splitLeft.setContinuousLayout(true);
		splitLeft.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		// splitLeft.setDividerSize(4);

		splitRight = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		splitRight.setResizeWeight(0.5);
		splitRight.setContinuousLayout(true);
		splitRight.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		// splitRight.setDividerSize(4);

		JTextField t1 = new JTextField("SIDEBAR");
		JTextField t2 = new JTextField("NOTELIST");
		JTextField t3 = new JTextField("NOTES");

		splitLeft.setLeftComponent(sideBar);
		splitLeft.setRightComponent(splitRight);

		splitRight.setLeftComponent(noteList);
		splitRight.setRightComponent(noteEditor);

		add(splitLeft, BorderLayout.CENTER);
	}

	private void showNotes() {
		splitLeft.setRightComponent(splitRight);
	}

	private void showNotebooks() {
		splitLeft.setRightComponent(notebooks);
	}

	private void showTags() {
		// XXX
	}

	public void showNotebook(Notebook notebook) {
		showNotes();
		noteList.load(notebook);
	}

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
}
