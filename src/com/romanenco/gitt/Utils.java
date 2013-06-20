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

package com.romanenco.gitt;

import com.romanenco.gitt.R;

import android.annotation.SuppressLint;
import android.content.Context;

/**
 * Utils class.
 * 
 * @author Andrew Romanenco
 *
 */
public class Utils {
	
	private static final float kb = 1024;
	private static final float mb = 1024*1024;

	/**
	 * Folder name for a repo's name.
	 * 
	 * @param s
	 * @return
	 */
	public static String makeFolderName(String s) {
		return Integer.toString(Math.abs(s.hashCode()));
	}
	
	/**
	 * Return user friendly size in bytes, kb or mb.
	 * Does not print .0
	 * 
	 * @param context
	 * @param size
	 * @return
	 */
	@SuppressLint("DefaultLocale")
	public static String formatFileSize(Context context, long size) {
		int rid;
		float reduced;
		if (size < kb) {
			return context.getString(R.string.size_bytes, size);
		} else if (size < mb) {
			rid = R.string.size_kb;
			reduced = (float)size/kb;
		} else {
			rid = R.string.size_mb;
			reduced = (float)size/mb;
		}		
		String value = String.format("%.1f", reduced);
		if (value.endsWith("0")) {
			value = String.format("%.0f", reduced);
		}
		return context.getString(rid, value);
	}

}
