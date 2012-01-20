package org.musicmod.android.app;

import java.util.Arrays;

import org.musicmod.android.Constants;
import org.musicmod.android.IMusicPlaybackService;
import org.musicmod.android.R;
import org.musicmod.android.util.MusicUtils;

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
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItem;
import android.support.v4.widget.CursorAdapter;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

public class TrackBrowserFragment extends Fragment implements
		LoaderManager.LoaderCallbacks<Cursor>, OnItemClickListener, Constants {

	private TracksAdapter mAdapter;
	private ListView mListView;
	private Cursor mTrackCursor;
	private int mSelectedPosition;
	private long mSelectedId;
	private String mCurrentTrackName, mCurrentAlbumName, mCurrentArtistNameForAlbum;
	private int mAudioIdIdx, mTrackIdx, mArtistIdx, mDurationIdx;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		// We have a menu item to show in action bar.
		setHasOptionsMenu(true);

		mAdapter = new TracksAdapter(getActivity(), null, false);

		View fragmentView = getView();
		mListView = (ListView) fragmentView.findViewById(R.id.tracks_listview);
		mListView.setAdapter(mAdapter);
		mListView.setOnItemClickListener(this);
		mListView.setOnCreateContextMenuListener(this);
		mListView.setTextFilterEnabled(true);

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
		String sort_order = Audio.Media.DEFAULT_SORT_ORDER;

		StringBuilder where = new StringBuilder();

		// This is called when a new Loader needs to be created. This
		// sample only has one Loader, so we don't care about the ID.
		// First, pick the base URI to use depending on whether we are
		// currently filtering.
		Uri uri = Audio.Media.EXTERNAL_CONTENT_URI;

		if (getArguments() != null) {

			String mimeType = getArguments().getString(INTENT_KEY_MIMETYPE);

			if (Audio.Playlists.CONTENT_TYPE.equals(mimeType)) {
				long playlist_id = getArguments().getLong(Audio.Playlists._ID);
				cols = new String[] { Playlists.Members._ID, Playlists.Members.AUDIO_ID,
						Playlists.Members.TITLE, Playlists.Members.ARTIST,
						Playlists.Members.DURATION };
				uri = Playlists.Members.getContentUri(EXTERNAL_VOLUME, playlist_id);
				sort_order = Playlists.Members.DEFAULT_SORT_ORDER;
				where.append(Playlists.Members.IS_MUSIC + "=1");
			} else {
				where.append(Audio.Media.TITLE + " != ''");
				where.append(" AND " + Audio.Media.IS_MUSIC + "=1");
			}

			if (Audio.Albums.CONTENT_TYPE.equals(mimeType)) {
				long album_id = getArguments().getLong(Audio.Albums._ID);
				where.append(" AND " + Audio.Media.ALBUM_ID + "=" + album_id);
			}

			if (Audio.Artists.CONTENT_TYPE.equals(mimeType)) {
				long artist_id = getArguments().getLong(Audio.Artists._ID);
				where.append(" AND " + Audio.Media.ARTIST_ID + "=" + artist_id);
			}

		} else {
			where.append(Audio.Media.IS_MUSIC + "=1");
		}

		// Now create and return a CursorLoader that will take care of
		// creating a Cursor for the data being displayed.
		return new CursorLoader(getActivity(), uri, cols, where.toString(), null, sort_order);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

		mTrackCursor = data;
		if (getArguments() != null
				&& Playlists.CONTENT_TYPE.equals(getArguments().getString(INTENT_KEY_MIMETYPE))) {
			mAudioIdIdx = data.getColumnIndexOrThrow(Playlists.Members.AUDIO_ID);
			mTrackIdx = data.getColumnIndexOrThrow(Playlists.Members.TITLE);
			mArtistIdx = data.getColumnIndexOrThrow(Playlists.Members.ARTIST);
			mDurationIdx = data.getColumnIndexOrThrow(Playlists.Members.DURATION);
		} else {
			mAudioIdIdx = data.getColumnIndexOrThrow(Audio.Media._ID);
			mTrackIdx = data.getColumnIndexOrThrow(Audio.Media.TITLE);
			mArtistIdx = data.getColumnIndexOrThrow(Audio.Media.ARTIST);
			mDurationIdx = data.getColumnIndexOrThrow(Audio.Media.DURATION);
		}

		mAdapter.swapCursor(data);

		// The list should now be shown.
		// if (isResumed()) {
		// setListShown(true);
		// } else {
		// setListShownNoAnimation(true);
		// }
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		// This is called when the last Cursor provided to onLoadFinished()
		// above is about to be closed. We need to make sure we are no
		mTrackCursor = null;
		mAdapter.swapCursor(null);
	}

	@Override
	public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
		if (mTrackCursor == null || mTrackCursor.getCount() == 0) return;
		// When selecting a track from the queue, just jump there instead of
		// reloading the queue. This is both faster, and prevents accidentally
		// dropping out of party shuffle.
		if (mTrackCursor instanceof NowPlayingCursor) {
			MusicUtils.setQueuePosition(position);
		}
		MusicUtils.playAll(getActivity(), mTrackCursor, position);

	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo info) {

		if (mTrackCursor == null) return;

		getActivity().getMenuInflater().inflate(R.menu.track_list, menu);

		AdapterContextMenuInfo adapterinfo = (AdapterContextMenuInfo) info;
		mSelectedPosition = adapterinfo.position;
		mTrackCursor.moveToPosition(mSelectedPosition);
		try {
			mSelectedId = mTrackCursor.getLong(mAudioIdIdx);
		} catch (IllegalArgumentException ex) {
			mSelectedId = adapterinfo.id;
		}

		mCurrentAlbumName = mTrackCursor.getString(mTrackCursor
				.getColumnIndexOrThrow(Audio.Media.ALBUM));
		mCurrentArtistNameForAlbum = mTrackCursor.getString(mTrackCursor
				.getColumnIndexOrThrow(Audio.Media.ARTIST));
		mCurrentTrackName = mTrackCursor.getString(mTrackCursor
				.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
		menu.setHeaderTitle(mCurrentTrackName);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {

		if (mTrackCursor == null) return false;

		Intent intent;

		switch (item.getItemId()) {
			case PLAY_SELECTION:
				int position = mSelectedPosition;
				if (mTrackCursor instanceof NowPlayingCursor) {
					MusicUtils.setQueuePosition(position);
				}
				MusicUtils.playAll(getActivity(), mTrackCursor, position);
				return true;
			case DELETE_ITEMS:
				final long[] list = new long[1];
				list[0] = (int) mSelectedId;
				intent = new Intent(INTENT_DELETE_ITEMS);
				intent.putExtra(INTENT_KEY_CONTENT, mCurrentTrackName);
				intent.setType(Audio.Media.CONTENT_TYPE);
				intent.putExtra(INTENT_KEY_ITEMS, list);
				startActivity(intent);
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
			mListView.invalidateViews();
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

			long audio_id = cursor.getLong(mAudioIdIdx);

			long currentaudioid = MusicUtils.getCurrentAudioId();
			if (currentaudioid == audio_id) {
				viewholder.track_name.setCompoundDrawablesWithIntrinsicBounds(0, 0,
						R.drawable.ic_indicator_nowplaying_small, 0);
			} else {
				viewholder.track_name.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
			}

		}

	}

	private class NowPlayingCursor extends AbstractCursor {

		private void makeNowPlayingCursor() {

			mCurrentPlaylistCursor = null;
			try {
				mNowPlaying = mService.getQueue();
			} catch (RemoteException ex) {
				mNowPlaying = new long[0];
			}
			mSize = mNowPlaying.length;
			if (mSize == 0) {
				return;
			}

			StringBuilder where = new StringBuilder();
			where.append(MediaStore.Audio.Media._ID + " IN (");
			for (int i = 0; i < mSize; i++) {
				where.append(mNowPlaying[i]);
				if (i < mSize - 1) {
					where.append(",");
				}
			}
			where.append(")");

			mCurrentPlaylistCursor = MusicUtils.query(getActivity(),
					MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mCols, where.toString(), null,
					MediaStore.Audio.Media._ID);

			if (mCurrentPlaylistCursor == null) {
				mSize = 0;
				return;
			}

			int size = mCurrentPlaylistCursor.getCount();
			mCursorIdxs = new long[size];
			mCurrentPlaylistCursor.moveToFirst();
			int colidx = mCurrentPlaylistCursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
			for (int i = 0; i < size; i++) {
				mCursorIdxs[i] = mCurrentPlaylistCursor.getLong(colidx);
				mCurrentPlaylistCursor.moveToNext();
			}
			mCurrentPlaylistCursor.moveToFirst();

			// At this point we can verify the 'now playing' list we got
			// earlier to make sure that all the items in there still exist
			// in the database, and remove those that aren't. This way we
			// don't get any blank items in the list.
			try {
				int removed = 0;
				for (int i = mNowPlaying.length - 1; i >= 0; i--) {
					long trackid = mNowPlaying[i];
					int crsridx = Arrays.binarySearch(mCursorIdxs, trackid);
					if (crsridx < 0) {
						// Log.i("@@@@@", "item no longer exists in db: " +
						// trackid);
						removed += mService.removeTrack(trackid);
					}
				}
				if (removed > 0) {
					mNowPlaying = mService.getQueue();
					mSize = mNowPlaying.length;
					if (mSize == 0) {
						mCursorIdxs = null;
						return;
					}
				}
			} catch (RemoteException ex) {
				mNowPlaying = new long[0];
			}
		}

		@Override
		public int getCount() {

			return mSize;
		}

		@Override
		public boolean onMove(int oldPosition, int newPosition) {

			if (oldPosition == newPosition) return true;

			if (mNowPlaying == null || mCursorIdxs == null || newPosition >= mNowPlaying.length) {
				return false;
			}

			long newid = mNowPlaying[newPosition];
			int crsridx = Arrays.binarySearch(mCursorIdxs, newid);
			mCurrentPlaylistCursor.moveToPosition(crsridx);

			return true;
		}

		@Override
		public String getString(int column) {

			try {
				return mCurrentPlaylistCursor.getString(column);
			} catch (Exception ex) {
				onChange(true);
				return "";
			}
		}

		@Override
		public short getShort(int column) {

			return mCurrentPlaylistCursor.getShort(column);
		}

		@Override
		public int getInt(int column) {

			try {
				return mCurrentPlaylistCursor.getInt(column);
			} catch (Exception ex) {
				onChange(true);
				return 0;
			}
		}

		@Override
		public long getLong(int column) {

			try {
				return mCurrentPlaylistCursor.getLong(column);
			} catch (Exception ex) {
				onChange(true);
				return 0;
			}
		}

		@Override
		public float getFloat(int column) {

			return mCurrentPlaylistCursor.getFloat(column);
		}

		@Override
		public double getDouble(int column) {

			return mCurrentPlaylistCursor.getDouble(column);
		}

		@Override
		public boolean isNull(int column) {

			return mCurrentPlaylistCursor.isNull(column);
		}

		@Override
		public String[] getColumnNames() {

			return mCols;
		}

		@Override
		public void deactivate() {

			if (mCurrentPlaylistCursor != null) mCurrentPlaylistCursor.deactivate();
		}

		@Override
		public boolean requery() {

			makeNowPlayingCursor();
			return true;
		}

		private String[] mCols;
		private Cursor mCurrentPlaylistCursor; // updated in onMove
		private int mSize; // size of the queue
		private long[] mNowPlaying;
		private long[] mCursorIdxs;
		private IMusicPlaybackService mService;
	}

}