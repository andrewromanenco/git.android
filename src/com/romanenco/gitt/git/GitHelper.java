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

package com.romanenco.gitt.git;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.ListTagCommand;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.errors.DetachedHeadException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.errors.NotSupportedException;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import android.util.Log;

import com.romanenco.gitt.GittApp;

/**
 * Common GIT operations based on jgit.
 * 
 * @author Andrew Romanenco
 * 
 */
public class GitHelper {
	
	private static final String TAG = "GitHelper";

	/**
	 * Clone remote HTTP/S repo to local file system.
	 * 
	 * @param url
	 * @param localPath
	 * @param user
	 * @param password
	 * @throws GitError
	 */
	public static void clone(String url, String localPath, String user,
			String password, ProgressMonitor monitor) throws GitError {
		Log.d(TAG, "Cloning: " + url);
		CloneCommand clone = Git.cloneRepository();
		clone.setURI(url);
		clone.setDirectory(new File(localPath));
		if ((user != null) && (password != null)) {
			UsernamePasswordCredentialsProvider access = new UsernamePasswordCredentialsProvider(
					user, password);
			clone.setCredentialsProvider(access);
		}
		if (monitor != null) {
			clone.setProgressMonitor(monitor);
		}
		
		try {
			FileUtils.deleteDirectory(new File(localPath));
			clone.call();
			return;
		} catch (InvalidRemoteException e) {
			Log.e(TAG, "InvalidRemote", e);
			GittApp.saveErrorTrace(e);
			throw new NotGitRepoError();
		} catch (TransportException e) {
			String trace = GittApp.saveErrorTrace(e);
			if (trace.indexOf("not authorized") != -1) {
				Log.e(TAG, "Auth", e);
				throw new AuthFailError();
			}
			Log.e(TAG, "Transport", e);
			throw new ConnectionError();
		} catch (GitAPIException e) {
			Log.e(TAG, "GitApi", e);
			GittApp.saveErrorTrace(e);
		} catch (IOException e) {
			Log.e(TAG, "IO", e);
			GittApp.saveErrorTrace(e);
		} catch (JGitInternalException e) {
			Log.e(TAG, "GitInternal", e);
			GittApp.saveErrorTrace(e);
			if (e.getCause() instanceof NotSupportedException) {
				throw new ConnectionError();
			} else {
				throw new GitError();
			}
		}
		throw new GitError();
	}
	
	/**
	 * Folder size in bytes.
	 * 
	 * @param localPath
	 * @return
	 */
	public static long getRepoSize(String localPath) {
		return FileUtils.sizeOfDirectory(new File(localPath));
	}
	
	
	/**
	 * Current branch/tag name.
	 * 
	 * @param path
	 * @return
	 */
	public static String currentBranchName(String localPath) {
		try {
			Git git = Git.open(new File(localPath));
		
			String name = git.getRepository().getBranch();
			
			if (name.length() == 40) {
				//this could be tag's ref name
				ListTagCommand getTagsCmd = git.tagList();
				List<Ref> tags = getTagsCmd.call();
				for (Ref r: tags) {
					if (name.equals(r.getObjectId().getName())) {
						name = r.getName();
						break;
					}
				}
			}
			return nameReFormat(name);
		} catch (IOException e) {
			GittApp.saveErrorTrace(e);
		} catch (GitAPIException e) {
			GittApp.saveErrorTrace(e);
		}
		return null;
	}
	
	/**
	 * List all branches and tags in a repo.
	 * 
	 * @param branches
	 * @param tags
	 * @param path
	 */
	public static void readBranchesAndTags(List<String> branches, List<String> tags, String localPath) {
		if ((branches == null)||(tags == null)) return;
		try {
			Git git = Git.open(new File(localPath));
			
			ListTagCommand getTagsCmd = git.tagList();
			List<Ref> repoTags = getTagsCmd.call();
			for (Ref r: repoTags) {
				tags.add(nameReFormat(r.getName()));
			}
			
			ListBranchCommand getBranchesCmd = git.branchList();
			List<Ref> repoBranches = getBranchesCmd.call();
			for (Ref r: repoBranches) {
				branches.add(nameReFormat(r.getName()));
			}

		} catch (Exception e) {
			GittApp.saveErrorTrace(e);
		}
	}
	
	/**
	 * Format name by taking last part of a ref.
	 * 
	 * @param name
	 * @return
	 */
	private static String nameReFormat(String name) {
		int index = name.lastIndexOf("/");
		if (index > -1) {
			return name.substring(++index);
		} else {
			return name;
		}
	}
	
	/**
	 * Delete local repo.
	 * 
	 * @param localPath
	 * @throws IOException
	 */
	public static void deleteRepo(String localPath) throws IOException {
		FileUtils.deleteDirectory(new File(localPath));
	} 
	
	/**
	 * Checkout specific branch or tag.
	 * 
	 * @param localPath
	 * @param name
	 * @throws GitError
	 */
	public static void checkout(String localPath, String name) throws GitError {
		try {
			Git git = Git.open(new File(localPath));
			CheckoutCommand co = git.checkout();
			co.setName(name);
			co.call();
		} catch (Exception e) {
			GittApp.saveErrorTrace(e);
			throw new GitError();
		}
	}
	
	/**
	 * Pull repo.
	 * 
	 * @param localPath
	 * @param user
	 * @param password
	 * @param pm
	 * @throws GitError
	 */
	public static void pull(String localPath, String user, String password, ProgressMonitor pm) throws GitError {
		try {
			Git git = Git.open(new File(localPath));
			PullCommand pull = git.pull();
			pull.setProgressMonitor(pm);
			if ((user != null) && (password != null)) {
				UsernamePasswordCredentialsProvider access = new UsernamePasswordCredentialsProvider(
						user, password);
				pull.setCredentialsProvider(access);
			}
			pull.call();
			return;
		} catch (DetachedHeadException e) {
			Log.e(TAG, "Detached head", e);
			GittApp.saveErrorTrace(e);
			throw new NoHeadError();
		} catch (InvalidRemoteException e) {
			Log.e(TAG, "InvalidRemote", e);
			GittApp.saveErrorTrace(e);
			throw new NotGitRepoError();
		} catch (TransportException e) {
			String trace = GittApp.saveErrorTrace(e);
			if (trace.indexOf("not authorized") != -1) {
				Log.e(TAG, "Auth", e);
				throw new AuthFailError();
			}
			Log.e(TAG, "Transport", e);
			throw new ConnectionError();
		} catch (GitAPIException e) {
			Log.e(TAG, "GitApi", e);
			GittApp.saveErrorTrace(e);
		} catch (IOException e) {
			Log.e(TAG, "IO", e);
			GittApp.saveErrorTrace(e);
		} catch (JGitInternalException e) {
			Log.e(TAG, "GitInternal", e);
			GittApp.saveErrorTrace(e);
			if (e.getCause() instanceof NotSupportedException) {
				throw new ConnectionError();
			}
		}
		throw new GitError();
	}

}
