package com.pinktwins.elephant;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import javax.imageio.ImageIO;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import com.pinktwins.elephant.data.Vault;

public class ImageScalingCache {

	private static final Logger LOG = Logger.getLogger(ImageScalingCache.class.getName());
	private long lastPurgeTs = System.currentTimeMillis();
	private final long purgeAfterMs = 1000 * 60 * 60 * 6;

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

	private void store(File sourceFile, Image img, int w, int h) {
		String ext = FilenameUtils.getExtension(sourceFile.getName()).toUpperCase();
		File cache = getCacheFile(sourceFile, w, h);
		cache.getParentFile().mkdirs();
		try {
			ImageIO.write(toBufferedImage(img, "PNG".equals(ext)), ext, cache);
		} catch (IOException e) {
			LOG.severe("Fail: " + e);
		}

		try {
			purgeOldCacheFiles();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void purgeOldCacheFiles() throws IOException {
		if (System.currentTimeMillis() - lastPurgeTs < purgeAfterMs) {
			return;
		}

		LOG.info("Purging old, unused cache files.");
		long start = System.currentTimeMillis();
		long count = 0;
		// Purge last accessed more than x days ago
		FileTime olderThan = FileTime.fromMillis(System.currentTimeMillis() - 1000 * 3600 * 24 * 120l);

		File folder = new File(getImageCacheDir());
		for (File f : folder.listFiles()) {
			if (f.isFile()) {
				String name = f.getName();
				String ext = FilenameUtils.getExtension(f.getName()).toLowerCase();
				if (name.charAt(0) != '.' && (ext.equals("png") || ext.equals("jpg"))) {
					BasicFileAttributes attrs = Files.readAttributes(Paths.get(f.getAbsolutePath()), BasicFileAttributes.class);
					if (attrs.lastAccessTime().compareTo(olderThan) < 0) {
						FileUtils.deleteQuietly(f);
						count++;
					}
				}
			}
		}

		lastPurgeTs = start;

		LOG.info("Purging " + count + " took " + (System.currentTimeMillis() - start) + " ms.");
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
