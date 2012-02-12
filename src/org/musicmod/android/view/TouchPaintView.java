package org.musicmod.android.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class TouchPaintView extends View {
	
	private EventListener mListener;

	private static final int FADE_ALPHA = 0x10;

	/** How often to fade the contents of the window (in ms). */
	private static final int FADE_DELAY = 10;
	private static final int TRACKBALL_SCALE = 10;
	private static final int MAX_FADE_STEPS = 256 / FADE_ALPHA * 2 + 8;
	
	private int mFadeSteps = 0;

	private Bitmap mBitmap;
	private Canvas mCanvas;
	private final Rect mRect = new Rect();
	private Paint mPaint;
	private float mCurX;
	private float mCurY;
	private int mColor = Color.WHITE;

	public TouchPaintView(Context c) {
		super(c);
		init();
	}

	public TouchPaintView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	private void init() {
		setFocusable(true);
		mPaint = new Paint();
		mPaint.setAntiAlias(true);
		mPaint.setColor(Color.TRANSPARENT);
	}

	private void fade() {
		if (mCanvas != null && mFadeSteps < MAX_FADE_STEPS) {
			mCanvas.drawColor(Color.argb(FADE_ALPHA, 0xFF, 0xFF, 0xFF), Mode.DST_OUT);
			invalidate();
			mFadeSteps ++;
		} else if (mFadeSteps >= MAX_FADE_STEPS) {
			mFadeSteps = 0;
			mHandler.removeCallbacksAndMessages(null);
		}
	}
	
	public void setColor(int color) {
		mColor = color;
	}

	@Override
	public void onSizeChanged(int w, int h, int oldw, int oldh) {
		int curW = mBitmap != null ? mBitmap.getWidth() : 0;
		int curH = mBitmap != null ? mBitmap.getHeight() : 0;
		if (curW >= w && curH >= h) {
			return;
		}

		if (curW < w) curW = w;
		if (curH < h) curH = h;

		Bitmap newBitmap = Bitmap.createBitmap(curW, curH, Bitmap.Config.ARGB_8888);
		Canvas newCanvas = new Canvas();
		newCanvas.setBitmap(newBitmap);
		if (mBitmap != null) {
			newCanvas.drawBitmap(mBitmap, 0, 0, null);
		}
		mBitmap = newBitmap;
		mCanvas = newCanvas;
	}

	@Override
	public void onDraw(Canvas canvas) {
		if (mBitmap != null) {
			canvas.drawBitmap(mBitmap, 0, 0, null);
		}
	}

	@Override
	public boolean onTrackballEvent(MotionEvent event) {
		
		if (mListener != null) mListener.onTrackballEvent(event);
		
		int N = event.getHistorySize();
		final float scaleX = event.getXPrecision() * TRACKBALL_SCALE;
		final float scaleY = event.getYPrecision() * TRACKBALL_SCALE;
		for (int i = 0; i < N; i++) {
			mCurX += event.getHistoricalX(i) * scaleX;
			mCurY += event.getHistoricalY(i) * scaleY;
			drawPoint(mCurX, mCurY, 1.0f, 16.0f);
		}
		mCurX += event.getX() * scaleX;
		mCurY += event.getY() * scaleY;
		drawPoint(mCurX, mCurY, 1.0f, 16.0f);
		mFadeSteps = 0;
		mHandler.sendEmptyMessageDelayed(0, FADE_DELAY);
		return true;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		
		if (mListener != null) mListener.onTouchEvent(event);
		
		int action = event.getActionMasked();
		if (action != MotionEvent.ACTION_UP && action != MotionEvent.ACTION_CANCEL) {
			int N = event.getHistorySize();
			int P = event.getPointerCount();
			for (int i = 0; i < N; i++) {
				for (int j = 0; j < P; j++) {
					mCurX = event.getHistoricalX(j, i);
					mCurY = event.getHistoricalY(j, i);
					drawPoint(mCurX, mCurY, event.getHistoricalPressure(j, i),
							event.getHistoricalTouchMajor(j, i));
				}
			}
			for (int j = 0; j < P; j++) {
				mCurX = event.getX(j);
				mCurY = event.getY(j);
				drawPoint(mCurX, mCurY, event.getPressure(j), event.getTouchMajor(j));
			}
		}
		mFadeSteps = 0;
		mHandler.sendEmptyMessageDelayed(0, FADE_DELAY);
		return true;
	}

	private void drawPoint(float x, float y, float pressure, float width) {
		if (width < 1) width = 1;
		if (mBitmap != null) {
			float radius = width / 2;
			int pressureLevel = (int) (Math.sqrt(pressure) * 128);
			mPaint.setARGB(pressureLevel, Color.red(mColor), Color.green(mColor), Color.blue(mColor));
			mCanvas.drawCircle(x, y, radius, mPaint);
			mRect.set((int) (x - radius - 2), (int) (y - radius - 2), (int) (x + radius + 2),
					(int) (y + radius + 2));
			invalidate(mRect);
		}
	}

	public void setEventListener(EventListener listener) {
		mListener = listener;
	}
	
	public interface EventListener {
		boolean onTouchEvent(MotionEvent event);
		boolean onTrackballEvent(MotionEvent event);
	}
	
	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			mHandler.removeCallbacksAndMessages(null);
			if (mFadeSteps < MAX_FADE_STEPS) {
				fade();
				mHandler.sendEmptyMessageDelayed(0, FADE_DELAY);
			} else {
				mFadeSteps = 0;
			}
		}
	};
}