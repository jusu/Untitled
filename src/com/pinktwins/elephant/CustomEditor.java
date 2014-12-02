package com.pinktwins.elephant;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

public class CustomEditor extends JPanel {

	private JTextField title;
	private JTextPane note;

	final Color kDividerColor = Color.decode("#dbdbdb");

	public CustomEditor() {
		super();
		setBackground(Color.WHITE);
		setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
		setLayout(new BorderLayout());

		JPanel titlePanel = new JPanel();
		titlePanel.setLayout(new BorderLayout());
		titlePanel.setBackground(kDividerColor);
		titlePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 1, 0));

		title = new JTextField();
		title.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));

		titlePanel.add(title, BorderLayout.CENTER);

		note = new JTextPane();
		note.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

		title.setText("");

		add(titlePanel, BorderLayout.NORTH);
		add(note, BorderLayout.CENTER);
	}

	public void setTitle(String s) {
		title.setText(s);
		title.setCaretPosition(0);
		title.setSelectionEnd(0);
	}

	public void setText(String s) {
		note.setText(s);
	}

	public String getTitle() {
		return title.getText();
	}
	
	public String getText() throws BadLocationException {
		Document doc = note.getDocument();
		return doc.getText(0, doc.getLength());
	}
	
	public void clear() {
		setTitle("");
		setText("");
	}

	public boolean hasFocus() {
		return note.hasFocus() || title.hasFocus();
	}
	
	public void initialFocus() {
		note.setCaretPosition(0);
		note.requestFocusInWindow();
	}
}
