package com.pinktwins.elephant;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;

import com.google.common.eventbus.Subscribe;
import com.pinktwins.elephant.data.Tag;
import com.pinktwins.elephant.data.Vault;
import com.pinktwins.elephant.eventbus.NoteChangedEvent;
import com.pinktwins.elephant.util.Factory;
import com.pinktwins.elephant.util.Images;

public class TagList extends ToolbarList<TagList.TagItem> {

	private static Image tile, newTag;

	private ElephantWindow window;

	static {
		Iterator<Image> i = Images.iterator(new String[] { "notebooks", "newTag" });
		tile = i.next();
		newTag = i.next();
	}

	public TagList(ElephantWindow w) {
		super(tile, newTag, "Find a tag");
		window = w;

		Elephant.eventBus.register(this);

		initialize();
	}

	@Override
	protected List<TagItem> queryFilter(String text) {
		ArrayList<TagItem> items = Factory.newArrayList();
		for (Tag t : Vault.getInstance().getTagsWithFilter(search.getText())) {
			items.add(new TagItem(t));
		}
		return items;
	}

	public void openSelected() {
	}

	@Override
	protected void newButtonAction() {
		System.out.println("NEW TAG");
	}

	class TagItem extends BackgroundPanel implements ToolbarList.ToolbarListItem, MouseListener {

		private Tag tag;
		private Dimension size = new Dimension(200, 30);
		private JLabel name;

		public TagItem(Tag t) {
			setLayout(null);
			setOpaque(false);

			tag = t;

			name = new JLabel(t.name(), JLabel.LEFT);
			name.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 0));
			name.setForeground(Color.DARK_GRAY);

			add(name);

			name.setBounds(0, 0, 200, 30);
		}

		@Override
		public void setSelected(boolean b) {
			if (b) {
				name.setForeground(Color.RED);
			} else {
				name.setForeground(Color.DARK_GRAY);
			}
			/*
			 * if (b) { setImage(notebookBgSelected); selectedNotebook = this; }
			 * else { setImage(notebookBg); } repaint();
			 */
		}

		@Override
		public Dimension getPreferredSize() {
			return size;
		}

		@Override
		public Dimension getMinimumSize() {
			return size;
		}

		@Override
		public Dimension getMaximumSize() {
			return size;
		}

		@Override
		public void mouseClicked(MouseEvent e) {
		}

		@Override
		public void mousePressed(MouseEvent e) {
		}

		@Override
		public void mouseReleased(MouseEvent e) {
		}

		@Override
		public void mouseEntered(MouseEvent e) {
		}

		@Override
		public void mouseExited(MouseEvent e) {
		}

	}

	@Override
	protected void vkEnter() {
		System.out.println("TagList: ENTER");
	}

	@Subscribe
	public void handleNoteChangedEvent(NoteChangedEvent event) {
		refresh();
	}
}
