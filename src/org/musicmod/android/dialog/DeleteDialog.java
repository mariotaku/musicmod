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

package org.musicmod.android.dialog;

import org.musicmod.android.Constants;
import org.musicmod.android.R;
import org.musicmod.android.util.MusicUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.DialogInterface.OnShowListener;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

public class DeleteDialog extends Activity implements Constants, OnMultiChoiceClickListener,
		DialogInterface.OnClickListener, OnCancelListener, OnShowListener, View.OnClickListener {

	private AlertDialog mDeleteMultiSelect, mDeleteConfirm;

	private boolean delete_lyrics, delete_music, restore_confirm;
	private final int DELETE_LYRICS_ID = 0, DELETE_MUSIC_ID = 1;
	private String KEY_DELETE_LYRICS = "delete_lyrics";
	private String KEY_DELETE_MUSIC = "delete_music";
	private String KEY_RESTORE_CONFIRM = "restore_confirm";

	private String action;
	String type, content = "";
	long[] items;

	@Override
	public void onCreate(Bundle icicle) {

		super.onCreate(icicle);

		setContentView(new LinearLayout(this));

		action = getIntent().getAction();

		if (INTENT_DELETE_ITEMS.equals(action)) {
			restore_confirm = icicle != null ? icicle.getBoolean(KEY_RESTORE_CONFIRM) : getIntent()
					.getBooleanExtra(KEY_RESTORE_CONFIRM, false);
			delete_lyrics = icicle != null ? icicle.getBoolean(KEY_DELETE_LYRICS) : getIntent()
					.getBooleanExtra(KEY_DELETE_LYRICS, false);
			delete_music = icicle != null ? icicle.getBoolean(KEY_DELETE_MUSIC) : getIntent()
					.getBooleanExtra(KEY_DELETE_MUSIC, false);
			type = icicle != null ? icicle.getString(INTENT_KEY_TYPE) : getIntent().getStringExtra(
					INTENT_KEY_TYPE);
			content = icicle != null ? icicle.getString(INTENT_KEY_CONTENT) : getIntent()
					.getStringExtra(INTENT_KEY_CONTENT);
			items = icicle != null ? icicle.getLongArray(INTENT_KEY_ITEMS) : getIntent()
					.getLongArrayExtra(INTENT_KEY_ITEMS);
		} else {
			Toast.makeText(this, R.string.error_bad_parameters, Toast.LENGTH_SHORT).show();
			finish();
		}
	}

	@Override
	public void onClick(DialogInterface dialog, int which, boolean isChecked) {

		switch (which) {
			case DELETE_LYRICS_ID:
				delete_lyrics = isChecked;
				break;
			case DELETE_MUSIC_ID:
				delete_music = isChecked;
				break;
		}
		if (mDeleteMultiSelect != null) {
			mDeleteMultiSelect.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(
					delete_lyrics || delete_music);
		}
	}

	@Override
	protected void onResume() {

		super.onResume();
		mDeleteMultiSelect = new AlertDialog.Builder(this)
				.setMultiChoiceItems(
						new CharSequence[] { getString(R.string.delete_lyrics),
								getString(R.string.delete_music) },
						new boolean[] { delete_lyrics, delete_music }, this)
				.setIcon(android.R.drawable.ic_dialog_alert).setTitle(R.string.delete)
				.setPositiveButton(android.R.string.ok, this)
				.setNegativeButton(android.R.string.cancel, this).setOnCancelListener(this)
				.create();
		mDeleteMultiSelect.setOnShowListener(this);
		mDeleteMultiSelect.show();
		if (mDeleteConfirm != null && restore_confirm) {
			mDeleteConfirm.show();
		}
	}

	@Override
	public void onPause() {

		if (mDeleteMultiSelect != null && mDeleteMultiSelect.isShowing()) {
			mDeleteMultiSelect.dismiss();
		}
		if (mDeleteConfirm != null && mDeleteConfirm.isShowing()) {
			mDeleteConfirm.dismiss();
		}
		super.onPause();
	}

	@Override
	public void onShow(DialogInterface dialog) {

		Button mButton = mDeleteMultiSelect.getButton(AlertDialog.BUTTON_POSITIVE);
		mButton.setEnabled(delete_lyrics || delete_music);
		mButton.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {

		String desc = "";
		if (TYPE_ARTIST_ALBUM.equals(type)) {
			if (delete_lyrics) {
				desc += "\n" + getString(R.string.delete_artist_lyrics, content);
			}
			if (delete_music) {
				desc += "\n" + getString(R.string.delete_artist_tracks, content);
			}
		} else if (TYPE_ALBUM.equals(type)) {
			if (delete_lyrics) {
				desc += "\n" + getString(R.string.delete_album_lyrics, content);
			}
			if (delete_music) {
				desc += "\n" + getString(R.string.delete_album_tracks, content);
			}
		} else if (TYPE_TRACK.equals(type)) {
			if (delete_lyrics) {
				desc += "\n" + getString(R.string.delete_song_lyrics, content);
			}
			if (delete_music) {
				desc += "\n" + getString(R.string.delete_song_track, content);
			}
		}

		confirmDelete(desc, items);
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {

		if (dialog == mDeleteMultiSelect) {
			switch (which) {
				case DialogInterface.BUTTON_NEGATIVE:
					finish();
					break;
			}

		}
		if (dialog == mDeleteConfirm) {
			switch (which) {
				case DialogInterface.BUTTON_POSITIVE:
					if (delete_lyrics) {
						MusicUtils.deleteLyrics(DeleteDialog.this, items);
					}
					if (delete_music) {
						MusicUtils.deleteTracks(DeleteDialog.this, items);
					}
					setResult(RESULT_DELETE_MUSIC);
					finish();
					break;
				case DialogInterface.BUTTON_NEGATIVE:
					restore_confirm = false;
					break;
			}
		}
	}

	@Override
	public void onCancel(DialogInterface dialog) {

		if (dialog == mDeleteMultiSelect) {
			finish();
		}
		if (dialog == mDeleteConfirm) {
			restore_confirm = false;
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outcicle) {

		outcicle.putBoolean(KEY_RESTORE_CONFIRM, restore_confirm);
		outcicle.putBoolean(KEY_DELETE_LYRICS, delete_lyrics);
		outcicle.putBoolean(KEY_DELETE_MUSIC, delete_music);
		outcicle.putString(INTENT_KEY_TYPE, type);
		outcicle.putString(INTENT_KEY_CONTENT, content);
		outcicle.putLongArray(INTENT_KEY_LIST, items);
	}

	public void confirmDelete(String desc, final long[] list) {

		mDeleteConfirm = new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert)
				.setTitle(R.string.delete_lyrics)
				.setMessage(getString(R.string.delete_confirm, desc))
				.setPositiveButton(android.R.string.ok, this)
				.setNegativeButton(android.R.string.cancel, this).setOnCancelListener(this).show();
		restore_confirm = true;
	}

}