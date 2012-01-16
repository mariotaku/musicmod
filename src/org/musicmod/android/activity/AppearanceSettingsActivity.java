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

package org.musicmod.android.activity;

import org.musicmod.android.Constants;
import org.musicmod.android.R;
import org.musicmod.android.util.SharedPrefs;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

import android.media.AudioManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceClickListener;

public class AppearanceSettingsActivity extends PreferenceActivity implements Constants,
		OnPreferenceClickListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		getPreferenceManager().setSharedPreferencesName(SHAREDPREFS_PREFERENCES);
		addPreferencesFromResource(R.xml.appearance_settings);
		toggleCostomizedColor();
		findPreference(KEY_AUTO_COLOR).setOnPreferenceClickListener(this);

	}

	@Override
	public boolean onPreferenceClick(Preference preference) {

		if (preference == findPreference(KEY_AUTO_COLOR)) {
			toggleCostomizedColor();
		}
		return true;
	}

	private void toggleCostomizedColor() {

		ColorPickerPreference customized_color = (ColorPickerPreference) findPreference(KEY_CUSTOMIZED_COLOR);
		if (new SharedPrefs(this).getBooleanPref(KEY_AUTO_COLOR, true)) {
			customized_color.setEnabled(false);
		} else {
			customized_color.setEnabled(true);
		}
	}
}