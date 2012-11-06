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

import java.net.URISyntaxException;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.bexkat.feedlib.db.Item;
import com.bexkat.feedlib.db.ItemTable;

public class ItemDetailActivity extends SherlockFragmentActivity {
	private static final String TAG = "ItemDetailActivity";
	private static final int MENU_ABOUT = 2;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTheme(R.style.Theme_Sherlock);

		Bundle b = new Bundle();

		Intent intent = getIntent();
		long id = intent.getLongExtra(ItemTable._ID, 0);

		ItemTable table = new ItemTable(this);
		Item item = table.getItem(id);

		b.putString("title", item.getTitle());
		b.putString("pubDate", item.getPubdate().toString());
		b.putString("content", item.getContent());
		b.putBoolean("fav", item.isFavorite());
		b.putLong("id", id);
		try {
			b.putString("uri", item.getLink().toURI().toString());
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

		Fragment f = getSupportFragmentManager().findFragmentById(
				android.R.id.content);
		if (f == null) {
			f = ItemDetailFragment.newInstance(b);
			FragmentTransaction ft = getSupportFragmentManager()
					.beginTransaction();
			ft.add(android.R.id.content, f).commit();
		}
	}

	public void onClick(View v) {
		ItemDetailFragment f = (ItemDetailFragment) getSupportFragmentManager().findFragmentById(android.R.id.content);
		if (f == null)
			return;
		f.onClick(v);
	}
}
