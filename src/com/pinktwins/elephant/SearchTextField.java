package com.pinktwins.elephant;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.io.IOException;

import javax.imageio.ImageIO;

// :-o this class

public class SearchTextField extends HintTextField {
	private static final long serialVersionUID = -4326037468358734997L;

	private static Image searchLeft, searchMiddle, searchRight, searchLeftInactive, searchMiddleInactive, searchRightInactive, searchLeftHilite,
			searchMiddleHilite, searchRightHilite, searchRightHiliteCross;

	boolean hasWindowFocus, hasSearchFocus;

	static {
		try {
			searchLeft = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/searchLeft.png"));
			searchMiddle = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/searchMiddle.png"));
			searchRight = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/searchRight.png"));
			searchLeftInactive = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/searchLeftInactive.png"));
			searchMiddleInactive = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/searchMiddleInactive.png"));
			searchRightInactive = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/searchRightInactive.png"));
			searchLeftHilite = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/searchLeftHilite.png"));
			searchMiddleHilite = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/searchMiddleHilite.png"));
			searchRightHilite = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/searchRightHilite.png"));
			searchRightHiliteCross = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/searchRightHiliteCross.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public SearchTextField(String hint) {
		super(hint);

		setBackground(Color.decode("#fafafa"));
	}

	public void windowFocusGained() {
		hasWindowFocus = true;
		setBackground(Color.decode("#fafafa"));
	}

	public void windowFocusLost() {
		hasWindowFocus = false;
		setBackground(Color.decode("#fafafa"));
	}

	public void searchFocusGained() {
		hasSearchFocus = true;
	}

	public void searchFocusLost() {
		hasSearchFocus = false;
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);

		int w = getWidth();

		if (hasWindowFocus) {
			if (hasSearchFocus) {
				g.drawImage(searchLeftHilite, 0, 0, null);
				if (getText().length() == 0) {
					g.drawImage(searchRightHilite, w - 20, 0, null);
				} else {
					g.drawImage(searchRightHiliteCross, w - 20, 0, null);
				}
			} else {
				g.drawImage(searchLeft, 0, 0, null);
				g.drawImage(searchRight, w - 20, 0, null);
			}
		} else {
			g.drawImage(searchLeftInactive, 0, 0, null);
			g.drawImage(searchRightInactive, w - 20, 0, null);
		}

		for (int n = 20; n < w - 20; n += 10) {
			if (hasWindowFocus) {
				if (hasSearchFocus) {
					g.drawImage(searchMiddleHilite, n, 0, 10, 26, null);
				} else {
					g.drawImage(searchMiddle, n, 0, 10, 26, null);
				}
			} else {
				g.drawImage(searchMiddleInactive, n, 0, 10, 26, null);
			}
		}
	}
}
