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

package com.bexkat.feedlib.db;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParserException;

import com.bexkat.feedlib.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {
	private static final String TAG = "DatabaseHelper";
	private static final String DATABASE_NAME = "dbfeed";
	private static final int DATABASE_VERSION = 14;
	public static final String SORT_ASC = " ASC";
	public static final String SORT_DESC = " DESC";
	public static final String[] ORDERS = { SORT_ASC, SORT_DESC };
	public static final int OFF = 0;
	public static final int ON = 1;
	private Context context;

	public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		this.context = context;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		FeedTable.onCreate(db);
		EnclosureTable.onCreate(db);
		ItemTable.onCreate(db);

		populateFeeds(db);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		FeedTable.onUpgrade(db, oldVersion, newVersion);
		ItemTable.onUpgrade(db, oldVersion, newVersion);
		EnclosureTable.onUpgrade(db, oldVersion, newVersion);

		populateFeeds(db);
	}

	@Override
	public void onOpen(SQLiteDatabase db) {
		super.onOpen(db);
		
		Log.d(TAG, "onOpen()");
		
		SharedPreferences prefs = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
		int oldver = prefs.getInt("version", 1);
		int newver = 1;
		
		try {
			newver = context.getPackageManager()
					.getPackageInfo(context.getPackageName(), 0).versionCode;
		} catch (NameNotFoundException e) {
		}
		
		if (newver > oldver) {
			Log.d(TAG, "app version change, updating feeds");
			populateFeeds(db);
			SharedPreferences.Editor edit = prefs.edit();
			edit.putInt("version", newver);
			edit.commit();
		}
	}

	private List<Feed> getOPMLResourceFeeds() throws XmlPullParserException,
			MalformedURLException, IOException {
		List<Feed> feeds = new ArrayList<Feed>();
		Feed feed;

		XmlResourceParser parser = context.getResources().getXml(R.xml.feeds);

		int eventType = -1;
		while (eventType != XmlResourceParser.END_DOCUMENT) {
			if (eventType == XmlResourceParser.START_TAG) {
				String tagName = parser.getName();
				if (tagName.equals("outline")
						&& parser.getAttributeCount() >= 4) {
					feed = new Feed();
					feed.setTitle(parser.getAttributeValue(null, "title"));
					feed.setURL(new URL(parser
							.getAttributeValue(null, "xmlUrl")));
					feed.setHomePage(new URL(parser.getAttributeValue(null,
							"htmlUrl")));
					feed.setType(parser.getAttributeValue(null, "type"));
					feed.setDescription(parser.getAttributeValue(null, "text"));
					feed.setEnabled(ON);
					feeds.add(feed);
				}
			}
			eventType = parser.next();
		}
		parser.close();
		return feeds;
	}

	// All of the initial population stuff needs to interact with the raw DB,
	// not via content provider
	public void populateFeeds(SQLiteDatabase db) {
		// Read and populate OPML feeds
		try {
			for (Feed feed : getOPMLResourceFeeds()) {
				if (hasFeed(db, feed) == -1)
					db.insert(FeedTable.TABLE_NAME, null,
							feed.toContentValues());
				else
					db.update(FeedTable.TABLE_NAME, feed.toContentValues(),
							FeedTable._ID + "=?",
							new String[] { Long.toString(feed.getId()) });
			}
		} catch (XmlPullParserException xppe) {
			Log.e(TAG, "", xppe);
		} catch (MalformedURLException mue) {
			Log.e(TAG, "", mue);
		} catch (IOException ioe) {
			Log.e(TAG, "", ioe);
		}
	}

	// check if feed URL already exists in the DB
	// if exists, returns feed id
	// if does not exist, returns -1
	private long hasFeed(SQLiteDatabase db, Feed feed) {
		long feedId = -1;
		String[] projection = { FeedTable._ID };
		Cursor cursor = db.query(FeedTable.TABLE_NAME, projection,
				FeedTable.COLUMN_URL + "=?", new String[] { feed.getURL()
						.toString() }, null, null, null);
		if (cursor.moveToFirst())
			feedId = cursor.getLong(cursor.getColumnIndex(FeedTable._ID));

		if (cursor != null)
			cursor.close();

		return feedId;
	}
}