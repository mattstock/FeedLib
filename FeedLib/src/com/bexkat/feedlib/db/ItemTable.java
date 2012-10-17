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

public class ItemTable implements BaseColumns {
	public static final String TAG = "ItemTable";
	public static final String TABLE_NAME = "items";
	public static final String COLUMN_FEED_ID = "feed_id";
	public static final String COLUMN_LINK = "link";
	public static final String COLUMN_GUID = "guid";
	public static final String COLUMN_TITLE = "title";
	public static final String COLUMN_DESCRIPTION = "description"; // not used
	public static final String COLUMN_CONTENT = "content";
	public static final String COLUMN_IMAGE = "image";
	public static final String COLUMN_PUBDATE = "pubdate";
	public static final String COLUMN_FAVORITE = "favorite";
	public static final String COLUMN_READ = "read";
	public static final String[] COLUMNS = { _ID, COLUMN_FEED_ID, COLUMN_LINK,
			COLUMN_GUID, COLUMN_TITLE, COLUMN_DESCRIPTION, COLUMN_CONTENT,
			COLUMN_IMAGE, COLUMN_PUBDATE, COLUMN_FAVORITE, COLUMN_READ };
	public static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME
			+ " (" + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
			+ COLUMN_FEED_ID + " INTEGER NOT NULL," + COLUMN_LINK
			+ " TEXT NOT NULL," + COLUMN_GUID + " TEXT NOT NULL,"
			+ COLUMN_TITLE + " TEXT NOT NULL," + COLUMN_DESCRIPTION + " TEXT,"
			+ COLUMN_CONTENT + " TEXT," + COLUMN_IMAGE + " TEXT,"
			+ COLUMN_PUBDATE + " INTEGER NOT NULL," + COLUMN_FAVORITE
			+ " INTEGER NOT NULL," + COLUMN_READ + " INTEGER NOT NULL);";
	public static final String DROP_TABLE = "DROP TABLE IF EXISTS "
			+ TABLE_NAME;
	private ContentResolver mResolver;

	public ItemTable(ContentResolver resolver) {
		mResolver = resolver;
	}

	public ItemTable(Context context) {
		mResolver = context.getContentResolver();
	}

	public static void onCreate(SQLiteDatabase database) {
		database.execSQL(CREATE_TABLE);
	}

	public static void onUpgrade(SQLiteDatabase database, int oldVersion,
			int newVersion) {
		Log.w(Item.class.getName(), "Upgrading database from version "
				+ oldVersion + " to " + newVersion
				+ ", which will destroy all old data");
		database.execSQL(DROP_TABLE);
		onCreate(database);
	}

	public long addItem(Feed feed, Item item) {
		return addItem(feed.getId(), item);
	}

	public long addItem(long feedId, Item item) {
		ContentValues values = item.toContentValues();
		values.put(ItemTable.COLUMN_FEED_ID, feedId);
		return addItem(values, item.getEnclosures());
	}

	public long addItem(ContentValues values, List<Enclosure> enclosures) {
		Uri itemUri = mResolver.insert(MyContentProvider.ITEM_CONTENT_URI,
				values);
		Item item = getItem(itemUri);
		EnclosureTable et = new EnclosureTable(mResolver);

		if (enclosures != null && itemUri != null) {
			for (Enclosure enclosure : enclosures) {
				et.addEnclosure(item, enclosure);
			}
		}

		return item.getId();

	}

	public Item getItem(long itemId) {
		return getItem(Uri.parse(MyContentProvider.ITEM_CONTENT_URI + "/"
				+ itemId));
	}

	private Item getItem(Uri itemUri) {
		Item item = null;
		Cursor cursor = mResolver.query(itemUri, null, null, null,
				COLUMN_PUBDATE + DatabaseHelper.SORT_DESC);

		if (cursor != null && cursor.moveToFirst()) {
			item = cursorToItem(cursor);
		}

		if (cursor != null)
			cursor.close();

		return item;
	}

	private Item cursorToItem(Cursor cursor) {
		Item item = new Item();
		try {
			item.setId(cursor.getLong(cursor.getColumnIndex(_ID)));
			item.setLink(new URL(cursor.getString(cursor
					.getColumnIndex(COLUMN_LINK))));
			item.setGuid(cursor.getString(cursor.getColumnIndex(COLUMN_GUID)));
			item.setTitle(cursor.getString(cursor.getColumnIndex(COLUMN_TITLE)));
			if (!cursor.isNull(cursor.getColumnIndex(COLUMN_DESCRIPTION)))
				item.setDescription(cursor.getString(cursor
						.getColumnIndex(COLUMN_DESCRIPTION)));
			if (!cursor.isNull(cursor.getColumnIndex(COLUMN_CONTENT)))
				item.setContent(cursor.getString(cursor
						.getColumnIndex(COLUMN_CONTENT)));
			if (!cursor.isNull(cursor.getColumnIndex(COLUMN_IMAGE)))
				item.setImage(new URL(cursor.getString(cursor
						.getColumnIndex(COLUMN_IMAGE))));
			item.setPubdate(new Date(cursor.getLong(cursor
					.getColumnIndex(COLUMN_PUBDATE))));
		} catch (MalformedURLException mue) {
			Log.e(TAG, "", mue);
		}
		return item;
	}

	public Item getFirstItem(long feedId) {
		Item item = null;
		Cursor cursor = mResolver.query(MyContentProvider.ITEM_CONTENT_URI,
				null, ItemTable.COLUMN_FEED_ID + "=? ", new String[] { Long.toString(feedId) }, COLUMN_PUBDATE + DatabaseHelper.SORT_ASC);

		if (cursor != null && cursor.moveToFirst()) {
			item = cursorToItem(cursor);
		}

		if (cursor != null)
			cursor.close();

		return item;
	}

	public boolean hasItem(long feedId, Item item) {
		boolean found;
		String[] args = new String[] { Long.toString(feedId),
				item.getLink().toString(), item.getGuid(), item.getTitle() };
		String[] projection = new String[] { ItemTable._ID };
		Cursor cursor = mResolver.query(MyContentProvider.ITEM_CONTENT_URI,
				projection, ItemTable.COLUMN_FEED_ID + "=? AND ("
						+ ItemTable.COLUMN_LINK + "=? OR "
						+ ItemTable.COLUMN_GUID + "=? OR "
						+ ItemTable.COLUMN_TITLE + "=?)", args, null);

		found = (cursor != null && cursor.moveToFirst());

		if (cursor != null)
			cursor.close();

		return found;
	}

	public Item getLastItem(long feedId) {
		Item item = null;
		Cursor cursor = mResolver.query(MyContentProvider.ITEM_CONTENT_URI,
				null, ItemTable.COLUMN_FEED_ID + "=? ", new String[] { Long.toString(feedId) }, COLUMN_PUBDATE + DatabaseHelper.SORT_DESC);

		if (cursor != null && cursor.moveToFirst()) {
			item = cursorToItem(cursor);
		}

		if (cursor != null)
			cursor.close();

		return item;
	}

	public void cleanDbItems(long feedId) {
		Log.d(TAG, "cleanDbItems(): NOP");
		// Prune items if we want, but I don't think there's much of a need.
	}

	public void updateItem(long itemId, ContentValues values) {
		mResolver.update(Uri.parse(MyContentProvider.ITEM_CONTENT_URI + "/" + itemId),
				values, null, null);
	}
	
	public void markAllAsRead(long feedId) {
		ContentValues values = new ContentValues();
		values.put(ItemTable.COLUMN_READ, DatabaseHelper.ON);
		mResolver.update(MyContentProvider.ITEM_CONTENT_URI,
				values, ItemTable.COLUMN_FEED_ID + "=? ", new String[] { Long.toString(feedId) });
	}

}
