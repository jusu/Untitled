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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import org.pushingpixels.trident.Timeline;

import com.google.common.eventbus.Subscribe;
import com.pinktwins.elephant.data.Notebook;
import com.pinktwins.elephant.data.NotebookEvent;
import com.pinktwins.elephant.data.Vault;
import com.pinktwins.elephant.data.VaultEvent;

public class Notebooks extends BackgroundPanel {

	private static final long serialVersionUID = 7129502018764896415L;
	private static Image tile, notebookBg, notebookBgSelected, notebooksHLine, newNotebook;

	private ElephantWindow window;
	private NotebookItem selectedNotebook;

	private ArrayList<NotebookItem> notebookItems = new ArrayList<NotebookItem>();

	static {
		try {
			tile = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/notebooks.png"));
			notebookBg = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/notebookBg.png"));
			notebookBgSelected = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/notebookBgSelected.png"));
			notebooksHLine = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/notebooksHLine.png"));
			newNotebook = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/newNotebook.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	boolean isEditing = false;

	public Notebooks(ElephantWindow w) {
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

	@Subscribe
	public void handleNotebookEvent(NotebookEvent event) {
		refresh();
		revalidate();
	}

	JScrollPane scroll;
	JPanel main;
	JButton bNew;
	SearchTextField search;

	private void createComponents() {
		main = new JPanel();
		main.setLayout(null);

		BackgroundPanel div = new BackgroundPanel(notebooksHLine);
		div.setBounds(0, 42, 1920, 44);
		div.setStyle(BackgroundPanel.SCALED_X);

		JPanel tools = new JPanel(null);
		tools.setBounds(0, 0, 800, 44);

		bNew = new JButton("");
		bNew.setIcon(new ImageIcon(newNotebook));
		bNew.setBorderPainted(false);
		bNew.setBounds(10, 10, newNotebook.getWidth(null), newNotebook.getHeight(null));

		search = new SearchTextField("Find a notebook");
		search.setBorder(BorderFactory.createEmptyBorder(0, 22, 0, 20));
		search.setBounds(134, 8, 160, 26);
		search.setFont(ElephantWindow.fontMedium);
		search.setFixedColor(Color.decode("#e9e9e9"));
		search.useV2();
		search.windowFocusGained();

		tools.add(bNew);
		tools.add(search);

		scroll = new JScrollPane(main);
		scroll.setBorder(ElephantWindow.emptyBorder);
		scroll.getHorizontalScrollBar().setUnitIncrement(5);

		add(tools);
		add(div);
		add(scroll);

		main.addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent e) {
				deselectAll();
				search.setFocusable(false);
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
		});

		bNew.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				window.newNotebookAction.actionPerformed(null);
			}
		});

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
		for (NotebookItem item : notebookItems) {
			item.setVisible(false);
		}

		main.removeAll();
		notebookItems.clear();

		Collection<Notebook> list = Vault.getInstance().getNotebooksWithFilter(search.getText());
		for (Notebook nb : list) {
			NotebookItem item = new NotebookItem(nb);
			main.add(item);
			notebookItems.add(item);
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

		for (NotebookItem item : notebookItems) {
			size = item.getPreferredSize();
			itemsPerRow = (b.height - yOff) / size.height;

			item.setBounds(xOff + x, y + insets.top, size.width, size.height);

			y += size.height - 1;

			if (y + size.height > b.height) {
				x += size.width + xOff;
				y = yOff;
			}
		}

		Dimension d = main.getPreferredSize();
		d.width = x + size.width + xOff * 2;
		main.setPreferredSize(d);
	}

	private void deselectAll() {
		if (selectedNotebook != null) {
			selectedNotebook.setSelected(false);
			selectedNotebook = null;
		}
	}

	int itemsPerRow;

	public void changeSelection(int delta, int keyCode) {
		int len = notebookItems.size();
		int select = -1;

		if (keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_RIGHT) {
			delta *= itemsPerRow;
		}

		if (selectedNotebook == null) {
			if (len > 0) {
				if (delta < 0) {
					select = len - 1;
				} else {
					select = 0;
				}
			}
		} else {
			int currentIndex = notebookItems.indexOf(selectedNotebook);
			select = currentIndex + delta;
		}

		if (select >= 0 && select < len) {
			NotebookItem item = notebookItems.get(select);
			selectNotebook(item);
		}
	}

	void selectNotebook(NotebookItem item) {
		deselectAll();
		item.setSelected(true);
		selectedNotebook = item;

		Rectangle b = item.getBounds();
		int itemX = b.x;
		int x = scroll.getHorizontalScrollBar().getValue();
		int scrollWidth = scroll.getBounds().width;

		if (itemX < x || itemX + b.height >= x + scrollWidth) {

			if (itemX < x) {
				itemX -= 12;
			} else {
				itemX -= scrollWidth - b.width - 12;
			}

			JScrollBar bar = scroll.getHorizontalScrollBar();
			Timeline timeline = new Timeline(bar);
			timeline.addPropertyToInterpolate("value", bar.getValue(), itemX);
			timeline.setDuration(100);
			timeline.play();
		}

	}

	public void openSelected() {
		if (selectedNotebook != null) {
			window.showNotebook(selectedNotebook.notebook);
		}
	}

	public void newNotebook() {
		try {
			Notebook nb = Notebook.createNotebook();
			NotebookItem newItem = new NotebookItem(nb);
			newItem.setEditable();
			notebookItems.add(0, newItem);
			main.add(newItem, 0);
			layoutItems();

			deselectAll();
			newItem.edit.requestFocusInWindow();
			isEditing = true;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public boolean isEditing() {
		return isEditing || search.hasFocus();
	}

	class NotebookItem extends BackgroundPanel implements MouseListener {
		private static final long serialVersionUID = -7285867977183764620L;

		private Notebook notebook;
		private Dimension size = new Dimension(252, 51);
		private JLabel name;
		private JLabel count;
		private JTextField edit;

		public NotebookItem(Notebook nb) {
			super(notebookBg);

			setLayout(null);

			notebook = nb;

			name = new JLabel(nb.name());
			name.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 0));
			name.setForeground(Color.DARK_GRAY);

			count = new JLabel(String.valueOf(nb.count()), SwingConstants.CENTER);
			count.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 16));
			count.setForeground(Color.DARK_GRAY);
			count.setFont(ElephantWindow.fontMedium);

			add(name);
			add(count);

			name.setBounds(0, 0, 200, 51);
			count.setBounds(202, 0, 60, 51);

			addMouseListener(this);
		}

		public void setEditable() {
			edit = new JTextField();
			edit.setText(notebook.name());
			edit.setSelectionStart(0);
			edit.setSelectionEnd(notebook.name().length());
			edit.setMaximumSize(new Dimension(200, 30));
			remove(name);
			add(edit);

			edit.setBounds(12, 10, 180, 31);

			edit.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					doneEditing();
				}
			});

			edit.addKeyListener(new KeyListener() {
				@Override
				public void keyTyped(KeyEvent e) {
				}

				@Override
				public void keyPressed(KeyEvent e) {
					if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
						cancelEditing();
					}
				}

				@Override
				public void keyReleased(KeyEvent e) {
				}
			});
		}

		protected void doneEditing() {
			String s = edit.getText();
			if (notebook.rename(s)) {
				isEditing = false;
				Elephant.eventBus.post(new VaultEvent(VaultEvent.Kind.notebookCreated));
				Elephant.eventBus.post(new VaultEvent(VaultEvent.Kind.notebookListChanged));

				for (NotebookItem item : notebookItems) {
					if (item.notebook.equals(notebook.folder())) {
						selectNotebook(item);
					}
				}

			} else {
				// XXX likely nonconforming characters in name. explain it.
			}
		}

		protected void cancelEditing() {
			try {
				notebook.folder().delete();
			} catch (Exception e) {
				e.printStackTrace();
			}

			isEditing = false;
			Elephant.eventBus.post(new VaultEvent(VaultEvent.Kind.notebookListChanged));
		}

		public void setSelected(boolean b) {
			if (b) {
				setImage(notebookBgSelected);
				selectedNotebook = this;
			} else {
				setImage(notebookBg);
			}
			repaint();
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
			if (e.getClickCount() == 2) {
				window.showNotebook(notebook);
			}
		}

		@Override
		public void mousePressed(MouseEvent e) {
			if (isEditing) {
				return;
			}

			selectNotebook(NotebookItem.this);
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

	public void handleKeyEvent(final KeyEvent e) {
		if (e.getKeyCode() != KeyEvent.VK_ESCAPE) {
			if (e.getModifiers() == 0) {
				if (!search.hasFocus()) {
					final Document d = search.getDocument();
					final int pos = search.getCaretPosition();

					// Avoid inserted character to be highlighted and wiped
					// by succeeding keystrokes
					Timer t = new Timer();
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
				search.setFocusable(true);
				search.requestFocusInWindow();
			}
		}
	}
}
