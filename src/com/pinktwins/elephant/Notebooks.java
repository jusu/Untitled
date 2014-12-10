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
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import org.pushingpixels.trident.Timeline;

import com.google.common.eventbus.Subscribe;
import com.pinktwins.elephant.data.Notebook;
import com.pinktwins.elephant.data.NotebookEvent;
import com.pinktwins.elephant.data.Vault;
import com.pinktwins.elephant.data.VaultEvent;

public class Notebooks extends BackgroundPanel {

	private static final long serialVersionUID = 7129502018764896415L;
	private static Image tile, notebookBg, notebookBgSelected;

	private ElephantWindow window;
	private NotebookItem selectedNotebook;

	private ArrayList<NotebookItem> notebookItems = new ArrayList<NotebookItem>();

	static {
		try {
			tile = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/notebooks.png"));
			notebookBg = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/notebookBg.png"));
			notebookBgSelected = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/notebookBgSelected.png"));
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

	private void createComponents() {
		main = new JPanel();
		main.setLayout(null);

		scroll = new JScrollPane(main);
		scroll.setBorder(ElephantWindow.emptyBorder);
		scroll.getHorizontalScrollBar().setUnitIncrement(5);

		add(scroll);

		main.addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent e) {
				deselectAll();
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
	}

	public void refresh() {
		update();
		layoutItems();
	}

	private void update() {
		main.removeAll();
		notebookItems.clear();

		List<Notebook> list = Vault.getInstance().getNotebooks();
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
		int x = 0;
		int y = 12;

		Rectangle b = main.getBounds();

		for (NotebookItem item : notebookItems) {
			size = item.getPreferredSize();
			itemsPerRow = b.height / size.height;

			item.setBounds(xOff + x, y + insets.top, size.width, size.height);

			y += size.height;

			if (y + size.height > b.height) {
				x += size.width + xOff;
				y = 12;
			}
		}

		Dimension d = main.getPreferredSize();
		d.width = x + size.width + xOff * 2;
		main.setPreferredSize(d);

		revalidate();
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
		return isEditing;
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
			count.setForeground(Color.LIGHT_GRAY);

			add(name);
			add(count);

			name.setBounds(0, 0, 200, 51);
			count.setBounds(202, 0, 52, 51);

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
}
