package com.pinktwins.elephant.util;

public class MeasureUtil {
	static long start;

	static public void start() {
		start = System.currentTimeMillis();
	}

	static public void stop(String s) {
		long stop = System.currentTimeMillis();
		System.out.println(s + ": " + (stop - start) + " ms.");
	}
}
