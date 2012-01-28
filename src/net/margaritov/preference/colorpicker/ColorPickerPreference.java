/*
 * Copyright (C) 2011 Sergey Margaritov
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

import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Bitmap.Config;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

/**
 * A preference type that allows a user to choose a time
 * 
 * @author Sergey Margaritov
 */
public class ColorPickerPreference extends Preference implements
		Preference.OnPreferenceClickListener, ColorPickerDialog.OnColorChangedListener {

	private View mView;
	private int mDefaultValue = Color.WHITE;
	private int mValue = Color.WHITE;
	private String mTitle = null;
	private float mDensity = 0;
	private boolean mAlphaSliderEnabled = false;

	private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";
	private static final String ATTR_DEFAULTVALUE = "defaultValue";
	private static final String ATTR_ALPHASLIDER = "alphaSlider";
	private static final String ATTR_DIALOGTITLE = "dialogTitle";
	private static final String ATTR_TITLE = "title";

	public ColorPickerPreference(Context context) {

		super(context);
		init(context, null);
	}

	public ColorPickerPreference(Context context, AttributeSet attrs) {

		super(context, attrs);
		init(context, attrs);
	}

	public ColorPickerPreference(Context context, AttributeSet attrs, int defStyle) {

		super(context, attrs, defStyle);
		init(context, attrs);
	}

	@Override
	protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {

		onColorChanged(restoreValue ? getValue() : (Integer) defaultValue);
	}

	private void init(Context context, AttributeSet attrs) {

		mDensity = getContext().getResources().getDisplayMetrics().density;
		setOnPreferenceClickListener(this);
		if (attrs != null) {
			try {
				mTitle = context.getString(attrs.getAttributeResourceValue(ANDROID_NS,
						ATTR_DIALOGTITLE, -1));
			} catch (NotFoundException e) {
				mTitle = attrs.getAttributeValue(ANDROID_NS, ATTR_DIALOGTITLE);
			}

			if (mTitle == null) {
				try {
					mTitle = context.getString(attrs.getAttributeResourceValue(ANDROID_NS,
							ATTR_TITLE, -1));
				} catch (NotFoundException e) {
					mTitle = attrs.getAttributeValue(ANDROID_NS, ATTR_TITLE);
				}
			}

			String defaultValue = attrs.getAttributeValue(ANDROID_NS, ATTR_DEFAULTVALUE);
			if (defaultValue.startsWith("#")) {
				try {
					mDefaultValue = convertToColorInt(defaultValue);
				} catch (NumberFormatException e) {
					Log.e("ColorPickerPreference", "Wrong color: " + defaultValue);
					mDefaultValue = Color.WHITE;
				}
			} else {
				int colorResourceId = attrs.getAttributeResourceValue(ANDROID_NS,
						ATTR_DEFAULTVALUE, 0);
				if (colorResourceId != 0) {
					mDefaultValue = context.getResources().getColor(colorResourceId);
				}
			}
			mAlphaSliderEnabled = attrs.getAttributeBooleanValue(null, ATTR_ALPHASLIDER, false);
		}
		mValue = mDefaultValue;
	}

	@Override
	protected void onBindView(View view) {

		super.onBindView(view);
		mView = view;
		setPreviewColor();
	}

	private void setPreviewColor() {

		if (mView == null) return;
		ImageView iView = new ImageView(getContext());
		LinearLayout widgetFrameView = ((LinearLayout) mView
				.findViewById(android.R.id.widget_frame));
		if (widgetFrameView == null) return;
		widgetFrameView.setPadding(widgetFrameView.getPaddingLeft(),
				widgetFrameView.getPaddingTop(), (int) (mDensity * 8),
				widgetFrameView.getPaddingBottom());
		// remove already create preview image
		int count = widgetFrameView.getChildCount();
		if (count > 0) {
			widgetFrameView.removeViews(0, count);
		}
		widgetFrameView.addView(iView);
		iView.setBackgroundDrawable(new AlphaPatternDrawable((int) (5 * mDensity)));
		iView.setImageBitmap(getPreviewBitmap());
	}

	private Bitmap getPreviewBitmap() {

		int d = (int) (mDensity * 31); // 30dip
		int color = getValue();
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

	public int getValue() {

		try {
			if (isPersistent()) {
				mValue = getPersistedInt(mDefaultValue);
			}
		} catch (ClassCastException e) {
			mValue = mDefaultValue;
		}

		return mValue;
	}

	@Override
	public void onColorChanged(int color) {

		if (isPersistent()) {
			persistInt(color);
		}
		mValue = color;
		setPreviewColor();
		try {
			getOnPreferenceChangeListener().onPreferenceChange(this, color);
		} catch (NullPointerException e) {

		}
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {

		ColorPickerDialog dialog = new ColorPickerDialog(getContext(), getValue());
		if (mTitle != null) {
			dialog.setTitle(mTitle);
		} else {
			dialog.setTitle("Set Color");
		}
		dialog.setOnColorChangedListener(this);
		if (mAlphaSliderEnabled) {
			dialog.setAlphaSliderVisible(true);
		}
		dialog.show();

		return false;
	}

	/**
	 * Toggle Alpha Slider visibility (by default it's disabled)
	 * 
	 * @param enable
	 */
	public void setAlphaSliderEnabled(boolean enable) {

		mAlphaSliderEnabled = enable;
	}

	/**
	 * For custom purposes. Not used by ColorPickerPreferrence
	 * 
	 * @param color
	 * @author Unknown
	 */
	public static String convertToARGB(int color) {

		String alpha = Integer.toHexString(Color.alpha(color));
		String red = Integer.toHexString(Color.red(color));
		String green = Integer.toHexString(Color.green(color));
		String blue = Integer.toHexString(Color.blue(color));

		if (alpha.length() == 1) {
			alpha = "0" + alpha;
		}

		if (red.length() == 1) {
			red = "0" + red;
		}

		if (green.length() == 1) {
			green = "0" + green;
		}

		if (blue.length() == 1) {
			blue = "0" + blue;
		}

		return "#" + alpha + red + green + blue;
	}

	/**
	 * For custom purposes. Not used by ColorPickerPreferrence
	 * 
	 * @param argb
	 * @throws NumberFormatException
	 * @author Unknown
	 */
	public static int convertToColorInt(String argb) throws NumberFormatException {

		if (argb.startsWith("#")) {
			argb = argb.replace("#", "");
		}

		int alpha = -1, red = -1, green = -1, blue = -1;

		if (argb.length() == 8) {
			alpha = Integer.parseInt(argb.substring(0, 2), 16);
			red = Integer.parseInt(argb.substring(2, 4), 16);
			green = Integer.parseInt(argb.substring(4, 6), 16);
			blue = Integer.parseInt(argb.substring(6, 8), 16);
		} else if (argb.length() == 6) {
			alpha = 255;
			red = Integer.parseInt(argb.substring(0, 2), 16);
			green = Integer.parseInt(argb.substring(2, 4), 16);
			blue = Integer.parseInt(argb.substring(4, 6), 16);
		}

		return Color.argb(alpha, red, green, blue);
	}

}