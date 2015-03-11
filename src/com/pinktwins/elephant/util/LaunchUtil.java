package com.pinktwins.elephant.util;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.lang3.SystemUtils;

public class LaunchUtil {
	private static final Logger LOG = Logger.getLogger(LaunchUtil.class.getName());

	private LaunchUtil() {
	}

	public static void launch(File f) {
		if (f == null || !f.exists()) {
			return;
		}

		if (SystemUtils.IS_OS_WINDOWS || SystemUtils.IS_OS_LINUX) {
			final List<String> command = Factory.newArrayList();

			if (SystemUtils.IS_OS_WINDOWS) {
				command.add("rundll32");
				command.add("SHELL32.DLL,ShellExec_RunDLL");
			} else {
				command.add("/usr/bin/env");
				command.add("xdg-open");
			}

			command.add(f.getAbsolutePath());

			final ProcessBuilder builder = new ProcessBuilder(command);
			try {
				builder.start();
			} catch (IOException e) {
				LOG.severe("Fail: " + e);
			}
		} else {
			try {
				Desktop.getDesktop().edit(f);
			} catch (IOException e) {
				LOG.severe("Fail: " + e);
			}
		}
	}
}
