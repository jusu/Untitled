package com.pinktwins.elephant;

import java.awt.Desktop;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.logging.Logger;

import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;

import com.pinktwins.elephant.util.CustomMouseListener;
import com.pinktwins.elephant.util.LaunchUtil;

public class HtmlPaneMouseListener extends CustomMouseListener {

	private static final Logger LOG = Logger.getLogger(HtmlPaneMouseListener.class.getName());

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
							File f = new File(URLDecoder.decode(noteAttachmentBasedir + href, "UTF-8"));
							if (!f.exists()) {
								// Look under parent. Some ENEX -> markdown importer might put the resource here.
								f = new File(URLDecoder.decode(new File(noteAttachmentBasedir).getParent() + File.separator + href, "UTF-8"));
							}
							if (f.exists()) {
								LaunchUtil.launch(f);
							} else {
								LOG.severe("Link \"" + href + "\" not found.");
							}
						} catch (IOException e2) {
							LOG.severe("Link \"" + href + "\" is unsupported for now.");
						}
					} catch (URISyntaxException e1) {
						LOG.severe("Malformed link: " + href);
					}
				}
			}
			return;
		}

		if (onTerminalClick != null) {
			onTerminalClick.run();
		}
	}
}
