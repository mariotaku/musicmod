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

package org.musicmod.android.app;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import org.musicmod.android.Constants;
import org.musicmod.android.IMusicPlaybackService;
import org.musicmod.android.R;
import org.musicmod.android.util.*;

public class EqualizerActivity extends Activity implements Constants {

	private IMusicPlaybackService mService = null;
	private ServiceToken mToken;
	private EqualizerWrapper mEqualizer;
	private ScrollView mScrollView;
	private LinearLayout mLinearLayout;
	private int mAudioSessionId;
	private PreferencesEditor mPrefs;

	@Override
	public void onCreate(Bundle icicle) {

		super.onCreate(icicle);

		mPrefs = new PreferencesEditor(getApplicationContext());

		requestWindowFeature(Window.FEATURE_NO_TITLE);

		mScrollView = new ScrollView(this);
		mScrollView.setVerticalScrollBarEnabled(false);

		setContentView(mScrollView);

		mLinearLayout = new LinearLayout(this);
		mLinearLayout.setOrientation(LinearLayout.VERTICAL);

		mScrollView.addView(mLinearLayout);

		DisplayMetrics dm = new DisplayMetrics();
		dm = getResources().getDisplayMetrics();

		getWindow().setLayout((int) (300 * dm.density), WindowManager.LayoutParams.WRAP_CONTENT);

	}

	@Override
	protected void onStart() {

		super.onStart();

		setVolumeControlStream(AudioManager.STREAM_MUSIC);

		mToken = MusicUtils.bindToService(this, osc);
		if (mToken == null) {
			finish();
		}

		if (mService != null) {
			try {
				mAudioSessionId = mService.getAudioSessionId();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		setupEqualizerFxAndUI(mAudioSessionId);
	}

	@Override
	protected void onStop() {

		reloadEqualizer();
		if (mEqualizer != null) {
			mEqualizer.release();
		}
		MusicUtils.unbindFromService(mToken);
		mService = null;
		super.onStop();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		MenuInflater mInflater = getMenuInflater();
		mInflater.inflate(R.menu.equalizer, menu);

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {

		MenuItem MENU_PRESETS = menu.findItem(EQUALIZER_PRESETS);
		if (mEqualizer != null) {
			if (mEqualizer.getNumberOfPresets() <= 0) {
				MENU_PRESETS.setVisible(false);
			} else {
				MENU_PRESETS.setVisible(true);
			}
		}
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
			case EQUALIZER_PRESETS:
				showPresets();
				break;
			case EQUALIZER_RESET:
				resetEqualizer();
				break;

		}
		return super.onOptionsItemSelected(item);
	}

	private void setupEqualizerFxAndUI(int audioSessionId) {

		// Create the Equalizer object (an AudioEffect subclass) and attach it
		// to our media player, with a default priority (0).
		mEqualizer = new EqualizerWrapper(0, audioSessionId);
		if (mEqualizer == null) {
			finish();
			return;
		}
		mEqualizer.setEnabled(false);

		short bands = mEqualizer.getNumberOfBands();

		final short minEQLevel = mEqualizer.getBandLevelRange()[0];
		final short maxEQLevel = mEqualizer.getBandLevelRange()[1];

		mLinearLayout.removeAllViews();

		for (short i = 0; i < bands; i++) {
			final short band = i;

			TextView freqTextView = new TextView(this);
			freqTextView.setLayoutParams(new ViewGroup.LayoutParams(
					ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
			freqTextView.setGravity(Gravity.CENTER_HORIZONTAL);

			if (mEqualizer.getCenterFreq(band) / 1000 < 1000) {
				freqTextView.setText((mEqualizer.getCenterFreq(band) / 1000) + " Hz");
			} else {
				freqTextView.setText(((float) mEqualizer.getCenterFreq(band) / 1000 / 1000)
						+ " KHz");
			}

			mLinearLayout.addView(freqTextView);

			LinearLayout row = new LinearLayout(this);
			row.setOrientation(LinearLayout.HORIZONTAL);

			TextView minDbTextView = new TextView(this);
			minDbTextView.setLayoutParams(new ViewGroup.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
			minDbTextView.setText((minEQLevel / 100) + " dB");

			TextView maxDbTextView = new TextView(this);
			maxDbTextView.setLayoutParams(new ViewGroup.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
			maxDbTextView.setText((maxEQLevel / 100) + " dB");

			LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			layoutParams.weight = 1;
			SeekBar bar = new SeekBar(this);
			bar.setLayoutParams(layoutParams);
			bar.setMax(maxEQLevel - minEQLevel);
			bar.setProgress(mPrefs.getEqualizerSetting(band,
					(short) ((maxEQLevel + minEQLevel) / 2)) - minEQLevel);
			mEqualizer.setBandLevel(band,
					mPrefs.getEqualizerSetting(band, (short) ((maxEQLevel + minEQLevel) / 2)));

			bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

					mEqualizer.setBandLevel(band, (short) (minEQLevel + progress));
					mPrefs.setEqualizerSetting(band, (short) (minEQLevel + progress));
					reloadEqualizer();
				}

				public void onStartTrackingTouch(SeekBar seekBar) {

				}

				public void onStopTrackingTouch(SeekBar seekBar) {

				}
			});

			row.addView(minDbTextView);
			row.addView(bar);
			row.addView(maxDbTextView);

			mLinearLayout.addView(row);
		}
	}

	private void reloadEqualizer() {

		try {
			mService.reloadEqualizer();
		} catch (Exception e) {
		}
	}

	private void showPresets() {

		if (mEqualizer != null) {
			List<String> mPresetsList = new ArrayList<String>();
			String mPresetName;
			for (short preset_id = 0; preset_id < mEqualizer.getNumberOfPresets(); preset_id++) {
				mPresetName = mEqualizer.getPresetName(preset_id);
				mPresetsList.add(mPresetName);
			}

			CharSequence[] mPresetsItems = mPresetsList.toArray(new CharSequence[mPresetsList
					.size()]);

			new AlertDialog.Builder(this).setTitle(R.string.equalizer_presets)
					.setItems(mPresetsItems, new DialogInterface.OnClickListener() {

						public void onClick(DialogInterface dialog, int item) {

							setPreset((short) item);
							setupEqualizerFxAndUI(mAudioSessionId);
							reloadEqualizer();
						}
					}).show();
		}
	}

	private void setPreset(short preset_id) {

		if (mEqualizer != null) {
			mEqualizer.usePreset(preset_id);
			for (short i = 0; i < mEqualizer.getNumberOfBands(); i++) {
				short band = i;
				mPrefs.setEqualizerSetting(band, mEqualizer.getBandLevel(band));
			}
		}
	}

	private void resetEqualizer() {

		if (mEqualizer != null) {
			short bands = mEqualizer.getNumberOfBands();

			final short minEQLevel = mEqualizer.getBandLevelRange()[0];
			final short maxEQLevel = mEqualizer.getBandLevelRange()[1];

			for (short i = 0; i < bands; i++) {
				final short band = i;
				mEqualizer.setBandLevel(band, (short) ((maxEQLevel + minEQLevel) / 2));
				mPrefs.setEqualizerSetting(band, (short) ((maxEQLevel + minEQLevel) / 2));
			}
			reloadEqualizer();
			setupEqualizerFxAndUI(mAudioSessionId);
		}
	}

	private ServiceConnection osc = new ServiceConnection() {

		public void onServiceConnected(ComponentName classname, IBinder obj) {

			mService = IMusicPlaybackService.Stub.asInterface(obj);
		}

		public void onServiceDisconnected(ComponentName classname) {

			mService = null;
		}
	};
}
