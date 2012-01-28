package org.musicmod.android.dialog;

import org.musicmod.android.Constants;
import org.musicmod.android.R;
import org.musicmod.android.util.MusicUtils;
import org.musicmod.android.util.ServiceToken;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.widget.LinearLayout;
import android.widget.Toast;

public class PlayShortcut extends FragmentActivity implements Constants {

	private long mPlaylistId;
	private ServiceToken mToken = null;

	@Override
	public void onCreate(Bundle icicle) {

		super.onCreate(icicle);

		setContentView(new LinearLayout(this));
		mPlaylistId = getIntent().getLongExtra(MAP_KEY_ID, PLAYLIST_UNKNOWN);
		mToken = MusicUtils.bindToService(this, osc);
	}

	@Override
	public void onStop() {

		if (mToken != null) {
			MusicUtils.unbindFromService(mToken);
		}
		finish();
		super.onStop();
	}

	private ServiceConnection osc = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName classname, IBinder obj) {

			if (getIntent().getAction() != null
					&& getIntent().getAction().equals(INTENT_PLAY_SHORTCUT)
					&& mPlaylistId != PLAYLIST_UNKNOWN) {
				switch ((int) mPlaylistId) {
					case (int) PLAYLIST_ALL_SONGS:
						MusicUtils.playAll(getApplicationContext());
						break;
					case (int) PLAYLIST_RECENTLY_ADDED:
						MusicUtils.playRecentlyAdded(getApplicationContext());
						break;
					default:
						if (mPlaylistId >= 0)
							MusicUtils.playPlaylist(PlayShortcut.this, mPlaylistId);
						break;
				}

			} else {
				Toast.makeText(PlayShortcut.this, R.string.error_bad_parameters, Toast.LENGTH_SHORT)
						.show();
			}
			finish();
		}

		@Override
		public void onServiceDisconnected(ComponentName classname) {

			finish();
		}
	};

}