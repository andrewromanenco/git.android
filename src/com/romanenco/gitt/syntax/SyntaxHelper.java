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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import android.annotation.SuppressLint;
import android.text.TextUtils;
import android.util.Log;

/**
 * Utils to use with SyntaxHighlighter js.
 * 
 * @author Andrew Romanenco
 *
 */
public class SyntaxHelper {
	
	private static final String TAG = "SyntaxHelper";
	
	public static final String baseUrl = "file:///android_asset/syntaxhighlighter";
	
	public static final String Bash = "Bash";
	public static final String CSharp = "CSharp";
	public static final String ColdFusion = "ColdFusion";
	public static final String Cpp = "Cpp";
	public static final String Css = "Css";
	public static final String Delphi = "Delphi";
	public static final String Diff = "Diff";
	public static final String Erlang = "Erlang";
	public static final String Groovy = "Groovy";
	public static final String JScript = "JScript";
	public static final String Java = "Java";
	public static final String JavaFX = "JavaFX";
	public static final String Perl = "Perl";
	public static final String Php = "Php";
	public static final String Plain = "Plain";
	public static final String PowerShell = "PowerShell";
	public static final String Python = "Python";
	public static final String Ruby = "Ruby";
	public static final String Sass = "Sass";
	public static final String Scala = "Scala";
	public static final String Sql = "Sql";
	public static final String Vb = "Vb";
	public static final String Xml = "Xml";
	
	/**
	 * Mapping extension to brush.
	 */
	public static final String[][] mapping = {
		{Bash, "bash", "sh"},
		{CSharp,"cs"},
		{ColdFusion},
		{Cpp, "m", "h", "cpp", "c", "cc", "hpp"},
		{Css, "css"},
		{Delphi, "pas"},
		{Diff, "diff"},
		{Erlang, "erl"},
		{Groovy, "groovy"},
		{JScript, "js"},
		{Java, "java"},
		{JavaFX},
		{Perl, "pl"},
		{Php, "php"},
		{Plain, "txt", "cfg", "config", "md"},
		{PowerShell, "ps1", "psm1", "cmd"},
		{Python, "py"},
		{Ruby, "rb"},
		{Sass, "sass"},
		{Scala, "scala"},
		{Sql, "sql"},
		{Vb, },
		{Xml, "xml", "html"}
	};
	
	/**
	 * Get brush name based on extension.
	 * 
	 * @param extension
	 * @return null if no mapping found
	 */
	@SuppressLint("DefaultLocale")
	public static String getBrush(String extension) {
		if (TextUtils.isEmpty(extension)) {
			return null;
		}
		extension = extension.toLowerCase();
		for (String map[]: mapping) {
			for (int i = 1; i < map.length; i++) {
				if (extension.equals(map[i])) {
					return map[0];
				}
			}
		}
		return null;
	}
	
	/**
	 * Get render ready html for source code.
	 * No point to move html to external file.
	 * 
	 * @param brush
	 * @param pathToFile
	 * @return
	 */
	@SuppressLint("DefaultLocale")
	public static String getCodeAsHTML(String brush, String pathToFile) {
		StringBuilder content = new StringBuilder();
		content.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n");
		content.append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">\n");
		content.append("<head>\n");
		content.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />\n");
		content.append("<script type=\"text/javascript\" src=\"file:///android_asset/syntaxhighlighter/scripts/shCore.js\"></script>\n");
		content.append("<script type=\"text/javascript\" src=\"file:///android_asset/syntaxhighlighter/scripts/shBrush" + brush + ".js\"></script>\n");
		content.append("<link type=\"text/css\" rel=\"stylesheet\" href=\"file:///android_asset/syntaxhighlighter/styles/shCoreDefault.css\"/>\n");
		content.append("<script type=\"text/javascript\">\n");
		content.append("SyntaxHighlighter.defaults.toolbar = false;\n");
		content.append("SyntaxHighlighter.all();\n");
		content.append("</script>\n");
		content.append("</head>\n");

		content.append("<body style=\"background: white; font-family: Helvetica\">\n");

		content.append("<pre class=\"brush: " + brush.toLowerCase()  + ";\">\n");
		content.append(fileContent(pathToFile).replaceAll("<", "&lt;"));
		content.append("</pre>\n");
		content.append("</body>\n");
		content.append("</html>");
		return content.toString();
	}
	
	private static String fileContent(String path) {
		StringBuilder fileData = new StringBuilder();
		File f = new File(path);
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(f));
		} catch (FileNotFoundException e) {
			Log.e(TAG, "File not found", e);
			return e.getMessage();
		}
		char[] buf = new char[1024];
		int numRead=0;
		try {
			while((numRead=reader.read(buf)) != -1){
				String readData = String.valueOf(buf, 0, numRead);
				fileData.append(readData);
			}
		} catch (IOException e) {
			Log.e(TAG, "File read error", e);
			return e.getMessage();
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				// nothing
			}
		}
		return fileData.toString();
	}

}
