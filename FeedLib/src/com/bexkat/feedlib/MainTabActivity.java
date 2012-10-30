package com.bexkat.feedlib;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.bexkat.feedlib.db.Feed;
import com.bexkat.feedlib.db.FeedTable;

public class MainTabActivity extends SherlockFragmentActivity {
	private static final String TAG = "MainTabActivity";
	private static final int MENU_ABOUT = 2;
	private int position = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setTheme(R.style.Theme_Sherlock);
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		if (savedInstanceState != null)
			position = savedInstanceState.getInt("position");

		// Enable http cache if available
		try {
			File httpCacheDir = new File(getCacheDir(), "http");
			long httpCacheSize = 10 * 1024 * 1024; // 10 MiB
			Class.forName("android.net.http.HttpResponseCache")
					.getMethod("install", File.class, long.class)
					.invoke(null, httpCacheDir, httpCacheSize);
		} catch (Exception httpResponseCacheNotAvailable) {
			Log.d(TAG, "HTTP cache not available");
		}

		final ActionBar actionBar = getSupportActionBar();

		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		MyTabListener tabListener = new MyTabListener();

		FeedTable ft = new FeedTable(this);
		for (Feed feed : ft.getEnabledFeeds()) {
			Tab tab = actionBar.newTab();
			tab.setText(feed.getTitle());
			tab.setTag(feed);
			tab.setTabListener(tabListener);
			actionBar.addTab(tab);
		}
		actionBar.setSelectedNavigationItem(position);
	}

	@Override
	protected void onResume() {
		super.onResume();
		getSupportActionBar().setSelectedNavigationItem(position);
		checkFreshness();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, MENU_ABOUT, Menu.CATEGORY_SECONDARY, "About")
				.setIcon(R.drawable.ic_menu_info_details)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
		return super.onCreateOptionsMenu(menu);
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_ABOUT:
			FragmentTransaction ft = getSupportFragmentManager()
					.beginTransaction();
			AboutFragment af = new AboutFragment();
			af.show(ft, "about");
			return true;
		}
		return false;
	}

	@Override
	public void onRestoreInstanceState(Bundle inState) {
		Log.d(TAG, "Loading tab state");
		super.onRestoreInstanceState(inState);
		position = inState.getInt("position");
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		Log.d(TAG, "Saving tab state");
		outState.putInt("position", position);
		super.onSaveInstanceState(outState);
	}

	private class MyTabListener implements ActionBar.TabListener {

		@Override
		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			Feed feed = (Feed) tab.getTag();
			Log.d(TAG, "onTabSelected(" + tab.getText() + "): " + feed.getId());
			Fragment f;
			FragmentManager fm = getSupportFragmentManager();

			position = tab.getPosition();
			f = fm.findFragmentByTag((String) tab.getText());
			if (f == null) {
				Bundle args = new Bundle();
				f = new ItemListFragment();
				args.putLong("feedId", feed.getId());
				args.putString("title", feed.getTitle());
				f.setArguments(args);
				// f.setRetainInstance(true);
				ft.add(android.R.id.content, f, (String) tab.getText());
			} else {
				if (f.isDetached())
					ft.attach(f);
			}
		}

		@Override
		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
			Feed feed = (Feed) tab.getTag();
			Log.d(TAG,
					"onTabUnselected(" + tab.getText() + "): " + feed.getId());
			Fragment f;
			FragmentManager fm = getSupportFragmentManager();
			f = fm.findFragmentByTag((String) tab.getText());
			if (f != null) {
				ft.detach(f);
			}
		}

		@Override
		public void onTabReselected(Tab tab, FragmentTransaction ft) {
			Feed feed = (Feed) tab.getTag();
			Log.d(TAG,
					"onTabReselected(" + tab.getText() + "): " + feed.getId());
		}

	}

	@SuppressWarnings("unchecked")
	public void checkFreshness() {
		Date now = new Date();
		ArrayList<Feed> oldFeeds = new ArrayList<Feed>();
		FeedTable ft = new FeedTable(this);

		for (Feed feed : ft.getEnabledFeeds())
			if (feed.getRefresh() == null)
				oldFeeds.add(feed);
			else if ((now.getTime() - feed.getRefresh().getTime()) > 30 * 60 * 1000)
				oldFeeds.add(feed);

		new UpdateFeeds(this).execute(oldFeeds);
	}
}
