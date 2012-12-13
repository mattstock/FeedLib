package com.bexkat.feedlib;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.util.Log;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Window;
import com.bexkat.feedlib.db.DatabaseHelper;
import com.bexkat.feedlib.db.Feed;
import com.bexkat.feedlib.db.FeedTable;
import com.viewpagerindicator.IconPagerAdapter;
import com.viewpagerindicator.TabPageIndicator;

public class MainTabActivity extends SherlockFragmentActivity implements IndicatorCallback {
	private static final String TAG = "MainTabActivity";
	private boolean firstuse;
	private SharedPreferences prefs;
	private ViewPager pager;
	private TabPageIndicator indicator;
	
	private int number_icons[] = {
			0,
			R.drawable.one,
			R.drawable.two,
			R.drawable.three,
			R.drawable.four,
			R.drawable.five,
			R.drawable.six,
			R.drawable.seven,
			R.drawable.eight,
			R.drawable.nine };
	
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
		
		FragmentStatePagerAdapter adapter = new FeedPagerAdapter(getSupportFragmentManager());
        pager = (ViewPager)findViewById(R.id.pager);
        pager.setAdapter(adapter);
        indicator = (TabPageIndicator)findViewById(R.id.indicator);
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
		
        pager.setCurrentItem(prefs.getInt("position", 1));
		checkFreshness();
		
	}

	@Override
	public void onPause() {
		SharedPreferences.Editor edit = prefs.edit();
		edit.putInt("position", pager.getCurrentItem());
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

	private class FeedPagerAdapter extends FragmentStatePagerAdapter implements IconPagerAdapter {
		ArrayList<Feed> mFeeds;
		FeedTable feedtable;
		
		public FeedPagerAdapter(FragmentManager fm) {
			super(fm);
			feedtable = new FeedTable(MainTabActivity.this);
			mFeeds = feedtable.getEnabledFeeds();
			mFeeds.add(0, null); // For favorites
		}

		@Override
		public Fragment getItem(int newposition) {
			Bundle args = new Bundle();
			ItemListFragment f = new ItemListFragment();

			if (newposition == 0) { // We want the favorites fragment
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

		@Override
		public int getIconResId(int index) {
			Feed feed = mFeeds.get(index);
			
			if (feed == null)
				return mapCount(feedtable.getUnreadCount(-1));
			else
				return mapCount(feedtable.getUnreadCount(feed.getId()));
		}
		
		// Takes a count and returns the correct icon resource ID
		private int mapCount(int count) {
			if (count > 9)
				return R.drawable.nineplus;
			return number_icons[count];
		}
	}
	
	public void refreshUnreadCount() {
		indicator.notifyDataSetChanged();
	}
	
}
