/*
 *			Copyright (C) 2012 The MusicMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *			http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.musicmod.android;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.PowerManager.WakeLock;
import android.provider.MediaStore;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.Toast;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import org.musicmod.android.util.EqualizerWrapper;
import org.musicmod.android.util.LyricsParser;
import org.musicmod.android.util.MusicUtils;
import org.musicmod.android.util.ShakeListener;
import org.musicmod.android.util.ShakeListener.OnShakeListener;
import org.musicmod.android.util.PreferencesEditor;

/**
 * Provides "background" audio playback capabilities, allowing the user to
 * switch between activities without stopping playback.
 */
public class MusicPlaybackService extends Service implements Constants, OnShakeListener {

	/**
	 * used to specify whether enqueue() should start playing the new list of
	 * files right away, next or once all the currently queued files have been
	 * played
	 */

	private static final int TRACK_ENDED = 1;
	private static final int RELEASE_WAKELOCK = 2;
	private static final int SERVER_DIED = 3;
	private static final int FOCUSCHANGE = 4;
	private static final int FADEDOWN = 5;
	private static final int FADEUP = 6;

	private static final int NEW_LYRICS_LOADED = 1;
	private static final int POSITION_CHANGED = 2;
	private static final int LYRICS_REFRESHED = 3;
	private static final int LYRICS_PAUSED = 4;
	private static final int LYRICS_RESUMED = 5;

	private static final int START_SLEEP_TIMER = 1;
	private static final int STOP_SLEEP_TIMER = 2;

	private MultiPlayer mPlayer;
	private String mFileToPlay;
	private NotificationManager mNotification;
	private int mShuffleMode = SHUFFLE_NONE;
	private int mRepeatMode = REPEAT_NONE;
	private int mMediaMountedCount = 0;
	private long[] mPlayList = null;
	private int mPlayListLen = 0;
	private Vector<Integer> mHistory = new Vector<Integer>();
	private Cursor mCursor;
	private int mPlayPos = -1;
	private final Shuffler mShuffler = new Shuffler();
	private int mOpenFailedCounter = 0;
	private String[] mCursorCols = new String[] { "audio._id AS _id",
			MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM,
			MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DATA,
			MediaStore.Audio.Media.MIME_TYPE, MediaStore.Audio.Media.ARTIST_ID,
			MediaStore.Audio.Media.ALBUM_ID, MediaStore.Audio.Media.IS_PODCAST,
			MediaStore.Audio.Media.BOOKMARK };
	private final static int IDCOLIDX = 0;
	private final static int PODCASTCOLIDX = 8;
	private final static int BOOKMARKCOLIDX = 9;
	private BroadcastReceiver mUnmountReceiver = null;
	private BroadcastReceiver mA2dpReceiver = null;
	private WakeLock mWakeLock;
	private int mServiceStartId = -1;
	private boolean mServiceInUse = false;
	private boolean mIsSupposedToBePlaying = false;
	private boolean mQuietMode = false;
	private AudioManager mAudioManager;
	private boolean mQueueIsSaveable = true;
	// used to track what type of audio focus loss caused the playback to pause
	private boolean mPausedByTransientLossOfFocus = false;
	// used to track current volume
	private float mCurrentVolume = 1.0f;

	private PreferencesEditor mPrefs;
	// We use this to distinguish between different cards when saving/restoring
	// playlists.
	// This will have to change if we want to support multiple simultaneous
	// cards.
	private int mCardId;

	private MediaAppWidgetProvider4x1 mAppWidgetProvider4x1 = MediaAppWidgetProvider4x1
			.getInstance();
	private MediaAppWidgetProvider4x2 mAppWidgetProvider4x2 = MediaAppWidgetProvider4x2
			.getInstance();

	// interval after which we stop the service when idle
	private static final int IDLE_DELAY = 60000;

	private boolean mGentleSleepTimer, mSleepTimerTimedUp;
	private long mCurrentTimestamp, mStopTimestamp;

	private EqualizerWrapper mEqualizer = null;
	private boolean mEqualizerSupported = EqualizerWrapper.isSupported();

	private LyricsParser mLyricsParser = new LyricsParser();
	private int mLyricsStatus = 0;
	private int mLyricsId = -1;
	private String[] mLyrics = new String[] {};

	private ShakeListener mShakeDetector;
	private boolean mShakeEnabled;
	private String mShakingBehavior;
	private float mShakingThreshold = DEFAULT_SHAKING_THRESHOLD;

	private boolean mScrobbleEnabled = false;

	private Handler mMediaplayerHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {

			MusicUtils.debugLog("mMediaplayerHandler.handleMessage " + msg.what);
			switch (msg.what) {
				case FADEDOWN:
					mCurrentVolume -= 0.05f;
					if (mCurrentVolume > 0.2f) {
						mMediaplayerHandler.sendEmptyMessageDelayed(FADEDOWN, 10);
					} else {
						mCurrentVolume = 0.2f;
					}
					mPlayer.setVolume(mCurrentVolume);
					break;
				case FADEUP:
					mCurrentVolume += 0.01f;
					if (mCurrentVolume < 1.0f) {
						mMediaplayerHandler.sendEmptyMessageDelayed(FADEUP, 10);
					} else {
						mCurrentVolume = 1.0f;
					}
					mPlayer.setVolume(mCurrentVolume);
					break;
				case SERVER_DIED:
					if (mIsSupposedToBePlaying) {
						next(true);
					} else {
						// the server died when we were idle, so just
						// reopen the same song (it will start again
						// from the beginning though when the user
						// restarts)
						openCurrent();
					}
					break;
				case TRACK_ENDED:
					if (mRepeatMode == REPEAT_CURRENT) {
						seek(0);
						play();
					} else {
						next(false);
					}
					break;
				case RELEASE_WAKELOCK:
					mWakeLock.release();
					break;
				case FOCUSCHANGE:
					// This code is here so we can better synchronize it with
					// the code that handles fade-in
					switch (msg.arg1) {
						case AudioManager.AUDIOFOCUS_LOSS:
							Log.v(LOGTAG_SERVICE, "AudioFocus: received AUDIOFOCUS_LOSS");
							if (isPlaying()) {
								mPausedByTransientLossOfFocus = false;
							}
							pause();
							break;
						case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
							mMediaplayerHandler.removeMessages(FADEUP);
							mMediaplayerHandler.sendEmptyMessage(FADEDOWN);
							break;
						case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
							Log.v(LOGTAG_SERVICE, "AudioFocus: received AUDIOFOCUS_LOSS_TRANSIENT");
							if (isPlaying()) {
								mPlayer.setVolume((float) Math.pow(10.0, -8 / 20.0));
							}
							break;
						case AudioManager.AUDIOFOCUS_GAIN:
							Log.v(LOGTAG_SERVICE, "AudioFocus: received AUDIOFOCUS_GAIN");
							if (isPlaying() || mPausedByTransientLossOfFocus) {
								mPausedByTransientLossOfFocus = false;
								mCurrentVolume = 0f;
								mPlayer.setVolume(mCurrentVolume);
								play(); // also queues a fade-in
							} else {
								mMediaplayerHandler.removeMessages(FADEDOWN);
								mMediaplayerHandler.sendEmptyMessage(FADEUP);
							}
							break;
						default:
							Log.e(LOGTAG_SERVICE, "Unknown audio focus change code");
					}
					break;

				default:
					break;
			}
		}
	};

	public void reloadLyrics() {

		mLyricsHandler.removeCallbacksAndMessages(null);
		mLyricsHandler.sendEmptyMessage(NEW_LYRICS_LOADED);

	}

	/**
	 * Lyrics Handler
	 * 
	 * load lyrics automatically, most accurate and fastest ever.<br>
	 * Usage: send a empty message {@link #NEW_LYRICS_LOADED} when new song
	 * played.
	 * 
	 * @author mariotaku
	 */
	private Handler mLyricsHandler = new Handler() {

		private List<String> mLyricsList = new ArrayList<String>();
		private int lyrics_id = 0;

		@Override
		public void handleMessage(Message msg) {

			mLyricsHandler.removeMessages(LYRICS_REFRESHED);
			switch (msg.what) {
				case NEW_LYRICS_LOADED:
					mLyricsId = -1;
					lyrics_id = 0;

					if (getMediaPath() == null) return;

					String media_path = getMediaPath();
					String lyrics_path = media_path.substring(0, media_path.lastIndexOf("."))
							+ ".lrc";
					mLyricsStatus = mLyricsParser.parseLyrics(lyrics_path);
					mLyricsList = mLyricsParser.getAllLyrics();
					mLyrics = mLyricsList.toArray(new String[mLyricsList.size()]);
					notifyLyricsChange(BROADCAST_NEW_LYRICS_LOADED);
					if (mLyricsStatus == LYRICS_STATUS_OK && isPlaying()) {
						if (mLyricsParser.getTimestamp(lyrics_id) <= position()) {
							mLyricsHandler.sendEmptyMessage(LYRICS_REFRESHED);
						} else {
							mLyricsHandler.sendEmptyMessageDelayed(LYRICS_REFRESHED,
									mLyricsParser.getTimestamp(lyrics_id) - position());
						}
					}
					break;
				case LYRICS_REFRESHED:
					if (mLyricsParser == null || mLyricsStatus != LYRICS_STATUS_OK) {
						return;
					}
					if (lyrics_id < mLyricsParser.getAllLyrics().size()) {
						mLyricsId = lyrics_id;
						lyrics_id++;
						notifyLyricsChange(BROADCAST_LYRICS_REFRESHED);
						if (isPlaying()) {
							mLyricsHandler.sendEmptyMessageDelayed(LYRICS_REFRESHED,
									mLyricsParser.getTimestamp(lyrics_id) - position());
						}
					}
					break;
				case POSITION_CHANGED:
					if (mLyricsParser == null || mLyricsStatus != LYRICS_STATUS_OK) {
						return;
					}
					lyrics_id = mLyricsParser.getId(msg.arg1);
					mLyricsHandler.sendEmptyMessageDelayed(LYRICS_REFRESHED,
							mLyricsParser.getTimestamp(lyrics_id) - position());
					break;
				case LYRICS_PAUSED:
					if (mLyricsParser == null || mLyricsStatus != LYRICS_STATUS_OK) {
						return;
					}
					break;
				case LYRICS_RESUMED:
					if (mLyricsParser == null || mLyricsStatus != LYRICS_STATUS_OK) {
						return;
					}
					if (lyrics_id < mLyricsParser.getAllLyrics().size()) {
						lyrics_id = mLyricsParser.getId(position());
						mLyricsHandler.sendEmptyMessageDelayed(LYRICS_REFRESHED,
								mLyricsParser.getTimestamp(lyrics_id) - position());
					}
					break;
			}
		}
	};

	private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {

			String action = intent.getAction();
			String cmd = intent.getStringExtra("command");
			MusicUtils.debugLog("mIntentReceiver.onReceive " + action + " / " + cmd);
			if (CMDNEXT.equals(cmd) || NEXT_ACTION.equals(action)) {
				next(true);
			} else if (CMDPREVIOUS.equals(cmd) || PREVIOUS_ACTION.equals(action)) {
				prev();
			} else if (CMDTOGGLEPAUSE.equals(cmd) || TOGGLEPAUSE_ACTION.equals(action)) {
				if (isPlaying()) {
					pause();
					mPausedByTransientLossOfFocus = false;
				} else {
					play();
				}
			} else if (CMDPAUSE.equals(cmd) || PAUSE_ACTION.equals(action)) {
				pause();
				mPausedByTransientLossOfFocus = false;
			} else if (CMDSTOP.equals(cmd)) {
				pause();
				mPausedByTransientLossOfFocus = false;
				seek(0);
			} else if (CMDCYCLEREPEAT.equals(cmd) || CYCLEREPEAT_ACTION.equals(action)) {
				cycleRepeat();
			} else if (CMDTOGGLESHUFFLE.equals(cmd) || TOGGLESHUFFLE_ACTION.equals(action)) {
				toggleShuffle();
			} else if (CMDTOGGLEFAVORITE.equals(cmd)) {
				if (!isFavorite()) {
					addToFavorites();
				} else {
					removeFromFavorites();
				}
			} else if (MediaAppWidgetProvider4x1.CMDAPPWIDGETUPDATE.equals(cmd)) {
				// Someone asked us to refresh a set of specific widgets,
				// probably
				// because they were just added.
				int[] appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
				mAppWidgetProvider4x1.performUpdate(MusicPlaybackService.this, appWidgetIds);
			} else if (MediaAppWidgetProvider4x2.CMDAPPWIDGETUPDATE.equals(cmd)) {
				// Someone asked us to refresh a set of specific widgets,
				// probably
				// because they were just added.
				int[] appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
				mAppWidgetProvider4x2.performUpdate(MusicPlaybackService.this, appWidgetIds);
			}
		}
	};

	private OnAudioFocusChangeListener mAudioFocusListener = new OnAudioFocusChangeListener() {

		@Override
		public void onAudioFocusChange(int focusChange) {

			mMediaplayerHandler.obtainMessage(FOCUSCHANGE, focusChange, 0).sendToTarget();
		}
	};

	private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {

		@Override
		public void onCallStateChanged(int state, String incomingNumber) {

			switch (state) {
				case TelephonyManager.CALL_STATE_RINGING:
					Log.v(LOGTAG_SERVICE, "PhoneState: received CALL_STATE_RINGING");
					if (isPlaying()) {
						mPausedByTransientLossOfFocus = true;
						pause();
					}
					break;

				case TelephonyManager.CALL_STATE_OFFHOOK:
					Log.v(LOGTAG_SERVICE, "PhoneState: received CALL_STATE_OFFHOOK");
					mPausedByTransientLossOfFocus = false;
					if (isPlaying()) {
						pause();
					}
					break;
			}
		}
	};

	public MusicPlaybackService() {

	}

	@Override
	public void onCreate() {

		super.onCreate();

		mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

		mAudioManager.registerMediaButtonEventReceiver(new ComponentName(getPackageName(),
				MediaButtonIntentReceiver.class.getName()));

		mPrefs = new PreferencesEditor(getApplicationContext());

		mNotification = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mShakeDetector = new ShakeListener(this);

		mCardId = MusicUtils.getCardId(this);

		registerExternalStorageListener();
		registerA2dpServiceListener();

		// Needs to be done in this thread, since otherwise
		// ApplicationContext.getPowerManager() crashes.
		mPlayer = new MultiPlayer();
		mPlayer.setHandler(mMediaplayerHandler);

		if (mEqualizerSupported) {
			mEqualizer = new EqualizerWrapper(0, getAudioSessionId());
			if (mEqualizer != null) {
				mEqualizer.setEnabled(true);
				reloadEqualizer();
			}
		}

		reloadQueue();

		IntentFilter commandFilter = new IntentFilter();
		commandFilter.addAction(SERVICECMD);
		commandFilter.addAction(TOGGLEPAUSE_ACTION);
		commandFilter.addAction(PAUSE_ACTION);
		commandFilter.addAction(NEXT_ACTION);
		commandFilter.addAction(PREVIOUS_ACTION);
		commandFilter.addAction(CYCLEREPEAT_ACTION);
		commandFilter.addAction(TOGGLESHUFFLE_ACTION);
		commandFilter.addAction(BROADCAST_PLAYSTATUS_REQUEST);
		registerReceiver(mIntentReceiver, commandFilter);

		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this.getClass().getName());
		mWakeLock.setReferenceCounted(false);

		// If the service was idle, but got killed before it stopped itself, the
		// system will relaunch it. Make sure it gets stopped again in that
		// case.
		Message msg = mDelayedStopHandler.obtainMessage();
		mDelayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY);
	}

	@Override
	public void onShake() {

		mShakingBehavior = mPrefs.getStringPref(KEY_SHAKING_BEHAVIOR, DEFAULT_SHAKING_BEHAVIOR);

		if (!BEHAVIOR_PLAY_PAUSE.equals(mShakingBehavior)
				|| !BEHAVIOR_NEXT_SONG.equals(mShakingBehavior)) {
			return;
		}

		if (mPlayer.isInitialized()) {
			if (BEHAVIOR_PLAY_PAUSE.equals(mShakingBehavior)) {
				if (isPlaying()) {
					pause();
					Toast.makeText(this, R.string.pause_toast, Toast.LENGTH_SHORT).show();
				} else {
					play();
					Toast.makeText(this, R.string.play_toast, Toast.LENGTH_SHORT).show();
				}
			} else if (BEHAVIOR_NEXT_SONG.equals(mShakingBehavior)) {
				if (isPlaying()) {
					next(true);
					Toast.makeText(this, R.string.next_toast, Toast.LENGTH_SHORT).show();
				} else {
					play();
					Toast.makeText(this, R.string.play_toast, Toast.LENGTH_SHORT).show();
				}

			}
		} else {
			MusicUtils.shuffleAll(getApplicationContext());
			Toast.makeText(this, R.string.shuffle_all, Toast.LENGTH_SHORT).show();
		}

	}

	@Override
	public void onDestroy() {

		// Check that we're not being destroyed while something is still
		// playing.
		if (isPlaying()) {
			Log.e(LOGTAG_SERVICE, "Service being destroyed while still playing.");
		}

		if (mEqualizer != null) {
			mEqualizer.setEnabled(false);
			mEqualizer.release();
		}

		mPlayer.release();
		mPlayer = null;

		mAudioManager.abandonAudioFocus(mAudioFocusListener);

		TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		telephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);

		// make sure there aren't any other messages coming
		mDelayedStopHandler.removeCallbacksAndMessages(null);
		mMediaplayerHandler.removeCallbacksAndMessages(null);

		if (mCursor != null) {
			mCursor.close();
			mCursor = null;
		}

		unregisterReceiver(mIntentReceiver);
		unregisterReceiver(mA2dpReceiver);
		if (mUnmountReceiver != null) {
			unregisterReceiver(mUnmountReceiver);
			mUnmountReceiver = null;
		}
		mWakeLock.release();
		mNotification.cancelAll();
		super.onDestroy();
	}

	private final char hexdigits[] = new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
			'a', 'b', 'c', 'd', 'e', 'f' };

	private void saveQueue(boolean full) {

		if (!mQueueIsSaveable) {
			return;
		}

		// long start = System.currentTimeMillis();
		if (full) {
			StringBuilder q = new StringBuilder();

			// The current playlist is saved as a list of "reverse hexadecimal"
			// numbers, which we can generate faster than normal decimal or
			// hexadecimal numbers, which in turn allows us to save the playlist
			// more often without worrying too much about performance.
			// (saving the full state takes about 40 ms under no-load conditions
			// on the phone)
			int len = mPlayListLen;
			for (int i = 0; i < len; i++) {
				long n = mPlayList[i];
				if (n < 0) {
					continue;
				} else if (n == 0) {
					q.append("0;");
				} else {
					while (n != 0) {
						int digit = (int) (n & 0xf);
						n >>>= 4;
						q.append(hexdigits[digit]);
					}
					q.append(";");
				}
			}
			// Log.i("@@@@ service", "created queue string in " +
			// (System.currentTimeMillis() - start) + " ms");
			mPrefs.setStringState(STATE_KEY_QUEUE, q.toString());
			mPrefs.setIntState(STATE_KEY_CARDID, mCardId);
			if (mShuffleMode != SHUFFLE_NONE) {
				// In shuffle mode we need to save the history too
				len = mHistory.size();
				q.setLength(0);
				for (int i = 0; i < len; i++) {
					int n = mHistory.get(i);
					if (n == 0) {
						q.append("0;");
					} else {
						while (n != 0) {
							int digit = (n & 0xf);
							n >>>= 4;
							q.append(hexdigits[digit]);
						}
						q.append(";");
					}
				}
				mPrefs.setStringState(STATE_KEY_HISTORY, q.toString());
			}
		}
		mPrefs.setIntState(STATE_KEY_CURRPOS, mPlayPos);
		if (mPlayer.isInitialized()) {
			mPrefs.setLongState(STATE_KEY_SEEKPOS, mPlayer.position());
		}

		mPrefs.setIntState(STATE_KEY_REPEATMODE, mRepeatMode);
		mPrefs.setIntState(STATE_KEY_SHUFFLEMODE, mShuffleMode);

		// Log.i("@@@@ service", "saved state in " + (System.currentTimeMillis()
		// - start) + " ms");
	}

	private void reloadQueue() {

		String q = null;

		int id = mCardId;
		if (getSharedPreferences(SHAREDPREFS_STATES, Context.MODE_PRIVATE).contains(
				STATE_KEY_CARDID)) {
			id = mPrefs.getIntState(STATE_KEY_CARDID, mCardId);
		}
		if (id == mCardId) {
			// Only restore the saved playlist if the card is still
			// the same one as when the playlist was saved
			q = mPrefs.getStringState(STATE_KEY_QUEUE, "");
		}
		int qlen = q != null ? q.length() : 0;
		if (qlen > 1) {
			// Log.i("@@@@ service", "loaded queue: " + q);
			int plen = 0;
			int n = 0;
			int shift = 0;
			for (int i = 0; i < qlen; i++) {
				char c = q.charAt(i);
				if (c == ';') {
					ensurePlayListCapacity(plen + 1);
					mPlayList[plen] = n;
					plen++;
					n = 0;
					shift = 0;
				} else {
					if (c >= '0' && c <= '9') {
						n += ((c - '0') << shift);
					} else if (c >= 'a' && c <= 'f') {
						n += ((10 + c - 'a') << shift);
					} else {
						// bogus playlist data
						plen = 0;
						break;
					}
					shift += 4;
				}
			}
			mPlayListLen = plen;

			int pos = mPrefs.getIntState(STATE_KEY_CURRPOS, 0);
			if (pos < 0 || pos >= mPlayListLen) {
				// The saved playlist is bogus, discard it
				mPlayListLen = 0;
				return;
			}
			mPlayPos = pos;

			// When reloadQueue is called in response to a card-insertion,
			// we might not be able to query the media provider right away.
			// To deal with this, try querying for the current file, and if
			// that fails, wait a while and try again. If that too fails,
			// assume there is a problem and don't restore the state.
			Cursor crsr = MusicUtils.query(this, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
					new String[] { "_id" }, "_id=" + mPlayList[mPlayPos], null, null);
			if (crsr == null || crsr.getCount() == 0) {
				// wait a bit and try again
				SystemClock.sleep(3000);
				crsr = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
						mCursorCols, "_id=" + mPlayList[mPlayPos], null, null);
			}
			if (crsr != null) {
				crsr.close();
			}

			mOpenFailedCounter = 20;
			mQuietMode = true;
			openCurrent();
			mQuietMode = false;
			if (!mPlayer.isInitialized()) {
				// couldn't restore the saved state
				mPlayListLen = 0;
				return;
			}

			mLyricsHandler.sendEmptyMessage(NEW_LYRICS_LOADED);

			long seekpos = mPrefs.getLongState(STATE_KEY_SEEKPOS, 0);
			seek(seekpos >= 0 && seekpos < duration() ? seekpos : 0);
			Log.d(LOGTAG_SERVICE, "restored queue, currently at position " + position() + "/"
					+ duration() + " (requested " + seekpos + ")");

			int repmode = mPrefs.getIntState(STATE_KEY_REPEATMODE, REPEAT_NONE);
			if (repmode != REPEAT_ALL && repmode != REPEAT_CURRENT) {
				repmode = REPEAT_NONE;
			}
			mRepeatMode = repmode;

			int shufmode = mPrefs.getIntState(STATE_KEY_SHUFFLEMODE, SHUFFLE_NONE);
			if (shufmode != SHUFFLE_NORMAL) {
				shufmode = SHUFFLE_NONE;
			}
			if (shufmode != SHUFFLE_NONE) {
				// in shuffle mode we need to restore the history too
				q = mPrefs.getStringState(STATE_KEY_HISTORY, "");
				qlen = q != null ? q.length() : 0;
				if (qlen > 1) {
					plen = 0;
					n = 0;
					shift = 0;
					mHistory.clear();
					for (int i = 0; i < qlen; i++) {
						char c = q.charAt(i);
						if (c == ';') {
							if (n >= mPlayListLen) {
								// bogus history data
								mHistory.clear();
								break;
							}
							if (!mHistory.contains(mPlayPos)) {
								mHistory.add(mPlayPos);
							}
							n = 0;
							shift = 0;
						} else {
							if (c >= '0' && c <= '9') {
								n += ((c - '0') << shift);
							} else if (c >= 'a' && c <= 'f') {
								n += ((10 + c - 'a') << shift);
							} else {
								// bogus history data
								mHistory.clear();
								break;
							}
							shift += 4;
						}
					}
				}
			}
			mShuffleMode = shufmode;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {

		mDelayedStopHandler.removeCallbacksAndMessages(null);
		mServiceInUse = true;
		return mBinder;
	}

	@Override
	public void onRebind(Intent intent) {

		mDelayedStopHandler.removeCallbacksAndMessages(null);
		mServiceInUse = true;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		mServiceStartId = startId;
		mDelayedStopHandler.removeCallbacksAndMessages(null);

		if (intent != null) {
			String action = intent.getAction();
			String cmd = intent.getStringExtra("command");
			MusicUtils.debugLog("onStartCommand " + action + " / " + cmd);

			if (CMDNEXT.equals(cmd) || NEXT_ACTION.equals(action)) {
				next(true);
			} else if (CMDPREVIOUS.equals(cmd) || PREVIOUS_ACTION.equals(action)) {
				prev();
			} else if (CMDTOGGLEPAUSE.equals(cmd) || TOGGLEPAUSE_ACTION.equals(action)) {
				if (isPlaying()) {
					pause();
					mPausedByTransientLossOfFocus = false;
				} else {
					play();
				}
			} else if (CMDTOGGLEFAVORITE.equals(cmd)) {
				if (!isFavorite()) {
					addToFavorites();
				} else {
					removeFromFavorites();
				}
			} else if (CMDPAUSE.equals(cmd) || PAUSE_ACTION.equals(action)) {
				pause();
				mPausedByTransientLossOfFocus = false;
			} else if (CMDSTOP.equals(cmd)) {
				pause();
				mPausedByTransientLossOfFocus = false;
				seek(0);
			} else if (CMDCYCLEREPEAT.equals(cmd) || CYCLEREPEAT_ACTION.equals(action)) {
				cycleRepeat();
			} else if (CMDTOGGLESHUFFLE.equals(cmd) || TOGGLESHUFFLE_ACTION.equals(action)) {
				toggleShuffle();
			} else if (BROADCAST_PLAYSTATUS_REQUEST.equals(action)) {
				notifyChange(BROADCAST_PLAYSTATUS_RESPONSE);
			}
		}

		// make sure the service will shut down on its own if it was
		// just started but not bound to and nothing is playing
		mDelayedStopHandler.removeCallbacksAndMessages(null);
		Message msg = mDelayedStopHandler.obtainMessage();
		mDelayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY);
		return START_STICKY;
	}

	@Override
	public boolean onUnbind(Intent intent) {

		mServiceInUse = false;

		// Take a snapshot of the current playlist
		saveQueue(true);

		if (isPlaying() || mPausedByTransientLossOfFocus) {
			// something is currently playing, or will be playing once
			// an in-progress action requesting audio focus ends, so don't stop
			// the service now.
			return true;
		}

		// If there is a playlist but playback is paused, then wait a while
		// before stopping the service, so that pause/resume isn't slow.
		// Also delay stopping the service if we're transitioning between
		// tracks.
		if (mPlayListLen > 0 || mMediaplayerHandler.hasMessages(TRACK_ENDED)) {
			Message msg = mDelayedStopHandler.obtainMessage();
			mDelayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY);
			return true;
		}

		// No active playlist, OK to stop the service right now
		stopSelf(mServiceStartId);
		return true;
	}

	private Handler mDelayedStopHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {

			// Check again to make sure nothing is playing right now
			if (isPlaying() || mPausedByTransientLossOfFocus || mServiceInUse
					|| mMediaplayerHandler.hasMessages(TRACK_ENDED)) {
				return;
			}
			// save the queue again, because it might have changed
			// since the user exited the music app (because of
			// party-shuffle or because the play-position changed)
			saveQueue(true);
			stopSelf(mServiceStartId);
		}
	};

	/**
	 * Called when we receive a ACTION_MEDIA_EJECT notification.
	 * 
	 * @param storagePath
	 *            path to mount point for the removed media
	 */
	public void closeExternalStorageFiles(String storagePath) {

		stop(true);
		notifyChange(BROADCAST_QUEUE_CHANGED);
		notifyChange(BROADCAST_META_CHANGED);
	}

	/**
	 * Registers an intent to listen for ACTION_MEDIA_EJECT notifications. The
	 * intent will call closeExternalStorageFiles() if the external media is
	 * going to be ejected, so applications can clean up any files they have
	 * open.
	 */
	public void registerExternalStorageListener() {

		if (mUnmountReceiver == null) {
			mUnmountReceiver = new BroadcastReceiver() {

				@Override
				public void onReceive(Context context, Intent intent) {

					String action = intent.getAction();
					if (action.equals(Intent.ACTION_MEDIA_EJECT)) {
						saveQueue(true);
						mQueueIsSaveable = false;
						closeExternalStorageFiles(intent.getData().getPath());
					} else if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
						mMediaMountedCount++;
						mCardId = MusicUtils.getCardId(MusicPlaybackService.this);
						reloadQueue();
						mQueueIsSaveable = true;
						notifyChange(BROADCAST_QUEUE_CHANGED);
						notifyChange(BROADCAST_META_CHANGED);
					}
				}
			};
			IntentFilter iFilter = new IntentFilter();
			iFilter.addAction(Intent.ACTION_MEDIA_EJECT);
			iFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
			iFilter.addDataScheme("file");
			registerReceiver(mUnmountReceiver, iFilter);
		}
	}

	public void registerA2dpServiceListener() {

		mA2dpReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {

				String action = intent.getAction();
				if (BROADCAST_PLAYSTATUS_REQUEST.equals(action)) {
					notifyChange(BROADCAST_PLAYSTATUS_RESPONSE);
				}
			}
		};
		IntentFilter iFilter = new IntentFilter();
		iFilter.addAction(BROADCAST_PLAYSTATUS_REQUEST);
		registerReceiver(mA2dpReceiver, iFilter);
	}

	/**
	 * Notify the change-receivers that something has changed. The intent that
	 * is sent contains the following data for the currently playing track: "id"
	 * - Integer: the database row ID "artist" - String: the name of the artist
	 * "album_artist" - String: the name of the album artist "album" - String:
	 * the name of the album "track" - String: the name of the track The intent
	 * has an action that is one of "org.musicmod.android.metachanged"
	 * "org.musicmod.android.queuechanged",
	 * "org.musicmod.android.playbackcomplete"
	 * "org.musicmod.android.playstatechanged" respectively indicating that a
	 * new track has started playing, that the playback queue has changed, that
	 * playback has stopped because the last file in the list has been played,
	 * or that the play-state changed (paused/resumed).
	 */
	private void notifyChange(String what) {

		Intent i = new Intent(what);
		i.putExtra(BROADCAST_KEY_ID, getAudioId());
		i.putExtra(BROADCAST_KEY_ARTIST, getArtistName());
		i.putExtra(BROADCAST_KEY_ALBUM, getAlbumName());
		i.putExtra(BROADCAST_KEY_TRACK, getTrackName());
		i.putExtra(BROADCAST_KEY_PLAYING, isPlaying());
		i.putExtra(BROADCAST_KEY_ISFAVORITE, isFavorite());
		i.putExtra(BROADCAST_KEY_SONGID, getAudioId());
		i.putExtra(BROADCAST_KEY_ALBUMID, getAlbumId());
		i.putExtra(BROADCAST_KEY_DURATION, duration());
		i.putExtra(BROADCAST_KEY_POSITION, position());
		if (mPlayList != null)
			i.putExtra(BROADCAST_KEY_LISTSIZE, Long.valueOf(mPlayList.length));
		else
			i.putExtra(BROADCAST_KEY_LISTSIZE, Long.valueOf(mPlayListLen));
		sendBroadcast(i);

		if (BROADCAST_META_CHANGED.equals(what)) {
			mLyricsHandler.sendEmptyMessage(NEW_LYRICS_LOADED);
			if (isPlaying()) {
				sendScrobbleBroadcast(SCROBBLE_PLAYSTATE_START);
			} else {
				sendScrobbleBroadcast(SCROBBLE_PLAYSTATE_COMPLETE);
			}
		}
		if (BROADCAST_PLAYSTATE_CHANGED.equals(what)) {
			notifyLyricsChange(BROADCAST_LYRICS_REFRESHED);
			if (isPlaying()) {
				mLyricsHandler.sendEmptyMessage(LYRICS_RESUMED);
				sendScrobbleBroadcast(SCROBBLE_PLAYSTATE_RESUME);
			} else {
				mLyricsHandler.sendEmptyMessage(LYRICS_PAUSED);
				sendScrobbleBroadcast(SCROBBLE_PLAYSTATE_PAUSE);
			}
		}

		if (BROADCAST_QUEUE_CHANGED.equals(what)) {
			saveQueue(true);
		} else {
			saveQueue(false);
		}

		// Share this notification directly with our widgets
		mAppWidgetProvider4x1.notifyChange(this, what);
		mAppWidgetProvider4x2.notifyChange(this, what);
	}

	public void notifyLyricsChange(String action) {

		Intent i = new Intent(action);
		if (BROADCAST_LYRICS_REFRESHED.equals(action)) {
			i.putExtra(BROADCAST_KEY_LYRICS_ID, mLyricsId);
		} else if (BROADCAST_NEW_LYRICS_LOADED.equals(action)) {
			i.putExtra(BROADCAST_KEY_LYRICS_STATUS, mLyricsStatus);
		} else {
			return;
		}
		sendBroadcast(i);
	}

	private void sendScrobbleBroadcast(int state) {

		mScrobbleEnabled = mPrefs.getBooleanPref(KEY_ENABLE_SCROBBLING, false);

		// check that state is a valid state
		if (state != SCROBBLE_PLAYSTATE_START && state != SCROBBLE_PLAYSTATE_RESUME
				&& state != SCROBBLE_PLAYSTATE_PAUSE && state != SCROBBLE_PLAYSTATE_COMPLETE) {
			return;
		}

		if (mScrobbleEnabled) {
			Intent i = new Intent(SCROBBLE_SLS_API);

			i.putExtra(BROADCAST_KEY_APP_NAME, getString(R.string.app_name));
			i.putExtra(BROADCAST_KEY_APP_PACKAGE, getPackageName());

			i.putExtra(BROADCAST_KEY_STATE, state);

			i.putExtra(BROADCAST_KEY_ARTIST, getArtistName());
			i.putExtra(BROADCAST_KEY_ALBUM, getAlbumName());
			i.putExtra(BROADCAST_KEY_TRACK, getTrackName());
			i.putExtra(BROADCAST_KEY_DURATION, (int) (duration() / 1000));

			sendBroadcast(i);
		}

	}

	private void ensurePlayListCapacity(int size) {

		if (mPlayList == null || size > mPlayList.length) {
			// reallocate at 2x requested size so we don't
			// need to grow and copy the array for every
			// insert
			long[] newlist = new long[size * 2];
			int len = mPlayList != null ? mPlayList.length : mPlayListLen;
			for (int i = 0; i < len; i++) {
				newlist[i] = mPlayList[i];
			}
			mPlayList = newlist;
		}
	}

	// insert the list of songs at the specified position in the playlist
	private void addToPlayList(long[] list, int position) {

		int addlen = list.length;
		if (position < 0) { // overwrite
			mPlayListLen = 0;
			position = 0;
		}
		ensurePlayListCapacity(mPlayListLen + addlen);
		if (position > mPlayListLen) {
			position = mPlayListLen;
		}

		// move part of list after insertion point
		int tailsize = mPlayListLen - position;
		for (int i = tailsize; i > 0; i--) {
			mPlayList[position + i] = mPlayList[position + i - addlen];
		}

		// copy list into playlist
		for (int i = 0; i < addlen; i++) {
			mPlayList[position + i] = list[i];
		}
		mPlayListLen += addlen;
		if (mPlayListLen == 0) {
			mCursor.close();
			mCursor = null;
			notifyChange(BROADCAST_META_CHANGED);
		}
	}

	private void startSleepTimer(long milliseconds, boolean gentle) {

		Calendar now = Calendar.getInstance();
		mCurrentTimestamp = now.getTimeInMillis();
		mStopTimestamp = mCurrentTimestamp + milliseconds;

		int format_flags = DateUtils.FORMAT_NO_NOON_MIDNIGHT | DateUtils.FORMAT_ABBREV_ALL
				| DateUtils.FORMAT_CAP_AMPM;

		format_flags |= (DateUtils.FORMAT_SHOW_TIME);
		String time = DateUtils.formatDateTime(this, mStopTimestamp, format_flags);

		CharSequence contentTitle = getString(R.string.sleep_timer_enabled);
		CharSequence contentText = getString(R.string.notification_sleep_timer, time);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(), 0);
		Notification notification = new Notification(R.drawable.ic_stat_playback, null, 0);
		notification.flags = Notification.FLAG_ONGOING_EVENT;
		notification.icon = R.drawable.ic_stat_sleeptimer;
		notification.setLatestEventInfo(this, contentTitle, contentText, contentIntent);

		mGentleSleepTimer = gentle;
		mNotification.notify(SLEEPTIMER_STATUS, notification);
		mSleepTimerHandler.sendEmptyMessageDelayed(START_SLEEP_TIMER, milliseconds);
		Toast.makeText(
				this,
				getResources().getQuantityString(R.plurals.NNNminutes_notif,
						Integer.valueOf((int) milliseconds / 60 / 1000),
						Integer.valueOf((int) milliseconds / 60 / 1000)), Toast.LENGTH_SHORT)
				.show();
	}

	private void stopSleepTimer() {

		mSleepTimerHandler.sendEmptyMessage(STOP_SLEEP_TIMER);
		Toast.makeText(this, R.string.sleep_timer_disabled, Toast.LENGTH_SHORT).show();
	}

	private long getSleepTimerRemained() {

		Calendar now = Calendar.getInstance();
		long mCurrentTimestamp = now.getTimeInMillis();
		if (mStopTimestamp != 0) {
			return mStopTimestamp - mCurrentTimestamp;
		} else {
			return 0;
		}
	}

	private Handler mSleepTimerHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {

			switch (msg.what) {
				case START_SLEEP_TIMER:
					mSleepTimerHandler.removeMessages(START_SLEEP_TIMER, null);
					if (mGentleSleepTimer) {
						if (isPlaying()) {
							mSleepTimerTimedUp = true;
						} else {
							pause();
							mNotification.cancel(SLEEPTIMER_STATUS);
						}
					} else {
						pause();
						mNotification.cancel(SLEEPTIMER_STATUS);
					}
					mStopTimestamp = 0;
					break;
				case STOP_SLEEP_TIMER:
					mStopTimestamp = 0;
					mSleepTimerHandler.removeMessages(START_SLEEP_TIMER, null);
					mNotification.cancel(SLEEPTIMER_STATUS);
					break;
			}
		}
	};

	// TODO reload server settings
	public void reloadSettings() {

		mShakeEnabled = mPrefs.getBooleanPref(KEY_SHAKE_ENABLED, false);
		mShakingThreshold = mPrefs.getFloatPref(KEY_SHAKING_THRESHOLD, DEFAULT_SHAKING_THRESHOLD);
		if (mShakeDetector != null) {
			mShakeDetector.stop();
			if (mShakeEnabled) {
				mShakeDetector.start();
			}
			mShakeDetector.setShakeThreshold(mShakingThreshold);
		}
	}

	/**
	 * Appends a list of tracks to the current playlist. If nothing is playing
	 * currently, playback will be started at the first track. If the action is
	 * NOW, playback will switch to the first of the new tracks immediately.
	 * 
	 * @param list
	 *            The list of tracks to append.
	 * @param action
	 *            NOW, NEXT or LAST
	 */
	public void enqueue(long[] list, int action) {

		synchronized (this) {
			if (action == NEXT && mPlayPos + 1 < mPlayListLen) {
				addToPlayList(list, mPlayPos + 1);
				notifyChange(BROADCAST_QUEUE_CHANGED);
			} else {
				// action == LAST || action == NOW || mPlayPos + 1 ==
				// mPlayListLen
				addToPlayList(list, Integer.MAX_VALUE);
				notifyChange(BROADCAST_QUEUE_CHANGED);
				if (action == NOW) {
					mPlayPos = mPlayListLen - list.length;
					openCurrent();
					play();
					notifyChange(BROADCAST_META_CHANGED);
					return;
				}
			}
			if (mPlayPos < 0) {
				mPlayPos = 0;
				openCurrent();
				play();
				notifyChange(BROADCAST_META_CHANGED);
			}
		}
	}

	/**
	 * Replaces the current playlist with a new list, and prepares for starting
	 * playback at the specified position in the list, or a random position if
	 * the specified position is 0.
	 * 
	 * @param list
	 *            The new list of tracks.
	 */
	public void open(long[] list, int position) {

		synchronized (this) {
			long oldId = getAudioId();
			int listlength = list.length;
			boolean newlist = true;
			if (mPlayListLen == listlength) {
				// possible fast path: list might be the same
				newlist = false;
				for (int i = 0; i < listlength; i++) {
					if (list[i] != mPlayList[i]) {
						newlist = true;
						break;
					}
				}
			}
			if (newlist) {
				addToPlayList(list, -1);
				notifyChange(BROADCAST_QUEUE_CHANGED);
			}
			if (position >= 0) {
				mPlayPos = position;
			} else {
				mPlayPos = mShuffler.shuffle(mPlayListLen);
			}
			mHistory.clear();

			saveBookmarkIfNeeded();
			openCurrent();
			if (oldId != getAudioId()) {
				notifyChange(BROADCAST_META_CHANGED);
			}
		}
	}

	/**
	 * Moves the item at index1 to index2.
	 * 
	 * @param from
	 * @param to
	 */
	public void moveQueueItem(int from, int to) {

		synchronized (this) {
			if (from >= mPlayListLen) {
				from = mPlayListLen - 1;
			}
			if (to >= mPlayListLen) {
				to = mPlayListLen - 1;
			}
			if (from < to) {
				long tmp = mPlayList[from];
				for (int i = from; i < to; i++) {
					mPlayList[i] = mPlayList[i + 1];
				}
				mPlayList[to] = tmp;
				if (mPlayPos == from) {
					mPlayPos = to;
				} else if (mPlayPos >= from && mPlayPos <= to) {
					mPlayPos--;
				}
			} else if (to < from) {
				long tmp = mPlayList[from];
				for (int i = from; i > to; i--) {
					mPlayList[i] = mPlayList[i - 1];
				}
				mPlayList[to] = tmp;
				if (mPlayPos == from) {
					mPlayPos = to;
				} else if (mPlayPos >= to && mPlayPos <= from) {
					mPlayPos++;
				}
			}
			notifyChange(BROADCAST_QUEUE_CHANGED);
		}
	}

	public void toggleFavorite() {
		if (!isFavorite()) {
			addToFavorites();
		} else {
			removeFromFavorites();
		}
	}

	public void addToFavorites() {
		if (getAudioId() >= 0) {
			addToFavorites(getAudioId());
		}
	}

	public void removeFromFavorites() {
		if (getAudioId() >= 0) {
			removeFromFavorites(getAudioId());
		}
	}

	public boolean isFavorite() {
		if (getAudioId() >= 0) {
			return isFavorite(getAudioId());
		}
		return false;
	}

	public void addToFavorites(long id) {
		MusicUtils.addToFavorites(this, id);
		notifyChange(BROADCAST_FAVORITESTATE_CHANGED);
	}

	public void removeFromFavorites(long id) {
		MusicUtils.removeFromFavorites(this, id);
		notifyChange(BROADCAST_FAVORITESTATE_CHANGED);
	}

	public boolean isFavorite(long id) {
		return MusicUtils.isFavorite(this, id);
	}

	/**
	 * Returns the current play list
	 * 
	 * @return An array of integers containing the IDs of the tracks in the play
	 *         list
	 */
	public long[] getQueue() {

		synchronized (this) {
			int len = mPlayListLen;
			long[] list = new long[len];
			for (int i = 0; i < len; i++) {
				list[i] = mPlayList[i];
			}
			return list;
		}
	}

	private void openCurrent() {

		synchronized (this) {
			if (mCursor != null) {
				mCursor.close();
				mCursor = null;
			}

			if (mPlayListLen == 0) {
				return;
			}
			stop(false);

			String id = String.valueOf(mPlayList[mPlayPos]);

			mCursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
					mCursorCols, "_id=" + id, null, null);
			if (mCursor != null) {
				mCursor.moveToFirst();
				open(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI + "/" + id);
				// go to bookmark if needed
				if (isPodcast()) {
					long bookmark = getBookmark();
					// Start playing a little bit before the bookmark,
					// so it's easier to get back in to the narrative.
					seek(bookmark - 5000);
				}
			}
		}
	}

	/**
	 * Opens the specified file and readies it for playback.
	 * 
	 * @param path
	 *            The full path of the file to be opened.
	 */
	public void open(String path) {

		synchronized (this) {
			if (path == null) {
				return;
			}

			// if mCursor is null, try to associate path with a database cursor
			if (mCursor == null) {

				ContentResolver resolver = getContentResolver();
				Uri uri;
				String where;
				String selectionArgs[];
				if (path.startsWith("content://media/")) {
					uri = Uri.parse(path);
					where = null;
					selectionArgs = null;
				} else {
					uri = MediaStore.Audio.Media.getContentUriForPath(path);
					where = MediaStore.Audio.Media.DATA + "=?";
					selectionArgs = new String[] { path };
				}

				try {
					mCursor = resolver.query(uri, mCursorCols, where, selectionArgs, null);
					if (mCursor != null) {
						if (mCursor.getCount() == 0) {
							mCursor.close();
							mCursor = null;
						} else {
							mCursor.moveToNext();
							ensurePlayListCapacity(1);
							mPlayListLen = 1;
							mPlayList[0] = mCursor.getLong(IDCOLIDX);
							mPlayPos = 0;
						}
					}
				} catch (UnsupportedOperationException ex) {
				}
			}
			mFileToPlay = path;
			mPlayer.setDataSource(mFileToPlay);
			if (!mPlayer.isInitialized()) {
				stop(true);
				if (mOpenFailedCounter++ < 10 && mPlayListLen > 1) {
					// beware: this ends up being recursive because next() calls
					// open() again.
					next(false);
				}
				if (!mPlayer.isInitialized() && mOpenFailedCounter != 0) {
					// need to make sure we only shows this once
					mOpenFailedCounter = 0;
					if (!mQuietMode) {
						Toast.makeText(this, R.string.playback_failed, Toast.LENGTH_SHORT).show();
					}
					Log.d(LOGTAG_SERVICE, "Failed to open file for playback");
				}
			} else {
				mOpenFailedCounter = 0;
			}
		}
	}

	/**
	 * Starts playback of a previously opened file.
	 */
	public void play() {

		CharSequence contentTitle, contentText = null;
		PendingIntent contentIntent;

		TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		if (telephonyManager.getCallState() == TelephonyManager.CALL_STATE_OFFHOOK) {
			return;
		}

		mAudioManager.requestAudioFocus(mAudioFocusListener, AudioManager.STREAM_MUSIC,
				AudioManager.AUDIOFOCUS_GAIN);
		mAudioManager.registerMediaButtonEventReceiver(new ComponentName(this.getPackageName(),
				MediaButtonIntentReceiver.class.getName()));

		telephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);

		if (mPlayer.isInitialized()) {
			// if we are at the end of the song, go to the next song first
			long duration = mPlayer.duration();
			if (mRepeatMode != REPEAT_CURRENT && duration > 2000
					&& mPlayer.position() >= duration - 2000) {
				next(true);
			}

			mPlayer.start();

			// make sure we fade in, in case a previous fadein was stopped
			// because
			// of another focus loss
			mMediaplayerHandler.removeMessages(FADEDOWN);
			mMediaplayerHandler.sendEmptyMessage(FADEUP);

			contentTitle = getTrackName();

			String artist = getArtistName();
			boolean isUnknownArtist = (artist == null || MediaStore.UNKNOWN_STRING.equals(artist));

			String album = getAlbumName();
			boolean isUnknownAlbum = (album == null || MediaStore.UNKNOWN_STRING.equals(album));

			if (!isUnknownArtist && !isUnknownAlbum) {
				contentText = getString(R.string.notification_artist_album, artist, album);
			} else if (isUnknownArtist && !isUnknownAlbum) {
				contentText = album;
			} else if (!isUnknownArtist && isUnknownAlbum) {
				contentText = artist;
			}

			contentIntent = PendingIntent.getActivity(this, 0, new Intent(INTENT_PLAYBACK_VIEWER),
					0);

			Notification status = new Notification(R.drawable.ic_stat_playback, null, 0);
			status.flags = Notification.FLAG_ONGOING_EVENT;
			status.icon = R.drawable.ic_stat_playback;
			status.setLatestEventInfo(this, contentTitle, contentText, contentIntent);
			mNotification.notify(PLAYBACKSERVICE_STATUS, status);

			if (!mIsSupposedToBePlaying) {
				mIsSupposedToBePlaying = true;
				notifyChange(BROADCAST_PLAYSTATE_CHANGED);
			}

		} else if (mPlayListLen <= 0) {
			// This is mostly so that if you press 'play' on a bluetooth headset
			// without every having played anything before, it will still play
			// something.
			setShuffleMode(SHUFFLE_NORMAL);
		}
	}

	private void stop(boolean remove_status_icon) {

		if (mPlayer.isInitialized()) {
			mPlayer.stop();
		}
		mFileToPlay = null;
		if (mCursor != null) {
			mCursor.close();
			mCursor = null;
		}
		if (remove_status_icon) {
			gotoIdleState();
		} else {
			mNotification.cancel(PLAYBACKSERVICE_STATUS);
		}
		if (remove_status_icon) {
			mIsSupposedToBePlaying = false;
		}
	}

	/**
	 * Stops playback.
	 */
	public void stop() {

		stop(true);
	}

	/**
	 * Pauses playback (call play() to resume)
	 */
	public void pause() {

		synchronized (this) {
			mMediaplayerHandler.removeMessages(FADEUP);
			if (isPlaying()) {

				mPlayer.pause();
				gotoIdleState();
				mIsSupposedToBePlaying = false;
				notifyChange(BROADCAST_PLAYSTATE_CHANGED);
				saveBookmarkIfNeeded();
			}
		}
	}

	/**
	 * Returns whether something is currently playing
	 * 
	 * @return true if something is playing (or will be playing shortly, in case
	 *         we're currently transitioning between tracks), false if not.
	 */
	public boolean isPlaying() {

		return mIsSupposedToBePlaying;
	}

	/*
	 * Desired behavior for prev/next/shuffle:
	 * 
	 * - NEXT will move to the next track in the list when not shuffling, and to
	 * a track randomly picked from the not-yet-played tracks when shuffling. If
	 * all tracks have already been played, pick from the full set, but avoid
	 * picking the previously played track if possible. - when shuffling, PREV
	 * will go to the previously played track. Hitting PREV again will go to the
	 * track played before that, etc. When the start of the history has been
	 * reached, PREV is a no-op. When not shuffling, PREV will go to the
	 * sequentially previous track (the difference with the shuffle-case is
	 * mainly that when not shuffling, the user can back up to tracks that are
	 * not in the history).
	 * 
	 * Example: When playing an album with 10 tracks from the start, and
	 * enabling shuffle while playing track 5, the remaining tracks (6-10) will
	 * be shuffled, e.g. the final play order might be 1-2-3-4-5-8-10-6-9-7.
	 * When hitting 'prev' 8 times while playing track 7 in this example, the
	 * user will go to tracks 9-6-10-8-5-4-3-2. If the user then hits 'next', a
	 * random track will be picked again. If at any time user disables shuffling
	 * the next/previous track will be picked in sequential order again.
	 */

	public void prev() {

		synchronized (this) {
			if (mShuffleMode == SHUFFLE_NORMAL) {
				// go to previously-played track and remove it from the history
				int histsize = mHistory.size();
				if (histsize == 0) {
					// prev is a no-op
					return;
				}
				Integer pos = mHistory.remove(histsize - 1);
				mPlayPos = pos.intValue();
			} else {
				if (mPlayPos > 0) {
					mPlayPos--;
				} else {
					mPlayPos = mPlayListLen - 1;
				}
			}
			saveBookmarkIfNeeded();
			stop(false);
			openCurrent();
			play();
			notifyChange(BROADCAST_META_CHANGED);
		}
	}

	public void next(boolean force) {

		synchronized (this) {
			if (mSleepTimerTimedUp) {
				pause();
				mNotification.cancel(SLEEPTIMER_STATUS);
				mSleepTimerTimedUp = false;
				return;
			}

			if (mPlayListLen <= 0) {
				Log.d(LOGTAG_SERVICE, "No play queue");
				return;
			}

			if (mShuffleMode == SHUFFLE_NORMAL) {
				if (mPlayPos >= 0) {
					if (!mHistory.contains(mPlayPos)) {
						mHistory.add(mPlayPos);
					}
				}

				int numTracks = mPlayListLen;
				int[] tracks = new int[numTracks];
				for (int i = 0; i < numTracks; i++) {
					tracks[i] = i;
				}

				int numHistory = mHistory.size();
				int numUnplayed = numTracks;
				for (int i = 0; i < numHistory; i++) {
					int idx = mHistory.get(i).intValue();
					if (idx < numTracks && tracks[idx] >= 0) {
						numUnplayed--;
						tracks[idx] = -1;
					}
				}

				// 'numUnplayed' now indicates how many tracks have not yet
				// been played, and 'tracks' contains the indices of those
				// tracks.
				if (numUnplayed <= 0) {
					// everything's already been played
					if (mRepeatMode == REPEAT_ALL || force) {
						// pick from full set
						numUnplayed = numTracks;
						for (int i = 0; i < numTracks; i++) {
							tracks[i] = i;
						}
					} else {
						// all done
						gotoIdleState();
						if (mIsSupposedToBePlaying) {
							mIsSupposedToBePlaying = false;
							notifyChange(BROADCAST_PLAYSTATE_CHANGED);
						}
						return;
					}
				}
				int skip = mShuffler.shuffle(numUnplayed);
				int cnt = -1;
				while (true) {
					while (tracks[++cnt] < 0)
						;
					skip--;
					if (skip < 0) {
						break;
					}
				}
				mPlayPos = cnt;
			} else {
				if (mPlayPos >= mPlayListLen - 1) {
					// we're at the end of the list
					if (mRepeatMode == REPEAT_NONE && !force) {
						// all done
						gotoIdleState();
						mIsSupposedToBePlaying = false;
						notifyChange(BROADCAST_PLAYSTATE_CHANGED);
						return;
					} else if (mRepeatMode == REPEAT_ALL || force) {
						mPlayPos = 0;
					}
				} else {
					mPlayPos++;
				}
			}
			saveBookmarkIfNeeded();
			stop(false);
			openCurrent();
			play();
			notifyChange(BROADCAST_META_CHANGED);
		}
	}

	public void cycleRepeat() {

		if (mRepeatMode == REPEAT_NONE) {
			setRepeatMode(REPEAT_ALL);
		} else if (mRepeatMode == REPEAT_ALL) {
			setRepeatMode(REPEAT_CURRENT);
			if (mShuffleMode != SHUFFLE_NONE) {
				setShuffleMode(SHUFFLE_NONE);
			}
		} else {
			setRepeatMode(REPEAT_NONE);
		}
	}

	public void toggleShuffle() {

		if (mShuffleMode == SHUFFLE_NONE) {
			setShuffleMode(SHUFFLE_NORMAL);
			if (mRepeatMode == REPEAT_CURRENT) {
				setRepeatMode(REPEAT_ALL);
			}
		} else if (mShuffleMode == SHUFFLE_NORMAL) {
			setShuffleMode(SHUFFLE_NONE);
		} else {
			setShuffleMode(SHUFFLE_NONE);
			Log.e("MediaPlaybackService", "Invalid shuffle mode: " + mShuffleMode);
		}
	}

	private void gotoIdleState() {

		mDelayedStopHandler.removeCallbacksAndMessages(null);
		Message msg = mDelayedStopHandler.obtainMessage();
		mDelayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY);
		mNotification.cancel(PLAYBACKSERVICE_STATUS);
	}

	private void saveBookmarkIfNeeded() {

		try {
			if (isPodcast()) {
				long pos = position();
				long bookmark = getBookmark();
				long duration = duration();
				if ((pos < bookmark && (pos + 10000) > bookmark)
						|| (pos > bookmark && (pos - 10000) < bookmark)) {
					// The existing bookmark is close to the current
					// position, so don't update it.
					return;
				}
				if (pos < 15000 || (pos + 10000) > duration) {
					// if we're near the start or end, clear the bookmark
					pos = 0;
				}

				// write 'pos' to the bookmark field
				ContentValues values = new ContentValues();
				values.put(MediaStore.Audio.Media.BOOKMARK, pos);
				Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
						mCursor.getLong(IDCOLIDX));
				getContentResolver().update(uri, values, null, null);
			}
		} catch (SQLiteException ex) {
		}
	}

	// A simple variation of Random that makes sure that the
	// value it returns is not equal to the value it returned
	// previously, unless the interval is 1.
	private class Shuffler {

		private int mPrevious;
		private Random mRandom = new Random();

		public int shuffle(int interval) {

			int ret;
			long ret_id;
			do {
				ret = mRandom.nextInt(interval);
				ret_id = mPlayList[ret];
			} while ((ret == mPrevious && interval > 1)
					|| (!isFavorite(ret_id) && mHistory.contains(ret_id)));

			mPrevious = ret;
			return ret;
		}
	};

	/**
	 * Removes the range of tracks specified from the play list. If a file
	 * within the range is the file currently being played, playback will move
	 * to the next file after the range.
	 * 
	 * @param first
	 *            The first file to be removed
	 * @param last
	 *            The last file to be removed
	 * @return the number of tracks deleted
	 */
	public int removeTracks(int first, int last) {

		int numremoved = removeTracksInternal(first, last);
		if (numremoved > 0) {
			notifyChange(BROADCAST_QUEUE_CHANGED);
		}
		return numremoved;
	}

	private int removeTracksInternal(int first, int last) {

		synchronized (this) {
			if (last < first) return 0;
			if (first < 0) first = 0;
			if (last >= mPlayListLen) last = mPlayListLen - 1;

			boolean gotonext = false;
			if (first <= mPlayPos && mPlayPos <= last) {
				mPlayPos = first;
				gotonext = true;
			} else if (mPlayPos > last) {
				mPlayPos -= (last - first + 1);
			}
			int num = mPlayListLen - last - 1;
			for (int i = 0; i < num; i++) {
				mPlayList[first + i] = mPlayList[last + 1 + i];
			}
			mPlayListLen -= last - first + 1;

			if (gotonext) {
				if (mPlayListLen == 0) {
					stop(true);
					mPlayPos = -1;
					if (mCursor != null) {
						mCursor.close();
						mCursor = null;
					}
				} else {
					if (mPlayPos >= mPlayListLen) {
						mPlayPos = 0;
					}
					boolean wasPlaying = isPlaying();
					stop(false);
					openCurrent();
					if (wasPlaying) {
						play();
					}
				}
				notifyChange(BROADCAST_META_CHANGED);
			}
			return last - first + 1;
		}
	}

	/**
	 * Removes all instances of the track with the given id from the playlist.
	 * 
	 * @param id
	 *            The id to be removed
	 * @return how many instances of the track were removed
	 */
	public int removeTrack(long id) {

		int numremoved = 0;
		synchronized (this) {
			for (int i = 0; i < mPlayListLen; i++) {
				if (mPlayList[i] == id) {
					numremoved += removeTracksInternal(i, i);
					i--;
				}
			}
		}
		if (numremoved > 0) {
			notifyChange(BROADCAST_QUEUE_CHANGED);
		}
		return numremoved;
	}

	public void setShuffleMode(int shufflemode) {

		synchronized (this) {
			if (mShuffleMode == shufflemode && mPlayListLen > 0) {
				return;
			}
			if (mRepeatMode == REPEAT_CURRENT) mRepeatMode = REPEAT_NONE;
			mShuffleMode = shufflemode;
			notifyChange(BROADCAST_SHUFFLEMODE_CHANGED);
			saveQueue(false);
		}
	}

	public int getShuffleMode() {

		return mShuffleMode;
	}

	public void setRepeatMode(int repeatmode) {

		synchronized (this) {
			mRepeatMode = repeatmode;
			notifyChange(BROADCAST_REPEATMODE_CHANGED);
			saveQueue(false);
		}
	}

	public int getRepeatMode() {

		return mRepeatMode;
	}

	public int getMediaMountedCount() {

		return mMediaMountedCount;
	}

	/**
	 * Returns the path of the currently playing file, or null if no file is
	 * currently playing.
	 */
	public String getPath() {

		return mFileToPlay;
	}

	/**
	 * Returns the absolute path of the currently playing file, or null if no
	 * file is currently playing.
	 */
	public String getMediaPath() {

		synchronized (this) {
			if (mCursor == null) {
				return null;
			}
			return mCursor.getString(mCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
		}
	}

	/**
	 * Returns the rowid of the currently playing file, or -1 if no file is
	 * currently playing.
	 */
	public long getAudioId() {

		synchronized (this) {
			if (mPlayPos >= 0 && mPlayer.isInitialized()) {
				return mPlayList[mPlayPos];
			}
		}
		return -1;
	}

	public Bitmap getAlbumArt() {

		synchronized (this) {
			return MusicUtils.getArtwork(this, getAudioId(), getAlbumId());
		}
	}

	public Uri getArtworkUri() {

		synchronized (this) {
			return MusicUtils.getArtworkUri(this, getAudioId(), getAlbumId());
		}
	}

	public String[] getLyrics() {

		return mLyrics;
	}

	public int getLyricsStatus() {

		return mLyricsStatus;
	}

	public int getCurrentLyricsId() {

		return mLyricsId;
	}

	public long getPositionByLyricsId(int id) {

		synchronized (this) {
			if (mLyricsParser != null && id < mLyricsParser.getAllTimestamp().size()) {
				return mLyricsParser.getTimestamp(id);
			}
		}
		return position();
	}

	public void reloadEqualizer() {

		if (mEqualizerSupported && mEqualizer != null) {
			short bands = mEqualizer.getNumberOfBands();

			final short minEQLevel = mEqualizer.getBandLevelRange()[0];
			final short maxEQLevel = mEqualizer.getBandLevelRange()[1];

			for (short i = 0; i < bands; i++) {
				final short band = i;
				mEqualizer.setBandLevel(band,
						mPrefs.getEqualizerSetting(band, (short) ((maxEQLevel + minEQLevel) / 2)));
			}
		}
	}

	/**
	 * Returns the position in the queue
	 * 
	 * @return the position in the queue
	 */
	public int getQueuePosition() {

		synchronized (this) {
			return mPlayPos;
		}
	}

	/**
	 * Starts playing the track at the given position in the queue.
	 * 
	 * @param pos
	 *            The position in the queue of the track that will be played.
	 */
	public void setQueuePosition(int pos) {

		synchronized (this) {
			stop(false);
			mPlayPos = pos;
			openCurrent();
			play();
			notifyChange(BROADCAST_META_CHANGED);
		}
	}

	/**
	 * Starts playing the track at the given id in the queue.
	 * 
	 * @param id
	 *            The id in the queue of the track that will be played.
	 */
	public void setQueueId(long id) {
		int pos = -1;

		for (int i = 0; i < mPlayList.length; i++) {
			if (id == mPlayList[i]) pos = i;
		}
		if (pos < 0) return;

		setQueuePosition(pos);
	}

	public String getArtistName() {

		synchronized (this) {
			if (mCursor == null) {
				return null;
			}
			return mCursor.getString(mCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
		}
	}

	public long getArtistId() {

		synchronized (this) {
			if (mCursor == null) {
				return -1;
			}
			return mCursor.getLong(mCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID));
		}
	}

	public String getAlbumName() {

		synchronized (this) {
			if (mCursor == null) {
				return null;
			}
			return mCursor.getString(mCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));
		}
	}

	public long getAlbumId() {

		synchronized (this) {
			if (mCursor == null) {
				return -1;
			}
			return mCursor.getLong(mCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID));
		}
	}

	public String getTrackName() {

		synchronized (this) {
			if (mCursor == null) {
				return null;
			}
			return mCursor.getString(mCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
		}
	}

	private boolean isPodcast() {

		synchronized (this) {
			if (mCursor == null) {
				return false;
			}
			return (mCursor.getInt(PODCASTCOLIDX) > 0);
		}
	}

	private long getBookmark() {

		synchronized (this) {
			if (mCursor == null) {
				return 0;
			}
			return mCursor.getLong(BOOKMARKCOLIDX);
		}
	}

	/**
	 * Returns the duration of the file in milliseconds. Currently this method
	 * returns -1 for the duration of MIDI files.
	 */
	public long duration() {

		if (mPlayer.isInitialized()) {
			return mPlayer.duration();
		}
		return -1;
	}

	/**
	 * Returns the current playback position in milliseconds
	 */
	public long position() {

		if (mPlayer.isInitialized()) {
			return mPlayer.position();
		}
		return -1;
	}

	/**
	 * Seeks to the position specified.
	 * 
	 * @param pos
	 *            The position to seek to, in milliseconds
	 */
	public long seek(long pos) {

		if (mPlayer.isInitialized()) {
			if (pos < 0) pos = 0;
			if (pos > mPlayer.duration()) pos = mPlayer.duration();
			long result = mPlayer.seek(pos);
			mLyricsHandler.obtainMessage(POSITION_CHANGED, (int) result, 0).sendToTarget();
			return result;
		}
		return 0;
	}

	/**
	 * Returns the audio session ID.
	 */
	public int getAudioSessionId() {

		synchronized (this) {
			return mPlayer.getAudioSessionId();
		}
	}

	/**
	 * Provides a unified interface for dealing with midi files and other media
	 * files.
	 */
	private class MultiPlayer {

		private MediaPlayer mMediaPlayer = new MediaPlayer();
		private Handler mHandler;
		private boolean mIsInitialized = false;

		public MultiPlayer() {

			mMediaPlayer.setWakeMode(MusicPlaybackService.this, PowerManager.PARTIAL_WAKE_LOCK);
		}

		public void setDataSource(String path) {

			try {
				mMediaPlayer.reset();
				mMediaPlayer.setOnPreparedListener(null);
				if (path.startsWith("content://")) {
					mMediaPlayer.setDataSource(MusicPlaybackService.this, Uri.parse(path));
				} else {
					mMediaPlayer.setDataSource(path);
				}
				mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
				mMediaPlayer.prepare();
			} catch (IOException ex) {
				mIsInitialized = false;
				return;
			} catch (IllegalArgumentException ex) {
				mIsInitialized = false;
				return;
			}
			mMediaPlayer.setOnCompletionListener(listener);
			mMediaPlayer.setOnErrorListener(errorListener);
			mIsInitialized = true;
		}

		public boolean isInitialized() {

			return mIsInitialized;
		}

		public void start() {

			MusicUtils.debugLog(new Exception("MultiPlayer.start called"));
			mMediaPlayer.start();
		}

		public void stop() {

			mMediaPlayer.reset();
			mIsInitialized = false;
		}

		/**
		 * You CANNOT use this player anymore after calling release()
		 */
		public void release() {

			stop();
			mMediaPlayer.release();
		}

		public void pause() {

			mMediaPlayer.pause();
		}

		public boolean isPlaying() {

			return mMediaPlayer.isPlaying();
		}

		public void setHandler(Handler handler) {

			mHandler = handler;
		}

		private MediaPlayer.OnCompletionListener listener = new MediaPlayer.OnCompletionListener() {

			@Override
			public void onCompletion(MediaPlayer mp) {

				// Acquire a temporary wakelock, since when we return from
				// this callback the MediaPlayer will release its wakelock
				// and allow the device to go to sleep.
				// This temporary wakelock is released when the RELEASE_WAKELOCK
				// message is processed, but just in case, put a timeout on it.
				mWakeLock.acquire(30000);
				mHandler.sendEmptyMessage(TRACK_ENDED);
				mHandler.sendEmptyMessage(RELEASE_WAKELOCK);
			}
		};

		private MediaPlayer.OnErrorListener errorListener = new MediaPlayer.OnErrorListener() {

			@Override
			public boolean onError(MediaPlayer mp, int what, int extra) {

				switch (what) {
					case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
						mIsInitialized = false;
						mMediaPlayer.release();
						// Creating a new MediaPlayer and settings its wakemode
						// does
						// not
						// require the media service, so it's OK to do this now,
						// while the
						// service is still being restarted
						mMediaPlayer = new MediaPlayer();
						mMediaPlayer.setWakeMode(MusicPlaybackService.this,
								PowerManager.PARTIAL_WAKE_LOCK);
						mHandler.sendMessageDelayed(mHandler.obtainMessage(SERVER_DIED), 2000);
						return true;
					default:
						Log.d("MultiPlayer", "Error: " + what + "," + extra);
						break;
				}
				return false;
			}
		};

		public long duration() {

			return mMediaPlayer.getDuration();
		}

		public long position() {

			return mMediaPlayer.getCurrentPosition();
		}

		public long seek(long whereto) {

			mMediaPlayer.seekTo((int) whereto);
			return whereto;
		}

		public void setVolume(float vol) {

			mMediaPlayer.setVolume(vol, vol);
			mCurrentVolume = vol;
		}

		public int getAudioSessionId() {

			try {
				return (Integer) mMediaPlayer.getClass()
						.getMethod("getAudioSessionId", new Class[] {})
						.invoke(mMediaPlayer, new Object[] {});
			} catch (Exception e) {
				return 0;
			}
		}

	}

	/*
	 * By making this a static class with a WeakReference to the Service, we
	 * ensure that the Service can be GCd even when the system process still has
	 * a remote reference to the stub.
	 */
	static class ServiceStub extends IMusicPlaybackService.Stub {

		WeakReference<MusicPlaybackService> mService;

		ServiceStub(MusicPlaybackService service) {

			mService = new WeakReference<MusicPlaybackService>(service);
		}

		@Override
		public void reloadLyrics() {

			mService.get().reloadLyrics();
		}

		@Override
		public void refreshLyrics() {

			mService.get().notifyLyricsChange(BROADCAST_LYRICS_REFRESHED);
		}

		@Override
		public void openFile(String path) {

			mService.get().open(path);
		}

		@Override
		public void open(long[] list, int position) {

			mService.get().open(list, position);
		}

		@Override
		public int getQueuePosition() {

			return mService.get().getQueuePosition();
		}

		@Override
		public void setQueuePosition(int index) {

			mService.get().setQueuePosition(index);
		}

		@Override
		public void setQueueId(long id) {
			mService.get().setQueueId(id);
		}

		@Override
		public boolean isPlaying() {

			return mService.get().isPlaying();
		}

		@Override
		public void stop() {

			mService.get().stop();
		}

		@Override
		public void pause() {

			mService.get().pause();
		}

		@Override
		public void play() {

			mService.get().play();
		}

		@Override
		public void prev() {

			mService.get().prev();
		}

		@Override
		public void next() {

			mService.get().next(true);
		}

		@Override
		public void cycleRepeat() {

			mService.get().cycleRepeat();
		}

		@Override
		public void toggleShuffle() {

			mService.get().toggleShuffle();
		}

		@Override
		public String getTrackName() {

			return mService.get().getTrackName();
		}

		@Override
		public String getAlbumName() {

			return mService.get().getAlbumName();
		}

		@Override
		public long getAlbumId() {

			return mService.get().getAlbumId();
		}

		@Override
		public String getArtistName() {

			return mService.get().getArtistName();
		}

		@Override
		public long getArtistId() {

			return mService.get().getArtistId();
		}

		@Override
		public void enqueue(long[] list, int action) {

			mService.get().enqueue(list, action);
		}

		@Override
		public long[] getQueue() {

			return mService.get().getQueue();
		}

		@Override
		public void moveQueueItem(int from, int to) {

			mService.get().moveQueueItem(from, to);
		}

		@Override
		public String getPath() {

			return mService.get().getPath();
		}

		@Override
		public String getMediaPath() {

			return mService.get().getMediaPath();
		}

		@Override
		public Bitmap getAlbumArt() {

			return mService.get().getAlbumArt();
		}

		@Override
		public Uri getArtworkUri() {

			return mService.get().getArtworkUri();
		}

		@Override
		public String[] getLyrics() {

			return mService.get().getLyrics();
		}

		@Override
		public int getLyricsStatus() {

			return mService.get().getLyricsStatus();
		}

		@Override
		public int getCurrentLyricsId() {

			return mService.get().getCurrentLyricsId();
		}

		@Override
		public long getPositionByLyricsId(int id) {

			return mService.get().getPositionByLyricsId(id);
		}

		@Override
		public void reloadEqualizer() {

			mService.get().reloadEqualizer();
		}

		@Override
		public long getAudioId() {

			return mService.get().getAudioId();
		}

		@Override
		public long position() {

			return mService.get().position();
		}

		@Override
		public long duration() {

			return mService.get().duration();
		}

		@Override
		public long seek(long pos) {

			return mService.get().seek(pos);
		}

		@Override
		public void setShuffleMode(int shufflemode) {

			mService.get().setShuffleMode(shufflemode);
		}

		@Override
		public int getShuffleMode() {

			return mService.get().getShuffleMode();
		}

		@Override
		public int removeTracks(int first, int last) {

			return mService.get().removeTracks(first, last);
		}

		@Override
		public int removeTrack(long id) {

			return mService.get().removeTrack(id);
		}

		@Override
		public void setRepeatMode(int repeatmode) {

			mService.get().setRepeatMode(repeatmode);
		}

		@Override
		public int getRepeatMode() {

			return mService.get().getRepeatMode();
		}

		@Override
		public int getMediaMountedCount() {

			return mService.get().getMediaMountedCount();
		}

		@Override
		public int getAudioSessionId() {

			return mService.get().getAudioSessionId();
		}

		@Override
		public void startSleepTimer(long milliseconds, boolean gentle) {

			mService.get().startSleepTimer(milliseconds, gentle);
		}

		@Override
		public void stopSleepTimer() {

			mService.get().stopSleepTimer();
		}

		@Override
		public long getSleepTimerRemained() {

			return mService.get().getSleepTimerRemained();
		}

		@Override
		public void reloadSettings() {

			mService.get().reloadSettings();
		}

		@Override
		public void toggleFavorite() {
			mService.get().toggleFavorite();
		}

		@Override
		public void addToFavorites(long id) {
			mService.get().addToFavorites(id);
		}

		@Override
		public void removeFromFavorites(long id) {
			mService.get().removeFromFavorites(id);
		}

		@Override
		public boolean isFavorite(long id) {
			return mService.get().isFavorite(id);
		}
	}

	@Override
	protected void dump(FileDescriptor fd, PrintWriter writer, String[] args) {

		writer.println("" + mPlayListLen + " items in queue, currently at index " + mPlayPos);
		writer.println("Currently loaded:");
		writer.println(getArtistName());
		writer.println(getAlbumName());
		writer.println(getTrackName());
		writer.println(getPath());
		writer.println("playing: " + mIsSupposedToBePlaying);
		writer.println("actual: " + mPlayer.isPlaying());
		writer.println("shuffle mode: " + mShuffleMode);
		MusicUtils.debugDump(writer);
	}

	private final IBinder mBinder = new ServiceStub(this);
}
