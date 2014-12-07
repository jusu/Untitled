package com.pinktwins.elephant;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.ArrayList;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.eventbus.Subscribe;
import com.pinktwins.elephant.data.Note;
import com.pinktwins.elephant.data.NoteChangedEvent;
import com.pinktwins.elephant.data.Vault;

public class SideBarList extends JPanel {
	private static final long serialVersionUID = 2473401062148102911L;

	ArrayList<SideBarListItem> items = new ArrayList<SideBarListItem>();

	private ElephantWindow window;
	
	public SideBarList(ElephantWindow w) {
		window = w;
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

		createComponents();
	}

	private void createComponents() {
		removeAll();
		setLayout(new GridLayout(0, 1));

		for (SideBarListItem item : items) {
			add(item);
		}
	}

	enum Icon {
		none, notebookSmall, notebookLarge, noteSmall, noteLarge
	};

	class SideBarListItem extends JPanel {
		private static final long serialVersionUID = 4837771971000290113L;

		File file;
		Icon icon;
		String target;
		JLabel label;
		
		@Subscribe
		public void handleNoteChanged(NoteChangedEvent event) {
			if (event.note.equals(file)) {
				refresh();
			}
		}

		public void refresh() {
			if (file.isDirectory()) {
				icon = Icon.notebookSmall;
				label.setText(file.getName());
			} else {
				icon = Icon.noteSmall;
				Note note = new Note(file);
				label.setText(note.getMeta().title());
			}
		}
		
		public SideBarListItem(String targetFileName) {
			setOpaque(false);

			Elephant.eventBus.register(this);
			
			String path = Vault.getInstance().getHome() + File.separator + targetFileName;

			file = new File(path);
			target = file.getAbsolutePath();

			label = new JLabel("");
			label.setForeground(Color.LIGHT_GRAY);
			label.setFont(ElephantWindow.fontBoldNormal);
			add(label);

			refresh();

			addMouseListener(new MouseListener() {
				@Override
				public void mouseClicked(MouseEvent e) {
					window.openShortcut(target);
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
			});
		}
	}
}
