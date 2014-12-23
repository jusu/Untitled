package com.pinktwins.elephant;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
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
import com.pinktwins.elephant.NoteEditor.NoteEditorStateListener;
import com.pinktwins.elephant.data.Note;
import com.pinktwins.elephant.data.NoteChangedEvent;
import com.pinktwins.elephant.data.Notebook;
import com.pinktwins.elephant.data.Vault;
import com.pinktwins.elephant.data.VaultEvent;

public class ElephantWindow extends JFrame {
	private static final long serialVersionUID = 1L;

	final public static Font fontStart = Font.decode("Arial-ITALIC-18");
	final public static Font fontTitle = Font.decode("Helvetica-BOLD-18");
	final public static Font fontH1 = Font.decode("Helvetica-BOLD-16");
	final public static Font fontSmall = Font.decode("Helvetica-10");
	final public static Font fontEditor = Font.decode("Arial-13");
	final public static Font fontBoldEditor = Font.decode("Arial-BOLD-13");
	final public static Font fontBoldNormal = Font.decode("Arial-BOLD-14");
	final public static Font fontNormal = Font.decode("Arial-14");
	final public static Font fontMedium = Font.decode("Arial-11");
	final public static Font fontModalHeader = Font.decode("Arial-BOLD-16");

	final public static Color colorTitle = Color.decode("#999999");
	final public static Color colorTitleButton = Color.decode("#666666");
	final public static Color colorGray5 = Color.decode("#555555");

	final public static Border emptyBorder = BorderFactory.createEmptyBorder(0, 0, 0, 0);

	CustomSplitPane splitLeft, splitRight;

	private Toolbar toolBar = new Toolbar(this);
	private Sidebar sideBar = new Sidebar(this);
	private NoteList noteList = new NoteList(this);
	private NoteEditor noteEditor = new NoteEditor(this);
	private Notebooks notebooks = new Notebooks(this, false, "");
	private Tags tags = new Tags(this);

	private boolean hasWindowFocus;

	public static int menuMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

	JMenuItem iUndo, iRedo;

	enum UiModes {
		notebooks, notes, tags
	};

	UiModes uiMode = UiModes.notes;

	public ElephantWindow() {
		setTitle("Elephant Premium");

		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		setBounds(loadBounds());

		Elephant.eventBus.register(this);

		createMenu();
		createSplit();
		createToolbar();

		if (Vault.getInstance().hasLocation()) {
			Notebook b = Vault.getInstance().getDefaultNotebook();
			if (b != null) {
				showNotebook(b);
			}
		}

		final KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
		final KeyDispatcher keyDisp = new KeyDispatcher();

		manager.addKeyEventDispatcher(keyDisp);

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowActivated(WindowEvent e) {
				hasWindowFocus = true;
				toolBar.focusGained();
			}

			@Override
			public void windowDeactivated(WindowEvent e) {
				hasWindowFocus = false;

				for (Window w : getWindows()) {
					if (w instanceof NotebookChooser && w.isActive()) {
						return;
					}
				}

				toolBar.focusLost();
			}

			@Override
			public void windowClosed(WindowEvent e) {
				saveChanges();

				manager.removeKeyEventDispatcher(keyDisp);

				boolean alive = false;

				for (Window w : getWindows()) {
					if (w.isShowing()) {
						alive = true;
					}
				}

				System.out.println("Window closed. alive=" + alive);

				if (!alive) {
					System.exit(0);
				}
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

		// Run dummy search in the background. This will populate the
		// 'SimpleSearchIndex' class
		// with string to note references for future searches.
		// Also cache notelist items to speed up search result displays.

		if (!Vault.ssi.ready()) {
			new Thread() {
				@Override
				public void run() {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						return;
					}

					System.out.println("Search optimization..");
					long start = System.currentTimeMillis();
					Vault.getInstance().search("a");
					System.out.println("Done in " + (System.currentTimeMillis() - start) + " ms");

					try {
						Thread.sleep(200);
					} catch (InterruptedException e) {
						return;
					}

					System.out.println("Thumbnail cache..");
					start = System.currentTimeMillis();
					noteList.cache(Notebook.getNotebookWithAllNotes());
					System.out.println("Done in " + (System.currentTimeMillis() - start) + " ms");
				}
			}.start();
		}
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
		b.height = h > 0 ? h : 600;
		return b;
	}

	private void saveBounds(Rectangle r) {
		Elephant.settings.setChain("windowX", r.x).setChain("windowY", r.y).setChain("windowWidth", r.width).set("windowHeight", r.height);
	}

	// Handling key dispatching for full control over keyboard interaction.
	// XXX this will bite me eventually
	private class KeyDispatcher implements KeyEventDispatcher {

		@Override
		public boolean dispatchKeyEvent(KeyEvent e) {
			if (!hasWindowFocus) {
				int n = 0;

				// XXX the windows accumulate. Get rid of them.
				for (Window w : getWindows()) {
					if (w instanceof NotebookChooser) {
						// System.out.println("NotebookChoosers: " + (++n));
						if (w.isActive()) {
							((NotebookChooser) w).handleKeyEvent(e);
							return false;
						}
					}
				}

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
					if ((e.getModifiers() & menuMask) == menuMask && (e.getModifiers() & KeyEvent.ALT_MASK) == 0) {
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

	public boolean openShortcut(String target) {
		if (Sidebar.ACTION_NOTES.equals(target)) {
			showNotes();
			return true;
		}

		if (Sidebar.ACTION_NOTEBOOKS.equals(target)) {
			showNotebooks();
			return true;
		}

		if (Sidebar.ACTION_TAGS.equals(target)) {
			showTags();
			return true;
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

			return true;
		} else {
			return false;
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

	public void showSettings() {
	}

	public void focusEditor() {
		noteEditor.focusTitle();
	}

	public void unfocusEditor() {
		noteList.unfocusEditor();
	}

	public void newNote() {
		if (noteList.isAllNotes() || noteList.isTrash()) {
			Notebook nb = Vault.getInstance().getDefaultNotebook();
			showNotebook(nb);
		}

		noteList.newNote();
		focusEditor();
	}

	public void search(String text) {
		if (text.length() == 0) {
			showNotebook(Vault.getInstance().getDefaultNotebook());
		} else {
			showNotebook(Vault.getInstance().search(text));
		}
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

	ActionListener cutTextAction = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			noteEditor.cutAction();
		}
	};

	ActionListener copyTextAction = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			noteEditor.copyAction();
		}
	};

	ActionListener pasteTextAction = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			noteEditor.pasteAction();
		}
	};

	ActionListener moveNoteAction = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			noteEditor.openNotebookChooserForMoving();
		}
	};

	ActionListener jumpToNotebookAction = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (uiMode == uiMode.notes) {
				noteList.openNotebookChooserForJumping();
			}
		}
	};

	ActionListener settingsAction = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			showSettings();
		}
	};

	ActionListener undoAction = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			noteEditor.undo();
		}
	};

	ActionListener redoAction = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			noteEditor.redo();
		}
	};

	ActionListener styleAction = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			Elephant.eventBus.post(new StyleCommandEvent(e));
		}
	};

	private JMenuItem menuItem(String title, int keyCode, int keyMask, ActionListener action) {
		JMenuItem mi = new JMenuItem(title);
		if (keyCode > 0 || keyMask > 0) {
			mi.setAccelerator(KeyStroke.getKeyStroke(keyCode, keyMask));
		}
		mi.addActionListener(action);
		return mi;
	}

	private void createMenu() {
		JMenuBar mb = new JMenuBar();

		JMenu file = new JMenu("File");
		file.add(menuItem("New Note", KeyEvent.VK_N, menuMask, newNoteAction));
		file.add(menuItem("New Notebook", KeyEvent.VK_N, menuMask | KeyEvent.SHIFT_DOWN_MASK, newNotebookAction));
		file.add(menuItem("New Elephant Window", KeyEvent.VK_N, menuMask | KeyEvent.ALT_DOWN_MASK, newWindowAction));
		file.addSeparator();
		file.add(menuItem("Save", KeyEvent.VK_S, menuMask, saveNoteAction));
		// file.addSeparator();
		// file.add(menuItem("Settings", KeyEvent.VK_COMMA,
		// ActionEvent.META_MASK, settingsAction));

		JMenu edit = new JMenu("Edit");

		iUndo = menuItem("Undo", KeyEvent.VK_Z, menuMask, undoAction);
		iUndo.setEnabled(false);
		iRedo = menuItem("Redo", KeyEvent.VK_Z, menuMask | KeyEvent.SHIFT_DOWN_MASK, redoAction);
		iRedo.setEnabled(false);

		edit.add(iUndo);
		edit.add(iRedo);
		edit.addSeparator();

		final JMenuItem iCut = menuItem("Cut", KeyEvent.VK_X, menuMask, cutTextAction);
		final JMenuItem iCopy = menuItem("Copy", KeyEvent.VK_C, menuMask, copyTextAction);
		final JMenuItem iPaste = menuItem("Paste", KeyEvent.VK_V, menuMask, pasteTextAction);
		edit.add(iCut);
		edit.add(iCopy);
		edit.add(iPaste);

		edit.addSeparator();
		edit.add(menuItem("Search Notes...", KeyEvent.VK_F, menuMask | KeyEvent.ALT_DOWN_MASK, searchAction));

		JMenu view = new JMenu("View");
		view.add(menuItem("Notes", KeyEvent.VK_2, menuMask | KeyEvent.ALT_DOWN_MASK, showNotesAction));
		view.add(menuItem("Notebooks", KeyEvent.VK_3, menuMask | KeyEvent.ALT_DOWN_MASK, showNotebooksAction));
		view.add(menuItem("Tags", KeyEvent.VK_4, menuMask | KeyEvent.ALT_DOWN_MASK, showTagsAction));
		view.addSeparator();
		view.add(menuItem("Show All Notes", KeyEvent.VK_A, menuMask | KeyEvent.SHIFT_DOWN_MASK, showAllNotesAction));
		view.add(menuItem("Jump to Notebook", KeyEvent.VK_J, menuMask, jumpToNotebookAction));

		JMenu note = new JMenu("Note");
		note.add(menuItem("Move To Notebook", KeyEvent.VK_M, menuMask | KeyEvent.CTRL_DOWN_MASK, moveNoteAction));

		JMenu format = new JMenu("Format");
		JMenu style = new JMenu("Style");

		style.add(menuItem("Bold", KeyEvent.VK_B, menuMask, styleAction));
		style.add(menuItem("Italic", KeyEvent.VK_I, menuMask, styleAction));
		style.add(menuItem("Underline", KeyEvent.VK_U, menuMask, styleAction));
		style.addSeparator();
		style.add(menuItem("Bigger", KeyEvent.VK_PLUS, menuMask, styleAction));
		style.add(menuItem("Smaller", KeyEvent.VK_MINUS, menuMask, styleAction));

		format.add(style);
		format.addSeparator();
		format.add(menuItem("Make Plain Text", 0, 0, styleAction));

		mb.add(file);
		mb.add(edit);
		mb.add(view);
		mb.add(note);
		mb.add(format);

		setJMenuBar(mb);

		noteEditor.addStateListener(new NoteEditorStateListener() {
			@Override
			public void stateChange(boolean hasFocus, boolean hasSelection) {
				iCut.setEnabled(hasFocus && hasSelection);
				iCopy.setEnabled(hasFocus && hasSelection);
				iPaste.setEnabled(hasFocus);
			}
		});
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

		if (!Vault.getInstance().hasLocation()) {
			Start start = new Start(new Runnable() {
				@Override
				public void run() {
					ElephantWindow.this.setVisible(false);
					ElephantWindow.this.dispose();
					newWindow();
				}
			});
			add(start);
		} else {
			add(splitLeft, BorderLayout.CENTER);
		}
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
		splitLeft.revalidate();
	}

	@Subscribe
	public void handleVaultEvent(VaultEvent event) {
		if (event.kind == VaultEvent.Kind.notebookListChanged) {
			notebooks.refresh();
		}
	}

	@Subscribe
	public void handleUndoRedoSUR(UndoRedoStateUpdateRequest r) {
		if (r.manager.canUndo()) {
			iUndo.setEnabled(true);
			iUndo.setName(r.manager.getUndoPresentationName());
		} else {
			iUndo.setEnabled(false);
			iUndo.setName("Undo");
		}

		if (r.manager.canRedo()) {
			iRedo.setEnabled(true);
			iRedo.setName(r.manager.getRedoPresentationName());
		} else {
			iRedo.setEnabled(false);
			iRedo.setName("Redo");
		}
	}
}
