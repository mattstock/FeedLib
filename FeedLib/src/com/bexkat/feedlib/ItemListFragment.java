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

package com.bexkat.feedlib;

import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter.ViewBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.ShareActionProvider;
import com.bexkat.feedlib.db.DatabaseHelper;
import com.bexkat.feedlib.db.Feed;
import com.bexkat.feedlib.db.FeedTable;
import com.bexkat.feedlib.db.Item;
import com.bexkat.feedlib.db.ItemTable;

public class ItemListFragment extends SherlockListFragment implements
		LoaderManager.LoaderCallbacks<Cursor> {
	private static final String TAG = "ItemListFragment";
	private SimpleCursorAdapter mCursorAdapter;
	private SimpleDateFormat mFormat = new SimpleDateFormat(
			"EEEE, MMMM d, yyyy");
	private long mFeedId;
	private ActionMode mActionMode;
	private Item selectedItem;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);

		Bundle b = getArguments();
		if (b != null)
			mFeedId = getArguments().getLong("feedId");
		else
			mFeedId = 1;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_pager_list, container,
				false);
		return v;
	}

	@Override
	public void onActivityCreated(Bundle saveInstanceState) {
		String[] from = new String[] { ItemTable.COLUMN_TITLE,
				ItemTable.COLUMN_PUBDATE, ItemTable.COLUMN_READ, ItemTable.COLUMN_FAVORITE };
		int[] to = new int[] { R.id.row_title, R.id.row_pubdate,
				R.id.row_status, R.id.row_status };

		super.onActivityCreated(saveInstanceState);

		getListView().setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				if (mActionMode != null) {
					return false;
				}
				ItemTable db = new ItemTable(getSherlockActivity());
				selectedItem = db.getItem(id);
				// Start the CAB using the ActionMode.Callback defined above
				mActionMode = getSherlockActivity().startActionMode(
						mActionModeCallback);
				view.setSelected(true);
				return true;
			}
		});

		mCursorAdapter = new SimpleCursorAdapter(getActivity(),
				R.layout.item_list, null, from, to,
				SimpleCursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
		mCursorAdapter.setViewBinder(new ViewBinder() {

			@Override
			public boolean setViewValue(View v, Cursor c, int index) {
				if (index == c.getColumnIndex(ItemTable.COLUMN_READ)) {
					ImageView iv = (ImageView) v;
					if (c.getInt(index) == DatabaseHelper.OFF)
						iv.setImageResource(R.drawable.unread);
					return true;
				}
				if (index == c.getColumnIndex(ItemTable.COLUMN_PUBDATE)) {
					String date = mFormat.format(new Date(Long.parseLong(c
							.getString(index))));
					((TextView) v).setText(date);
					return true;
				}
				if (index == c.getColumnIndex(ItemTable.COLUMN_FAVORITE)) {
					ImageView iv = (ImageView) v;
					if (c.getInt(index) == DatabaseHelper.ON)
						iv.setImageResource(R.drawable.ic_favorite);
					if (c.getInt(c.getColumnIndex(ItemTable.COLUMN_READ)) == DatabaseHelper.ON &&
							c.getInt(index) == DatabaseHelper.OFF)
						iv.setVisibility(View.INVISIBLE);
					else
						iv.setVisibility(View.VISIBLE);	
					return true;
				}
				return false;
			}

		});
		setListAdapter(mCursorAdapter);
		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Intent intent = null;

		Log.d(TAG, "Item click for view (" + v.getId() + "): " + id);
		ItemTable db = new ItemTable(getSherlockActivity());
		Item item = db.getItem(id);

		// Mark as read
		ContentValues values = new ContentValues();
		values.put(ItemTable.COLUMN_READ, DatabaseHelper.ON);
		db.updateItem(id, values);
		// If there is content, display in a new view.
		// If not, ask someone else to handle the display of the item.
		String content = item.getContent();
		if (content == null || content.length() < 10) {
			try {
				intent = new Intent(Intent.ACTION_VIEW, Uri.parse(item
						.getLink().toURI().toString()));
				startActivity(intent);
			} catch (URISyntaxException e) {
				Log.d(TAG, "URL fail: " + item.getLink().toString());
				Toast.makeText(getActivity(), "Article can't be loaded",
						Toast.LENGTH_SHORT).show();
			}
		} else {
			intent = new Intent(getSherlockActivity(), ItemDetailActivity.class);
			intent.putExtra(ItemTable._ID, id);
			startActivity(intent);
		}
	}

	private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			// Inflate a menu resource providing context menu items
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.menu_item_detail, menu);
			MenuItem actionItem = menu.findItem(R.id.menu_item_share);
			ShareActionProvider actionProvider = (ShareActionProvider) actionItem
					.getActionProvider();
			actionProvider
					.setShareHistoryFileName(ShareActionProvider.DEFAULT_SHARE_HISTORY_FILE_NAME);
			getListAdapter();
			actionProvider.setShareIntent(ItemDetailFragment.createShareIntent(
					selectedItem.getTitle(), Uri.parse(selectedItem.getLink().toString())));
			return true;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false;
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			ItemTable table = new ItemTable(getSherlockActivity());
			if (item.getItemId() == R.id.menu_item_favorite) {
				ContentValues values = new ContentValues();
				values.put(ItemTable.COLUMN_FAVORITE,
						(selectedItem.isFavorite() ? DatabaseHelper.OFF
								: DatabaseHelper.ON));
				table.updateItem(selectedItem.getId(), values);
				mode.finish();
				return true;
			}
			mode.finish();
			return false;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			mActionMode = null;
		}
	};

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.itemlist, menu);
	}

	@SuppressWarnings("unchecked")
	public boolean onOptionsItemSelected(MenuItem item) {
		FeedTable ft = new FeedTable(getActivity());
		Feed f = ft.getFeed(mFeedId);

		Log.d(TAG, "onOptionsItemSelected: " + item.getTitle() + ", " + mFeedId);

		int itemId = item.getItemId();
		if (itemId == R.id.menu_item_refresh) {
			ArrayList<Feed> array = new ArrayList<Feed>();
			array.add(f);
			new UpdateFeeds(getSherlockActivity()).execute(array);
			return true;
		} else if (itemId == R.id.menu_item_read) {
			ft.markAllAsRead(mFeedId);
			return true;
		}
		return false;
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		String[] projection = new String[] { ItemTable._ID,
				ItemTable.COLUMN_TITLE, ItemTable.COLUMN_PUBDATE,
				ItemTable.COLUMN_READ, ItemTable.COLUMN_FAVORITE };
		CursorLoader cursorLoader = new CursorLoader(getActivity(),
				Uri.parse(MyContentProvider.FEEDLIST_CONTENT_URI + "/"
						+ mFeedId), projection, null, null,
				ItemTable.COLUMN_READ + DatabaseHelper.SORT_ASC + ","
						+ ItemTable.COLUMN_PUBDATE + DatabaseHelper.SORT_DESC);
		return cursorLoader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		mCursorAdapter.swapCursor(data);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		mCursorAdapter.swapCursor(null);
	}
}