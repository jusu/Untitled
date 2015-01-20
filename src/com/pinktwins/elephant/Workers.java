package com.pinktwins.elephant;

import java.util.ArrayList;

import javax.swing.SwingWorker;

import com.pinktwins.elephant.util.Factory;

class Workers<T> {
	private final ArrayList<SwingWorker<T, Void>> workers = Factory.newArrayList();

	public void add(SwingWorker<T, Void> w) {
		workers.add(w);
	}

	public void next() {
		if (workers.size() > 0) {
			SwingWorker<T, Void> w = workers.get(0);
			workers.remove(0);
			w.execute();
		}
	}

	public void last() {
		if (workers.size() > 0) {
			SwingWorker<T, Void> w = workers.get(workers.size() - 1);
			workers.clear();
			w.execute();
		}
	}

	public boolean isEmpty() {
		return workers.isEmpty();
	}

	public int size() {
		return workers.size();
	}

	public void clear() {
		workers.clear();
	}
}
