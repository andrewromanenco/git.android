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
import android.content.Intent;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.EditText;

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
	private File file;
	private String brush;

	private WebView webView;
	private View finderBar;
	private EditText finderText;

	@SuppressLint("SetJavaScriptEnabled")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_code_view);
		
		finderText = (EditText)findViewById(R.id.finder_text);
		finderText.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {}
			
			@Override
			public void afterTextChanged(Editable s) {
				searchInSource(s.toString());
			}

		});
		
		finderBar = findViewById(R.id.finder_bar);
		findViewById(R.id.finder_close).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						finderBar.setVisibility(View.GONE);
						webView.clearMatches();
					}
				});
		
		findViewById(R.id.finder_prev).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						webView.findNext(false);
					}
				});
		findViewById(R.id.finder_next).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						webView.findNext(true);
					}
				});
		
		webView = (WebView) findViewById(R.id.web);
		WebSettings webSettings = webView.getSettings();
		webSettings.setJavaScriptEnabled(true);
		webSettings.setBuiltInZoomControls(true);
		webSettings.setUseWideViewPort(true);
		file = (File)getIntent().getSerializableExtra(FILE_KEY);

		Log.d(TAG, "Openning: " + file);
		String name = file.getName();
		this.setTitle(name);
		String extension = "";
		int ind = name.lastIndexOf('.');
		if (ind > 0) {
			extension = name.substring(ind + 1);
		}
		brush = SyntaxHelper.getBrush(extension);
		if (savedInstanceState != null) {
			file = (File)savedInstanceState.getSerializable(FILE_KEY);
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
			break;
		case R.id.code_view_action_find:
			showFinderBar();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void loadFileContent() {
		webView.setVisibility(View.VISIBLE);
		Log.d(TAG, "Showing with brush: " + brush);
		if (brush.equals("Image")) {
			String path = "file://" + file.getAbsolutePath();
			Log.d(TAG, "Image: " + path);
			webView.loadUrl(path);
		} else {
			String contentPath = file.getAbsolutePath();
			String content = SyntaxHelper.getCodeAsHTML(brush, contentPath);
			webView.loadDataWithBaseURL(SyntaxHelper.baseUrl, content,
					"text/html", null, null);
		}
	}

	private void askForExplicitBrush() {
		Log.d(TAG, "Ask for specific brush");
		finderBar.setVisibility(View.GONE);
		webView.clearMatches();
		Intent intent = new Intent(this, SyntaxPickActivity.class);
		startActivityForResult(intent, 1);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == 1) {
			if (resultCode == RESULT_OK) {
				int index = data.getIntExtra(SyntaxPickActivity.BRUSH_INDEX, 0);
				brush = SyntaxHelper.mapping[index][0];
				Log.d(TAG, "Picked: " + brush);
				loadFileContent();
			} else {
				finish(); // if nothing was picked, no need to show source code
			}
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable(FILE_KEY, file);
		outState.putString(BRUSH_KEY, brush);
	}
	
	// Find in source

	private void showFinderBar() {
		finderBar.setVisibility(View.VISIBLE);
		if (!TextUtils.isEmpty(finderText.getText().toString())) {
			searchInSource(finderText.getText().toString());
		}
	}
	
	private void searchInSource(String text) {
		webView.findAllAsync(text);
		
	}
}
