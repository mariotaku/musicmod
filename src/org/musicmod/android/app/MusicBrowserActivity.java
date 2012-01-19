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
import org.musicmod.android.R;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ActionBar;
import android.support.v4.app.ActionBar.Tab;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;

public class MusicBrowserActivity extends FragmentActivity implements Constants {

	private ViewPager mViewPager;
	private TabsAdapter mTabsAdapter;

	public MusicBrowserActivity() {

	}

	/**
	 * Called when the activity is first created.
	 */
	@Override
	public void onCreate(Bundle icicle) {

		super.onCreate(icicle);

		setContentView(R.layout.music_browser);

		getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		ActionBar.Tab mAlbumsTab = getSupportActionBar().newTab().setText(
				getString(R.string.albums).toUpperCase());
		ActionBar.Tab mTracksTab = getSupportActionBar().newTab().setText(
				getString(R.string.tracks).toUpperCase());
		ActionBar.Tab mPlaylistsTab = getSupportActionBar().newTab().setText(
				getString(R.string.playlists).toUpperCase());

		mViewPager = (ViewPager) findViewById(R.id.pager);
		mTabsAdapter = new TabsAdapter(this, getSupportActionBar(), mViewPager);

		mTabsAdapter.addTab(mAlbumsTab, AlbumsFragment.class);
		mTabsAdapter.addTab(mTracksTab, TracksFragment.class);
		mTabsAdapter.addTab(mPlaylistsTab, PlaylistsFragment.class);

	}

	/**
	 * This is a helper class that implements the management of tabs and all
	 * details of connecting a ViewPager with associated TabHost. It relies on a
	 * trick. Normally a tab host has a simple API for supplying a View or
	 * Intent that each tab will show. This is not sufficient for switching
	 * between pages. So instead we make the content part of the tab host 0dp
	 * high (it is not shown) and the TabsAdapter supplies its own dummy view to
	 * show as the tab content. It listens to changes in tabs, and takes care of
	 * switch to the correct paged in the ViewPager whenever the selected tab
	 * changes.
	 */
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
}
