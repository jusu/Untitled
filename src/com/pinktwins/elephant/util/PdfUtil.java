package com.pinktwins.elephant.util;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFPage;
import com.sun.pdfview.PDFParseException;
import com.sun.pdfview.PDFRenderer;

// Based on:
// http://stackoverflow.com/questions/550129/export-pdf-pages-to-a-series-of-images-in-java
//
// Using "PDF Renderer" from Swinglabs:
// https://java.net/projects/pdf-renderer/downloads
//
// This is not the best renderer. Other options:
//
// - pdfbox: slow, bad output on some pdfs I tested with
// - iText: not a renderer
// - JPedal: commercial, insanely expensive
// - jPod: will check this
// - gnujpdf: no way
// - PDFJet: commercial
// - ICEpdf: free versions doesn't have "font renderer", possibly limited
// - jmupdf: by far fastests and most compatible, JNI wrapper to a native library.
//    latest version was missing binary for Linux. Might be an option to switch to. 

public class PdfUtil {

	private static final Logger LOG = Logger.getLogger(PdfUtil.class.getName());

	RandomAccessFile raf;
	PDFFile pdffile;
	int numPages;

	private static final int screenDpi = Toolkit.getDefaultToolkit().getScreenResolution();

	static {
		// It either works or not. Keep quiet.
		PDFRenderer.setSuppressSetErrorStackTrace(true);
	}

	public PdfUtil(File f) {
		try {
			raf = new RandomAccessFile(f, "r");
			FileChannel channel = raf.getChannel();
			ByteBuffer buf = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());

			pdffile = new PDFFile(buf);
			numPages = pdffile.getNumPages();
		} catch (FileNotFoundException e) {
			LOG.severe("Fail: " + e);
		} catch (IOException e) {
			LOG.severe("Fail: " + e);
		} catch (NullPointerException e) {
			LOG.severe("Fail: " + e);
			LOG.severe("Got NPE on file " + f);
		}
	}

	public int numPages() {
		return numPages;
	}

	public Dimension pageSize(int n) {
		PDFPage page = pdffile.getPage(n);
		return new Dimension((int) page.getBBox().getWidth(), (int) page.getBBox().getHeight());
	}

	public long getPageBBoxWidth() {
		if (pdffile == null) {
			return -1;
		}
		if (pdffile.getNumPages() <= 0) {
			return -1;
		}
		return (long) pdffile.getPage(0).getBBox().getWidth();
	}

	public Image writePage(int n, File outPath, int minimumWidth) {
		BufferedImage bImg = null;

		try {
			PDFPage page = pdffile.getPage(n);

			// Improve the image quality slightly compared to assuming 72dpi.
			// XXX magic formula
			double adjust = (screenDpi / 72.0 - 1.0) / 2.0 + 1.0;

			Rectangle rect = new Rectangle(0, 0, (int) page.getBBox().getWidth(), (int) page.getBBox().getHeight());

			// If minimumWidth is given, adjust to get at least that.
			if (minimumWidth != -1 && rect.width * adjust < minimumWidth) {
				adjust = minimumWidth / (float) rect.width;
			}

			Image img = page.getImage((int) (rect.width * adjust), (int) (rect.height * adjust), rect, // clip rect
					null, // null for the ImageObserver
					true, // fill background with white
					true // block until drawing is done
					);

			bImg = toBufferedImage(img);
			ImageIO.write(bImg, "png", outPath);
		} catch (PDFParseException e) {
			LOG.severe("Fail: " + e);
		} catch (IOException e) {
			LOG.severe("Fail: " + e);
		}

		return bImg;
	}

	public void close() {
		if (raf != null) {
			try {
				raf.close();
			} catch (IOException e) {
				LOG.severe("Fail: " + e);
			}
		}
	}

	private static BufferedImage toBufferedImage(Image image) {
		if (image instanceof BufferedImage) {
			return (BufferedImage) image;
		}

		image = new ImageIcon(image).getImage();

		boolean hasAlpha = hasAlpha(image);
		BufferedImage bimage = null;
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

		try {
			int transparency = Transparency.OPAQUE;
			if (hasAlpha) {
				transparency = Transparency.BITMASK;
			}
			GraphicsDevice gs = ge.getDefaultScreenDevice();
			GraphicsConfiguration gc = gs.getDefaultConfiguration();
			bimage = gc.createCompatibleImage(image.getWidth(null), image.getHeight(null), transparency);
		} catch (HeadlessException e) {
		}

		if (bimage == null) {
			int type = BufferedImage.TYPE_INT_RGB;
			if (hasAlpha) {
				type = BufferedImage.TYPE_INT_ARGB;
			}
			bimage = new BufferedImage(image.getWidth(null), image.getHeight(null), type);
		}

		Graphics g = bimage.createGraphics();
		g.drawImage(image, 0, 0, null);
		g.dispose();
		return bimage;
	}

	private static boolean hasAlpha(Image image) {
		if (image instanceof BufferedImage) {
			BufferedImage bimage = (BufferedImage) image;
			return bimage.getColorModel().hasAlpha();
		}

		PixelGrabber pg = new PixelGrabber(image, 0, 0, 1, 1, false);
		try {
			pg.grabPixels();
		} catch (InterruptedException e) {
		}

		ColorModel cm = pg.getColorModel();
		return cm.hasAlpha();
	}
}
