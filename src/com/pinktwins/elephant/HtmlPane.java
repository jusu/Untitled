package com.pinktwins.elephant;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

import javax.swing.JEditorPane;
import javax.swing.JTextPane;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import com.pinktwins.elephant.data.Settings;

public class HtmlPane extends JTextPane {

	private static final Logger LOG = Logger.getLogger(HtmlPane.class.getName());

	private String base = null;

	public HtmlPane(File noteFile, Runnable onTerminalClick) {
		super();

		setFocusable(false);
		setEditable(false);
		setContentType("text/html");

		base = noteFile.getAbsolutePath() + ".attachments" + File.separator;
		setDocumentBase();

		HTMLEditorKit kit = new HTMLEditorKit();
		this.setEditorKit(kit);
		HtmlPaneStylesheet.getInstance().addStylesheet(kit);

		addMouseListener(new HtmlPaneMouseListener(this, base, onTerminalClick));
	}

	private void setDocumentBase() {
		try {
			URL baseUrl = new File(base).toURI().toURL();

			Document doc = getDocument();
			HTMLDocument d;
			if (doc instanceof HTMLDocument) {
				d = (HTMLDocument) doc;
				d.setBase(baseUrl);

				String styles = Elephant.settings.getString(Settings.Keys.MARKDOWN_STYLES);
				if (!styles.isEmpty()) {
					d.getStyleSheet().addRule(styles);
				}

				// hint by http://stackoverflow.com/a/19785465/873282
				this.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
			}
		} catch (MalformedURLException e) {
			LOG.severe("Fail: " + e);
		}
	}

	@Override
	public void setText(String html) {
		super.setText(html);
		setDocumentBase();
	}
}
