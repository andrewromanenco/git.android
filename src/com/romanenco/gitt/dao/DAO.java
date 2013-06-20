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

package com.romanenco.gitt.dao;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

/**
 * DAO to manage local repos.
 * 
 * @author Andrew Romanenco
 * 
 */
public class DAO {

	private DAOHelper helper;
	private SQLiteDatabase database;

	public DAO(Context context) {
		helper = new DAOHelper(context);
	}

	public void open(boolean readWrite) throws SQLException {
		if (readWrite) {
			database = helper.getWritableDatabase();
		} else {
			database = helper.getReadableDatabase();
		}
	}

	public void close() {
		helper.close();
	}
	
	/**
	 * Checks if this repo already exists.
	 * 
	 * @param folder
	 * @return
	 */
	public boolean repoExists(String folder) {
		Cursor cursor = database.query(Repo.TABLE,
				new String []{Repo._ID},
				Repo.FOLDER + "=?",
				new String[] {folder},
				null,null, null);
		if (cursor != null) {
			boolean result = false;
			if (cursor.moveToFirst()) {
				result = true;
			}
			cursor.close();
			return result;
		}
		return false;
	}
	
	public void addRepo(Repo repo) {
		ContentValues values = new ContentValues();
		values.put(Repo.FOLDER, repo.getFolder());
		values.put(Repo.NAME, repo.getName());
		values.put(Repo.ADDRESS, repo.getAddress());
		values.put(Repo.SIZE, repo.getSize());
		values.put(Repo.USERNAME, repo.getUserName());
		values.put(Repo.STATE, repo.getState().name());
		database.insert(Repo.TABLE, null, values);
	}
	
	public List<Repo> listAll() {
		List<Repo> result = new ArrayList<Repo>();
		Cursor cursor = database.query(Repo.TABLE,
				new String[] {
					Repo._ID,
					Repo.FOLDER,
					Repo.NAME,
					Repo.ADDRESS,
					Repo.SIZE,
					Repo.USERNAME,
					Repo.STATE,
					Repo.ERROR}
			, null, null, null, null, Repo.NAME);
		if (cursor == null) return result;
		
		cursor.moveToFirst();
	    while (!cursor.isAfterLast()) {
	      Repo repo = readAsRepo(cursor);
	      result.add(repo);
	      cursor.moveToNext();
	    }
	    cursor.close();
	    return result;
	}

	private Repo readAsRepo(Cursor cursor) {
		Repo repo = new Repo();
		repo.setId(cursor.getInt(0));
		repo.setFolder(cursor.getString(1));
		repo.setName(cursor.getString(2));
		repo.setAddress(cursor.getString(3));
		repo.setSize(cursor.getInt(4));
		repo.setUserName(cursor.getString(5));
		repo.setState(Repo.State.valueOf(cursor.getString(6)));
		repo.setError(cursor.getString(7));
		return repo;
	}
	
	/**
	 * Partial update for a repo.
	 * 
	 * @param repo
	 */
	public void update(Repo repo) {
		ContentValues values = new ContentValues();
		values.put(Repo.SIZE, repo.getSize());
		values.put(Repo.STATE, repo.getState().name());
		values.put(Repo.ERROR, repo.getError());
		database.update(Repo.TABLE, values, Repo.FOLDER + " = ?", new String[]{repo.getFolder()});
	}

	public void delete(String folder) {
		database.delete(Repo.TABLE, Repo.FOLDER + " = ?", new String[]{folder});
	}

}
