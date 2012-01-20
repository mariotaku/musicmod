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

package org.musicmod.android.dialog;

import org.musicmod.android.Constants;
import org.musicmod.android.R;
import org.musicmod.android.util.PreferencesEditor;
import org.musicmod.android.view.VerticalTextSpinner;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.Toast;

public class WeekSelectorDialog extends Activity implements OnClickListener, Constants {

	private AlertDialog mAlert;

	private String action;
	private VerticalTextSpinner mVerticalTextSpinner;
	private PreferencesEditor mPrefs;

	@Override
	public void onCreate(Bundle icicle) {

		super.onCreate(icicle);

		mPrefs = new PreferencesEditor(this);

		setContentView(new LinearLayout(this));

		DisplayMetrics dm = new DisplayMetrics();
		dm = getResources().getDisplayMetrics();

		action = getIntent().getAction();

		mAlert = new AlertDialog.Builder(this).create();
		mAlert.setVolumeControlStream(AudioManager.STREAM_MUSIC);

		if (INTENT_WEEK_SELECTOR.equals(action)) {

			LinearLayout mContainer = new LinearLayout(this);
			mContainer.setGravity(Gravity.CENTER);

			mVerticalTextSpinner = new VerticalTextSpinner(this);
			mVerticalTextSpinner.setItems(getResources().getStringArray(R.array.weeklist));
			mVerticalTextSpinner.setWrapAround(true);
			mVerticalTextSpinner.setScrollInterval(200);
			int def = mPrefs.getIntPref(PREF_KEY_NUMWEEKS, 2);
			int pos = icicle != null ? icicle.getInt("numweeks", def - 1) : def - 1;
			mVerticalTextSpinner.setSelectedPos(pos);
			mContainer.addView(mVerticalTextSpinner, (int) (120 * dm.density),
					(int) (100 * dm.density));

			mAlert.setIcon(android.R.drawable.ic_dialog_info);
			mAlert.setTitle(R.string.set_time);
			mAlert.setView(mContainer);
			mAlert.setButton(Dialog.BUTTON_POSITIVE, getString(android.R.string.ok), this);
			mAlert.setButton(Dialog.BUTTON_NEGATIVE, getString(android.R.string.cancel),
					new OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {

							finish();
						}
					});
			mAlert.setOnCancelListener(new OnCancelListener() {

				@Override
				public void onCancel(DialogInterface dialog) {

					finish();
				}
			});
		} else {
			Toast.makeText(this, R.string.error_bad_parameters, Toast.LENGTH_SHORT).show();
			finish();
		}

	}

	@Override
	public void onSaveInstanceState(Bundle outcicle) {

		outcicle.putInt("numweeks", mVerticalTextSpinner.getCurrentSelectedPos());
	}

	@Override
	protected void onResume() {

		super.onResume();
		if (mAlert != null && !mAlert.isShowing()) {
			mAlert.show();
		}
	}

	@Override
	public void onPause() {

		if (mAlert != null && mAlert.isShowing()) {
			mAlert.dismiss();
		}
		super.onPause();
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {

		int numweeks = mVerticalTextSpinner.getCurrentSelectedPos() + 1;
		mPrefs.setIntPref(PREF_KEY_NUMWEEKS, numweeks);
		setResult(RESULT_OK);
		finish();
	}
}
