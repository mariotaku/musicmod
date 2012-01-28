/*
 * Copyright (C) 2010 Daniel Nilsson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.margaritov.preference.colorpicker;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.BitmapDrawable;
import android.widget.LinearLayout;

public class ColorPickerDialog extends AlertDialog implements
		ColorPickerView.OnColorChangedListener, OnClickListener {

	private ColorPickerView mColorPicker;
	private OnColorChangedListener mListener;

	public interface OnColorChangedListener {

		public void onColorChanged(int color);
	}

	public ColorPickerDialog(Context context, int initialColor) {

		super(context);

		init(initialColor);
	}

	@Override
	public void onColorChanged(int color) {

		setIcon(new BitmapDrawable(getPreviewBitmap(color)));

	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		switch (which) {
			case BUTTON_POSITIVE:
				if (mListener != null) {
					mListener.onColorChanged(mColorPicker.getColor());
				}
				break;
		}
		dismiss();

	}

	private void init(int color) {

		// To fight color branding.
		getWindow().setFormat(PixelFormat.RGBA_8888);

		Context context = getContext();

		LinearLayout mContentView = new LinearLayout(context);
		mContentView.setOrientation(LinearLayout.VERTICAL);

		mColorPicker = new ColorPickerView(context);

		mContentView.addView(mColorPicker);

		mContentView.setPadding(Math.round(mColorPicker.getDrawingOffset()), 0,
				Math.round(mColorPicker.getDrawingOffset()), 0);

		mColorPicker.setOnColorChangedListener(this);
		mColorPicker.setColor(color, true);

		setView(mContentView);

		this.setButton(BUTTON_POSITIVE, context.getString(android.R.string.ok), this);
		this.setButton(BUTTON_NEGATIVE, context.getString(android.R.string.cancel), this);

	}

	public void setAlphaSliderVisible(boolean visible) {

		mColorPicker.setAlphaSliderVisible(visible);
	}

	/**
	 * Set a OnColorChangedListener to get notified when the color selected by
	 * the user has changed.
	 * 
	 * @param listener
	 */
	public void setOnColorChangedListener(OnColorChangedListener listener) {

		mListener = listener;
	}

	public int getColor() {

		return mColorPicker.getColor();
	}

	private Bitmap getPreviewBitmap(int color) {

		int d = (int) (getContext().getResources().getDisplayMetrics().density * 32); // 32dip
		Bitmap bm = Bitmap.createBitmap(d, d, Config.ARGB_8888);
		int w = bm.getWidth();
		int h = bm.getHeight();
		int c = color;
		for (int i = 0; i < w; i++) {
			for (int j = i; j < h; j++) {
				c = (i <= 1 || j <= 1 || i >= w - 2 || j >= h - 2) ? Color.GRAY : color;
				bm.setPixel(i, j, c);
				if (i != j) {
					bm.setPixel(j, i, c);
				}
			}
		}

		return bm;
	}

}
