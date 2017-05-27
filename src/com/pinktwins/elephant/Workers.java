package com.pinktwins.elephant;

import java.util.List;

import javax.swing.SwingWorker;

import com.pinktwins.elephant.util.Factory;

class Workers<T> {
	private final List<SwingWorker<T, Void>> list = Factory.newArrayList();
	private SwingWorker<T, Void> finalizer = null;
	private int running = 0;

	public void add(SwingWorker<T, Void> w) {
		list.add(w);
	}

	public void addFinalizer(SwingWorker<T, Void> w) {
		this.finalizer = w;
	}

	public void next() {
		if (!list.isEmpty()) {
			SwingWorker<T, Void> w = list.get(0);
			list.remove(0);
			running++;
			w.execute();
		} else {
			if (running == 0) {
				if (finalizer != null) {
					finalizer.execute();
					finalizer = null;
				}
			}
		}
	}
/*
	public void last() {
		if (!list.isEmpty()) {
			SwingWorker<T, Void> w = list.get(list.size() - 1);
			clear();
			w.execute();
		}
	}
*/
	public void done() {
		running--;
		next();
	}

	public void finish() {
		running--;
		if (running == 0) {
			if (finalizer != null) {
				finalizer.execute();
				finalizer = null;
			}
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
