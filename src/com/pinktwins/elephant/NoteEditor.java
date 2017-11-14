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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JTextPane;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.TransferHandler;
import javax.swing.text.AbstractDocument.LeafElement;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Highlighter;

import org.apache.commons.lang3.SystemUtils;
import org.pegdown.PegDownProcessor;

import com.google.common.eventbus.Subscribe;
import com.pinktwins.elephant.CustomEditor.AttachmentInfo;
import com.pinktwins.elephant.data.Note;
import com.pinktwins.elephant.data.Note.Meta;
import com.pinktwins.elephant.data.Notebook;
import com.pinktwins.elephant.data.Vault;
import com.pinktwins.elephant.eventbus.TagsChangedEvent;
import com.pinktwins.elephant.eventbus.UIEvent;
import com.pinktwins.elephant.util.CustomMouseListener;
import com.pinktwins.elephant.util.Images;
import com.pinktwins.elephant.util.LaunchUtil;
import com.pinktwins.elephant.util.ResizeListener;
import com.pinktwins.elephant.util.ScreenUtil;
import com.pinktwins.elephant.util.SimpleImageInfo;

public class NoteEditor extends BackgroundPanel implements EditorEventListener {

	private static final Logger LOG = Logger.getLogger(NoteEditor.class.getName());

	public static final int kMinNoteSize = 288;

	private static Image tile, noteTopShadow, noteToolsNotebook, noteToolsTrash, noteToolsDivider;

	private ElephantWindow window;

	private boolean isDirty;

	private final int kNoteOffset = 64;
	private final int kBorder = 14;

	private Note loadAfterLayout = null;

	public static final ImageScalingCache scalingCache = new ImageScalingCache();

	public static final PegDownProcessor pegDown = new PegDownProcessor(
			org.pegdown.Parser.AUTOLINKS | org.pegdown.Parser.TABLES | org.pegdown.Parser.FENCED_CODE_BLOCKS | org.pegdown.Parser.DEFINITIONS);

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

	class EditorAttachmentTransferHandler extends AttachmentTransferHandler {
		public EditorAttachmentTransferHandler(EditorEventListener listener) {
			super(listener);
		}

		@Override
		public boolean canImport(TransferHandler.TransferSupport info) {
			if (editor.isShowingMarkdown()) {
				return false;
			}

			return true;
		}
	}

	class EditorWidthImageScaler implements ImageScaler {
		int adjust = (SystemUtils.IS_OS_WINDOWS || SystemUtils.IS_OS_LINUX) ? -20 : -12;

		@Override
		public Image scale(Image i, File source) {
			return getScaledImage(i, source, adjust, true);
		}

		@Override
		public Image getCachedScale(File source) {
			return getScaledImageCacheOnly(source, adjust, true);
		}

		@Override
		public long getTargetWidth() {
			return getUsableEditorWidth() + adjust;
		}
	}

	class ImageAttachmentImageScaler implements ImageScaler {
		public Image scale(Image i, File source) {
			return getScaledImage(i, source, 0, false);
		}

		@Override
		public Image getCachedScale(File source) {
			return getScaledImageCacheOnly(source, 0, false);
		}

		@Override
		public long getTargetWidth() {
			return getUsableEditorWidth();
		}
	}

	class EditorController {
		public void scrollTo(int value) {
			scroll.getVerticalScrollBar().setValue(value);
		}

		public void lockScrolling(boolean value) {
			scroll.setLocked(value);
		}

		public int noteHash() {
			if (currentNote == null) {
				return 0;
			} else {
				return currentNote.hashCode();
			}
		}
	}

	EditorWidthImageScaler editorWidthScaler = new EditorWidthImageScaler();
	ImageAttachmentImageScaler imageAttachmentImageScaler = new ImageAttachmentImageScaler();
	EditorController editorController = new EditorController();

	NoteEditorStateListener stateListener;

	private Note currentNote, previousNote;
	private NoteAttachments attachments = new NoteAttachments();

	JPanel main, area;
	ScrollablePanel areaHolder;
	BackgroundPanel scrollHolder;
	CustomScrollPane scroll;
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
			g.drawImage(noteToolsDivider, 14, 32, getWidth() - 28, 2, null);
		}
	}

	private class TopShadowPanel extends JPanel {
		private static final long serialVersionUID = 6626079564069649611L;

		private final Color lineColor = Color.decode("#b4b4b4");

		@Override
		public void paint(Graphics g) {
			super.paint(g);

			JScrollBar v = scroll.getVerticalScrollBar();
			if (!v.isVisible()) {
				g.drawImage(noteTopShadow, 0, kNoteOffset + 1, getWidth(), 4, null);
			} else {
				if (v.getValue() < 4) {
					int adjust = scroll.isLocked() ? 0 : kBorder + 1;
					g.drawImage(noteTopShadow, 0, kNoteOffset + 1, getWidth() - adjust, 4, null);
				} else {
					g.drawImage(noteTopShadow, 0, kNoteOffset + 1, getWidth(), 2, null);
				}
			}

			g.setColor(lineColor);
			g.drawLine(0, 0, 0, getHeight());
		}
	}

	// Custom panel to fix note editor width to window width.
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

	public void addStateListener(NoteEditorStateListener l) {
		stateListener = l;
	}

	public NoteEditor(ElephantWindow w) {
		super(tile);
		window = w;

		Elephant.eventBus.register(this);

		createComponents();
	}

	public void cleanup() {
		Elephant.eventBus.unregister(this);
		editor.cleanup();
		window = null;
		stateListener = null;
	}

	private void createComponents() {
		main = new TopShadowPanel();
		main.setLayout(null);
		main.setBorder(BorderFactory.createEmptyBorder(kBorder, kBorder, kBorder, kBorder));

		final DividedPanel tools = new DividedPanel(tile);
		tools.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
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
		areaHolder.setBorder(BorderFactory.createEmptyBorder(kBorder - topBorderOffset, kBorder - 1, kBorder, kBorder));
		areaHolder.add(area, BorderLayout.NORTH);

		scrollHolder = new BackgroundPanel();
		scrollHolder.setOpaque(false);

		scroll = new CustomScrollPane(areaHolder);
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

		scroll.addMouseListener(new CustomMouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {
				unfocus();

				// If we switch out from markdown-editing to rich display,
				// the unfocus happens too late to actually save edits.
				// This UIEvent marks a savepoint.
				new UIEvent(UIEvent.Kind.editorWillChangeNote).post();

				if (currentNote.isMarkdown()) {
					window.showNote(currentNote);
				}
			}
		});

		main.setTransferHandler(new EditorAttachmentTransferHandler(this));
	}

	private long getUsableEditorWidth() {
		if (!ScreenUtil.isRetina()) {
			return getWidth() - kBorder * 4 - 12;
		}

		// Retina
		return getWidth() * 2 - 147; // XXX: magic number: 147
	}

	private Image getScaledImageCacheOnly(File sourceFile, int widthOffset, boolean useFullWidth) {
		SimpleImageInfo info;
		try {
			info = new SimpleImageInfo(sourceFile);
		} catch (IOException e) {
			LOG.severe("Fail: " + e);
			return null;
		}

		long w = getUsableEditorWidth() + widthOffset;
		long iw = info.getWidth();
		long ih = info.getHeight();

		if (ScreenUtil.isRetina() && !useFullWidth && iw < w) {
			// Retina screen, requested NOT full editor width scaling,
			// image is smaller than editor width. Need to scale using
			// 2x image size, otherwise image would show up half as small.
			iw *= 2;
			ih *= 2;

			float f;
			if (iw < w) {
				f = 1;
			} else {
				f = w / (float) iw;
			}
			int scaledWidth = (int) (f * (float) iw);
			int scaledHeight = (int) (f * (float) ih);

			return scalingCache.get(sourceFile, scaledWidth, scaledHeight);
		}

		if (useFullWidth || iw > w) {
			float f = w / (float) iw;
			int scaledWidth = (int) (f * (float) iw);
			int scaledHeight = (int) (f * (float) ih);

			return scalingCache.get(sourceFile, scaledWidth, scaledHeight);
		}

		return null;
	}

	private Image getScaledImage(Image i, File sourceFile, int widthOffset, boolean useFullWidth) {
		long w = getUsableEditorWidth() + widthOffset;
		long iw = i.getWidth(null);
		long ih = i.getHeight(null);

		if (ScreenUtil.isRetina() && !useFullWidth && iw < w) {
			// Retina screen, requested NOT full editor width scaling,
			// image is smaller than editor width. Need to scale using
			// 2x image size, otherwise image would show up half as small.
			iw *= 2;
			ih *= 2;

			float f;
			if (iw < w) {
				f = 1;
			} else {
				f = w / (float) iw;
			}
			int scaledWidth = (int) (f * (float) iw);
			int scaledHeight = (int) (f * (float) ih);

			Image cached = scalingCache.get(sourceFile, scaledWidth, scaledHeight);
			if (cached != null) {
				return cached;
			}

			Image img = i.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_AREA_AVERAGING);
			scalingCache.put(sourceFile, scaledWidth, scaledHeight, img);

			return img;
		}

		if (useFullWidth || iw > w) {
			// Scale to editor width
			float f = w / (float) iw;
			int scaledWidth = (int) (f * (float) iw);
			int scaledHeight = (int) (f * (float) ih);

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

	public void openNotebookChooserForMoving() {
		if (currentNote != null) {
			NotebookChooser nbc = new NotebookChooser(window, String.format("Move \"%s\"", editor.getTitle()));

			// Center on window
			Point p = main.getLocationOnScreen();
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

		int index = window.getIndexOfFirstSelectedNoteInNoteList();

		window.sortAndUpdate();

		if (window.isShowingSearchResults()) {
			window.redoSearch();
		}

		if (window.isShowingAllNotes()) {
			window.showAllNotes();
		}

		clear();

		if (index >= 0) {
			window.selectNoteByIndex(index);
		}
	}

	public boolean isDirty() {
		return isDirty;
	}

	public void clear() {
		currentNote = null;
		attachments = new NoteAttachments();
		editor.saveSelection();
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

		if (note == null) {
			return;
		}

		if (!note.file().exists()) {
			// XXX Tell user what is going on.
			clear();
			return;
		}

		currentNote = note;
		attachments = new NoteAttachments();

		Meta m = note.getMeta();

		editor.setTitle(m.title());
		editor.setText(note.contents());
		editor.setMarkdown(note.isMarkdown());

		tagPane.load(Vault.getInstance().resolveTagIds(m.tags()));

		List<Note.AttachmentInfo> info = currentNote.getAttachmentList();
		if (!info.isEmpty()) {

			// We need to insert attachments from end to start - thus, sort.
			Collections.reverse(info);

			for (Note.AttachmentInfo ap : info) {

				// If position to insert attachment into would have
				// component content already, it would be overwritten.
				// Make sure there is none.
				AttributeSet as = editor.getAttributes(ap.position);
				if (as instanceof LeafElement) {
					LeafElement l = (LeafElement) as;
					if (!"content".equals(l.getName())) {
						editor.insertNewline(ap.position);
					}
				}

				attachments.insertFileIntoNote(this, ap.f, ap.position);
			}
		}

		attachments.loaded();

		editor.discardUndoBuffer();

		if (note.isMarkdown()) {
			String contents = note.contents();
			String html = pegDown.markdownToHtml(editor.isRichText ? Note.plainTextContents(contents) : contents);
			editor.displayHtml(currentNote.file(), html);
		}

		if (note.isHtml()) {
			editor.displayBrowser(currentNote.file());
		}

		visible(true);

		Notebook nb = Vault.getInstance().findNotebook(note.file().getParentFile());
		currNotebook.setText(nb.name());

		// trash.setVisible(!nb.folder().equals(Vault.getInstance().getTrash()));

		noteCreated.setText("Created: " + note.createdStr());
		noteUpdated.setText("Updated: " + note.updatedStr());

		caretChanged(editor.getTextPane());

		if (previousNote != null && previousNote.equals(note)) {
			editor.restoreSelection();
		}

		previousNote = currentNote;
	}

	private void reloadTags() {
		Meta m = currentNote.getMeta();
		tagPane.load(Vault.getInstance().resolveTagIds(m.tags()));
	}

	private void reloadDates() {
		noteCreated.setText("Created: " + currentNote.createdStr());
		noteUpdated.setText("Updated: " + currentNote.updatedStr());
	}

	public int reloadWordCount() {
		return currentNote.updateWordCount();
	}

	public void focusQuickLook() {
		for (Object o : attachments.keySet()) {
			if (o instanceof FileAttachment) {
				FileAttachment fa = (FileAttachment) o;
				fa.focusQuickLook();
				break;
			}
		}
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
	public void handleTagsChangedEvent(TagsChangedEvent event) {
		// some tags were added/changed. Not neccessarily this note's tags,
		// but might have, so reload tags.
		if (currentNote != null) {
			reloadTags();
		}
	}

	@Subscribe
	public void handleUIEvent(UIEvent event) {
		if (event.kind == UIEvent.Kind.editorWillChangeNote) {
			saveChanges();
		}
	}

	public void saveChanges() {
		if (!isDirty && !tagPane.isDirty()) {
			return;
		}

		SaveChanges.saveChanges(currentNote, attachments, this, tagPane);

		attachments.loaded();
		isDirty = false;

		scroll.setLocked(true);
		scroll.unlockAfter(100);

		reloadDates();
	}

	public void focusTitle() {
		editor.focusTitle();
	}

	public void focusEditor() {
		editor.getTextPane().requestFocusInWindow();
	}

	@Override
	public void editingFocusGained() {
		isDirty = true;
		JTextPane p = editor.getEditorPane();
		if (p != null) {
			Highlighter h = p.getHighlighter();
			if (h != null) {
				h.removeAllHighlights();
			}
		}
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

				// When editor is unfocused, caret should only change when
				// inserting images/attachments to document during loading.
				// We want to see the top of note after all loading is done,
				// so keep vertical scroll bar at 0.
				//
				// Only exception to this should be dropping a file,
				// which can change caret position while editor
				// is unfocused. editor.maybeImporting() tracks
				// drag'n'drop state - true indicates a drop might
				// be in progress, and we need to keep scroll value.
				if (!editor.isFocusOwner() && !editor.maybeImporting()) {
					scroll.getVerticalScrollBar().setValue(0);
				}

				// Writing new lines, keep scroll to bottom
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

	private void insertMarkdownLink(File f) {
		boolean isImage = Images.isImage(f);
		String name = f.getName();
		String encName = name;
		try {
			encName = URLEncoder.encode(name, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			LOG.severe("Fail: " + e);
		}
		encName = encName.replace("+", "%20");

		JTextPane tp = editor.getTextPane();
		try {
			tp.getDocument().insertString(tp.getCaretPosition(), String.format("%s[%s](%s \"\")\n", isImage ? "!" : "", name, encName), null);
		} catch (BadLocationException e) {
			LOG.severe("Fail: " + e);
		}
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
					isDirty = true;

					File attached = currentNote.importAttachment(f);
					currentNote.getMeta().setAttachmentPosition(attached, noteArea.getCaretPosition());

					attachments.insertFileIntoNote(this, attached, noteArea.getCaretPosition());

					if (currentNote.isMarkdown()) {
						insertMarkdownLink(f);
					}

					// Insert linefeed for each dropped file
					try {
						noteArea.getDocument().insertString(noteArea.getCaretPosition() + 1, "\n", null);
					} catch (BadLocationException e) {
						LOG.severe("Fail: " + e);
					}

					// If note is unnamed, use file name
					if (currentNote.getMeta().title().equals("Untitled")) {
						editor.setTitle(f.getName());
					}

					// Update modify time
					currentNote.file().setLastModified(System.currentTimeMillis());

				} catch (IOException e) {
					LOG.severe("Fail: " + e);
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
		editor.getTextPane().requestFocusInWindow();
	}

	public void encryptAction() {
		editor.encryptSelection();
	}

	public void decryptAction() {
		editor.decryptSelection();
	}

	public void undo() {
		editor.undo();
	}

	public void redo() {
		editor.redo();
	}

	public void updateUndoState() {
		editor.updateUndoState();
	}

	private void turnToPlainText_format() {
		List<AttachmentInfo> info = editor.getAttachmentInfo();
		List<AttachmentInfo> info_reverse = editor.removeAttachmentElements(info);
		editor.turnToPlainText();
		importAttachments(info_reverse);
	}

	public void turnToPlainText() {
		turnToPlainText_format();
		/*
		 * try { currentNote.attemptSafeRename(editor.getTitle() + ".txt"); editor.setMarkdown(false); } catch (IOException e) {
		 * e.printStackTrace(); }
		 */
	}

	/*
	 * somehow I just dont like this. public void turnToMarkdown() { if (!currentNote.isMarkdown()) {
	 * turnToPlainText_format(); try { currentNote.attemptSafeRename(editor.getTitle() + ".md"); editor.setMarkdown(true); }
	 * catch (IOException e) { e.printStackTrace(); } } }
	 */

	public void importAttachments(List<AttachmentInfo> info) {
		for (AttachmentInfo i : info) {
			File f = attachments.get(i.object);
			if (f != null) {
				// Use note.att-path in case note was renamed
				File ff = new File(currentNote.attachmentFolderPath() + File.separator + f.getName());

				// remove previous object mapping,
				attachments.remove(i.object);
				// insert.. will map a new Object -> File
				attachments.insertFileIntoNote(this, ff, i.startPosition);
			}
		}
	}

	@Override
	public void attachmentClicked(MouseEvent event, Object attachmentObject) {
		if (attachmentObject != null && event.getClickCount() == 2) {
			LaunchUtil.launch(attachments.get(attachmentObject));
		}
	}

	@Override
	public void attachmentMoved(AttachmentInfo info) {
		attachments.makeDirty();
	}

	public CustomEditor getEditor() {
		return editor;
	}
}
