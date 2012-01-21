/*
 * Copyright (C) 2012 The MusicMod Project
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

package org.musicmod.android.app;

import org.musicmod.android.Constants;
import org.musicmod.android.R;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class MusicSettingsActivity extends PreferenceActivity implements Constants {

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		getPreferenceManager().setSharedPreferencesName(SHAREDPREFS_PREFERENCES);
		addPreferencesFromResource(R.xml.music_settings);
	}

}
