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

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore.Audio;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.MenuItem;
import android.view.View;
import android.view.Window;
import org.musicmod.android.Constants;
import org.musicmod.android.R;
import org.musicmod.android.util.MusicUtils;
import org.musicmod.android.util.ServiceToken;

public class TrackBrowserActivity extends FragmentActivity implements
		View.OnCreateContextMenuListener, Constants, ServiceConnection {

	private ServiceToken mToken;
	private Intent intent;
	private Bundle bundle;

	@Override
	public void onCreate(Bundle icicle) {

		super.onCreate(icicle);

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		intent = getIntent();
		bundle = icicle != null ? icicle : intent.getExtras();

		if (bundle == null) {
			bundle = new Bundle();
		}

		if (bundle.getString(INTENT_KEY_ACTION) == null)
			bundle.putString(INTENT_KEY_ACTION, intent.getAction());
		if (bundle.getString(INTENT_KEY_MIMETYPE) == null)
			bundle.putString(INTENT_KEY_MIMETYPE, intent.getType());

		TrackBrowserFragment fragment = new TrackBrowserFragment();
		fragment.setArguments(bundle);

		getSupportFragmentManager().beginTransaction().add(android.R.id.content, fragment).commit();
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

	}

	@Override
	public void onDestroy() {

		super.onDestroy();
	}

	@Override
	public void onStart() {
		super.onStart();
		mToken = MusicUtils.bindToService(this, this);
		setTitle();
	}

	@Override
	public void onStop() {

		MusicUtils.unbindFromService(mToken);
		super.onStop();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				finish();
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {

	}

	@Override
	public void onServiceDisconnected(ComponentName name) {

		finish();
	}

	private void setTitle() {
		String mimeType = bundle.getString(INTENT_KEY_MIMETYPE);
		String name;
		long id;
		if (Audio.Playlists.CONTENT_TYPE.equals(mimeType)) {
			id = bundle.getLong(Audio.Playlists._ID);
			switch ((int) id) {
				case (int) PLAYLIST_RECENTLY_ADDED:
					getSupportActionBar().setTitle(R.string.recently_added);
					return;
				case (int) PLAYLIST_FAVORITES:
					getSupportActionBar().setTitle(R.string.favorites);
					return;
				case (int) PLAYLIST_PODCASTS:
					getSupportActionBar().setTitle(R.string.podcasts);
					return;
				default:
					if (id < 0) {
						getSupportActionBar().setTitle(R.string.music_library);
						return;
					}
			}

			name = MusicUtils.getPlaylistName(getApplicationContext(), id);
		} else if (Audio.Artists.CONTENT_TYPE.equals(mimeType)) {
			id = bundle.getLong(Audio.Artists._ID);
			name = MusicUtils.getArtistName(getApplicationContext(), id, true);
		} else if (Audio.Albums.CONTENT_TYPE.equals(mimeType)) {
			id = bundle.getLong(Audio.Albums._ID);
			name = MusicUtils.getAlbumName(getApplicationContext(), id, true);
		} else {
			getSupportActionBar().setTitle(R.string.music_library);
			return;
		}

		getSupportActionBar().setTitle(name);

	}

}