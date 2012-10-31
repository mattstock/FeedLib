package com.bexkat.feedlib;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;

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
		super.onCreate(savedInstanceState);
		setTheme(R.style.Theme_Sherlock);

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

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
		selectInSpinnerIfPresent(position, false);
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
		Log.d(TAG, "Saving tab state");
		outState.putInt("position", position);
		super.onSaveInstanceState(outState);
	}

	private class MyTabListener implements ActionBar.TabListener {

		@Override
		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			Feed feed = (Feed) tab.getTag();
			Log.d(TAG,
					"onTabSelected(" + tab.getText() + "): "
							+ tab.getPosition());
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
			Log.d(TAG,
					"onTabUnselected(" + tab.getText() + "): "
							+ tab.getPosition());
			Fragment f;
			FragmentManager fm = getSupportFragmentManager();
			f = fm.findFragmentByTag((String) tab.getText());
			if (f != null) {
				ft.detach(f);
			}
		}

		@Override
		public void onTabReselected(Tab tab, FragmentTransaction ft) {
			Log.d(TAG,
					"onTabReselected(" + tab.getText() + "): "
							+ tab.getPosition());
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

	/**
	 * Hack that takes advantage of interface parity between ActionBarSherlock
	 * and the native interface to reach inside the classes to manually select
	 * the appropriate tab spinner position if the overflow tab spinner is
	 * showing.
	 * 
	 * Related issues:
	 * https://github.com/JakeWharton/ActionBarSherlock/issues/240 and
	 * https://android-review.googlesource.com/#/c/32492/
	 * 
	 * @author toulouse@crunchyroll.com
	 */
	private void selectInSpinnerIfPresent(int position, boolean animate) {
		try {
			View actionBarView = findViewById(R.id.abs__action_bar);
			if (actionBarView == null) {
				int id = getResources().getIdentifier("action_bar", "id",
						"android");
				actionBarView = findViewById(id);
			}

			Class<?> actionBarViewClass = actionBarView.getClass();
			Field mTabScrollViewField = actionBarViewClass
					.getDeclaredField("mTabScrollView");
			mTabScrollViewField.setAccessible(true);

			Object mTabScrollView = mTabScrollViewField.get(actionBarView);
			if (mTabScrollView == null) {
				return;
			}

			Field mTabSpinnerField = mTabScrollView.getClass()
					.getDeclaredField("mTabSpinner");
			mTabSpinnerField.setAccessible(true);

			Object mTabSpinner = mTabSpinnerField.get(mTabScrollView);
			if (mTabSpinner == null) {
				return;
			}

			Method setSelectionMethod = mTabSpinner
					.getClass()
					.getSuperclass()
					.getDeclaredMethod("setSelection", Integer.TYPE,
							Boolean.TYPE);
			setSelectionMethod.invoke(mTabSpinner, position, animate);

			Method requestLayoutMethod = mTabSpinner.getClass().getSuperclass()
					.getDeclaredMethod("requestLayout");
			requestLayoutMethod.invoke(mTabSpinner);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}
}
