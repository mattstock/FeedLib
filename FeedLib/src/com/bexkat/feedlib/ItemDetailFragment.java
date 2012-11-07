package com.bexkat.feedlib;

import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.ShareActionProvider;
import com.bexkat.feedlib.db.DatabaseHelper;
import com.bexkat.feedlib.db.ItemTable;

public class ItemDetailFragment extends SherlockFragment {
	Uri uri;
	String title;
	long id;
	boolean fav;
	ImageView favIcon;
	
	private static final String TAG = "ItemDetailFragment";

	public static ItemDetailFragment newInstance(Bundle b) {
		ItemDetailFragment f = new ItemDetailFragment();

		f.setArguments(b);
		return f;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.item_detail, container, false);

		Bundle b = getArguments();
		TextView tv = (TextView) view.findViewById(R.id.item_content);
		tv.setText(b.getString("content"));
		tv = (TextView) view.findViewById(R.id.item_pubdate);
		tv.setText(b.getString("pubDate"));
		tv = (TextView) view.findViewById(R.id.item_title);
		tv.setText(b.getString("title"));
		fav = b.getBoolean("fav");
		title = b.getString("title");
		uri = Uri.parse(b.getString("uri"));
		id = b.getLong("id");
		favIcon = (ImageView) view.findViewById(R.id.status);
		Log.d(TAG, "changing visibility - " + fav);
		favIcon.setVisibility(fav ? View.VISIBLE : View.INVISIBLE );
		return view;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.menu_item_detail, menu);
		MenuItem actionItem = menu.findItem(R.id.menu_item_share);
		ShareActionProvider actionProvider = (ShareActionProvider) actionItem
				.getActionProvider();
		actionProvider
				.setShareHistoryFileName(ShareActionProvider.DEFAULT_SHARE_HISTORY_FILE_NAME);
		actionProvider.setShareIntent(createShareIntent(title, uri));
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		ItemTable table = new ItemTable(getSherlockActivity());
		int itemId = item.getItemId();
		if (itemId == R.id.menu_item_favorite) {
			Log.d(TAG, "toggle favorite: " + id + " - " + fav);
			fav = !fav;
			favIcon.setVisibility(fav ? View.VISIBLE : View.INVISIBLE );
			ContentValues values = new ContentValues();
			values.put(ItemTable.COLUMN_FAVORITE,
					(fav ? DatabaseHelper.ON
							: DatabaseHelper.OFF));
			table.updateItem(id, values);
			return true;
		} else if (itemId == R.id.menu_item_about) {
			FragmentTransaction ftrans = getFragmentManager()
					.beginTransaction();
			AboutFragment af = new AboutFragment();
			af.show(ftrans, "about");
			return true;
		} else if (itemId == R.id.menu_item_help) {
			FragmentTransaction ftrans = getFragmentManager()
					.beginTransaction();
			HelpFragment hf = new HelpFragment();
			hf.show(ftrans, "help");
			return true;
		}		
		return false;
	}
	
	public void onClick(View v) {
		if (v.getId() == R.id.web_view) {
			Intent intent = new Intent(Intent.ACTION_VIEW, uri);
			startActivity(intent);
		}
	}

	static public Intent createShareIntent(String title, Uri uri) {
		Intent shareIntent = new Intent(Intent.ACTION_SEND);

		shareIntent.setType("text/plain");
		shareIntent.putExtra(Intent.EXTRA_SUBJECT, R.string.share_subject);
		shareIntent.putExtra(Intent.EXTRA_TEXT, "Article on " + title + ": "
				+ uri.toString());
		return shareIntent;
	}
}
