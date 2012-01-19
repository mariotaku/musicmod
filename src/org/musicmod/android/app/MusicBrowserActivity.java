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

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActionBar;
import android.support.v4.app.ActionBar.Tab;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.Menu;
import android.support.v4.view.ViewPager;
import android.view.MenuItem;

public class MusicBrowserActivity extends FragmentActivity implements Constants, ServiceConnection {

	private ViewPager mViewPager;
	private TabsAdapter mTabsAdapter;
	private ServiceToken mToken;
	private IMusicPlaybackService mService;

	/**
	 * Called when the activity is first created.
	 */
	@Override
	public void onCreate(Bundle icicle) {

		super.onCreate(icicle);
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		setContentView(R.layout.music_browser);

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

		mTabsAdapter.addTab(mArtistsTab, ArtistsFragment.class);
		mTabsAdapter.addTab(mAlbumsTab, AlbumsFragment.class);
		mTabsAdapter.addTab(mTracksTab, TracksFragment.class);
		mTabsAdapter.addTab(mPlaylistsTab, PlaylistsFragment.class);

	}

	@Override
	public void onStart() {
		super.onStart();
		mToken = MusicUtils.bindToService(this, this);
	}

	@Override
	public void onStop() {
		MusicUtils.unbindFromService(mToken);
		mService = null;
		super.onStop();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		getMenuInflater().inflate(R.menu.music_browser, menu);
		menu.findItem(R.id.goto_playback).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		return super.onCreateOptionsMenu(menu);
	}
	
	public static class TabsAdapter extends FragmentPagerAdapter implements
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

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		mService = IMusicPlaybackService.Stub.asInterface(service);
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
		mService = null;
		finish();
	}
}
