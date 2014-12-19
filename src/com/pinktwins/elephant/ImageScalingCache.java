package com.pinktwins.elephant;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;

import javax.imageio.ImageIO;

import org.apache.commons.io.FilenameUtils;

import com.pinktwins.elephant.data.Vault;

public class ImageScalingCache {

	public int maxSize = 100;

	HashMap<String, Image> map = new HashMap<String, Image>();
	ArrayList<String> gets = new ArrayList<String>();

	public Image get(File sourceFile, int w, int h) {
		String key = key(sourceFile, w, h);
		Image i = map.get(key);

		if (i == null) {
			i = load(sourceFile, w, h);
		}

		if (i != null) {
			gets.remove(key);
			gets.add(key);
		}

		return i;
	}

	public void put(File sourceFile, int w, int h, Image img) {
		String key = key(sourceFile, w, h);
		map.put(key, img);
		store(sourceFile, img, w, h);

		if (map.size() > maxSize) {
			String oldestGet = gets.get(0);
			gets.remove(0);
			map.remove(oldestGet);
		}
	}

	private File getCacheFile(File f, int w, int h) {
		String relativePath = f.getAbsolutePath().replace(Vault.getInstance().getHome().getAbsolutePath() + File.separator, "");
		String cacheName = relativePath.replaceAll(Matcher.quoteReplacement(File.separator), "_");

		int n = cacheName.indexOf("." + FilenameUtils.getExtension(cacheName));
		cacheName = cacheName.substring(0, n) + "_" + w + "_" + h + cacheName.substring(n);

		File cache = new File(Vault.getInstance().getHome().getAbsolutePath() + File.separator + ".imagecache" + File.separator + cacheName);
		return cache;
	}

	// XXX purge old cache files
	private void store(File sourceFile, Image img, int w, int h) {
		String ext = FilenameUtils.getExtension(sourceFile.getName()).toUpperCase();
		File cache = getCacheFile(sourceFile, w, h);
		cache.getParentFile().mkdirs();
		try {
			ImageIO.write(toBufferedImage(img, "PNG".equals(ext)), ext, cache);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private Image load(File sourceFile, int w, int h) {
		File cache = getCacheFile(sourceFile, w, h);
		if (!cache.exists()) {
			return null;
		}

		try {
			return ImageIO.read(cache);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	private String key(File f, int w, int h) {
		return String.format("%s:%d:%d", f.getAbsolutePath(), w, h);
	}

	// http://stackoverflow.com/questions/13605248/java-converting-image-to-bufferedimage
	private BufferedImage toBufferedImage(Image img, boolean alpha) {
		if (img instanceof BufferedImage) {
			return (BufferedImage) img;
		}

		// Create a buffered image with transparency
		BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), alpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);

		// Draw the image on to the buffered image
		Graphics2D bGr = bimage.createGraphics();
		bGr.drawImage(img, 0, 0, null);
		bGr.dispose();

		// Return the buffered image
		return bimage;
	}
}
