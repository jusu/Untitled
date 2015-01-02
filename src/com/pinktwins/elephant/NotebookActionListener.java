package com.pinktwins.elephant;

import com.pinktwins.elephant.data.Notebook;

interface NotebookActionListener {
	public void didCancelSelection();

	public void didSelect(Notebook nb);
}
