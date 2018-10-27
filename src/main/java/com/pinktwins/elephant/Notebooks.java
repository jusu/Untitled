package com.pinktwins.elephant;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import com.google.common.eventbus.Subscribe;
import com.pinktwins.elephant.data.Notebook;
import com.pinktwins.elephant.data.Vault;
import com.pinktwins.elephant.eventbus.NotebookEvent;
import com.pinktwins.elephant.eventbus.VaultEvent;
import com.pinktwins.elephant.util.Factory;
import com.pinktwins.elephant.util.Images;

public class Notebooks extends ToolbarList<Notebooks.NotebookItem> {

	private static final Logger LOG = Logger.getLogger(Notebooks.class.getName());

	private static Image tile, notebookBg, notebookBgSelected, newNotebook;
	private ElephantWindow window;
	private NotebookActionListener naListener;

	static {
		Iterator<Image> i = Images.iterator(new String[] { "notebooks", "notebookBg", "notebookBgSelected", "newNotebook" });
		tile = i.next();
		notebookBg = i.next();
		notebookBgSelected = i.next();
		newNotebook = i.next();
	}

	public Notebooks(ElephantWindow w) {
		super(tile, newNotebook, "Find a notebook");

		window = w;

		Elephant.eventBus.register(this);

		initialize();
		layoutItemHeightAdjustment = -1;
	}

	public void cleanup() {
		Elephant.eventBus.unregister(this);
		window = null;
	}

	public void setNotebookActionListener(NotebookActionListener l) {
		naListener = l;
	}

	@Subscribe
	public void handleNotebookEvent(NotebookEvent event) {
		refresh();
		revalidate();
	}

	@Override
	protected void newButtonAction() {
		window.newNotebookAction.actionPerformed(null);
	}

	@Override
	protected void trashButtonAction() {
		deleteSelected();
	}

	@Override
	protected List<NotebookItem> queryFilter(String text) {
		List<NotebookItem> items = Factory.newArrayList();
		for (Notebook nb : Vault.getInstance().getNotebooksWithFilter(search.getText())) {
			items.add(new NotebookItem(nb));
		}

		if (selectedIndex >= 0 && selectedIndex < items.size() && selectedItem == null) {
			selectItem(items.get(selectedIndex));
		}

		return items;
	}

	public void openSelected() {
		if (selectedItem != null) {
			window.showNotebook(selectedItem.notebook);
		}
	}

	public void deleteSelected() {
		if (selectedItem != null) {
			int index = itemList.indexOf(selectedItem);

			Vault.getInstance().deleteNotebook(selectedItem.notebook);

			if (index >= itemList.size()) {
				index = itemList.size() - 1;
			}
			if (index >= 0) {
				selectItem(itemList.get(index));
			}
		}
	}

	public void newNotebook() {
		try {
			Notebook nb = Notebook.createNotebook();
			NotebookItem newItem = new NotebookItem(nb);
			JTextField edit = setEditable(newItem, nb.name());
			itemList.add(0, newItem);
			main.add(newItem, 0);
			layoutItems();

			deselectAll();
			edit.requestFocusInWindow();
		} catch (IOException e) {
			LOG.severe("Fail: " + e);
		}
	}

	@Override
	protected JTextField setEditable(NotebookItem item, String name) {
		item.remove(item.name);
		JTextField edit = super.setEditable(item, name);

		edit.setMaximumSize(new Dimension(200, 30));
		edit.setBounds(12, 10, 180, 31);

		item.add(edit);
		return edit;
	}

	@SuppressWarnings("unlikely-arg-type")
	@Override
	protected void doneEditing(NotebookItem item, String text) {
		if (item.notebook.rename(text)) {
			new VaultEvent(VaultEvent.Kind.notebookCreated, item.notebook).post();
			new VaultEvent(VaultEvent.Kind.notebookListChanged, item.notebook).post();

			for (NotebookItem i : itemList) {
				if (i.notebook.equals(item.notebook.folder())) {
					selectItem(i);
				}
			}
		} else {
			// XXX likely nonconforming characters in name. explain it.
		}
	}

	@Override
	protected void cancelEditing(NotebookItem item) {
		try {
			item.notebook.folder().delete();
		} catch (Exception e) {
			LOG.severe("Fail: " + e);
		}

		new VaultEvent(VaultEvent.Kind.notebookListChanged, item.notebook).post();
	}

	class NotebookItem extends BackgroundPanel implements ToolbarList.ToolbarListItem, MouseListener {
		private static final long serialVersionUID = -7285867977183764620L;

		private Notebook notebook;
		private Dimension size = new Dimension(252, 51);
		private JLabel name;
		private JLabel count;

		public NotebookItem(Notebook nb) {
			super(notebookBg);
			keepScaleOnRetina(true, true);

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

		public Notebook getNotebook() {
			return notebook;
		}

		@Override
		public void setSelected(boolean b) {
			if (b) {
				setImage(notebookBgSelected);
				selectedItem = this;
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
				if (naListener != null) {
					naListener.didSelect(notebook);
				} else {
					window.showNotebook(notebook);
				}
			}
		}

		@Override
		public void mousePressed(MouseEvent e) {
			if (isEditing) {
				return;
			}

			selectItem(NotebookItem.this);
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
