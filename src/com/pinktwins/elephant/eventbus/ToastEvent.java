package com.pinktwins.elephant.eventbus;

public class ToastEvent extends ElephantEvent {
	public String text;

	public ToastEvent(String text) {
		this.text = text;
	}
}
