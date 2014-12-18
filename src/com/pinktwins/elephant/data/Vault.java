package com.pinktwins.elephant.data;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;

import com.google.common.eventbus.Subscribe;
import com.pinktwins.elephant.Elephant;

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

	private static ArrayList<Notebook> notebooks = new ArrayList<Notebook>();

	private Vault() {
		Elephant.eventBus.register(this);
	}

	public File getHome() {
		return home;
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

	private void populate() {
		home = new File(HOME);

		trash = new File(home.getAbsolutePath() + File.separator + "Trash");
		trash.mkdirs();

		for (File f : home.listFiles()) {
			if (f.isDirectory()) {
				if (findNotebook(f) == null) {
					notebooks.add(new Notebook(f));
				}
			}
		}

		Collections.sort(notebooks);
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

	public Notebook search(String text) {
		text = text.toLowerCase();

		Notebook found = new Notebook();
		found.setName(Notebook.NAME_SEARCH);

		for (Notebook nb : getNotebooks()) {
			if (!nb.isTrash()) {
				for (Note n : nb.notes) {
					boolean match = false;

					String title = n.getMeta().title();
					if (title.toLowerCase().indexOf(text) >= 0) {
						match = true;
					} else {
						String contents = n.contents();
						if (contents.toLowerCase().indexOf(text) >= 0) {
							match = true;
						}
					}

					if (match) {
						found.addNote(n);
					}
				}
			}
		}

		found.sortNotes();

		int len = found.notes.size();
		String s = len + " note";
		if (len != 1) {
			s += "s";
		}
		s += " found";

		found.setName(s);

		return found;
	}

	@Subscribe
	public void handleVaultEvent(VaultEvent event) {
		populate();
	}

	public void setLocation(String vaultPath) {
		if (new File(vaultPath).exists()) {
			Elephant.settings.set(vaultFolderSettingName, vaultPath);
			HOME = vaultPath;
			populate();
		}
	}

	public boolean hasLocation() {
		return !HOME.isEmpty();
	}

}
