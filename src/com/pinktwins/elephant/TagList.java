package com.pinktwins.elephant;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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

// XXX some duplicate code with Notebooks class. dedup.

public class TagList extends ToolbarList<TagList.TagItem> {

	private static Image tile, newTag;

	private ElephantWindow window;
	private ListController<TagItem> lc = ListController.newInstance();

	private ArrayList<TagItem> tagItems = Factory.newArrayList();

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
	public void refresh() {
		update();
		layoutItems();
	}

	@Override
	protected void update() {
		for (TagItem item : tagItems) {
			item.setVisible(false);
		}

		main.removeAll();
		tagItems.clear();

		Collection<Tag> list = Vault.getInstance().getTagsWithFilter(search.getText());
		Collections.sort((List<Tag>) list);

		for (Tag t : list) {
			TagItem item = new TagItem(t);
			main.add(item);
			tagItems.add(item);
		}
	}
	
	@Override
	protected void layoutItems() {
		Insets insets = main.getInsets();
		Dimension size = new Dimension();

		int xOff = 12 + insets.left;
		int yOff = 57;

		int x = 0;
		int y = yOff;

		Rectangle b = main.getBounds();

		for (TagItem item : tagItems) {
			size = item.getPreferredSize();
			lc.itemsPerRow = (b.height - yOff) / size.height;

			item.setBounds(xOff + x, y + insets.top, size.width, size.height);

			y += size.height;

			if (y + size.height > b.height) {
				x += size.width + xOff;
				y = yOff;
			}
		}

		Dimension d = main.getPreferredSize();
		d.width = x + size.width + xOff * 2;
		main.setPreferredSize(d);
	}

	@Override
	public void changeSelection(int delta, int keyCode) {
		boolean sideways = keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_RIGHT;

		TagItem item = lc.changeSelection(tagItems, selectedItem, delta, sideways);
		if (item != null) {
			selectTag(item);
		}
	}

	void selectTag(TagItem item) {
		deselectAll();
		item.setSelected(true);
		selectedItem = item;

		lc.updateHorizontalScrollbar(item, scroll);
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
