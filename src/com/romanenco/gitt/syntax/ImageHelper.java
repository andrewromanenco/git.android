/*
 * Copyright 2013 Andrew Romanenco.
 * 
 * This file is part of Gitt.
 * 
 * Gitt is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Gitt is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Gitt.  If not, see <http://www.gnu.org/licenses/>. 
 */

package com.romanenco.gitt.syntax;

import android.annotation.SuppressLint;

/**
 * Handle images from local repo files.
 * 
 * @author Andrew Romanenco
 *
 */
public class ImageHelper {

	public static final String[] Images = {
		"png",
		"jpg",
		"jpeg",
		"gif",
		"bmp"
	};
	
	@SuppressLint("DefaultLocale")
	public static boolean isImage(String extension) {
		extension = extension.toLowerCase();
		for (String ext: Images) {
			if (ext.equals(extension)) {
				return true;
			}
		}
		return false;
	}
	
	public static String getHtmlView(String file) {
		StringBuilder result = new StringBuilder();
		result.append("<html></body>");
		result.append("<img src=" + file + "/>");
		result.append("</body></html>");
		return result.toString();
	}
	
}
