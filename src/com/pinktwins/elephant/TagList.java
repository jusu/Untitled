package com.pinktwins.elephant;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import com.google.common.eventbus.Subscribe;
import com.pinktwins.elephant.data.Tag;
import com.pinktwins.elephant.data.Vault;
import com.pinktwins.elephant.eventbus.NoteChangedEvent;
import com.pinktwins.elephant.util.CustomMouseListener;
import com.pinktwins.elephant.util.Factory;
import com.pinktwins.elephant.util.Images;
import com.pinktwins.elephant.util.ResizeListener;

// XXX some duplicate code with Notebooks class. dedup.

public class TagList extends BackgroundPanel {

	private static Image tile, tagsHLine, newTag;

	private ElephantWindow window;
	private ListController<TagItem> lc = ListController.newInstance();

	private ArrayList<TagItem> tagItems = Factory.newArrayList();
	private TagItem selectedTag;

	private JScrollPane scroll;
	private JPanel main;
	private JButton bNew;
	private SearchTextField search;

	private boolean isEditing;

	static {
		Iterator<Image> i = Images.iterator(new String[] { "notebooks", "notebooksHLine", "newTag" });
		tile = i.next();
		tagsHLine = i.next();
		newTag = i.next();
	}

	public TagList(ElephantWindow w) {
		super(tile);
		window = w;

		Elephant.eventBus.register(this);

		createComponents();
		update();

		addComponentListener(new ResizeListener() {
			@Override
			public void componentResized(ComponentEvent e) {
				layoutItems();
			}
		});
	}

	public boolean isEditing() {
		return isEditing;
	}

	private void createScrollPane() {
		scroll = new JScrollPane(main);
		scroll.setBorder(ElephantWindow.emptyBorder);
		scroll.getHorizontalScrollBar().setUnitIncrement(5);
		scroll.getVerticalScrollBar().setUnitIncrement(5);
	}

	private void createComponents() {
		main = new JPanel();
		main.setLayout(null);

		int divY = 42;

		BackgroundPanel div = new BackgroundPanel(tagsHLine);
		div.setBounds(0, divY, 1920, 2);
		div.setStyle(BackgroundPanel.SCALED_X);

		JPanel tools = new JPanel(null);
		tools.setBounds(0, 0, 800, divY);

		bNew = new JButton("");
		bNew.setIcon(new ImageIcon(newTag));
		bNew.setBorderPainted(false);
		bNew.setBounds(10, 10, newTag.getWidth(null), newTag.getHeight(null));

		search = new SearchTextField("Find a tag");
		search.setBorder(BorderFactory.createEmptyBorder(0, 22, 0, 20));
		search.setBounds(134, 8, 160, 26);
		search.setFont(ElephantWindow.fontMedium);
		search.setFixedColor(Color.decode("#e9e9e9"));
		search.useV2();
		search.windowFocusGained();

		tools.add(bNew);
		tools.add(search);

		createScrollPane();

		add(tools);
		add(div);
		add(scroll);

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
					window.newNotebookAction.actionPerformed(null);
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

	public void refresh() {
		update();
		layoutItems();
	}

	private void update() {
		for (TagItem item : tagItems) {
			item.setVisible(false);
		}

		main.removeAll();
		tagItems.clear();

		Collection<Tag> list = Vault.getInstance().getTagsWithFilter(search.getText());
		Collections.sort((List<Tag>) list);

		for (Tag t : list) {
			TagItem item = new TagItem(t);
			main.add(item);
			tagItems.add(item);
		}
	}

	private void layoutItems() {
		Insets insets = main.getInsets();
		Dimension size = new Dimension();

		int xOff = 12 + insets.left;
		int yOff = 57;

		int x = 0;
		int y = yOff;

		Rectangle b = main.getBounds();

		for (TagItem item : tagItems) {
			size = item.getPreferredSize();
			lc.itemsPerRow = (b.height - yOff) / size.height;

			item.setBounds(xOff + x, y + insets.top, size.width, size.height);

			y += size.height;

			if (y + size.height > b.height) {
				x += size.width + xOff;
				y = yOff;
			}
		}

		Dimension d = main.getPreferredSize();
		d.width = x + size.width + xOff * 2;
		main.setPreferredSize(d);
	}

	public void changeSelection(int delta, int keyCode) {
		boolean sideways = keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_RIGHT;

		TagItem item = lc.changeSelection(tagItems, selectedTag, delta, sideways);
		if (item != null) {
			selectTag(item);
		}
	}

	void deselectAll() {
		if (selectedTag != null) {
			selectedTag.setSelected(false);
			selectedTag = null;
		}
	}

	void selectTag(TagItem item) {
		deselectAll();
		item.setSelected(true);
		selectedTag = item;

		lc.updateHorizontalScrollbar(item, scroll);
	}

	public void openSelected() {
	}

	class TagItem extends BackgroundPanel implements MouseListener {

		private Tag tag;
		private Dimension size = new Dimension(200, 30);
		private JLabel name;

		public TagItem(Tag t) {
			setLayout(null);
			setOpaque(false);

			tag = t;

			name = new JLabel(t.name(), JLabel.LEFT);
			name.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 0));
			name.setForeground(Color.DARK_GRAY);

			add(name);

			name.setBounds(0, 0, 200, 30);
		}

		public void setSelected(boolean b) {
			if (b) {
				name.setForeground(Color.RED);
			} else {
				name.setForeground(Color.DARK_GRAY);
			}
			/*
			 * if (b) { setImage(notebookBgSelected); selectedNotebook = this; }
			 * else { setImage(notebookBg); } repaint();
			 */
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

		@Override
		public void mouseClicked(MouseEvent e) {
		}

		@Override
		public void mousePressed(MouseEvent e) {
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

	}

	static final Timer t = new Timer();

	public void handleKeyEvent(final KeyEvent e) {
		switch (e.getID()) {
		case KeyEvent.KEY_PRESSED:
			switch (e.getKeyCode()) {
			case KeyEvent.VK_ENTER:
				System.out.println("TagList: ENTER");
				/*
				 * if (selectedTag != null && naListener != null) {
				 * naListener.didSelect(selectedNotebook.notebook); }
				 */
				break;
			default:
				if (e.getModifiers() == 0) {
					if (!search.hasFocus()) {
						final Document d = search.getDocument();
						final int pos = search.getCaretPosition();

						// Avoid inserted character to be highlighted and wiped
						// by succeeding keystrokes
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
					}
				}
				search.setFocusable(true);
				search.requestFocusInWindow();
			}
			break;
		}
	}

	@Subscribe
	public void handleNoteChangedEvent(NoteChangedEvent event) {
		refresh();
	}
}
