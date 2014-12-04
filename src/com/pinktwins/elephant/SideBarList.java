package com.pinktwins.elephant;

import java.awt.Color;
import java.awt.GridLayout;
import java.io.File;
import java.util.ArrayList;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.pinktwins.elephant.data.Vault;

public class SideBarList extends JPanel {
	private static final long serialVersionUID = 2473401062148102911L;

	ArrayList<SideBarListItem> items = new ArrayList<SideBarListItem>();

	public SideBarList() {
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

		Icon icon;
		String label;
		String target;

		public SideBarListItem(String target) {
			setOpaque(false);

			String path = Vault.getInstance().getHome() + File.separator + target;

			File f = new File(path);
			if (f.isDirectory()) {
				icon = Icon.notebookSmall;
			} else {
				icon = Icon.noteSmall;
			}

			this.target = f.getAbsolutePath();
			label = f.getName();

			JLabel l = new JLabel(label);
			l.setForeground(Color.LIGHT_GRAY);
			l.setFont(ElephantWindow.fontBoldNormal);
			add(l);
		}
	}
}
