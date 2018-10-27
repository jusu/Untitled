package com.pinktwins.elephant;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Iterator;
import java.util.List;

import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.google.common.eventbus.Subscribe;
import com.pinktwins.elephant.data.Notebook;
import com.pinktwins.elephant.data.Search;
import com.pinktwins.elephant.data.Tag;
import com.pinktwins.elephant.data.Vault;
import com.pinktwins.elephant.eventbus.SearchIndexChangedEvent;
import com.pinktwins.elephant.eventbus.TagsChangedEvent;
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

		initialize();

		Elephant.eventBus.register(this);
	}

	public void cleanup() {
		Elephant.eventBus.unregister(this);
		window = null;
	}

	// Called when initial SimpleSearchIndex run has completed
	public void ssiDone() {
		refresh();
	}

	@Override
	protected List<TagItem> queryFilter(String text) {
		List<TagItem> items = Factory.newArrayList();
		for (Tag t : Vault.getInstance().getTagsWithFilter(search.getText())) {
			items.add(new TagItem(t));
		}
		return items;
	}

	public void openSelected() {
		if (selectedItem != null) {
			window.showNotebook(Notebook.getNotebookWithTag(selectedItem.tag.id(), selectedItem.tag.name()));
		}
	}

	public void deleteSelected() {
		if (selectedItem != null) {
			Vault.getInstance().deleteTag(selectedItem.tag.id(), selectedItem.tag.name());
			refresh();
		}
	}

	@Override
	protected void newButtonAction() {
		newTag();
	}

	@Override
	protected void trashButtonAction() {
	}

	@Override
	protected void layoutItems() {
		TagItem prev = null;

		// Small gap before each header
		for (TagItem item : itemList) {
			String startChar = item.nameStr.substring(0, 1).toLowerCase();
			String prevStartChar = prev != null ? prev.nameStr.substring(0, 1).toLowerCase() : "";

			item.setDrawHeader(!item.isEditable && (prevStartChar.isEmpty() || !prevStartChar.equals(startChar)), false);

			if (prev != null && item.drawHeader) {
				prev.size.height += 8;
			}
			prev = item;
		}

		super.layoutItems();

		// First item of each column should have header
		if (itemList.size() > 0) {
			TagItem first = itemList.get(0);
			int firstY = first.getBounds().y;

			boolean loop = true;
			while (loop) {
				loop = false;
				for (int n = 1; n < itemList.size(); n++) {
					TagItem t = itemList.get(n);
					if (t.getBounds().y == firstY && !t.drawHeader) {
						t.setDrawHeader(true, true);
						loop = true;
						super.layoutItems();
						break;
					}
				}
			}
		}
	}

	// Fix sideways movement due to variable item heights
	@Override
	public void changeSelection(int delta, int keyCode) {
		boolean sideways = keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_RIGHT;

		if (sideways && selectedItem != null) {
			int idx = itemList.indexOf(selectedItem);
			int dir = keyCode == KeyEvent.VK_LEFT ? -1 : 1;

			Rectangle r = selectedItem.getBounds();
			for (int n = idx + dir; n >= 0 && n < itemList.size(); n += dir) {
				TagItem t = itemList.get(n);
				Rectangle tr = t.getBounds();
				if (tr.x != r.x) {
					if ((dir == -1 && tr.y <= r.y) || (dir == 1 && tr.y >= r.y)) {
						lc.itemsPerRow = Math.abs(idx - n);
						break;
					}
				}
			}
		}

		super.changeSelection(delta, keyCode);
	}

	class TagItem extends BackgroundPanel implements ToolbarList.ToolbarListItem, MouseListener {

		private Tag tag;
		private Dimension size = new Dimension(200, 34);
		private String nameStr;
		private int count;
		private boolean isSelected, isEditable;
		private boolean drawHeader, isContd;

		private final Stroke STROKE = new BasicStroke(1f);

		Color headerRed = Color.decode("#f14d53");
		Color headerLine = Color.decode("#bfbfbf");

		Color grayBg = Color.decode("#e3e3e3");
		Color grayLine = Color.decode("#cecece");
		Color darkGrayLine = Color.decode("#929292");
		Color fontColor = Color.decode("#666666");

		Color selectedBg = Color.decode("#32a5f3");
		Color selectedLine = Color.decode("#1a5279");

		public TagItem(Tag t) {
			setLayout(null);
			setOpaque(false);

			tag = t;

			nameStr = t.name();
			count = Search.ssi.notesByTag(t.id()).size();

			addMouseListener(this);
		}

		public String tagId() {
			return tag.id();
		}

		public void setDrawHeader(boolean b, boolean contd) {
			drawHeader = b;
			isContd = contd;

			if (b) {
				size = new Dimension(200, 58);
			} else {
				size = new Dimension(200, 34);
			}
		}

		@Override
		public void paint(Graphics g) {
			int countWidth = 2, countOffsetX = 25, maxWidth = 200;

			String countStr = Search.ssi.ready() ? String.valueOf(count) : "-";

			Font font = ElephantWindow.fontMediumPlus;
			g.setFont(font);
			FontMetrics fm = g.getFontMetrics(font);
			countWidth = fm.stringWidth(countStr);
			int width = fm.stringWidth(nameStr) + countWidth;
			int h = 25, hd2 = h / 2;

			width = Math.min(width, maxWidth);

			Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

			g2.setStroke(STROKE);

			int yOff = drawHeader ? 24 : 0;

			if (drawHeader) {
				g.setColor(headerLine);
				g.drawLine(0, 0, 190, 0);
				g.setColor(headerRed);
				g.drawString(nameStr.substring(0, 1).toUpperCase() + (isContd ? " (cont'd)" : ""), 10, hd2 + fm.getAscent() / 2);
			}

			g.setColor(isEditable ? Color.WHITE : (isSelected ? selectedBg : grayBg));
			g.fillArc(0, yOff + 2, h, h, 90, 180);
			g.fillArc(width + hd2, yOff + 2, h, h, 270, 180);
			g.fillRect(hd2, yOff + 2, width + hd2 + 1, h);

			g.setColor(isEditable ? darkGrayLine : (isSelected ? selectedLine : grayLine));
			g.drawArc(0, yOff + 2, h, h, 90, 180);
			g.drawArc(width + hd2, yOff + 2, h, h, 270, 180);
			g.drawLine(hd2, yOff + 2, width + h, yOff + 2);
			g.drawLine(hd2, yOff + h + 2, width + h, yOff + h + 2);

			if (!isEditable) {
				if (width == maxWidth) {
					// happens only on first draw - nameStr remains clipped. this is okay.
					while (!nameStr.isEmpty() && fm.stringWidth(nameStr) + countWidth > maxWidth) {
						nameStr = nameStr.substring(0, nameStr.length() - 1);
					}
					nameStr += "...";
				}

				g.setColor(isSelected ? Color.WHITE : fontColor);
				g.drawString(nameStr, hd2 - 2, yOff + hd2 + fm.getAscent() / 2 + 2);

				g.drawString(countStr, width - countWidth + countOffsetX, yOff + hd2 + fm.getAscent() / 2 + 2);
			}

			super.paint(g);
		}

		@Override
		public void setSelected(boolean b) {
			isSelected = b;
			repaint();
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
			if (isEditing) {
				return;
			}

			selectItem(this);
			if (e.getClickCount() == 2) {
				openSelected();
			}
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

	@Subscribe
	public void handleSearchIndexChanged(SearchIndexChangedEvent event) {
		refresh();
	}

	@Subscribe
	public void handleTagsChangedEvent(TagsChangedEvent event) {
		refresh();
	}

	public void newTag() {
		TagItem newTag = new TagItem(new Tag("New Tag"));
		JTextField edit = setEditable(newTag, "New Tag");

		edit.setBorder(null);
		edit.setBounds(12, 5, 170, 20);
		edit.setFont(ElephantWindow.fontMediumPlus);
		edit.setMaximumSize(new Dimension(180, 30));

		itemList.add(0, newTag);
		main.add(newTag, 0);
		layoutItems();

		deselectAll();
		edit.requestFocusInWindow();
	}

	@Override
	protected JTextField setEditable(final TagItem item, String name) {
		item.isEditable = true;
		final JTextField edit = super.setEditable(item, name);

		edit.getDocument().addDocumentListener(new DocumentListener() {
			private void update() {
				item.nameStr = edit.getText();
				item.repaint();
			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				update();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				update();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				update();
			}
		});

		item.add(edit);
		return edit;
	}

	@Override
	protected void doneEditing(TagItem item, String text) {
		Tag tag = null;

		if (item.getComponentCount() > 0) {
			Component c = item.getComponent(0);
			if (c instanceof JTextField) {
				JTextField t = (JTextField) c;
				String s = t.getText().trim();
				if (!s.isEmpty()) {
					tag = new Tag(s);
					Vault.getInstance().saveNewTag(tag);
				}
			}
		}

		update();
		layoutItems();

		if (tag != null) {
			for (TagItem t : itemList) {
				if (t.tagId().equals(tag.id())) {
					selectItem(t);
					break;
				}
			}
		}
	}

	@Override
	protected void cancelEditing(TagItem item) {
		update();
		layoutItems();
	}
}
