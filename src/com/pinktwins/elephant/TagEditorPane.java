package com.pinktwins.elephant;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Element;
import javax.swing.text.ElementIterator;
import javax.swing.text.StyleConstants;

import com.pinktwins.elephant.EditorEventListener;
import com.pinktwins.elephant.data.Factory;

public class TagEditorPane {

	private JTextPane editor;
	private JLabel tagHint;
	private JScrollPane scroll;
	private JPanel p;

	private static int prefH1 = 14, prefH2 = 20;
	private static final String clickToAddTags = "click to add tags";

	private static Image tagLeft, tagRight, tagMiddle;

	private List<String> loadedTags;

	static {
		try {
			tagLeft = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/tagLeft.png"));
			tagMiddle = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/tagMiddle.png"));
			tagRight = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/tagRight.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	static class TagDocument extends DefaultStyledDocument {
		private static final long serialVersionUID = 2807153134148093523L;

		TagEditorPane e;

		public TagDocument(TagEditorPane e) {
			super();
			this.e = e;
		}

		@Override
		public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
			boolean lf = str.indexOf("\n") >= 0;
			if (lf) {
				str = str.replaceAll("\n", "");
				EventQueue.invokeLater(new Runnable() {
					@Override
					public void run() {
						try {
							String s = getText(0, getLength()).trim();
							if (!s.isEmpty()) {
								e.turnTextToTag(s);
							}
						} catch (BadLocationException e) {
							e.printStackTrace();
						}
					}
				});
			}
			super.insertString(offs, str, a);
		}
	}

	EditorEventListener eeListener;

	public void setEditorEventListener(EditorEventListener l) {
		eeListener = l;
	}

	public TagEditorPane() {
		editor = new JTextPane();
		editor.setDocument(new TagDocument(this));
		editor.setOpaque(true);
		editor.setForeground(ElephantWindow.colorTitleButton);
		editor.setFont(ElephantWindow.fontMediumPlus);
		editor.setText(clickToAddTags);
		editor.setCaretPosition(0);
		editor.setBackground(ElephantWindow.colorDB);
		TextComponentUtils.insertListenerForHintText(editor, clickToAddTags);
		editor.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {
				if (eeListener != null) {
					eeListener.editingFocusGained();
				}
			}

			@Override
			public void focusLost(FocusEvent e) {
				if (eeListener != null) {
					eeListener.editingFocusLost();
				}
			}
		});

		tagHint = new JLabel(clickToAddTags, JLabel.LEADING);
		tagHint.setForeground(ElephantWindow.colorTitleButton);
		tagHint.setFont(ElephantWindow.fontMediumPlus);

		scroll = new JScrollPane(editor);
		scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
		scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setPreferredSize(new Dimension(300, prefH1));
		scroll.setBorder(ElephantWindow.emptyBorder);
		scroll.setOpaque(false);

		p = new JPanel(new GridBagLayout());
		p.setPreferredSize(new Dimension(400, prefH2));
		p.setBorder(ElephantWindow.emptyBorder);
		p.setOpaque(false);

		tagHint.addMouseListener(new CustomMouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {
				showEditor();
				editor.requestFocusInWindow();
			}
		});

		hideEditor();
	}

	public boolean isDirty() {
		if (loadedTags == null) {
			return false;
		}

		List<String> current = getTagNames();
		if (loadedTags.size() != current.size()) {
			return true;
		}

		for (String s : current) {
			if (!loadedTags.contains(s)) {
				return true;
			}
		}

		return false;
	}

	public void load(List<String> tagNames) {
		editor.setText("");
		loadedTags = tagNames;
		if (tagNames.isEmpty()) {
			hideEditor();
		} else {
			showEditor();
			for (String s : tagNames) {
				turnTextToTag(s);
			}
		}
	}

	public void turnTextToTag(String tagText) {
		String tx = editor.getText();
		int n = tx.indexOf(tagText);
		if (n >= 0) {
			try {
				editor.getDocument().remove(n, tagText.length());
			} catch (BadLocationException e) {
				e.printStackTrace();
			}
		}

		TagLabel t = new TagLabel(tagText);
		editor.insertIcon(t);
	}

	public List<String> getTagNames() {
		ArrayList<String> list = Factory.newArrayList();

		ElementIterator iterator = new ElementIterator(editor.getDocument());
		Element element;
		while ((element = iterator.next()) != null) {
			AttributeSet as = element.getAttributes();
			if (as.containsAttribute(CustomEditor.ELEM, CustomEditor.ICON)) {
				Object o = StyleConstants.getIcon(as);
				if (o instanceof TagLabel) {
					String name = ((TagLabel) o).name;
					list.add(name);
				}
			}
		}

		return list;
	}

	private void hideEditor() {
		p.removeAll();
		p.add(tagHint);
		p.revalidate();
	}

	private void showEditor() {
		p.removeAll();
		p.add(scroll);
		p.revalidate();
	}

	public Component getComponent() {
		return p;
	}

	public boolean hasFocus() {
		return editor != null && editor.hasFocus();
	}

	public void requestFocus() {
		showEditor();
		editor.requestFocusInWindow();
	}

	public void updateWidth(int w) {
		scroll.setPreferredSize(new Dimension(w - 128, prefH1));
		tagHint.setPreferredSize(new Dimension(w - 128, prefH2));

		p.setPreferredSize(new Dimension(w - 100, prefH2));
		p.revalidate();
	}

	class TagLabel extends ImageIcon {
		public String name;

		@Override
		public boolean equals(Object o) {
			if (o == null) {
				return false;
			}

			if (!(o instanceof TagLabel)) {
				return false;
			}

			return name.equals(((TagLabel) o).name);
		}

		@Override
		public int hashCode() {
			return name.hashCode();
		}

		public TagLabel(String name) {
			super();

			this.name = name;

			Font font = ElephantWindow.fontMedium;

			BufferedImage im = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = im.createGraphics();
			g.setFont(font);
			FontMetrics fm = g.getFontMetrics(font);
			int width = fm.stringWidth(name);
			g.dispose();

			BufferedImage image = new BufferedImage(width + 22, 11, BufferedImage.TYPE_INT_ARGB);
			g = image.createGraphics();
			g.setFont(font);
			g.setColor(ElephantWindow.colorTitleButton);
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

			g.drawImage(tagLeft, 3, 0, null);
			for (int n = 13; n < width + 9; n += 10) {
				g.drawImage(tagMiddle, n, 0, null);
			}
			g.drawImage(tagRight, width + 9, 0, null);

			g.drawString(name, 11, 9);
			g.dispose();

			setImage(image);
		}
	}
}
