package com.pinktwins.elephant;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class Toolbar extends BackgroundPanel {

	private static final long serialVersionUID = -8186087241529191436L;

	ElephantWindow window;

	private static Image toolbarBg, toolbarBgInactive;

	SearchTextField search;

	static {
		try {
			toolbarBg = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/toolbarBg.png"));
			toolbarBgInactive = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/toolbarBgInactive.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Toolbar(ElephantWindow w) {
		super(toolbarBg);
		window = w;

		createComponents();
	}

	private void createComponents() {
		final int searchWidth = 360;

		search = new SearchTextField("Search notes");
		search.setPreferredSize(new Dimension(searchWidth, 26));
		search.setBorder(BorderFactory.createEmptyBorder(0, 22, 0, 20));
		search.setFocusable(false);

		JPanel p = new JPanel(new FlowLayout());
		p.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 10));
		p.add(search);

		add(p, BorderLayout.EAST);

		search.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				window.search(search.getText());
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				window.search(search.getText());
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				window.search(search.getText());
			}
		});

		search.addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent e) {
			}

			@Override
			public void mousePressed(MouseEvent e) {
				search.setFocusable(true);
				search.requestFocusInWindow();

				if (e.getX() >= searchWidth - 20) {
					search.setText("");
					search.setFocusable(false);
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

		search.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {
				search.searchFocusGained();
			}

			@Override
			public void focusLost(FocusEvent e) {
				search.searchFocusLost();
				search.setFocusable(false);
			}
		});

		search.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {
			}

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					if (search.getText().length() > 0) {
						search.setText("");
					} else {
						search.setFocusable(false);
					}
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
			}
		});

	}

	public void focusGained() {
		setImage(toolbarBg);
		search.windowFocusGained();
	}

	public void focusLost() {
		setImage(toolbarBgInactive);
		search.windowFocusLost();
	}

	public boolean isEditing() {
		return search.isFocusOwner();
	}

	public void focusSearch() {
		search.setFocusable(true);
		search.requestFocusInWindow();
	}
	
	public void clearSearch() {
		search.setText("");
		search.setFocusable(false);
	}
}
