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

import com.romanenco.gitt.syntax.SyntaxHelper;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Pick specific syntax for a source file.
 * 
 * @author Andrew Romanenco
 * 
 */
public class SyntaxPickActivity extends ListActivity {

	public static final String BRUSH_INDEX = "brush_index";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		ListView listView = getListView();
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> data, View view, int at,
					long id) {
				Intent returnIntent = new Intent();
				returnIntent.putExtra(BRUSH_INDEX, at);
				setResult(RESULT_OK, returnIntent);
				finish();
			}
		});
		listView.setAdapter(new BrushesAdapter());
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
			float scale = SyntaxPickActivity.this.getResources()
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
				view = new TextView(SyntaxPickActivity.this);
				view.setPadding(padding_px, padding_px, padding_px, padding_px);
				((TextView) view).setTextAppearance(SyntaxPickActivity.this,
						android.R.attr.textAppearanceMedium);
			}
			((TextView) view).setText(SyntaxHelper.mapping[at][0]);
			return view;
		}

	}
}
