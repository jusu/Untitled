package com.pinktwins.elephant.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Factory {
	public static <T> HashSet<T> newHashSet() {
		return new HashSet<T>();
	}
	
	public static <T> ArrayList<T> newArrayList() {
		return new ArrayList<T>();
	}
	
	public static <K,V> HashMap<K,V> newHashMap() {
		return new HashMap<K,V>();
	}
}
