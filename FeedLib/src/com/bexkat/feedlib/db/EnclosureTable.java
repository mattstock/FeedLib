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

public class EnclosureTable implements BaseColumns {
	public static final String TAG = "EnclosureTable";
	public static final String TABLE_NAME = "enclosures";
	public static final String COLUMN_ITEM_ID = "item_id";
	public static final String COLUMN_MIME = "mime";
	public static final String COLUMN_URL = "URL";
	public static final String[] COLUMNS = { _ID, COLUMN_ITEM_ID, COLUMN_MIME,
			COLUMN_URL };
	public static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME
			+ " (" + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
			+ COLUMN_ITEM_ID + " INTEGER NOT NULL," + COLUMN_MIME
			+ " TEXT NOT NULL," + COLUMN_URL + " TEXT NOT NULL);";
	public static final String DROP_TABLE = "DROP TABLE IF EXISTS "
			+ TABLE_NAME;
	private ContentResolver mResolver;

	public EnclosureTable(ContentResolver resolver) {
		mResolver = resolver;
	}
	
	public EnclosureTable(Context context) {
		mResolver = context.getContentResolver();
	}

	public static void onCreate(SQLiteDatabase database) {
		database.execSQL(CREATE_TABLE);
	}

	public static void onUpgrade(SQLiteDatabase database, int oldVersion,
			int newVersion) {
		Log.w(Enclosure.class.getName(), "Upgrading database from version "
				+ oldVersion + " to " + newVersion
				+ ", which will destroy all old data");
		database.execSQL(DROP_TABLE);
		onCreate(database);
	}

	public long addEnclosure(Item item, Enclosure enclosure) {
		return addEnclosure(item.getId(), enclosure);
	}

	public long addEnclosure(long itemId, Enclosure enclosure) {
		ContentValues values = enclosure.toContentValues();
		values.put(COLUMN_ITEM_ID, itemId);
		return addEnclosure(values);
	}

	public long addEnclosure(ContentValues values) {
		Uri enclosureUri = mResolver.insert(
				MyContentProvider.ENCLOSURE_CONTENT_URI, values);
		Log.d(TAG, "addEnclosure(): " + enclosureUri.toString());
		return getEnclosure(enclosureUri).getId();
	}

	public List<Enclosure> getEnclosures(Item item) {
		List<Enclosure> tmp = new ArrayList<Enclosure>();
		
		Cursor cursor = mResolver.query(Uri.parse(MyContentProvider.ITEMLIST_CONTENT_URI + "/" + item.getId()),
				null, null, null, null);
		
		if (cursor == null)
			return tmp;
		
		// TODO need to iterate
		cursor.moveToFirst();
		tmp.add(cursorToEnclosure(cursor));
		cursor.close();
		return tmp;
	}
	
	public Enclosure getEnclosure(long enclosureId) {
		return getEnclosure(Uri.parse(MyContentProvider.ENCLOSURE_CONTENT_URI
				+ "/" + enclosureId));
	}

	public Enclosure getEnclosure(Uri enclosureUri) {
		Enclosure enclosure = null;
		Cursor cursor = mResolver.query(enclosureUri, null, null, null, null);

		if (cursor != null && cursor.moveToFirst()) {
			enclosure = cursorToEnclosure(cursor);
		}

		if (cursor != null)
			cursor.close();

		return enclosure;
	}

	private Enclosure cursorToEnclosure(Cursor cursor) {
		Enclosure e = new Enclosure();
		try {
			e.setId(cursor.getLong(cursor.getColumnIndex(_ID)));
			e.setURL(new URL(
					cursor.getString(cursor.getColumnIndex(COLUMN_URL))));
			e.setMime(cursor.getString(cursor.getColumnIndex(COLUMN_MIME)));
		} catch (MalformedURLException mue) {
			Log.e(TAG, "", mue);
		}
		return e;
	}
}
