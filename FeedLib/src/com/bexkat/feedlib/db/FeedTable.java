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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.bexkat.feedlib.MyContentProvider;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

public class FeedTable implements BaseColumns {
	public static final String TAG = "FeedTable";
	public static final String TABLE_NAME = "feeds";
	public static final String COLUMN_URL = "url";
	public static final String COLUMN_HOMEPAGE = "homepage";
	public static final String COLUMN_DESCRIPTION = "description";
	public static final String COLUMN_TITLE = "title";
	public static final String COLUMN_TYPE = "type";
	public static final String COLUMN_REFRESH = "refresh";
	public static final String COLUMN_ENABLE = "enable";
	public static final String[] COLUMNS = { COLUMN_URL, COLUMN_HOMEPAGE,
			COLUMN_DESCRIPTION, COLUMN_TITLE, COLUMN_TYPE, COLUMN_REFRESH,
			COLUMN_ENABLE, _ID };
	public static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME
			+ " (" + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + COLUMN_URL
			+ " TEXT NOT NULL," + COLUMN_HOMEPAGE + " TEXT NOT NULL,"
			+ COLUMN_TITLE + " TEXT NOT NULL," + COLUMN_DESCRIPTION + " TEXT,"
			+ COLUMN_TYPE + " TEXT," + COLUMN_REFRESH + " INTEGER,"
			+ COLUMN_ENABLE + " INTEGER NOT NULL);";
	public static final String DROP_TABLE = "DROP TABLE IF EXISTS "
			+ TABLE_NAME;
	private ContentResolver mResolver;

	public static void onCreate(SQLiteDatabase database) {
		database.execSQL(CREATE_TABLE);
	}

	public static void onUpgrade(SQLiteDatabase database, int oldVersion,
			int newVersion) {
		Log.w(Feed.class.getName(), "Upgrading database from version "
				+ oldVersion + " to " + newVersion
				+ ", which will destroy all old data");
		database.execSQL(DROP_TABLE);
		onCreate(database);
	}

	public FeedTable(Context context) {
		mResolver = context.getContentResolver();
	}

	public long addFeed(Feed feed) {
		return addFeed(feed.toContentValues(), feed.getItems());
	}

	public long addFeed(ContentValues values, List<Item> items) {
		Uri feedUri = mResolver.insert(MyContentProvider.FEED_CONTENT_URI,
				values);
		ItemTable itemtable = new ItemTable(mResolver);

		if (items != null && feedUri != null) {
			for (Item item : items) {
				itemtable.addItem(getFeed(feedUri), item);
			}
		}

		return getFeed(feedUri).getId();
	}

	public Feed getFeed(Uri feedUri) {
		Feed feed = null;
		Cursor cursor = mResolver.query(feedUri, null, null, null, null);

		try {
			if (cursor.moveToFirst()) {
				feed = new Feed();
				feed.setId(cursor.getLong(cursor.getColumnIndex(FeedTable._ID)));
				feed.setURL(new URL(cursor.getString(cursor
						.getColumnIndex(FeedTable.COLUMN_URL))));
				feed.setHomePage(new URL(cursor.getString(cursor
						.getColumnIndex(FeedTable.COLUMN_HOMEPAGE))));
				feed.setTitle(cursor.getString(cursor
						.getColumnIndex(FeedTable.COLUMN_TITLE)));
				feed.setDescription(cursor.getString(cursor
						.getColumnIndex(FeedTable.COLUMN_DESCRIPTION)));
				if (!cursor
						.isNull(cursor.getColumnIndex(FeedTable.COLUMN_TYPE)))
					feed.setType(cursor.getString(cursor
							.getColumnIndex(FeedTable.COLUMN_TYPE)));
				if (!cursor.isNull(cursor
						.getColumnIndex(FeedTable.COLUMN_REFRESH)))
					feed.setRefresh(new Date(cursor.getLong(cursor
							.getColumnIndex(FeedTable.COLUMN_REFRESH))));
				feed.setEnabled(cursor.getInt(cursor
						.getColumnIndex(FeedTable.COLUMN_ENABLE)));
			}
		} catch (MalformedURLException mue) {
			Log.e(TAG, "", mue);
		}

		if (cursor != null)
			cursor.close();

		return feed;
	}

	public Feed getFeed(long feedId) {
		return getFeed(Uri.parse(MyContentProvider.FEED_CONTENT_URI + "/"
				+ feedId));
	}

	public Feed getFirstFeed() {
		String[] projection = { FeedTable._ID };
		Cursor cursor = mResolver
				.query(MyContentProvider.FEED_CONTENT_URI, projection, null,
						null, FeedTable._ID + DatabaseHelper.SORT_ASC);
		Feed firstFeed = null;

		if (cursor.moveToFirst())
			firstFeed = getFeed(cursor.getLong(cursor
					.getColumnIndex(FeedTable._ID)));

		if (cursor != null)
			cursor.close();
		return firstFeed;
	}

	public ContentValues getUpdateContentValues(Feed feed) {
		ContentValues values = new ContentValues();

		if (feed.getRefresh() == null)
			values.putNull(FeedTable.COLUMN_REFRESH);
		else
			values.put(FeedTable.COLUMN_REFRESH, feed.getRefresh().getTime());
		int state = DatabaseHelper.ON;
		if (!feed.isEnabled())
			state = DatabaseHelper.OFF;
		values.put(FeedTable.COLUMN_ENABLE, state);
		return values;
	}

	public boolean updateFeed(Feed feed) {
		return updateFeed(feed.getId(), getUpdateContentValues(feed),
				feed.getItems());
	}

	public boolean updateFeed(long feedId, ContentValues values,
			List<Item> items) {
		ItemTable itemtable = new ItemTable(mResolver);
		int changed = mResolver.update(
				Uri.parse(MyContentProvider.FEED_CONTENT_URI + "/" + feedId),
				values, null, null);

		if (changed > 0 && items != null) {
			Item firstDbItem = itemtable.getFirstItem(feedId);
			for (Item item : items) {
				if (!itemtable.hasItem(feedId, item)) {
					if (firstDbItem == null)
						itemtable.addItem(feedId, item); // Db is empty
					else {
						if (item.getPubdate().after(firstDbItem.getPubdate()))
							itemtable.addItem(feedId, item);
					}
				}
			}
		}

		return (changed > 0);
	}

	public ArrayList<Feed> getEnabledFeeds() {
		return getFeeds(FeedTable.COLUMN_ENABLE + "=?",
				new String[] { Long.toString(DatabaseHelper.ON) });
	}

	public ArrayList<Feed> getFeeds() {
		return getFeeds(null, null);
	}

	public ArrayList<Feed> getFeeds(String selection, String[] selectionArgs) {

		String[] projection = { FeedTable._ID };
		ArrayList<Feed> feeds = new ArrayList<Feed>();
		Cursor cursor = mResolver.query(MyContentProvider.FEED_CONTENT_URI,
				projection, selection, selectionArgs, FeedTable.COLUMN_TITLE
						+ DatabaseHelper.SORT_ASC);
		if (cursor == null)
			return null;
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			Feed feed = getFeed(cursor.getLong(cursor
					.getColumnIndex(FeedTable._ID)));
			if (feed != null)
				feeds.add(feed);
			cursor.moveToNext();
		}
		if (cursor != null)
			cursor.close();
		return feeds;
	}

	public int getUnreadCount(long feedId) {
		String[] projection = { ItemTable._ID };
		Cursor cursor;

		if (feedId == -1)
			return 0;
		
		cursor = mResolver.query(Uri.parse(MyContentProvider.FEEDLIST_CONTENT_URI + "/" + feedId),
					projection, ItemTable.COLUMN_READ + "=?", new String[] { Long.toString(DatabaseHelper.OFF) }, null);
			
		if (cursor == null)
			return 0;
		int foo = cursor.getCount();
		cursor.close();
		return foo;
	}

	public void markAllAsRead(long feedId) {
		ContentValues values = new ContentValues();
		values.put(ItemTable.COLUMN_READ, DatabaseHelper.ON);
		mResolver.update(Uri.parse(MyContentProvider.FEEDLIST_CONTENT_URI + "/"
				+ feedId), values, null, null);
	}
}
