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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;

import org.musicmod.android.Constants;
import org.musicmod.android.R;
import org.musicmod.android.util.ImageDownloader;
import org.musicmod.android.util.LyricsDownloader;
import org.musicmod.android.util.MusicUtils;
import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

public class SearchDialog extends Activity implements Constants, TextWatcher, OnCancelListener {

	private ProgressDialog mProgress = null;

	private LinearLayout mLinearLayout;
	private AlertDialog mSearchDialog, mLyricsChooser, mLyricsConfirm, mAlbumArtConfirm;
	private ProgressDialog mSearchProgress, mDownloadProgress;

	private boolean restore_lyrics_chooser, restore_lyrics_confirm,
			restore_albumart_confirm = false;

	LyricsDownloader mDownloader;

	LyricsSearchTask mLyricsSearchTask;
	LyricsDownloadTask mLyricsDownloadTask;

	AlbumArtSearchTask mAlbumArtSearchTask;
	AlbumArtDownloadTask mAlbumArtDownloadTask;

	private String action;
	private LinearLayout mContainer;
	private TextView mKeywordSummary1, mKeywordSummary2;
	private EditText mSearchKeyword1, mSearchKeyword2;
	private String mKeyword1, mKeyword2 = "";
	private String mPath = null;

	@Override
	public void onCreate(Bundle icicle) {

		super.onCreate(icicle);

		mLinearLayout = new LinearLayout(this);
		mLinearLayout.setOrientation(LinearLayout.VERTICAL);

		setContentView(mLinearLayout);

		action = getIntent().getAction();

		DisplayMetrics dm = new DisplayMetrics();
		dm = getResources().getDisplayMetrics();

		mSearchDialog = new AlertDialog.Builder(this).create();
		mSearchDialog.setVolumeControlStream(AudioManager.STREAM_MUSIC);

		if ((INTENT_SEARCH_ALBUMART.equals(action) || INTENT_SEARCH_LYRICS.equals(action))) {

			mPath = icicle != null ? icicle.getString(INTENT_KEY_PATH) : getIntent()
					.getStringExtra(INTENT_KEY_PATH);

			mContainer = new LinearLayout(this);
			mContainer.setOrientation(LinearLayout.VERTICAL);

			mKeywordSummary1 = new TextView(this);
			mKeywordSummary1.setTextAppearance(this, android.R.attr.textAppearanceSmall);
			mKeywordSummary1.setText(R.string.artist);
			mContainer.addView(mKeywordSummary1);

			mKeyword1 = icicle != null ? icicle.getString(INTENT_KEY_ARTIST) : getIntent()
					.getStringExtra(INTENT_KEY_ARTIST);
			mSearchKeyword1 = new EditText(this);
			mSearchKeyword1.setSingleLine(true);
			mSearchKeyword1.addTextChangedListener(this);
			mContainer.addView(mSearchKeyword1);

			mKeywordSummary2 = new TextView(this);
			mKeywordSummary2.setTextAppearance(this, android.R.attr.textAppearanceSmall);

			if (INTENT_SEARCH_ALBUMART.equals(action)) {
				mKeyword2 = icicle != null ? icicle.getString(INTENT_KEY_ALBUM) : getIntent()
						.getStringExtra(INTENT_KEY_ALBUM);
				mKeywordSummary2.setText(R.string.album);
			} else if (INTENT_SEARCH_LYRICS.equals(action)) {
				mKeyword2 = icicle != null ? icicle.getString(INTENT_KEY_TRACK) : getIntent()
						.getStringExtra(INTENT_KEY_TRACK);
				mKeywordSummary2.setText(R.string.track);
			}
			mContainer.addView(mKeywordSummary2);

			mSearchKeyword2 = new EditText(this);
			mSearchKeyword2.setSingleLine(true);
			mSearchKeyword2.addTextChangedListener(this);
			mContainer.addView(mSearchKeyword2);

			mSearchDialog.setIcon(android.R.drawable.ic_dialog_info);

			if (INTENT_SEARCH_ALBUMART.equals(action)) {
				mSearchDialog.setTitle(R.string.search_albumart);
			} else if (INTENT_SEARCH_LYRICS.equals(action)) {
				mSearchDialog.setTitle(R.string.search_lyrics);
			}
			mSearchDialog.setView(mContainer, (int) (8 * dm.density), (int) (4 * dm.density),
					(int) (8 * dm.density), (int) (4 * dm.density));
			mSearchDialog.setButton(Dialog.BUTTON_POSITIVE, getString(android.R.string.search_go),
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {

							// Do nothing here. We override the onClick
						}
					});
			mSearchDialog.setButton(Dialog.BUTTON_NEGATIVE, getString(android.R.string.cancel),
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {

							finish();
						}
					});
			mSearchDialog.setOnShowListener(new DialogInterface.OnShowListener() {

				@Override
				public void onShow(DialogInterface dialog) {

					Button mButton = mSearchDialog.getButton(AlertDialog.BUTTON_POSITIVE);
					if (INTENT_SEARCH_ALBUMART.equals(action)) {
						mButton.setOnClickListener(mSearchAlbumArtOnClickListener);
					} else if (INTENT_SEARCH_LYRICS.equals(action)) {
						mButton.setOnClickListener(mSearchLyricsOnClickListener);
					}
				}
			});
			mSearchDialog.setOnCancelListener(this);

			mProgress = new ProgressDialog(this);
			mProgress.setCancelable(true);
			mProgress.setOnCancelListener(this);

			mSearchDialog.show();
			mSearchKeyword1.setText(mKeyword1);
			mSearchKeyword2.setText(mKeyword2);
			setSaveButton();
		} else {
			Toast.makeText(this, R.string.error_bad_parameters, Toast.LENGTH_SHORT).show();
			finish();
		}

	}

	@Override
	protected void onResume() {

		super.onResume();
		if (mSearchDialog != null && !mSearchDialog.isShowing()) {
			mSearchDialog.show();
		}
		if (mLyricsChooser != null && restore_lyrics_chooser) {
			mLyricsChooser.show();
		}
		if (mLyricsConfirm != null && restore_lyrics_confirm) {
			mLyricsConfirm.show();
		}
		if (mAlbumArtConfirm != null && restore_albumart_confirm) {
			mAlbumArtConfirm.show();
		}
		if ((mLyricsSearchTask != null && mLyricsSearchTask.getStatus().equals(Status.RUNNING))
				|| (mAlbumArtSearchTask != null && !mAlbumArtSearchTask.getStatus().equals(
						Status.RUNNING))) {
			mProgress.setMessage(getString(R.string.searching_please_wait));
			mProgress.show();
		}
		if ((mLyricsDownloadTask != null && mLyricsDownloadTask.getStatus().equals(Status.RUNNING))
				|| (mAlbumArtDownloadTask != null && !mAlbumArtDownloadTask.getStatus().equals(
						Status.RUNNING))) {
			mProgress.setMessage(getString(R.string.downloading_please_wait));
			mProgress.show();
		}
	}

	@Override
	public void onPause() {

		if (mSearchDialog != null && mSearchDialog.isShowing()) {
			mSearchDialog.dismiss();
		}
		if (mLyricsChooser != null && mLyricsChooser.isShowing()) {
			restore_lyrics_chooser = true;
			mLyricsChooser.dismiss();
		}
		if (mLyricsConfirm != null && mLyricsConfirm.isShowing()) {
			restore_lyrics_confirm = true;
			mLyricsConfirm.dismiss();
		}
		if (mAlbumArtConfirm != null && mAlbumArtConfirm.isShowing()) {
			restore_albumart_confirm = true;
			mAlbumArtConfirm.dismiss();
		}
		if ((mLyricsSearchTask != null || mAlbumArtSearchTask != null
				|| mLyricsDownloadTask != null || mAlbumArtDownloadTask != null)
				&& mProgress.isShowing()) {
			mProgress.dismiss();
		}
		super.onPause();
	}

	@Override
	public void onSaveInstanceState(Bundle outcicle) {

		if (INTENT_SEARCH_ALBUMART.equals(action)) {
			outcicle.putString(INTENT_KEY_ALBUM, mSearchKeyword2.getText().toString());
		} else if (INTENT_SEARCH_LYRICS.equals(action)) {
			outcicle.putString(INTENT_KEY_TRACK, mSearchKeyword2.getText().toString());
		}
		outcicle.putString(INTENT_KEY_PATH, mPath);
		outcicle.putString(INTENT_KEY_ARTIST, mSearchKeyword1.getText().toString());
	}

	private void setSaveButton() {

		String mTypedKeyword1 = mSearchKeyword1.getText().toString();
		String mTypedKeyword2 = mSearchKeyword2.getText().toString();
		Button button = mSearchDialog.getButton(Dialog.BUTTON_POSITIVE);
		if (mTypedKeyword1.trim().length() == 0 || mTypedKeyword2.trim().length() == 0) {
			button.setEnabled(false);
		} else {
			button.setEnabled(true);
		}
		button.invalidate();
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {

		// don't care about this one
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {

		setSaveButton();
	};

	@Override
	public void afterTextChanged(Editable s) {

		// don't care about this one
	}

	private View.OnClickListener mSearchLyricsOnClickListener = new View.OnClickListener() {

		@Override
		public void onClick(View view) {

			String mTypedKeyword1 = mSearchKeyword1.getText().toString();
			String mTypedKeyword2 = mSearchKeyword2.getText().toString();
			mLyricsSearchTask = new LyricsSearchTask();
			mLyricsSearchTask.execute(mTypedKeyword1, mTypedKeyword2, mPath);
		}
	};

	private View.OnClickListener mSearchAlbumArtOnClickListener = new View.OnClickListener() {

		@Override
		public void onClick(View view) {

			String mTypedKeyword1 = mSearchKeyword1.getText().toString();
			String mTypedKeyword2 = mSearchKeyword2.getText().toString();
			mAlbumArtSearchTask = new AlbumArtSearchTask();
			mAlbumArtSearchTask.execute(mTypedKeyword1, mTypedKeyword2, mPath);
		}
	};

	@Override
	public void onCancel(DialogInterface dialog) {

		if (dialog == mSearchDialog) {
			finish();
		}
		if (dialog == mSearchProgress) {
			if (mLyricsSearchTask != null) {
				mLyricsSearchTask.cancel(true);
			}
			if (mAlbumArtSearchTask != null) {
				mAlbumArtSearchTask.cancel(true);
			}
		}
		if (dialog == mDownloadProgress) {
			if (mLyricsDownloadTask != null) {
				mLyricsDownloadTask.cancel(true);
			}
			if (mAlbumArtDownloadTask != null) {
				mAlbumArtDownloadTask.cancel(true);
			}
		}

		return;
	}

	private class LyricsSearchTask extends AsyncTask<String, Void, String[]> implements
			OnCancelListener, OnClickListener {

		private int mItem = 0;

		@Override
		protected String[] doInBackground(String... params) {

			mPath = params[2];
			try {
				return mDownloader.search(params[0], params[1]);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (XmlPullParserException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPreExecute() {

			mDownloader = new LyricsDownloader();
			mProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			mProgress.setMessage(getString(R.string.searching_please_wait));
			mProgress.show();
		}

		@Override
		protected void onPostExecute(String[] result) {

			if (mProgress != null) {
				mProgress.dismiss();
			}
			if (result.length > 0) {
				chooseLyrics(result);
			} else {
				Toast.makeText(SearchDialog.this, R.string.search_noresult, Toast.LENGTH_SHORT)
						.show();
			}
		}

		@Override
		public void onCancel(DialogInterface dialog) {

			if (dialog == mLyricsChooser) {
				restore_lyrics_chooser = false;
			}
			if (dialog == mLyricsConfirm) {
				restore_lyrics_confirm = false;
			}

		}

		@Override
		public void onClick(DialogInterface dialog, int item) {

			if (dialog == mLyricsChooser) {
				restore_lyrics_chooser = false;
				confirmOverwrite(item, mPath);
			}
			if (dialog == mLyricsConfirm) {
				restore_lyrics_confirm = false;
				switch (item) {
					case DialogInterface.BUTTON_POSITIVE:
						mLyricsDownloadTask = new LyricsDownloadTask();
						mLyricsDownloadTask.execute(String.valueOf(mItem), mPath);
						break;
				}
			}
		}

		private void chooseLyrics(final String[] result) {

			mLyricsChooser = new AlertDialog.Builder(SearchDialog.this)
					.setTitle(R.string.search_lyrics).setItems(result, this)
					.setOnCancelListener(this).show();
		}

		private void confirmOverwrite(int item, String path) {

			mItem = item;

			if (new File(mPath).exists()) {
				mLyricsConfirm = new AlertDialog.Builder(SearchDialog.this)
						.setIcon(android.R.drawable.ic_dialog_alert)
						.setTitle(R.string.confirm_overwrite)
						.setMessage(getString(R.string.lyrics_already_exist))
						.setPositiveButton(android.R.string.ok, this)
						.setNegativeButton(android.R.string.cancel, this).setOnCancelListener(this)
						.show();
			} else {
				mLyricsDownloadTask = new LyricsDownloadTask();
				mLyricsDownloadTask.execute(String.valueOf(item), mPath);
			}
		}

	}

	private class LyricsDownloadTask extends AsyncTask<String, Integer, Void> {

		@Override
		protected Void doInBackground(String... params) {

			try {
				mDownloader.download(Integer.valueOf(params[0]), params[1]);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPreExecute() {

			mProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			mProgress.setMessage(getString(R.string.downloading_please_wait));
			mProgress.show();
		}

		@Override
		protected void onPostExecute(Void result) {

			if (mProgress != null) {
				mProgress.dismiss();
			}
			if (mSearchDialog != null && mSearchDialog.isShowing()) {
				mSearchDialog.dismiss();
			}
			MusicUtils.reloadLyrics();
			finish();
		}
	}

	private class AlbumArtSearchTask extends AsyncTask<String, Void, String> implements
			OnClickListener {

		String mUrl, mPath;

		@Override
		protected String doInBackground(String... params) {

			mPath = params[2];
			try {
				return ImageDownloader.getCoverUrl(params[0], params[1]);
			} catch (Exception e) {
				return null;
			}
		}

		@Override
		protected void onPreExecute() {

			mProgress.setMessage(getString(R.string.searching_please_wait));
			mProgress.show();
		}

		@Override
		protected void onPostExecute(String result) {

			if (mProgress != null) {
				mProgress.dismiss();
			}
			if (result != null) {
				confirmOverwrite(result, mPath);
			} else {
				Toast.makeText(SearchDialog.this, R.string.search_noresult, Toast.LENGTH_SHORT)
						.show();
			}

		}

		@Override
		public void onClick(DialogInterface dialog, int which) {

			restore_albumart_confirm = false;
			if (dialog == mAlbumArtConfirm) {
				switch (which) {
					case DialogInterface.BUTTON_POSITIVE:
						mAlbumArtDownloadTask = new AlbumArtDownloadTask();
						mAlbumArtDownloadTask.execute(mUrl, mPath);
				}
			}

		}

		private void confirmOverwrite(final String url, final String path) {

			mUrl = url;
			mPath = path;

			if (new File(path).exists()) {
				mAlbumArtConfirm = new AlertDialog.Builder(SearchDialog.this)
						.setIcon(android.R.drawable.ic_dialog_alert)
						.setTitle(R.string.confirm_overwrite)
						.setMessage(getString(R.string.albumart_already_exist))
						.setPositiveButton(android.R.string.ok, this)
						.setNegativeButton(android.R.string.cancel, this).show();
			} else {
				mAlbumArtDownloadTask = new AlbumArtDownloadTask();
				mAlbumArtDownloadTask.execute(url, path);
			}
		}

	}

	private class AlbumArtDownloadTask extends AsyncTask<String, Void, Bitmap> {

		String albumArtPath;

		@Override
		protected Bitmap doInBackground(String... params) {

			albumArtPath = params[1];
			return ImageDownloader.getCoverBitmap(params[0]);
		}

		@Override
		protected void onPreExecute() {

			mProgress.setMessage(getString(R.string.downloading_please_wait));
			mProgress.show();
		}

		@Override
		protected void onPostExecute(Bitmap result) {

			File art = new File(albumArtPath);
			art.delete();
			writeAlbumArt(result, albumArtPath);
			if (mProgress != null) {
				mProgress.dismiss();
			}
			if (mSearchDialog != null && mSearchDialog.isShowing()) {
				mSearchDialog.dismiss();
			}
			finish();
		}

		private void writeAlbumArt(Bitmap bitmap, String path) {

			try {
				FileOutputStream fos = new FileOutputStream(path);
				bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
				fos.flush();
				fos.close();
				MusicUtils.clearAlbumArtCache();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
