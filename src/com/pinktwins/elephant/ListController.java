package com.pinktwins.elephant;

import java.awt.Component;
import java.awt.Rectangle;
import java.util.List;

import javax.swing.JScrollBar;
import javax.swing.JScrollPane;

import org.pushingpixels.trident.Timeline;

public class ListController<T> {

	public static <T> ListController<T> newInstance() {
		return new ListController<T>();
	}

	public int itemsPerRow = 1;

	public T changeSelection(List<T> list, T currentSelectionItem, int delta, boolean sideways) {
		int len = list.size();
		int select = -1;

		if (sideways) {
			delta *= itemsPerRow;
		}

		if (currentSelectionItem == null) {
			if (len > 0) {
				if (delta < 0) {
					select = len - 1;
				} else {
					select = 0;
				}
			}
		} else {
			int currentIndex = list.indexOf(currentSelectionItem);
			select = currentIndex + delta;
		}

		if (select == len && len > 0) {
			select--;
		}

		if (select < 0) {
			select = 0;
		}

		return (select >= 0 && select < len) ? list.get(select) : null;
	}

	public <T2 extends Component> void updateVerticalScrollbar(T2 item, JScrollPane scroll) {
		Rectangle b = item.getBounds();
		int itemY = b.y;
		int y = scroll.getVerticalScrollBar().getValue();
		int scrollHeight = scroll.getBounds().height;

		if (itemY < y || itemY + b.height >= y + scrollHeight) {

			if (itemY < y) {
				itemY -= 12;
			} else {
				itemY -= scrollHeight - b.height - 12;
			}

			JScrollBar bar = scroll.getVerticalScrollBar();
			Timeline timeline = new Timeline(bar);
			timeline.addPropertyToInterpolate("value", bar.getValue(), itemY);
			timeline.setDuration(100);
			timeline.play();
		}
	}

	public <T2 extends Component> void updateHorizontalScrollbar(T2 item, JScrollPane scroll) {
		Rectangle b = item.getBounds();
		int itemX = b.x;
		int x = scroll.getHorizontalScrollBar().getValue();
		int scrollWidth = scroll.getBounds().width;

		if (itemX < x || itemX + b.height >= x + scrollWidth) {

			if (itemX < x) {
				itemX -= 12;
			} else {
				itemX -= scrollWidth - b.width - 12;
			}

			JScrollBar bar = scroll.getHorizontalScrollBar();
			Timeline timeline = new Timeline(bar);
			timeline.addPropertyToInterpolate("value", bar.getValue(), itemX);
			timeline.setDuration(100);
			timeline.play();
		}
	}
}
