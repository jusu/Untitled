package com.pinktwins.elephant;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextPane;

import com.pinktwins.elephant.CustomEditor.AttachmentInfo;
import com.pinktwins.elephant.data.Note;
import com.pinktwins.elephant.data.Note.Meta;
import com.pinktwins.elephant.data.Notebook;
import com.pinktwins.elephant.data.Vault;
import com.pinktwins.elephant.eventbus.NoteChangedEvent;
import com.pinktwins.elephant.eventbus.TagsChangedEvent;
import com.pinktwins.elephant.util.Factory;
import com.pinktwins.elephant.util.Images;
import com.pinktwins.elephant.util.ResizeListener;

public class MultipleNotes extends BackgroundPanel implements EditorEventListener {

	// private static final Logger LOG = Logger.getLogger(MultipleNotes.class.getName());

	private ElephantWindow window;

	private static Image tile, multiSelection, multiSelectionTagFocus, moveToNotebook;

	private final Font headerFont = Font.decode("Helvetica-BOLD-16");
	private final Color headerColor = Color.decode("#7a7a7a");
	private final Color lineColor = Color.decode("#b4b4b4");

	BackgroundPanel frame;
	JLabel header;
	TagEditorPane tagPane;

	private Set<Note> currentNotes;

	private List<String> emptyList = Collections.emptyList();

	static {
		Iterator<Image> i = Images.iterator(new String[] { "notebooks", "multiSelection", "multiSelectionTagFocus", "moveToNotebook" });
		tile = i.next();
		multiSelection = i.next();
		multiSelectionTagFocus = i.next();
		moveToNotebook = i.next();
	}

	public MultipleNotes(ElephantWindow w) {
		super(tile);
		window = w;
		createComponents();
	}

	public void cleanup() {
		window = null;
	}

	private void createComponents() {
		header = new JLabel("wowowo");
		header.setFont(headerFont);
		header.setForeground(headerColor);

		JButton bMove = new JButton();
		bMove.setIcon(new ImageIcon(moveToNotebook));
		bMove.setBorderPainted(false);
		bMove.setContentAreaFilled(false);

		tagPane = new TagEditorPane();
		tagPane.makeV2();
		tagPane.setEditorEventListener(this);

		frame = new BackgroundPanel(multiSelection);
		frame.setLayout(null);
		frame.keepScaleOnRetina(true, true);
		frame.add(bMove);
		frame.add(tagPane.getComponent());
		bMove.setBounds(20, 31, 290, 24);
		tagPane.getComponent().setBounds(21, 70, 288, 28);

		setLayout(null);
		add(header);
		add(frame);

		addComponentListener(new ResizeListener() {
			@Override
			public void componentResized(ComponentEvent e) {
				updateLayout();
			}
		});

		bMove.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				openNotebookChooserForMoving();
			}
		});
	}

	private void updateLayout() {
		int x = (getWidth() - multiSelection.getWidth(null)) / 2;
		int y = getHeight() / 2;
		frame.setBounds(x, y, multiSelection.getWidth(null), multiSelection.getHeight(null));

		int w = header.getPreferredSize().width;
		x = (getWidth() - w) / 2;
		y = getHeight() / 2 - 30;
		header.setBounds(x, y, w, 20);

		tagPane.updateWidth(280 + 128);
	}

	public void load(Set<Note> selection) {
		currentNotes = selection;
		header.setText(String.valueOf(selection.size()) + " notes selected");
		tagPane.load(emptyList);
		updateLayout();
	}

	public boolean hasFocus() {
		return tagPane.hasFocus();
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);
		g.setColor(lineColor);
		g.drawLine(0, 0, 0, getHeight());
	}

	public void openNotebookChooserForMoving() {
		NotebookChooser nbc = new NotebookChooser(window, String.format("Move %s notes", currentNotes.size()));

		// Center on window
		Point p = this.getLocationOnScreen();
		Rectangle r = window.getBounds();
		int x = r.x + r.width / 2 - NotebookChooser.fixedWidth / 2;
		nbc.setBounds(x, p.y, NotebookChooser.fixedWidth, NotebookChooser.fixedHeight);

		nbc.setVisible(true);

		nbc.setNotebookActionListener(new NotebookActionListener() {
			@Override
			public void didCancelSelection() {
			}

			@Override
			public void didSelect(Notebook nb) {
				moveNoteAction(nb);
			}
		});
	}

	protected void moveNoteAction(Notebook destination) {
		if (currentNotes == null || destination == null) {
			throw new AssertionError();
		}

		for (Note n : currentNotes) {
			File source = n.file().getParentFile();
			if (destination.folder().equals(source)) {
				continue;
			}

			System.out.println("move " + n.getMeta().title() + " -> " + destination.name() + " (" + destination.folder() + ")");

			n.moveTo(destination.folder());
		}

		int index = window.getIndexOfFirstSelectedNoteInNoteList();

		window.sortAndUpdate();

		if (window.isShowingSearchResults()) {
			window.redoSearch();
		}

		if (window.isShowingAllNotes()) {
			window.showAllNotes();
		}
		
		currentNotes.clear();
		window.clearNoteEditor();

		if (index >= 0) {
			window.selectNoteByIndex(index);
		}
	}

	@Override
	public void editingFocusGained() {
		frame.setImage(multiSelectionTagFocus);
	}

	@Override
	public void editingFocusLost() {

		frame.setImage(multiSelection);

		List<String> newTagNames = tagPane.getTagNames();

		// Apply 'tags' to notes
		if (!newTagNames.isEmpty()) {
			List<String> ids = Vault.getInstance().resolveTagNames(newTagNames);
			if (!ids.isEmpty()) {
				for (Note n : currentNotes) {
					Meta m = n.getMeta();
					List<String> oldIds = m.tags();

					List<String> oldNames = Vault.getInstance().resolveTagIds(oldIds);

					List<String> allNames = Factory.newArrayList();
					List<String> allIds = Factory.newArrayList();

					allNames.addAll(oldNames);
					allNames.addAll(newTagNames);

					allIds.addAll(oldIds);
					allIds.addAll(ids);

					m.setTags(allIds, allNames);

					new NoteChangedEvent(n, true).post();
				}
			}
			new TagsChangedEvent().post();
		}

		currentNotes.clear();
	}

	@Override
	public void caretChanged(JTextPane text) {
	}

	@Override
	public void filesDropped(List<File> files) {
	}

	@Override
	public void attachmentClicked(MouseEvent event, Object attachmentObject) {
	}

	@Override
	public void attachmentMoved(AttachmentInfo info) {
	}
}
