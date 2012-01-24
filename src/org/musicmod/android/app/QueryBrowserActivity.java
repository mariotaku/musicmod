/*
 * Copyright (C) 2007 The Android Open Source Project
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

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;

import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;

import org.musicmod.android.Constants;
import org.musicmod.android.util.MusicUtils;
import org.musicmod.android.util.ServiceToken;

public class QueryBrowserActivity extends FragmentActivity implements Constants, ServiceConnection {

	private ServiceToken mToken;
	private Intent intent;
	private Bundle bundle;

	@Override
	public void onCreate(Bundle icicle) {

		super.onCreate(icicle);

		setVolumeControlStream(AudioManager.STREAM_MUSIC);

		intent = getIntent();
		bundle = icicle != null ? icicle : intent.getExtras();

		if (bundle == null) {
			bundle = new Bundle();
		}

		if (bundle.getString(INTENT_KEY_ACTION) == null)
			bundle.putString(INTENT_KEY_ACTION, intent.getAction());
		if (bundle.getString(INTENT_KEY_DATA) == null)
			bundle.putString(INTENT_KEY_DATA, intent.getDataString());
		if (bundle.getString(SearchManager.QUERY) == null)
			bundle.putString(SearchManager.QUERY, intent.getStringExtra(SearchManager.QUERY));
		if (bundle.getString(MediaStore.EXTRA_MEDIA_FOCUS) == null)
			bundle.putString(MediaStore.EXTRA_MEDIA_FOCUS,
					intent.getStringExtra(MediaStore.EXTRA_MEDIA_FOCUS));
		if (bundle.getString(MediaStore.EXTRA_MEDIA_ARTIST) == null)
			bundle.putString(MediaStore.EXTRA_MEDIA_ARTIST,
					intent.getStringExtra(MediaStore.EXTRA_MEDIA_ARTIST));
		if (bundle.getString(MediaStore.EXTRA_MEDIA_ALBUM) == null)
			bundle.putString(MediaStore.EXTRA_MEDIA_ALBUM,
					intent.getStringExtra(MediaStore.EXTRA_MEDIA_ALBUM));
		if (bundle.getString(MediaStore.EXTRA_MEDIA_TITLE) == null)
			bundle.putString(MediaStore.EXTRA_MEDIA_TITLE,
					intent.getStringExtra(MediaStore.EXTRA_MEDIA_TITLE));

		QueryBrowserFragment fragment = new QueryBrowserFragment(bundle);

		getSupportFragmentManager().beginTransaction().replace(android.R.id.content, fragment)
				.commit();

	}

	@Override
	public void onSaveInstanceState(Bundle outcicle) {
		outcicle.putAll(bundle);
		super.onSaveInstanceState(outcicle);
	}

	public void onStart() {
		super.onStart();
		mToken = MusicUtils.bindToService(this, this);
	}

	public void onStop() {
		MusicUtils.unbindFromService(mToken);
		super.onStop();
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {

	}

	@Override
	public void onServiceDisconnected(ComponentName name) {

	}

}