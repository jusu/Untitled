package com.pinktwins.elephant.util;

public class MeasureUtil {
	static long start;

	public static void start() {
		start = System.currentTimeMillis();
	}

	public static void stop(String s) {
		long stop = System.currentTimeMillis();
		System.out.println(s + ": " + (stop - start) + " ms.");
	}
}
