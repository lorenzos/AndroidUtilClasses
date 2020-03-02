package com.lorenzostanco.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.widget.Toast;

/**
 * Enqueue toasts in order to avoid Toast overlapping occurring in Android 8.1+
 * https://issuetracker.google.com/issues/79159357
 */
@SuppressWarnings("unused") public class ToastQueue {

	private static final int LONG_DELAY = 3500; 
	private static final int SHORT_DELAY = 2000;

	private static long nextAvailableTime = 0;
	
	private static final Object lock = new Object();

	/** Shows a toast, delaying it in order to avoid Toast overlapping occurring in Android 8.1+ */
	@SuppressLint("ShowToast") public static void enqueue(final Context c, final int message, final int length) {
		enqueue(Toast.makeText(c, message, length));
	}

	/** Shows a toast, delaying it in order to avoid Toast overlapping occurring in Android 8.1+ */
	@SuppressLint("ShowToast") public static void enqueue(final Context c, final String message, final int length) {
		enqueue(Toast.makeText(c, message, length));
	}
	
	/** Shows an already built toast, delaying it in order to avoid Toast overlapping occurring in Android 8.1+ */
	public static void enqueue(final Toast toast) {
		
		// Compute delay
		final long delay;
		synchronized (lock) {
			final long duration = toast.getDuration() == Toast.LENGTH_LONG ? LONG_DELAY : SHORT_DELAY;
			final long now = System.currentTimeMillis();
			if (nextAvailableTime < now) {
				delay = 0;
				nextAvailableTime = now + duration;
			} else {
				delay = nextAvailableTime - now;
				nextAvailableTime += duration;
			}
		}
		
		// Show the toast, delaying if necessary
		try {
			new Handler().postDelayed(new Runnable() {
				@Override public void run() {
					try {
						toast.show();
					} catch (Exception ignored) { }
				}
			}, delay);
		} catch (Exception ignored) { }
		
	}
	
}
