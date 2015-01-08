package com.pinktwins.elephant;

import java.awt.Image;
import java.io.File;

public interface ImageScaler {
	public Image scale(Image i, File source);
}
