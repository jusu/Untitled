package com.pinktwins.elephant;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.KeyboardFocusManager;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import com.apple.mrj.macos.carbon.Timer;

public class CustomEditor extends RoundPanel {

	private static final long serialVersionUID = -6604641427725747091L;

	private JTextField title;
	private JTextPane note;

	final Color kDividerColor = Color.decode("#dbdbdb");

	public interface EditorEventListener {
		public void editingFocusGained();
		public void editingFocusLost();

		public void caretChanged(JTextPane text);
	}

	EditorEventListener eeListener;

	public void setEditorEventListener(EditorEventListener l) {
		eeListener = l;
	}

	FocusListener editorFocusListener = new FocusListener() {
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
	};

	public CustomEditor() {
		super();
		setBackground(Color.WHITE);
		setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
		setLayout(new BorderLayout());

		JPanel titlePanel = new JPanel();
		titlePanel.setLayout(new BorderLayout());
		titlePanel.setBackground(kDividerColor);
		titlePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 1, 0));

		title = new JTextField();
		title.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
		title.addFocusListener(editorFocusListener);

		final KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();

		title.addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(KeyEvent e) {
			}

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					manager.focusNextComponent();
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
			}
		});

		titlePanel.add(title, BorderLayout.CENTER);

		note = new JTextPane();
		note.addFocusListener(editorFocusListener);
		note.setFont(ElephantWindow.fontEditor);
		note.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

		note.addCaretListener(new CaretListener() {
			@Override
			public void caretUpdate(CaretEvent e) {
				if (eeListener != null) {
					eeListener.caretChanged(note);
				}
			}
		});

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

	public void focusTitle() {
		title.setCaretPosition(0);
		title.requestFocusInWindow();
	}
}
