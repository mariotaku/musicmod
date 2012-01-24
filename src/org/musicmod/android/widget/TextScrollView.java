package org.musicmod.android.widget;

import org.musicmod.android.R;
import org.musicmod.android.util.LyricsSplitter;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class TextScrollView extends ScrollView implements OnLongClickListener {

	// Namespaces to read attributes
	private static final String TEXTSCROLLVIEW_NS = "http://schemas.android.com/apk/res/org.musicmod.android";

	private LinearLayout mScrollContainer;
	private LinearLayout mContentContainer, mContentEmptyView;
	private boolean mSmoothScrolling = false;
	private boolean mEnableAutoScrolling = true;
	private int mTextColor = Color.WHITE;
	private float mTextSize = 15.0f;
	private int mLastLineId = -1;
	private String[] mContent;
	private final int TIMEOUT = 1;
	public OnLineSelectedListener mListener;

	public TextScrollView(Context context) {

		super(context);
		init(context);
	}

	public TextScrollView(Context context, AttributeSet attrs) {

		super(context, attrs);
		init(context);
	}

	public interface OnLineSelectedListener {

		void onLineSelected(int id);
	}

	public void registerLineSelectedListener(OnLineSelectedListener listener) {

		mListener = listener;
	}

	public void unregisterLineSelectedListener(OnLineSelectedListener listener) {

		mListener = listener;
	}

	private void init(Context context) {

		setVerticalScrollBarEnabled(false);
		mScrollContainer = new LinearLayout(context);
		mScrollContainer.setOrientation(LinearLayout.VERTICAL);
		addView(mScrollContainer, new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT));

		mContentContainer = new LinearLayout(context);
		mContentContainer.setOrientation(LinearLayout.VERTICAL);
		mContentContainer.setPadding(0, getHeight() / 2, 0, getHeight() / 2);
		mScrollContainer.addView(mContentContainer, new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.WRAP_CONTENT));

		mContentEmptyView = (LinearLayout) inflate(context, R.layout.content_empty_view, null);
		mContentEmptyView.setLayoutParams(new LayoutParams(getWidth(), getHeight()));
		mScrollContainer.addView(mContentEmptyView, new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.WRAP_CONTENT, Gravity.CENTER));
	}

	public void setTextContent(String[] content, Context context) {

		mContent = content;
		mLastLineId = -1;
		mContentContainer.removeAllViews();
		System.gc();

		if (content == null || content.length == 0) {
			mContentContainer.setVisibility(View.GONE);
			mContentEmptyView.setVisibility(View.VISIBLE);
			return;
		}

		mContentContainer.setVisibility(View.VISIBLE);
		mContentEmptyView.setVisibility(View.GONE);

		int content_id = 0;

		for (String line : content) {
			TextView mTextView = new TextView(context);
			mTextView.setText(LyricsSplitter.split(line, mTextView.getTextSize()));
			mTextView.setTextColor(Color.argb(0xD0, Color.red(mTextColor), Color.green(mTextColor),
					Color.blue(mTextColor)));
			float density = getResources().getDisplayMetrics().density;
			mTextView.setShadowLayer(4 * density, 0, 0, Color.BLACK);
			mTextView.setGravity(Gravity.CENTER);
			mTextView.setTextSize(mTextSize);
			mTextView.setOnLongClickListener(this);
			if (content_id < content.length) {
				mTextView.setTag(content_id);
				content_id++;
			}
			mContentContainer.addView(mTextView, new LayoutParams(LayoutParams.WRAP_CONTENT,
					LayoutParams.WRAP_CONTENT, Gravity.CENTER));
		}

	}

	public void setContentGravity(int gravity) {

		if (mContentContainer != null) {
			mContentContainer.setGravity(gravity);
		}
	}

	public void setTextColor(int color, Context context) {

		mTextColor = color;
		setTextContent(mContent, context);
		setCurrentLine(mLastLineId, true);
	}

	public void setTextSize(float size, Context context) {

		mTextSize = size;
		setTextContent(mContent, context);
		setCurrentLine(mLastLineId, true);
	}

	public void setCurrentLine(int lineid, boolean force) {

		if (findViewWithTag(mLastLineId) != null) {
			((TextView) findViewWithTag(mLastLineId)).setTextColor(Color.argb(0xD0,
					Color.red(mTextColor), Color.green(mTextColor), Color.blue(mTextColor)));
			((TextView) findViewWithTag(mLastLineId)).getPaint().setFakeBoldText(false);
		}

		if (findViewWithTag(lineid) != null) {
			((TextView) findViewWithTag(lineid)).setTextColor(Color.argb(0xFF,
					Color.red(mTextColor), Color.green(mTextColor), Color.blue(mTextColor)));
			((TextView) findViewWithTag(lineid)).getPaint().setFakeBoldText(true);
			if (mEnableAutoScrolling || force) {
				if (mSmoothScrolling) {
					smoothScrollTo(0, findViewWithTag(lineid).getTop()
							+ findViewWithTag(lineid).getHeight() / 2 - getHeight() / 2);
				} else {
					scrollTo(0, findViewWithTag(lineid).getTop()
							+ findViewWithTag(lineid).getHeight() / 2 - getHeight() / 2);
				}
			}
			mLastLineId = lineid;
		}

	}

	@Override
	public boolean onLongClick(View view) {

		if (mListener != null) {
			Object tag = view.getTag();
			int id = tag != null ? Integer.valueOf(tag.toString()) : 0;
			mListener.onLineSelected(id);
		}
		return true;
	}

	@Override
	public void setSmoothScrollingEnabled(boolean smooth) {

		mSmoothScrolling = smooth;
	}

	@Override
	public void onSizeChanged(int width, int height, int old_width, int old_height) {

		mContentEmptyView.setLayoutParams(new LinearLayout.LayoutParams(width, height));
		mContentContainer.setPadding(0, height / 2, 0, height / 2);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		switch (event.getAction()) {
			case MotionEvent.ACTION_UP:
				mHandler.sendEmptyMessageDelayed(TIMEOUT, 2000L);
				break;
			case MotionEvent.ACTION_DOWN:
				mHandler.removeMessages(TIMEOUT);
				mEnableAutoScrolling = false;
				break;
			case MotionEvent.ACTION_MOVE:
				mHandler.removeMessages(TIMEOUT);
				mEnableAutoScrolling = false;
				break;
		}
		return super.onTouchEvent(event);

	}

	public Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {

			switch (msg.what) {
				case TIMEOUT:
					mHandler.removeMessages(TIMEOUT);
					mEnableAutoScrolling = true;
					break;
			}
		}
	};
}