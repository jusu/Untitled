package com.pinktwins.elephant;

import java.awt.Desktop;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;

import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;

import com.pinktwins.elephant.util.CustomMouseListener;

public class HtmlPaneMouseListener extends CustomMouseListener {

	JTextPane htmlPane;
	String noteAttachmentBasedir;
	Runnable onTerminalClick;

	public HtmlPaneMouseListener(JTextPane htmlPane, String noteAttachmentBasedir, Runnable onTerminalClick) {
		super();

		this.htmlPane = htmlPane;
		this.noteAttachmentBasedir = noteAttachmentBasedir;
		this.onTerminalClick = onTerminalClick;
	}

	private Element getHyperlinkElement(MouseEvent event) {
		int pos = htmlPane.getUI().viewToModel(htmlPane, event.getPoint());
		if (pos >= 0 && htmlPane.getDocument() instanceof HTMLDocument) {
			HTMLDocument hdoc = (HTMLDocument) htmlPane.getDocument();
			Element elem = hdoc.getCharacterElement(pos);
			if (elem.getAttributes().getAttribute(HTML.Tag.A) != null) {
				return elem;
			}
		}
		return null;
	}

	// XXX replace .decode
	@SuppressWarnings("deprecation")
	@Override
	public void mouseClicked(MouseEvent e) {
		Element h = getHyperlinkElement(e);
		if (h != null) {
			Object attribute = h.getAttributes().getAttribute(HTML.Tag.A);
			if (attribute instanceof AttributeSet) {
				AttributeSet set = (AttributeSet) attribute;
				String href = (String) set.getAttribute(HTML.Attribute.HREF);
				if (href != null) {
					try {
						Desktop.getDesktop().browse(new URI(href));
					} catch (IOException e1) {
						try {
							Desktop.getDesktop().edit(new File(URLDecoder.decode(noteAttachmentBasedir + href)));
						} catch (IOException e2) {
							System.out.println("Link \"" + href + "\" is unsupported for now.");
						}
					} catch (URISyntaxException e1) {
						System.out.println("Malformed link: " + href);
					}
				}
			}
			return;
		}

		onTerminalClick.run();
	}
}
