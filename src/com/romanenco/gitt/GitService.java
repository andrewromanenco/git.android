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
import java.io.Serializable;

import org.eclipse.jgit.lib.ProgressMonitor;

import com.romanenco.gitt.dao.DAO;
import com.romanenco.gitt.dao.Repo;
import com.romanenco.gitt.git.AuthFailError;
import com.romanenco.gitt.git.ConnectionError;
import com.romanenco.gitt.git.GitError;
import com.romanenco.gitt.git.GitHelper;
import com.romanenco.gitt.git.NoHeadError;
import com.romanenco.gitt.git.NotGitRepoError;

import android.app.IntentService;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

/**
 * Service to handle GIT repo operations in background. Updates UI with
 * broadcast with progress.
 * 
 * @author Andrew Romanenco
 * 
 */
public class GitService extends IntentService {

	static final String TAG = "GitService";

	/**
	 * Supported operations
	 */
	public enum Command {
		Clone, Checkout, Pull, Delete
	}

	public static final String BROADCAST_REFRESH = "com.romanenco.gitviewer.GitService.REFRESH";
	public static final String BROADCAST_PROGRESS = "com.romanenco.gitviewer.GitService.PROGRESS";

	/**
	 * Every intent must have command
	 */
	public static final String COMMAND = "key_command";

	/**
	 * Related repo
	 */
	public static final String REPO = "key_repo";

	/**
	 * When command is clone/pull and auth is required - this is user's
	 * password. User name is in repo info.
	 */
	public static final String AUTH_PASSWD = "key_password";

	/**
	 * Progress bean sent with broadcast
	 */
	public static final String BROADCAST_PROGRESS_DATA = "key_progress_data";

	/**
	 * When command is checkout: name of branch/tag
	 */
	public static final String SWITCH_TO = "key_co";

	/**
	 * Seq. number for broadcast ordering in MainActivity
	 */
	private static int sequenceNumber = 0;

	private DAO dao;
	private Handler handler;

	public GitService() {
		super("GitClone");
	}

	@Override
	public void onCreate() {
		super.onCreate();
		dao = new DAO(this);
		dao.open(true);
		handler = new Handler();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		dao.close();
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Command cmd = (Command) intent.getSerializableExtra(COMMAND);
		Log.d(TAG, "Next command: " + cmd);
		switch (cmd) {
		case Clone:
			clone(intent);
			break;
		case Checkout:
			checkout(intent);
			break;
		case Pull:
			pull(intent);
			break;
		case Delete:
			delete(intent);
			break;
		}
	}

	/**
	 * Expects repo to be already saved in DB as NEW.
	 * Status of repo is updated when done (with or without error).
	 * 
	 * @param intent
	 */
	private void clone(Intent intent) {
		final Repo repo = (Repo) intent.getSerializableExtra(REPO);
		String passwd = intent.getStringExtra(GitService.AUTH_PASSWD);

		Log.d(TAG, "Starting processing: " + repo.getName());

		String path = this.getFilesDir().getPath() + "/" + repo.getFolder();
		ProgressMonitor pm = new Progress(repo.getFolder());

		try {
			GitHelper.clone(repo.getAddress(), path, repo.getUserName(),
					passwd, pm);
			long size = GitHelper.getRepoSize(path);
			repo.setSize(size);
			repo.setState(Repo.State.Local);
			repo.setError("");
			Log.e(TAG, "DONE");
		} catch (ConnectionError e) {
			Log.e(TAG, "Git clone connect error");
			repo.setState(Repo.State.Error);
			repo.setError(getString(R.string.git_error_connect));
		} catch (NotGitRepoError e) {
			Log.e(TAG, "Not a git repo");
			repo.setState(Repo.State.Error);
			// issue-12
			String lcAddress = repo.getAddress().toLowerCase();
			if (!lcAddress.endsWith(".git")) {
				repo.setError(getString(R.string.git_error_not_git_guess));
			} else {
				repo.setError(getString(R.string.git_error_not_git));
			}
		} catch (AuthFailError e) {
			Log.e(TAG, "Not authorised");
			repo.setState(Repo.State.Error);
			repo.setError(getString(R.string.git_error_auth));
		} catch (GitError e) {
			Log.e(TAG, "Git clone error");
			repo.setState(Repo.State.Error);
			repo.setError(getString(R.string.git_error_generic));
		}
		dao.update(repo);
		notifyRepoList();
	}

	/**
	 * Delete repo from file system.
	 * Caller must delete it from the data storage.
	 * 
	 * @param intent
	 */
	private void delete(Intent intent) {
		Repo repo = (Repo) intent.getSerializableExtra(REPO);
		String path = this.getFilesDir().getPath() + "/" + repo.getFolder();
		try {
			GitHelper.deleteRepo(path);
		} catch (IOException e) {
			// no need to handle
			Log.e(TAG, "Delete error", e);
		}
	}

	/**
	 * Checkout to local branch/tag.
	 * In case of an error, just notify the user.
	 * 
	 * @param intent
	 */
	private void checkout(Intent intent) {
		Repo repo = (Repo) intent.getSerializableExtra(REPO);
		String path = this.getFilesDir().getPath() + "/" + repo.getFolder();
		String branchOrTag = intent.getStringExtra(SWITCH_TO);
		try {
			GitHelper.checkout(path, branchOrTag);
			long size = GitHelper.getRepoSize(path);
			repo.setSize(size);
			repo.setState(Repo.State.Local);
			repo.setError("");
			dao.update(repo);
			toast(getString(R.string.msg_checking_out_done));
		} catch (GitError e) {
			toast(getString(R.string.msg_checking_out_failes));
		}
		notifyRepoList();
	}

	/**
	 * Pull from origin.
	 * In case of an error, just notify the user.
	 * 
	 * @param intent
	 */
	private void pull(Intent intent) {
		Repo repo = (Repo) intent.getSerializableExtra(REPO);
		String path = this.getFilesDir().getPath() + "/" + repo.getFolder();
		ProgressMonitor pm = new Progress(repo.getFolder());
		String passwd = intent.getStringExtra(AUTH_PASSWD);

		try {
			GitHelper.pull(path, repo.getUserName(), passwd, pm);
			toast(getString(R.string.msg_pull_done));
		} catch (NoHeadError e) {
			toast(getString(R.string.msg_pull_failed) + "\n"
					+ getString(R.string.git_error_head));
		} catch (AuthFailError e) {
			toast(getString(R.string.msg_pull_failed) + "\n"
					+ getString(R.string.git_error_auth));
		} catch (ConnectionError e) {
			toast(getString(R.string.msg_pull_failed) + "\n"
					+ getString(R.string.git_error_connect));
		} catch (GitError e) {
			toast(getString(R.string.msg_pull_failed) + "\n"
					+ getString(R.string.git_error_generic));
		}
		Log.d(TAG, "Pull done");
		repo.setState(Repo.State.Local);
		dao.update(repo);
		notifyRepoList();
	}

	private void toast(final String message) {
		handler.post(new Runnable() {

			@Override
			public void run() {
				Toast.makeText(GitService.this, message, Toast.LENGTH_LONG)
						.show();
			}
		});
	}

	/**
	 * Generic event sent to main ui, when processing of a repo is done.
	 */
	private void notifyRepoList() {
		Intent notify = new Intent();
		notify.setAction(BROADCAST_REFRESH);
		sendBroadcast(notify);
	}

	/**
	 * Progress event has operation name and current percentage.
	 * 
	 * @param progress
	 */
	private void notifyProgress(ProgressBean progress) {
		Intent notify = new Intent();
		notify.setAction(BROADCAST_PROGRESS);
		notify.putExtra(BROADCAST_PROGRESS_DATA, progress);
		sendBroadcast(notify);
	}

	/**
	 * Progress monitor.
	 * Sends broadcasts for every 5%.
	 * 
	 * @author Andrew Romanenco
	 * 
	 */
	class Progress implements ProgressMonitor {

		private int lastProgress = 0;

		private String receiverId;
		private String currentTask;
		private int totalUnits;
		private int currentUnits;

		Progress(String receiverId) {
			this.receiverId = receiverId;
		}

		@Override
		public void beginTask(String task, int units) {
			currentTask = task;
			totalUnits = units;
			currentUnits = 0;
			lastProgress = 0;
			ProgressBean progress = new ProgressBean(receiverId, task, 0,
					sequenceNumber++);
			notifyProgress(progress);
		}

		@Override
		public void endTask() {

		}

		@Override
		public boolean isCancelled() {
			return false;
		}

		@Override
		public void start(int units) {
		}

		@Override
		public void update(int units) {
			if (totalUnits == 0)
				return;// we don't always have this info
			currentUnits += units;
			int p = (int) ((float) currentUnits / totalUnits * 100.0);
			if ((p >= (lastProgress + 5)) || (p == 100)) {
				ProgressBean progress = new ProgressBean(receiverId,
						currentTask, p, sequenceNumber++);
				lastProgress = p;
				notifyProgress(progress);
			}
		}

	}

	/**
	 * Progress bean sent with broadcast
	 * 
	 * @author Andrew Romanenco
	 * 
	 */
	public static class ProgressBean implements Serializable {

		private static final long serialVersionUID = -4280431521919842892L;

		public String receiverId;
		public String task;
		public int progress;
		public int sequence;

		public ProgressBean() {

		}

		public ProgressBean(String receiverId, String task, int progress,
				int sequence) {
			this.receiverId = receiverId;
			this.task = task;
			this.progress = progress;
			this.sequence = sequence;
		}
	}

}
