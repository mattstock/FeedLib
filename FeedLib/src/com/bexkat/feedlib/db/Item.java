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

public class Item {
	private long mId = -1;
	private URL mLink;
	private String mGuid;
	private String mTitle;
	private String mDescription;
	private String mContent;
	private URL mImage = null;
	private ArrayList<Enclosure> mEnclosures;
	private Date mPubdate;
	private boolean mFavorite = false;
	private boolean mRead = false;

	public Item() {
		mPubdate = new Date();
		mEnclosures = new ArrayList<Enclosure>();
	}

	public Item(long id, URL link, String guid, String title,
			String description, String content, URL image, Date pubdate,
			boolean favorite, boolean read) {
		super();
		this.mId = id;
		this.mLink = link;
		this.mGuid = guid;
		this.mTitle = title;
		this.mDescription = description;
		this.mContent = content;
		this.mImage = image;
		this.mPubdate = pubdate;
		this.mFavorite = favorite;
		this.mRead = read;
	}

	public void setId(long id) {
		this.mId = id;
	}

	public long getId() {
		return mId;
	}

	public void setLink(URL link) {
		this.mLink = link;
	}

	public URL getLink() {
		return this.mLink;
	}

	public void setGuid(String guid) {
		this.mGuid = guid;
	}

	public String getGuid() {
		return mGuid;
	}

	public void setTitle(String title) {
		this.mTitle = title;
	}

	public String getTitle() {
		return this.mTitle;
	}

	public void setDescription(String description) {
		this.mDescription = description;
	}

	public String getDescription() {
		return mDescription;
	}

	public void setContent(String content) {
		this.mContent = content;
	}

	public String getContent() {
		return mContent;
	}

	public void setImage(URL image) {
		this.mImage = image;
	}

	public URL getImage() {
		return this.mImage;
	}

	public void setPubdate(Date pubdate) {
		this.mPubdate = pubdate;
	}

	public Date getPubdate() {
		return this.mPubdate;
	}

	public void favorite() {
		this.mFavorite = true;
	}

	public void unfavorite() {
		this.mFavorite = false;
	}

	public void setFavorite(int state) {
		if (state == DatabaseHelper.OFF)
			this.mFavorite = false;
		else
			this.mFavorite = true;
	}

	public boolean isFavorite() {
		return this.mFavorite;
	}

	public void read() {
		this.mRead = true;
	}

	public void unread() {
		this.mRead = false;
	}

	public void addEnclosure(Enclosure e) {
		this.mEnclosures.add(e);
	}

	public void setEnclosures(ArrayList<Enclosure> es) {
		this.mEnclosures = es;
	}

	public List<Enclosure> getEnclosures() {
		return this.mEnclosures;
	}

	public void setRead(int state) {
		if (state == DatabaseHelper.OFF)
			this.mRead = false;
		else
			this.mRead = true;
	}

	public boolean isRead() {
		return this.mRead;
	}

	public ContentValues toContentValues() {
		ContentValues values = new ContentValues();
		// values.put(ItemTable._ID, mId);
		if (mLink == null)
			values.putNull(ItemTable.COLUMN_LINK);
		else
			values.put(ItemTable.COLUMN_LINK, mLink.toString());
		values.put(ItemTable.COLUMN_GUID, mGuid);
		values.put(ItemTable.COLUMN_TITLE, mTitle);
		if (mDescription == null)
			values.putNull(ItemTable.COLUMN_DESCRIPTION);
		else
			values.put(ItemTable.COLUMN_DESCRIPTION, mDescription);
		if (mContent == null)
			values.putNull(ItemTable.COLUMN_CONTENT);
		else
			values.put(ItemTable.COLUMN_CONTENT, mContent);
		if (mImage == null)
			values.putNull(ItemTable.COLUMN_IMAGE);
		else
			values.put(ItemTable.COLUMN_IMAGE, mImage.toString());
		values.put(ItemTable.COLUMN_PUBDATE, mPubdate.getTime());
		int state = DatabaseHelper.ON;
		if (!mFavorite)
			state = DatabaseHelper.OFF;
		values.put(ItemTable.COLUMN_FAVORITE, state);
		if (!mRead)
			state = DatabaseHelper.OFF;
		else
			state = DatabaseHelper.ON;
		values.put(ItemTable.COLUMN_READ, state);
		return values;
	}

	public String toString() {
		String s = "{ID=" + this.mId + " link=";
		if (this.mLink == null)
			s += "null";
		else
			s += this.mLink.toString();
		s += " GUID=" + this.mGuid + " title=" + this.mTitle + " description="
				+ this.mDescription + " content=" + this.mContent + " image=";
		if (this.mImage == null)
			s += "null";
		else
			s += this.mImage.toString();
		if (this.mPubdate == null)
			s += " pubdate=null";
		else
			s += " pubdate=" + this.mPubdate.toString();
		s += " favorite=" + this.mFavorite + " read=" + this.mRead + "}";

		return s;
	}

}
