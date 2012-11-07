package com.bexkat.feedlib;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.util.Log;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Window;
import com.bexkat.feedlib.db.Feed;
import com.bexkat.feedlib.db.FeedTable;
import com.viewpagerindicator.TabPageIndicator;

public class MainTabActivity extends SherlockFragmentActivity {
	private static final String TAG = "MainTabActivity";
	private int position;
	private boolean firstuse;
	private SharedPreferences prefs;
	private ViewPager pager;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTheme(R.style.Theme_FeedLib);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.simple_tabs);

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

		prefs = getPreferences(MODE_PRIVATE);
		
		FragmentPagerAdapter adapter = new FeedPagerAdapter(getSupportFragmentManager());
        pager = (ViewPager)findViewById(R.id.pager);
        pager.setAdapter(adapter);
        TabPageIndicator indicator = (TabPageIndicator)findViewById(R.id.indicator);
        indicator.setViewPager(pager);
	}

	@Override
	protected void onResume() {
		super.onResume();
		firstuse = prefs.getBoolean("firstuse", true);
		
		if (firstuse) {
			FragmentTransaction ftrans = getSupportFragmentManager()
					.beginTransaction();
			HelpFragment hf = new HelpFragment();
			hf.show(ftrans, "help");			
			firstuse = false;
		}
		
		position = prefs.getInt("position", 1);
        pager.setCurrentItem(position);
		checkFreshness();
	}

	@Override
	public void onPause() {
		SharedPreferences.Editor edit = prefs.edit();
		edit.putInt("position", position);
		edit.putBoolean("firstuse", firstuse);
		edit.commit();
		super.onPause();
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
		public Fragment getItem(int newposition) {
			Bundle args = new Bundle();
			ItemListFragment f = new ItemListFragment();

			position = newposition;
			if (mFeeds.get(newposition) == null) { // We want the favorites fragment
				args.putLong("feedId", -1);
				args.putString("title", "Favorites");
			} else {
				args.putLong("feedId", mFeeds.get(newposition).getId());
				args.putString("title", mFeeds.get(newposition).getTitle());
			}
			f.setArguments(args);
			return f;
		}

        public CharSequence getPageTitle(int index) {
        	if (mFeeds.get(index) == null)
        		return "Favorites";
        	else
        		return mFeeds.get(index).getTitle();
        }
		@Override
		public int getCount() {
			return mFeeds.size();
		}
	}
}
