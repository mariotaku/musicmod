/*
 *              Copyright (C) 2011 The MusicMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *            http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.musicmod.android.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

public class VisualizerViewFftSpectrum extends View {

	// Namespaces to read attributes
	private static final String VISUALIZER_NS = "http://schemas.android.com/apk/res/org.musicmod.android";

	// Attribute names
	private static final String ATTR_ANTIALIAS = "antialias";
	private static final String ATTR_COLOR = "color";

	// Default values for defaults
	private static final boolean DEFAULT_ANTIALIAS = true;
	private static final int DEFAULT_COLOR = Color.WHITE;

	// Real defaults
	private final boolean mAntiAlias;
	private final int mColor;

	private short[] mBytes = null;
	private float[] mPoints;
	private Rect mRect = new Rect();
	private Paint mForePaint = new Paint();

	public VisualizerViewFftSpectrum(Context context) {

		super(context);

		mAntiAlias = DEFAULT_ANTIALIAS;
		mColor = DEFAULT_COLOR;
		setAntiAlias(mAntiAlias);
		setColor(mColor);
	}

	public VisualizerViewFftSpectrum(Context context, AttributeSet attrs) {

		super(context, attrs);

		// Read parameters from attributes
		mAntiAlias = attrs.getAttributeBooleanValue(VISUALIZER_NS, ATTR_ANTIALIAS,
				DEFAULT_ANTIALIAS);
		mColor = attrs.getAttributeIntValue(VISUALIZER_NS, ATTR_COLOR, DEFAULT_COLOR);
		setAntiAlias(mAntiAlias);
		setColor(mColor);
	}

	public void updateVisualizer(short[] bytes) {

		mBytes = bytes;
		invalidate();
	}

	public void setAntiAlias(boolean antialias) {

		mForePaint.setAntiAlias(antialias);
	}

	public void setColor(int color) {

		mForePaint.setColor(Color.argb(0xA0, Color.red(color), Color.green(color),
				Color.blue(color)));
	}

	@Override
	protected void onDraw(Canvas canvas) {

		super.onDraw(canvas);

		if (mBytes == null) {
			return;
		}

		mRect.setEmpty();

		if (mPoints == null) {
			mPoints = new float[mBytes.length * 8];
		}

		short[] mAnalyzer = new short[256];
		// We always get 256 points
		for (int i = 0; i < 256; i++) {
			short newval = (short) (mBytes[i] * (i / 16 + 2));
			short oldval = mAnalyzer[i];
			if (newval >= oldval - 800) {
				// use new high value
			} else {
				newval = (short) (oldval - 800);
			}
			mAnalyzer[i] = newval;
		}

		// distribute the data over mWidth samples in the middle of the
		// mPointData array
		final int outlen = mPoints.length / 8;
		final int width = getWidth();
		final int skip = (outlen - getWidth()) / 2;

		int srcidx = 0;
		int cnt = 0;
		for (int i = 0; i < width; i++) {
			float val = mAnalyzer[srcidx] / 50;
			if (val < 1f && val > -1f) val = 1;
			mPoints[(i + skip) * 8 + 1] = val;
			mPoints[(i + skip) * 8 + 5] = -val;
			cnt += 256;
			if (cnt > width) {
				srcidx++;
				cnt -= width;
			}
		}

		canvas.drawLines(mPoints, mForePaint);

	}
}
