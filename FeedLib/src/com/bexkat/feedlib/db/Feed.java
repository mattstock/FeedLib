/*
 * Copyright 2012 Matthew Stock - http://www.bexkat.com/
 * Adapted from FeedGoal copyright 2010-2011 Mathieu Favez - http://mfavez.com
 * 
 * This is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FeedGoal.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.bexkat.feedlib.db;

import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;


import android.content.ContentValues;

public class Feed {
	public static final String TYPE_RDF = "rdf";
	public static final String TYPE_RSS = "rss";
	public static final String TYPE_ATOM = "atom";
	
	private long mId = -1;
	private URL mURL;
	private URL mHomePage;
	private String mTitle;
	private String mDescription;
	private String mType;
	private Date mRefresh = null;
	private boolean mEnabled = true;
	private List<Item> mItems;
	
	public Feed() {
		mItems = new ArrayList<Item>();
	}
		
	public void setId(long id) {
		this.mId = id;
	}
	
	public long getId() {
		return mId;
	}
	
	public void setURL(URL url) {
		this.mURL = url;
	}

	public URL getURL() {
		return this.mURL;
	}
	
	public void setHomePage(URL homepage) {
		this.mHomePage = homepage;
	}

	public URL getHomePage() {
		return this.mHomePage;
	}
	
	public void setTitle(String title) {
		this.mTitle = title;
	}
	
	public String getTitle() {
		return this.mTitle;
	}

	public void setDescription(String desc) {
		this.mDescription = desc;
	}
	
	public String getDescription() {
		return this.mDescription;
	}

	public void setType(String type) {
		this.mType = type;
	}

	public String getType() {
		return mType;
	}
	
	public void setRefresh(Date refresh) {
		mRefresh = refresh;
	}
	
	public Date getRefresh() {
		return mRefresh;
	}

	public void enable() {
		this.mEnabled = true;
	}
	
	public void disable() {
		this.mEnabled = false;
	}
	
	public void setEnabled(int state) {
		if (state == DatabaseHelper.OFF)
			this.mEnabled = false;
		else
			this.mEnabled = true;
	}
	
	public boolean isEnabled() {
		return this.mEnabled;
	}
	
	public void addItem(Item item) {
		this.mItems.add(item);
	}
	
	public void setItems(List<Item> items){
		this.mItems = items;
	}
	
	public List<Item> getItems() {
		return this.mItems;
	}

	public ContentValues toContentValues() {
		ContentValues values = new ContentValues();
		if (mTitle == null)
			values.putNull(FeedTable.COLUMN_TITLE);
		else
			values.put(FeedTable.COLUMN_TITLE, mTitle);
		values.put(FeedTable.COLUMN_ENABLE, mEnabled);
		values.put(FeedTable.COLUMN_URL, mURL.toString());
		if (mHomePage == null)
			values.putNull(FeedTable.COLUMN_HOMEPAGE);
		else
			values.put(FeedTable.COLUMN_HOMEPAGE, mHomePage.toString());
		if (mDescription == null)
			values.putNull(FeedTable.COLUMN_DESCRIPTION);
		else
			values.put(FeedTable.COLUMN_DESCRIPTION, mDescription);
		if (mType == null)
			values.putNull(FeedTable.COLUMN_TYPE);
		else
			values.put(FeedTable.COLUMN_TYPE, mType);
		if (mRefresh == null)
			values.putNull(FeedTable.COLUMN_REFRESH);
		else
			values.put(FeedTable.COLUMN_REFRESH, mRefresh.getTime());
		
		return values;
	}
	
	public String toString() {
		String s = "{ID=" + this.mId + " URL=" + this.mURL.toString() + " homepage=" + this.mHomePage.toString() + " title=" + this.mTitle + " type=" + this.mType + " update=" + this.mRefresh.toString() + " enabled=" + this.mEnabled;
		s = s + " items={";
		Iterator<Item> iterator = this.mItems.iterator();
		while (iterator.hasNext()) {
			s = s + iterator.next().toString();
		}
		s = s + "}}";
		return s;
	}

}
