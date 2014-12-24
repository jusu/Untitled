package com.pinktwins.elephant;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridBagLayout;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;

public class TagEditorPane {

	private JTextPane editor;
	private JLabel tagHint;
	private JScrollPane scroll;
	private JPanel p;

	private static int prefH1 = 14, prefH2 = 20;
	private static final String clickToAddTags = "click to add tags";

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
						e.turnTextToTag();
					}
				});
			}
			super.insertString(offs, str, a);
		}
	}

	public TagEditorPane() {
		editor = new JTextPane();
		editor.setDocument(new TagDocument(this));
		editor.setOpaque(false);
		editor.setForeground(ElephantWindow.colorTitleButton);
		editor.setFont(ElephantWindow.fontMediumPlus);
		editor.setText(clickToAddTags);
		editor.setCaretPosition(0);
		TextComponentUtils.insertListenerForHintText(editor, clickToAddTags);

		tagHint = new JLabel(clickToAddTags, JLabel.LEADING);
		tagHint.setForeground(ElephantWindow.colorTitleButton);
		tagHint.setBackground(Color.RED);
		tagHint.setFont(ElephantWindow.fontMediumPlus);

		scroll = new JScrollPane(editor);
		scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
		scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setPreferredSize(new Dimension(300, prefH1));
		scroll.setBorder(ElephantWindow.emptyBorder);

		p = new JPanel(new GridBagLayout());
		p.setPreferredSize(new Dimension(400, prefH2));
		p.setBorder(ElephantWindow.emptyBorder);

		tagHint.addMouseListener(new CustomMouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {
				showEditor();
				editor.requestFocusInWindow();
			}
		});

		hideEditor();
	}

	public void turnTextToTag() {
		String tagText = editor.getText();
		System.out.println("tag from this: '" + tagText + "'");

		// XXX on save, collect all tags written, ask Tags to 'resolve' them,
		// returning tag ids to save as note metadata. loading happens in
		// reverse -
		// metadata loads ids and asks Tags for their names.
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

	public void updateWidth(int w) {
		scroll.setPreferredSize(new Dimension(w - 128, prefH1));
		tagHint.setPreferredSize(new Dimension(w - 128, prefH2));

		p.setPreferredSize(new Dimension(w - 100, prefH2));
		p.revalidate();
	}
}
