package com.pinktwins.elephant.data;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;

import com.google.common.eventbus.Subscribe;
import com.pinktwins.elephant.Elephant;
import com.pinktwins.elephant.eventbus.VaultEvent;
import com.pinktwins.elephant.util.Factory;

// 'root' data provider
public class Vault {

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
	}

	public List<Notebook> getNotebooks() {
		return notebooks;
	}

	public Collection<Notebook> getNotebooksWithFilter(final String text) {
		if (text == null || text.length() == 0) {
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

}
