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

package org.musicmod.android;

import org.musicmod.android.activity.GridActivity;
import org.musicmod.android.activity.MusicSettingsActivity;
import org.musicmod.android.util.MusicUtils;
import org.musicmod.android.util.ServiceToken;

import android.app.SearchManager;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AlphabetIndexer;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.GridView;
import android.widget.SectionIndexer;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class AlbumBrowserActivity extends GridActivity implements View.OnCreateContextMenuListener,
		Constants, ServiceConnection, OnItemClickListener {

	private String mCurrentAlbumId;
	private String mCurrentAlbumName;
	private String mCurrentArtistNameForAlbum;
	boolean mIsUnknownArtist;
	boolean mIsUnknownAlbum;
	private AlbumListAdapter mAdapter;
	private boolean mAdapterSent;
	private final static int SEARCH = CHILD_MENU_BASE;
	private ServiceToken mToken;
	private boolean mShowFadeAnimation = false;
	private GridView mGridView;

	public AlbumBrowserActivity() {

	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle icicle) {

		super.onCreate(icicle);

		if (icicle != null) {
			mCurrentAlbumId = icicle.getString(INTENT_KEY_ALBUM);
			mArtistId = icicle.getString(INTENT_KEY_ARTIST);
		} else {
			mArtistId = getIntent().getStringExtra(INTENT_KEY_ARTIST);
		}

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		mToken = MusicUtils.bindToService(this, this);

		IntentFilter f = new IntentFilter();
		f.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
		f.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
		f.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
		f.addDataScheme("file");
		registerReceiver(mScanListener, f);

		setContentView(R.layout.album_browser);
		MusicUtils.updateButtonBar(this, R.id.albumtab);
		mGridView = getGridView();
		mGridView.setOnItemClickListener(this);
		mGridView.setOnCreateContextMenuListener(this);
		mGridView.setTextFilterEnabled(true);

		mAdapter = (AlbumListAdapter) getLastNonConfigurationInstance();
		if (mAdapter == null) {
			mAdapter = new AlbumListAdapter(getApplicationContext(), mAlbumCursor, false);
			setAdapter(mAdapter);
			setTitle(R.string.working_albums);
			getAlbumCursor(mAdapter.getQueryHandler(), null);
		} else {
			setAdapter(mAdapter);
			mAlbumCursor = mAdapter.getCursor();
			if (mAlbumCursor != null) {
				init(mAlbumCursor);
			} else {
				getAlbumCursor(mAdapter.getQueryHandler(), null);
			}
		}
	}

	@Override
	public Object onRetainNonConfigurationInstance() {

		mAdapterSent = true;
		return mAdapter;
	}

	@Override
	public void onSaveInstanceState(Bundle outcicle) {

		// need to store the selected item so we don't lose it in case
		// of an orientation switch. Otherwise we could lose it while
		// in the middle of specifying a playlist to add the item to.
		outcicle.putString(INTENT_KEY_ALBUM, mCurrentAlbumId);
		outcicle.putString(INTENT_KEY_ARTIST, mArtistId);
		super.onSaveInstanceState(outcicle);
	}

	@Override
	public void onDestroy() {

		MusicUtils.unbindFromService(mToken);
		// If we have an adapter and didn't send it off to another activity yet,
		// we should
		// close its cursor, which we do by assigning a null cursor to it. Doing
		// this
		// instead of closing the cursor directly keeps the framework from
		// accessing
		// the closed cursor later.
		if (!mAdapterSent && mAdapter != null) {
			mAdapter.changeCursor(null);
		}
		// Because we pass the adapter to the next activity, we need to make
		// sure it doesn't keep a reference to this activity. We can do this
		// by clearing its DatasetObservers, which setListAdapter(null) does.
		setAdapter(null);
		mAdapter = null;
		unregisterReceiver(mScanListener);
		super.onDestroy();
	}

	@Override
	public void onResume() {

		super.onResume();

		loadSettings();

		IntentFilter f = new IntentFilter();
		f.addAction(BROADCAST_META_CHANGED);
		f.addAction(BROADCAST_QUEUE_CHANGED);
		registerReceiver(mTrackListListener, f);
		mTrackListListener.onReceive(null, null);

		MusicUtils.setSpinnerState(this);
	}

	private void loadSettings() {

		try {
			float mTransitionAnimation = Settings.System.getFloat(this.getContentResolver(),
					Settings.System.TRANSITION_ANIMATION_SCALE);
			if (mTransitionAnimation > 0.0) {
				mShowFadeAnimation = true;
			} else {
				mShowFadeAnimation = false;
			}

		} catch (SettingNotFoundException e) {
			e.printStackTrace();
		}
	}

	private BroadcastReceiver mTrackListListener = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {

			mGridView.invalidateViews();
			MusicUtils.updateNowPlaying(AlbumBrowserActivity.this);
		}
	};

	private BroadcastReceiver mScanListener = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {

			MusicUtils.setSpinnerState(AlbumBrowserActivity.this);
			mReScanHandler.sendEmptyMessage(0);
			if (intent.getAction().equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
				MusicUtils.clearAlbumArtCache();
			}
		}
	};

	private Handler mReScanHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {

			if (mAdapter != null) {
				getAlbumCursor(mAdapter.getQueryHandler(), null);
			}
		}
	};

	@Override
	public void onPause() {

		unregisterReceiver(mTrackListListener);
		mReScanHandler.removeCallbacksAndMessages(null);
		super.onPause();
	}

	public void init(Cursor cursor) {

		if (mAdapter == null) {
			return;
		}
		mAdapter.changeCursor(cursor); // also sets mAlbumCursor

		if (mAlbumCursor == null) {
			MusicUtils.displayDatabaseError(this);
			closeContextMenu();
			mReScanHandler.sendEmptyMessageDelayed(0, 1000);
			return;
		}

		MusicUtils.hideDatabaseError(this);
		MusicUtils.updateButtonBar(this, R.id.albumtab);
		setTitle();
	}

	private void setTitle() {

		CharSequence fancyName = "";
		if (mAlbumCursor != null && mAlbumCursor.getCount() > 0) {
			mAlbumCursor.moveToFirst();
			fancyName = mAlbumCursor.getString(mAlbumCursor
					.getColumnIndex(MediaStore.Audio.Albums.ARTIST));
			if (fancyName == null || fancyName.equals(MediaStore.UNKNOWN_STRING))
				fancyName = getText(R.string.unknown_artist);
		}

		if (mArtistId != null && fancyName != null)
			setTitle(fancyName);
		else
			setTitle(R.string.albums_title);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfoIn) {

		menu.add(0, PLAY_SELECTION, 0, R.string.play_selection);
		SubMenu sub = menu.addSubMenu(0, ADD_TO_PLAYLIST, 0, R.string.add_to_playlist);
		MusicUtils.makePlaylistMenu(this, sub);
		menu.add(0, DELETE_ITEMS, 0, R.string.delete_item);

		AdapterContextMenuInfo mi = (AdapterContextMenuInfo) menuInfoIn;
		mAlbumCursor.moveToPosition(mi.position);
		mCurrentAlbumId = mAlbumCursor.getString(mAlbumCursor
				.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID));
		mCurrentAlbumName = mAlbumCursor.getString(mAlbumCursor
				.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM));
		mCurrentArtistNameForAlbum = mAlbumCursor.getString(mAlbumCursor
				.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST));
		mIsUnknownArtist = mCurrentArtistNameForAlbum == null
				|| mCurrentArtistNameForAlbum.equals(MediaStore.UNKNOWN_STRING);
		mIsUnknownAlbum = mCurrentAlbumName == null
				|| mCurrentAlbumName.equals(MediaStore.UNKNOWN_STRING);
		if (mIsUnknownAlbum) {
			menu.setHeaderTitle(getString(R.string.unknown_album));
		} else {
			menu.setHeaderTitle(mCurrentAlbumName);
		}
		if (!mIsUnknownAlbum || !mIsUnknownArtist) {
			menu.add(0, SEARCH, 0, android.R.string.search_go);
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {

		Intent intent;

		switch (item.getItemId()) {
			case PLAY_SELECTION: {
				// play the selected album
				long[] list = MusicUtils.getSongListForAlbum(this, Long.parseLong(mCurrentAlbumId));
				MusicUtils.playAll(this, list, 0);
				return true;
			}

			case QUEUE: {
				long[] list = MusicUtils.getSongListForAlbum(this, Long.parseLong(mCurrentAlbumId));
				MusicUtils.addToCurrentPlaylist(this, list);
				return true;
			}

			case NEW_PLAYLIST: {
				intent = new Intent(INTENT_CREATE_PLAYLIST);
				long[] list = MusicUtils.getSongListForAlbum(this, Long.parseLong(mCurrentAlbumId));
				intent.putExtra(INTENT_KEY_LIST, list);
				startActivity(intent);
				return true;
			}

			case PLAYLIST_SELECTED: {
				long[] list = MusicUtils.getSongListForAlbum(this, Long.parseLong(mCurrentAlbumId));
				long playlist = item.getIntent().getLongExtra("playlist", 0);
				MusicUtils.addToPlaylist(this, list, playlist);
				return true;
			}
			case DELETE_ITEMS: {
				final long[] list = MusicUtils.getSongListForAlbum(this,
						Long.parseLong(mCurrentAlbumId));
				intent = new Intent(INTENT_DELETE_ITEMS);
				intent.putExtra(INTENT_KEY_CONTENT, mCurrentAlbumName);
				intent.putExtra(INTENT_KEY_TYPE, TYPE_ALBUM);
				intent.putExtra(INTENT_KEY_ITEMS, list);
				startActivity(intent);
				return true;
			}
			case SEARCH:
				doSearch();
				return true;

		}
		return super.onContextItemSelected(item);
	}

	void doSearch() {

		CharSequence title = null;
		String query = "";

		Intent i = new Intent();
		i.setAction(MediaStore.INTENT_ACTION_MEDIA_SEARCH);
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		title = "";
		if (!mIsUnknownAlbum) {
			query = mCurrentAlbumName;
			i.putExtra(MediaStore.EXTRA_MEDIA_ALBUM, mCurrentAlbumName);
			title = mCurrentAlbumName;
		}
		if (!mIsUnknownArtist) {
			query = query + " " + mCurrentArtistNameForAlbum;
			i.putExtra(MediaStore.EXTRA_MEDIA_ARTIST, mCurrentArtistNameForAlbum);
			title = title + " " + mCurrentArtistNameForAlbum;
		}
		// Since we hide the 'search' menu item when both album and artist are
		// unknown, the query and title strings will have at least one of those.
		i.putExtra(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE);
		title = getString(R.string.mediasearch, title);
		i.putExtra(SearchManager.QUERY, query);

		startActivity(Intent.createChooser(i, title));
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

		switch (requestCode) {
			case SCAN_DONE:
				if (resultCode == RESULT_CANCELED) {
					finish();
				} else {
					getAlbumCursor(mAdapter.getQueryHandler(), null);
				}
				break;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View v, int position, long id) {

		Intent intent = new Intent(Intent.ACTION_PICK);
		intent.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/track");
		intent.setPackage("org.musicmod.android");
		intent.putExtra(INTENT_KEY_ALBUM, Long.valueOf(id).toString());
		intent.putExtra(INTENT_KEY_ARTIST, mArtistId);
		startActivity(intent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.options_menu_album, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {

		MusicUtils.setPartyShuffleMenuIcon(menu);
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		Intent intent;
		switch (item.getItemId()) {
			case PARTY_SHUFFLE:
				MusicUtils.togglePartyShuffle();
				break;

			case SHUFFLE_ALL:
				MusicUtils.shuffleAll(this);
				return true;

			case SETTINGS:
				intent = new Intent();
				intent.setClass(this, MusicSettingsActivity.class);
				startActivityForResult(intent, SETTINGS);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private Cursor getAlbumCursor(AsyncQueryHandler async, String filter) {

		String[] cols = new String[] { MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ARTIST,
				MediaStore.Audio.Albums.ALBUM };

		Cursor ret = null;
		if (mArtistId != null) {
			Uri uri = MediaStore.Audio.Artists.Albums.getContentUri("external",
					Long.valueOf(mArtistId));
			if (!TextUtils.isEmpty(filter)) {
				uri = uri.buildUpon().appendQueryParameter("filter", Uri.encode(filter)).build();
			}
			if (async != null) {
				async.startQuery(0, null, uri, cols, null, null,
						MediaStore.Audio.Albums.DEFAULT_SORT_ORDER);
			} else {
				ret = MusicUtils.query(this, uri, cols, null, null,
						MediaStore.Audio.Albums.DEFAULT_SORT_ORDER);
			}
		} else {
			Uri uri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
			if (!TextUtils.isEmpty(filter)) {
				uri = uri.buildUpon().appendQueryParameter("filter", Uri.encode(filter)).build();
			}
			if (async != null) {
				async.startQuery(0, null, uri, cols, null, null,
						MediaStore.Audio.Albums.DEFAULT_SORT_ORDER);
			} else {
				ret = MusicUtils.query(this, uri, cols, null, null,
						MediaStore.Audio.Albums.DEFAULT_SORT_ORDER);
			}
		}
		return ret;
	}

	private class AlbumListAdapter extends CursorAdapter implements SectionIndexer {

		private final BitmapDrawable mDefaultAlbumIcon;
		private int mAlbumIndex;
		private int mArtistIndex;
		private int mAlbumIdIndex;
		private final Resources mResources;
		private AlphabetIndexer mIndexer;
		private AsyncQueryHandler mQueryHandler;
		private String mConstraint = null;
		private boolean mConstraintIsValid = false;

		private class ViewHolder {

			TextView album_name;
			TextView artist_name;
			ImageView album_art;

			public ViewHolder(View view) {
				album_name = (TextView) view.findViewById(R.id.album_name);
				artist_name = (TextView) view.findViewById(R.id.artist_name);
				album_art = (ImageView) view.findViewById(R.id.album_art);
			}

		}

		private class QueryHandler extends AsyncQueryHandler {

			public QueryHandler(ContentResolver resolver) {

				super(resolver);
			}

			@Override
			protected void onQueryComplete(int token, Object cookie, Cursor cursor) {

				AlbumBrowserActivity.this.init(cursor);
			}
		}

		private AlbumListAdapter(Context context, Cursor cursor, boolean autoRequery) {

			super(context, cursor, autoRequery);

			mQueryHandler = new QueryHandler(context.getContentResolver());

			Resources r = context.getResources();

			Bitmap b = BitmapFactory.decodeResource(r, R.drawable.ic_mp_albumart_unknown);
			mDefaultAlbumIcon = new BitmapDrawable(context.getResources(), b);
			// no filter or dither, it's a lot faster and we can't tell the
			// difference
			mDefaultAlbumIcon.setFilterBitmap(false);
			mDefaultAlbumIcon.setDither(false);
			getColumnIndices(cursor);
			mResources = context.getResources();
		}

		private void getColumnIndices(Cursor cursor) {

			if (cursor != null) {
				mAlbumIdIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID);
				mAlbumIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM);
				mArtistIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST);

				if (mIndexer != null) {
					mIndexer.setCursor(cursor);
				} else {
					mIndexer = new AlphabetIndexer(cursor, mAlbumIndex,
							mResources.getString(R.string.fast_scroll_alphabet));
				}
			}
		}

		public AsyncQueryHandler getQueryHandler() {

			return mQueryHandler;
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {

			View view = getLayoutInflater().inflate(R.layout.album_grid_item, null);
			ViewHolder viewholder = new ViewHolder(view);
			view.setTag(viewholder);
			return view;
		}

//		@Override
//		public View getView(int position, View convertView, ViewGroup parent) {
//
//			View view = super.getView(position, convertView, parent);
//
//			mAlbumCursor.moveToPosition(position);
//
//			ViewHolder viewholder = (ViewHolder) view.getTag();
//
//			String album_name = mAlbumCursor.getString(mAlbumIndex);
//			if (album_name == null || MediaStore.UNKNOWN_STRING.equals(album_name)) {
//				viewholder.album_name.setText(R.string.unknown_album_name);
//			} else {
//				viewholder.album_name.setText(album_name);
//			}
//
//			String artist_name = mAlbumCursor.getString(mArtistIndex);
//			if (album_name == null || MediaStore.UNKNOWN_STRING.equals(album_name)) {
//				viewholder.artist_name.setText(R.string.unknown_artist_name);
//			} else {
//				viewholder.artist_name.setText(artist_name);
//			}
//
//			// We don't actually need the path to the thumbnail file,
//			// we just use it to see if there is album art or not
//			long aid = mAlbumCursor.getLong(mAlbumIdIndex);
//			int width = getResources().getDimensionPixelSize(R.dimen.gridview_bitmap_width);
//			int height = getResources().getDimensionPixelSize(R.dimen.gridview_bitmap_height);
//
//			viewholder.album_art.setImageBitmap(MusicUtils.getCachedArtwork(
//					getApplicationContext(), aid, width, height));
//
//			// viewholder.album_art.setTag(aid);
//			// new AsyncAlbumArtLoader(viewholder.album_art, mShowFadeAnimation,
//			// aid, width, height).execute();
//
//			long currentalbumid = MusicUtils.getCurrentAlbumId();
//			if (currentalbumid == aid) {
//				viewholder.album_name.setCompoundDrawablesWithIntrinsicBounds(0, 0,
//						R.drawable.ic_indicator_nowplaying_small, 0);
//			} else {
//				viewholder.album_name.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
//			}
//
//			return view;
//		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			
			ViewHolder viewholder = (ViewHolder) view.getTag();

			String album_name = cursor.getString(mAlbumIndex);
			if (album_name == null || MediaStore.UNKNOWN_STRING.equals(album_name)) {
				viewholder.album_name.setText(R.string.unknown_album);
			} else {
				viewholder.album_name.setText(album_name);
			}

			String artist_name = mAlbumCursor.getString(mArtistIndex);
			if (album_name == null || MediaStore.UNKNOWN_STRING.equals(album_name)) {
				viewholder.artist_name.setText(R.string.unknown_artist);
			} else {
				viewholder.artist_name.setText(artist_name);
			}

			// We don't actually need the path to the thumbnail file,
			// we just use it to see if there is album art or not
			long aid = mAlbumCursor.getLong(mAlbumIdIndex);
			int width = getResources().getDimensionPixelSize(R.dimen.gridview_bitmap_width);
			int height = getResources().getDimensionPixelSize(R.dimen.gridview_bitmap_height);

			viewholder.album_art.setImageBitmap(MusicUtils.getCachedArtwork(
					getApplicationContext(), aid, width, height));

			// viewholder.album_art.setTag(aid);
			// new AsyncAlbumArtLoader(viewholder.album_art, mShowFadeAnimation,
			// aid, width, height).execute();

			long currentalbumid = MusicUtils.getCurrentAlbumId();
			if (currentalbumid == aid) {
				viewholder.album_name.setCompoundDrawablesWithIntrinsicBounds(0, 0,
						R.drawable.ic_indicator_nowplaying_small, 0);
			} else {
				viewholder.album_name.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
			}

		}
		
		@Override
		public void changeCursor(Cursor cursor) {

			if (isFinishing() && cursor != null) {
				cursor.close();
				cursor = null;
			}
			if (cursor != mAlbumCursor) {
				mAlbumCursor = cursor;
				getColumnIndices(cursor);
				super.changeCursor(cursor);
			}
		}

		@Override
		public Cursor runQueryOnBackgroundThread(CharSequence constraint) {

			String s = constraint.toString();
			if (mConstraintIsValid
					&& ((s == null && mConstraint == null) || (s != null && s.equals(mConstraint)))) {
				return getCursor();
			}
			Cursor c = getAlbumCursor(null, s);
			mConstraint = s;
			mConstraintIsValid = true;
			return c;
		}

		public Object[] getSections() {

			return mIndexer.getSections();
		}

		public int getPositionForSection(int section) {

			return mIndexer.getPositionForSection(section);
		}

		public int getSectionForPosition(int position) {

			return 0;
		}

	}

	private Cursor mAlbumCursor;
	private String mArtistId;

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {

		MusicUtils.updateNowPlaying(this);
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {

		finish();
	}

	// FIXME image loaded some times incorrect
	private class AsyncAlbumArtLoader extends AsyncTask<Object, Void, Bitmap> {

		boolean enable_animation = false;
		private ImageView imageview;
		private long album_id;
		private int width, height;

		public AsyncAlbumArtLoader(ImageView imageview, Boolean animation, long album_id,
				int width, int height) {

			enable_animation = animation;
			this.imageview = imageview;
			this.album_id = album_id;
			this.width = width;
			this.height = height;
		}

		@Override
		protected void onPreExecute() {

			if (imageview.getTag() == null || (Long) imageview.getTag() != album_id) {
				return;
			}

			if (enable_animation) {
				imageview.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(),
						android.R.anim.fade_out));
				imageview.setVisibility(View.INVISIBLE);
			}
		}

		@Override
		protected Bitmap doInBackground(Object... params) {

			if (imageview.getTag() == null || (Long) imageview.getTag() != album_id) {
				return null;
			}

			return MusicUtils.getCachedArtwork(getApplicationContext(), album_id, width, height);
		}

		@Override
		protected void onPostExecute(Bitmap result) {

			if (imageview.getTag() == null || (Long) imageview.getTag() != album_id) {
				return;
			}

			if (result != null) {
				imageview.setImageBitmap(result);
			} else {
				imageview.setImageResource(R.drawable.ic_mp_albumart_unknown);
			}
			if (enable_animation) {
				imageview.setVisibility(View.VISIBLE);
				imageview.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(),
						android.R.anim.fade_in));
			}

		}
	}

}