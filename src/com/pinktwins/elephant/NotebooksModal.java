package com.pinktwins.elephant;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

// 'Modal' version of Notebooks view, used by NotebookChooser

public class NotebooksModal extends ToolbarList<NotebooksModal.NotebookItem> {

	private static Image tile, notebookBg, notebookBgSelected, notebooksHLine, newNotebook;

	private ElephantWindow window;
	private NotebookActionListener naListener;

	static {
		Iterator<Image> i = Images.iterator(new String[] { "notebooks", "notebookBg", "notebookBgSelected", "notebooksHLine", "newNotebook" });
		tile = i.next();
		notebookBg = i.next();
		notebookBgSelected = i.next();
		notebooksHLine = i.next();
		newNotebook = i.next();
	}

	private String modalHeader;
	private boolean isJump;

	public NotebooksModal(ElephantWindow w, String modalHeader) {
		super(tile, newNotebook, "Find a notebook");

		window = w;
		this.modalHeader = modalHeader;

		Elephant.eventBus.register(this);

		initialize();
		layoutItemHeightAdjustment = -1;
		layoutXOffAdjustment = isJump ? 80 : 56;
		layoutYOffAdjustment = -49;
		layoutHeightOnly = true;
	}

	public void setNotebookActionListener(NotebookActionListener l) {
		naListener = l;
	}

	@Subscribe
	public void handleNotebookEvent(NotebookEvent event) {
		refresh();
		revalidate();
	}

	JButton bMove;

	Color colorMove = Color.decode("#eaeaea");
	Color colorJump = Color.decode("#fdfdfd"); //#f6f6f6");

	@Override
	protected void createComponents() {
		setLayout(null);
		setImage(null);

		isJump = modalHeader.isEmpty(); // gah

		setBackground(isJump ? colorJump : colorMove);

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
		if (isJump) {
			search.useV4();
		} else {
			search.useV3();
		}
		search.setFixedColor(Color.WHITE);

		search.windowFocusGained();

		JLabel title = new JLabel(modalHeader, JLabel.CENTER);
		title.setFont(ElephantWindow.fontModalHeader);
		title.setBounds(0, 14, NotebookChooser.fixedWidth, 40);
		tools.add(title);
		tools.add(search);

		scroll = new JScrollPane(main);
		scroll.setBorder(ElephantWindow.emptyBorder);
		scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.getVerticalScrollBar().setUnitIncrement(5);

		if (isJump) {
			scroll.setBounds(0, 44, NotebookChooser.fixedWidth, NotebookChooser.fixedHeight - 56);
		} else {
			scroll.setBorder(BorderFactory.createLineBorder(Color.decode("#d9d9d9"), 1));
			scroll.setBounds(18, 103, 424 - 18, 564 - 103);
		}

		add(tools);
		add(scroll);

		scroll.setOpaque(true);
		scroll.setBackground(isJump ? colorJump : Color.decode("#e6e6e6"));

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

		addComponentListeners();
	}

	@Override
	protected void newButtonAction() {
		window.newNotebookAction.actionPerformed(null);
	}

	@Override
	protected List<NotebookItem> queryFilter(String text) {
		ArrayList<NotebookItem> items = Factory.newArrayList();
		for (Notebook nb : Vault.getInstance().getNotebooksWithFilter(search.getText())) {
			items.add(new NotebookItem(nb));
		}
		return items;
	}

	@Override
	protected void deselectAll() {
		super.deselectAll();
		bMove.setEnabled(false);
	}

	@Override
	protected void selectItem(NotebookItem item) {
		super.selectItem(item);

		bMove.setEnabled(true);
		bMove.requestFocusInWindow();

		lc.updateVerticalScrollbar(item, scroll);
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

	// XXX rewrite to look right
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
				Elephant.eventBus.post(new VaultEvent(VaultEvent.Kind.notebookCreated, notebook));
				Elephant.eventBus.post(new VaultEvent(VaultEvent.Kind.notebookListChanged, notebook));

				for (NotebookItem item : itemList) {
					if (item.notebook.equals(notebook.folder())) {
						selectItem(item);
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
			Elephant.eventBus.post(new VaultEvent(VaultEvent.Kind.notebookListChanged, notebook));
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
