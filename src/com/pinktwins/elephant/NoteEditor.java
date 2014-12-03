package com.pinktwins.elephant;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.text.BadLocationException;

import com.pinktwins.elephant.CustomEditor.EditorEventListener;
import com.pinktwins.elephant.data.Note;

public class NoteEditor extends BackgroundPanel implements EditorEventListener {

	private static final long serialVersionUID = 5649274177360148568L;
	private static Image tile;

	private ElephantWindow window;
	private final int kBorder = 14;

	static {
		try {
			tile = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/noteeditor.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public NoteEditor(ElephantWindow w) {
		super(tile);
		window = w;
		createComponents();
	}

	private Note currentNote;
	JPanel main;
	CustomEditor editor;

	private void createComponents() {
		main = new JPanel();
		main.setLayout(null);
		main.setBorder(BorderFactory.createEmptyBorder(kBorder, kBorder, kBorder, kBorder));

		JPanel tools = new JPanel();
		tools.setBackground(Color.MAGENTA);
		tools.setBounds(0, 0, 1920, 65);
		main.add(tools);

		final JPanel area = new JPanel();
		area.setLayout(new GridLayout(1, 1));
		area.setBackground(Color.WHITE);

		editor = new CustomEditor();
		editor.setEditorEventListener(this);
		area.add(editor);
		area.setBounds(kBorder, 65 + kBorder, 200, 288);

		main.add(area);

		add(main, BorderLayout.CENTER);

		addComponentListener(new ResizeListener() {
			@Override
			public void componentResized(ComponentEvent e) {
				Rectangle mb = main.getBounds();
				Rectangle ab = area.getBounds();
				ab.width = mb.width - kBorder * 2;
				area.setBounds(ab);
			}
		});

		main.addMouseListener(new CustomMouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {
				unfocus();
			}
		});
	}

	public void clear() {
		// saveChanges();
		currentNote = null;
		editor.clear();
		visible(false);
	}

	public void load(Note note) {
		currentNote = note;
		editor.setTitle(note.getMeta().title());
		editor.setText(note.contents());
		visible(true);
	}

	private void visible(boolean b) {
		main.setVisible(b);
	}

	public boolean hasFocus() {
		return editor.hasFocus();
	}

	public void unfocus() {
		window.unfocusEditor();
	}

	private void saveChanges() {
		if (currentNote != null) {
			boolean changed = false;
			boolean contentChanged = false;

			try {
				String fileTitle = currentNote.getMeta().title();
				String editedTitle = editor.getTitle();
				if (!fileTitle.equals(editedTitle)) {
					currentNote.getMeta().title(editedTitle);
					changed = true;
				}

				String fileText = currentNote.contents();
				String editedText = editor.getText();
				if (!fileText.equals(editedText)) {
					currentNote.save(editedText);
					changed = true;
					contentChanged = true;
				}
			} catch (BadLocationException e) {
				e.printStackTrace();
			}

			if (changed) {
				window.updateThumb(currentNote);
				if (contentChanged) {
					window.sortAndUpdate();
				}
			}
		}
	}

	public void focusTitle() {
		editor.focusTitle();
	}

	@Override
	public void editingFocusLost() {
		saveChanges();
	}
}
