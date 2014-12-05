package com.pinktwins.elephant;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import com.google.common.eventbus.Subscribe;
import com.pinktwins.elephant.data.Notebook;
import com.pinktwins.elephant.data.NotebookEvent;
import com.pinktwins.elephant.data.Vault;

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

	public Notebooks(ElephantWindow w) {
		super(tile);

		window = w;

		Elephant.eventBus.register(this);

		createComponents();
		update();
	}

	@Subscribe
	public void handleNotebookEvent(NotebookEvent event) {
		refresh();
	}

	JPanel main;

	private void createComponents() {
		main = new JPanel();
		main.setLayout(null);
		add(main);

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
	}

	private void update() {
		main.removeAll();
		notebookItems.clear();

		Insets insets = main.getInsets();
		Dimension size;
		int y = 12;

		List<Notebook> list = Vault.getInstance().getNotebooks();
		int n = 0, len = list.size();
		for (Notebook nb : list) {
			NotebookItem item = new NotebookItem(nb);
			main.add(item);
			notebookItems.add(item);

			size = item.getPreferredSize();
			item.setBounds(12 + insets.left, y + insets.top, size.width, size.height);
			y += size.height;

			if (n < len - 1) {
				// y--;
			}

			n++;
		}
	}

	private void deselectAll() {
		if (selectedNotebook != null) {
			selectedNotebook.setSelected(false);
			selectedNotebook = null;
		}
	}

	public void changeSelection(int delta) {
		int len = notebookItems.size();
		int select = -1;

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
			deselectAll();

			NotebookItem item = notebookItems.get(select);
			item.setSelected(true);
			selectedNotebook = item;
		}
	}

	public void openSelected() {
		if (selectedNotebook != null) {
			window.showNotebook(selectedNotebook.notebook);
		}
	}

	class NotebookItem extends BackgroundPanel implements MouseListener {
		private static final long serialVersionUID = -7285867977183764620L;

		private Notebook notebook;
		private Dimension size = new Dimension(252, 51);
		private JLabel name;
		private JLabel count;

		public NotebookItem(Notebook nb) {
			super(notebookBg);

			notebook = nb;

			JSplitPane p = new JSplitPane();
			p.setResizeWeight(0.82);
			p.setDividerSize(0);
			p.setBorder(null);

			name = new JLabel(nb.name());
			name.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 0));
			name.setForeground(Color.DARK_GRAY);
			p.setLeftComponent(name);

			count = new JLabel(String.valueOf(nb.count()));
			count.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 0));
			count.setForeground(Color.LIGHT_GRAY);
			p.setRightComponent(count);

			add(p);

			p.addMouseListener(this);
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
			deselectAll();
			setSelected(true);

			if (e.getClickCount() == 2) {
				window.showNotebook(notebook);
			}
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
}
