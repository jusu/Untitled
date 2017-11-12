package com.pinktwins.elephant.util;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.lang.reflect.Field;

public class ScreenUtil {

	private static Boolean hasRetina = null;
	
	public static boolean isRetina() {
		if (hasRetina == null) {
			hasRetina = _isRetina();
		}
		return hasRetina;
	}

	// http://bulenkov.com/2013/06/23/retina-support-in-oracle-jdk-1-7/
	private static boolean _isRetina() {
		GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
		final GraphicsDevice device = env.getDefaultScreenDevice();

		try {
			Field field = device.getClass().getDeclaredField("scale");

			if (field != null) {
				field.setAccessible(true);
				Object scale = field.get(device);

				if (scale instanceof Integer && ((Integer) scale).intValue() == 2) {
					return true;
				}
			}
		} catch (Exception ignore) {
		}
		return false;
	}
}
