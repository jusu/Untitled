package com.pinktwins.elephant;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;

import javax.imageio.ImageIO;

// :-o this class

public class SearchTextField extends HintTextField {
	private static final long serialVersionUID = -4326037468358734997L;

	private static Image searchLeft, searchMiddle, searchRight, searchLeftInactive, searchMiddleInactive, searchRightInactive, searchLeftHilite,
			searchMiddleHilite, searchRightHilite, searchRightHiliteCross;

	private static Image searchLeftV2, searchMiddleV2, searchRightV2, searchLeftHiliteV2, searchMiddleHiliteV2, searchRightHiliteV2, searchRightHiliteCrossV2;

	private static Image searchLeftV3, searchMiddleV3, searchRightV3, searchLeftHiliteV3, searchMiddleHiliteV3, searchRightHiliteV3, searchRightHiliteCrossV3;

	boolean hasWindowFocus, hasSearchFocus;

	private Color fixedColor;

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

			searchLeftV2 = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/searchLeftV2.png"));
			searchMiddleV2 = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/searchMiddleV2.png"));
			searchRightV2 = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/searchRightV2.png"));
			searchLeftHiliteV2 = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/searchLeftHiliteV2.png"));
			searchMiddleHiliteV2 = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/searchMiddleHiliteV2.png"));
			searchRightHiliteV2 = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/searchRightHiliteV2.png"));
			searchRightHiliteCrossV2 = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/searchRightHiliteCrossV2.png"));

			searchLeftV3 = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/searchLeftV3.png"));
			searchMiddleV3 = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/searchMiddleV3.png"));
			searchRightV3 = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/searchRightV3.png"));
			searchLeftHiliteV3 = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/searchLeftHiliteV3.png"));
			searchMiddleHiliteV3 = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/searchMiddleHiliteV3.png"));
			searchRightHiliteV3 = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/searchRightHiliteV3.png"));
			searchRightHiliteCrossV3 = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/searchRightHiliteCrossV3.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private Image[] images = new Image[10];

	enum Idx {
		searchLeft, searchMiddle, searchRight, searchLeftInactive, searchMiddleInactive, searchRightInactive, searchLeftHilite, searchMiddleHilite, searchRightHilite, searchRightHiliteCross
	};

	public SearchTextField(String hint) {
		super(hint);

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

		setBackground(Color.decode("#fafafa"));

		addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent e) {
			}

			@Override
			public void mousePressed(MouseEvent e) {
				setFocusable(true);
				requestFocusInWindow();

				if (e.getX() >= getWidth() - 20) {
					setText("");
					setFocusable(false);
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
			setBackground(Color.decode("#fafafa"));
		}
	}

	public void windowFocusLost() {
		hasWindowFocus = false;
		if (fixedColor == null) {
			setBackground(Color.decode("#fafafa"));
		}
	}

	public void searchFocusGained() {
		hasSearchFocus = true;
	}

	public void searchFocusLost() {
		hasSearchFocus = false;
	}

	public void setFixedColor(Color c) {
		fixedColor = c;
		setBackground(c);
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
