package com.pinktwins.elephant;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.List;

import javax.swing.TransferHandler;

public class AttachmentTransferHandler extends TransferHandler {

	final private EditorEventListener listener;

	public AttachmentTransferHandler(EditorEventListener listener) {
		super();
		this.listener = listener;
	}

	@Override
	public boolean canImport(TransferHandler.TransferSupport info) {
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean importData(TransferHandler.TransferSupport info) {
		if (!info.isDrop()) {
			return false;
		}

		Transferable t = info.getTransferable();
		List<File> data;
		try {
			data = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
		} catch (Exception e) {
			return false;
		}

		listener.filesDropped(data);

		return true;
	}
}
