package com.bexkat.feedlib;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import com.actionbarsherlock.app.SherlockDialogFragment;

public class AboutFragment extends SherlockDialogFragment {

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Bundle b = getArguments();
		String title = b.getString("title");
		String description = b.getString("desc");
		
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setMessage(description)
				.setTitle(title)
				.setPositiveButton(R.string.ok,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface arg0, int arg1) {
							}

						});
		;
		return builder.create();
	}

}
