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

import java.io.Serializable;

/**
 * Git repo data object.
 * 
 * @author Andrew Romanenco
 * 
 */
public class Repo implements Serializable {

	private static final long serialVersionUID = 8753374579416116856L;

	public static final String TABLE = "repos";
	
	public static final String _ID = "_ID";
	public static final String FOLDER = "FOLDER";
	public static final String NAME = "NAME";
	public static final String ADDRESS = "ADDRESS";
	public static final String SIZE = "SIZE";
	public static final String USERNAME = "USERNAME";
	public static final String STATE = "STATE";
	public static final String ERROR = "ERROR";
	
	/**
	 * Repo lifecycle
	 * 
	 * @author Andrew Romanenco
	 *
	 */
	public enum State {
		New,		// has not been cloned yet
		Error,		// clone error
		Local,		// ready
		Busy		// busy by pull or checkout
	}
	
	public static final String CREATE_SQL = "CREATE TABLE "
			+ TABLE 
			+ " ("
			+ " _ID integer primary key autoincrement,"
			+ " FOLDER text,"
			+ " NAME text,"
			+ " ADDRESS text,"
			+ " SIZE integer,"
			+ " USERNAME text null,"
			+ " STATE state,"
			+ " ERROR text null"
			+ ")";

	private int id;
	private String folder;
	private String name;
	private String address;
	private long size;
	private String userName;
	private State state;
	private String error;
	

	public String getFolder() {
		return folder;
	}

	public void setFolder(String folder) {
		this.folder = folder;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

}
