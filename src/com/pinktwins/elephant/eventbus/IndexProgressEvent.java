package com.pinktwins.elephant.eventbus;

import com.pinktwins.elephant.Elephant;

public class IndexProgressEvent {
	public float progress;

	public IndexProgressEvent(final float progress) {
		this.progress = progress;
	}
	
	public static void post(float progress) {
		Elephant.eventBus.post(new IndexProgressEvent(progress));
	}
}
