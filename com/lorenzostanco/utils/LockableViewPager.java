package com.lorenzostanco.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/** Un ViewPager orizzontale, la cui paginazione col tocco può essere all'occorrenza bloccata 
 * http://stackoverflow.com/a/7814054/995958 */
public class LockableViewPager extends ViewPager {
	
	private boolean isPagingEnabled = true;
	
	public LockableViewPager(Context context) {
		super(context);
	}
	
	public LockableViewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	@SuppressLint("ClickableViewAccessibility") @Override public boolean onTouchEvent(MotionEvent event) {
		return this.isPagingEnabled && super.onTouchEvent(event);
	}
	
	@Override public boolean onInterceptTouchEvent(MotionEvent event) {
		return this.isPagingEnabled && super.onInterceptTouchEvent(event);
	}
	
	public void setPagingEnabled(boolean b) {
		this.isPagingEnabled = b;
	}
	
	public boolean isPagingEnabled() {
		return isPagingEnabled;
	}
	
}