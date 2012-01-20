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
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
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
		Cursor cursor;
		String[] cols;
		Uri uri;
		String where;
		long id;
		if (Audio.Playlists.CONTENT_TYPE.equals(mimeType)) {
			id = bundle.getLong(Audio.Playlists._ID);
			where = Audio.Playlists._ID + "=" + id;
			cols = new String[] { Audio.Playlists.NAME };
			uri = Audio.Playlists.EXTERNAL_CONTENT_URI;
		} else if (Audio.Artists.CONTENT_TYPE.equals(mimeType)) {
			id = bundle.getLong(Audio.Artists._ID);
			where = Audio.Artists._ID + "=" + id;
			cols = new String[] { Audio.Artists.ARTIST };
			uri = Audio.Artists.EXTERNAL_CONTENT_URI;
		} else if (Audio.Albums.CONTENT_TYPE.equals(mimeType)) {
			id = bundle.getLong(Audio.Albums._ID);
			where = Audio.Albums._ID + "=" + id;
			cols = new String[] { Audio.Albums.ALBUM };
			uri = Audio.Albums.EXTERNAL_CONTENT_URI;
		} else {
			getSupportActionBar().setTitle(R.string.musicbrowserlabel);
			return;
		}

		cursor = getContentResolver().query(uri, cols, where, null, null);
		if (cursor.getCount() <= 0) {
			getSupportActionBar().setTitle(R.string.musicbrowserlabel);
			return;
		}
		
		cursor.moveToFirst();
		String name = cursor.getString(0);
		cursor.close();
		if (name == null || MediaStore.UNKNOWN_STRING.equals(name)) {
			if (Audio.Artists.CONTENT_TYPE.equals(mimeType)) {
				name = getString(R.string.unknown_artist);
			} else if (Audio.Albums.CONTENT_TYPE.equals(mimeType)) {
				name = getString(R.string.unknown_album);
			}
		}
		getSupportActionBar().setTitle(name);
		
	}

}