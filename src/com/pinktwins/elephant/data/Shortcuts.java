package com.pinktwins.elephant.data;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.eventbus.Subscribe;
import com.pinktwins.elephant.Elephant;
import com.pinktwins.elephant.eventbus.NotebookEvent;
import com.pinktwins.elephant.eventbus.ShortcutsChangedEvent;
import com.pinktwins.elephant.util.Factory;
import com.pinktwins.elephant.util.IOUtil;

public class Shortcuts {

	private static final Logger LOG = Logger.getLogger(Shortcuts.class.getName());

	private List<String> list = Factory.newArrayList();

	public Shortcuts() {
		Elephant.eventBus.register(this);
	}

	private File shortcutsFile() {
		return new File(Vault.getInstance().getHome() + File.separator + ".shortcuts");
	}

	public List<String> load() {
		list.clear();

		JSONObject o = IOUtil.loadJson(shortcutsFile());
		if (o.has("list")) {
			try {
				JSONArray arr = o.getJSONArray("list");

				for (int n = 0, len = arr.length(); n < len; n++) {
					String s = arr.getString(n);
					list.add(s);
				}
			} catch (JSONException e) {
				LOG.severe("Fail: " + e);
			}
		}

		return list;
	}

	public List<String> list() {
		return list;
	}

	@Subscribe
	public void handleNotebookChanged(NotebookEvent event) {
		switch (event.kind) {
		case noteCreated:
			break;
		case noteMoved:
		case noteRenamed:
			if (event.source == null || event.dest == null) {
				return;
			}

			String prefix = Vault.getInstance().getHome() + File.separator;
			String oldPath = event.source.getAbsolutePath();
			boolean modified = false;

			for (int n = 0; n < list.size(); n++) {
				String s = list.get(n);

				String fullShortcut = prefix + s;

				if (oldPath.equals(fullShortcut)) {
					String newPath = event.dest.getAbsolutePath();
					newPath = newPath.replace(prefix, "");
					list.remove(n);
					list.add(n, newPath);
					modified = true;
				}
			}

			if (modified) {
				save();
				new ShortcutsChangedEvent().post();
			}

			break;
		default:
			break;

		}
	}

	private void save() {
		JSONArray arr = new JSONArray();

		for (String s : list) {
			arr.put(s);
		}

		JSONObject o = new JSONObject();
		try {
			o.put("list", arr);
			IOUtil.writeFile(shortcutsFile(), o.toString(4));
		} catch (JSONException e) {
			LOG.severe("Fail: " + e);
		} catch (IOException e) {
			LOG.severe("Fail: " + e);
		}
	}
}
