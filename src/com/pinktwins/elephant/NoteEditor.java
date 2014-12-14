package com.pinktwins.elephant;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.text.BadLocationException;

import com.google.common.eventbus.Subscribe;
import com.pinktwins.elephant.CustomEditor.AttachmentInfo;
import com.pinktwins.elephant.CustomEditor.EditorEventListener;
import com.pinktwins.elephant.Notebooks.NotebookActionListener;
import com.pinktwins.elephant.data.Note;
import com.pinktwins.elephant.data.Note.Meta;
import com.pinktwins.elephant.data.NoteChangedEvent;
import com.pinktwins.elephant.data.Notebook;
import com.pinktwins.elephant.data.Vault;

public class NoteEditor extends BackgroundPanel implements EditorEventListener {

	private static final long serialVersionUID = 5649274177360148568L;

	static public final int kMinNoteSize = 288;

	private static Image tile, noteTopShadow, noteToolsNotebook, noteToolsTrash, noteToolsDivider;

	private ElephantWindow window;

	private boolean isDirty;

	private final int kNoteOffset = 64;
	private final int kBorder = 14;

	static {
		try {
			tile = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/noteeditor.png"));
			noteTopShadow = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/noteTopShadow.png"));
			noteToolsNotebook = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/noteToolsNotebook.png"));
			noteToolsTrash = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/noteToolsTrash.png"));
			noteToolsDivider = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/noteToolsDivider.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	interface NoteEditorStateListener {
		public void stateChange(boolean hasFocus, boolean hasSelection);
	}

	public NoteEditor(ElephantWindow w) {
		super(tile);
		window = w;

		Elephant.eventBus.register(this);

		createComponents();
	}

	NoteEditorStateListener stateListener;

	public void addStateListener(NoteEditorStateListener l) {
		stateListener = l;
	}

	private Note currentNote;
	private HashMap<Object, File> currentAttachments = new HashMap<Object, File>();

	JPanel main, area;
	ScrollablePanel areaHolder;
	BackgroundPanel scrollHolder;
	JScrollPane scroll;
	CustomEditor editor;
	BackgroundPanel topShadow;
	JButton currNotebook;
	JLabel noteCreated, noteUpdated;
	BorderLayout areaHolderLayout;

	private class DividedPanel extends BackgroundPanel {
		private static final long serialVersionUID = -7285142017724975923L;

		public DividedPanel(Image i) {
			super(i);
		}

		@Override
		public void paint(Graphics g) {
			super.paint(g);
			g.drawImage(noteToolsDivider, 16, 32, getWidth() - 32, 2, null);
		}
	}

	private class TopShadowPanel extends JPanel {
		private static final long serialVersionUID = 6626079564069649611L;

		@Override
		public void paint(Graphics g) {
			super.paint(g);

			if (scroll.getVerticalScrollBar().getValue() < 4) {
				g.drawImage(noteTopShadow, 0, kNoteOffset, getWidth(), 4, null);
			} else {
				g.drawImage(noteTopShadow, 0, kNoteOffset, getWidth(), 2, null);
			}
		}
	}

	public class ScrollablePanel extends JPanel implements Scrollable {
		public Dimension getPreferredScrollableViewportSize() {
			Dimension d = getPreferredSize();
			if (d.height < kMinNoteSize) {
				d.height = kMinNoteSize;
			}
			return d;
		}

		public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
			return 10;
		}

		public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
			return ((orientation == SwingConstants.VERTICAL) ? visibleRect.height : visibleRect.width) - 10;
		}

		public boolean getScrollableTracksViewportWidth() {
			return true;
		}

		public boolean getScrollableTracksViewportHeight() {
			return false;
		}
	}

	private void createComponents() {
		main = new TopShadowPanel();
		main.setLayout(null);
		main.setBorder(BorderFactory.createEmptyBorder(kBorder, kBorder, kBorder, kBorder));

		final DividedPanel tools = new DividedPanel(tile);
		tools.setBounds(0, 0, 1920, 65);

		JPanel toolsTop = new JPanel(new BorderLayout());
		toolsTop.setOpaque(false);

		JPanel toolsTopLeft = new JPanel(new BorderLayout());
		toolsTopLeft.setOpaque(false);

		JPanel toolsTopRight = new JPanel(new BorderLayout());
		toolsTopRight.setOpaque(false);

		currNotebook = new JButton("");
		currNotebook.setBorderPainted(false);
		currNotebook.setContentAreaFilled(false);
		currNotebook.setIcon(new ImageIcon(noteToolsNotebook));
		currNotebook.setForeground(ElephantWindow.colorTitleButton);

		JButton trash = new JButton("");
		trash.setBorderPainted(false);
		trash.setContentAreaFilled(false);
		trash.setIcon(new ImageIcon(noteToolsTrash));

		toolsTopLeft.add(currNotebook, BorderLayout.WEST);
		toolsTopRight.add(trash, BorderLayout.EAST);
		toolsTop.add(toolsTopLeft, BorderLayout.WEST);
		toolsTop.add(toolsTopRight, BorderLayout.EAST);

		JPanel toolsBot = new JPanel(new FlowLayout(FlowLayout.LEFT));

		noteCreated = new JLabel("Created: xxxxxx");
		noteCreated.setBorder(BorderFactory.createEmptyBorder(0, 4, 4, 10));
		noteCreated.setForeground(ElephantWindow.colorTitleButton);
		noteCreated.setFont(ElephantWindow.fontMedium);

		noteUpdated = new JLabel("Updated: xxxxxx");
		noteUpdated.setBorder(BorderFactory.createEmptyBorder(0, 4, 4, 20));
		noteUpdated.setForeground(ElephantWindow.colorTitleButton);
		noteUpdated.setFont(ElephantWindow.fontMedium);

		toolsBot.add(noteCreated);
		toolsBot.add(noteUpdated);

		tools.add(toolsTop, BorderLayout.NORTH);
		tools.add(toolsBot, BorderLayout.SOUTH);

		main.add(tools);

		area = new JPanel();
		area.setLayout(new GridLayout(1, 1));
		area.setBackground(Color.WHITE);

		editor = new CustomEditor();
		editor.setEditorEventListener(this);
		area.add(editor);
		area.setBounds(kBorder, kBorder, 200, kMinNoteSize);

		// Swing when you're winning part #1.

		final int topBorderOffset = 2;
		areaHolderLayout = new BorderLayout();
		areaHolder = new ScrollablePanel();
		areaHolder.setLayout(areaHolderLayout);
		areaHolder.setBorder(BorderFactory.createEmptyBorder(kBorder - topBorderOffset, kBorder, kBorder, kBorder));
		//areaHolder.setBounds(0, 0, 200, kMinNoteSize);
		areaHolder.add(area, BorderLayout.NORTH);

		scrollHolder = new BackgroundPanel();
		scrollHolder.setOpaque(false);
		//scrollHolder.setBounds(0, 0, 200, kMinNoteSize);

		scroll = new JScrollPane(areaHolder);
		scroll.setOpaque(false);
		scroll.setBorder(ElephantWindow.emptyBorder);
		scroll.getVerticalScrollBar().setUnitIncrement(10);
		scroll.getHorizontalScrollBar().setUnitIncrement(10);

		scrollHolder.add(scroll, BorderLayout.CENTER);

		main.add(scrollHolder);

		add(main, BorderLayout.CENTER);

		caretChanged(editor.getTextPane());

		currNotebook.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				openNotebookChooserForMoving();
			}
		});

		trash.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				window.deleteSelectedNote();
			}
		});

		addComponentListener(new ResizeListener() {
			@Override
			public void componentResized(ComponentEvent e) {
				Rectangle mb = main.getBounds();
				Rectangle ab = area.getBounds();

				ab.width = mb.width - kBorder * 2;
				area.setBounds(ab);

				scrollHolder.setBounds(0, kNoteOffset + topBorderOffset, getWidth(), getHeight() - kNoteOffset - topBorderOffset);
				areaHolder.setBounds(0, 0, ab.width, ab.height);

				EventQueue.invokeLater(new Runnable() {
					public void run() {
						//NoteEditor.this.editor.revalidate();

						Rectangle r = tools.getBounds();
						r.width = getWidth();
						tools.setBounds(r);

						tools.revalidate();
					}
				});
			}
		});

		main.addMouseListener(new CustomMouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {
				unfocus();
			}
		});
	}

	public void openNotebookChooserForMoving() {
		if (currentNote != null) {
			NotebookChooser nbc = new NotebookChooser(window, String.format("Move \"%s\"", editor.getTitle()));

			// Center on window
			Point p = currNotebook.getLocationOnScreen();
			Rectangle r = window.getBounds();
			int x = r.x + r.width / 2 - NotebookChooser.fixedWidth / 2;
			nbc.setBounds(x, p.y, NotebookChooser.fixedWidth, NotebookChooser.fixedHeight);

			nbc.setVisible(true);

			nbc.setNotebookActionListener(new NotebookActionListener() {
				@Override
				public void didCancelSelection() {
				}

				@Override
				public void didSelect(Notebook nb) {
					moveNoteAction(currentNote, nb);
				}
			});
		}
	}

	protected void moveNoteAction(Note n, Notebook destination) {
		if (n == null || destination == null) {
			throw new AssertionError();
		}

		File source = n.file().getParentFile();
		if (destination.folder().equals(source)) {
			return;
		}

		System.out.println("move " + n.getMeta().title() + " -> " + destination.name() + " (" + destination.folder() + ")");

		n.moveTo(destination.folder());
		window.sortAndUpdate();
		clear();
	}

	public void clear() {
		currentNote = null;
		currentAttachments.clear();
		editor.clear();
		isDirty = false;
		visible(false);
	}

	public void load(Note note) {
		currentNote = note;
		currentAttachments.clear();

		Meta m = note.getMeta();
		editor.setTitle(m.title());
		editor.setText(note.contents());

		File[] files = currentNote.getAttachmentList();
		if (files != null) {
			for (File f : files) {
				if (f.getName().charAt(0) != '.' && f.isFile()) {
					int position = m.getAttachmentPosition(f);
					insertFileIntoNote(f, position);
				}
			}
		}

		visible(true);

		Notebook nb = Vault.getInstance().findNotebook(note.file().getParentFile());
		currNotebook.setText(nb.name());

		noteCreated.setText("Created: " + note.createdStr());
		noteUpdated.setText("Updated: " + note.updatedStr());

		caretChanged(editor.getTextPane());
	}

	private void visible(boolean b) {
		main.setVisible(b);
	}

	public boolean hasFocus() {
		return editor.hasFocus();
	}

	public void unfocus() {
		window.unfocusEditor();
	}

	@Subscribe
	public void handleUIEvent(UIEvent event) {
		if (event.kind == UIEvent.Kind.editorWillChangeNote) {
			saveChanges();
		}
	}

	public void saveChanges() {
		if (!isDirty) {
			return;
		}

		if (currentNote != null) {
			boolean changed = false;
			boolean contentChanged = false;

			try {
				String fileTitle = currentNote.getMeta().title();
				String editedTitle = editor.getTitle();
				if (!fileTitle.equals(editedTitle)) {
					currentNote.getMeta().title(editedTitle);
					currentNote.attemptSafeRename(editedTitle + (editor.isRichText ? ".rtf" : ".txt"));
					changed = true;
				}

				String fileText = currentNote.contents();
				String editedText = editor.getText();
				if (!fileText.equals(editedText)) {
					currentNote.save(editedText);
					changed = true;
					contentChanged = true;

					// update attachment positions
					Set<Object> remainingAttachments = currentAttachments.keySet();
					for (AttachmentInfo info : editor.getAttachmentInfo()) {
						if (info.object instanceof ImageIcon || info.object instanceof FileAttachment) {
							File f = currentAttachments.get(info.object);
							if (f != null) {
								currentNote.getMeta().setAttachmentPosition(f, info.startPosition);
								remainingAttachments.remove(info.object);
							}
						}
					}

					// remainingAttachments were not found in document anymore.
					// Move to 'deleted'
					for (Object o : remainingAttachments) {
						File f = currentAttachments.get(o);
						System.out.println("No longer in document: " + f);
						currentNote.removeAttachment(f);
						currentAttachments.remove(o);
					}
				}
			} catch (BadLocationException e) {
				e.printStackTrace();
			}

			if (changed) {
				Elephant.eventBus.post(new NoteChangedEvent(currentNote));
				if (contentChanged) {
					window.sortAndUpdate();
				}
			}
		}

		isDirty = false;
	}

	public void focusTitle() {
		editor.focusTitle();
	}

	@Override
	public void editingFocusGained() {
		isDirty = true;
	}

	@Override
	public void editingFocusLost() {
		saveChanges();
	}

	@Override
	public void caretChanged(final JTextPane text) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				int pos = text.getCaretPosition();
				int len = text.getDocument().getLength();
				if (pos == len) {
					scroll.getVerticalScrollBar().setValue(Integer.MAX_VALUE);
				}
			}
		});

		int len = text.getDocument().getLength();
		int start = text.getSelectionStart(), end = text.getSelectionEnd();
		boolean hasSelection = (start >= 0 && start < len && end > start && end <= len);
		boolean hasFocus = text.hasFocus();

		if (stateListener != null) {
			stateListener.stateChange(hasFocus, hasSelection);
		}
	}

	private void insertFileIntoNote(File f, int position) {
		JTextPane noteArea = editor.getTextPane();

		int caret = noteArea.getCaretPosition();

		// String ext = FilenameUtils.getExtension(attached.getAbsolutePath());
		// if ext is image?
		try {
			Image i = ImageIO.read(f);
			if (i != null) {
				ImageIcon ii = new ImageIcon(i);

				noteArea.setCaretPosition(position);
				noteArea.insertIcon(ii);

				currentAttachments.put(ii, f);
			} else {
				FileAttachment aa = new FileAttachment(f);

				noteArea.setCaretPosition(position);
				noteArea.insertComponent(aa);

				currentAttachments.put(aa, f);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		noteArea.setCaretPosition(caret);
	}

	@Override
	public void filesDropped(List<File> files) {
		JTextPane noteArea = editor.getTextPane();
		for (File f : files) {
			if (f.isDirectory()) {
				// XXX directory dropped. what to do? compress and import zip?
			}
			if (f.isFile()) {
				System.out.println("file: " + f.getAbsolutePath());
				try {
					File attached = currentNote.importAttachment(f);
					currentNote.getMeta().setAttachmentPosition(attached, noteArea.getCaretPosition());

					insertFileIntoNote(attached, noteArea.getCaretPosition());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void cutAction() {
		editor.getTextPane().cut();
	}

	public void copyAction() {
		editor.getTextPane().copy();
	}

	public void pasteAction() {
		editor.getTextPane().paste();
	}

}
