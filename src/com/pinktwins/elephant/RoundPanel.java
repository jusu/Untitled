package com.pinktwins.elephant;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

public class RoundPanel extends JPanel {

	private static final Logger log = Logger.getLogger(RoundPanel.class.getName());

	static Image[] note9p = new Image[8];

	static {
		try {
			for (int n = 0; n < 8; n++) {
				note9p[n] = ImageIO.read(RoundPanel.class.getClass().getResourceAsStream(String.format("/images/note9p%d.png", n + 1)));
			}
		} catch (IOException e) {
			log.log(Level.SEVERE, e.toString());
		}
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		Dimension d = getSize();
		Rectangle b = new Rectangle(0, 0, d.width, d.height);
		g.drawImage(note9p[0], 0, 0, null);

		for (int x = 5; x < b.width - 5; x += 5) {
			g.drawImage(note9p[1], x, 0, null);
		}

		g.drawImage(note9p[2], b.width - 5, 0, null);

		for (int y = 5; y < b.height - 5; y += 5) {
			g.drawImage(note9p[3], 0, y, null);
			g.drawImage(note9p[4], b.width - 5, y, null);
		}

		g.drawImage(note9p[5], 0, b.height - 5, null);

		for (int x = 5; x < b.width - 5; x += 5) {
			g.drawImage(note9p[6], x, b.height - 5, null);
		}

		g.drawImage(note9p[7], b.width - 5, b.height - 5, null);
	}

}
