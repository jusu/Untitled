package com.pinktwins.elephant;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Iterator;

import com.pinktwins.elephant.util.Images;

// :-o this class

public class SearchTextField extends HintTextField {
	private static final long serialVersionUID = -4326037468358734997L;

	private static Image searchLeft, searchMiddle, searchRight, searchLeftInactive, searchMiddleInactive, searchRightInactive, searchLeftHilite,
			searchMiddleHilite, searchRightHilite, searchRightHiliteCross;

	private static Image searchLeftV2, searchMiddleV2, searchRightV2, searchLeftHiliteV2, searchMiddleHiliteV2, searchRightHiliteV2, searchRightHiliteCrossV2;

	private static Image searchLeftV3, searchMiddleV3, searchRightV3, searchLeftHiliteV3, searchMiddleHiliteV3, searchRightHiliteV3, searchRightHiliteCrossV3;

	private static Image searchLeftV4, searchMiddleV4, searchRightV4, searchLeftHiliteV4, searchMiddleHiliteV4, searchRightHiliteV4, searchRightHiliteCrossV4;

	boolean hasWindowFocus, hasSearchFocus;

	private Color fixedColor;
	private static final Color colorDefaultBg = Color.decode("#fafafa");

	static {
		Iterator<Image> i = Images.iterator(new String[] { "searchLeft", "searchMiddle", "searchRight", "searchLeftInactive", "searchMiddleInactive",
				"searchRightInactive", "searchLeftHilite", "searchMiddleHilite", "searchRightHilite", "searchRightHiliteCross", "searchLeftV2",
				"searchMiddleV2", "searchRightV2", "searchLeftHiliteV2", "searchMiddleHiliteV2", "searchRightHiliteV2", "searchRightHiliteCrossV2",
				"searchLeftV3", "searchMiddleV3", "searchRightV3", "searchLeftHiliteV3", "searchMiddleHiliteV3", "searchRightHiliteV3",
				"searchRightHiliteCrossV3", "searchLeftV4", "searchMiddleV4", "searchRightV4", "searchLeftHiliteV4", "searchMiddleHiliteV4", "searchRightHiliteV4",
				"searchRightHiliteCrossV4" });
		searchLeft = i.next();
		searchMiddle = i.next();
		searchRight = i.next();
		searchLeftInactive = i.next();
		searchMiddleInactive = i.next();
		searchRightInactive = i.next();
		searchLeftHilite = i.next();
		searchMiddleHilite = i.next();
		searchRightHilite = i.next();
		searchRightHiliteCross = i.next();

		searchLeftV2 = i.next();
		searchMiddleV2 = i.next();
		searchRightV2 = i.next();
		searchLeftHiliteV2 = i.next();
		searchMiddleHiliteV2 = i.next();
		searchRightHiliteV2 = i.next();
		searchRightHiliteCrossV2 = i.next();

		searchLeftV3 = i.next();
		searchMiddleV3 = i.next();
		searchRightV3 = i.next();
		searchLeftHiliteV3 = i.next();
		searchMiddleHiliteV3 = i.next();
		searchRightHiliteV3 = i.next();
		searchRightHiliteCrossV3 = i.next();

		searchLeftV4 = i.next();
		searchMiddleV4 = i.next();
		searchRightV4 = i.next();
		searchLeftHiliteV4 = i.next();
		searchMiddleHiliteV4 = i.next();
		searchRightHiliteV4 = i.next();
		searchRightHiliteCrossV4 = i.next();
	}

	private Image[] images = new Image[10];

	enum Idx {
		searchLeft, searchMiddle, searchRight, searchLeftInactive, searchMiddleInactive, searchRightInactive, searchLeftHilite, searchMiddleHilite, searchRightHilite, searchRightHiliteCross
	};

	public SearchTextField(String hint, Font font) {
		super(hint, font);

		useV1();

		setBackground(colorDefaultBg);

		addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent e) {
			}

			@Override
			public void mousePressed(MouseEvent e) {
				setFocusable(true);
				requestFocusInWindow();

				if (e.getX() >= getWidth() - 20) {
					if (getText().length() > 0) {
						setText("");
					}
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
		});

		addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {
				searchFocusGained();
			}

			@Override
			public void focusLost(FocusEvent e) {
				searchFocusLost();
				setFocusable(false);
			}
		});

		addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {
			}

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					if (getText().length() > 0) {
						setText("");
					} else {
						setFocusable(false);
					}
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
			}
		});

	}

	public void windowFocusGained() {
		hasWindowFocus = true;
		if (fixedColor == null) {
			setBackground(colorDefaultBg);
		}
	}

	public void windowFocusLost() {
		hasWindowFocus = false;
		if (fixedColor == null) {
			setBackground(colorDefaultBg);
		}
	}

	public void searchFocusGained() {
		hasSearchFocus = true;
		repaint();
	}

	public void searchFocusLost() {
		hasSearchFocus = false;
		repaint();
	}

	public void setFixedColor(Color c) {
		fixedColor = c;
		setBackground(c);
	}

	public void useV1() {
		images[0] = searchLeft;
		images[1] = searchMiddle;
		images[2] = searchRight;
		images[3] = searchLeftInactive;
		images[4] = searchMiddleInactive;
		images[5] = searchRightInactive;
		images[6] = searchLeftHilite;
		images[7] = searchMiddleHilite;
		images[8] = searchRightHilite;
		images[9] = searchRightHiliteCross;
	}

	public void useV2() {
		images[0] = searchLeftV2;
		images[1] = searchMiddleV2;
		images[2] = searchRightV2;
		images[3] = searchLeftInactive;
		images[4] = searchMiddleInactive;
		images[5] = searchRightInactive;
		images[6] = searchLeftHiliteV2;
		images[7] = searchMiddleHiliteV2;
		images[8] = searchRightHiliteV2;
		images[9] = searchRightHiliteCrossV2;
	}

	public void useV3() {
		images[0] = searchLeftV3;
		images[1] = searchMiddleV3;
		images[2] = searchRightV3;
		images[3] = searchLeftInactive;
		images[4] = searchMiddleInactive;
		images[5] = searchRightInactive;
		images[6] = searchLeftHiliteV3;
		images[7] = searchMiddleHiliteV3;
		images[8] = searchRightHiliteV3;
		images[9] = searchRightHiliteCrossV3;
	}

	public void useV4() {
		images[0] = searchLeftV4;
		images[1] = searchMiddleV4;
		images[2] = searchRightV4;
		images[3] = searchLeftInactive;
		images[4] = searchMiddleInactive;
		images[5] = searchRightInactive;
		images[6] = searchLeftHiliteV4;
		images[7] = searchMiddleHiliteV4;
		images[8] = searchRightHiliteV4;
		images[9] = searchRightHiliteCrossV4;
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);

		int w = getWidth();

		if (hasWindowFocus) {
			if (hasSearchFocus) {
				g.drawImage(images[Idx.searchLeftHilite.ordinal()], 0, 0, null);
				if (getText().length() == 0) {
					g.drawImage(images[Idx.searchRightHilite.ordinal()], w - 20, 0, null);
				} else {
					g.drawImage(images[Idx.searchRightHiliteCross.ordinal()], w - 20, 0, null);
				}
			} else {
				g.drawImage(images[Idx.searchLeft.ordinal()], 0, 0, null);
				g.drawImage(images[Idx.searchRight.ordinal()], w - 20, 0, null);
			}
		} else {
			g.drawImage(images[Idx.searchLeftInactive.ordinal()], 0, 0, null);
			g.drawImage(images[Idx.searchRightInactive.ordinal()], w - 20, 0, null);
		}

		for (int n = 20; n < w - 20; n += 10) {
			if (hasWindowFocus) {
				if (hasSearchFocus) {
					g.drawImage(images[Idx.searchMiddleHilite.ordinal()], n, 0, 10, 26, null);
				} else {
					g.drawImage(images[Idx.searchMiddle.ordinal()], n, 0, 10, 26, null);
				}
			} else {
				g.drawImage(images[Idx.searchMiddleInactive.ordinal()], n, 0, 10, 26, null);
			}
		}
	}
}
