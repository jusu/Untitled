package com.pinktwins.elephant;

import java.awt.Image;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class ImageScalingCache {

	public int maxSize = 100;

	HashMap<String, Image> map = new HashMap<String, Image>();
	ArrayList<String> gets = new ArrayList<String>();

	public Image get(File sourceFile, int scaledWidth, int scaledHeight) {
		String key = key(sourceFile, scaledWidth, scaledHeight);
		Image i = map.get(key);
		if (i != null) {
			gets.remove(key);
			gets.add(key);
		}
		return i;
	}

	public void put(File sourceFile, int scaledWidth, int scaledHeight, Image img) {
		String key = key(sourceFile, scaledWidth, scaledHeight);
		map.put(key, img);

		if (map.size() > maxSize) {
			String oldestGet = gets.get(0);
			gets.remove(0);
			map.remove(oldestGet);
		}
	}

	private String key(File f, int w, int h) {
		return String.format("%s:%d:%d", f.getAbsolutePath(), w, h);
	}
}
