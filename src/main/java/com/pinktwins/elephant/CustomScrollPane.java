package com.pinktwins.elephant;

import java.awt.Component;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.plaf.ScrollBarUI;

import com.pinktwins.elephant.ui.CustomScrollBarUI;

public class CustomScrollPane extends JScrollPane {
	private boolean isLocked;
	private int lockedValue;
	private int inactivity = -1;

	Timer timer = new Timer();

	class Unlock extends TimerTask {
		@Override
		public void run() {
			setLocked(false);
		}
	}

	public CustomScrollPane(Component view) {
		super(view);

		final JScrollBar bar = getVerticalScrollBar();

		bar.addAdjustmentListener(new AdjustmentListener() {
			@Override
			public void adjustmentValueChanged(AdjustmentEvent e) {
				if (isLocked) {
					bar.setValue(lockedValue);

					if (inactivity > 0) {
						timer.schedule(new Unlock(), inactivity);
						inactivity = -1;
					}
				}
			}
		});
	}

	public void setLocked(boolean b) {
		isLocked = b;
		lockedValue = getVerticalScrollBar().getValue();
	}

	public boolean isLocked() {
		return isLocked;
	}

	public void unlockAfter(int inactivity) {
		this.inactivity = inactivity;
	}

	public void useTrackColorB() {
		ScrollBarUI ui = this.getVerticalScrollBar().getUI();
		if (ui instanceof CustomScrollBarUI) {
			((CustomScrollBarUI) ui).useTrackColorB();
		}
	}
}
