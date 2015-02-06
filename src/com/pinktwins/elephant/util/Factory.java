package com.pinktwins.elephant.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

public class Factory {

	private Factory() {
	}

	public static <T> Set<T> newHashSet() {
		return new HashSet<T>();
	}

	public static <T> SortedSet<T> newSortedSet() {
		return new TreeSet<T>();
	}

	public static <T> List<T> newArrayList() {
		return new ArrayList<T>();
	}

	public static <K, V> Map<K, V> newHashMap() {
		return new HashMap<K, V>();
	}

	public static <K, V> Map<K, V> newConcurrentHashMap() {
		return new ConcurrentHashMap<K, V>();
	}
}
