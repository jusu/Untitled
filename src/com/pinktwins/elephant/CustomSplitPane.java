package com.pinktwins.elephant;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JSplitPane;
import javax.swing.plaf.SplitPaneUI;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

public class CustomSplitPane extends JSplitPane {
	private static final long serialVersionUID = -8922501394423552180L;

	public static enum DividerColor {
		COLOR1, COLOR2
	};

	private String saveKey;
	private int locationLimit;

	int fixedLocation;
	boolean isDragging;

	Color color1 = Color.decode("#eeeeee");
	Color color2 = Color.decode("#dadada");
	Color colorLine1 = Color.decode("#cccccc");
	Color colorLine2 = Color.decode("#bfbfbf");
	Color colorLine3 = Color.decode("#e3e3e3");

	Color currentColor = color1;

	class CustomSplitPaneUI extends BasicSplitPaneUI {
		public CustomSplitPaneUI() {
			super();
		}

		@Override
		public BasicSplitPaneDivider createDefaultDivider() {
			return new BasicSplitPaneDivider(this) {
				@Override
				public void paint(Graphics g) {
					super.paint(g);
					g.setColor(currentColor);
					g.fillRect(0, 0, getWidth(), getHeight());

					if (currentColor == color1) {
						g.setColor(colorLine1);
						g.drawLine(0, NoteList.separatorLineY(), getWidth(), NoteList.separatorLineY());
					}
					if (currentColor == color2) {
						g.setColor(colorLine2);
						g.drawLine(0, 42, getWidth(), 42);
						g.setColor(colorLine3);
						g.drawLine(0, 43, getWidth(), 43);
					}
				}
			};
		}

		public void hideDivider() {
			BasicSplitPaneDivider d = getDivider();
			if (d != null) {
				d.setBorder(null);
			}
		}
	}

	public void setDividerColor(DividerColor color) {
		switch (color) {
		case COLOR1:
			currentColor = color1;
			repaint();
			break;
		case COLOR2:
			currentColor = color2;
			repaint();
			break;
		default:
			break;
		}
	}

	public CustomSplitPane(int i) {
		super(i);

		final CustomSplitPaneUI ui = new CustomSplitPaneUI();
		this.setUI(ui);

		addComponentListener(new ComponentListener() {

			@Override
			public void componentResized(ComponentEvent e) {
				setDividerLocation(fixedLocation);
				ui.hideDivider();
			}

			@Override
			public void componentMoved(ComponentEvent e) {
			}

			@Override
			public void componentShown(ComponentEvent e) {
			}

			@Override
			public void componentHidden(ComponentEvent e) {
			}
		});

		addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent pce) {
				if (!isDragging) {
					setDividerLocation(fixedLocation);
				}

				if (locationLimit > 0 && getDividerLocation() > locationLimit) {
					setDividerLocation(locationLimit);
				}

				if (isDragging) {
					fixedLocation = getDividerLocation();
				}
			}
		});
	}

	public void setFixedLocation(int loc) {
		fixedLocation = loc;
		setDividerLocation(loc);
	}

	public void initLocationWithKey(String key, int defaultValue) {
		if (saveKey != null) {
			// Should call only once
			throw new AssertionError();
		}

		int loc = Elephant.settings.getInt(key);

		if (loc == 0) {
			loc = defaultValue;
		}

		if (loc > 0) {
			setFixedLocation(loc);
		}

		saveKey = key;

		SplitPaneUI ui = getUI();
		if (ui instanceof BasicSplitPaneUI) {
			BasicSplitPaneUI bui = (BasicSplitPaneUI) ui;

			bui.getDivider().addMouseListener(new MouseListener() {

				@Override
				public void mouseClicked(MouseEvent e) {
				}

				@Override
				public void mousePressed(MouseEvent e) {
					isDragging = true;
				}

				@Override
				public void mouseReleased(MouseEvent e) {
					isDragging = false;
					Elephant.settings.set(saveKey, getDividerLocation());
				}

				@Override
				public void mouseEntered(MouseEvent e) {
				}

				@Override
				public void mouseExited(MouseEvent e) {
				}
			});
		}
	}

	public void limitLocation(int limit) {
		locationLimit = limit;
	}

	@Override
	public void setLeftComponent(Component c) {
		int i = getDividerLocation();
		super.setLeftComponent(c);
		setFixedLocation(i);
	}

	@Override
	public void setRightComponent(Component c) {
		int i = getDividerLocation();
		super.setRightComponent(c);
		setFixedLocation(i);
	}

}
