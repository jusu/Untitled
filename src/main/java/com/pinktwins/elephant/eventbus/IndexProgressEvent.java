package com.pinktwins.elephant.eventbus;

public class IndexProgressEvent extends ElephantEvent {
	public final float progress;

	public IndexProgressEvent(final float progress) {
		this.progress = progress;
	}
}
