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

import java.util.ArrayList;

import org.musicmod.android.Constants;
import org.musicmod.android.IMusicPlaybackService;
import org.musicmod.android.R;
import org.musicmod.android.util.MusicUtils;
import org.musicmod.android.util.ServiceToken;
import org.musicmod.android.util.PreferencesEditor;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.support.v4.app.ActionBar;
import android.support.v4.app.ActionBar.Tab;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.support.v4.view.ViewPager;

public class MusicBrowserActivity extends FragmentActivity implements Constants, ServiceConnection {

	private ViewPager mViewPager;
	private TabsAdapter mTabsAdapter;
	private ServiceToken mToken;
	private IMusicPlaybackService mService;
	private PreferencesEditor mPrefs;

	/**
	 * Called when the activity is first created.
	 */
	@Override
	public void onCreate(Bundle icicle) {

		super.onCreate(icicle);
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		setContentView(R.layout.music_browser);
		mPrefs = new PreferencesEditor(getApplicationContext());

		getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		ActionBar.Tab mArtistsTab = getSupportActionBar().newTab().setText(
				getString(R.string.artists).toUpperCase());
		ActionBar.Tab mAlbumsTab = getSupportActionBar().newTab().setText(
				getString(R.string.albums).toUpperCase());
		ActionBar.Tab mTracksTab = getSupportActionBar().newTab().setText(
				getString(R.string.tracks).toUpperCase());
		ActionBar.Tab mPlaylistsTab = getSupportActionBar().newTab().setText(
				getString(R.string.playlists).toUpperCase());

		mViewPager = (ViewPager) findViewById(R.id.pager);

		mTabsAdapter = new TabsAdapter(this, getSupportActionBar(), mViewPager);

		mTabsAdapter.addTab(mArtistsTab, ArtistsTabFragment.class);
		mTabsAdapter.addTab(mAlbumsTab, AlbumBrowserFragment.class);
		mTabsAdapter.addTab(mTracksTab, TrackBrowserFragment.class);
		mTabsAdapter.addTab(mPlaylistsTab, PlaylistsTabFragment.class);

	}

	@Override
	public void onStart() {
		super.onStart();
		mToken = MusicUtils.bindToService(this, this);
		IntentFilter filter = new IntentFilter();
		filter.addAction(BROADCAST_META_CHANGED);
		filter.addAction(BROADCAST_QUEUE_CHANGED);
		registerReceiver(mMediaStatusReceiver, filter);

		int currenttab = mPrefs.getIntState(STATE_KEY_CURRENTTAB, 0);
		mViewPager.setCurrentItem(currenttab);
	}

	@Override
	public void onStop() {

		mPrefs.setIntState(STATE_KEY_CURRENTTAB, mViewPager.getCurrentItem());
		unregisterReceiver(mMediaStatusReceiver);
		MusicUtils.unbindFromService(mToken);
		mService = null;
		super.onStop();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		getMenuInflater().inflate(R.menu.music_browser, menu);
		menu.findItem(GOTO_PLAYBACK).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		Intent intent;
		switch (item.getItemId()) {
			case GOTO_PLAYBACK:
				intent = new Intent(INTENT_PLAYBACK_VIEWER);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intent);
				break;
			case SHUFFLE_ALL:
				MusicUtils.shuffleAll(getApplicationContext());
				break;
			case SETTINGS:
				intent = new Intent(INTENT_MUSIC_SETTINGS);
				startActivity(intent);
				break;
		}

		return super.onOptionsItemSelected(item);
	}

	private BroadcastReceiver mMediaStatusReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			updateTitleBar();
		}

	};

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		mService = IMusicPlaybackService.Stub.asInterface(service);
		updateTitleBar();
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
		mService = null;
		finish();
	}

	private void updateTitleBar() {
		if (mService == null) return;
		try {
			if (mService.getAudioId() > -1 || mService.getPath() != null) {
				getSupportActionBar().setTitle(mService.getTrackName());
				if (mService.getArtistName() != null
						&& !MediaStore.UNKNOWN_STRING.equals(mService.getArtistName())) {
					getSupportActionBar().setSubtitle(mService.getArtistName());
				} else if (mService.getAlbumName() != null
						&& !MediaStore.UNKNOWN_STRING.equals(mService.getAlbumName())) {
					getSupportActionBar().setSubtitle(mService.getAlbumName());
				} else {
					getSupportActionBar().setSubtitle(null);
				}
			} else {
				getSupportActionBar().setTitle(R.string.music_library);
				getSupportActionBar().setSubtitle(null);
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	private class TabsAdapter extends FragmentPagerAdapter implements
			ViewPager.OnPageChangeListener, ActionBar.TabListener {

		private final Context mContext;
		private final ActionBar mActionBar;
		private final ViewPager mViewPager;
		private final ArrayList<String> mTabs = new ArrayList<String>();

		public TabsAdapter(FragmentActivity activity, ActionBar actionBar, ViewPager pager) {
			super(activity.getSupportFragmentManager());
			mContext = activity;
			mActionBar = actionBar;
			mViewPager = pager;
			mViewPager.setAdapter(this);
			mViewPager.setOnPageChangeListener(this);
		}

		public void addTab(ActionBar.Tab tab, Class<?> clss) {
			mTabs.add(clss.getName());
			mActionBar.addTab(tab.setTabListener(this));
			notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			return mTabs.size();
		}

		@Override
		public Fragment getItem(int position) {
			return Fragment.instantiate(mContext, mTabs.get(position), null);
		}

		@Override
		public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
		}

		@Override
		public void onPageSelected(int position) {
			mActionBar.setSelectedNavigationItem(position);
		}

		@Override
		public void onPageScrollStateChanged(int state) {
		}

		@Override
		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			mViewPager.setCurrentItem(tab.getPosition());
		}

		@Override
		public void onTabReselected(Tab tab, FragmentTransaction ft) {
		}

		@Override
		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
		}
	}
}
