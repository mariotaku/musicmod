/*
 *			Copyright (C) 2011-2012 The MusicMod Project
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

import org.musicmod.android.Constants;
import org.musicmod.android.R;

import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.MenuItem;

import org.mariotaku.actionbarcompat.ActionBarPreferenceActivity;

public class AppearanceSettingsActivity extends ActionBarPreferenceActivity implements Constants {

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		getPreferenceManager().setSharedPreferencesName(SHAREDPREFS_PREFERENCES);
		addPreferencesFromResource(R.xml.appearance_settings);

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		Intent intent;
		switch (item.getItemId()) {
			case GOTO_HOME:
				intent = new Intent(INTENT_PLAYBACK_VIEWER);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intent);
				finish();
				break;
		}
		return super.onOptionsItemSelected(item);
	}

}