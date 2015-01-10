package com.pinktwins.elephant.data;

import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;

import com.google.common.eventbus.Subscribe;
import com.pinktwins.elephant.Elephant;
import com.pinktwins.elephant.data.WatchDir.WatchDirListener;
import com.pinktwins.elephant.eventbus.VaultEvent;
import com.pinktwins.elephant.util.Factory;

// 'root' data provider
public class Vault implements WatchDirListener {

	public static final String vaultFolderSettingName = "noteFolder";

	private static Vault instance = null;

	static public Vault getInstance() {
		if (instance == null) {
			instance = new Vault();
		}
		return instance;
	}

	private String HOME = "";
	private String defaultNotebook = "Inbox";

	private File home;
	private File trash;

	private ArrayList<Notebook> notebooks = Factory.newArrayList();
	private Tags tags = new Tags();

	WatchDir watchDir;

	private Vault() {
		Elephant.eventBus.register(this);

		String def = Elephant.settings.getString("defaultNotebook");
		if (!def.isEmpty()) {
			defaultNotebook = def;
		}
	}

	public File getHome() {
		return home;
	}

	public boolean hasLocation() {
		return !HOME.isEmpty();
	}

	public void setLocation(String vaultPath) {
		if (new File(vaultPath).exists()) {
			Elephant.settings.set(vaultFolderSettingName, vaultPath);
			HOME = vaultPath;
			populate();
		}
	}

	public Notebook getDefaultNotebook() {
		if (!hasLocation()) {
			return null;
		}
		File f = new File(home.getAbsolutePath() + File.separator + defaultNotebook);
		Notebook nb = findNotebook(f);
		if (nb == null && notebooks.size() > 0) {
			nb = notebooks.get(0);
		}
		return nb;
	}

	public File getTrash() {
		return trash;
	}

	public void populate() {
		home = new File(HOME);

		trash = new File(home.getAbsolutePath() + File.separator + "Trash");
		trash.mkdirs();

		for (File f : home.listFiles()) {
			if (f.isDirectory() && f.getName().charAt(0) != '.') {
				if (findNotebook(f) == null) {
					notebooks.add(new Notebook(f));
				}
			}
		}

		Collections.sort(notebooks);

		tags.reload(home.getAbsolutePath() + File.separator + ".tags");

		if (watchDir == null) {
			new Thread() {
				@Override
				public void run() {
					try {
						watchDir = new WatchDir(HOME, false, Vault.this);
						watchDir.processEvents();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}.start();
		}
	}

	public List<Notebook> getNotebooks() {
		return notebooks;
	}

	public Collection<Notebook> getNotebooksWithFilter(final String text) {
		if (text == null || text.isEmpty()) {
			return notebooks;
		}

		final String lo = text.toLowerCase();
		return CollectionUtils.select(notebooks, new Predicate<Notebook>() {
			@Override
			public boolean evaluate(Notebook nb) {
				return nb.name().toLowerCase().indexOf(lo) >= 0;
			}
		});
	}

	public Collection<Tag> getTagsWithFilter(String text) {
		if (text == null || text.isEmpty()) {
			return tags.asList();
		}

		final String lo = text.toLowerCase();
		return CollectionUtils.select(tags.asList(), new Predicate<Tag>() {
			@Override
			public boolean evaluate(Tag t) {
				return t.name.toLowerCase().indexOf(lo) >= 0;
			}
		});
	}

	public Notebook findNotebook(File f) {
		for (Notebook n : notebooks) {
			if (n.equals(f)) {
				return n;
			}
		}
		return null;
	}

	public List<String> resolveTagNames(List<String> tagNames) {
		return tags.resolveNames(tagNames);
	}

	public List<String> resolveTagIds(List<String> tagIds) {
		return tags.resolveIds(tagIds);
	}

	@Subscribe
	public void handleVaultEvent(VaultEvent event) {
		populate();
	}

	@Override
	public void watchEvent(final String kind, final String file) {
		if ("ENTRY_MODIFY".equals(kind)) {
			EventQueue.invokeLater(new Runnable() {
				@Override
				public void run() {
					Notebook nb = findNotebook(new File(file));
					if (nb != null) {
						nb.refresh();
						Elephant.eventBus.post(new VaultEvent(VaultEvent.Kind.notebookRefreshed, nb));
					}
				}
			});
		}
	}
}
