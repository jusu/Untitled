package com.pinktwins.elephant;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;

import com.google.common.eventbus.Subscribe;
import com.pinktwins.elephant.CustomEditor.EditorEventListener;
import com.pinktwins.elephant.data.Note;
import com.pinktwins.elephant.data.NoteChangedEvent;
import com.pinktwins.elephant.data.Notebook;
import com.pinktwins.elephant.data.Vault;

public class NoteEditor extends BackgroundPanel implements EditorEventListener {

	private static final long serialVersionUID = 5649274177360148568L;

	private static Image tile, noteTopShadow, noteToolsNotebook, noteToolsTrash, noteToolsDivider;

	private ElephantWindow window;

	private boolean isDirty;

	private final int kNoteOffset = 64;
	private final int kMinNoteSize = 288;
	private final int kBorder = 14;

	static {
		try {
			tile = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/noteeditor.png"));
			noteTopShadow = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/noteTopShadow.png"));
			noteToolsNotebook = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/noteToolsNotebook.png"));
			noteToolsTrash = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/noteToolsTrash.png"));
			noteToolsDivider = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/noteToolsDivider.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public NoteEditor(ElephantWindow w) {
		super(tile);
		window = w;

		Elephant.eventBus.register(this);

		createComponents();
	}

	private Note currentNote;
	JPanel main, area, areaHolder;
	BackgroundPanel scrollHolder;
	JScrollPane scroll;
	CustomEditor editor;
	BackgroundPanel topShadow;
	JButton currNotebook;
	JLabel noteCreated, noteUpdated;
	BorderLayout areaHolderLayout;

	private class DividedPanel extends BackgroundPanel {
		private static final long serialVersionUID = -7285142017724975923L;

		public DividedPanel(Image i) {
			super(i);
		}

		@Override
		public void paint(Graphics g) {
			super.paint(g);
			g.drawImage(noteToolsDivider, 16, 32, getWidth() - 32, 2, null);
		}
	}

	private class TopShadowPanel extends JPanel {
		private static final long serialVersionUID = 6626079564069649611L;

		@Override
		public void paint(Graphics g) {
			super.paint(g);

			if (scroll.getVerticalScrollBar().getValue() < 4) {
				g.drawImage(noteTopShadow, 0, kNoteOffset, getWidth(), 4, null);
			} else {
				g.drawImage(noteTopShadow, 0, kNoteOffset, getWidth(), 2, null);
			}
		}
	}

	private void createComponents() {
		main = new TopShadowPanel();
		main.setLayout(null);
		main.setBorder(BorderFactory.createEmptyBorder(kBorder, kBorder, kBorder, kBorder));

		final DividedPanel tools = new DividedPanel(tile);
		tools.setBounds(0, 0, 1920, 65);

		JPanel toolsTop = new JPanel(new BorderLayout());
		toolsTop.setOpaque(false);

		JPanel toolsTopLeft = new JPanel(new BorderLayout());
		toolsTopLeft.setOpaque(false);

		JPanel toolsTopRight = new JPanel(new BorderLayout());
		toolsTopRight.setOpaque(false);

		currNotebook = new JButton("");
		currNotebook.setBorderPainted(false);
		currNotebook.setIcon(new ImageIcon(noteToolsNotebook));
		currNotebook.setForeground(ElephantWindow.colorTitleButton);

		JButton trash = new JButton("");
		trash.setBorderPainted(false);
		trash.setIcon(new ImageIcon(noteToolsTrash));

		toolsTopLeft.add(currNotebook, BorderLayout.WEST);
		toolsTopRight.add(trash, BorderLayout.EAST);
		toolsTop.add(toolsTopLeft, BorderLayout.WEST);
		toolsTop.add(toolsTopRight, BorderLayout.EAST);

		JPanel toolsBot = new JPanel(new FlowLayout(FlowLayout.LEFT));

		noteCreated = new JLabel("Created: xxxxxx");
		noteCreated.setBorder(BorderFactory.createEmptyBorder(0, 4, 4, 10));
		noteCreated.setForeground(ElephantWindow.colorTitleButton);
		noteCreated.setFont(ElephantWindow.fontMedium);

		noteUpdated = new JLabel("Updated: xxxxxx");
		noteUpdated.setBorder(BorderFactory.createEmptyBorder(0, 4, 4, 20));
		noteUpdated.setForeground(ElephantWindow.colorTitleButton);
		noteUpdated.setFont(ElephantWindow.fontMedium);

		toolsBot.add(noteCreated);
		toolsBot.add(noteUpdated);

		tools.add(toolsTop, BorderLayout.NORTH);
		tools.add(toolsBot, BorderLayout.SOUTH);

		main.add(tools);

		area = new JPanel();
		area.setLayout(new GridLayout(1, 1));
		area.setBackground(Color.WHITE);

		editor = new CustomEditor();
		editor.setEditorEventListener(this);
		area.add(editor);
		area.setBounds(kBorder, kBorder, 200, kMinNoteSize);

		// Swing when you're winning part #1.

		final int topBorderOffset = 2;
		areaHolderLayout = new BorderLayout();
		areaHolder = new JPanel(areaHolderLayout);
		areaHolder.setBorder(BorderFactory.createEmptyBorder(kBorder - topBorderOffset, kBorder, kBorder, kBorder));
		areaHolder.setBounds(0, 0, 200, kMinNoteSize);
		areaHolder.add(area, BorderLayout.NORTH);

		scrollHolder = new BackgroundPanel();
		scrollHolder.setOpaque(false);
		scrollHolder.setBounds(0, 0, 200, kMinNoteSize);

		scroll = new JScrollPane(areaHolder);
		scroll.setOpaque(false);
		scroll.setBorder(ElephantWindow.emptyBorder);
		scroll.getVerticalScrollBar().setUnitIncrement(10);
		scroll.getHorizontalScrollBar().setUnitIncrement(10);

		scrollHolder.add(scroll, BorderLayout.CENTER);

		main.add(scrollHolder);

		add(main, BorderLayout.CENTER);

		caretChanged(editor.getTextPane());

		trash.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				window.deleteSelectedNote();
			}
		});

		addComponentListener(new ResizeListener() {
			@Override
			public void componentResized(ComponentEvent e) {
				Rectangle mb = main.getBounds();
				Rectangle ab = area.getBounds();

				ab.width = mb.width - kBorder * 2;
				area.setBounds(ab);

				scrollHolder.setBounds(0, kNoteOffset + topBorderOffset, getWidth(), getHeight() - kNoteOffset - topBorderOffset);
				areaHolder.setBounds(0, 0, ab.width, ab.height);

				EventQueue.invokeLater(new Runnable() {
					public void run() {
						NoteEditor.this.editor.revalidate();

						Rectangle r = tools.getBounds();
						r.width = getWidth();
						tools.setBounds(r);
					}
				});
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
		currentNote = null;
		editor.clear();
		isDirty = false;
		visible(false);
	}

	public void load(Note note) {
		currentNote = note;
		editor.setTitle(note.getMeta().title());
		editor.setText(note.contents());
		visible(true);

		Notebook nb = Vault.getInstance().findNotebook(note.file().getParentFile());
		currNotebook.setText(nb.name());

		noteCreated.setText("Created: " + note.createdStr());
		noteUpdated.setText("Updated: " + note.updatedStr());
		note.flushAttrs();
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

	@Subscribe
	public void handleUIEvent(UIEvent event) {
		if (event.kind == UIEvent.Kind.editorWillChangeNote) {
			saveChanges();
		}
	}

	public void saveChanges() {
		if (!isDirty) {
			return;
		}

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
				Elephant.eventBus.post(new NoteChangedEvent(currentNote));
				if (contentChanged) {
					window.sortAndUpdate();
				}
			}
		}

		isDirty = false;
	}

	public void focusTitle() {
		editor.focusTitle();
	}

	@Override
	public void editingFocusGained() {
		isDirty = true;
	}

	@Override
	public void editingFocusLost() {
		saveChanges();
	}

	@Override
	public void caretChanged(final JTextPane text) {
		// Expand/shrink JTextPane to content height
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Rectangle y = text.modelToView(text.getDocument().getLength());
					if (y != null) {
						int height = y.y + y.height + kNoteOffset;
						if (height < kMinNoteSize) {
							height = kMinNoteSize;
						}

						// Swing when you're winning part #2.
						//
						// BorderLayout is fine EXCEPT when note should be at
						// least kMinNoteSize height.
						//
						// BorderLayout would make it as small as the real text
						// height.
						//
						// Prevent that by switching with null layout on the go.

						if (height < kMinNoteSize + kBorder) {
							areaHolder.setLayout(null);
						} else {
							areaHolder.setLayout(areaHolderLayout);
						}

						int pos = text.getCaretPosition();
						int len = text.getDocument().getLength();
						if (pos == len) {
							scroll.getVerticalScrollBar().setValue(Integer.MAX_VALUE);
						}

						Rectangle b = area.getBounds();
						area.setBounds(b.x, b.y, b.width, height);
						area.revalidate();

						b = areaHolder.getBounds();
						b.height = height + kNoteOffset + kBorder * 2;
						areaHolder.setBounds(b.x, b.y, getWidth(), b.height);

						// not needed?
						// areaHolder.revalidate();
						// scrollHolder.revalidate();
					}
				} catch (BadLocationException e) {
				}
			}
		});
	}
}
