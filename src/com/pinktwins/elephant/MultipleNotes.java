package com.pinktwins.elephant;

import java.awt.Image;
import java.util.Iterator;
import java.util.Set;

import com.pinktwins.elephant.data.Note;
import com.pinktwins.elephant.util.Images;

public class MultipleNotes extends BackgroundPanel {

	private static Image tile;

	static {
		Iterator<Image> i = Images.iterator(new String[] { "notebooks" });
		tile = i.next();
	}

	public MultipleNotes(ElephantWindow w) {
		super(tile);
		createComponents();
	}

	private void createComponents() {

	}

	public void load(Set<Note> selection) {

	}
}
