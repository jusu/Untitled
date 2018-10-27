package com.pinktwins.elephant;

import java.awt.AlphaComposite;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;
import javax.swing.text.StyleConstants;

import org.apache.commons.lang3.SystemUtils;

import com.pinktwins.elephant.CustomEditor.AttachmentInfo;
import com.pinktwins.elephant.util.CustomMouseListener;
import com.pinktwins.elephant.util.Images;

public class AttachmentDragMouseListener extends CustomMouseListener {

	private static final Logger LOG = Logger.getLogger(AttachmentDragMouseListener.class.getName());
	private static final Image dragHand, dragFile;

	static {
		Iterator<Image> i = Images.iterator(new String[] { "dragHand", "dragFile" });
		dragHand = i.next();
		dragFile = i.next();
	}

	private final CustomEditor editor;
	private final JTextPane note;

	private Object attachmentDragObject = null;
	private Point mouseDownPoint;
	private final Highlighter defaultHighlighter;
	private final Cursor defaultCursor;

	protected Object attachmentObject = null;

	public AttachmentDragMouseListener(CustomEditor editor, JTextPane note) {
		this.editor = editor;
		this.note = note;

		defaultHighlighter = note.getHighlighter();
		defaultCursor = note.getCursor();
	}

	private void checkIconAtPosition(int n) {
		if (n >= 0 && n < note.getDocument().getLength()) {
			AttributeSet as = editor.getAttributes(n);

			boolean hasIcon = as.containsAttribute(CustomEditor.ELEM, CustomEditor.ICON);
			boolean hasComp = as.containsAttribute(CustomEditor.ELEM, CustomEditor.COMP);
			if (hasIcon || hasComp) {
				// Build cursor approriate for dragging this attachment
				attachmentDragObject = hasIcon ? StyleConstants.getIcon(as) : StyleConstants.getComponent(as);
				note.setHighlighter(null);

				if (SystemUtils.IS_OS_WINDOWS) {
					// Cursor is more limited on Windows, cannot use the image.
					Toolkit toolkit = Toolkit.getDefaultToolkit();
					Cursor c = toolkit.createCustomCursor(dragHand, new Point(0, 8), "img");
					note.setCursor(c);
				} else {
					// On Mac, use a scaled version of image next to dragHand.
					Image ref = null;

					if (attachmentDragObject instanceof ImageIcon) {
						ImageIcon img = (ImageIcon) attachmentDragObject;
						ref = img.getImage();
					} else {
						ref = dragFile;
					}

					float f = 96.0f / ref.getHeight(null);
					Image scaled = ref.getScaledInstance((int) (f * ref.getWidth(null)), (int) (f * ref.getHeight(null)), Image.SCALE_FAST);

					int dhWidth = dragHand.getWidth(null), dhHeight = dragHand.getHeight(null);

					BufferedImage composite = new BufferedImage(scaled.getWidth(null) + dhWidth, Math.max(scaled.getHeight(null), dhHeight),
							BufferedImage.TYPE_INT_ARGB);

					int yOffset = (composite.getHeight() - dhHeight) / 2;

					Graphics2D g = composite.createGraphics();
					g.drawImage(dragHand, 0, yOffset, null);
					g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC, 0.4f));
					g.drawImage(scaled, dhWidth, 0, null);
					g.dispose();

					Toolkit toolkit = Toolkit.getDefaultToolkit();
					Cursor c = toolkit.createCustomCursor(composite, new Point(0, yOffset + 8), "img");
					note.setCursor(c);
				}
			}
		}
	}

	@Override
	public void mousePressed(MouseEvent event) {
		int i = note.viewToModel(event.getPoint());
		checkIconAtPosition(i);
		checkIconAtPosition(i - 1);

		mouseDownPoint = event.getPoint();
	}

	@Override
	public void mouseReleased(MouseEvent event) {
		if (attachmentDragObject != null) {
			// Started dragging from image. Move image to current caret position.
			List<AttachmentInfo> info = editor.getAttachmentInfo();
			for (AttachmentInfo i : info) {
				if (i.object == attachmentDragObject) {
					AttributeSet as = editor.getAttributes(i.startPosition);
					int len = i.endPosition - i.startPosition;
					Document doc = note.getDocument();

					if (!event.getPoint().equals(mouseDownPoint)) {
						try {
							String s = doc.getText(i.startPosition, len);
							doc.remove(i.startPosition, len);
							doc.insertString(note.getCaretPosition(), s, as);
							attachmentMoved(i);
						} catch (BadLocationException e) {
							LOG.severe("Fail: " + e);
						}
					}

					if (i.object instanceof ImageIcon) {
						attachmentObject = i.object;
					} else {
						attachmentObject = null;
					}

					break;
				}
			}

			attachmentDragObject = null;
			note.setHighlighter(defaultHighlighter);
			note.setCursor(defaultCursor);
		}
	}

	protected void attachmentMoved(AttachmentInfo info) {
	}
}
