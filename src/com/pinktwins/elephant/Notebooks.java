package com.pinktwins.elephant;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
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

	private static final long serialVersionUID = 7129502018764896415L;
	private static Image tile, notebookBg, notebookBgSelected, notebooksHLine, newNotebook;

	interface NotebookActionListener {
		public void didCancelSelection();

		public void didSelect(Notebook nb);
	}

	NotebookActionListener naListener;

	public void setNotebookActionListener(NotebookActionListener l) {
		naListener = l;
	}

	private ElephantWindow window;

	private ArrayList<NotebookItem> notebookItems = Factory.newArrayList();

	static {
		Iterator<Image> i = Images.iterator(new String[] { "notebooks", "notebookBg", "notebookBgSelected", "notebooksHLine", "newNotebook" });
		tile = i.next();
		notebookBg = i.next();
		notebookBgSelected = i.next();
		notebooksHLine = i.next();
		newNotebook = i.next();
	}

	private boolean isModal;
	private String modalHeader;
	private ListController<NotebookItem> lc = ListController.newInstance();

	public Notebooks(ElephantWindow w, boolean modalChooser, String modalHeader) {
		super(tile, newNotebook, "Find a notebook");

		window = w;
		isModal = modalChooser;
		this.modalHeader = modalHeader;

		Elephant.eventBus.register(this);

		initialize();
	}

	@Subscribe
	public void handleNotebookEvent(NotebookEvent event) {
		refresh();
		revalidate();
	}

	JButton bMove;

	private void createComponents_Modal() {
		setLayout(null);
		setImage(null);
		setBackground(Color.decode("#eaeaea"));

		boolean isJump = modalHeader.isEmpty(); // gah

		main = new JPanel();
		main.setLayout(null);

		int divY = 86;

		BackgroundPanel div = new BackgroundPanel(notebooksHLine);
		div.setBounds(0, divY, 1920, 2);
		div.setStyle(BackgroundPanel.SCALED_X);

		JPanel tools = new JPanel(null);
		tools.setBounds(0, 0, 800, divY);

		search = new SearchTextField("Find a notebook");
		search.setBorder(BorderFactory.createEmptyBorder(0, 22, 0, 20));
		if (isJump) {
			search.setBounds(14, 10, 414, 26);
		} else {
			search.setBounds(14, 60, 414, 26);
		}

		search.setFont(ElephantWindow.fontMedium);
		search.setFixedColor(Color.decode("#e9e9e9"));
		search.useV3();
		search.setFixedColor(Color.WHITE);

		search.windowFocusGained();

		JLabel title = new JLabel(modalHeader, JLabel.CENTER);
		title.setFont(ElephantWindow.fontModalHeader);
		title.setBounds(0, 14, NotebookChooser.fixedWidth, 40);
		tools.add(title);
		tools.add(search);

		scroll = new JScrollPane(main);
		scroll.setBorder(ElephantWindow.emptyBorder);
		scroll.getHorizontalScrollBar().setUnitIncrement(5);
		scroll.getVerticalScrollBar().setUnitIncrement(5);

		scroll.setBorder(BorderFactory.createLineBorder(Color.decode("#d9d9d9"), 1));
		if (isJump) {
			scroll.setBounds(18, 40, 424 - 18, 564);
		} else {
			scroll.setBounds(18, 103, 424 - 18, 564 - 103);
		}

		add(tools);
		add(scroll);

		scroll.setOpaque(true);
		scroll.setBackground(Color.decode("#e6e6e6"));

		JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton bCancel = new JButton("Cancel");
		bMove = new JButton("Move");

		if (!isJump) {
			actions.add(bCancel);
			actions.add(bMove);
		}

		actions.setBounds(0, NotebookChooser.fixedHeight - 46, NotebookChooser.fixedWidth - 7, 40);
		add(actions);

		bCancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (naListener != null) {
					naListener.didCancelSelection();
				}
			}
		});

		bMove.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (selectedItem != null) {
					naListener.didSelect(selectedItem.notebook);
				}
			}
		});
	}

	protected void createComponents() {
		if (isModal) {
			createComponents_Modal();
			addComponentListeners();
		} else {
			super.createComponents();
		}
	}

	@Override
	protected void newButtonAction() {
		window.newNotebookAction.actionPerformed(null);
	}

	@Override
	public void refresh() {
		update();
		layoutItems();
	}

	@Override
	protected void update() {
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

	@Override
	protected void layoutItems() {
		Insets insets = main.getInsets();
		Dimension size = new Dimension();

		int xOff = 12 + insets.left;
		int yOff = 57;

		if (isModal) {
			xOff += 56;
			yOff -= 49;
		}

		int x = 0;
		int y = yOff;

		Rectangle b = main.getBounds();

		for (NotebookItem item : notebookItems) {
			size = item.getPreferredSize();
			lc.itemsPerRow = (b.height - yOff) / (size.height - 1);

			item.setBounds(xOff + x, y + insets.top, size.width, size.height);

			y += size.height - 1;

			if (!isModal) {
				if (y + size.height > b.height) {
					x += size.width + xOff;
					y = yOff;
				}
			}
		}

		Dimension d = main.getPreferredSize();
		d.width = x + size.width + xOff * 2;
		if (isModal) {
			d.height = y + size.height + yOff;
		}
		main.setPreferredSize(d);
	}

	@Override
	protected void deselectAll() {
		super.deselectAll();
		if (isModal) {
			bMove.setEnabled(false);
		}
	}

	@Override
	public void changeSelection(int delta, int keyCode) {
		boolean sideways = keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_RIGHT;

		NotebookItem item = lc.changeSelection(notebookItems, selectedItem, delta, sideways);
		if (item != null) {
			selectNotebook(item);
		}
	}

	void selectNotebook(NotebookItem item) {
		deselectAll();
		item.setSelected(true);
		selectedItem = item;

		if (isModal) {
			bMove.setEnabled(true);
			bMove.requestFocusInWindow();
		}

		if (isModal) {
			lc.updateVerticalScrollbar(item, scroll);
		} else {
			lc.updateHorizontalScrollbar(item, scroll);
		}
	}

	public void openSelected() {
		if (selectedItem != null) {
			window.showNotebook(selectedItem.notebook);
		}
	}

	@Override
	protected void vkEnter() {
		if (selectedItem != null && naListener != null) {
			naListener.didSelect(selectedItem.notebook);
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

	class NotebookItem extends BackgroundPanel implements ToolbarList.ToolbarListItem, MouseListener {
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
