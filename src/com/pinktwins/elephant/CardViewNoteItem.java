package com.pinktwins.elephant;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.pinktwins.elephant.data.Note;

public class CardViewNoteItem extends NoteItem {

	private JPanel previewPane;
	private Dimension size = new Dimension(196, 196);

	public CardViewNoteItem(Note n) {
		super(n, NoteList.ListModes.CARDVIEW);
		setLayout(new BorderLayout());

		root = new BackgroundPanel(noteShadow, 2);
		root.setBorder(BorderFactory.createEmptyBorder(7, 7, 7, 7));
		root.setMinimumSize(size);
		root.setMaximumSize(size);
		root.keepScaleOnRetina(true, true);

		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		p.setBackground(Color.WHITE);
		p.setBorder(BorderFactory.createLineBorder(kColorNoteBorder, 1));

		JLabel name = new JLabel(n.getMeta().title());
		name.setFont(ElephantWindow.fontH1);
		name.setBorder(BorderFactory.createEmptyBorder(12, 12, 8, 12));
		p.add(name, BorderLayout.NORTH);

		previewPane = new JPanel();
		previewPane.setLayout(null);
		previewPane.setBackground(Color.WHITE);

		createPreviewComponents(previewPane);

		p.add(previewPane, BorderLayout.CENTER);
		root.addOpaque(p, BorderLayout.CENTER);
		add(root, BorderLayout.CENTER);

		p.addMouseListener(this);
	}

	@Override
	protected String getContentPreview() {
		String contents = note.contents().trim();
		if (contents.length() > 200) {
			contents = contents.substring(0, 200) + "…";
		}
		return contents;
	}

	@Override
	protected boolean addPictureThumbnail(File f) {
		Image scaled = getPictureThumbnail(f);
		if (scaled != null) {
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
		return false;
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

}
