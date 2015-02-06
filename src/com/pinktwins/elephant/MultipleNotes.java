package com.pinktwins.elephant;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.util.Iterator;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;

import com.pinktwins.elephant.data.Note;
import com.pinktwins.elephant.util.Images;
import com.pinktwins.elephant.util.ResizeListener;

public class MultipleNotes extends BackgroundPanel {

	private static Image tile, multiSelection, moveToNotebook;
	private final Font headerFont = Font.decode("Helvetica-BOLD-16");
	private final Color headerColor = Color.decode("#7a7a7a");
	private final Color lineColor = Color.decode("#b4b4b4");

	JLabel header;

	static {
		Iterator<Image> i = Images.iterator(new String[] { "notebooks", "multiSelection", "moveToNotebook" });
		tile = i.next();
		multiSelection = i.next();
		moveToNotebook = i.next();
	}

	public MultipleNotes(ElephantWindow w) {
		super(tile);
		createComponents();
	}

	private void createComponents() {
		header = new JLabel("wowowo");
		header.setFont(headerFont);
		header.setForeground(headerColor);
		final BackgroundPanel frame = new BackgroundPanel(multiSelection);

		JButton bMove = new JButton();
		bMove.setIcon(new ImageIcon(moveToNotebook));
		bMove.setBorderPainted(false);
		bMove.setContentAreaFilled(false);

		frame.setLayout(null);
		frame.add(bMove, BorderLayout.CENTER);
		bMove.setBounds(20, 31, 290, 24);

		setLayout(null);
		add(header);
		add(frame);

		addComponentListener(new ResizeListener() {
			@Override
			public void componentResized(ComponentEvent e) {
				int x = (getWidth() - multiSelection.getWidth(null)) / 2;
				int y = getHeight() / 2;
				frame.setBounds(x, y, multiSelection.getWidth(null), multiSelection.getHeight(null));

				int w = header.getPreferredSize().width;
				x = (getWidth() - w) / 2;
				y = getHeight() / 2 - 30;
				header.setBounds(x, y, w, 20);
			}
		});

		bMove.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println("move");
			}
		});
	}

	public void load(Set<Note> selection) {
		header.setText(String.valueOf(selection.size()) + " notes selected");
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);
		g.setColor(lineColor);
		g.drawLine(0, 0, 0, getHeight());
	}

}
