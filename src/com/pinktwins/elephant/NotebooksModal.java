package com.pinktwins.elephant;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.google.common.eventbus.Subscribe;
import com.pinktwins.elephant.data.Notebook;
import com.pinktwins.elephant.data.Vault;
import com.pinktwins.elephant.eventbus.NotebookEvent;
import com.pinktwins.elephant.util.Factory;
import com.pinktwins.elephant.util.Images;

// 'Modal' version of Notebooks view, used by NotebookChooser

public class NotebooksModal extends ToolbarList<NotebooksModal.NotebookItem> {

	private static Image tile, notebookChooserSelected, notebookChooserSelectedLarge, notebookChooserTop, notebooksHLine, newNotebook;

	private ElephantWindow window;
	private NotebookActionListener naListener;

	static {
		Iterator<Image> i = Images.iterator(new String[] { "notebooks", "notebookChooserSelected", "notebookChooserSelectedLarge", "notebookChooserTop",
				"notebooksHLine", "newNotebook" });
		tile = i.next();
		notebookChooserSelected = i.next();
		notebookChooserSelectedLarge = i.next();
		notebookChooserTop = i.next();
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
		layoutXOffAdjustment = isJump ? -9 : -11;
		layoutYOffAdjustment = -49;
		layoutHeightOnly = true;
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);

		if (!isJump) {
			g.drawImage(notebookChooserTop, 0, 0, null);
		}
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
	Color colorJump = Color.decode("#fdfdfd"); // #f6f6f6");

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

		search = new SearchTextField("Find a notebook", ElephantWindow.fontMedium);
		search.setBorder(BorderFactory.createEmptyBorder(0, 22, 0, 20));
		if (isJump) {
			search.setBounds(14, 10, 414 - (NotebookChooser.fixedWidth - NotebookChooser.fixedWidthJump), 26);
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
		title.setBounds(0, 14, isJump ? NotebookChooser.fixedWidthJump : NotebookChooser.fixedWidth, 40);
		tools.add(title);
		tools.add(search);

		scroll = new JScrollPane(main);
		scroll.setBorder(ElephantWindow.emptyBorder);
		scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.getVerticalScrollBar().setUnitIncrement(5);
		scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

		if (isJump) {
			scroll.setBounds(0, 44, NotebookChooser.fixedWidthJump, NotebookChooser.fixedHeight - 56);
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
	protected void trashButtonAction() {
	}

	@Override
	protected List<NotebookItem> queryFilter(String text) {
		List<NotebookItem> items = Factory.newArrayList();

		if (isJump && search.getText().isEmpty()) {
			items.add(this.createSpecialNotebookItem(SpecialItems.ALL_NOTES));
		}

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
			selectedItem.resolveSpecialType();
			naListener.didSelect(selectedItem.notebook);
		}
	}

	private static enum SpecialItems {
		NONE(""), ALL_NOTES("All Notes");

		private final String label;

		SpecialItems(String label) {
			this.label = label;
		}
	};

	NotebookItem createSpecialNotebookItem(SpecialItems type) {
		Notebook nb = new Notebook();

		switch (type) {
		case ALL_NOTES:
			nb.setName(SpecialItems.ALL_NOTES.label);
			break;
		case NONE:
			break;
		}

		NotebookItem item = new NotebookItem(nb);
		item.setSpecialType(type);
		return item;
	}

	Color colorTextUnselected = Color.decode("#0a0a0a");

	class NotebookItem extends BackgroundPanel implements ToolbarList.ToolbarListItem, MouseListener {
		private static final long serialVersionUID = -7285867977183764620L;

		private Notebook notebook;
		private Dimension size = new Dimension(383, 21);
		private JLabel name;
		private SpecialItems specialType = SpecialItems.NONE;

		public NotebookItem(Notebook nb) {
			super();

			setLayout(null);
			setOpaque(false);
			setStyle(BackgroundPanel.ACTUAL);

			notebook = nb;

			name = new JLabel(nb.name());
			name.setBorder(BorderFactory.createEmptyBorder(0, 28, 0, 0));
			name.setForeground(colorTextUnselected);
			name.setFont(ElephantWindow.fontNotebookChooser);

			add(name);

			name.setBounds(0, 0, 200, 21);

			addMouseListener(this);
		}

		public void setSpecialType(SpecialItems type) {
			specialType = type;
			if (type != SpecialItems.NONE) {
				name.setFont(name.getFont().deriveFont(name.getFont().getStyle() | Font.BOLD));
				name.setBorder(BorderFactory.createEmptyBorder(5, 28, 0, 0));
				size.height = 26;
			}
		}

		public void resolveSpecialType() {
			if (specialType == SpecialItems.ALL_NOTES) {
				notebook = Notebook.getNotebookWithAllNotes();
			}
		}

		@Override
		public void setSelected(boolean b) {
			if (b) {
				setImage(specialType == SpecialItems.NONE ? notebookChooserSelected : notebookChooserSelectedLarge);
				name.setForeground(Color.WHITE);
				selectedItem = this;
			} else {
				name.setForeground(colorTextUnselected);
				setImage(null);
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
			// Jumping: one click is enough.
			// Moving: need double click.
			if (isJump || e.getClickCount() == 2) {
				resolveSpecialType();

				if (naListener != null) {
					naListener.didSelect(notebook);
				} else {
					window.showNotebook(notebook);
				}
			}
		}

		@Override
		public void mousePressed(MouseEvent e) {
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

	@Override
	protected void doneEditing(NotebookItem item, String text) {
	}

	@Override
	protected void cancelEditing(NotebookItem item) {
	}

}
