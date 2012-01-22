/*
 *              Copyright (C) 2011 The MusicMod Project
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
import org.musicmod.android.IMusicPlaybackService;
import org.musicmod.android.MusicPlaybackService;
import org.musicmod.android.R;
import org.musicmod.android.util.ColorAnalyser;
import org.musicmod.android.util.MusicUtils;
import org.musicmod.android.util.ServiceToken;
import org.musicmod.android.util.PreferencesEditor;
import org.musicmod.android.util.VisualizerWrapper;
import org.musicmod.android.util.VisualizerWrapper.OnDataChangedListener;
import org.musicmod.android.view.VisualizerViewFftSpectrum;
import org.musicmod.android.view.VisualizerViewWaveForm;
import org.musicmod.android.widget.RepeatingImageButton;
import org.musicmod.android.widget.RepeatingImageButton.RepeatListener;
import org.musicmod.android.widget.TextScrollView;
import org.musicmod.android.widget.TextScrollView.OnLineSelectedListener;

import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.GestureDetector.OnGestureListener;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class MusicPlaybackActivity extends FragmentActivity implements Constants,
		View.OnTouchListener, View.OnLongClickListener, OnLineSelectedListener, ServiceConnection {

	private boolean mSeeking = false;
	private boolean mDeviceHasDpad;
	private long mStartSeekPos = 0;
	private long mLastSeekEventTime;
	private IMusicPlaybackService mService = null;
	private RepeatingImageButton mPrevButton;
	private ImageButton mPauseButton;
	private RepeatingImageButton mNextButton;
	private ImageButton mRepeatButton;
	private ImageButton mShuffleButton;
	private AsyncAlbumArtLoader mAlbumArtLoader;
	private AsyncColorAnalyser mColorAnalyser;
	private Toast mToast;
	private ServiceToken mToken;
	private boolean mIntentDeRegistered = false;

	private PreferencesEditor mPrefs;

	private int mUIColor = Color.WHITE;
	private boolean mAutoColor = true;
	private boolean mBlurBackground = false;

	private GestureDetector mGestureDetector;
	private View mVolumeSliderLeft, mVolumeSliderRight;
	private AudioManager mAudioManager;
	private VisualizerViewFftSpectrum mVisualizerViewFftSpectrum;
	private VisualizerViewWaveForm mVisualizerViewWaveForm;
	private FrameLayout mVisualizerView;

	private VisualizerWrapper mVisualizer;

	private static final int RESULT_ALBUMART_DOWNLOADED = 1;

	// for lyrics displaying
	private TextScrollView mLyricsScrollView;

	private LinearLayout mLyricsView, mInfoView, mVolumeSlider;

	private boolean mDisplayLyrics = true;
	private boolean mShowFadeAnimation = false;
	private boolean mLyricsWakelock = DEFAULT_LYRICS_WAKELOCK;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle icicle) {

		super.onCreate(icicle);
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		mGestureDetector = new GestureDetector(getApplicationContext(), mVolumeSlideListener);
		mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		mPrefs = new PreferencesEditor(this);
		configureActivity();

		mVisualizer = new VisualizerWrapper(50, false, false);
		mVisualizer.setOnDataChangedListener(mDataChangedListener);
	}

	private OnDataChangedListener mDataChangedListener = new OnDataChangedListener() {

		@Override
		public void onFftDataChanged(short[] data, int len) {

			Log.d("MusicMod", "onFftDataChanged, len = " + len);

			if (mVisualizerViewFftSpectrum != null) {
				// mVisualizerViewFftSpectrum.updateVisualizer(data);
			}
		}

		@Override
		public void onWaveDataChanged(short[] data, int len) {

			Log.d("MusicMod", "onWaveDataChanged, len = " + len);

			if (mVisualizerViewWaveForm != null) {
				// mVisualizerViewWaveForm.updateVisualizer(data);
			}

		}

	};

	private void configureActivity() {

		setContentView(R.layout.nowplaying_default);

		mCurrentTime = (TextView) findViewById(R.id.currenttime);
		((View) mCurrentTime.getParent()).setOnLongClickListener(mToggleVisualizerListener);

		mTotalTime = (TextView) findViewById(R.id.totaltime);
		((View) mTotalTime.getParent()).setOnLongClickListener(mToggleVisualizerListener);

		mProgress = (ProgressBar) findViewById(android.R.id.progress);

		mAlbum = (ImageView) findViewById(R.id.album);
		mAlbum.setOnClickListener(mQueueListener);
		mAlbum.setOnLongClickListener(mSearchAlbumArtListener);

		mArtistNameView = (TextView) findViewById(R.id.artistname);
		mAlbumNameView = (TextView) findViewById(R.id.albumname);

		mLyricsView = (LinearLayout) findViewById(R.id.lyrics_view);

		mLyricsScrollView = (TextScrollView) findViewById(R.id.lyrics_scroll);
		mLyricsScrollView.setContentGravity(Gravity.CENTER_HORIZONTAL);

		mInfoView = (LinearLayout) findViewById(R.id.info_view);

		mVolumeSlider = (LinearLayout) findViewById(R.id.volume_layout);

		mVolumeSliderLeft = findViewById(R.id.volume_slider_left);
		mVolumeSliderLeft.setOnTouchListener(this);

		mVolumeSliderRight = findViewById(R.id.volume_slider_right);
		mVolumeSliderRight.setOnTouchListener(this);

		mPrevButton = (RepeatingImageButton) findViewById(R.id.prev);
		mPrevButton.setBackgroundDrawable(new ButtonStateDrawable(new Drawable[] { getResources()
				.getDrawable(R.drawable.btn_mp_playback) }));
		mPrevButton.setOnClickListener(mPrevListener);
		mPrevButton.setRepeatListener(mRewListener, 260);

		mPauseButton = (ImageButton) findViewById(R.id.pause);
		mPauseButton.setBackgroundDrawable(new ButtonStateDrawable(new Drawable[] { getResources()
				.getDrawable(R.drawable.btn_mp_playback) }));
		mPauseButton.requestFocus();
		mPauseButton.setOnClickListener(mPauseListener);

		mNextButton = (RepeatingImageButton) findViewById(R.id.next);
		mNextButton.setBackgroundDrawable(new ButtonStateDrawable(new Drawable[] { getResources()
				.getDrawable(R.drawable.btn_mp_playback) }));
		mNextButton.setOnClickListener(mNextListener);
		mNextButton.setRepeatListener(mFfwdListener, 260);

		mDeviceHasDpad = (getResources().getConfiguration().navigation == Configuration.NAVIGATION_DPAD);

		mShuffleButton = new ImageButton(this);
		mShuffleButton.setBackgroundDrawable(new ButtonStateDrawable(
				new Drawable[] { getResources().getDrawable(R.drawable.btn_mp_playback) }));
		mShuffleButton.setOnClickListener(mShuffleListener);

		mRepeatButton = new ImageButton(this);
		mRepeatButton.setBackgroundDrawable(new ButtonStateDrawable(new Drawable[] { getResources()
				.getDrawable(R.drawable.btn_mp_playback) }));
		mRepeatButton.setOnClickListener(mRepeatListener);

		mVisualizerViewFftSpectrum = new VisualizerViewFftSpectrum(this);
		mVisualizerViewWaveForm = new VisualizerViewWaveForm(this);
		mVisualizerView = (FrameLayout) findViewById(R.id.visualizer_view);
		mVisualizerView.addView(mVisualizerViewFftSpectrum);
		mVisualizerView.addView(mVisualizerViewWaveForm);

		if (mProgress instanceof SeekBar) {
			SeekBar seeker = (SeekBar) mProgress;
			seeker.setOnSeekBarChangeListener(mSeekListener);
		}
		mProgress.setMax(1000);
	}

	// TODO loadPreferences
	private void loadPreferences() {

		mLyricsWakelock = mPrefs.getBooleanPref(KEY_LYRICS_WAKELOCK, DEFAULT_LYRICS_WAKELOCK);
		mDisplayLyrics = mPrefs.getBooleanState(KEY_DISPLAY_LYRICS, DEFAULT_DISPLAY_LYRICS);
		mAutoColor = mPrefs.getBooleanPref(KEY_AUTO_COLOR, true);
		mBlurBackground = mPrefs.getBooleanPref(KEY_BLUR_BACKGROUND, false);
	}

	int mInitialX = -1;
	int mLastX = -1;
	int mTextWidth = 0;
	int mViewWidth = 0;
	boolean mDraggingLabel = false;

	@Override
	public boolean onTouch(View v, MotionEvent event) {

		int action = event.getAction();
		if (action == MotionEvent.ACTION_DOWN) {
			v.setBackgroundColor(Color.argb(0x33, Color.red(mUIColor), Color.green(mUIColor),
					Color.blue(mUIColor)));
		} else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
			v.setBackgroundColor(Color.TRANSPARENT);
		}
		mGestureDetector.onTouchEvent(event);
		return true;
	}

	private OnGestureListener mVolumeSlideListener = new OnGestureListener() {

		int view_height = 0;
		int max_volume = 0;
		float delta = 0;

		@Override
		public boolean onSingleTapUp(MotionEvent e) {

			// nothing to do here.
			return false;
		}

		@Override
		public void onShowPress(MotionEvent e) {

		}

		@Override
		public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX,
				float distanceY) {

			if (delta >= 1 || delta <= -1) {
				adjustVolume((int) delta);
				delta = 0;
			} else {
				delta += max_volume * distanceY / view_height * 2;
			}
			return false;
		}

		@Override
		public void onLongPress(MotionEvent e) {

			delta = 0;
		}

		@Override
		public boolean onDown(MotionEvent e) {

			max_volume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
			view_height = mVolumeSlider.getHeight();
			delta = 0;
			return false;
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

			return false;
		}
	};

	@Override
	public boolean onLongClick(View v) {

		String track = getSupportActionBar().getTitle().toString();
		String artist = mArtistNameView.getText().toString();
		String album = mAlbumNameView.getText().toString();

		CharSequence title = getString(R.string.mediasearch, track);
		Intent i = new Intent();
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		i.setAction(MediaStore.INTENT_ACTION_MEDIA_SEARCH);
		i.putExtra(MediaStore.EXTRA_MEDIA_TITLE, track);

		String query = track;
		if (!getString(R.string.unknown_artist).equals(artist)
				&& !getString(R.string.unknown_album).equals(album)) {
			query = artist + " " + track;
			i.putExtra(MediaStore.EXTRA_MEDIA_ALBUM, album);
			i.putExtra(MediaStore.EXTRA_MEDIA_ARTIST, artist);
		} else if (getString(R.string.unknown_artist).equals(artist)
				&& !getString(R.string.unknown_album).equals(album)) {
			query = album + " " + track;
			i.putExtra(MediaStore.EXTRA_MEDIA_ALBUM, album);
		} else if (!getString(R.string.unknown_artist).equals(artist)
				&& getString(R.string.unknown_album).equals(album)) {
			query = artist + " " + track;
			i.putExtra(MediaStore.EXTRA_MEDIA_ARTIST, artist);
		}
		i.putExtra(SearchManager.QUERY, query);
		i.putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "audio/*");
		startActivity(Intent.createChooser(i, title));
		return true;
	}

	@Override
	public void onLineSelected(int id) {

		try {
			mService.seek(mService.getPositionByLyricsId(id));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {

		@Override
		public void onStartTrackingTouch(SeekBar bar) {

			mLastSeekEventTime = 0;
			mFromTouch = true;
			mHandler.removeMessages(REFRESH);
		}

		@Override
		public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {

			if (!fromuser || (mService == null)) return;
			mPosOverride = mDuration * progress / 1000;
			try {
				mService.seek(mPosOverride);
			} catch (RemoteException ex) {
			}

			refreshNow();
			// trackball event, allow progress updates
			if (!mFromTouch) {
				refreshNow();
				mPosOverride = -1;
			}
		}

		@Override
		public void onStopTrackingTouch(SeekBar bar) {

			mPosOverride = -1;
			mFromTouch = false;
			// Ensure that progress is properly updated in the future,
			mHandler.sendEmptyMessage(REFRESH);
		}
	};

	private View.OnClickListener mQueueListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {

			Bundle bundle = new Bundle();
			bundle.putString(INTENT_KEY_MIMETYPE, MediaStore.Audio.Playlists.CONTENT_TYPE);
			bundle.putLong(MediaStore.Audio.Playlists._ID, PLAYLIST_QUEUE);
			Intent intent = new Intent(getApplicationContext(), TrackBrowserActivity.class);
			intent.putExtras(bundle);
			
			startActivity(intent);
		}
	};

	private View.OnClickListener mShuffleListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {

			toggleShuffle();
		}
	};

	private View.OnClickListener mRepeatListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {

			toggleRepeat();
		}
	};

	private View.OnClickListener mPauseListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {

			doPauseResume();
		}
	};

	private View.OnClickListener mPrevListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {

			doPrev();
		}
	};

	private View.OnClickListener mNextListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {

			doNext();
		}
	};

	private View.OnLongClickListener mToggleVisualizerListener = new View.OnLongClickListener() {

		@Override
		public boolean onLongClick(View v) {

			// TODO toggle visualizer
			return true;
		}
	};

	private View.OnLongClickListener mSearchLyricsListener = new View.OnLongClickListener() {

		@Override
		public boolean onLongClick(View v) {

			searchLyrics();
			return true;
		}
	};

	private View.OnLongClickListener mSearchAlbumArtListener = new View.OnLongClickListener() {

		@Override
		public boolean onLongClick(View v) {

			searchAlbumArt();
			return true;
		}
	};

	private RepeatListener mRewListener = new RepeatListener() {

		@Override
		public void onRepeat(View v, long howlong, int repcnt) {

			scanBackward(repcnt, howlong);
		}
	};

	private RepeatListener mFfwdListener = new RepeatListener() {

		@Override
		public void onRepeat(View v, long howlong, int repcnt) {

			scanForward(repcnt, howlong);
		}
	};

	@Override
	public void onStop() {

		paused = true;
		if (!mIntentDeRegistered) {
			mHandler.removeMessages(REFRESH);
			unregisterReceiver(mStatusListener);
		}
		mLyricsScrollView.unregisterLineSelectedListener(this);
		unregisterReceiver(mScreenTimeoutListener);
		if (mAlbumArtLoader != null) mAlbumArtLoader.cancel(true);

		// TODO visualizer
		mVisualizer.stop();

		MusicUtils.unbindFromService(mToken);
		mService = null;
		super.onStop();
	}

	@Override
	public void onStart() {

		super.onStart();
		paused = false;
		mToken = MusicUtils.bindToService(this, this);
		if (mToken == null) {
			// something went wrong
			mHandler.sendEmptyMessage(QUIT);
		}
		loadPreferences();
		mLyricsScrollView.registerLineSelectedListener(this);

		if (mBlurBackground) {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
		} else {
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
		}

		// show lyrics
		if (mDisplayLyrics) {
			displayLyrics(mShowFadeAnimation, false);
		} else {
			displayInfo(mShowFadeAnimation, false);
		}

		// TODO visualizer
		mVisualizer.start();

		try {
			float mWindowAnimation = Settings.System.getFloat(this.getContentResolver(),
					Settings.System.WINDOW_ANIMATION_SCALE);
			float mTransitionAnimation = Settings.System.getFloat(this.getContentResolver(),
					Settings.System.TRANSITION_ANIMATION_SCALE);
			if (mTransitionAnimation > 0.0) {
				mShowFadeAnimation = true;
			} else {
				mShowFadeAnimation = false;
			}

			mLyricsScrollView.setSmoothScrollingEnabled(mWindowAnimation > 0.0);

		} catch (SettingNotFoundException e) {
			e.printStackTrace();
		}

		IntentFilter f = new IntentFilter();
		f.addAction(BROADCAST_PLAYSTATE_CHANGED);
		f.addAction(BROADCAST_META_CHANGED);
		f.addAction(BROADCAST_NEW_LYRICS_LOADED);
		f.addAction(BROADCAST_LYRICS_REFRESHED);
		registerReceiver(mStatusListener, new IntentFilter(f));

		IntentFilter s = new IntentFilter();
		s.addAction(Intent.ACTION_SCREEN_ON);
		s.addAction(Intent.ACTION_SCREEN_OFF);
		registerReceiver(mScreenTimeoutListener, new IntentFilter(s));

		long next = refreshNow();
		queueNextRefresh(next);
	}

	@Override
	public void onResume() {

		super.onResume();
		if (mIntentDeRegistered) {
			paused = false;
		}
		setPauseButtonImage();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		getMenuInflater().inflate(R.menu.now_playing, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {

		if (mService == null) return false;

		MusicUtils.setPartyShuffleMenuIcon(menu);

		KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
		menu.setGroupVisible(1, !km.inKeyguardRestrictedInputMode());

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		Intent intent;
		switch (item.getItemId()) {
			case PARTY_SHUFFLE:
				MusicUtils.togglePartyShuffle();
				setShuffleButtonImage();
				break;
			case ADD_TO_PLAYLIST:
				intent = new Intent(INTENT_ADD_TO_PLAYLIST);
				long[] list_to_be_added = new long[1];
				list_to_be_added[0] = MusicUtils.getCurrentAudioId();
				intent.putExtra(INTENT_KEY_LIST, list_to_be_added);
				startActivity(intent);
				break;
			case EQUALIZER:
				intent = new Intent(INTENT_EQUALIZER);
				startActivity(intent);
				break;
			case SLEEP_TIMER:
				intent = new Intent(INTENT_SLEEP_TIMER);
				startActivity(intent);
				break;
			case DELETE_ITEMS:
				intent = new Intent(INTENT_DELETE_ITEMS);
				Uri data = Uri.withAppendedPath(Audio.Media.EXTERNAL_CONTENT_URI,
						String.valueOf(MusicUtils.getCurrentAudioId()));
				intent.setData(data);
				startActivity(intent);
				break;
			case SETTINGS:
				intent = new Intent(INTENT_APPEARANCE_SETTINGS);
				startActivity(intent);
				break;
			case GOTO_HOME:
				intent = new Intent(INTENT_MUSIC_BROWSER);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intent);
				finish();
				break;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

		if (resultCode != RESULT_OK) {
			return;
		}
		switch (requestCode) {
			case DELETE_ITEMS:
				if (resultCode == RESULT_DELETE_MUSIC) {
					finish();
				}
				break;
		}
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {

		try {
			switch (keyCode) {
				case KeyEvent.KEYCODE_DPAD_LEFT:
					if (!useDpadMusicControl()) {
						break;
					}
					if (mService != null) {
						if (!mSeeking && mStartSeekPos >= 0) {
							mPauseButton.requestFocus();
							if (mStartSeekPos < 1000) {
								mService.prev();
							} else {
								mService.seek(0);
							}
						} else {
							scanBackward(-1, event.getEventTime() - event.getDownTime());
							mPauseButton.requestFocus();
							mStartSeekPos = -1;
						}
					}
					mSeeking = false;
					mPosOverride = -1;
					return true;
				case KeyEvent.KEYCODE_DPAD_RIGHT:
					if (!useDpadMusicControl()) {
						break;
					}
					if (mService != null) {
						if (!mSeeking && mStartSeekPos >= 0) {
							mPauseButton.requestFocus();
							mService.next();
						} else {
							scanForward(-1, event.getEventTime() - event.getDownTime());
							mPauseButton.requestFocus();
							mStartSeekPos = -1;
						}
					}
					mSeeking = false;
					mPosOverride = -1;
					return true;
			}
		} catch (RemoteException ex) {
		}
		return super.onKeyUp(keyCode, event);
	}

	private boolean useDpadMusicControl() {

		if (mDeviceHasDpad
				&& (mPrevButton.isFocused() || mNextButton.isFocused() || mPauseButton.isFocused())) {
			return true;
		}
		return false;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		int repcnt = event.getRepeatCount();

		switch (keyCode) {

			case KeyEvent.KEYCODE_DPAD_LEFT:
				if (!useDpadMusicControl()) {
					break;
				}
				if (!mPrevButton.hasFocus()) {
					mPrevButton.requestFocus();
				}
				scanBackward(repcnt, event.getEventTime() - event.getDownTime());
				return true;
			case KeyEvent.KEYCODE_DPAD_RIGHT:
				if (!useDpadMusicControl()) {
					break;
				}
				if (!mNextButton.hasFocus()) {
					mNextButton.requestFocus();
				}
				scanForward(repcnt, event.getEventTime() - event.getDownTime());
				return true;

			case KeyEvent.KEYCODE_R:
				toggleRepeat();
				return true;

			case KeyEvent.KEYCODE_S:
				toggleShuffle();
				return true;

			case KeyEvent.KEYCODE_L:
				toggleLyrics();
				return true;

			case KeyEvent.KEYCODE_N:
				if (mService != null) {
					try {
						mService.next();
						return true;
					} catch (RemoteException ex) {
						// ex.printStackTrace();
					}
				} else {
					return false;
				}

			case KeyEvent.KEYCODE_P:
				if (mService != null) {
					try {
						mService.prev();
						return true;
					} catch (RemoteException ex) {
						// ex.printStackTrace();
					}
				} else {
					return false;
				}

			case KeyEvent.KEYCODE_DPAD_CENTER:
			case KeyEvent.KEYCODE_SPACE:
				doPauseResume();
				return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	private void displayLyrics(boolean show_animation, boolean fromuser) {

		if (mLyricsWakelock) {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}

		if (mInfoView.getVisibility() != View.INVISIBLE) {

			mInfoView.setVisibility(View.INVISIBLE);
			mInfoView.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out));
		}

		if (mLyricsView.getVisibility() != View.VISIBLE) {

			mLyricsView.setVisibility(View.VISIBLE);
			mLyricsView.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));

		}

	}

	private void displayInfo(boolean show_animation, boolean fromuser) {

		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		if (mInfoView.getVisibility() != View.VISIBLE) {

			mInfoView.setVisibility(View.VISIBLE);
			mInfoView.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
		}

		if (mLyricsView.getVisibility() != View.INVISIBLE) {

			mLyricsView.setVisibility(View.INVISIBLE);
			mLyricsView.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out));

		}
	}

	private void searchLyrics() {

		String artistName = "";
		String trackName = "";
		String mediaPath = "";
		String lyricsPath = "";
		try {
			artistName = mService.getArtistName();
			trackName = mService.getTrackName();
			mediaPath = mService.getMediaPath();
			lyricsPath = mediaPath.substring(0, mediaPath.lastIndexOf(".")) + ".lrc";
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			Intent intent = new Intent(INTENT_SEARCH_LYRICS);
			intent.putExtra(INTENT_KEY_ARTIST, artistName);
			intent.putExtra(INTENT_KEY_TRACK, trackName);
			intent.putExtra(INTENT_KEY_PATH, lyricsPath);
			startActivity(intent);
		} catch (ActivityNotFoundException e) {
			// e.printStackTrace();
		}
	}

	private void searchAlbumArt() {

		String artistName = "";
		String albumName = "";
		String mediaPath = "";
		String albumArtPath = "";
		try {
			artistName = mService.getArtistName();
			albumName = mService.getAlbumName();
			mediaPath = mService.getMediaPath();
			albumArtPath = mediaPath.substring(0, mediaPath.lastIndexOf("/")) + "/AlbumArt.jpg";
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			Intent intent = new Intent(INTENT_SEARCH_ALBUMART);
			intent.putExtra(INTENT_KEY_ARTIST, artistName);
			intent.putExtra(INTENT_KEY_ALBUM, albumName);
			intent.putExtra(INTENT_KEY_PATH, albumArtPath);
			startActivityForResult(intent, RESULT_ALBUMART_DOWNLOADED);
		} catch (ActivityNotFoundException e) {
			// e.printStackTrace();
		}

	}

	// TODO lyrics load animation
	private void loadLyricsToView() {

		try {
			mLyricsScrollView.setTextContent(mService.getLyrics(), this);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	private void scrollLyrics(boolean force) {

		try {
			mLyricsScrollView.setCurrentLine(mService.getCurrentLyricsId(), force);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	private void adjustVolume(int value) {

		int max_volume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		int current_volume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

		if ((value + current_volume) <= max_volume && (value + current_volume) >= 0) {
			mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, value + current_volume,
					AudioManager.FLAG_SHOW_UI);
		} else if ((value + current_volume) > max_volume) {
			mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, max_volume,
					AudioManager.FLAG_SHOW_UI);
		} else if ((value + current_volume) < 0) {
			mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI);
		}

	}

	private void scanBackward(int repcnt, long delta) {

		if (mService == null) return;
		try {
			if (repcnt == 0) {
				mStartSeekPos = mService.position();
				mLastSeekEventTime = 0;
				mSeeking = false;
			} else {
				mSeeking = true;
				if (delta < 5000) {
					// seek at 10x speed for the first 5 seconds
					delta = delta * 10;
				} else {
					// seek at 40x after that
					delta = 50000 + (delta - 5000) * 40;
				}
				long newpos = mStartSeekPos - delta;
				if (newpos < 0) {
					// move to previous track
					mService.prev();
					long duration = mService.duration();
					mStartSeekPos += duration;
					newpos += duration;
				}
				if (((delta - mLastSeekEventTime) > 250) || repcnt < 0) {
					mService.seek(newpos);
					mLastSeekEventTime = delta;
				}
				if (repcnt >= 0) {
					mPosOverride = newpos;
				} else {
					mPosOverride = -1;
				}
				refreshNow();
			}
		} catch (RemoteException ex) {
		}
	}

	private void scanForward(int repcnt, long delta) {

		if (mService == null) return;
		try {
			if (repcnt == 0) {
				mStartSeekPos = mService.position();
				mLastSeekEventTime = 0;
				mSeeking = false;
			} else {
				mSeeking = true;
				if (delta < 5000) {
					// seek at 10x speed for the first 5 seconds
					delta = delta * 10;
				} else {
					// seek at 40x after that
					delta = 50000 + (delta - 5000) * 40;
				}
				long newpos = mStartSeekPos + delta;
				long duration = mService.duration();
				if (newpos >= duration) {
					// move to next track
					mService.next();
					mStartSeekPos -= duration; // is OK to go negative
					newpos -= duration;
				}
				if (((delta - mLastSeekEventTime) > 250) || repcnt < 0) {
					mService.seek(newpos);
					mLastSeekEventTime = delta;
				}
				if (repcnt >= 0) {
					mPosOverride = newpos;
				} else {
					mPosOverride = -1;
				}
				refreshNow();
			}
		} catch (RemoteException ex) {
		}
	}

	private void doPauseResume() {

		try {
			if (mService != null) {
				if (mService.isPlaying()) {
					mService.pause();
				} else {
					mService.play();
				}
				refreshNow();
				setPauseButtonImage();
			}
		} catch (RemoteException ex) {
		}
	}

	private void doPrev() {

		if (mService == null) return;
		try {
			if (mService.position() < 2000) {
				mService.prev();
			} else {
				mService.seek(0);
				mService.play();
			}
		} catch (RemoteException ex) {
		}
	}

	private void doNext() {

		if (mService == null) return;
		try {
			mService.next();
		} catch (RemoteException ex) {
		}
	}

	private void toggleShuffle() {

		if (mService == null) {
			return;
		}
		try {
			int shuffle = mService.getShuffleMode();
			if (shuffle == MusicPlaybackService.SHUFFLE_NONE) {
				mService.setShuffleMode(MusicPlaybackService.SHUFFLE_NORMAL);
				if (mService.getRepeatMode() == MusicPlaybackService.REPEAT_CURRENT) {
					mService.setRepeatMode(MusicPlaybackService.REPEAT_ALL);
					setRepeatButtonImage();
				}
				showToast(R.string.shuffle_on_notif);
			} else if (shuffle == MusicPlaybackService.SHUFFLE_NORMAL
					|| shuffle == MusicPlaybackService.SHUFFLE_AUTO) {
				mService.setShuffleMode(MusicPlaybackService.SHUFFLE_NONE);
				showToast(R.string.shuffle_off_notif);
			} else {
				Log.e("MediaPlaybackActivity", "Invalid shuffle mode: " + shuffle);
			}
			setShuffleButtonImage();
		} catch (RemoteException ex) {
		}
	}

	private void toggleRepeat() {

		if (mService == null) {
			return;
		}
		try {
			int mode = mService.getRepeatMode();
			if (mode == MusicPlaybackService.REPEAT_NONE) {
				mService.setRepeatMode(MusicPlaybackService.REPEAT_ALL);
				showToast(R.string.repeat_all_notif);
			} else if (mode == MusicPlaybackService.REPEAT_ALL) {
				mService.setRepeatMode(MusicPlaybackService.REPEAT_CURRENT);
				if (mService.getShuffleMode() != MusicPlaybackService.SHUFFLE_NONE) {
					mService.setShuffleMode(MusicPlaybackService.SHUFFLE_NONE);
					setShuffleButtonImage();
				}
				showToast(R.string.repeat_current_notif);
			} else {
				mService.setRepeatMode(MusicPlaybackService.REPEAT_NONE);
				showToast(R.string.repeat_off_notif);
			}
			setRepeatButtonImage();
		} catch (RemoteException ex) {
		}

	}

	private void toggleLyrics() {

		if (mDisplayLyrics) {
			displayInfo(mShowFadeAnimation, true);
			mDisplayLyrics = false;
		} else {
			displayLyrics(mShowFadeAnimation, true);
			mDisplayLyrics = true;
		}
		mPrefs.setBooleanState(KEY_DISPLAY_LYRICS, mDisplayLyrics);
	}

	private void showToast(int resid) {

		if (mToast == null) {
			mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
		}
		mToast.setText(resid);
		mToast.show();
	}

	@Override
	public void onServiceConnected(ComponentName classname, IBinder obj) {

		mService = IMusicPlaybackService.Stub.asInterface(obj);

		try {
			if (mService.getAudioId() >= 0 || mService.isPlaying() || mService.getPath() != null) {
				updateTrackInfo(false);
				loadLyricsToView();
				scrollLyrics(true);
				long next = refreshNow();
				queueNextRefresh(next);
				setRepeatButtonImage();
				setShuffleButtonImage();
				setPauseButtonImage();
			} else {
				Intent intent = new Intent(Intent.ACTION_MAIN);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				intent.setClass(getApplicationContext(), MusicBrowserActivity.class);
				startActivity(intent);
				finish();
			}

		} catch (RemoteException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void onServiceDisconnected(ComponentName classname) {

		mService = null;
	}

	private void setRepeatButtonImage() {

		if (mService == null) return;
		try {
			switch (mService.getRepeatMode()) {
				case REPEAT_ALL:
					mRepeatButton.setImageResource(R.drawable.ic_mp_repeat_all_btn);
					break;
				case REPEAT_CURRENT:
					mRepeatButton.setImageResource(R.drawable.ic_mp_repeat_once_btn);
					break;
				default:
					mRepeatButton.setImageResource(R.drawable.ic_mp_repeat_off_btn);
					break;
			}
		} catch (RemoteException ex) {
		}
	}

	private void setShuffleButtonImage() {

		if (mService == null) return;
		try {
			switch (mService.getShuffleMode()) {
				case SHUFFLE_NONE:
					mShuffleButton.setImageResource(R.drawable.ic_mp_shuffle_off_btn);
					break;
				case SHUFFLE_AUTO:
					mShuffleButton.setImageResource(R.drawable.ic_mp_partyshuffle_on_btn);
					break;
				default:
					mShuffleButton.setImageResource(R.drawable.ic_mp_shuffle_on_btn);
					break;
			}
		} catch (RemoteException ex) {
		}
	}

	private void setPauseButtonImage() {

		try {
			if (mService != null && mService.isPlaying()) {
				mPauseButton.setImageResource(R.drawable.btn_playback_ic_pause);
			} else {
				mPauseButton.setImageResource(R.drawable.btn_playback_ic_play);
			}
		} catch (RemoteException ex) {
		}
	}

	private ImageView mAlbum;
	private TextView mCurrentTime, mTotalTime;
	private TextView mArtistNameView, mAlbumNameView;
	private ProgressBar mProgress;
	private long mPosOverride = -1;
	private boolean mFromTouch = false;
	private long mDuration;
	private boolean paused;

	private static final int REFRESH = 1;
	private static final int QUIT = 2;

	private void queueNextRefresh(long delay) {

		if (!paused && !mFromTouch) {
			Message msg = mHandler.obtainMessage(REFRESH);
			mHandler.removeMessages(REFRESH);
			mHandler.sendMessageDelayed(msg, delay);
		}
	}

	private long refreshNow() {

		if (mService == null) return 500;
		try {
			long pos = mPosOverride < 0 ? mService.position() : mPosOverride;
			long remaining = 1000 - (pos % 1000);
			if ((pos >= 0) && (mDuration > 0)) {
				mCurrentTime.setText(MusicUtils.makeTimeString(this, pos / 1000));

				if (mService.isPlaying()) {
					mCurrentTime.setVisibility(View.VISIBLE);
				} else {
					// blink the counter
					// If the progress bar is still been dragged, then we do not
					// want to blink the
					// currentTime. It would cause flickering due to change in
					// the visibility.
					if (mFromTouch) {
						mCurrentTime.setVisibility(View.VISIBLE);
					} else {
						int vis = mCurrentTime.getVisibility();
						mCurrentTime.setVisibility(vis == View.INVISIBLE ? View.VISIBLE
								: View.INVISIBLE);
					}
					remaining = 500;
				}

				mProgress.setProgress((int) (1000 * pos / mDuration));
			} else {
				mCurrentTime.setText("--:--");
				mProgress.setProgress(1000);
			}
			// return the number of milliseconds until the next full second, so
			// the counter can be updated at just the right time
			return remaining;
		} catch (RemoteException ex) {
		}
		return 500;
	}

	private final Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {

			switch (msg.what) {
				case REFRESH:
					long next = refreshNow();
					queueNextRefresh(next);
					break;

				case QUIT:
					// This can be moved back to onCreate once the bug that
					// prevents
					// Dialogs from being started from onCreate/onResume is
					// fixed.
					new AlertDialog.Builder(MusicPlaybackActivity.this)
							.setTitle(R.string.service_start_error_title)
							.setMessage(R.string.service_start_error_msg)
							.setPositiveButton(android.R.string.ok,
									new DialogInterface.OnClickListener() {

										public void onClick(DialogInterface dialog, int whichButton) {

											finish();
										}
									}).setCancelable(false).show();
					break;

				default:
					break;
			}
		}
	};

	private BroadcastReceiver mStatusListener = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {

			String action = intent.getAction();
			if (BROADCAST_META_CHANGED.equals(action)) {
				// redraw the artist/title info and
				// set new max for progress bar
				updateTrackInfo(mShowFadeAnimation);
				setPauseButtonImage();
				queueNextRefresh(1);
			} else if (BROADCAST_PLAYSTATE_CHANGED.equals(action)) {
				setPauseButtonImage();
			} else if (BROADCAST_NEW_LYRICS_LOADED.equals(action)) {
				loadLyricsToView();
			} else if (BROADCAST_LYRICS_REFRESHED.equals(action)) {
				scrollLyrics(false);
			}
		}
	};

	private BroadcastReceiver mScreenTimeoutListener = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {

			if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
				if (mIntentDeRegistered) {
					IntentFilter f = new IntentFilter();
					f.addAction(BROADCAST_PLAYSTATE_CHANGED);
					f.addAction(BROADCAST_META_CHANGED);
					f.addAction(BROADCAST_NEW_LYRICS_LOADED);
					f.addAction(BROADCAST_LYRICS_REFRESHED);
					registerReceiver(mStatusListener, new IntentFilter(f));
					mIntentDeRegistered = false;
				}
				updateTrackInfo(false);
				loadLyricsToView();
				scrollLyrics(true);
				long next = refreshNow();
				queueNextRefresh(next);
			} else if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
				paused = true;

				if (!mIntentDeRegistered) {
					mHandler.removeMessages(REFRESH);
					unregisterReceiver(mStatusListener);
					mIntentDeRegistered = true;
				}
			}
		}
	};

	// TODO update track info with animation
	private void updateTrackInfo(boolean animation) {

		if (mService == null) {
			finish();
			return;
		}
		try {

			String artistName = mService.getArtistName();
			if (MediaStore.UNKNOWN_STRING.equals(artistName)) {
				artistName = getString(R.string.unknown_artist);
			}
			mArtistNameView.setText(artistName);
			String albumName = mService.getAlbumName();
			if (MediaStore.UNKNOWN_STRING.equals(albumName)) {
				albumName = getString(R.string.unknown_album);
			}
			mAlbumNameView.setText(albumName);

			getSupportActionBar().setTitle(mService.getTrackName());

			if (mAlbumArtLoader != null) mAlbumArtLoader.cancel(true);
			mAlbumArtLoader = new AsyncAlbumArtLoader(mAlbum, animation);
			mAlbumArtLoader.execute();

			if (mColorAnalyser != null) mColorAnalyser.cancel(true);
			mColorAnalyser = new AsyncColorAnalyser();
			mColorAnalyser.execute();

			mDuration = mService.duration();
			mTotalTime.setText(MusicUtils.makeTimeString(this, mDuration / 1000));

		} catch (RemoteException e) {
			e.printStackTrace();
			finish();
		}
	}

	private class AsyncAlbumArtLoader extends AsyncTask<Void, Void, Bitmap> {

		boolean enable_animation = false;
		private ImageView mImageView;

		public AsyncAlbumArtLoader(ImageView iv, boolean animation) {

			mImageView = iv;
			enable_animation = animation;
		}

		@Override
		protected void onPreExecute() {

			if (enable_animation) {
				mImageView.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(),
						android.R.anim.fade_out));
				mImageView.setVisibility(View.INVISIBLE);
			}
		}

		@Override
		protected Bitmap doInBackground(Void... params) {

			if (mService != null) {
				try {
					return mService.getAlbumArt();
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(Bitmap result) {

			if (result != null) {
				mImageView.setImageBitmap(result);
			} else {
				mImageView.setImageResource(R.drawable.ic_mp_albumart_unknown);
			}
			if (enable_animation) {
				mImageView.setVisibility(View.VISIBLE);
				mImageView.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(),
						android.R.anim.fade_in));
			}
		}
	}

	private class AsyncColorAnalyser extends AsyncTask<Void, Void, Integer> {

		@Override
		protected Integer doInBackground(Void... params) {

			if (mService != null) {
				try {
					if (mAutoColor) {
						mUIColor = ColorAnalyser.analyse(mService.getAlbumArt());
					} else {
						mUIColor = mPrefs.getIntPref(KEY_CUSTOMIZED_COLOR, Color.WHITE);
					}
					return mUIColor;
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
			return Color.WHITE;
		}

		@Override
		protected void onPostExecute(Integer result) {

			setUIColor(mUIColor);
		}
	}

	private void setUIColor(int color) {

		LayerDrawable mLayerDrawableProgress = (LayerDrawable) mProgress.getProgressDrawable();
		mLayerDrawableProgress.getDrawable(mLayerDrawableProgress.getNumberOfLayers() - 1)
				.setColorFilter(mUIColor, Mode.MULTIPLY);
		mProgress.invalidate();
	}

	private class ButtonStateDrawable extends LayerDrawable {

		int pressed = android.R.attr.state_pressed;
		int focused = android.R.attr.state_focused;

		public ButtonStateDrawable(Drawable[] layers) {

			super(layers);
		}

		@Override
		protected boolean onStateChange(int[] states) {

			for (int state : states) {
				if (state == pressed || state == focused) {
					super.setColorFilter(mUIColor, Mode.MULTIPLY);
					return super.onStateChange(states);
				}
			}
			super.clearColorFilter();
			return super.onStateChange(states);
		}

		@Override
		public boolean isStateful() {

			return super.isStateful();
		}
	}
}
