package org.musicmod.android.app;

import java.util.Arrays;

import org.musicmod.android.Constants;
import org.musicmod.android.IMusicPlaybackService;
import org.musicmod.android.R;
import org.musicmod.android.util.MusicUtils;
import org.musicmod.android.util.PreferencesEditor;

import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.AbstractCursor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio;
import android.provider.MediaStore.Audio.Playlists;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItem;
import android.support.v4.widget.CursorAdapter;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.TextView;

public class TrackBrowserFragment extends ListFragment implements LoaderCallbacks<Cursor>,
		Constants {

	private TracksAdapter mAdapter;
	private Cursor mCursor;
	private int mSelectedPosition;
	private long mSelectedId;
	private String mCurrentTrackName, mCurrentAlbumName, mCurrentArtistNameForAlbum;
	private int mIdIdx, mTrackIdx, mAlbumIdx, mArtistIdx, mDurationIdx;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		// We have a menu item to show in action bar.
		setHasOptionsMenu(true);

		mAdapter = new TracksAdapter(getActivity(), null, false);

		setListAdapter(mAdapter);

		getListView().setOnCreateContextMenuListener(this);

		// Prepare the loader. Either re-connect with an existing one,
		// or start a new one.
		getLoaderManager().initLoader(0, null, this);

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.tracks_browser, container, false);
		return view;
	}

	@Override
	public void onStart() {
		super.onStart();

		IntentFilter filter = new IntentFilter();
		filter.addAction(BROADCAST_META_CHANGED);
		filter.addAction(BROADCAST_QUEUE_CHANGED);
		getActivity().registerReceiver(mMediaStatusReceiver, filter);
	}

	@Override
	public void onStop() {
		getActivity().unregisterReceiver(mMediaStatusReceiver);
		super.onStop();
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {

		String[] cols = new String[] { Audio.Media._ID, Audio.Media.TITLE, Audio.Media.DATA,
				Audio.Media.ALBUM, Audio.Media.ARTIST, Audio.Media.ARTIST_ID, Audio.Media.DURATION };

		StringBuilder where = new StringBuilder();

		where.append(Audio.Media.IS_MUSIC + "=1");
		where.append(" AND " + Audio.Media.TITLE + " != ''");

		Uri uri = Audio.Media.EXTERNAL_CONTENT_URI;

		if (getArguments() != null) {

			String mimeType = getArguments().getString(INTENT_KEY_MIMETYPE);

			if (Audio.Playlists.CONTENT_TYPE.equals(mimeType)) {
				long playlist_id = getArguments().getLong(Audio.Playlists._ID);

				where = new StringBuilder();
				where.append(Playlists.Members.IS_MUSIC + "=1");
				where.append(" AND " + Playlists.Members.TITLE + " != ''");

				switch ((int) playlist_id) {
					case (int) PLAYLIST_QUEUE:

						uri = Audio.Media.EXTERNAL_CONTENT_URI;
						long[] mNowPlaying = MusicUtils.getQueue();
						if (mNowPlaying.length == 0) {
							return null;
						}
						where = new StringBuilder();
						where.append(MediaStore.Audio.Media._ID + " IN (");
						for (int i = 0; i < mNowPlaying.length; i++) {
							where.append(mNowPlaying[i]);
							if (i < mNowPlaying.length - 1) {
								where.append(",");
							}
						}
						where.append(")");

						break;
					case (int) PLAYLIST_FAVORITES:
						String favorites_where = Audio.Playlists.NAME + "='"
								+ PLAYLIST_NAME_FAVORITES + "'";
						String[] favorites_cols = new String[] { Audio.Playlists._ID };
						Uri favorites_uri = Audio.Playlists.EXTERNAL_CONTENT_URI;
						Cursor cursor = getActivity().getContentResolver().query(favorites_uri,
								favorites_cols, favorites_where, null, null);
						if (cursor.getCount() <= 0) {
							return null;
						}
						cursor.moveToFirst();
						long favorites_id = cursor.getLong(0);
						cursor.close();

						cols = new String[] { Playlists.Members._ID, Playlists.Members.AUDIO_ID,
								Playlists.Members.TITLE, Playlists.Members.ALBUM,
								Playlists.Members.ARTIST, Playlists.Members.DURATION };
						uri = Playlists.Members.getContentUri(EXTERNAL_VOLUME, favorites_id);
						break;
					case (int) PLAYLIST_RECENTLY_ADDED:
						int X = new PreferencesEditor(getActivity()).getIntPref(PREF_KEY_NUMWEEKS,
								2) * (3600 * 24 * 7);
						where = new StringBuilder();
						where.append(MediaStore.Audio.Media.TITLE + " != ''");
						where.append(" AND " + MediaStore.MediaColumns.DATE_ADDED + ">");
						where.append(System.currentTimeMillis() / 1000 - X);
						break;
					case (int) PLAYLIST_PODCASTS:
						where = new StringBuilder();
						where.append(Audio.Media.TITLE + " != ''");
						where.append(" AND " + Audio.Media.IS_PODCAST + "=1");
						break;
					default:
						if (id < 0) return null;
						cols = new String[] { Playlists.Members._ID, Playlists.Members.AUDIO_ID,
								Playlists.Members.TITLE, Playlists.Members.ALBUM,
								Playlists.Members.ARTIST, Playlists.Members.DURATION };

						uri = Playlists.Members.getContentUri(EXTERNAL_VOLUME, playlist_id);
						break;
				}

			} else {

				if (Audio.Albums.CONTENT_TYPE.equals(mimeType)) {
					long album_id = getArguments().getLong(Audio.Albums._ID);
					where.append(" AND " + Audio.Media.ALBUM_ID + "=" + album_id);
				} else if (Audio.Artists.CONTENT_TYPE.equals(mimeType)) {
					long artist_id = getArguments().getLong(Audio.Artists._ID);
					where.append(" AND " + Audio.Media.ARTIST_ID + "=" + artist_id);
				}

			}

		}

		// Now create and return a CursorLoader that will take care of
		// creating a Cursor for the data being displayed.
		return new CursorLoader(getActivity(), uri, cols, where.toString(), null, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

		mCursor = data;
		if (getArguments() != null
				&& Playlists.CONTENT_TYPE.equals(getArguments().getString(INTENT_KEY_MIMETYPE))
				&& getArguments().getLong(Playlists._ID) != PLAYLIST_QUEUE
				&& getArguments().getLong(Playlists._ID) != PLAYLIST_PODCASTS
				&& getArguments().getLong(Playlists._ID) != PLAYLIST_RECENTLY_ADDED){
			mIdIdx = data.getColumnIndexOrThrow(Playlists.Members.AUDIO_ID);
			mTrackIdx = data.getColumnIndexOrThrow(Playlists.Members.TITLE);
			mAlbumIdx = data.getColumnIndexOrThrow(Playlists.Members.ALBUM);
			mArtistIdx = data.getColumnIndexOrThrow(Playlists.Members.ARTIST);
			mDurationIdx = data.getColumnIndexOrThrow(Playlists.Members.DURATION);
		} else {
			mIdIdx = data.getColumnIndexOrThrow(Audio.Media._ID);
			mTrackIdx = data.getColumnIndexOrThrow(Audio.Media.TITLE);
			mAlbumIdx = data.getColumnIndexOrThrow(Audio.Media.ALBUM);
			mArtistIdx = data.getColumnIndexOrThrow(Audio.Media.ARTIST);
			mDurationIdx = data.getColumnIndexOrThrow(Audio.Media.DURATION);
		}

		mAdapter.swapCursor(data);

	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mCursor = null;
		mAdapter.swapCursor(null);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		if (mCursor == null || mCursor.getCount() == 0) return;
		// When selecting a track from the queue, just jump there instead of
		// reloading the queue. This is both faster, and prevents accidentally
		// dropping out of party shuffle.
		long[] list_for_cursor = MusicUtils.getSongListForCursor(mCursor);
		long[] queue = MusicUtils.getQueue();
		
		if (Arrays.equals(list_for_cursor, queue)) {
			MusicUtils.setQueuePosition(position);
			return;
		}
		MusicUtils.playAll(getActivity(), mCursor, position);

	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo info) {

		if (mCursor == null) return;

		getActivity().getMenuInflater().inflate(R.menu.music_browser_item, menu);

		AdapterContextMenuInfo adapterinfo = (AdapterContextMenuInfo) info;
		mSelectedPosition = adapterinfo.position;
		mCursor.moveToPosition(mSelectedPosition);
		try {
			mSelectedId = mCursor.getLong(mIdIdx);
		} catch (IllegalArgumentException ex) {
			mSelectedId = adapterinfo.id;
		}

		mCurrentAlbumName = mCursor.getString(mAlbumIdx);
		mCurrentArtistNameForAlbum = mCursor.getString(mArtistIdx);
		mCurrentTrackName = mCursor.getString(mTrackIdx);
		menu.setHeaderTitle(mCurrentTrackName);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {

		if (mCursor == null) return false;

		Intent intent;

		switch (item.getItemId()) {
			case PLAY_SELECTION:
				int position = mSelectedPosition;
				long[] list_for_cursor = MusicUtils.getSongListForCursor(mCursor);
				long[] queue = MusicUtils.getQueue();
				
				if (Arrays.equals(list_for_cursor, queue)) {
					MusicUtils.setQueuePosition(position);
					return true;
				}
				MusicUtils.playAll(getActivity(), mCursor, position);
				return true;
			case DELETE_ITEMS:
				intent = new Intent(INTENT_DELETE_ITEMS);
				Uri data = Uri.withAppendedPath(Audio.Media.EXTERNAL_CONTENT_URI,
						String.valueOf(mSelectedId));
				intent.setData(data);
				return true;
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

		title = mCurrentTrackName;
		if (MediaStore.UNKNOWN_STRING.equals(mCurrentArtistNameForAlbum)) {
			query = mCurrentTrackName;
		} else {
			query = mCurrentArtistNameForAlbum + " " + mCurrentTrackName;
			i.putExtra(MediaStore.EXTRA_MEDIA_ARTIST, mCurrentArtistNameForAlbum);
		}
		if (MediaStore.UNKNOWN_STRING.equals(mCurrentAlbumName)) {
			i.putExtra(MediaStore.EXTRA_MEDIA_ALBUM, mCurrentAlbumName);
		}
		i.putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "audio/*");
		title = getString(R.string.mediasearch, title);
		i.putExtra(SearchManager.QUERY, query);

		startActivity(Intent.createChooser(i, title));
	}

	private BroadcastReceiver mMediaStatusReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			getListView().invalidateViews();
		}

	};

	private class TracksAdapter extends CursorAdapter {

		private class ViewHolder {

			TextView track_name;
			TextView artist_name;
			TextView track_duration;

			public ViewHolder(View view) {
				track_name = (TextView) view.findViewById(R.id.track_name);
				artist_name = (TextView) view.findViewById(R.id.artist_name);
				track_duration = (TextView) view.findViewById(R.id.track_duration);
			}

		}

		private TracksAdapter(Context context, Cursor cursor, boolean autoRequery) {
			super(context, cursor, autoRequery);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {

			View view = getLayoutInflater(getArguments()).inflate(R.layout.track_list_item, null);
			ViewHolder viewholder = new ViewHolder(view);
			view.setTag(viewholder);
			return view;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {

			ViewHolder viewholder = (ViewHolder) view.getTag();

			String track_name = cursor.getString(mTrackIdx);
			viewholder.track_name.setText(track_name);

			String artist_name = cursor.getString(mArtistIdx);
			if (artist_name == null || MediaStore.UNKNOWN_STRING.equals(artist_name)) {
				viewholder.artist_name.setText(R.string.unknown_artist);
			} else {
				viewholder.artist_name.setText(artist_name);
			}

			long secs = cursor.getLong(mDurationIdx) / 1000;

			if (secs <= 0) {
				viewholder.track_duration.setText("");
			} else {
				viewholder.track_duration.setText(MusicUtils.makeTimeString(context, secs));
			}

			long audio_id = cursor.getLong(mIdIdx);

			long currentaudioid = MusicUtils.getCurrentAudioId();
			if (currentaudioid == audio_id) {
				viewholder.track_name.setCompoundDrawablesWithIntrinsicBounds(0, 0,
						R.drawable.ic_indicator_nowplaying_small, 0);
			} else {
				viewholder.track_name.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
			}

		}

	}

}