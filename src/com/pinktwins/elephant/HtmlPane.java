package com.pinktwins.elephant;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

import javax.swing.JTextPane;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLDocument;

public class HtmlPane extends JTextPane {

	private static final Logger LOG = Logger.getLogger(HtmlPane.class.getName());

	public HtmlPane(File noteFile, Runnable onTerminalClick) {
		super();

		setFocusable(false);
		setEditable(false);
		setContentType("text/html");

		String base = noteFile.getAbsolutePath() + ".attachments" + File.separator;

		try {
			URL baseUrl = new File(base).toURI().toURL();

			Document doc = getDocument();
			HTMLDocument d;
			if (doc instanceof HTMLDocument) {
				d = (HTMLDocument) doc;
				d.setBase(baseUrl);
			}
		} catch (MalformedURLException e) {
			LOG.severe("Fail: " + e);
		}

		addMouseListener(new HtmlPaneMouseListener(this, base, onTerminalClick));
	}
}
