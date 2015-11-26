package com.pinktwins.elephant;

import java.awt.Color;
import java.awt.Container;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.pinktwins.elephant.data.Note;
import com.pinktwins.elephant.util.Factory;
import com.pinktwins.elephant.util.Images;
import com.pinktwins.elephant.util.PdfUtil;
import com.pinktwins.elephant.util.SimpleImageInfo;

abstract class NoteItem extends JPanel implements Comparable<NoteItem>, MouseListener {

	private static final Logger LOG = Logger.getLogger(NoteItem.class.getName());

	interface NoteItemListener {
		public void noteClicked(NoteItem item, boolean doubleClick, MouseEvent e);
	}

	private static final DateTimeFormatter df = DateTimeFormat.forPattern("dd/MM/yy").withLocale(Locale.getDefault());
	private static final long time_24h = 1000L * 60 * 60 * 24;
	private static final Map<File, NoteItem> itemCache = Factory.newHashMap();
	protected static final Color kColorNoteBorder = Color.decode("#cdcdcd");

	protected static Image noteShadow, noteSelection;

	public Note note;
	protected NoteList.ListModes listMode;

	protected JTextPane preview;
	protected BackgroundPanel root;

	protected boolean isSelected = false;

	static {
		Iterator<Image> i = Images.iterator(new String[] { "noteShadow", "noteSelection" });
		noteShadow = i.next();
		noteSelection = i.next();
	}

	@Override
	public int compareTo(NoteItem o) {
		return note.compareTo(o.note);
	}

	public static void removeCacheKey(File f) {
		itemCache.remove(f);
	}

	public static void clearItemCache() {
		itemCache.clear();
	}

	public static NoteItem itemOf(Note n, NoteList.ListModes listMode) {
		NoteItem item = itemCache.get(n.file());

		// If cached item is attached in another ElephantWindow,
		// we need a new instance.
		if (item != null && item.getParent() != null) {
			item = null;
		}

		if (item == null) {
			item = itemOfNoteForListMode(n, listMode);
			itemCache.put(n.file(), item);
		}

		if (itemCache.size() > 15000) {
			// XXX nonsensical purge algo
			boolean t = false;
			File[] keys = new File[0];
			keys = itemCache.keySet().toArray(keys);
			for (File old : keys) {
				if (t) {
					itemCache.remove(old);
				}
				t = !t;
			}
		}

		return item;
	}

	synchronized private static NoteItem itemOfNoteForListMode(Note n, NoteList.ListModes listMode) {
		switch (listMode) {
		case CARDVIEW:
			return new CardViewNoteItem(n);
		case SNIPPETVIEW:
			return new SnippetViewNoteItem(n);
		default:
			throw new AssertionError("Invalid listMode!");
		}
	}

	protected NoteItem(Note n, NoteList.ListModes listMode) {
		super();
		note = n;
		this.listMode = listMode;
	}

	protected void createPreviewComponents(JPanel previewPane) {
		previewPane.removeAll();

		if (note.isMarkdown()) {
			preview = new HtmlPane(note.file(), null);
		} else {
			preview = new JTextPane();
		}
		preview.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));
		preview.setEditable(false);
		preview.setFocusable(false);
		preview.setFont(ElephantWindow.fontMediumPlus);
		preview.setForeground(ElephantWindow.colorPreviewGray);
		preview.setBackground(Color.WHITE);
		preview.setBounds(0, 0, 176, 138);
		preview.addMouseListener(this);

		// time
		String ts = "";
		Color col = ElephantWindow.colorGreen;

		long now = System.currentTimeMillis();
		Date noteDate = new Date(note.lastModified());

		boolean today = DateUtils.isSameDay(new Date(now), noteDate);
		if (today) {
			ts = "Today";
		} else {
			boolean yesterday = DateUtils.isSameDay(new Date(now - time_24h), noteDate);
			if (yesterday) {
				ts = "Yesterday";
			} else {
				ts = df.print(note.lastModified());
			}
		}

		if (now - note.lastModified() > time_24h * 30) {
			col = ElephantWindow.colorBlue;
		}

		Style style = preview.addStyle("timestampStyle", null);
		StyleConstants.setForeground(style, col);
		
		if (note.isMarkdown()) {
			String htmlts = "<p style='margin:0;padding:0;color:" + String.format("#%02x%02x%02x", col.getRed(), col.getGreen(), col.getBlue()) +"'>" + ts + "</p>\n";
			String contents = htmlts + note.contents();
			String html = NoteEditor.pegDown.markdownToHtml(contents);
			// hint by http://stackoverflow.com/a/19785465/873282
			preview.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
			preview.setText(html);
		} else {
			CustomEditor.setTextRtfOrPlain(preview, getContentPreview());
			try {
				preview.getDocument().insertString(0, ts + " ", style);
			} catch (BadLocationException e) {
				LOG.severe("Fail: " + e);
			}
		}

		previewPane.add(preview);

		// Picture thumbnail.
		for (Note.AttachmentInfo i : note.getAttachmentList()) {
			String ext = FilenameUtils.getExtension(i.f.getAbsolutePath()).toLowerCase();

			if (Images.isImage(i.f)) {
				if (addPictureThumbnail(i.f)) {
					break;
				}
			}

			if ("pdf".equals(ext)) {
				File[] files = FileAttachment.previewFiles(i.f);
				boolean done = false;
				for (File ff : files) {
					if (Images.isImage(ff)) {
						if (addPictureThumbnail(ff)) {
							done = true;
							break;
						}
					}
				}
				if (done) {
					break;
				}
				File previewDir = FileAttachment.getPreviewDirectory(i.f);
				previewDir.mkdirs();
				if (previewDir.exists()) {
					PdfUtil pdf = new PdfUtil(i.f);
					if (pdf.numPages() > 0) {
						File ff = FileAttachment.getPreviewFileForPage(previewDir, 1);
						if (pdf.writePage(1, ff) != null) {
							addPictureThumbnail(ff);
						}
						pdf.close();
						break;
					}
				}
			}
		}
	}

	protected Image getPictureThumbnail(File f) {
		try {
			int w, h;

			switch (listMode) {
			case CARDVIEW:
				SimpleImageInfo info = new SimpleImageInfo(f);

				float scale = info.getWidth() / (float) (196 - 12 - 4);
				w = (int) (info.getWidth() / scale);
				h = (int) ((float) info.getHeight() / scale);
				break;
			case SNIPPETVIEW:
				w = 75;
				h = 75;
				break;
			default:
				throw new AssertionError();
			}

			Image scaled = NoteEditor.scalingCache.get(f, w, h);
			if (scaled == null) {
				Image i = ImageIO.read(f);
				if (i != null) {
					scaled = i.getScaledInstance(w, h, Image.SCALE_AREA_AVERAGING);
					NoteEditor.scalingCache.put(f, w, h, scaled);
				}
			}

			return scaled;
		} catch (IOException e) {
			LOG.severe("Fail: " + e);
		}

		return null;
	}

	abstract protected boolean addPictureThumbnail(File f);

	private NoteItemListener getNoteItemListenerFromParentTree() {
		Container c = getParent();
		while (c != null && !(c instanceof NoteItemListener)) {
			c = c.getParent();
		}
		return (NoteItemListener) c;
	}

	abstract protected String getContentPreview();

	public void setSelected(boolean b) {
		if (b) {
			root.setImage(noteSelection);
		} else {
			root.setImage(noteShadow);
		}
		isSelected = b;
		repaint();
	}

	public boolean isSelected() {
		return isSelected;
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mousePressed(MouseEvent e) {
		NoteItemListener l = getNoteItemListenerFromParentTree();
		if (l != null) {
			l.noteClicked(this, e.getClickCount() == 2, e);
		}
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
