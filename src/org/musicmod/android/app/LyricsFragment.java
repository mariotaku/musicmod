package org.musicmod.android.app;

import org.musicmod.android.Constants;
import org.musicmod.android.IMusicPlaybackService;
import org.musicmod.android.R;
import org.musicmod.android.util.MusicUtils;
import org.musicmod.android.util.ServiceToken;
import org.musicmod.android.widget.TextScrollView;
import org.musicmod.android.widget.TextScrollView.OnLineSelectedListener;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

public class LyricsFragment extends Fragment implements Constants, OnLineSelectedListener,
		OnLongClickListener, ServiceConnection {

	private IMusicPlaybackService mService = null;
	private ServiceToken mToken;

	// for lyrics displaying
	private TextScrollView mLyricsScrollView;
	private TextView mLyricsInfoMessage;
	private boolean mIntentDeRegistered = false;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		View fragmentView = getView();
		mLyricsScrollView = (TextScrollView) fragmentView.findViewById(R.id.lyrics_scroll);
		mLyricsScrollView.setContentGravity(Gravity.CENTER_HORIZONTAL);

		mLyricsInfoMessage = (TextView) fragmentView.findViewById(R.id.message);
		mLyricsInfoMessage.setOnLongClickListener(this);

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.lyrics_view, container, false);
	}

	@Override
	public void onStart() {
		super.onStart();
		mToken = MusicUtils.bindToService(getActivity(), this);
		mLyricsScrollView.setLineSelectedListener(this);

		try {
			float mWindowAnimation = Settings.System.getFloat(getActivity().getContentResolver(),
					Settings.System.WINDOW_ANIMATION_SCALE);
			mLyricsScrollView.setSmoothScrollingEnabled(mWindowAnimation > 0.0);

		} catch (SettingNotFoundException e) {
			e.printStackTrace();
		}

		IntentFilter lyricsstatusfilter = new IntentFilter();
		lyricsstatusfilter.addAction(BROADCAST_NEW_LYRICS_LOADED);
		lyricsstatusfilter.addAction(BROADCAST_LYRICS_REFRESHED);
		getActivity().registerReceiver(mStatusListener, lyricsstatusfilter);

		IntentFilter screenstatusfilter = new IntentFilter();
		screenstatusfilter.addAction(Intent.ACTION_SCREEN_ON);
		screenstatusfilter.addAction(Intent.ACTION_SCREEN_OFF);
		getActivity().registerReceiver(mScreenTimeoutListener, screenstatusfilter);
	}

	@Override
	public void onStop() {

		if (!mIntentDeRegistered) {
			getActivity().unregisterReceiver(mStatusListener);
		}
		getActivity().unregisterReceiver(mScreenTimeoutListener);

		MusicUtils.unbindFromService(mToken);
		mService = null;
		super.onStop();
	}

	@Override
	public void onServiceConnected(ComponentName classname, IBinder obj) {
		mService = IMusicPlaybackService.Stub.asInterface(obj);
		try {
			if (mService.getAudioId() >= 0 || mService.isPlaying() || mService.getPath() != null) {
				loadLyricsToView();
				scrollLyrics(true);
			} else {
				getActivity().finish();
			}

		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onServiceDisconnected(ComponentName paramComponentName) {
		mService = null;
		getActivity().finish();
	}

	@Override
	public void onLineSelected(int id) {

		try {
			mService.seek(mService.getPositionByLyricsId(id));
		} catch (RemoteException e) {
			e.printStackTrace();
		}

	}

	@Override
	public boolean onLongClick(View v) {

		searchLyrics();
		return true;
	}

	private BroadcastReceiver mStatusListener = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (BROADCAST_NEW_LYRICS_LOADED.equals(action)) {
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
					f.addAction(BROADCAST_NEW_LYRICS_LOADED);
					f.addAction(BROADCAST_LYRICS_REFRESHED);
					getActivity().registerReceiver(mStatusListener, new IntentFilter(f));
					mIntentDeRegistered = false;
				}
				loadLyricsToView();
				scrollLyrics(true);
			} else if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
				if (!mIntentDeRegistered) {
					getActivity().unregisterReceiver(mStatusListener);
					mIntentDeRegistered = true;
				}
			}
		}
	};

	// TODO lyrics load animation
	private void loadLyricsToView() {

		if (mLyricsScrollView == null || mService == null) return;

		try {
			mLyricsScrollView.setTextContent(mService.getLyrics());

			if (mService.getLyricsStatus() == LYRICS_STATUS_OK) {
			} else {
			}

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

}
