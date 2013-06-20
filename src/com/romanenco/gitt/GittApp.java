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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import com.romanenco.gitt.dao.DAO;
import com.romanenco.gitt.dao.Repo;

import android.app.Application;
import android.content.Intent;

/**
 * Clean up application on restart.
 * 
 * All NEW repos are resubmitted for checkout.
 * All BUSY repos are released.
 * 
 * Handle last error stack trace for entire app.
 * 
 * @author Andrew Romanenco
 *
 */
public class GittApp extends Application {
	
	private static String lastErrorTrace;

	@Override
	public void onCreate() {
		super.onCreate();
		DAO dao = new DAO(this);
		dao.open(true);
		List<Repo> repos = dao.listAll();
		for (Repo repo: repos) {
			if (repo.getState() == Repo.State.New) {
				Intent git = new Intent(this, GitService.class);
				git.putExtra(GitService.COMMAND, GitService.Command.Clone);
				git.putExtra(GitService.REPO, repo);
				startService(git);
			} else if (repo.getState() == Repo.State.Busy) {
				repo.setState(Repo.State.Local);
				dao.update(repo);
			}
		}
		dao.close();
	}
	
	public static synchronized String saveErrorTrace(Exception ex) {
		StringWriter trace = new StringWriter();
		ex.printStackTrace(new PrintWriter(trace));
		lastErrorTrace = trace.toString();
		return lastErrorTrace;
	}
	
	public static synchronized String getLastErrorTrace() {
		return lastErrorTrace;
	}
	
}
