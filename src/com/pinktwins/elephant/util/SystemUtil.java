package com.pinktwins.elephant.util;

import org.apache.commons.lang.text.StrSubstitutor;

public class SystemUtil {

	private SystemUtil() {
	}

	static public String interpolateEnvironmentVariables(final String str) {
		if (str == null) {
			return null;
		}

		String res = new StrSubstitutor(System.getenv()).replace(str);
		return res;
	}

}
