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

import android.app.ExpandableListActivity;
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
import android.database.CursorWrapper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AlphabetIndexer;
import android.widget.ExpandableListView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SectionIndexer;
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleCursorTreeAdapter;
import android.widget.TextView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;

import org.musicmod.android.activity.MusicSettingsActivity;
import org.musicmod.android.util.MusicUtils;
import org.musicmod.android.util.ServiceToken;

public class ArtistAlbumBrowserActivity extends ExpandableListActivity implements
		View.OnCreateContextMenuListener, Constants, ServiceConnection {

	private String mCurrentArtistId;
	private String mCurrentArtistName;
	private String mCurrentAlbumId;
	private String mCurrentAlbumName;
	private String mCurrentArtistNameForAlbum;
	boolean mIsUnknownArtist;
	boolean mIsUnknownAlbum;
	private ArtistAlbumListAdapter mArtistAdapter;
	private AlbumChildAdapter mAlbumAdapter;
	private boolean mAdapterSent;
	private final static int SEARCH = CHILD_MENU_BASE;
	private static int mLastListPosCourse = -1;
	private static int mLastListPosFine = -1;
	private ServiceToken mToken;
	private boolean mShowFadeAnimation = false;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle icicle) {

		super.onCreate(icicle);

		loadSettings();

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		if (icicle != null) {
			mCurrentAlbumId = icicle.getString("selectedalbum");
			mCurrentAlbumName = icicle.getString("selectedalbumname");
			mCurrentArtistId = icicle.getString("selectedartist");
			mCurrentArtistName = icicle.getString("selectedartistname");
		}
		mToken = MusicUtils.bindToService(this, this);

		IntentFilter f = new IntentFilter();
		f.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
		f.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
		f.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
		f.addDataScheme("file");
		registerReceiver(mScanListener, f);

		setContentView(R.layout.artist_album_browser);
		MusicUtils.updateButtonBar(this, R.id.artisttab);
		ExpandableListView lv = getExpandableListView();
		lv.setOnCreateContextMenuListener(this);
		lv.setTextFilterEnabled(true);

		mArtistAdapter = (ArtistAlbumListAdapter) getLastNonConfigurationInstance();

		if (mArtistAdapter == null) {
			mArtistAdapter = new ArtistAlbumListAdapter(getApplication(), this,
					null, // cursor
					R.layout.artist_list_item_group, new String[] {}, new int[] {},
					R.layout.artist_list_item_child, new String[] {}, new int[] {});
			setListAdapter(mArtistAdapter);
			setTitle(R.string.working_artists);
			getArtistCursor(mArtistAdapter.getQueryHandler(), null);
		} else {
			mArtistAdapter.setActivity(this);
			setListAdapter(mArtistAdapter);
			mArtistCursor = mArtistAdapter.getCursor();
			if (mArtistCursor != null) {
				init(mArtistCursor);
			} else {
				getArtistCursor(mArtistAdapter.getQueryHandler(), null);
			}
		}
	}

	@Override
	public Object onRetainNonConfigurationInstance() {

		mAdapterSent = true;
		return mArtistAdapter;
	}

	@Override
	public void onSaveInstanceState(Bundle outcicle) {

		// need to store the selected item so we don't lose it in case
		// of an orientation switch. Otherwise we could lose it while
		// in the middle of specifying a playlist to add the item to.
		outcicle.putString("selectedalbum", mCurrentAlbumId);
		outcicle.putString("selectedalbumname", mCurrentAlbumName);
		outcicle.putString("selectedartist", mCurrentArtistId);
		outcicle.putString("selectedartistname", mCurrentArtistName);
		super.onSaveInstanceState(outcicle);
	}

	@Override
	public void onDestroy() {

		ExpandableListView lv = getExpandableListView();
		if (lv != null) {
			mLastListPosCourse = lv.getFirstVisiblePosition();
			View cv = lv.getChildAt(0);
			if (cv != null) {
				mLastListPosFine = cv.getTop();
			}
		}

		MusicUtils.unbindFromService(mToken);
		// If we have an adapter and didn't send it off to another activity yet,
		// we should
		// close its cursor, which we do by assigning a null cursor to it. Doing
		// this
		// instead of closing the cursor directly keeps the framework from
		// accessing
		// the closed cursor later.
		if (!mAdapterSent && mArtistAdapter != null) {
			mArtistAdapter.changeCursor(null);
		}
		// Because we pass the adapter to the next activity, we need to make
		// sure it doesn't keep a reference to this activity. We can do this
		// by clearing its DatasetObservers, which setListAdapter(null) does.
		setListAdapter(null);
		mArtistAdapter = null;
		unregisterReceiver(mScanListener);
		setListAdapter(null);
		super.onDestroy();
	}

	@Override
	public void onResume() {

		super.onResume();
		IntentFilter f = new IntentFilter();
		f.addAction(BROADCAST_META_CHANGED);
		f.addAction(BROADCAST_QUEUE_CHANGED);
		registerReceiver(mTrackListListener, f);
		mTrackListListener.onReceive(null, null);

		MusicUtils.setSpinnerState(this);
	}

	private void configureActivity() {

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

			getExpandableListView().invalidateViews();
			MusicUtils.updateNowPlaying(ArtistAlbumBrowserActivity.this);
		}
	};

	private BroadcastReceiver mScanListener = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {

			MusicUtils.setSpinnerState(ArtistAlbumBrowserActivity.this);
			mReScanHandler.sendEmptyMessage(0);
			if (intent.getAction().equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
				MusicUtils.clearAlbumArtCache();
			}
		}
	};

	private Handler mReScanHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {

			if (mArtistAdapter != null) {
				getArtistCursor(mArtistAdapter.getQueryHandler(), null);
			}
		}
	};

	@Override
	public void onPause() {

		unregisterReceiver(mTrackListListener);
		mReScanHandler.removeCallbacksAndMessages(null);
		super.onPause();
	}

	public void init(Cursor c) {

		if (mArtistAdapter == null) {
			return;
		}
		mArtistAdapter.changeCursor(c); // also sets mArtistCursor

		if (mArtistCursor == null) {
			MusicUtils.displayDatabaseError(this);
			closeContextMenu();
			mReScanHandler.sendEmptyMessageDelayed(0, 1000);
			return;
		}

		// restore previous position
		if (mLastListPosCourse >= 0) {
			ExpandableListView elv = getExpandableListView();
			elv.setSelectionFromTop(mLastListPosCourse, mLastListPosFine);
			mLastListPosCourse = -1;
		}

		MusicUtils.hideDatabaseError(this);
		MusicUtils.updateButtonBar(this, R.id.artisttab);
		setTitle(R.string.artists);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.options_menu_artist, menu);
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
				break;
			case SETTINGS:
				intent = new Intent();
				intent.setClass(this, MusicSettingsActivity.class);
				startActivityForResult(intent, SETTINGS);
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfoIn) {

		menu.add(0, PLAY_SELECTION, 0, R.string.play_selection);
		SubMenu sub = menu.addSubMenu(0, ADD_TO_PLAYLIST, 0, R.string.add_to_playlist);
		MusicUtils.makePlaylistMenu(this, sub);
		menu.add(0, DELETE_ITEMS, 0, R.string.delete_item);

		ExpandableListContextMenuInfo mi = (ExpandableListContextMenuInfo) menuInfoIn;

		int itemtype = ExpandableListView.getPackedPositionType(mi.packedPosition);
		int gpos = ExpandableListView.getPackedPositionGroup(mi.packedPosition);
		if (itemtype == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
			if (gpos == -1) {
				// this shouldn't happen
				Log.d("Artist/Album", "no group");
				return;
			}
			gpos = gpos - getExpandableListView().getHeaderViewsCount();
			mArtistCursor.moveToPosition(gpos);
			mCurrentArtistId = mArtistCursor.getString(mArtistCursor
					.getColumnIndexOrThrow(MediaStore.Audio.Artists._ID));
			mCurrentArtistName = mArtistCursor.getString(mArtistCursor
					.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST));
			mCurrentAlbumId = null;
			mIsUnknownArtist = mCurrentArtistName == null
					|| mCurrentArtistName.equals(MediaStore.UNKNOWN_STRING);
			mIsUnknownAlbum = true;
			if (mIsUnknownArtist) {
				menu.setHeaderTitle(getString(R.string.unknown_artist_name));
			} else {
				menu.setHeaderTitle(mCurrentArtistName);
				menu.add(0, SEARCH, 0, android.R.string.search_go);
			}
			return;
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {

		Intent intent;

		switch (item.getItemId()) {
			case PLAY_SELECTION: {
				// play everything by the selected artist
				long[] list = mCurrentArtistId != null ? MusicUtils.getSongListForArtist(this,
						Long.parseLong(mCurrentArtistId)) : MusicUtils.getSongListForAlbum(this,
						Long.parseLong(mCurrentAlbumId));

				MusicUtils.playAll(this, list, 0);
				return true;
			}

			case QUEUE: {
				long[] list = mCurrentArtistId != null ? MusicUtils.getSongListForArtist(this,
						Long.parseLong(mCurrentArtistId)) : MusicUtils.getSongListForAlbum(this,
						Long.parseLong(mCurrentAlbumId));
				MusicUtils.addToCurrentPlaylist(this, list);
				return true;
			}

			case NEW_PLAYLIST: {
				intent = new Intent(INTENT_CREATE_PLAYLIST);
				long[] list = null;
				if (mCurrentArtistId != null) {
					list = MusicUtils.getSongListForArtist(this, Long.parseLong(mCurrentArtistId));
				} else if (mCurrentAlbumId != null) {
					list = MusicUtils.getSongListForAlbum(this, Long.parseLong(mCurrentAlbumId));
				}
				intent.putExtra(INTENT_KEY_LIST, list);
				startActivity(intent);
				return true;
			}

			case PLAYLIST_SELECTED: {
				long[] list = mCurrentArtistId != null ? MusicUtils.getSongListForArtist(this,
						Long.parseLong(mCurrentArtistId)) : MusicUtils.getSongListForAlbum(this,
						Long.parseLong(mCurrentAlbumId));
				long playlist = item.getIntent().getLongExtra("playlist", 0);
				MusicUtils.addToPlaylist(this, list, playlist);
				return true;
			}

			case DELETE_ITEMS: {
				final long[] list;
				String content;
				intent = new Intent(INTENT_DELETE_ITEMS);
				if (mCurrentArtistId != null) {
					list = MusicUtils.getSongListForArtist(this, Long.parseLong(mCurrentArtistId));
					content = mCurrentArtistName;
					intent.putExtra(INTENT_KEY_TYPE, TYPE_ARTIST_ALBUM);
				} else {
					list = MusicUtils.getSongListForAlbum(this, Long.parseLong(mCurrentAlbumId));
					content = mCurrentAlbumName;
					intent.putExtra(INTENT_KEY_TYPE, TYPE_ALBUM);
				}
				intent.putExtra(INTENT_KEY_CONTENT, content);
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

	private void doSearch() {

		CharSequence title = null;
		String query = null;

		Intent i = new Intent();
		i.setAction(MediaStore.INTENT_ACTION_MEDIA_SEARCH);
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		if (mCurrentArtistId != null) {
			title = mCurrentArtistName;
			query = mCurrentArtistName;
			i.putExtra(MediaStore.EXTRA_MEDIA_ARTIST, mCurrentArtistName);
			i.putExtra(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE);
		} else {
			if (mIsUnknownAlbum) {
				title = query = mCurrentArtistNameForAlbum;
			} else {
				title = query = mCurrentAlbumName;
				if (!mIsUnknownArtist) {
					query = query + " " + mCurrentArtistNameForAlbum;
				}
			}
			i.putExtra(MediaStore.EXTRA_MEDIA_ARTIST, mCurrentArtistNameForAlbum);
			i.putExtra(MediaStore.EXTRA_MEDIA_ALBUM, mCurrentAlbumName);
			i.putExtra(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE);
		}
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
					getArtistCursor(mArtistAdapter.getQueryHandler(), null);
				}
				break;
		}
	}

	private Cursor getArtistCursor(AsyncQueryHandler async, String filter) {

		String[] cols = new String[] { MediaStore.Audio.Artists._ID,
				MediaStore.Audio.Artists.ARTIST, MediaStore.Audio.Artists.NUMBER_OF_ALBUMS,
				MediaStore.Audio.Artists.NUMBER_OF_TRACKS };

		Uri uri = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI;
		if (!TextUtils.isEmpty(filter)) {
			uri = uri.buildUpon().appendQueryParameter("filter", Uri.encode(filter)).build();
		}

		Cursor ret = null;
		if (async != null) {
			async.startQuery(0, null, uri, cols, null, null, MediaStore.Audio.Artists.ARTIST_KEY);
		} else {
			ret = MusicUtils
					.query(this, uri, cols, null, null, MediaStore.Audio.Artists.ARTIST_KEY);
		}
		return ret;
	}

	private Cursor getAlbumCursor(AsyncQueryHandler async, String filter) {

		String[] cols = new String[] { MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ARTIST,
				MediaStore.Audio.Albums.ALBUM, MediaStore.Audio.Albums.ALBUM_ART };

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

	private class ArtistAlbumListAdapter extends SimpleCursorTreeAdapter implements SectionIndexer {

		private int mGroupArtistIdIdx;
		private int mGroupArtistIdx;
		private int mGroupAlbumIdx;
		private int mGroupSongIdx;
		private final Resources mResources;
		private final String mUnknownArtist;
		private AlphabetIndexer mIndexer;
		private ArtistAlbumBrowserActivity mActivity;
		private AsyncQueryHandler mQueryHandler;
		private String mConstraint = null;
		private boolean mConstraintIsValid = false;

		private class ViewHolderGroup {

			TextView artist_name;
			TextView album_count;
			ImageView play_indicator;
		}

		private class ViewHolderChild {

			GridView grid_view;
		}

		class QueryHandler extends AsyncQueryHandler {

			QueryHandler(ContentResolver res) {

				super(res);
			}

			@Override
			protected void onQueryComplete(int token, Object cookie, Cursor cursor) {

				// Log.i("@@@", "query complete");
				mActivity.init(cursor);
			}
		}

		private ArtistAlbumListAdapter(Context context, ArtistAlbumBrowserActivity currentactivity,
				Cursor cursor, int glayout, String[] gfrom, int[] gto, int clayout, String[] cfrom,
				int[] cto) {

			super(context, cursor, glayout, gfrom, gto, clayout, cfrom, cto);
			mActivity = currentactivity;
			mQueryHandler = new QueryHandler(context.getContentResolver());

			getColumnIndices(cursor);
			mResources = context.getResources();
			mUnknownArtist = context.getString(R.string.unknown_artist_name);
		}

		private void getColumnIndices(Cursor cursor) {

			if (cursor != null) {
				mGroupArtistIdIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists._ID);
				mGroupArtistIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST);
				mGroupAlbumIdx = cursor
						.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS);
				mGroupSongIdx = cursor
						.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_TRACKS);
				if (mIndexer != null) {
					mIndexer.setCursor(cursor);
				} else {
					mIndexer = new AlphabetIndexer(cursor, mGroupArtistIdx,
							mResources.getString(R.string.fast_scroll_alphabet));
				}
			}
		}

		public void setActivity(ArtistAlbumBrowserActivity newactivity) {

			mActivity = newactivity;
		}

		public AsyncQueryHandler getQueryHandler() {

			return mQueryHandler;
		}

		@Override
		public View newGroupView(Context context, Cursor cursor, boolean isExpanded,
				ViewGroup parent) {

			View v = super.newGroupView(context, cursor, isExpanded, parent);
			ViewHolderGroup vh = new ViewHolderGroup();
			vh.artist_name = (TextView) v.findViewById(R.id.artist_name);
			vh.album_count = (TextView) v.findViewById(R.id.album_count);
			vh.play_indicator = (ImageView) v.findViewById(R.id.play_indicator);
			v.setTag(vh);
			return v;
		}

		@Override
		public View newChildView(Context context, Cursor cursor, boolean isLastChild,
				ViewGroup parent) {

			View v = super.newChildView(context, cursor, isLastChild, parent);
			ViewHolderChild vh = new ViewHolderChild();
			vh.grid_view = (GridView) v.findViewById(R.id.grid_view);
			v.setTag(vh);
			return v;
		}

		@Override
		public void bindGroupView(View view, Context context, Cursor cursor, boolean isexpanded) {

			ViewHolderGroup vh = (ViewHolderGroup) view.getTag();

			String artist = cursor.getString(mGroupArtistIdx);
			String displayartist = artist;
			boolean unknown = artist == null || artist.equals(MediaStore.UNKNOWN_STRING);
			if (unknown) {
				displayartist = mUnknownArtist;
			}
			vh.artist_name.setText(displayartist);

			int numalbums = cursor.getInt(mGroupAlbumIdx);
			int numsongs = cursor.getInt(mGroupSongIdx);

			String songs_albums = MusicUtils.makeAlbumsLabel(context, numalbums, numsongs, unknown);

			vh.album_count.setText(songs_albums);

			long currentartistid = MusicUtils.getCurrentArtistId();
			long artistid = cursor.getLong(mGroupArtistIdIdx);
			if (currentartistid == artistid && !isexpanded) {
				vh.play_indicator.setImageResource(R.drawable.ic_indicator_nowplaying_small);
			} else {
				vh.play_indicator.setImageResource(0);
			}
		}

		@Override
		public void bindChildView(View view, Context context, Cursor cursor, boolean islast) {

			ViewHolderChild vh = (ViewHolderChild) view.getTag();

			// TODO
			vh.grid_view.setAdapter(mAlbumAdapter);
		}

		@Override
		protected Cursor getChildrenCursor(Cursor groupCursor) {

			long id = groupCursor.getLong(groupCursor
					.getColumnIndexOrThrow(MediaStore.Audio.Artists._ID));

			String[] cols = new String[] { MediaStore.Audio.Albums._ID,
					MediaStore.Audio.Albums.ALBUM, MediaStore.Audio.Albums.NUMBER_OF_SONGS,
					MediaStore.Audio.Albums.NUMBER_OF_SONGS_FOR_ARTIST,
					MediaStore.Audio.Albums.ALBUM_ART };
			Cursor c = MusicUtils.query(mActivity,
					MediaStore.Audio.Artists.Albums.getContentUri("external", id), cols, null,
					null, MediaStore.Audio.Albums.DEFAULT_SORT_ORDER);

			class MyCursorWrapper extends CursorWrapper {

				String mArtistName;
				int mMagicColumnIdx;

				MyCursorWrapper(Cursor c, String artist) {

					super(c);
					mArtistName = artist;
					if (mArtistName == null || mArtistName.equals(MediaStore.UNKNOWN_STRING)) {
						mArtistName = mUnknownArtist;
					}
					mMagicColumnIdx = c.getColumnCount();
				}

				@Override
				public String getString(int columnIndex) {

					if (columnIndex != mMagicColumnIdx) {
						return super.getString(columnIndex);
					}
					return mArtistName;
				}

				@Override
				public int getColumnIndexOrThrow(String name) {

					if (MediaStore.Audio.Albums.ARTIST.equals(name)) {
						return mMagicColumnIdx;
					}
					return super.getColumnIndexOrThrow(name);
				}

				@Override
				public String getColumnName(int idx) {

					if (idx != mMagicColumnIdx) {
						return super.getColumnName(idx);
					}
					return MediaStore.Audio.Albums.ARTIST;
				}

				@Override
				public int getColumnCount() {

					return super.getColumnCount() + 1;
				}
			}
			return new MyCursorWrapper(c, groupCursor.getString(mGroupArtistIdx));
		}

		@Override
		public void changeCursor(Cursor cursor) {

			if (mActivity.isFinishing() && cursor != null) {
				cursor.close();
				cursor = null;
			}
			if (cursor != mActivity.mArtistCursor) {
				mActivity.mArtistCursor = cursor;
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
			Cursor c = mActivity.getArtistCursor(null, s);
			mConstraint = s;
			mConstraintIsValid = true;
			return c;
		}

		public Object[] getSections() {

			return mIndexer.getSections();
		}

		public int getPositionForSection(int sectionIndex) {

			return mIndexer.getPositionForSection(sectionIndex);
		}

		public int getSectionForPosition(int position) {

			return 0;
		}
	}

	private class AlbumChildAdapter extends SimpleCursorAdapter implements SectionIndexer {

		private final BitmapDrawable mDefaultAlbumIcon;
		private int mAlbumIndex;
		private int mArtistIndex;
		private int mAlbumArtIndex;
		private final Resources mResources;
		private final String mUnknownAlbum;
		private final String mUnknownArtist;
		private AlphabetIndexer mIndexer;
		private ArtistAlbumBrowserActivity mActivity;
		private AsyncQueryHandler mQueryHandler;
		private String mConstraint = null;
		private boolean mConstraintIsValid = false;

		private class ViewHolderItem {

			TextView album_name;
			TextView artist_name;
			ImageView album_art;
		}

		private class QueryHandler extends AsyncQueryHandler {

			public QueryHandler(ContentResolver resolver) {

				super(resolver);
			}

			@Override
			protected void onQueryComplete(int token, Object cookie, Cursor cursor) {

				mActivity.init(cursor);
			}
		}

		private AlbumChildAdapter(Context context, ArtistAlbumBrowserActivity currentactivity,
				int layout, Cursor cursor, String[] from, int[] to) {

			super(context, layout, cursor, from, to);

			mActivity = currentactivity;
			mQueryHandler = new QueryHandler(context.getContentResolver());

			mUnknownAlbum = context.getString(R.string.unknown_album_name);
			mUnknownArtist = context.getString(R.string.unknown_artist_name);

			Resources r = context.getResources();

			Bitmap b = BitmapFactory.decodeResource(r, R.drawable.albumart_mp_unknown);
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
				mAlbumIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM);
				mArtistIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST);
				mAlbumArtIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM_ART);

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

			View view = super.newView(context, cursor, parent);
			ViewHolderItem mViewHolder = new ViewHolderItem();
			mViewHolder.album_name = (TextView) view.findViewById(R.id.album_name);
			mViewHolder.artist_name = (TextView) view.findViewById(R.id.artist_name);
			mViewHolder.album_art = (ImageView) view.findViewById(R.id.album_art);
			view.setTag(mViewHolder);
			return view;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {

			ViewHolderItem viewholder = (ViewHolderItem) view.getTag();

			String name = cursor.getString(mAlbumIndex);
			String displayname = name;
			boolean unknown = name == null || name.equals(MediaStore.UNKNOWN_STRING);
			if (unknown) {
				displayname = mUnknownAlbum;
			}
			viewholder.album_name.setText(displayname);

			name = cursor.getString(mArtistIndex);
			displayname = name;
			if (name == null || name.equals(MediaStore.UNKNOWN_STRING)) {
				displayname = mUnknownArtist;
			}
			viewholder.artist_name.setText(displayname);

			// We don't actually need the path to the thumbnail file,
			// we just use it to see if there is album art or not
			String art = cursor.getString(mAlbumArtIndex);
			long aid = cursor.getLong(0);
			if (unknown || art == null || art.length() == 0) {
				viewholder.album_art.setImageResource(R.drawable.albumart_mp_unknown);
			} else {
				int w = context.getResources().getDimensionPixelSize(R.dimen.gridview_bitmap_width);
				int h = context.getResources()
						.getDimensionPixelSize(R.dimen.gridview_bitmap_height);
				Bitmap bitmap = MusicUtils.getCachedArtwork(context, aid, w, h);
				viewholder.album_art.setImageBitmap(bitmap);
			}

			long currentalbumid = MusicUtils.getCurrentAlbumId();
			if (currentalbumid == aid) {
				viewholder.album_name.setCompoundDrawablesWithIntrinsicBounds(
						R.drawable.ic_indicator_nowplaying_small, 0, 0, 0);
			} else {
				viewholder.album_name.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
			}
		}

		@Override
		public void changeCursor(Cursor cursor) {

			if (mActivity.isFinishing() && cursor != null) {
				cursor.close();
				cursor = null;
			}
			if (cursor != mActivity.mAlbumCursor) {
				mActivity.mAlbumCursor = cursor;
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
			Cursor c = mActivity.getAlbumCursor(null, s);
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

	private Cursor mArtistCursor;
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
}
