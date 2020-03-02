package com.lorenzostanco.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;

// For AndroidX projects:
// import androidx.annotation.NonNull;
// import androidx.viewpager.widget.ViewPager;

/** Un ViewPager orizzontale, la cui paginazione col tocco può essere all'occorrenza bloccata 
 * http://stackoverflow.com/a/7814054/995958 */
@SuppressWarnings("unused") public class LockableViewPager extends ViewPager {
	
	private boolean isPagingEnabled = true;
	
	public LockableViewPager(final Context context) {
		super(context);
	}
	
	public LockableViewPager(final Context context, final AttributeSet attrs) {
		super(context, attrs);
	}
	
	@SuppressLint("ClickableViewAccessibility") @Override public boolean onTouchEvent(final MotionEvent event) {
		return this.isPagingEnabled && super.onTouchEvent(event);
	}
	
	@Override public boolean onInterceptTouchEvent(final MotionEvent event) {
		return this.isPagingEnabled && super.onInterceptTouchEvent(event);
	}

	@Override public boolean executeKeyEvent(@NonNull final KeyEvent event) {
		return false;
	}
	
	public void setPagingEnabled(final boolean b) {
		this.isPagingEnabled = b;
	}
	
	public boolean isPagingEnabled() {
		return isPagingEnabled;
	}
	
}
