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

import java.io.IOException;

import com.romanenco.gitt.dao.DAO;
import com.romanenco.gitt.dao.Repo;
import com.romanenco.gitt.git.GitHelper;
import com.romanenco.gitt.R;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

/**
 * Handle user input for new repo creation and editing failed one.
 * 
 * @author Andrew Romanenco
 * 
 */
public class CloneActivity extends Activity {

	public static final String REPO = "key_repo";

	/**
	 * Not null if this is edit request
	 */
	private Repo current; // null for new, not-null

	private EditText nameView;
	private EditText addressView;
	private EditText userNameView;
	private EditText passwdView;
	private Button deleteButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_clone);

		nameView = (EditText) findViewById(R.id.clone_name);
		addressView = (EditText) findViewById(R.id.clone_address);
		userNameView = (EditText) findViewById(R.id.clone_username);
		passwdView = (EditText) findViewById(R.id.clone_passwd);
		deleteButton = (Button) findViewById(R.id.clone_delete);

		current = (Repo) getIntent().getSerializableExtra(REPO);
		if (current != null) {
			nameView.setText(current.getName());
			addressView.setText(current.getAddress());
			userNameView.setText(current.getUserName());
			deleteButton.setVisibility(View.VISIBLE);
		}

		findViewById(R.id.clone_btn_clone).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						cloneGitRepo();
					}
				});
		findViewById(R.id.clone_delete).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						deleteGitRepo();
					}
				});

		setupEditTextCleaners();
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		current = (Repo) savedInstanceState.getSerializable(REPO);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable(REPO, current);
	}

	/**
	 * - On repo update, just remove and create new one.
	 * - if authentication is required, both username and password must be provided.
	 * 
	 */
	private void cloneGitRepo() {
		nameView.setError(null);
		addressView.setError(null);

		String name = nameView.getText().toString();
		String address = addressView.getText().toString();
		String username = userNameView.getText().toString();
		String passwd = passwdView.getText().toString();

		if (TextUtils.isEmpty(name)) {
			nameView.setError(getString(R.string.error_empty));
			nameView.requestFocus();
			return;
		}
		if (TextUtils.isEmpty(address)) {
			addressView.setError(getString(R.string.error_empty));
			addressView.requestFocus();
			return;
		}

		if (!TextUtils.isEmpty(username) && TextUtils.isEmpty(passwd)) {
			passwdView.setError(getString(R.string.error_empty));
			passwdView.requestFocus();
			return;
		}
		if (TextUtils.isEmpty(username) && !TextUtils.isEmpty(passwd)) {
			userNameView.setError(getString(R.string.error_empty));
			userNameView.requestFocus();
			return;
		}
		
		if (!address.startsWith("http://") && (!address.startsWith("https://"))) {
			addressView.setError(getString(R.string.error_http));
			addressView.requestFocus();
			return;
		}

		DAO dao = new DAO(this);
		dao.open(true);
		if (current != null) { // kill repo for edit
			dao.delete(current.getFolder());
		}

		if (dao.repoExists(Utils.makeFolderName(name))) {
			nameView.setError(getString(R.string.error_duplicate_repo_name));
			nameView.requestFocus();
			return;
		}

		Intent clone = new Intent(this, GitService.class);
		clone.putExtra(GitService.COMMAND, GitService.Command.Clone);
		Repo repo = new Repo();
		repo.setState(Repo.State.New);
		repo.setName(name);
		repo.setAddress(address);
		repo.setFolder(Utils.makeFolderName(repo.getName()));
		clone.putExtra(GitService.REPO, repo);
		if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(passwd)) {
			repo.setUserName(username);
			clone.putExtra(GitService.AUTH_PASSWD, passwd);
		}

		dao.addRepo(repo);
		dao.close();

		startService(clone);
		finish();
	}

	/**
	 * Delete without asking confirmation as this is for wrong repo anyway.
	 */
	private void deleteGitRepo() {
		DAO dao = new DAO(this);
		dao.open(true);
		dao.delete(current.getFolder());
		try {
			GitHelper.deleteRepo(getFilesDir() + "/" + current.getFolder());
		} catch (IOException e) {
		} // no need to handle
		dao.close();
		finish();
	}

	private void setupEditTextCleaners() {
		findViewById(R.id.clone_clear_name).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						nameView.setText("");

					}
				});
		findViewById(R.id.clone_clear_address).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						addressView.setText("");

					}
				});
		findViewById(R.id.clone_clear_username).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						userNameView.setText("");

					}
				});
		findViewById(R.id.clone_clear_passwd).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						passwdView.setText("");

					}
				});
	}

}
