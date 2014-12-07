package com.pinktwins.elephant.data;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.common.eventbus.Subscribe;
import com.pinktwins.elephant.Elephant;

// 'root' data provider
public class Vault {

	private static Vault instance = null;

	static public Vault getInstance() {
		if (instance == null) {
			instance = new Vault();
		}
		return instance;
	}

	// XXX locate HOME
	private final String HOME = "/Users/jusu/Desktop/elephant";

	private File home;
	private File trash;

	private static ArrayList<Notebook> notebooks = new ArrayList<Notebook>();

	private Vault() {
		Elephant.eventBus.register(this);

		populate();
	}

	public File getHome() {
		return home;
	}

	public File getTrash() {
		return trash;
	}

	private void populate() {
		home = new File(HOME);

		trash = new File(home.getAbsolutePath() + File.separator + "Trash");
		trash.mkdirs();

		for (File f : home.listFiles()) {
			if (Files.isDirectory(f.toPath())) {
				if (findNotebook(f) == null) {
					notebooks.add(new Notebook(f));
				}
			}
		}

		sortNotebooks();
	}

	private void sortNotebooks() {
		Collections.sort(notebooks, new Comparator<Notebook>() {
			@Override
			public int compare(Notebook o1, Notebook o2) {
				return o1.name().toLowerCase().compareTo(o2.name().toLowerCase());
			}
		});
	}

	public List<Notebook> getNotebooks() {
		return notebooks;
	}

	public Notebook findNotebook(File f) {
		for (Notebook n : notebooks) {
			if (n.equals(f)) {
				return n;
			}
		}
		return null;
	}

	@Subscribe
	public void handleVaultEvent(VaultEvent event) {
		populate();
	}

}
