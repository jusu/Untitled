package com.pinktwins.elephant;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.Image;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import com.pinktwins.elephant.util.Images;

public class NoteListUI {

	private static Image iAllNotes;

	static {
		Iterator<Image> i = Images.iterator(new String[] { "allNotes" });
		iAllNotes = i.next();
	}

	JScrollPane scroll;
	JPanel main, allNotesPanel, fillerPanel, sep;
	JLabel currentName;
	JButton allNotes;

	public NoteListUI(Container parent) {
		final JPanel title = new JPanel(new BorderLayout());
		title.setBorder(ElephantWindow.emptyBorder);

		allNotes = new JButton("");
		allNotes.setIcon(new ImageIcon(iAllNotes));
		allNotes.setBorderPainted(false);
		allNotes.setContentAreaFilled(false);

		allNotesPanel = new JPanel(new GridLayout(1, 1));
		allNotesPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		allNotesPanel.add(allNotes);

		fillerPanel = new JPanel(new GridLayout(1, 1));

		currentName = new JLabel("", JLabel.CENTER);
		currentName.setBorder(BorderFactory.createEmptyBorder(13, 0, 9, 0));
		currentName.setFont(ElephantWindow.fontTitle);
		currentName.setForeground(ElephantWindow.colorTitle);

		sep = new JPanel(null);
		sep.setBounds(0, 0, 1920, 1);
		sep.setBackground(Color.decode("#cccccc"));

		title.add(allNotesPanel, BorderLayout.WEST);
		title.add(currentName, BorderLayout.CENTER);
		title.add(fillerPanel, BorderLayout.EAST);
		title.add(sep, BorderLayout.SOUTH);

		// main notes area
		main = new JPanel();
		main.setLayout(null);

		scroll = new JScrollPane(main);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setBorder(ElephantWindow.emptyBorder);
		scroll.getVerticalScrollBar().setUnitIncrement(10);

		parent.add(title, BorderLayout.NORTH);
		parent.add(scroll, BorderLayout.CENTER);
	}
}
