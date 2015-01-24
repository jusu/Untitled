package com.pinktwins.elephant;

import java.util.List;

import javax.swing.SwingWorker;

import com.pinktwins.elephant.util.Factory;

class Workers<T> {
	private final List<SwingWorker<T, Void>> list = Factory.newArrayList();

	public void add(SwingWorker<T, Void> w) {
		list.add(w);
	}

	public void next() {
		if (!list.isEmpty()) {
			SwingWorker<T, Void> w = list.get(0);
			list.remove(0);
			w.execute();
		}
	}

	public void last() {
		if (!list.isEmpty()) {
			SwingWorker<T, Void> w = list.get(list.size() - 1);
			clear();
			w.execute();
		}
	}

	public boolean isEmpty() {
		return list.isEmpty();
	}

	public int size() {
		return list.size();
	}

	public void clear() {
		list.clear();
	}
}
