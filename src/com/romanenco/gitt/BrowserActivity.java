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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.comparator.NameFileComparator;

import com.romanenco.gitt.dao.DAO;
import com.romanenco.gitt.dao.Repo;
import com.romanenco.gitt.git.GitHelper;
import com.romanenco.gitt.R;

import android.os.Bundle;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Browse repo checked out to local file system.
 * 
 * Use the same activity instance to browse source tree.
 * Open code viewer on file touch.
 * 
 * @author Andrew Romanenco
 *
 */
public class BrowserActivity extends ListActivity {
	
	static final String TAG = "Browser";
	
	public static final String REPO = "key_repo";
	public static final String PATH = "key_path";
	
	private static final String FILTER = "filterbar.visibility";
	
	/**
	 * We cache last viewed folder for each repo. With no persistence.
	 * Use this values to handle back button and return to the repo.
	 */
	private static Map<String, String> browseCache = new HashMap<String, String>();
	
	/**
	 * Current repo, sent to every next step
	 */
	private Repo current;
	
	/**
	 * Path to folder to show content from.
	 * It's relative to current repo's root.
	 */
	private String path;
	
	private FileListAdapter adapter;
	private View filterBar;
	private EditText filterText;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_browse);
		
		filterBar = findViewById(R.id.filter_bar);
		
		findViewById(R.id.filter_close).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						showFilterBar(false);
					}
				});
		
		filterText = (EditText)findViewById(R.id.filter_text);
		filterText.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {}
			
			@Override
			public void afterTextChanged(Editable s) {
				adapter.reFilter(s.toString());
			}

		});
		
		if (savedInstanceState == null) {
			current = (Repo)getIntent().getSerializableExtra(REPO);
			path = browseCache.get(current.getFolder());
			if (path == null) {
				path = ".";
			}
		} else {
			current = (Repo)savedInstanceState.getSerializable(REPO);
			path = savedInstanceState.getString(PATH);
			filterBar.setVisibility(savedInstanceState.getInt(FILTER));
		}
		
		updateTitleWithPath();
		adapter = new FileListAdapter(this, current, path);
		getListView().setAdapter(adapter);
	}
	
	private void updateTitleWithPath() {
		int index = path.lastIndexOf("/");
		if (index == -1) {
			//reading branch name in main thread...
			String branch = GitHelper.currentBranchName(GitHelper.getBaseRepoDir(this, current.getFolder()) + "/" + current.getFolder()); 
			this.setTitle("(" + branch + ")/.");
		} else {
			this.setTitle(".." + path.substring(index));
		}
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		if (adapter.isFolder(position)) {
			String step = adapter.getItem(position);
			if (step.equals("..")) { // browse up
				int index = path.lastIndexOf("/");
				path = path.substring(0, index);
			} else {
				path += "/" + step;
			}
			updateTitleWithPath();
			showFilterBar(false);
			adapter = new FileListAdapter(this, current, path);
			getListView().setAdapter(adapter);
		} else {
			Intent next = new Intent(this, CodeViewActivity.class);
			File file = new File(GitHelper.getBaseRepoDir(this, current.getFolder()), current.getFolder());
			file = new File(file, path);
			file = new File(file, adapter.getItem(position));
			next.putExtra(CodeViewActivity.FILE_KEY, file);
			startActivity(next);
		}
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable(REPO, current);
		outState.putSerializable(PATH, path);
		outState.putInt(FILTER, filterBar.getVisibility());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.browser, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.browser_menu_delete:
			deleteThisRepo();
			break;
		case R.id.browser_switch_branch:
			Intent co = new Intent(this, CheckoutActivity.class);
			co.putExtra(CheckoutActivity.REPO, current);
			startActivity(co);
			break;
		case R.id.browser_menu_pull:
			pullFromOrigin();
			break;
		case R.id.browser_filter:
			showFilterBar(true);
			break;
		case R.id.browser_repo_log:
			Intent logView = new Intent(this, LogViewActivity.class);
			logView.putExtra(LogViewActivity.REPO, current);
			startActivity(logView);
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	private void deleteThisRepo() {
		AlertDialog dlg = new AlertDialog.Builder(this)
			.setMessage(R.string.confirm_repo_delete)
			.setPositiveButton(getString(android.R.string.ok),
				new DialogInterface.OnClickListener() {
	        		public void onClick(DialogInterface dialog, int which) {
	        			DAO dao = new DAO(BrowserActivity.this);
	        			dao.open(true);
	        			dao.delete(current.getFolder());
	        			dao.close();
	        			Intent delete = new Intent(BrowserActivity.this, GitService.class);
	        			delete.putExtra(GitService.COMMAND, GitService.Command.Delete);
	        			delete.putExtra(GitService.REPO, current);
	        			startService(delete);
	        			Intent main = new Intent(BrowserActivity.this, MainActivity.class);
	        			main.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	        			startActivity(main);
	        		}
	     		}
				)
			.setNegativeButton(getString(android.R.string.cancel),
				null
				)
			.create();
		dlg.setCanceledOnTouchOutside(false);
		dlg.show();
	}
	
	/**
	 * When pull from origin there are several case.
	 * 1. We are in TAG (detached head): will inform user to
	 * checkout a branch first.
	 * 2. Authentication required: ask for password (no passwd save).
	 * 3. Anonymous user: just poll.
	 */
	private void pullFromOrigin() {
		if (current.getUserName() == null) {
			//no authentication required
			current.setState(Repo.State.Busy);
			DAO dao = new DAO(BrowserActivity.this);
			dao.open(true);
			dao.update(current);
			dao.close();
			Intent pull = new Intent(this, GitService.class);
			pull.putExtra(GitService.COMMAND, GitService.Command.Pull);
			pull.putExtra(GitService.REPO, current);
			startService(pull);
			Intent main = new Intent(this, MainActivity.class);
			main.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(main);
		} else {
			String req = getString(R.string.passwd_request, current.getUserName());
			final EditText passwd = new EditText(this);
			passwd.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
			AlertDialog dlg = new AlertDialog.Builder(this)
			.setMessage(req)
			.setPositiveButton(getString(android.R.string.ok),
				new DialogInterface.OnClickListener() {
	        		public void onClick(DialogInterface dialog, int which) {
	        			String password = passwd.getText().toString();
	        			if (TextUtils.isEmpty(password)) {
	        				pullFromOrigin();
	        			} else {
	        				current.setState(Repo.State.Busy);
	        				DAO dao = new DAO(BrowserActivity.this);
	        				dao.open(true);
	        				dao.update(current);
	        				dao.close();
	        				Intent pull = new Intent(BrowserActivity.this, GitService.class);
	        				pull.putExtra(GitService.COMMAND, GitService.Command.Pull);
	        				pull.putExtra(GitService.REPO, current);
	        				pull.putExtra(GitService.AUTH_PASSWD, password);
	        				startService(pull);
	        				Intent main = new Intent(BrowserActivity.this, MainActivity.class);
	        				main.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	        				startActivity(main);
	        			}
	        		}
	     		}
				)
			.setNegativeButton(getString(android.R.string.cancel),
				null
				)
			.create();
		dlg.setCanceledOnTouchOutside(false);
		dlg.setView(passwd);
		dlg.show();
		}
		
	}
	
	private void showFilterBar(boolean show) {
		if (show) {
			filterBar.setVisibility(View.VISIBLE);
			filterText.setText("");
		} else {
			filterBar.setVisibility(View.GONE);
			if (adapter != null) adapter.reFilter(null);
		}
	}

	/**
	 * List data source.
	 * Each item has name and type file/fodler.
	 * 
	 * @author Andrew Romanenco
	 *
	 */
	class FileListAdapter extends BaseAdapter {
		
		private Context context;
		private List<Item> allItems;
		private Map<String, String> fileSizes; //size is cached
		
		private List<Item> filteredList;

		/**
		 * @param context
		 * @param repo
		 * @param folder - relative to repo root.
		 */
		public FileListAdapter(Context context, Repo repo, String folder) {
			Log.d(TAG, "Reading: " + folder);
			this.context = context;
			browseCache.put(repo.getFolder(), folder);
			File repoDir = new File(GitHelper.getBaseRepoDir(context, current.getFolder()), repo.getFolder());
			File dir = new File(repoDir, folder);
			allItems = new ArrayList<Item>();
			if (dir.exists()) { // prevent crash if repo folder was deleted
				File[] files = dir.listFiles();
				Arrays.sort(files, NameFileComparator.NAME_COMPARATOR);
				fileSizes = new HashMap<String, String>();
				if (!folder.equals(".")) {
					allItems.add(new Item("..", true));
				}
				for (File f: files) {
					if (".git".equals(f.getName())) continue;
					boolean isDir = f.isDirectory();
					allItems.add(new Item(f.getName(), isDir));
					if (!isDir) {
						fileSizes.put(f.getName(), Utils.formatFileSize(BrowserActivity.this, f.length()));
					}
				}
			}
			filteredList = allItems;
		}
		
		private void reFilter(String pattern) {
			if (TextUtils.isEmpty(pattern)) {
				filteredList = allItems;
			} else {
				filteredList = new ArrayList<Item>();
				if ((allItems.size() > 0)&&allItems.get(0).name.equals("..")) {
					filteredList.add(allItems.get(0));
				}
				pattern = pattern.toLowerCase();
				for (Item item: allItems) {
					if (item.name.toLowerCase().indexOf(pattern) > -1) {
						filteredList.add(item);
					}
				}
			}
			this.notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			return filteredList.size();
		}

		@Override
		public String getItem(int position) {
			return filteredList.get(position).name;
		}
		
		public String getItemSize(int position) {
			return fileSizes.get(position);
		}

		@Override
		public long getItemId(int position) {
			return filteredList.get(position).name.hashCode();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Item item = filteredList.get(position);
			if (convertView == null) {
				LayoutInflater inflater = LayoutInflater.from(context);
				convertView = inflater.inflate(R.layout.item_row, parent, false);
			}
			TextView name = (TextView)convertView.findViewById(R.id.item_name);
			TextView size = (TextView)convertView.findViewById(R.id.item_size);
			ImageView type = (ImageView)convertView.findViewById(R.id.item_icon);
			name.setText(item.name);
			if (item.isFolder) {
				type.setVisibility(View.VISIBLE);
				size.setText("");
			} else {
				type.setVisibility(View.INVISIBLE);
				size.setText(fileSizes.get(item.name));
			}
			return convertView;
		}
		
		public boolean isFolder(int index) {
			return filteredList.get(index).isFolder;
		}
		
		private class Item {
			String name;
			boolean isFolder;
			public Item(String name, boolean isFolder) {
				this.name = name;
				this.isFolder = isFolder;
			}
		}
		
	}
	
}
