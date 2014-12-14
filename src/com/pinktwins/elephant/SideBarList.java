package com.pinktwins.elephant;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.eventbus.Subscribe;
import com.pinktwins.elephant.data.IOUtil;
import com.pinktwins.elephant.data.Note;
import com.pinktwins.elephant.data.NoteChangedEvent;
import com.pinktwins.elephant.data.Vault;

public class SideBarList extends JPanel {
	private static final long serialVersionUID = 2473401062148102911L;

	ArrayList<SideBarListItem> items = new ArrayList<SideBarListItem>();

	private ElephantWindow window;
	private String header;
	public boolean highlightSelection = false;

	private static int NUM_IMAGES = 5;
	private final String fileMovedStr = "(file moved)";

	enum Images {
		sidebarNote, sidebarNotebook, sidebarNotesLarge, sidebarNotebooksLarge, sidebarTagsLarge
	};

	private static String[] imageFiles = { "sidebarNote", "sidebarNotebook", "sidebarNotesLarge", "sidebarNotebooksLarge", "sidebarTagsLarge" };
	private static ImageIcon[] imageIcons = new ImageIcon[NUM_IMAGES];
	private static Image sidebarTile, largeHighlight;

	static {
		try {
			int n = 0;
			for (String fileName : imageFiles) {
				Image i = ImageIO.read(Sidebar.class.getClass().getResourceAsStream(String.format("/images/%s.png", fileName)));
				imageIcons[n] = new ImageIcon(i);
				n++;
			}

			sidebarTile = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/sidebar.png"));
			largeHighlight = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/sidebarLargeHilight.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private ImageIcon getImageIcon(Images kind) {
		return imageIcons[kind.ordinal()];
	}

	public SideBarList(ElephantWindow w, String header) {
		window = w;
		this.header = header;
		setOpaque(false);
	}

	public String getTarget(int n) {
		if (n >= 0 && n < items.size()) {
			return items.get(n).target;
		}
		return "";
	}

	public void load(File f) {
		items.clear();

		JSONObject o = IOUtil.loadJson(f);
		if (o.has("list")) {
			try {
				JSONArray arr = o.getJSONArray("list");

				for (int n = 0, len = arr.length(); n < len; n++) {
					String s = arr.getString(n);

					SideBarListItem item = new SideBarListItem(s);
					items.add(item);
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		createComponents(true);
	}

	public void load(List<Note> notes) {
		items.clear();

		for (Note n : notes) {
			SideBarListItem item = new SideBarListItem(n);
			items.add(item);
		}

		createComponents(true);
	}

	public void addNavigation() {
		items.clear();

		SideBarListItem i;
		i = new SideBarListItem("Notes", Images.sidebarNotesLarge, Sidebar.ACTION_NOTES);
		items.add(i);
		i = new SideBarListItem("Notebooks", Images.sidebarNotebooksLarge, Sidebar.ACTION_NOTEBOOKS);
		items.add(i);
		i = new SideBarListItem("Tags", Images.sidebarTagsLarge, Sidebar.ACTION_TAGS);
		items.add(i);

		createComponents(false);
	}

	private void createComponents(boolean useHeader) {
		removeAll();

		setLayout(new BorderLayout());

		JPanel grid = new JPanel();
		grid.setOpaque(false);
		grid.setLayout(new GridLayout(0, 1));

		if (useHeader) {
			JLabel lHeader = new JLabel(header);
			lHeader.setForeground(Color.decode("#93989d"));
			lHeader.setBorder(BorderFactory.createEmptyBorder(6, 10, 3, 0));

			add(lHeader, BorderLayout.NORTH);
		}

		add(grid, BorderLayout.CENTER);

		for (SideBarListItem item : items) {
			grid.add(item);
		}
	}

	protected void deselectAll() {
		if (highlightSelection) {
			for (SideBarListItem item : items) {
				item.setImage(sidebarTile);
			}
		}
	}

	public void select(int idx) {
		if (highlightSelection) {
			deselectAll();
			if (idx >= 0 && idx < items.size()) {
				SideBarListItem item = items.get(idx);
				item.setImage(largeHighlight);
			}
		}
	}

	class SideBarListItem extends BackgroundPanel {
		private static final long serialVersionUID = 4837771971000290113L;

		File file;
		String target;
		JButton icon = new JButton();
		JLabel label = new JLabel("");

		@Subscribe
		public void handleNoteChanged(NoteChangedEvent event) {
			if (event.note.equals(file)) {
				refresh();
			}
		}

		public void refresh() {
			String s;

			if (file.exists()) {
				if (file.isDirectory()) {
					icon.setIcon(getImageIcon(Images.sidebarNotebook));
					icon.setPressedIcon(getImageIcon(Images.sidebarNotebook));
					s = file.getName();
				} else {
					icon.setIcon(getImageIcon(Images.sidebarNote));
					icon.setPressedIcon(getImageIcon(Images.sidebarNote));
					Note note = new Note(file);
					s = note.getMeta().title();
				}

				if (s.length() > 20) {
					s = s.substring(0, 20) + "…";
				}
			} else {
				s = fileMovedStr;
			}

			label.setText(s);
		}

		public void init() {
			Elephant.eventBus.register(this);

			setOpaque(false);

			setLayout(new BorderLayout());
			setBorder(BorderFactory.createEmptyBorder(2, 12, 2, 12));

			icon.setBorder(ElephantWindow.emptyBorder);

			label.setForeground(Color.LIGHT_GRAY);
			label.setFont(ElephantWindow.fontNormal);
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

		public SideBarListItem(String targetFileName) {
			init();

			String path = Vault.getInstance().getHome() + File.separator + targetFileName;

			file = new File(path);
			target = file.getAbsolutePath();

			refresh();
		}

		public SideBarListItem(Note note) {
			init();

			file = note.file();
			target = file.getAbsolutePath();

			refresh();
		}

		public SideBarListItem(String title, Images imageIcon, String targetAction) {
			super(sidebarTile);
			init();

			target = targetAction;
			label.setText(title);
			icon.setIcon(getImageIcon(imageIcon));
			icon.setBorder(BorderFactory.createEmptyBorder(5, 0, 4, 0));
		}
	}
}
