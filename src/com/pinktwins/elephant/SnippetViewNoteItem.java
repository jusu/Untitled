package com.pinktwins.elephant;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.io.File;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;

import com.pinktwins.elephant.data.Note;
import com.pinktwins.elephant.util.Images;
import com.pinktwins.elephant.util.ScreenUtil;

public class SnippetViewNoteItem extends NoteItem {

	private static final Image noteBg, noteBgSelected;
	private static final Color kColorNoteHighlight = Color.decode("#f5f5f5");
	private static final Color kColorSnippetTitle = Color.decode("#323232");

	static {
		Iterator<Image> i = Images.iterator(new String[] { "notelist", "snippetNoteBgSelected" });
		noteBg = i.next();
		noteBgSelected = i.next();
	}

	private JPanel previewPane;

	public SnippetViewNoteItem(Note n) {
		super(n, NoteList.ListModes.SNIPPETVIEW);

		setLayout(new BorderLayout());

		root = new BackgroundPanel(noteBg, BackgroundPanel.TILED);
		root.setBorder(ElephantWindow.emptyBorder);

		JPanel p = new JPanel();
		p.setOpaque(false);
		p.setLayout(new BorderLayout());

		JLabel name = new JLabel(n.getMeta().title());
		name.setFont(ElephantWindow.fontH2);
		name.setForeground(kColorSnippetTitle);
		name.setBorder(BorderFactory.createEmptyBorder(12, 10, 2, 2));

		previewPane = new JPanel();
		previewPane.setLayout(null);
		previewPane.setOpaque(false);
		previewPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));

		createPreviewComponents(previewPane);

		preview.setBorder(BorderFactory.createEmptyBorder(0, 10, 14, 12));

		// XXX code abomination
		Component[] comps = previewPane.getComponents();

		Component text = comps[0];
		previewPane.remove(text);
		Component image = null;
		if (comps.length == 2) {
			image = comps[1];
			previewPane.remove(image);
		}

		((JTextPane) text).setOpaque(false);
		((JTextPane) text).setFont(ElephantWindow.fontEditor);

		previewPane.setLayout(new BorderLayout());
		previewPane.add(name, BorderLayout.NORTH);
		previewPane.add(text, BorderLayout.CENTER);

		p.add(previewPane, BorderLayout.CENTER);
		root.addOpaque(p, BorderLayout.CENTER);

		if (image != null) {
			((JPanel) image).setOpaque(false);

			JPanel borders = new JPanel(null);
			borders.setOpaque(false);
			borders.add(image, BorderLayout.CENTER);
			image.setBounds(5, 5, 75, 75);
			borders.setPreferredSize(new Dimension(85, 85));

			p.add(borders, BorderLayout.EAST);
		}

		add(root, BorderLayout.CENTER);

		p.addMouseListener(this);
	}

	@Override
	protected String getContentPreview() {
		String contents = note.contents().trim();
		if (contents.length() > 600) {
			contents = contents.substring(0, 600) + "…";
		}
		return contents;
	}

	@Override
	protected boolean addPictureThumbnail(File f) {
		Image scaled = getPictureThumbnail(f);
		if (scaled != null) {
			JLabel l = new JLabel("");
			l.setIcon(new ImageIcon(scaled));
			l.setBounds(0, 0, 75, 75);

			JPanel pa = new JPanel(null);
			pa.setBorder(ElephantWindow.emptyBorder);
			pa.setBackground(Color.WHITE);
			pa.add(l);

			previewPane.add(pa);
			return true;
		}
		return false;
	}

	@Override
	public void setSelected(boolean b) {
		if (b) {
			root.setImage(noteBgSelected);
			root.keepScaleOnRetina(false, true);
		} else {
			root.setImage(noteBg);
			root.keepScaleOnRetina(false, false);
		}
		isSelected = b;
		repaint();
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);

		if (!isSelected) {
			int y = 1;
			if (ScreenUtil.isRetina()) {
				y = 0;
			}

			g.setColor(kColorNoteBorder);
			g.drawLine(0, getHeight() - y, getWidth() - 1, getHeight() - y);
			
			g.setColor(kColorNoteHighlight);
			g.drawLine(0, 0, getWidth() - 1, 0);
		}
	}
}
