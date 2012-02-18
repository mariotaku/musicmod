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

import java.util.ArrayList;

import org.mariotaku.actionbarcompat.ActionBarActivity;
import org.mariotaku.actionbarcompat.ActionBarCompat;
import org.musicmod.android.Constants;
import org.musicmod.android.IMusicPlaybackService;
import org.musicmod.android.R;
import org.musicmod.android.dialog.ScanningProgress;
import org.musicmod.android.util.MusicUtils;
import org.musicmod.android.util.ServiceToken;
import org.musicmod.android.util.PreferencesEditor;

import com.viewpagerindicator.TitlePageIndicator;
import com.viewpagerindicator.TitleProvider;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageButton;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

public class MusicBrowserActivity extends ActionBarActivity implements Constants,
		ViewSwitcher.ViewFactory, ServiceConnection {

	private ViewPager mViewPager;
	private TabsAdapter mTabsAdapter;
	private ServiceToken mToken;
	private IMusicPlaybackService mService;
	private PreferencesEditor mPrefs;
	private ImageSwitcher mAlbumArt;
	private TextView mTrackName, mTrackDetail;
	private ImageButton mPlayPauseButton, mNextButton;
	private AsyncAlbumArtLoader mAlbumArtLoader;
	private TitlePageIndicator mIndicator;

	@Override
	public void onCreate(Bundle icicle) {

		super.onCreate(icicle);
		setVolumeControlStream(AudioManager.STREAM_MUSIC);

		mPrefs = new PreferencesEditor(getApplicationContext());

		String mount_state = Environment.getExternalStorageState();

		if (!Environment.MEDIA_MOUNTED.equals(mount_state)
				&& !Environment.MEDIA_MOUNTED_READ_ONLY.equals(mount_state)) {
			startActivity(new Intent(this, ScanningProgress.class));
			finish();
		}

		configureActivity();
		configureTabs(icicle);

	}

	@Override
	public void onStart() {
		super.onStart();
		mToken = MusicUtils.bindToService(this, this);
		IntentFilter filter = new IntentFilter();
		filter.addAction(BROADCAST_META_CHANGED);
		filter.addAction(BROADCAST_QUEUE_CHANGED);
		filter.addAction(BROADCAST_PLAYSTATE_CHANGED);
		registerReceiver(mMediaStatusReceiver, filter);

	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putAll(getIntent().getExtras() != null ? getIntent().getExtras() : new Bundle());
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onStop() {

		unregisterReceiver(mMediaStatusReceiver);
		MusicUtils.unbindFromService(mToken);
		mService = null;
		super.onStop();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		getMenuInflater().inflate(R.menu.music_browser, menu);
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

	@Override
	public View makeView() {
		ImageView i = new ImageView(this);
		i.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
		i.setLayoutParams(new ImageSwitcher.LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.MATCH_PARENT));
		return i;
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		mService = IMusicPlaybackService.Stub.asInterface(service);
		updateNowplaying();
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
		mService = null;
		finish();
	}

	private BroadcastReceiver mMediaStatusReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (BROADCAST_META_CHANGED.equals(intent.getAction())
					|| BROADCAST_META_CHANGED.equals(intent.getAction())) {
				updateNowplaying();
			} else if (BROADCAST_PLAYSTATE_CHANGED.equals(intent.getAction())) {
				updatePlayPauseButton();
			}

		}

	};

	private void configureActivity() {

		setContentView(R.layout.music_browser);

		ActionBarCompat mActionBar = getActionBarCompat();

		mActionBar.setCustomView(R.layout.actionbar_music_browser);

		View mCustomView = mActionBar.getCustomView();

		mActionBar.setDisplayShowHomeEnabled(false);
		mActionBar.setDisplayShowTitleEnabled(false);
		mActionBar.setDisplayShowCustomEnabled(true);

		mCustomView.setOnClickListener(mActionBarClickListener);

		mAlbumArt = (ImageSwitcher) mCustomView.findViewById(R.id.album_art);
		mAlbumArt.setFactory(this);
		mTrackName = (TextView) mCustomView.findViewById(R.id.track_name);
		mTrackDetail = (TextView) mCustomView.findViewById(R.id.track_detail);
		mPlayPauseButton = (ImageButton) mCustomView.findViewById(R.id.play_pause);
		mPlayPauseButton.setOnClickListener(mPlayPauseClickListener);
		mNextButton = (ImageButton) mCustomView.findViewById(R.id.next);
		mNextButton.setOnClickListener(mNextClickListener);

		mTabsAdapter = new TabsAdapter(getSupportFragmentManager());
		mViewPager = (ViewPager) findViewById(R.id.pager);

		mIndicator = (TitlePageIndicator) mCustomView.findViewById(R.id.indicator);
		if (mIndicator == null) {
			mIndicator = (TitlePageIndicator) findViewById(R.id.indicator);
		}

	}

	private void configureTabs(Bundle args) {

		mTabsAdapter.addFragment(new ArtistFragment(args), getString(R.string.artists)
				.toUpperCase());
		mTabsAdapter.addFragment(new AlbumFragment(args), getString(R.string.albums).toUpperCase());
		mTabsAdapter.addFragment(new TrackFragment(args), getString(R.string.tracks).toUpperCase());
		mTabsAdapter.addFragment(new PlaylistFragment(args), getString(R.string.playlists)
				.toUpperCase());
		mTabsAdapter.addFragment(new GenreFragment(args), getString(R.string.genres).toUpperCase());

		mViewPager.setAdapter(mTabsAdapter);
		mIndicator.setViewPager(mViewPager);
		int currenttab = mPrefs.getIntState(STATE_KEY_CURRENTTAB, 0);
		mIndicator.setCurrentItem(currenttab < mTabsAdapter.getCount() ? currenttab : mTabsAdapter
				.getCount() - 1);
		mIndicator.setOnPageChangeListener(mOnPageChangeListener);
	}

	private void updateNowplaying() {
		if (mService == null) return;
		try {
			if (mService.getAudioId() > -1 || mService.getPath() != null) {
				mPlayPauseButton.setVisibility(View.VISIBLE);
				mNextButton.setVisibility(View.VISIBLE);
				mTrackName.setText(mService.getTrackName());
				if (mService.getArtistName() != null
						&& !MediaStore.UNKNOWN_STRING.equals(mService.getArtistName())) {
					mTrackDetail.setText(mService.getArtistName());
				} else if (mService.getAlbumName() != null
						&& !MediaStore.UNKNOWN_STRING.equals(mService.getAlbumName())) {
					mTrackDetail.setText(mService.getAlbumName());
				} else {
					mTrackDetail.setText(R.string.unknown_artist);
				}
			} else {
				mPlayPauseButton.setVisibility(View.GONE);
				mNextButton.setVisibility(View.GONE);
				mTrackName.setText(R.string.music_library);
				mTrackDetail.setText(R.string.touch_to_shuffle_all);
			}
			if (mAlbumArtLoader != null) mAlbumArtLoader.cancel(true);
			mAlbumArtLoader = new AsyncAlbumArtLoader();
			mAlbumArtLoader.execute();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	private void updatePlayPauseButton() {
		if (mService == null) return;
		try {
			if (mService.isPlaying()) {
				mPlayPauseButton.setImageResource(R.drawable.ic_action_media_pause);
			} else {
				mPlayPauseButton.setImageResource(R.drawable.ic_action_media_play);
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	private OnClickListener mActionBarClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (mService == null) return;
			try {
				if (mService.getAudioId() > -1 || mService.getPath() != null) {
					Intent intent = new Intent(INTENT_PLAYBACK_VIEWER);
					intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(intent);
				} else {
					MusicUtils.shuffleAll(getApplicationContext());
				}
			} catch (RemoteException e) {
				e.printStackTrace();
			}

		}
	};

	private OnClickListener mPlayPauseClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (mService == null) return;
			try {
				if (mService.isPlaying()) {
					mService.pause();
				} else {
					mService.play();
				}

			} catch (RemoteException e) {
				e.printStackTrace();
			}

		}
	};

	private OnClickListener mNextClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (mService == null) return;
			try {
				mService.next();
			} catch (RemoteException e) {
				e.printStackTrace();
			}

		}
	};

	private OnPageChangeListener mOnPageChangeListener = new OnPageChangeListener() {

		@Override
		public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

		}

		@Override
		public void onPageSelected(int position) {

			mPrefs.setIntState(STATE_KEY_CURRENTTAB, position);
		}

		@Override
		public void onPageScrollStateChanged(int state) {

		}
	};

	private class TabsAdapter extends FragmentPagerAdapter implements TitleProvider {

		private final ArrayList<Fragment> mFragments = new ArrayList<Fragment>();
		private final ArrayList<String> mTitles = new ArrayList<String>();

		public TabsAdapter(FragmentManager manager) {
			super(manager);
		}

		public void addFragment(Fragment fragment, String name) {
			mFragments.add(fragment);
			mTitles.add(name);
			notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			return mFragments.size();
		}

		@Override
		public Fragment getItem(int position) {
			return mFragments.get(position);
		}

		@Override
		public String getTitle(int position) {
			return mTitles.get(position);
		}

	}

	private class AsyncAlbumArtLoader extends AsyncTask<Void, Void, Drawable> {

		@Override
		public Drawable doInBackground(Void... params) {

			if (mService != null) {
				try {
					Bitmap bitmap = MusicUtils.getArtwork(getApplicationContext(),
							mService.getAudioId(), mService.getAlbumId());
					if (bitmap == null) return null;
					int value = 0;
					if (bitmap.getHeight() <= bitmap.getWidth()) {
						value = bitmap.getHeight();
					} else {
						value = bitmap.getWidth();
					}
					Bitmap result = Bitmap.createBitmap(bitmap, (bitmap.getWidth() - value) / 2,
							(bitmap.getHeight() - value) / 2, value, value);
					return new BitmapDrawable(getResources(), result);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
			return null;
		}

		@Override
		public void onPostExecute(Drawable result) {
			if (mAlbumArt != null) {
				if (result != null) {
					mAlbumArt.setImageDrawable(result);
				} else {
					mAlbumArt.setImageResource(R.drawable.ic_mp_albumart_unknown);
				}
			}
		}
	}
}
