package com.pinktwins.elephant;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
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
import javax.swing.ImageIcon;
import javax.swing.JLabel;
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

class NoteItem extends JPanel implements Comparable<NoteItem>, MouseListener {

	private static final Logger LOG = Logger.getLogger(NoteItem.class.getName());

	interface NoteItemListener {
		public void noteClicked(NoteItem item, boolean doubleClick, MouseEvent e);
	}

	private static final DateTimeFormatter df = DateTimeFormat.forPattern("dd/MM/yy").withLocale(Locale.getDefault());
	private static final long time_24h = 1000L * 60 * 60 * 24;
	private static final Color kColorNoteBorder = Color.decode("#cdcdcd");
	private static final Map<File, NoteItem> itemCache = Factory.newHashMap();

	private static Image noteShadow, noteSelection;

	public Note note;
	private Dimension size = new Dimension(196, 196);
	private JLabel name;
	private JTextPane preview;
	private JPanel previewPane;
	private BackgroundPanel root;

	private boolean isSelected = false;

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

	public static NoteItem itemOf(Note n) {
		NoteItem item = itemCache.get(n.file());

		// If cached item is attached in another ElephantWindow,
		// we need a new instance.
		if (item != null && item.getParent() != null) {
			item = null;
		}

		if (item == null) {
			item = new NoteItem(n);
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

	private NoteItem(Note n) {
		super();

		note = n;

		setLayout(new BorderLayout());

		root = new BackgroundPanel(noteShadow, 2);
		root.setBorder(BorderFactory.createEmptyBorder(7, 7, 7, 7));
		root.setMinimumSize(size);
		root.setMaximumSize(size);

		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		p.setBackground(Color.WHITE);
		p.setBorder(BorderFactory.createLineBorder(kColorNoteBorder, 1));

		name = new JLabel(n.getMeta().title());
		name.setFont(ElephantWindow.fontH1);
		name.setBorder(BorderFactory.createEmptyBorder(12, 12, 8, 12));
		p.add(name, BorderLayout.NORTH);

		previewPane = new JPanel();
		previewPane.setLayout(null);
		previewPane.setBackground(Color.WHITE);

		createPreviewComponents();

		p.add(previewPane, BorderLayout.CENTER);
		root.addOpaque(p, BorderLayout.CENTER);
		add(root, BorderLayout.CENTER);

		p.addMouseListener(this);
	}

	private void createPreviewComponents() {
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

		if (note.isMarkdown()) {
			String contents = note.contents();
			String html = NoteEditor.pegDown.markdownToHtml(contents);
			preview.setText(html);
		} else {
			CustomEditor.setTextRtfOrPlain(preview, getContentPreview());
		}

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
		try {
			preview.getDocument().insertString(0, ts + " ", style);
		} catch (BadLocationException e) {
			LOG.severe("Fail: " + e);
		}

		previewPane.add(preview);

		// Picture thumbnail.
		for (Note.AttachmentInfo i : note.getAttachmentList()) {
			String ext = FilenameUtils.getExtension(i.f.getAbsolutePath()).toLowerCase();
			if ("png".equals(ext) || "jpg".equals(ext) || "gif".equals(ext)) {
				if (addPictureThumbnail(i.f)) {
					break;
				}
			}

			if ("pdf".equals(ext)) {
				File[] files = FileAttachment.previewFiles(i.f);
				boolean done = false;
				for (File ff : files) {
					ext = FilenameUtils.getExtension(ff.getAbsolutePath()).toLowerCase();
					if ("png".equals(ext) || "jpg".equals(ext) || "jpeg".equals(ext) || "gif".equals(ext)) {
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

	private boolean addPictureThumbnail(File f) {
		try {
			Image i = ImageIO.read(f);
			if (i != null) {
				float scale = i.getWidth(null) / (float) (196 - 12 - 4);
				int w = (int) (i.getWidth(null) / scale);
				int h = (int) ((float) i.getHeight(null) / scale);

				Image scaled = NoteEditor.scalingCache.get(f, w, h);
				if (scaled == null) {
					scaled = i.getScaledInstance(w, h, Image.SCALE_AREA_AVERAGING);
					NoteEditor.scalingCache.put(f, w, h, scaled);
				}

				JLabel l = new JLabel("");
				l.setIcon(new ImageIcon(scaled));
				l.setBounds(0, 4, 190, 99);

				JPanel pa = new JPanel(null);
				pa.setBorder(ElephantWindow.emptyBorder);
				pa.setBackground(Color.WHITE);
				pa.add(l);

				preview.setBounds(0, 0, 176, 40);
				pa.setBounds(0, 40, 190, 103);

				previewPane.add(pa);
				return true;
			}
		} catch (IOException e) {
			LOG.severe("Fail: " + e);
		}

		return false;
	}

	private NoteItemListener getNoteItemListenerFromParentTree() {
		Container c = getParent();
		while (c != null && !(c instanceof NoteItemListener)) {
			c = c.getParent();
		}
		return (NoteItemListener) c;
	}

	private String getContentPreview() {
		String contents = note.contents().trim();
		if (contents.length() > 200) {
			contents = contents.substring(0, 200) + "…";
		}
		return contents;
	}

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
	public Dimension getPreferredSize() {
		return size;
	}

	@Override
	public Dimension getMinimumSize() {
		return size;
	}

	@Override
	public Dimension getMaximumSize() {
		return size;
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
