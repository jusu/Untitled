package com.pinktwins.elephant;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import javax.imageio.ImageIO;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;

import com.pinktwins.elephant.data.Vault;

public class ImageScalingCache {

	private static final Logger LOG = Logger.getLogger(ImageScalingCache.class.getName());

	public Image get(File sourceFile, int w, int h) {
		return load(sourceFile, w, h);
	}

	public void put(File sourceFile, int w, int h, Image img) {
		store(sourceFile, img, w, h);
	}

	public static String getImageCacheDir() {
		return Vault.getInstance().getHome().getAbsolutePath() + File.separator + ".imagecache";
	}

	private File getCacheFile(File f, int w, int h) {
		String relativePath = f.getAbsolutePath().replace(Vault.getInstance().getHome().getAbsolutePath() + File.separator, "");
		String cacheName = relativePath.replaceAll(Matcher.quoteReplacement(File.separator), "_");

		long lastMod = f.lastModified();
		long fileSize = f.length();

		String ext = FilenameUtils.getExtension(cacheName);
		int n = cacheName.indexOf("." + ext);
		cacheName = cacheName.substring(0, n) + "_" + lastMod + "_" + fileSize + "_" + w + "_" + h + cacheName.substring(n);
		String hash = DigestUtils.md5Hex(cacheName);

		return new File(getImageCacheDir() + File.separator + hash + "." + ext);
	}

	// XXX purge old cache files
	private void store(File sourceFile, Image img, int w, int h) {
		String ext = FilenameUtils.getExtension(sourceFile.getName()).toUpperCase();
		File cache = getCacheFile(sourceFile, w, h);
		cache.getParentFile().mkdirs();
		try {
			ImageIO.write(toBufferedImage(img, "PNG".equals(ext)), ext, cache);
		} catch (IOException e) {
			LOG.severe("Fail: " + e);
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
			LOG.severe("Fail: " + e);
			return null;
		}
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
