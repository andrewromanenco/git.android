/*
 * Copyright 2013 Andrew Romanenco.
 * 
 * This file is part of Git.
 * 
 * Git is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Git is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Git.  If not, see <http://www.gnu.org/licenses/>. 
 */

package com.romanenco.gitt;

import java.io.File;
import java.util.List;

import com.romanenco.gitt.dao.Repo;
import com.romanenco.gitt.git.GitHelper;
import com.romanenco.gitt.git.GitHelper.LogEntry;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;
import android.app.Activity;

/**
 * Show up to 50 log messages from current active branch.
 * 
 * This is VERY basic implementation.
 * To be replaced with table layout and additional features.
 * 
 * @author Andrew Romanenco
 *
 */
public class LogViewActivity extends Activity {
	
	private static final int LOG_LIMIT = 50; // max log messages to show
	
	public static final String REPO = "REPO";
	
	private Repo current;
	private TextView textView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_log_view);
		
		current = (Repo)getIntent().getSerializableExtra(REPO);
		
		textView = (TextView)findViewById(R.id.text_log);
		textView.setMovementMethod(new ScrollingMovementMethod());
		File repoDir = new File(GitHelper.getBaseRepoDir(this, current.getFolder()), current.getFolder());
		
		new LogReaderTask().execute(repoDir);
	}
	
	/**
	 * Walk through commit tree in background.
	 * 
	 * @author Andrew Romanenco
	 *
	 */
	class LogReaderTask extends AsyncTask<File, Void, List<LogEntry>> {

		@Override
		protected List<LogEntry> doInBackground(File... params) {
			File repoDir = params[0];
			List<LogEntry> log = GitHelper.readRepoHistory(repoDir.getAbsolutePath(), LOG_LIMIT);
			return log;
		}

		@Override
		protected void onPostExecute(List<LogEntry> result) {
			StringBuilder sb = new StringBuilder();
			for (LogEntry entry: result) {
				sb.append(entry.getText());
				sb.append("\n-----------------\n\n"); // hardcode for now, make nice view later
			}
			textView.setText(sb.toString());
		}
		
	}

}
