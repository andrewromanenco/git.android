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

import java.io.File;

import com.romanenco.gitt.syntax.ImageHelper;
import com.romanenco.gitt.syntax.SyntaxHelper;
import com.romanenco.gitt.R;

import android.os.Bundle;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Shows file content in WebView with syntax highlighting.
 * 
 * Style is chosen based on file extension.
 * If no match found, user is asked for style to use.
 * 
 * Special case when file is an image.
 * 
 * @author Andrew Romanenco
 * 
 */
public class CodeViewActivity extends Activity {

	static final String TAG = "CodeView";

	public static final String FILE_KEY = "FILE";
	public static final String BRUSH_KEY = "BRUSH";
	private String file;
	private String brush;

	private WebView webView;
	private ListView listView;

	private BrushesAdapter brushesAdapter;

	@SuppressLint("SetJavaScriptEnabled")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_code_view);
		listView = (ListView) findViewById(R.id.list);
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> data, View view, int at,
					long id) {
				applyBrush(at);
			}
		});

		brushesAdapter = new BrushesAdapter();
		listView.setAdapter(brushesAdapter);
		webView = (WebView) findViewById(R.id.web);
		WebSettings webSettings = webView.getSettings();
		webSettings.setJavaScriptEnabled(true);
		webSettings.setBuiltInZoomControls(true);
		webSettings.setUseWideViewPort(true);
		file = getIntent().getStringExtra(FILE_KEY);

		Log.d(TAG, "Openning: " + file);
		File f = new File(this.getFilesDir(), file);
		String name = f.getName();
		this.setTitle(name);
		String extension = "";
		int ind = name.lastIndexOf('.');
		if (ind > 0) {
			extension = name.substring(ind + 1);
		}
		brush = SyntaxHelper.getBrush(extension);
		if (savedInstanceState != null) {
			file = savedInstanceState.getString(FILE_KEY);
			brush = savedInstanceState.getString(BRUSH_KEY);
		}
		if (brush == null) {
			if (ImageHelper.isImage(extension)) {
				brush = "Image";
				loadFileContent();
			} else {
				askForExplicitBrush();
			}
		} else {
			loadFileContent();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.code_view, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.code_view_action_syntax:
			askForExplicitBrush();
		}
		return super.onOptionsItemSelected(item);
	}

	private void loadFileContent() {
		webView.setVisibility(View.VISIBLE);
		listView.setVisibility(View.GONE);
		Log.d(TAG, "Showing with brush: " + brush);
		if (brush.equals("Image")) {
			String path = "file://" + this.getFilesDir() + "/" + file;
			Log.d(TAG, "Image: " + path);
			webView.loadUrl(path);
		} else {
			String contentPath = this.getFilesDir() + "/" + file;
			String content = SyntaxHelper.getCodeAsHTML(brush, contentPath);
			webView.loadDataWithBaseURL(SyntaxHelper.baseUrl, content,
					"text/html", null, null);
		}
	}

	private void askForExplicitBrush() {
		Log.d(TAG, "Ask for specific brush");
		webView.setVisibility(View.GONE);
		listView.setVisibility(View.VISIBLE);
	}

	private void applyBrush(int at) {
		brush = brushesAdapter.getItem(at);
		loadFileContent();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(FILE_KEY, file);
		outState.putString(BRUSH_KEY, brush);
	}

	/**
	 * Use Syntax helper as source for all possible brushes.
	 * 
	 * @author Andrew Romanenco
	 * 
	 */
	class BrushesAdapter extends BaseAdapter {

		private final int padding_dp = 20;
		private final int padding_px;

		public BrushesAdapter() {
			float scale = CodeViewActivity.this.getResources()
					.getDisplayMetrics().density;
			padding_px = (int) (padding_dp * scale + 0.5f);
		}

		@Override
		public int getCount() {
			return SyntaxHelper.mapping.length;
		}

		@Override
		public String getItem(int at) {
			return SyntaxHelper.mapping[at][0];
		}

		@Override
		public long getItemId(int at) {
			return SyntaxHelper.mapping[at][0].hashCode();
		}

		@Override
		public View getView(int at, View view, ViewGroup parent) {
			if (view == null) {
				view = new TextView(CodeViewActivity.this);
				view.setPadding(padding_px, padding_px, padding_px, padding_px);
				((TextView) view).setTextAppearance(CodeViewActivity.this,
						android.R.attr.textAppearanceMedium);
			}
			((TextView) view).setText(SyntaxHelper.mapping[at][0]);
			return view;
		}

	}

}
