/*
 * Copyright 2012 Matthew Stock - http://www.bexkat.com/
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

package com.bexkat.feedlib;

import java.util.Arrays;
import java.util.HashSet;

import com.bexkat.feedlib.db.DatabaseHelper;
import com.bexkat.feedlib.db.EnclosureTable;
import com.bexkat.feedlib.db.FeedTable;
import com.bexkat.feedlib.db.ItemTable;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class MyContentProvider extends ContentProvider {
	private DatabaseHelper database;
	private static final String TAG = "MyContentProvider";
	private static String AUTHORITY;
	private static final int FEEDS = 10;
	private static final int FEED_ID = 20;
	private static final int ITEMS = 30;
	private static final int ITEM_ID = 40;
	private static final int FEEDLIST_ID = 50;
	private static final int ENCLOSURES = 80;
	private static final int ENCLOSURE_ID = 60;
	private static final int ITEMLIST_ID = 70;

	private static final String FEED_BASE_PATH = "feeds";
	private static final String ITEM_BASE_PATH = "items";
	private static final String FEEDLIST_BASE_PATH = "itemsinfeed";
	private static final String ENCLOSURE_BASE_PATH = "enclosure";
	private static final String ITEMLIST_BASE_PATH = "enclosuresinitem";

	public static Uri FEED_CONTENT_URI;
	public static Uri ITEM_CONTENT_URI;
	public static Uri FEEDLIST_CONTENT_URI;
	public static Uri ENCLOSURE_CONTENT_URI;
	public static Uri ITEMLIST_CONTENT_URI;
	public static String FEED_CONTENT_TYPE;
	public static String FEED_CONTENT_ITEM_TYPE;
	public static String ITEM_CONTENT_TYPE;
	public static String ITEM_CONTENT_ITEM_TYPE;
	public static String FEEDLIST_CONTENT_TYPE;

	private static final UriMatcher sURIMatcher = new UriMatcher(
			UriMatcher.NO_MATCH);

	@Override
	public boolean onCreate() {
		database = new DatabaseHelper(getContext());
		AUTHORITY = getContext().getResources().getString(R.string.authority);
		FEED_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/"
				+ FEED_BASE_PATH);
		ITEM_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/"
				+ ITEM_BASE_PATH);
		FEEDLIST_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/"
				+ FEEDLIST_BASE_PATH);
		ENCLOSURE_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/"
				+ ENCLOSURE_BASE_PATH);
		ITEMLIST_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/"
				+ ITEMLIST_BASE_PATH);
		FEED_CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/feeds";
		FEED_CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
				+ "/feed";
		ITEM_CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/items";
		ITEM_CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
				+ "/item";
		FEEDLIST_CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
				+ "/itemsinfeed";
		sURIMatcher.addURI(AUTHORITY, FEED_BASE_PATH, FEEDS);
		sURIMatcher.addURI(AUTHORITY, FEED_BASE_PATH + "/#", FEED_ID);
		sURIMatcher.addURI(AUTHORITY, ITEM_BASE_PATH, ITEMS);
		sURIMatcher.addURI(AUTHORITY, ITEM_BASE_PATH + "/#", ITEM_ID);
		sURIMatcher.addURI(AUTHORITY, ENCLOSURE_BASE_PATH, ENCLOSURES);
		sURIMatcher.addURI(AUTHORITY, ENCLOSURE_BASE_PATH + "/#", ENCLOSURE_ID);
		sURIMatcher.addURI(AUTHORITY, FEEDLIST_BASE_PATH + "/#", FEEDLIST_ID);
		sURIMatcher.addURI(AUTHORITY, ITEMLIST_BASE_PATH + "/#", ITEMLIST_ID);
		Log.d(TAG, "onCreate()");
		return false;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {

		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

		int uriType = sURIMatcher.match(uri);
		switch (uriType) {
		case FEEDS:
			queryBuilder.setTables(FeedTable.TABLE_NAME);
			break;
		case FEED_ID:
			checkColumns(FeedTable.COLUMNS, projection);
			queryBuilder.setTables(FeedTable.TABLE_NAME);
			queryBuilder.appendWhere(FeedTable._ID + "="
					+ uri.getLastPathSegment());
			break;
		case ITEMS:
			queryBuilder.setTables(ItemTable.TABLE_NAME);
			break;
		case ITEM_ID:
			checkColumns(ItemTable.COLUMNS, projection);
			queryBuilder.setTables(ItemTable.TABLE_NAME);
			queryBuilder.appendWhere(ItemTable._ID + "="
					+ uri.getLastPathSegment());
			break;
		case ENCLOSURE_ID:
			checkColumns(EnclosureTable.COLUMNS, projection);
			queryBuilder.setTables(EnclosureTable.TABLE_NAME);
			queryBuilder.appendWhere(EnclosureTable._ID + "="
					+ uri.getLastPathSegment());
			break;
		case FEEDLIST_ID:
			checkColumns(ItemTable.COLUMNS, projection);
			queryBuilder.setTables(ItemTable.TABLE_NAME);
			queryBuilder.appendWhere(ItemTable.COLUMN_FEED_ID + "="
					+ uri.getLastPathSegment());
			break;
		case ITEMLIST_ID:
			checkColumns(EnclosureTable.COLUMNS, projection);
			queryBuilder.setTables(EnclosureTable.TABLE_NAME);
			queryBuilder.appendWhere(EnclosureTable.COLUMN_ITEM_ID + "="
					+ uri.getLastPathSegment());
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}

		SQLiteDatabase db = database.getWritableDatabase();
		Cursor cursor = queryBuilder.query(db, projection, selection,
				selectionArgs, null, null, sortOrder);
		// Make sure that potential listeners are getting notified
		switch (uriType) {
		case FEEDLIST_ID:
			cursor.setNotificationUri(getContext().getContentResolver(),
					FEEDLIST_CONTENT_URI);
			break;
		case ITEMLIST_ID:
			cursor.setNotificationUri(getContext().getContentResolver(),
					ITEMLIST_CONTENT_URI);
			break;
		}
		cursor.setNotificationUri(getContext().getContentResolver(), uri);
		return cursor;
	}

	private void checkColumns(String[] available, String[] projection) {
		if (projection != null) {
			HashSet<String> requestedColumns = new HashSet<String>(
					Arrays.asList(projection));
			HashSet<String> availableColumns = new HashSet<String>(
					Arrays.asList(available));
			// Check if all columns which are requested are available
			if (!availableColumns.containsAll(requestedColumns)) {
				throw new IllegalArgumentException(
						"Unknown columns in projection");
			}
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		int uriType = sURIMatcher.match(uri);
		SQLiteDatabase sqlDB = database.getWritableDatabase();
		long id = 0;
		Uri result;
		switch (uriType) {
		case FEEDS:
			id = sqlDB.insert(FeedTable.TABLE_NAME, null, values);
			result = Uri.parse(FEED_CONTENT_URI + "/" + id);
			getContext().getContentResolver().notifyChange(FEED_CONTENT_URI,
					null);
			break;
		case ITEMS:
			id = sqlDB.insert(ItemTable.TABLE_NAME, null, values);
			result = Uri.parse(ITEM_CONTENT_URI + "/" + id);
			getContext().getContentResolver().notifyChange(ITEM_CONTENT_URI,
					null);
			getContext().getContentResolver().notifyChange(
					FEEDLIST_CONTENT_URI, null);
			break;
		case ENCLOSURES:
			id = sqlDB.insert(EnclosureTable.TABLE_NAME, null, values);
			result = Uri.parse(ENCLOSURE_CONTENT_URI + "/" + id);
			getContext().getContentResolver().notifyChange(
					ENCLOSURE_CONTENT_URI, null);
			getContext().getContentResolver().notifyChange(
					ITEMLIST_CONTENT_URI, null);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		return result;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int uriType = sURIMatcher.match(uri);
		SQLiteDatabase sqlDB = database.getWritableDatabase();
		int rowsDeleted = 0;
		String id;

		switch (uriType) {
		case FEEDS:
			rowsDeleted = sqlDB.delete(FeedTable.TABLE_NAME, selection,
					selectionArgs);
			break;
		case FEED_ID:
			id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection))
				rowsDeleted = sqlDB.delete(FeedTable.TABLE_NAME, FeedTable._ID
						+ "=" + id, null);
			else
				rowsDeleted = sqlDB.delete(FeedTable.TABLE_NAME, FeedTable._ID
						+ "=" + id + " and " + selection, selectionArgs);
			break;
		case ITEMS:
			rowsDeleted = sqlDB.delete(ItemTable.TABLE_NAME, selection,
					selectionArgs);
			break;
		case ITEM_ID:
			id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection))
				rowsDeleted = sqlDB.delete(ItemTable.TABLE_NAME, ItemTable._ID
						+ "=" + id, null);
			else
				rowsDeleted = sqlDB.delete(ItemTable.TABLE_NAME, ItemTable._ID
						+ "=" + id + " and " + selection, selectionArgs);
			break;
		case ENCLOSURES:
			rowsDeleted = sqlDB.delete(EnclosureTable.TABLE_NAME, selection,
					selectionArgs);
			break;
		case ENCLOSURE_ID:
			id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection))
				rowsDeleted = sqlDB.delete(EnclosureTable.TABLE_NAME,
						EnclosureTable._ID + "=" + id, null);
			else
				rowsDeleted = sqlDB.delete(EnclosureTable.TABLE_NAME,
						EnclosureTable._ID + "=" + id + " and " + selection,
						selectionArgs);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return rowsDeleted;
	}

	@Override
	public String getType(Uri arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		int uriType = sURIMatcher.match(uri);
		SQLiteDatabase sqlDB = database.getWritableDatabase();
		int rowsUpdated = 0;
		String id;
		switch (uriType) {
		case FEEDS:
			rowsUpdated = sqlDB.update(FeedTable.TABLE_NAME, values, selection,
					selectionArgs);
			break;
		case FEED_ID:
			id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection))
				rowsUpdated = sqlDB.update(FeedTable.TABLE_NAME, values,
						FeedTable._ID + "=" + id, null);
			else
				rowsUpdated = sqlDB.update(FeedTable.TABLE_NAME, values,
						FeedTable._ID + "=" + id + " and " + selection,
						selectionArgs);
			break;
		case FEEDLIST_ID:
			id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection))
				rowsUpdated = sqlDB.update(ItemTable.TABLE_NAME, values,
						ItemTable.COLUMN_FEED_ID + "=" + id, null);
			else
				rowsUpdated = sqlDB.update(ItemTable.TABLE_NAME, values,
						ItemTable.COLUMN_FEED_ID + "=" + id + " and "
								+ selection, selectionArgs);
			break;
		case ITEMS:
			rowsUpdated = sqlDB.update(ItemTable.TABLE_NAME, values, selection,
					selectionArgs);
			break;
		case ITEM_ID:
			id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection))
				rowsUpdated = sqlDB.update(ItemTable.TABLE_NAME, values,
						ItemTable._ID + "=" + id, null);
			else
				rowsUpdated = sqlDB.update(ItemTable.TABLE_NAME, values,
						ItemTable._ID + "=" + id + " and " + selection,
						selectionArgs);
			// TODO Refine this if we need to - we can have different queries
			// register based on feed ID,
			// and just have a specific feed ID be updated here.
			getContext().getContentResolver().notifyChange(
					FEEDLIST_CONTENT_URI, null);
			break;
		case ENCLOSURES:
			rowsUpdated = sqlDB.update(EnclosureTable.TABLE_NAME, values,
					selection, selectionArgs);
			break;
		case ENCLOSURE_ID:
			id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection))
				rowsUpdated = sqlDB.update(EnclosureTable.TABLE_NAME, values,
						EnclosureTable._ID + "=" + id, null);
			else
				rowsUpdated = sqlDB.update(EnclosureTable.TABLE_NAME, values,
						EnclosureTable._ID + "=" + id + " and " + selection,
						selectionArgs);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return rowsUpdated;
	}
}
