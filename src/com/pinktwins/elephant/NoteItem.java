package com.pinktwins.elephant;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

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

class NoteItem extends JPanel implements MouseListener {

	interface NoteItemListener {
		public void noteClicked(NoteItem item);
	}

	final static private DateTimeFormatter df = DateTimeFormat.forPattern("dd/MM/yy").withLocale(Locale.getDefault());
	final static private long time_24h = 1000 * 60 * 60 * 24;
	final static private Color kColorNoteBorder = Color.decode("#cdcdcd");
	final static private HashMap<File, NoteItem> itemCache = Factory.newHashMap();

	static private Image noteShadow, noteSelection;

	public Note note;
	private Dimension size = new Dimension(196, 196);
	private JLabel name;
	private JTextPane preview;
	private JPanel previewPane;
	private BackgroundPanel root;

	final private NoteItemListener itemListener;

	static {
		Iterator<Image> i = Images.iterator(new String[] { "noteShadow", "noteSelection" });
		noteShadow = i.next();
		noteSelection = i.next();
	}

	static public NoteItem itemOf(Note n, NoteItemListener l) {
		NoteItem item = itemCache.get(n.file());
		if (item == null) {
			item = new NoteItem(n, l);
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

	private NoteItem(Note n, NoteItemListener l) {
		super();

		note = n;
		itemListener = l;

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

		preview = new JTextPane();
		preview.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));
		preview.setEditable(false);
		preview.setFont(ElephantWindow.fontMediumPlus);
		preview.setForeground(ElephantWindow.colorPreviewGray);
		CustomEditor.setTextRtfOrPlain(preview, getContentPreview());
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
		try {
			preview.getDocument().insertString(0, ts + " ", style);
		} catch (BadLocationException e1) {
			e1.printStackTrace();
		}

		previewPane.add(preview);

		// Picture thumbnail.
		// XXX with many notes, this absolutely must be postponed.
		for (File f : note.getAttachmentList()) {
			String ext = FilenameUtils.getExtension(f.getAbsolutePath()).toLowerCase();
			if ("png".equals(ext) || "jpg".equals(ext) || "gif".equals(ext)) {
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
						break;
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

	private String getContentPreview() {
		String contents = note.contents().trim();
		if (contents.length() > 200) {
			contents = contents.substring(0, 200) + "…";
		}
		return contents;
	}

	public void updateThumb() {
		name.setText(note.getMeta().title());
		createPreviewComponents();
	}

	public void setSelected(boolean b) {
		if (b) {
			root.setImage(noteSelection);
		} else {
			root.setImage(noteShadow);
		}
		repaint();
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
		if (e.getClickCount() == 2) {
			// XXX open note in new window
		}
	}

	@Override
	public void mousePressed(MouseEvent e) {
		itemListener.noteClicked(this);
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
