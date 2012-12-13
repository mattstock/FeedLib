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

public class UpdateFeeds extends AsyncTask<ArrayList<Feed>, Long, Boolean> {
	private static final String TAG = "UpdateFeeds";
	private SherlockFragmentActivity activity;
	private IndicatorCallback callback;
	
	public UpdateFeeds(SherlockFragmentActivity activity) {
		this.activity = activity;

		try {
			callback = (IndicatorCallback) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement IndicatorCallback");
		}
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

				if (ft.updateFeed(handledFeed))
					publishProgress(feedId);
				
				it.cleanDbItems(feedId);

			} catch (IOException ioe) {
				Log.e(TAG, "", ioe);
			} catch (SAXException se) {
				Log.e(TAG, "SAX Error");
			} catch (ParserConfigurationException pce) {
				Log.e(TAG, "Parser Error");
			}

			lastItem = it.getLastItem(feedId);
			if (lastItem != null)
				lastItemIdAfterUpdate = lastItem.getId();
			if (lastItemIdAfterUpdate > lastItemIdBeforeUpdate)
				newitems = true;
		}
		return newitems;
	}
	
	protected void onProgressUpdate(Long... values) {
		callback.refreshUnreadCount();
	}
	
	protected void onPostExecute(Boolean newItems) {
        activity.setSupportProgressBarIndeterminateVisibility(false);
		if (newItems)
			Toast.makeText(activity, "New items found", Toast.LENGTH_SHORT).show();
	}
}
