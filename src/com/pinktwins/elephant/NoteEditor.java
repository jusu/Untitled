package com.pinktwins.elephant;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridBagLayout;
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
import java.util.Iterator;
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

import org.apache.commons.io.FilenameUtils;

import com.google.common.eventbus.Subscribe;
import com.pinktwins.elephant.CustomEditor.AttachmentInfo;
import com.pinktwins.elephant.EditorEventListener;
import com.pinktwins.elephant.Notebooks.NotebookActionListener;
import com.pinktwins.elephant.data.Factory;
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

	private Note loadAfterLayout = null;

	static public ImageScalingCache scalingCache = new ImageScalingCache();

	static {
		Iterator<Image> i = Images.iterator(new String[] { "noteeditor", "noteTopShadow", "noteToolsNotebook", "noteToolsTrash", "noteToolsDivider" });
		tile = i.next();
		noteTopShadow = i.next();
		noteToolsNotebook = i.next();
		noteToolsTrash = i.next();
		noteToolsDivider = i.next();
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
	private HashMap<Object, File> currentAttachments = Factory.newHashMap();

	JPanel main, area;
	ScrollablePanel areaHolder;
	BackgroundPanel scrollHolder;
	JScrollPane scroll;
	CustomEditor editor;
	TagEditorPane tagPane;
	BackgroundPanel topShadow;
	JButton currNotebook, trash;
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

		private final Color lineColor = Color.decode("#b4b4b4");

		@Override
		public void paint(Graphics g) {
			super.paint(g);

			if (scroll.getVerticalScrollBar().getValue() < 4) {
				g.drawImage(noteTopShadow, 0, kNoteOffset, getWidth(), 4, null);
			} else {
				g.drawImage(noteTopShadow, 0, kNoteOffset, getWidth(), 2, null);
			}

			g.setColor(lineColor);
			g.drawLine(0, 0, 0, getHeight());
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
		tools.setBounds(1, 0, 1920, 65);

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
		currNotebook.setFont(ElephantWindow.fontMediumPlus);

		tagPane = new TagEditorPane();
		tagPane.setEditorEventListener(this);

		trash = new JButton("");
		trash.setBorderPainted(false);
		trash.setContentAreaFilled(false);
		trash.setIcon(new ImageIcon(noteToolsTrash));

		JPanel toolsTopLeftWest = new JPanel(new GridBagLayout());
		toolsTopLeftWest.setOpaque(false);
		toolsTopLeftWest.add(currNotebook);
		toolsTopLeftWest.add(tagPane.getComponent());

		toolsTopLeft.add(toolsTopLeftWest, BorderLayout.WEST);
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
		areaHolder.add(area, BorderLayout.NORTH);

		scrollHolder = new BackgroundPanel();
		scrollHolder.setOpaque(false);

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

				Rectangle r = tools.getBounds();
				r.width = getWidth();
				tools.setBounds(r);

				tagPane.updateWidth(r.width);

				if (loadAfterLayout != null) {
					EventQueue.invokeLater(new Runnable() {
						@Override
						public void run() {
							_load(loadAfterLayout);
							loadAfterLayout = null;
						}
					});
				}
			}
		});

		main.addMouseListener(new CustomMouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {
				unfocus();
			}
		});
	}

	// XXX always scale from original image?
	// XXX cache scaled image to get it again faster
	private Image getScaledImage(Image i, File sourceFile) {
		long w = getWidth() - kBorder * 4 - 12;
		long iw = i.getWidth(null);

		if (i.getWidth(null) > w) {
			float f = w / (float) iw;
			int scaledWidth = (int) (f * (float) iw);
			int scaledHeight = (int) (f * (float) i.getHeight(null));

			Image cached = scalingCache.get(sourceFile, scaledWidth, scaledHeight);
			if (cached != null) {
				return cached;
			}

			Image img = i.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_AREA_AVERAGING);
			scalingCache.put(sourceFile, scaledWidth, scaledHeight, img);

			return img;
		} else {
			return i;
		}
	}

	/*
	 * private void scaleImagesToEditorWidth() {
	 * 
	 * EventQueue.invokeLater(new Runnable() {
	 * 
	 * @Override public void run() { System.out.println("scaling getWidth=" +
	 * getWidth()); for (Object o : currentAttachments.keySet()) { if (o
	 * instanceof ImageIcon) { ImageIcon ii = (ImageIcon) o; if
	 * (ii.getIconWidth() > getWidth() - kBorder * 4) {
	 * ii.setImage(getScaledImage(ii.getImage())); } } } } }); }
	 */

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

		if (window.isShowingSearchResults()) {
			window.redoSearch();
		}

		clear();
	}

	public void clear() {
		currentNote = null;
		currentAttachments.clear();
		editor.clear();
		isDirty = false;
		visible(false);
	}

	public void load(final Note note) {
		if (getWidth() == 0) {
			loadAfterLayout = note;
		} else {
			_load(note);
		}
	}

	public void _load(Note note) {
		currentNote = note;
		currentAttachments.clear();

		Meta m = note.getMeta();
		editor.setTitle(m.title());
		editor.setText(note.contents());

		tagPane.load(Vault.getInstance().resolveTagIds(m.tags()));

		File[] files = currentNote.getAttachmentList();
		if (files != null) {
			for (File f : files) {
				if (f.getName().charAt(0) != '.' && f.isFile()) {
					int position = m.getAttachmentPosition(f);
					insertFileIntoNote(f, position);
				}
			}
		}

		editor.discardUndoBuffer();

		visible(true);

		Notebook nb = Vault.getInstance().findNotebook(note.file().getParentFile());
		currNotebook.setText(nb.name());

		trash.setVisible(!nb.folder().equals(Vault.getInstance().getTrash()));

		noteCreated.setText("Created: " + note.createdStr());
		noteUpdated.setText("Updated: " + note.updatedStr());

		caretChanged(editor.getTextPane());
	}

	private void visible(boolean b) {
		main.setVisible(b);
	}

	public boolean hasFocus() {
		return editor.hasFocus() || tagPane.hasFocus();
	}

	public void focusTags() {
		tagPane.requestFocus();
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

	private void renameAccordingToFormat(String title) {
		try {
			currentNote.attemptSafeRename(title + (editor.isRichText ? ".rtf" : ".txt"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void saveChanges() {
		if (!isDirty && !tagPane.isDirty()) {
			return;
		}

		if (currentNote != null) {
			boolean changed = false;
			boolean contentChanged = false;

			try {
				// Title
				String fileTitle = currentNote.getMeta().title();
				String editedTitle = editor.getTitle();
				if (!fileTitle.equals(editedTitle)) {
					currentNote.getMeta().title(editedTitle);
					renameAccordingToFormat(editedTitle);
					changed = true;
				}

				// Format
				if (!changed) {
					// Did format change during edit?
					String ext = FilenameUtils.getExtension(currentNote.file().getAbsolutePath()).toLowerCase();
					if ((editor.isRichText && "txt".equals(ext)) || (!editor.isRichText && "rtf".equals(ext))) {
						renameAccordingToFormat(editedTitle);
					}
				}

				// Tags
				if (tagPane.isDirty()) {
					List<String> tagNames = tagPane.getTagNames();
					List<String> tagIds = Vault.getInstance().resolveTagNames(tagNames);
					currentNote.getMeta().setTags(tagIds, tagNames);
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
				if (getWidth() > 0) {
					i = getScaledImage(i, f);
				} else {
					throw new AssertionError();
				}

				ImageIcon ii = new ImageIcon(i);

				if (position > noteArea.getDocument().getLength()) {
					position = 0;
				}

				try {
					noteArea.setCaretPosition(position);
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				}

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

	public void undo() {
		editor.undo();
	}

	public void redo() {
		editor.redo();
	}
}
