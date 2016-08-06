package com.pinktwins.elephant;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
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
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.border.Border;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter;
import javax.swing.text.Document;

import org.apache.commons.lang3.SystemUtils;

import com.google.common.eventbus.Subscribe;
import com.pinktwins.elephant.NoteEditor.NoteEditorStateListener;
import com.pinktwins.elephant.data.Note;
import com.pinktwins.elephant.data.Notebook;
import com.pinktwins.elephant.data.Search;
import com.pinktwins.elephant.data.Settings;
import com.pinktwins.elephant.data.Vault;
import com.pinktwins.elephant.eventbus.NoteChangedEvent;
import com.pinktwins.elephant.eventbus.ShortcutsChangedEvent;
import com.pinktwins.elephant.eventbus.StyleCommandEvent;
import com.pinktwins.elephant.eventbus.UIEvent;
import com.pinktwins.elephant.eventbus.UndoRedoStateUpdateRequest;
import com.pinktwins.elephant.eventbus.VaultEvent;
import com.pinktwins.elephant.util.Images;

public class ElephantWindow extends JFrame {

	public static final Font fontStart = Font.decode("Arial-ITALIC-18");
	public static final Font fontTitle = Font.decode("Helvetica-BOLD-18");
	public static final Font fontH1 = Font.decode("Helvetica-BOLD-16");
	public static final Font fontH2 = Font.decode("Helvetica-BOLD-14");
	public static final Font fontSmall = Font.decode("Helvetica-10");
	public static final Font fontEditorTitle = Font.decode("Helvetica-15");
	public static final Font fontEditor = Font.decode("Arial-13");
	public static final Font fontBoldEditor = Font.decode("Arial-BOLD-13");
	public static final Font fontBoldNormal = Font.decode("Arial-BOLD-14");
	public static final Font fontNormal = Font.decode("Arial-14");
	public static final Font fontMediumPlus = Font.decode("Arial-12");
	public static final Font fontMedium = Font.decode("Arial-11");
	public static final Font fontMediumMinus = Font.decode("Arial-10");
	public static final Font fontModalHeader = Font.decode("Arial-BOLD-16");
	public static final Font fontSideBarText = Font.decode("Arial-BOLD-13");
	public static final Font fontNotebookChooser = Font.decode("Helvetica-12");

	public static final Color colorTitle = Color.decode("#999999");
	public static final Color colorTitleButton = Color.decode("#666666");
	public static final Color colorGray5 = Color.decode("#555555");
	public static final Color colorDB = Color.decode("#dbdbdb");
	public static final Color colorGreen = Color.decode("#00a834");
	public static final Color colorBlue = Color.decode("#0091e6");
	public static final Color colorPreviewGray = Color.decode("#666663");
	public static final Color colorPreviewGrayOlder = Color.decode("#b1b1b1");

	private static final Color[] colorHighlights = { Color.YELLOW, Color.PINK, Color.CYAN, Color.ORANGE, Color.GREEN };
	private static final DefaultHighlightPainter[] highlightPainters = new DefaultHighlightPainter[colorHighlights.length];

	public static final int bigWidth = 1920;

	public static final Border emptyBorder = BorderFactory.createEmptyBorder(0, 0, 0, 0);

	private CustomSplitPane splitLeft, splitRight;

	private final Toolbar toolBar = new Toolbar(this);
	private final Sidebar sideBar = new Sidebar(this);
	private final NoteList noteList = new NoteList(this);
	private final NoteEditor noteEditor = new NoteEditor(this);
	private final MultipleNotes multipleNotes = new MultipleNotes(this);
	private final Notebooks notebooks = new Notebooks(this);
	private final TagList tagList = new TagList(this);

	private final History history = new History(this);

	private boolean hasWindowFocus;

	public static final int menuMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

	private JMenuBar menuBar;
	private JMenuItem iUndo, iRedo, iSaveSearch;
	private JCheckBoxMenuItem iCard, iSnippet, iRecentNotes;

	private static final Image elephantIcon;

	enum UiModes {
		notebooks, notes, tags
	};

	UiModes uiMode = UiModes.notes;

	private String previousSearchText = "";

	static {
		Iterator<Image> i = Images.iterator(new String[] { "elephantIcon" });
		elephantIcon = i.next();

		for (int n = 0; n < highlightPainters.length; n++) {
			highlightPainters[n] = new DefaultHighlightPainter(colorHighlights[n % colorHighlights.length]);
		}
	}

	ActionListener newNoteAction = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			// XXX if showing notebooks, open new note in solo window
			new UIEvent(UIEvent.Kind.editorWillChangeNote).post();
			showNotes();
			newNote();
		}
	};

	ActionListener saveNoteAction = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			noteEditor.focusEditor();
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

	ActionListener newTagAction = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			showTags();
			tagList.newTag();
		}
	};

	ActionListener newWindowAction = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			newWindow();
		}
	};

	ActionListener switchNoteLocationAction = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			// show start in a new frame
			JFrame startFrame = new JFrame();
			startFrame.setTitle("Elephant Note Location Switch");

			// Mac packages the icon just fine. Windows needs this for taskbar icon. Linux?
			if (SystemUtils.IS_OS_WINDOWS || SystemUtils.IS_OS_LINUX) {
				startFrame.setIconImage(elephantIcon);
			}

			startFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			// wild guessing for the coordinates
			Rectangle bounds = new Rectangle(130, 121, 495, 548);
			startFrame.setBounds(bounds);

			callStart(startFrame);
			startFrame.setVisible(true);
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

	ActionListener noteListCardViewAction = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			iCard.setSelected(true);
			iSnippet.setSelected(false);
			splitLeft.setDividerSize(6);
			noteList.changeMode(NoteList.ListModes.CARDVIEW);
		}
	};

	ActionListener noteListSnippetViewAction = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			iCard.setSelected(false);
			iSnippet.setSelected(true);
			splitLeft.setDividerSize(2);
			noteList.changeMode(NoteList.ListModes.SNIPPETVIEW);
		}
	};

	ActionListener showAllNotesAction = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			new UIEvent(UIEvent.Kind.editorWillChangeNote).post();
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

	ActionListener editTitleAction = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			noteEditor.editor.focusTitle();
		}
	};

	ActionListener editTagsAction = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			noteEditor.focusTags();
		}
	};

	ActionListener moveNoteAction = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (splitRight.getRightComponent() == noteEditor) {
				noteEditor.openNotebookChooserForMoving();
			}
			if (splitRight.getRightComponent() == multipleNotes) {
				multipleNotes.openNotebookChooserForMoving();
			}
		}
	};

	ActionListener addSearchToShortcutsAction = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			String s = toolBar.search.getText();
			if (!s.isEmpty()) {
				sideBar.addToShortcuts("search:" + s);
				new ShortcutsChangedEvent().post();
			}
		}
	};

	ActionListener addToShortcutsAction = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			for (Note n : noteList.getSelection()) {
				sideBar.addToShortcuts(n);
			}
			new ShortcutsChangedEvent().post();
		}
	};

	ActionListener addNotebookToShortcutsAction = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			Notebook nb = null;

			switch (uiMode) {
			case notes:
				nb = noteList.currentNotebook();
				break;
			case notebooks:
				if (notebooks.selectedItem != null) {
					nb = notebooks.selectedItem.getNotebook();
				}
				break;
			case tags:
				break;
			default:
				break;
			}

			if (nb != null && !nb.isDynamicallyCreatedNotebook()) {
				sideBar.addToShortcuts(nb);
				new ShortcutsChangedEvent().post();
			}
		}
	};

	ActionListener countNotebookWordsAction = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			JOptionPane.showMessageDialog(null, noteEditor.reloadWordCount() + " Words", noteEditor.editor.getTitle(), JOptionPane.PLAIN_MESSAGE);
		}
	};

	ActionListener jumpToNotebookAction = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (uiMode == UiModes.notes) {
				noteList.openNotebookChooserForJumping();
			}
		}
	};

	ActionListener showRecentNotesAction = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			sideBar.toggleRecentNotes();
		}
	};

	ActionListener viewBackAction = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			history.back();
		}
	};

	ActionListener viewForwardAction = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			history.forward();
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
			new StyleCommandEvent(e).post();
		}
	};

	ActionListener plainTextAction = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			noteEditor.turnToPlainText();
		}
	};

	ActionListener markdownAction = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
		}
	};

	ActionListener websiteAction = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				Desktop.getDesktop().browse(new URI("http://elephant.mine.nu"));
			} catch (IOException e1) {
				e1.printStackTrace();
			} catch (URISyntaxException e1) {
				e1.printStackTrace();
			}
		}
	};

	public ElephantWindow() {
		setTitle("Elephant Premium");

		// Mac packages the icon just fine. Windows needs this for taskbar icon. Linux?
		if (SystemUtils.IS_OS_WINDOWS || SystemUtils.IS_OS_LINUX) {
			setIconImage(elephantIcon);
		}

		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setBounds(loadBounds());
		setExtendedState(loadExtendedState());

		Elephant.eventBus.register(this);

		createMenu();
		createSplit();
		createToolbar();

		boolean startCalled = false;

		if (Vault.getInstance().hasLocation()) {
			add(splitLeft, BorderLayout.CENTER);

			Notebook b = Vault.getInstance().getDefaultNotebook();
			if (b != null) {
				showNotebook(b);
			}
		} else {
			callStart(this);
			startCalled = true;
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

				if (!alive) {
					System.exit(0);
				}
			}
		});

		addComponentListener(new ComponentListener() {
			@Override
			public void componentResized(ComponentEvent e) {
				saveBounds(ElephantWindow.this.getBounds());
				saveExtendedState(ElephantWindow.this.getExtendedState());
			}

			@Override
			public void componentMoved(ComponentEvent e) {
				saveBounds(ElephantWindow.this.getBounds());
				saveExtendedState(ElephantWindow.this.getExtendedState());
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

		toolBar.indexingInProgress(true);

		if (!startCalled && !Search.ssi.ready()) {
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
					Search.search("searchOptimization");
					System.out.println("Done in " + (System.currentTimeMillis() - start) + " ms");

					tagList.ssiDone();
					toolBar.indexingInProgress(false);

					try {
						Thread.sleep(200);
					} catch (InterruptedException e) {
						return;
					}

					if (Vault.getInstance().getNoteCount() < 5000) {
						System.out.println("Thumbnail cache..");
						start = System.currentTimeMillis();
						noteList.cache(Notebook.getNotebookWithAllNotes());
						System.out.println("Done in " + (System.currentTimeMillis() - start) + " ms");
					}
				}
			}.start();
		}
	}

	private void callStart(JFrame frame) {
		Start start = new Start(new Runnable() {
			@Override
			public void run() {
				if (!Elephant.restartApplication()) {
					JOptionPane.showMessageDialog(null, "Great! Now please restart.");
					System.exit(0);
				}
			}
		});

		frame.add(start, BorderLayout.CENTER);
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

	private int loadExtendedState() {
		int s = ElephantWindow.this.getExtendedState();
		if (Elephant.settings.getBoolean(Settings.Keys.WINDOW_MAXIMIZED)) {
			s |= JFrame.MAXIMIZED_BOTH;
		}
		return s;
	}

	private void saveExtendedState(int s) {
		Elephant.settings.setChain(Settings.Keys.WINDOW_MAXIMIZED, (s & JFrame.MAXIMIZED_BOTH) == JFrame.MAXIMIZED_BOTH ? true : false);
	}

	// Handling key dispatching for full control over keyboard interaction.
	// XXX this will bite me eventually
	private class KeyDispatcher implements KeyEventDispatcher {

		@Override
		public boolean dispatchKeyEvent(KeyEvent e) {
			if (!hasWindowFocus) {
				// XXX the windows accumulate. Get rid of them.
				for (Window w : getWindows()) {
					if (w instanceof NotebookChooser) {
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
				if (!noteEditor.hasFocus() && !multipleNotes.hasFocus() && !toolBar.isEditing()) {
					switch (e.getID()) {
					case KeyEvent.KEY_PRESSED:
						switch (e.getKeyCode()) {
						case KeyEvent.VK_UP:
							noteList.changeSelection(-1, e);
							break;
						case KeyEvent.VK_DOWN:
							noteList.changeSelection(1, e);
							break;
						case KeyEvent.VK_LEFT:
							noteList.changeSelection(-1, e);
							break;
						case KeyEvent.VK_RIGHT:
							noteList.changeSelection(1, e);
							break;
						case KeyEvent.VK_BACK_SPACE:
						case KeyEvent.VK_DELETE:
							deleteSelectedNote();
							break;
						}
						break;
					}
				} else {
					// XXX editor eats cmd-' for jumping to tags.
					// capture cmd-' here. why it comes as VK_BACK_SLASH
					// is unknown.
					// XXX works in mac, test win and test different key
					// layouts.
					switch (e.getID()) {
					case KeyEvent.KEY_PRESSED:
						switch (e.getKeyCode()) {
						case KeyEvent.VK_BACK_SLASH:
							if ((e.getModifiers() & menuMask) == menuMask) {
								editTagsAction.actionPerformed(null);
							}
							break;
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
						case KeyEvent.VK_BACK_SPACE:
						case KeyEvent.VK_DELETE:
							notebooks.deleteSelected();
							break;
						default:
							notebooks.handleKeyEvent(e);
						}
						break;
					}
				}
				break;
			case tags:
				if (!tagList.isEditing() && !toolBar.isEditing()) {
					switch (e.getID()) {
					case KeyEvent.KEY_PRESSED:
						switch (e.getKeyCode()) {
						case KeyEvent.VK_UP:
						case KeyEvent.VK_LEFT:
							tagList.changeSelection(-1, e.getKeyCode());
							break;
						case KeyEvent.VK_DOWN:
						case KeyEvent.VK_RIGHT:
							tagList.changeSelection(1, e.getKeyCode());
							break;
						case KeyEvent.VK_ENTER:
							tagList.openSelected();
							break;
						case KeyEvent.VK_BACK_SPACE:
						case KeyEvent.VK_DELETE:
							if (!tagList.isEditing) {
								tagList.deleteSelected();
								break;
							}
							// intentional fallthru to default
						default:
							tagList.handleKeyEvent(e);
						}
						break;
					}
				}
				break;
			default:
				break;
			}

			switch (e.getID()) {
			case KeyEvent.KEY_PRESSED:
				if (e.getKeyCode() >= KeyEvent.VK_1 && e.getKeyCode() <= KeyEvent.VK_9) {
					if ((e.getModifiers() & menuMask) == menuMask && (e.getModifiers() & KeyEvent.ALT_MASK) == 0) {
						String target = sideBar.shortcutList.getTarget(e.getKeyCode() - KeyEvent.VK_1);
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

		if (target.startsWith("search:")) {
			String search = target.substring("search:".length());
			toolBar.search.setText(search);

			new UIEvent(UIEvent.Kind.editorWillChangeNote).post();

			search(search);
			return true;
		}

		File f = new File(target);
		if (f.exists()) {

			new UIEvent(UIEvent.Kind.editorWillChangeNote).post();

			if (f.isDirectory()) {
				Notebook notebook = Vault.getInstance().findNotebook(f);
				if (notebook != null) {
					showNotebook(notebook);
					toolBar.clearSearch();
				}
			} else {
				File folder = f.getParentFile();
				Notebook notebook = Vault.getInstance().findNotebook(folder);
				if (notebook != null) {
					Note note = notebook.find(f.getName());
					if (note != null) {
						selectAndShowNote(notebook, note);
					}
				}
			}

			return true;
		} else {
			return false;
		}
	}

	public void selectAndShowNote(final Notebook nb, final Note n) {
		history.freeze();
		showNotebook(nb);
		noteList.selectNote(n, false);
		history.unFreeze();

		showNote(n);
		toolBar.clearSearch();
	}

	public void saveChanges() {
		noteEditor.saveChanges();
	}

	public void deleteSelectedNote() {

		// When deleting from Trash, confirm deletion
		if (noteList.isTrash() && Elephant.settings.getConfirmDeleteFromTrash()) {
			int count = noteList.getSelection().size();
			String noteName;
			if (count == 1) {
				noteName = "\"" + noteList.getSelection().iterator().next().getMeta().title() + "\"";
			} else {
				noteName = count + " notes";
			}
			String message = String.format("Permanently delete %s and any attachments? This cannot be undone.", noteName);
			if (JOptionPane.showConfirmDialog(null, message, String.format("Delete?"), JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
				return;
			}
		}

		noteEditor.clear();
		noteList.deleteSelected();

		// Should remove deleted note from history.
		// Let's just flush it.
		history.clear();
	}

	private void showNotes() {
		splitLeft.setRightComponent(splitRight);
		splitLeft.setDividerColor(CustomSplitPane.DividerColor.COLOR1);
		uiMode = UiModes.notes;
		sideBar.selectNavigation(0);
	}

	public void showNotebooks() {
		splitLeft.setRightComponent(notebooks);
		splitLeft.setDividerColor(CustomSplitPane.DividerColor.COLOR2);
		uiMode = UiModes.notebooks;
		sideBar.selectNavigation(1);

		history.addNotebooks();
	}

	public void showTags() {
		splitLeft.setRightComponent(tagList);
		splitLeft.setDividerColor(CustomSplitPane.DividerColor.COLOR2);
		uiMode = UiModes.tags;
		sideBar.selectNavigation(2);

		history.addTags();
	}

	public void showNotebook(Notebook notebook) {
		showNotes();
		noteEditor.clear();
		noteList.load(notebook);
		noteList.changeSelection(0, null);
	}

	public void showNote(Note note) {
		showNotes();
		splitRight.setRightComponent(noteEditor);
		refreshNote(note);
		if (!toolBar.isEditing()) {
			noteEditor.focusQuickLook();
		}

		history.add(note);
	}

	public void refreshNote(Note note) {
		noteEditor.clear();
		noteEditor.load(note);
		searchHighlight();
	}

	public void showMultipleNotes() {
		showNotes();
		noteEditor.clear();
		splitRight.setRightComponent(multipleNotes);
		multipleNotes.load(noteList.getSelection());
	}

	public boolean isEditorDirty() {
		return noteEditor.isDirty();
	}

	public void openNoteWindow(Note note) {
		// changes needed:
		// - create a new menubar for this jframe
		// - bind actions in the new menubar to this editor (style changes,
		// undo/redo, cmd-l, cmd-'
		// - disable actions in the editor that call window. or route them thru
		// eventBus
		// - remember and use previous notewindow position

		/*
		 * JFrame f = new JFrame(); f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		 * 
		 * NoteEditor e = new NoteEditor(null); e.load(note);
		 * 
		 * f.add(e); f.pack(); f.setBounds(400, 100, 550, 650); f.setVisible(true);
		 */
	}

	public void showAllNotes() {
		Notebook nb = Notebook.getNotebookWithAllNotes();
		showNotebook(nb);

		history.addAllNotes();
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
		if (noteList.isDynamicallyCreatedNotebook()) {
			Notebook nb = Vault.getInstance().getDefaultNotebook();
			showNotebook(nb);
		}

		noteList.newNote();
		focusEditor();
	}

	public void setSearchText(String text) {
		toolBar.search.setText(text);
	}

	public void search(String text) {
		previousSearchText = text;
		if (text.length() == 0) {
			showNotebook(Vault.getInstance().getDefaultNotebook());
			iSaveSearch.setEnabled(false);
		} else {
			history.freeze();

			showNotebook(Search.search(text));
			iSaveSearch.setEnabled(true);

			history.unFreeze();
			history.addSearch(text);
		}
	}

	public void redoSearch() {
		search(previousSearchText);
	}

	public boolean isShowingSearchResults() {
		return noteList.isSearch();
	}

	private JMenuItem menuItem(String title, int keyCode, int keyMask, ActionListener action) {
		JMenuItem mi = new JMenuItem(title);
		if (keyCode > 0 || keyMask > 0) {
			mi.setAccelerator(KeyStroke.getKeyStroke(keyCode, keyMask));
		}
		mi.addActionListener(action);
		return mi;
	}

	private void createMenu() {
		menuBar = new JMenuBar();

		JMenu file = new JMenu("File");
		file.add(menuItem("New Note", KeyEvent.VK_N, menuMask, newNoteAction));
		file.add(menuItem("New Notebook", KeyEvent.VK_N, menuMask | KeyEvent.SHIFT_DOWN_MASK, newNotebookAction));
		file.add(menuItem("New Tag", KeyEvent.VK_T, menuMask | KeyEvent.CTRL_DOWN_MASK, newTagAction));
		file.add(menuItem("New Elephant Window", KeyEvent.VK_N, menuMask | KeyEvent.ALT_DOWN_MASK, newWindowAction));
		file.add(menuItem("Switch Note Location", KeyEvent.VK_L, menuMask | KeyEvent.ALT_DOWN_MASK, switchNoteLocationAction));
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
		iSaveSearch = menuItem("Save Search to Shortcuts", 0, 0, addSearchToShortcutsAction);
		iSaveSearch.setEnabled(false);
		edit.add(iSaveSearch);

		JMenu view = new JMenu("View");
		view.add(menuItem("Notes", KeyEvent.VK_2, menuMask | KeyEvent.ALT_DOWN_MASK, showNotesAction));
		view.add(menuItem("Notebooks", KeyEvent.VK_3, menuMask | KeyEvent.ALT_DOWN_MASK, showNotebooksAction));
		view.add(menuItem("Tags", KeyEvent.VK_4, menuMask | KeyEvent.ALT_DOWN_MASK, showTagsAction));
		view.addSeparator();

		iCard = new JCheckBoxMenuItem("Card View");
		iCard.addActionListener(noteListCardViewAction);
		iSnippet = new JCheckBoxMenuItem("Snippet View");
		iSnippet.addActionListener(noteListSnippetViewAction);

		view.add(iCard);
		view.add(iSnippet);
		view.addSeparator();

		switch (Elephant.settings.getNoteListMode()) {
		case CARDVIEW:
			iCard.setSelected(true);
			break;
		case SNIPPETVIEW:
			iSnippet.setSelected(true);
			break;
		}

		view.add(menuItem("Show All Notes", KeyEvent.VK_A, menuMask | KeyEvent.SHIFT_DOWN_MASK, showAllNotesAction));
		view.add(menuItem("Jump to Notebook", KeyEvent.VK_J, menuMask, jumpToNotebookAction));

		view.addSeparator();
		iRecentNotes = new JCheckBoxMenuItem("Recent Notes");
		iRecentNotes.addActionListener(showRecentNotesAction);
		view.add(iRecentNotes);
		switch (Elephant.settings.getRecentNotesMode()) {
		case SHOW:
			iRecentNotes.setSelected(true);
			break;
		case HIDE:
			iRecentNotes.setSelected(false);
			break;
		}

		view.addSeparator();
		view.add(menuItem("Back", KeyEvent.VK_OPEN_BRACKET, menuMask, viewBackAction));
		view.add(menuItem("Forward", KeyEvent.VK_CLOSE_BRACKET, menuMask, viewForwardAction));

		JMenu note = new JMenu("Note");
		note.add(menuItem("Edit Note Title", KeyEvent.VK_L, menuMask, editTitleAction));
		note.add(menuItem("Edit Note Tags", KeyEvent.VK_QUOTE, menuMask, editTagsAction));
		note.addSeparator();
		note.add(menuItem("Move To Notebook", KeyEvent.VK_M, menuMask | KeyEvent.CTRL_DOWN_MASK, moveNoteAction));
		note.add(menuItem("Add Note to Shortcuts", 0, 0, addToShortcutsAction));
		note.add(menuItem("Add Notebook to Shortcuts", 0, 0, addNotebookToShortcutsAction));
		note.addSeparator();
		note.add(menuItem("Word Count...", 0, 0, countNotebookWordsAction));

		JMenu format = new JMenu("Format");
		JMenu style = new JMenu("Style");

		style.add(menuItem("Bold", KeyEvent.VK_B, menuMask, styleAction));
		style.add(menuItem("Italic", KeyEvent.VK_I, menuMask, styleAction));
		style.add(menuItem("Underline", KeyEvent.VK_U, menuMask, styleAction));
		style.add(menuItem("Strikethrough", KeyEvent.VK_K, menuMask | KeyEvent.CTRL_DOWN_MASK, styleAction));
		style.addSeparator();
		style.add(menuItem("Bigger", KeyEvent.VK_PLUS, menuMask, styleAction));
		style.add(menuItem("Smaller", KeyEvent.VK_MINUS, menuMask, styleAction));

		format.add(style);
		format.addSeparator();
		format.add(menuItem("Make Plain Text", 0, 0, plainTextAction));

		// somehow I dont like this menuitem. It should be much easier.
		// format.add(menuItem("Make Markdown", 0, 0, markdownAction));

		JMenu help = new JMenu("Help");
		JMenuItem version = menuItem("Elephant version " + Elephant.VERSION, 0, 0, null);
		version.setEnabled(false);
		help.add(version);
		help.add(menuItem("Elephant website", 0, 0, websiteAction));

		menuBar.add(file);
		menuBar.add(edit);
		menuBar.add(view);
		menuBar.add(note);
		menuBar.add(format);
		menuBar.add(help);

		setJMenuBar(menuBar);

		noteEditor.addStateListener(new NoteEditorStateListener() {
			@Override
			public void stateChange(boolean hasFocus, boolean hasSelection) {
				iCut.setEnabled(hasSelection);
				iCopy.setEnabled(hasSelection);
				iPaste.setEnabled(true);
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
		splitLeft.initLocationWithKey("divider1", 150);
		splitLeft.limitLocation(250);
		splitLeft.setDividerSize(6);

		if (Elephant.settings.getNoteListMode() == NoteList.ListModes.SNIPPETVIEW) {
			splitLeft.setDividerSize(2);
		}

		splitRight = new CustomSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		splitRight.setResizeWeight(0.5);
		splitRight.setContinuousLayout(true);
		splitRight.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		splitRight.initLocationWithKey("divider2", 425);
		splitRight.setDividerSize(2);

		splitLeft.setLeftComponent(sideBar);
		splitLeft.setRightComponent(splitRight);

		splitRight.setLeftComponent(noteList);
		splitRight.setRightComponent(noteEditor);
	}

	private void createToolbar() {
		toolBar.setPreferredSize(new Dimension(1920, 40));
		add(toolBar, BorderLayout.NORTH);
	}

	public void onNoteListClicked(MouseEvent e) {
		noteEditor.unfocus();
	}

	public void sortAndUpdate() {
		noteList.sortAndUpdate();
		splitLeft.revalidate();
	}

	public int getIndexOfFirstSelectedNoteInNoteList() {
		return noteList.getIndexOfFirstSelectedNote();
	}

	public void selectNoteByIndex(int index) {
		Note note = noteList.selectNote(index);
		if (note != null) {
			showNote(note);
		}
	}

	public void updateThumb(Note note) {
		noteList.updateThumb(note);
		splitLeft.revalidate();
	}

	@Subscribe
	public void handleNoteChanged(NoteChangedEvent event) {
		updateThumb(event.note);
		if (event.contentChanged) {
			sortAndUpdate();
		}
	}

	@Subscribe
	public void handleVaultEvent(VaultEvent event) {
		switch (event.kind) {
		case notebookCreated:
			break;
		case notebookListChanged:
			notebooks.refresh();
			break;
		case notebookRefreshed:
			if (noteList.isShowingNotebook(event.ref)) {
				sortAndUpdate();
			}
			break;
		default:
			break;
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

	public void searchHighlight() {
		textHighlight(toolBar.search.getText());
	}

	private void textHighlight(String text) {
		if (text.length() == 0)
			return;

		JTextPane editorTextPane = noteEditor.getEditor().getEditorPane();

		try {
			Document document = editorTextPane.getDocument();
			String documentText = document.getText(0, document.getLength()).toLowerCase();

			String[] searchWords = text.toLowerCase().split(" ");
			for (int n = 0; n < searchWords.length; n++) {
				String word = searchWords[n];
				DefaultHighlightPainter p = highlightPainters[n % highlightPainters.length];

				int index = documentText.indexOf(word);
				while (index > -1) {
					editorTextPane.getHighlighter().addHighlight(index, index + word.length(), p);
					index = documentText.indexOf(word, index + 1);
				}
			}

		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
