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

import org.musicmod.android.Constants;
import org.musicmod.android.R;
import org.musicmod.android.activity.MusicSettingsActivity;
import org.musicmod.android.util.MusicUtils;
import org.musicmod.android.util.ServiceToken;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

public class AlbumTab extends FragmentActivity implements View.OnCreateContextMenuListener,
		Constants, ServiceConnection, OnItemClickListener {

	private String mCurrentAlbumId;
	boolean mIsUnknownArtist;
	boolean mIsUnknownAlbum;
	private ServiceToken mToken;
	private boolean mShowFadeAnimation = false;

	public AlbumTab() {

	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle icicle) {

		super.onCreate(icicle);

		if (icicle != null) {
			mCurrentAlbumId = icicle.getString(INTENT_KEY_ALBUM);
			mArtistId = icicle.getString(INTENT_KEY_ARTIST);
		} else {
			mArtistId = getIntent().getStringExtra(INTENT_KEY_ARTIST);
		}

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		mToken = MusicUtils.bindToService(this, this);

		setContentView(R.layout.albums_browser);

		FragmentManager fm = getSupportFragmentManager();
		AlbumsFragment list = new AlbumsFragment();
		fm.beginTransaction().add(android.R.id.content, list).commit();

	}

	@Override
	public void onSaveInstanceState(Bundle outcicle) {

		// need to store the selected item so we don't lose it in case
		// of an orientation switch. Otherwise we could lose it while
		// in the middle of specifying a playlist to add the item to.
		outcicle.putString(INTENT_KEY_ALBUM, mCurrentAlbumId);
		outcicle.putString(INTENT_KEY_ARTIST, mArtistId);
		super.onSaveInstanceState(outcicle);
	}

	@Override
	public void onDestroy() {

		MusicUtils.unbindFromService(mToken);
		super.onDestroy();
	}

	@Override
	public void onResume() {

		super.onResume();

		loadSettings();

	}

	private void loadSettings() {

		try {
			float mTransitionAnimation = Settings.System.getFloat(this.getContentResolver(),
					Settings.System.TRANSITION_ANIMATION_SCALE);
			if (mTransitionAnimation > 0.0) {
				mShowFadeAnimation = true;
			} else {
				mShowFadeAnimation = false;
			}

		} catch (SettingNotFoundException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View v, int position, long id) {

		Intent intent = new Intent(Intent.ACTION_PICK);
		intent.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/track");
		intent.setPackage("org.musicmod.android");
		intent.putExtra(INTENT_KEY_ALBUM, Long.valueOf(id).toString());
		intent.putExtra(INTENT_KEY_ARTIST, mArtistId);
		startActivity(intent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.options_menu_album, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {

		MusicUtils.setPartyShuffleMenuIcon(menu);
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		Intent intent;
		switch (item.getItemId()) {
			case PARTY_SHUFFLE:
				MusicUtils.togglePartyShuffle();
				break;

			case SHUFFLE_ALL:
				MusicUtils.shuffleAll(this);
				return true;

			case SETTINGS:
				intent = new Intent();
				intent.setClass(this, MusicSettingsActivity.class);
				startActivityForResult(intent, SETTINGS);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private String mArtistId;

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {

		MusicUtils.updateNowPlaying(this);
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {

		finish();
	}

}