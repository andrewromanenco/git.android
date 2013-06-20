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

import java.util.ArrayList;
import java.util.List;

import com.romanenco.gitt.dao.DAO;
import com.romanenco.gitt.dao.Repo;
import com.romanenco.gitt.git.GitHelper;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.app.ListActivity;
import android.content.Intent;

/**
 * Display list of branches/tags to the user and handle checkout command.
 * 
 * @author Andrew Romanenco
 * 
 */
public class CheckoutActivity extends ListActivity {

	static final String TAG = "CheckoutActivity";

	public static final String REPO = "repo_key";

	private Repo repo;
	private RepoBranchesAndTagsAdapter adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		repo = (Repo) getIntent().getSerializableExtra(REPO);
		adapter = new RepoBranchesAndTagsAdapter();
		getListView().setAdapter(adapter);
	}

	@Override
	protected void onListItemClick(ListView lv, View view, int position, long id) {
		DAO dao = new DAO(this);
		dao.open(true);
		repo.setState(Repo.State.Busy);
		dao.update(repo);
		dao.close();
		Toast.makeText(this, getString(R.string.msg_checking_out),
				Toast.LENGTH_SHORT).show();
		Intent co = new Intent(this, GitService.class);
		co.putExtra(GitService.COMMAND, GitService.Command.Checkout);
		co.putExtra(GitService.REPO, repo);
		co.putExtra(GitService.SWITCH_TO, adapter.getItem(position));
		startService(co);
		Intent back = new Intent(this, MainActivity.class);
		back.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(back);
	}

	class RepoBranchesAndTagsAdapter extends BaseAdapter {

		private final int padding_dp = 20;
		private final int padding_px;

		private List<String> list;

		RepoBranchesAndTagsAdapter() {

			float scale = CheckoutActivity.this.getResources()
					.getDisplayMetrics().density;
			padding_px = (int) (padding_dp * scale + 0.5f);

			List<String> branches = new ArrayList<String>();
			List<String> tags = new ArrayList<String>();
			GitHelper.readBranchesAndTags(branches, tags, getFilesDir() + "/"
					+ repo.getFolder());
			list = new ArrayList<String>(branches.size() + tags.size());
			list.addAll(branches);
			list.addAll(tags);
		}

		@Override
		public int getCount() {
			return list.size();
		}

		@Override
		public String getItem(int at) {
			return list.get(at);
		}

		@Override
		public long getItemId(int at) {
			return list.get(at).hashCode();
		}

		@Override
		public View getView(int at, View view, ViewGroup parent) {
			if (view == null) {
				view = new TextView(CheckoutActivity.this);
				view.setPadding(padding_px, padding_px, padding_px, padding_px);
				((TextView) view).setTextAppearance(CheckoutActivity.this,
						android.R.attr.textAppearanceMedium);
			}
			((TextView) view).setText(list.get(at));
			return view;
		}

	}

	@Override
	protected void onRestoreInstanceState(Bundle state) {
		super.onRestoreInstanceState(state);
		repo = (Repo) state.getSerializable(REPO);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable(REPO, repo);
	}

}
