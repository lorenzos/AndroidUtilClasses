package com.lorenzostanco.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.ProgressBar;

/**
 * Simulates a progress overlay with a progress dialog with no borders, frame nor text.
 */
public class ProgressOverlay {
	
	/** Creates and shows a progress dialog with no borders, frame nor text over the default semi-transparent dialogs' background.
	 * @return ProgressDialog The new dialog instance */
	public static ProgressDialog show(final Context context) {
		return show(context, false);
	}

	/** Creates and shows a progress dialog with no borders, frame nor text over the default semi-transparent dialogs' background.
	 * @return ProgressDialog The new dialog instance */
	public static ProgressDialog show(final Context context, final boolean cancelable) {
		return show(context, cancelable, null);
	}

	/** Creates and shows a progress dialog with no borders, frame nor text over the default semi-transparent dialogs' background.
	 * @return ProgressDialog The new dialog instance */
	public static ProgressDialog show(final Context context, final boolean cancelable, final DialogInterface.OnCancelListener cancelListener) {
		final ProgressDialog dialog = ProgressDialog.show(context, null, null, true, cancelable, cancelListener);
		dialog.setContentView(new ProgressBar(context));
		dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
		return dialog;
	}
	
}
