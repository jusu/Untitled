package com.pinktwins.elephant;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.pinktwins.elephant.data.Note;
import com.pinktwins.elephant.data.Vault;
import com.pinktwins.elephant.util.Factory;
import com.pinktwins.elephant.util.Images;

public class SideBarList extends JPanel {
	private static final long serialVersionUID = 2473401062148102911L;

	private List<SideBarListItem> items = Factory.newArrayList();
	private JPanel itemGrid;

	private ElephantWindow window;
	private String header;
	private boolean allowModifying;
	public boolean highlightSelection = false;

	private final String fileMovedStr = "(file moved)";

	private static ImageIcon sidebarNote, sidebarNotebook, sidebarTag, sidebarSearch, sidebarNotesLarge, sidebarNotebooksLarge, sidebarTagsLarge;
	private static Image sidebarTile, largeHighlight, grayMinus;

	static {
		Iterator<Image> i = Images.iterator(new String[] { "sidebarNote", "sidebarNotebook", "sidebarTag", "sidebarSearch", "sidebarNotesLarge",
				"sidebarNotebooksLarge", "sidebarTagsLarge", "sidebar", "sidebarLargeHilight", "grayMinus" });
		sidebarNote = new ImageIcon(i.next());
		sidebarNotebook = new ImageIcon(i.next());
		sidebarTag = new ImageIcon(i.next());
		sidebarSearch = new ImageIcon(i.next());
		sidebarNotesLarge = new ImageIcon(i.next());
		sidebarNotebooksLarge = new ImageIcon(i.next());
		sidebarTagsLarge = new ImageIcon(i.next());
		sidebarTile = i.next();
		largeHighlight = i.next();
		grayMinus = i.next();
	}

	public interface SideBarItemModifier {
		public void swap(String a, String b);

		public void remove(String s);
	}

	final SidebarModifyingMouseListener modifyingListener;

	public void setItemModifier(SideBarItemModifier m) {
		modifyingListener.setItemModifier(m);
	}

	public SideBarList(ElephantWindow w, String header, boolean allowModifying) {
		window = w;
		this.header = header;
		this.allowModifying = allowModifying;

		setOpaque(false);

		modifyingListener = allowModifying ? new SidebarModifyingMouseListener(this, getCursor()) : null;
	}

	public void cleanup() {
		window = null;
	}

	public String getTarget(int n) {
		if (n >= 0 && n < items.size()) {
			return items.get(n).target;
		}
		return "";
	}

	public <T> void load(List<T> notes) {
		items.clear();

		for (T n : notes) {
			SideBarListItem item;

			if (n instanceof String) {
				item = new SideBarListItem((String) n);
			} else if (n instanceof Note) {
				item = new SideBarListItem((Note) n);
			} else {
				throw new IllegalArgumentException();
			}

			items.add(item);
		}

		createComponents(true);
	}

	public void addNavigation() {
		items.clear();

		SideBarListItem i;
		i = new SideBarListItem("Notes", sidebarNotesLarge, Sidebar.ACTION_NOTES);
		items.add(i);
		i = new SideBarListItem("Notebooks", sidebarNotebooksLarge, Sidebar.ACTION_NOTEBOOKS);
		items.add(i);
		i = new SideBarListItem("Tags", sidebarTagsLarge, Sidebar.ACTION_TAGS);
		items.add(i);

		createComponents(false);
	}

	private void createComponents(boolean useHeader) {
		removeAll();

		setLayout(new BorderLayout());

		itemGrid = new JPanel();
		itemGrid.setOpaque(false);
		itemGrid.setLayout(new GridLayout(0, 1));

		if (useHeader) {
			JLabel lHeader = new JLabel(header);
			lHeader.setForeground(Color.decode("#93989d"));
			lHeader.setBorder(BorderFactory.createEmptyBorder(6, 10, 3, 0));

			add(lHeader, BorderLayout.NORTH);
		}

		add(itemGrid, BorderLayout.CENTER);

		for (SideBarListItem item : items) {
			if (allowModifying) {
				item.addMouseListener(modifyingListener);
				item.addMouseMotionListener(modifyingListener);
			}
			itemGrid.add(item);
		}
	}

	protected void deselectAll() {
		if (highlightSelection) {
			for (SideBarListItem item : items) {
				item.setImage(sidebarTile);
				item.keepScaleOnRetina(false, false);
			}
		}
	}

	public void select(int idx) {
		if (highlightSelection) {
			deselectAll();
			if (idx >= 0 && idx < items.size()) {
				SideBarListItem item = items.get(idx);
				item.setImage(largeHighlight);
				item.keepScaleOnRetina(false, true);
			}
		}
	}

	class SideBarListItem extends BackgroundPanel {
		private static final long serialVersionUID = 4837771971000290113L;

		File file;
		String target;
		String rawInitString;
		JButton icon = new JButton();
		JLabel label = new JLabel("");

		public boolean drawMinus = false;

		public void refresh() {
			String s;

			String name = file.getName();

			if (name.startsWith("search:")) {
				target = name;

				String[] a = name.split(":");
				s = a[a.length - 1];

				if (name.startsWith("search:t:") || name.startsWith("search:tag:")) {
					icon.setIcon(sidebarTag);
					icon.setPressedIcon(sidebarTag);
				} else {
					icon.setIcon(sidebarSearch);
					icon.setPressedIcon(sidebarSearch);
				}
			} else {
				if (file.exists()) {
					if (file.isDirectory()) {
						icon.setIcon(sidebarNotebook);
						icon.setPressedIcon(sidebarNotebook);
						s = file.getName();
					} else {
						icon.setIcon(sidebarNote);
						icon.setPressedIcon(sidebarNote);
						Note note = new Note(file);
						s = note.getMeta().title();
					}

					if (s.length() > 16) {
						s = s.substring(0, 16) + "…";
					}
				} else {
					s = fileMovedStr;
				}
			}

			label.setText(s);
		}

		public void init() {
			// Elephant.eventBus.register(this);

			setOpaque(false);

			setLayout(new BorderLayout());
			setBorder(BorderFactory.createEmptyBorder(2, 12, 1, 12));

			icon.setBorderPainted(false);
			icon.setContentAreaFilled(false);
			icon.setBorder(ElephantWindow.emptyBorder);

			label.setForeground(Color.LIGHT_GRAY);
			label.setFont(ElephantWindow.fontSideBarText);
			label.setBorder(BorderFactory.createEmptyBorder(1, 6, 2, 0));

			add(icon, BorderLayout.WEST);
			add(label, BorderLayout.CENTER);

			MouseListener ml = new MouseListener() {
				@Override
				public void mouseClicked(MouseEvent e) {
					if (!window.openShortcut(target)) {
						label.setText(fileMovedStr);
					}
				}

				@Override
				public void mousePressed(MouseEvent e) {
					label.setForeground(Color.WHITE);
				}

				@Override
				public void mouseReleased(MouseEvent e) {
					label.setForeground(Color.LIGHT_GRAY);
				}

				@Override
				public void mouseEntered(MouseEvent e) {
				}

				@Override
				public void mouseExited(MouseEvent e) {
				}
			};

			addMouseListener(ml);
			icon.addMouseListener(ml);
		}

		public SideBarListItem(Image image) {
			super(image);
		}

		public SideBarListItem(String str) {
			init();

			String path = Vault.getInstance().getHome() + File.separator + str;

			file = new File(path);
			target = file.getAbsolutePath();
			rawInitString = str;

			refresh();
		}

		public SideBarListItem(Note note) {
			init();

			file = note.file();
			target = file.getAbsolutePath();

			refresh();
		}

		public SideBarListItem(String title, ImageIcon imageIcon, String targetAction) {
			super(sidebarTile);
			init();

			target = targetAction;
			label.setText(title);
			icon.setIcon(imageIcon);
			icon.setBorder(BorderFactory.createEmptyBorder(5, 0, 4, 0));
		}

		@Override
		public void paint(Graphics g) {
			super.paint(g);

			if (drawMinus) {
				g.drawImage(grayMinus, getWidth() - grayMinus.getWidth(null) - 8, (getHeight() - grayMinus.getHeight(null)) / 2, null);
			}
		}
	}
}
