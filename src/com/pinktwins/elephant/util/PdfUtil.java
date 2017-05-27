package com.pinktwins.elephant.util;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.rendering.PDFRenderer;

public class PdfUtil {

	private static final Logger LOG = Logger.getLogger(PdfUtil.class.getName());

	PDDocument pdfDoc;
	PDFRenderer pdfRenderer;
	int numPages;

	private static final int screenDpi = Toolkit.getDefaultToolkit().getScreenResolution();

	static {
		// It either works or not. Shut up.
		String[] loggers = { "org.apache.pdfbox.util.PDFStreamEngine", "org.apache.pdfbox.pdmodel.font.PDSimpleFont", "org.apache.pdfbox.pdmodel.font.PDFont",
				"org.apache.pdfbox.pdmodel.font.FontManager", "org.apache.pdfbox.pdfparser.PDFObjectStreamParser", "org.apache.pdfbox.pdmodel.PDDocument",
				"org.apache.pdfbox.pdmodel.PDPage", "org.apache.pdfbox.tools.PDFToImage", "org.apache.pdfbox.pdmodel.graphics.xobject.PDPixelMap",
				"org.apache.fontbox.type1.Type1CharStringReader", "org.apache.fontbox.cff.CharStringHandler", "org.apache.fontbox", "org.apache.pdfbox" };
		for (String logger : loggers) {
			org.apache.log4j.Logger logpdfengine = org.apache.log4j.Logger.getLogger(logger);
			logpdfengine.setLevel(org.apache.log4j.Level.OFF);
		}
	}

	public PdfUtil(File f) {
		try {
			pdfDoc = PDDocument.load(f);
			numPages = pdfDoc.getNumberOfPages();
			pdfRenderer = new PDFRenderer(pdfDoc);
		} catch (IOException e) {
			LOG.severe("Fail: " + e);
		}
	}

	public int numPages() {
		return numPages;
	}

	public Dimension pageSize(int n) {
		PDPage page = pdfDoc.getPage(n-1);
		return new Dimension((int) page.getBBox().getWidth(), (int) page.getBBox().getHeight());
	}

	public long getPageBBoxWidth() {
		if (pdfDoc == null) {
			return -1;
		}
		if (pdfDoc.getNumberOfPages() <= 0) {
			return -1;
		}
		return (long) pdfDoc.getPage(0).getBBox().getWidth();
	}

	public Image writePage(int n, File outPath, int minimumWidth) {
		BufferedImage bImg = null;

		try {
			PDPage page = pdfDoc.getPage(n-1);

			// Improve the image quality slightly compared to assuming 72dpi.
			double adjust = screenDpi / 72.0;

			Rectangle rect = new Rectangle(0, 0, (int) page.getBBox().getWidth(), (int) page.getBBox().getHeight());

			// If minimumWidth is given, adjust to get at least that.
			if (minimumWidth != -1 && rect.width * adjust < minimumWidth) {
				adjust = minimumWidth / (float) rect.width;
			}

			bImg = pdfRenderer.renderImage(n-1, (float) adjust);

			ImageIO.write(bImg, "png", outPath);
		} catch (IOException e) {
			LOG.severe("Fail: " + e);
		}

		return bImg;
	}

	public void close() {
		try {
			pdfDoc.close();
		} catch (IOException e) {
			LOG.severe("Fail: " + e);
		}
	}
}
