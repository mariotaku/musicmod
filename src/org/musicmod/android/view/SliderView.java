package org.musicmod.android.view;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.GestureDetector.OnGestureListener;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;


public class SliderView extends View implements OnGestureListener, OnTouchListener{

	private int mHeight = 0;
	private int mMaxValue = 16;
	private float mDelta = 0;
	private int mColor = Color.WHITE;
	private GestureDetector mGestureDetector;
	
	private OnValueChangeListener mListener;
	
	public SliderView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	public SliderView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public SliderView(Context context) {
		super(context);
		init(context);
	}
	
	@Override
	public boolean onTouch(View v, MotionEvent event) {

		int action = event.getAction();
		if (action == MotionEvent.ACTION_DOWN) {
			v.setBackgroundColor(Color.argb(0x33, Color.red(mColor), Color.green(mColor),
					Color.blue(mColor)));
		} else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
			v.setBackgroundColor(Color.TRANSPARENT);
		}
		mGestureDetector.onTouchEvent(event);
		return true;
	}
	
	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {

	}

	@Override
	public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX,
			float distanceY) {

		if (mDelta >= 1 || mDelta <= -1) {
			if (mListener != null) mListener.onValueChanged((int) mDelta);
			mDelta = 0;
		} else {
			mDelta += mMaxValue * distanceY / mHeight * 2;
		}
		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {
		mDelta = 0;
	}

	@Override
	public boolean onDown(MotionEvent e) {
		mDelta = 0;
		return false;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		return false;
	}
	
	@Override
	public void onSizeChanged(int w, int h, int oldw, int oldh) {
		mHeight = getHeight();
	}
	
	private void init(Context context) {
		setOnTouchListener(this);
		mGestureDetector = new GestureDetector(context, this);
		mHeight = getHeight();
	}
	
	public void setMax(int max) {
		mMaxValue = max;
	}
	
	public void setColor(int color) {
		mColor = color;
	}
	
	public void setOnValueChangeListener(OnValueChangeListener listener) {
		mListener = listener;
	}
	
	public interface OnValueChangeListener {
		void onValueChanged(int value);
	}

}
