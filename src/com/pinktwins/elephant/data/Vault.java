package com.pinktwins.elephant.data;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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
		for (File f : home.listFiles()) {
			if (Files.isDirectory(f.toPath())) {
				notebooks.add(new Notebook(f));
			}
			
		}

		trash = new File(home.getAbsolutePath() + File.separator + "Trash");
		trash.mkdirs();

		Collections.sort(notebooks, new Comparator<Notebook>(){
			@Override
			public int compare(Notebook o1, Notebook o2) {
				return o1.name().toLowerCase().compareTo(o2.name().toLowerCase());
			}
		});
	}

	public List<Notebook> getNotebooks() {
		return notebooks;
	}

}

