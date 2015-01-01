package com.pinktwins.elephant;

import java.awt.Color;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import com.pinktwins.elephant.util.CustomMouseListener;
import com.pinktwins.elephant.util.Images;
import com.pinktwins.elephant.util.ResizeListener;

public class ToolbarList<T extends ToolbarList.ToolbarListItem> extends BackgroundPanel {

	interface ToolbarListItem {
		public void setSelected(boolean b);
	}

	private static Image hLine;

	protected JPanel main;
	protected JButton bNew;
	protected SearchTextField search;
	protected JScrollPane scroll;

	protected boolean isEditing;

	protected T selectedItem;

	private Image newButtonImage;
	private String searchHint;

	static {
		Iterator<Image> i = Images.iterator(new String[] { "notebooksHLine" });
		hLine = i.next();
	}

	public ToolbarList(Image img, Image newButtonImage, String searchHint) {
		super(img);

		this.newButtonImage = newButtonImage;
		this.searchHint = searchHint;
	}

	protected void initialize() {
		createComponents();
		update();

		addComponentListener(new ResizeListener() {
			@Override
			public void componentResized(ComponentEvent e) {
				layoutItems();
			}
		});
	}

	protected void createComponents() {
		main = new JPanel();
		main.setLayout(null);

		int divY = 42;

		BackgroundPanel div = new BackgroundPanel(hLine);
		div.setBounds(0, divY, 1920, 2);
		div.setStyle(BackgroundPanel.SCALED_X);

		JPanel tools = new JPanel(null);
		tools.setBounds(0, 0, 800, divY);

		bNew = new JButton("");
		bNew.setIcon(new ImageIcon(newButtonImage));
		bNew.setBorderPainted(false);
		bNew.setBounds(10, 10, newButtonImage.getWidth(null), newButtonImage.getHeight(null));

		search = new SearchTextField(searchHint);
		search.setBorder(BorderFactory.createEmptyBorder(0, 22, 0, 20));
		search.setBounds(newButtonImage.getWidth(null) + 10, 8, 160, 26);
		search.setFont(ElephantWindow.fontMedium);
		search.setFixedColor(Color.decode("#e9e9e9"));
		search.useV2();
		search.windowFocusGained();

		tools.add(bNew);
		tools.add(search);

		scroll = new JScrollPane(main);
		scroll.setBorder(ElephantWindow.emptyBorder);
		scroll.getHorizontalScrollBar().setUnitIncrement(5);
		scroll.getVerticalScrollBar().setUnitIncrement(5);

		add(tools);
		add(div);
		add(scroll);

		addComponentListeners();
	}

	protected void update() {
		// Override
	}

	protected void layoutItems() {
		// Override
	}

	protected void vkEnter() {
		// Override
	}

	protected void newButtonAction() {
		// Override
	}

	protected void refresh() {
		// Override
	}

	protected void deselectAll() {
		if (selectedItem != null) {
			selectedItem.setSelected(false);
			selectedItem = null;
		}
	}

	public void changeSelection(int delta, int keyCode) {
		// Override
	}

	public boolean isEditing() {
		return isEditing || search.hasFocus();
	}

	protected void addComponentListeners() {
		main.addMouseListener(new CustomMouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {
				deselectAll();
				search.setFocusable(false);
			}
		});

		if (bNew != null) {
			bNew.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					newButtonAction();
				}
			});
		}

		search.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				refresh();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				refresh();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				refresh();
			}
		});

		search.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {
			}

			@Override
			public void keyPressed(KeyEvent e) {
				switch (e.getKeyCode()) {
				case KeyEvent.VK_UP:
					if (search.hasFocus()) {
						search.setFocusable(false);
						changeSelection(-1, 0);
					}
					break;
				case KeyEvent.VK_DOWN:
					if (search.hasFocus()) {
						search.setFocusable(false);
						changeSelection(1, 0);
					}
					break;
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
			}
		});

	}

	static final Timer t = new Timer();

	public void handleKeyEvent(final KeyEvent e) {
		switch (e.getID()) {
		case KeyEvent.KEY_PRESSED:
			switch (e.getKeyCode()) {
			case KeyEvent.VK_ENTER:
				vkEnter();
				break;
			default:
				if (e.getKeyCode() != KeyEvent.VK_ESCAPE && e.getKeyCode() != KeyEvent.VK_BACK_SPACE) {
					if (e.getModifiers() == 0) {
						if (!search.hasFocus()) {
							final Document d = search.getDocument();
							final int pos = search.getCaretPosition();

							// Avoid inserted character to be highlighted and
							// wiped by succeeding keystrokes. hack.

							TimerTask tt = new TimerTask() {
								@Override
								public void run() {
									try {
										d.insertString(pos, String.valueOf(e.getKeyChar()), null);
										search.setCaretPosition(search.getCaretPosition() + 1);
									} catch (BadLocationException e1) {
										e1.printStackTrace();
									}
								}
							};

							t.schedule(tt, 50);

							search.setFocusable(true);
							search.requestFocusInWindow();
						}
					}
				}
			}
			break;
		}
	}

}
