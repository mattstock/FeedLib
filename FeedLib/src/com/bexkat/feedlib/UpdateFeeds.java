package com.bexkat.feedlib;

import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.bexkat.feedlib.db.Feed;
import com.bexkat.feedlib.db.FeedTable;
import com.bexkat.feedlib.db.Item;
import com.bexkat.feedlib.db.ItemTable;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

public class UpdateFeeds extends AsyncTask<ArrayList<Feed>, Void, Boolean> {
	private static final String TAG = "UpdateFeeds";
	SherlockFragmentActivity activity;
	
	public UpdateFeeds(SherlockFragmentActivity activity) {
		this.activity = activity;
	}
	
	@Override
	protected void onPreExecute() {
        activity.setSupportProgressBarIndeterminateVisibility(true);		
	}
	
	
	@Override
	protected Boolean doInBackground(ArrayList<Feed>... params) {
		long lastItemIdBeforeUpdate = -1;
		long lastItemIdAfterUpdate = -1;
		Boolean newitems = false;
		FeedTable ft = new FeedTable(activity);
		ItemTable it = new ItemTable(activity);

		for (Feed feed : params[0]) {
			long feedId = feed.getId();
			Item lastItem = it.getLastItem(feedId);
			if (lastItem != null)
				lastItemIdBeforeUpdate = lastItem.getId();

			FeedHandler feedHandler = new FeedHandler(activity);

			try {
				Log.d(TAG, "Updating " + feed.getTitle());
				Feed handledFeed = feedHandler.handleFeed(feed.getURL());

				handledFeed.setId(feedId);

				ft.updateFeed(handledFeed);
				it.cleanDbItems(feedId);

			} catch (IOException ioe) {
				Log.e(TAG, "", ioe);
			} catch (SAXException se) {
				Log.e(TAG, "", se);
			} catch (ParserConfigurationException pce) {
				Log.e(TAG, "", pce);
			}

			lastItem = it.getLastItem(feedId);
			if (lastItem != null)
				lastItemIdAfterUpdate = lastItem.getId();
			if (lastItemIdAfterUpdate > lastItemIdBeforeUpdate)
				newitems = true;
		}
		return newitems;
	}
	
	protected void onPostExecute(Boolean newItems) {
        activity.setSupportProgressBarIndeterminateVisibility(false);
		if (newItems)
			Toast.makeText(activity, "New items found", Toast.LENGTH_SHORT).show();
	}
}
