package com.pinktwins.elephant.util;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.apache.commons.lang.SystemUtils;

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

	// https://stackoverflow.com/questions/30089804/true-full-screen-jframe-swing-application-in-mac-osx
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void enableOSXFullscreen(Window window) {
		if (SystemUtils.IS_OS_MAC_OSX) {
			try {
				Class util = Class.forName("com.apple.eawt.FullScreenUtilities");
				Class params[] = new Class[] { Window.class, Boolean.TYPE };
				Method method = util.getMethod("setWindowCanFullScreen", params);
				method.invoke(util, window, true);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void requestOSXFullscreen(Window window) {
		if (SystemUtils.IS_OS_MAC_OSX) {
			try {
				Class appClass = Class.forName("com.apple.eawt.Application");
				Class params[] = new Class[] {};

				Method getApplication = appClass.getMethod("getApplication", params);
				Object application = getApplication.invoke(appClass);
				Method requestToggleFulLScreen = application.getClass().getMethod("requestToggleFullScreen", Window.class);

				requestToggleFulLScreen.invoke(application, window);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
