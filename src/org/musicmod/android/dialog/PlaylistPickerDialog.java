package org.musicmod.android.dialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.musicmod.android.Constants;
import org.musicmod.android.R;
import org.musicmod.android.util.MusicUtils;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.widget.LinearLayout;
import android.widget.Toast;

public class PlaylistPickerDialog extends FragmentActivity implements DialogInterface.OnClickListener,
		DialogInterface.OnCancelListener, Constants {

	private AlertDialog mPlayListPickerDialog;

	List<Map<String, String>> mAllPlayLists = new ArrayList<Map<String, String>>();
	List<String> mPlayListNames = new ArrayList<String>();
	long[] mList = new long[] {};

	@Override
	public void onCreate(Bundle icicle) {

		super.onCreate(icicle);

		setContentView(new LinearLayout(this));

		if (getIntent().getAction() != null) {

			if (INTENT_ADD_TO_PLAYLIST.equals(getIntent().getAction())
					&& getIntent().getLongArrayExtra(INTENT_KEY_LIST) != null) {

				MusicUtils.makePlaylistList(this, false, mAllPlayLists);
				mList = getIntent().getLongArrayExtra(INTENT_KEY_LIST);
				for (int i = 0; i < mAllPlayLists.size(); i++) {
					mPlayListNames.add(mAllPlayLists.get(i).get(MAP_KEY_NAME));
				}
				mPlayListPickerDialog = new AlertDialog.Builder(this)
						.setTitle(R.string.add_to_playlist)
						.setItems(mPlayListNames.toArray(new CharSequence[mPlayListNames.size()]),
								this).setOnCancelListener(this).show();
			} else if (getIntent().getAction().equals(Intent.ACTION_CREATE_SHORTCUT)) {
				MusicUtils.makePlaylistList(this, true, mAllPlayLists);
				for (int i = 0; i < mAllPlayLists.size(); i++) {
					mPlayListNames.add(mAllPlayLists.get(i).get(MAP_KEY_NAME));
				}
				mPlayListPickerDialog = new AlertDialog.Builder(this)
						.setTitle(R.string.playlists)
						.setItems(mPlayListNames.toArray(new CharSequence[mPlayListNames.size()]),
								this).setOnCancelListener(this).show();
			}
		} else {
			Toast.makeText(this, R.string.error_bad_parameters, Toast.LENGTH_SHORT).show();
			finish();
		}
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {

		long listId = Long.valueOf(mAllPlayLists.get(which).get(MAP_KEY_ID));
		String listName = mAllPlayLists.get(which).get(MAP_KEY_NAME);
		Intent intent;
		boolean mCreateShortcut = getIntent().getAction().equals(Intent.ACTION_CREATE_SHORTCUT);

		if (mCreateShortcut) {
			final Intent shortcut = new Intent();
			shortcut.setAction(INTENT_PLAY_SHORTCUT);
			shortcut.putExtra(MAP_KEY_ID, listId);

			intent = new Intent();
			intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcut);
			intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, listName);
			intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource
					.fromContext(this, R.drawable.ic_launcher_shortcut_playlist));
			setResult(RESULT_OK, intent);
		} else {
			if (listId >= 0) {
				MusicUtils.addToPlaylist(this, mList, listId);
			} else if (listId == PLAYLIST_QUEUE) {
				MusicUtils.addToCurrentPlaylist(this, mList);
			} else if (listId == PLAYLIST_NEW) {
				intent = new Intent(INTENT_CREATE_PLAYLIST);
				intent.putExtra(INTENT_KEY_LIST, mList);
				startActivity(intent);
			}
		}
		finish();
	}

	@Override
	public void onCancel(DialogInterface dialog) {

		finish();
	}

	@Override
	protected void onResume() {

		super.onResume();
		if (mPlayListPickerDialog != null && !mPlayListPickerDialog.isShowing()) {
			mPlayListPickerDialog.show();
		}
	}

	@Override
	public void onPause() {

		if (mPlayListPickerDialog != null && mPlayListPickerDialog.isShowing()) {
			mPlayListPickerDialog.dismiss();
		}
		super.onPause();
	}

}
