package com.pinktwins.elephant;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.JTextPane;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLDocument;

public class HtmlPane extends JTextPane {

	public HtmlPane(File noteFile, Runnable onTerminalClick) {
		super();

		setFocusable(false);
		setEditable(false);
		setContentType("text/html");

		final String base = noteFile.getAbsolutePath() + ".attachments" + File.separator;

		Document doc = getDocument();
		HTMLDocument d;
		if (doc instanceof HTMLDocument) {
			d = (HTMLDocument) doc;
			try {
				d.setBase(new URL("file://" + base));
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}

		addMouseListener(new HtmlPaneMouseListener(this, base, onTerminalClick));
	}
}
