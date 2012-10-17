package com.bexkat.feedlib;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;

public class ItemDetailFragment extends SherlockFragment {
	Activity mActivity;

	private static final String TAG = "ItemDetailFragment";

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mActivity = activity;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.item_detail, container,
				false);
		return v;
	}

	public void updateState(Bundle b) {
		TextView tv = (TextView) mActivity.findViewById(R.id.item_content);
		tv.setText(b.getString("content"));
		tv = (TextView) mActivity.findViewById(R.id.item_pubdate);
		tv.setText(b.getString("pubDate"));
		tv = (TextView) mActivity.findViewById(R.id.item_title);
		tv.setText(b.getString("title"));
	}
}
