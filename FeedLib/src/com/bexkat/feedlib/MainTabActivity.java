package com.bexkat.feedlib;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.util.Log;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.bexkat.feedlib.db.Feed;
import com.bexkat.feedlib.db.FeedTable;
import com.viewpagerindicator.TabPageIndicator;

public class MainTabActivity extends SherlockFragmentActivity {
	private static final String TAG = "MainTabActivity";
	private static final int MENU_ABOUT = 2;
	private int position = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTheme(R.style.Theme_FeedLib);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.simple_tabs);

		Log.d(TAG, "onCreate()");
		if (savedInstanceState != null) {
			position = savedInstanceState.getInt("position");
			Log.d(TAG, "restoring state: " + position);
		}

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

		FragmentPagerAdapter adapter = new FeedPagerAdapter(getSupportFragmentManager());

        ViewPager pager = (ViewPager)findViewById(R.id.pager);
        pager.setAdapter(adapter);
        TabPageIndicator indicator = (TabPageIndicator)findViewById(R.id.indicator);
        indicator.setViewPager(pager);
        pager.setCurrentItem(1);
	}

	@Override
	protected void onResume() {
		super.onResume();
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
	public void onSaveInstanceState(Bundle outState) {
		outState.putInt("position", position);
		super.onSaveInstanceState(outState);
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

	private class FeedPagerAdapter extends FragmentPagerAdapter {
		ArrayList<Feed> mFeeds;

		public FeedPagerAdapter(FragmentManager fm) {
			super(fm);
			mFeeds = (new FeedTable(MainTabActivity.this)).getEnabledFeeds();
			mFeeds.add(0, null); // For favorites
		}

		@Override
		public Fragment getItem(int position) {
			Bundle args = new Bundle();
			ItemListFragment f = new ItemListFragment();

			if (mFeeds.get(position) == null) { // We want the favorites fragment
				args.putLong("feedId", -1);
				args.putString("title", "Favorites");
			} else {
				args.putLong("feedId", mFeeds.get(position).getId());
				args.putString("title", mFeeds.get(position).getTitle());
			}
			f.setArguments(args);
			return f;
		}

        public CharSequence getPageTitle(int position) {
        	if (mFeeds.get(position) == null)
        		return "Favorites";
        	else
        		return mFeeds.get(position).getTitle();
        }
		@Override
		public int getCount() {
			return mFeeds.size();
		}
	}
}
