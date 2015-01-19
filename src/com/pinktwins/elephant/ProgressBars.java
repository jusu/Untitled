package com.pinktwins.elephant;

public class ProgressBars {
	public static String getCharacterBar(int progress) {
		String s = "";
		for (int n = 0; n < 10; n++) {
			if (progress > n * 10) {
				s += "•";
			} else {
				s += "·";
			}
		}
		return s;
	}
}
