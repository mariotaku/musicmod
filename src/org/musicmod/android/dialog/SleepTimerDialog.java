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
import org.musicmod.android.util.MusicUtils;
import org.musicmod.android.util.PreferencesEditor;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class SleepTimerDialog extends Activity implements OnSeekBarChangeListener, Constants {

	private SeekBar mSetTime;
	private TextView mTimeView;
	private String mPrompt;
	private int mProgress, mTimerTime, mRemained;

	private AlertDialog mSleepTimerDialog;

	private String action;

	public void onCreate(Bundle icicle) {

		super.onCreate(icicle);

		setContentView(new LinearLayout(this));

		DisplayMetrics dm = new DisplayMetrics();
		dm = getResources().getDisplayMetrics();

		action = getIntent().getAction();

		mSleepTimerDialog = new AlertDialog.Builder(this).create();
		mSleepTimerDialog.setVolumeControlStream(AudioManager.STREAM_MUSIC);

		mRemained = (int) MusicUtils.getSleepTimerRemained() / 1000 / 60;

		LinearLayout mContainer = new LinearLayout(this);
		mContainer.setOrientation(LinearLayout.VERTICAL);
		mContainer.setPadding((int) dm.density * 8, 0, (int) dm.density * 8, 0);
		mTimeView = new TextView(this);
		mContainer.addView(mTimeView);
		mSetTime = new SeekBar(this);
		mSetTime.setMax(120);
		mContainer.addView(mSetTime);

		if (mRemained > 0) {
			mSetTime.setProgress(mRemained);
		} else {
			mSetTime.setProgress(30);
		}

		mSetTime.setOnSeekBarChangeListener(this);

		mProgress = mSetTime.getProgress();
		mTimerTime = mProgress;
		if (mTimerTime >= 1) {
			mPrompt = SleepTimerDialog.this.getResources().getQuantityString(R.plurals.NNNminutes,
					mTimerTime, mTimerTime);
		} else {
			mPrompt = SleepTimerDialog.this.getResources().getString(R.string.disabled);
		}
		mTimeView.setText(mPrompt);

		if (INTENT_SLEEP_TIMER.equals(action)) {
			mSleepTimerDialog.setIcon(android.R.drawable.ic_dialog_info);
			mSleepTimerDialog.setTitle(R.string.set_time);
			mSleepTimerDialog.setView(mContainer);
			mSleepTimerDialog.setButton(Dialog.BUTTON_POSITIVE, getString(android.R.string.ok),
					new OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {

							if (mTimerTime >= 1) {
								long milliseconds = mTimerTime * 60 * 1000;
								boolean gentle = new PreferencesEditor(getApplicationContext())
										.getBooleanPref(KEY_GENTLE_SLEEPTIMER, true);
								MusicUtils.startSleepTimer(milliseconds, gentle);
							} else {
								MusicUtils.stopSleepTimer();
							}
							finish();
						}
					});
			mSleepTimerDialog.setButton(Dialog.BUTTON_NEGATIVE, getString(android.R.string.cancel),
					new OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {

							finish();
						}
					});
			mSleepTimerDialog.setOnCancelListener(new OnCancelListener() {

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
	protected void onResume() {

		super.onResume();
		if (mSleepTimerDialog != null && !mSleepTimerDialog.isShowing()) {
			mSleepTimerDialog.show();
		}
	}

	@Override
	public void onPause() {

		if (mSleepTimerDialog != null && mSleepTimerDialog.isShowing()) {
			mSleepTimerDialog.dismiss();
		}
		super.onPause();
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

		mTimerTime = progress;
		if (progress >= 1) {
			mPrompt = SleepTimerDialog.this.getResources().getQuantityString(R.plurals.NNNminutes,
					mTimerTime, mTimerTime);
		} else {
			mPrompt = SleepTimerDialog.this.getResources().getString(R.string.disabled);
		}
		mTimeView.setText(mPrompt);
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {

	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {

	}

}
