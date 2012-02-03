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

public class VisualizerViewWaveForm extends View {

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

	private byte[] mBytes = null;
	private float[] mPoints;
	private Rect mRect = new Rect();
	private Paint mForePaint = new Paint();

	public VisualizerViewWaveForm(Context context) {
		super(context);

		mAntiAlias = DEFAULT_ANTIALIAS;
		mColor = DEFAULT_COLOR;
		mForePaint.setStrokeWidth(1.0f);
		setAntiAlias(mAntiAlias);
		setColor(mColor);
	}

	public VisualizerViewWaveForm(Context context, AttributeSet attrs) {
		super(context, attrs);

		// Read parameters from attributes
		mAntiAlias = attrs.getAttributeBooleanValue(VISUALIZER_NS, ATTR_ANTIALIAS,
				DEFAULT_ANTIALIAS);
		mColor = attrs.getAttributeIntValue(VISUALIZER_NS, ATTR_COLOR, DEFAULT_COLOR);

		mForePaint.setStrokeWidth(1.0f);
		setAntiAlias(mAntiAlias);
		setColor(mColor);
	}

	public void updateVisualizer(byte[] bytes) {
		mBytes = bytes;
		invalidate();
	}

	public void setAntiAlias(boolean antialias) {
		mForePaint.setAntiAlias(antialias);
	}

	public void setColor(int color) {
		mForePaint.setColor(Color.argb(0xFF, Color.red(color), Color.green(color),
				Color.blue(color)));
	}

	@Override
	protected void onSizeChanged(int width, int height, int old_width, int old_height) {
		mForePaint.setStrokeWidth(1.0f);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		if (mBytes == null) {
			return;
		}

		mRect.setEmpty();

		if (mPoints == null || mPoints.length < mBytes.length * 4) {
			mPoints = new float[mBytes.length * 4];
		}

		mRect.set(0, 0, getWidth(), getHeight());

		for (int i = 0; i < mBytes.length - 1; i++) {
			mPoints[i * 4] = mRect.width() * i / (mBytes.length - 1);
			mPoints[i * 4 + 1] = mRect.height() / 2 + ((byte) (mBytes[i] + 128))
					* (mRect.height() / 2) / 128;
			mPoints[i * 4 + 2] = mRect.width() * (i + 1) / (mBytes.length - 1);
			mPoints[i * 4 + 3] = mRect.height() / 2 + ((byte) (mBytes[i + 1] + 128))
					* (mRect.height() / 2) / 128;
		}
		canvas.drawLines(mPoints, mForePaint);

	}
}
