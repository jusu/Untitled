package com.pinktwins.elephant;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

public class TagEditorPane {

	private JTextPane editor;
	private JLabel tagHint;
	private JScrollPane scroll;
	private JPanel p;

	private static int prefH1 = 14, prefH2 = 20;
	private static final String clickToAddTags = "click to add tags";

	public TagEditorPane() {
		editor = new JTextPane();
		editor.setOpaque(false);
		editor.setForeground(ElephantWindow.colorTitleButton);
		editor.setFont(ElephantWindow.fontMediumPlus);
		editor.setText(clickToAddTags);
		editor.setCaretPosition(0);

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
		p.add(scroll);
		p.add(tagHint);

		tagHint.addMouseListener(new CustomMouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {
				showEditor();
				editor.requestFocusInWindow();
			}
		});

		hideEditor();
	}

	private void hideEditor() {
		p.removeAll();
		p.add(tagHint);
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
