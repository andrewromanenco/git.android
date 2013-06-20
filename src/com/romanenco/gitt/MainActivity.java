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

import java.util.List;

import com.romanenco.gitt.dao.DAO;
import com.romanenco.gitt.dao.Repo;
import com.romanenco.gitt.GitService.ProgressBean;
import com.romanenco.gitt.R;

import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Entry point for GitViewer App.
 * 
 * Display all repos.
 * Listen for broadcast to refresh the list for any changes.
 * (clone/pull) Listen for broadcast to update progress.
 * 
 * @author Andrew Romanenco
 * 
 */
public class MainActivity extends ListActivity {

	private static final String TAG = "Main";
	private RepoListAdapter adapter;

	/**
	 * Listen for repo state changes in GitService
	 */
	private BroadcastReceiver updateListener = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (adapter != null) {
				adapter.notifyDataSetChanged();
			}
		}

	};

	/**
	 * Listen GitService progress events Update specific cell elements according
	 * to repo id.
	 */
	private BroadcastReceiver progressListener = new BroadcastReceiver() {

		int modCount = -1;

		@Override
		public void onReceive(Context context, Intent intent) {
			ProgressBean data = (ProgressBean) intent
					.getSerializableExtra(GitService.BROADCAST_PROGRESS_DATA);
			if (data.sequence > modCount) {
				View[] ui = adapter.getItemProgressBar(data.receiverId);
				if (ui != null) {
					((ProgressBar) ui[0]).setIndeterminate(false);
					((ProgressBar) ui[0]).setProgress(data.progress);
					((TextView) ui[1]).setText(data.task);
				}
				modCount = data.sequence;
			}

		}

	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (adapter == null) {
			adapter = new RepoListAdapter(this); // load in UI thread
			this.setListAdapter(adapter);
		} else {
			adapter.notifyDataSetChanged();
		}
		if (adapter.getCount() == 0) {
			getListView().setVisibility(View.GONE);
			findViewById(R.id.textNoRepos).setVisibility(View.VISIBLE);
		} else {
			findViewById(R.id.textNoRepos).setVisibility(View.GONE);
			getListView().setVisibility(View.VISIBLE);
		}

		IntentFilter filter = new IntentFilter();
		filter.addAction(GitService.BROADCAST_REFRESH);
		registerReceiver(updateListener, filter);
		IntentFilter progress = new IntentFilter();
		progress.addAction(GitService.BROADCAST_PROGRESS);
		registerReceiver(progressListener, progress);
	};

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(updateListener);
		unregisterReceiver(progressListener);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.main_action_clone:
			Intent next = new Intent(this, CloneActivity.class);
			startActivity(next);
			break;
		case R.id.main_action_about:
			Intent about = new Intent(this, AboutActivity.class);
			startActivity(about);
			break;
		case R.id.main_action_trace:
			Intent trace = new Intent(this, TraceActivity.class);
			startActivity(trace);
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Repo repo = adapter.getItem(position);
		if (repo.getState() == Repo.State.Error) {
			Intent edit = new Intent(this, CloneActivity.class);
			edit.putExtra(CloneActivity.REPO, repo);
			startActivity(edit);
		} else if (repo.getState() == Repo.State.Local) {
			Intent browser = new Intent(this, BrowserActivity.class);
			browser.putExtra(BrowserActivity.REPO, repo);
			startActivity(browser);
		}
	}

	/**
	 * Data source for repos list Handle progress vies to be updated by
	 * GitService.
	 * 
	 * @author Andrew Romanenco
	 * 
	 */
	class RepoListAdapter extends BaseAdapter {

		private final int cBlack = Color.parseColor("#000000"); // yes
																// hard-coded :(
		private final int cGray = Color.parseColor("#CCCCCC");

		private Context context;
		private List<Repo> list;

		private View[] cacheViews;
		private String cacheValue;

		public RepoListAdapter(Context context) {
			this.context = context;
			loadData();
		}

		private void resetCache() {
			cacheValue = null;
			cacheViews = null;
		}

		private void loadData() {
			resetCache();
			DAO dao = new DAO(context);
			dao.open(false);
			list = dao.listAll();
			dao.close();
		}

		@Override
		public int getCount() {
			return list.size();
		}

		@Override
		public Repo getItem(int index) {
			return list.get(index);
		}

		@Override
		public long getItemId(int index) {
			return list.get(index).getId();
		}

		/**
		 * Return progress bar and label for update by GitService (via bcast).
		 * Cache views in case pf multiple progress events.
		 * 
		 * @param folder
		 * @return
		 */
		public View[] getItemProgressBar(String folder) {
			if (!folder.equals(cacheValue)) {
				ListView listView = getListView();
				for (int i = 0; i < listView.getChildCount(); i++) {
					if (folder.equals(listView.getChildAt(i).getTag())) {
						View cell = listView.getChildAt(i);
						cacheViews = new View[] {
								cell.findViewById(R.id.row_progress_bar),
								cell.findViewById(R.id.row_progress_task) };
						cacheValue = folder;
						break;
					}
				}
			}
			return cacheViews;
		}

		@Override
		public View getView(int index, View view, ViewGroup group) {
			resetCache();
			Repo repo = list.get(index);
			if (view == null) {
				LayoutInflater inflater = LayoutInflater.from(context);
				view = inflater.inflate(R.layout.repolist_row, group, false);
			}
			TextView name = (TextView) view.findViewById(R.id.row_repo_name);
			TextView git = (TextView) view.findViewById(R.id.row_repo_git);
			TextView size = (TextView) view.findViewById(R.id.row_repo_size);
			View progress = view.findViewById(R.id.row_progress);
			ProgressBar progressBar = ((ProgressBar) view
					.findViewById(R.id.row_progress_bar));
			progressBar.setIndeterminate(true);
			TextView progressText = ((TextView) view
					.findViewById(R.id.row_progress_task));
			name.setText(repo.getName());
			git.setText(repo.getAddress());

			switch (repo.getState()) {
			case New:
			case Busy:
				progress.setVisibility(View.VISIBLE);
				progressBar.setVisibility(View.VISIBLE);
				progressText.setVisibility(View.VISIBLE);
				progressText.setText("");
				progressBar.setProgress(0);
				name.setTextColor(cGray);
				size.setText("");
				break;
			case Local:
				size.setText(Utils.formatFileSize(MainActivity.this,
						repo.getSize()));
				progress.setVisibility(View.GONE);
				name.setTextColor(cBlack);
				break;
			case Error:
				size.setText("");
				progress.setVisibility(View.VISIBLE);
				progressBar.setVisibility(View.GONE);
				progressText.setVisibility(View.VISIBLE);
				progressText.setText(repo.getError());
				name.setTextColor(cGray);
				break;
			}

			view.setTag(repo.getFolder());

			return view;
		}

		@Override
		public void notifyDataSetChanged() {
			Log.d(TAG, "Refreshing adapter");
			loadData();
			super.notifyDataSetChanged();
		}

	}

}
