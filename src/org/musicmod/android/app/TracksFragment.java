package org.musicmod.android.app;

import org.musicmod.android.R;
import org.musicmod.android.util.MusicUtils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

public class TracksFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

	private TracksAdapter mAdapter;
	private String mCurFilter;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		// We have a menu item to show in action bar.
		setHasOptionsMenu(true);

		mAdapter = new TracksAdapter(getActivity(), null, false);

		View fragmentView = getView();
		ListView mListView = (ListView) fragmentView.findViewById(R.id.tracks_listview);
		mListView.setAdapter(mAdapter);
		// mGridView.setOnItemClickListener(this);
		// mGridView.setOnCreateContextMenuListener(this);
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

	public Loader<Cursor> onCreateLoader(int id, Bundle args) {

		String[] cols = new String[] { MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE,
				MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.ALBUM,
				MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ARTIST_ID,
				MediaStore.Audio.Media.DURATION };

		StringBuilder where = new StringBuilder();
		where.append(MediaStore.Audio.Media.TITLE + " != ''");

		// This is called when a new Loader needs to be created. This
		// sample only has one Loader, so we don't care about the ID.
		// First, pick the base URI to use depending on whether we are
		// currently filtering.
		Uri baseUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
		if (mCurFilter != null) {
			baseUri = baseUri.buildUpon().appendQueryParameter("filter", Uri.encode(mCurFilter))
					.build();
		}
		where.append(" AND " + MediaStore.Audio.Media.IS_MUSIC + "=1");

		// Now create and return a CursorLoader that will take care of
		// creating a Cursor for the data being displayed.
		return new CursorLoader(getActivity(), baseUri, cols, where.toString(), null,
				MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
	}

	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		// Swap the new cursor in. (The framework will take care of closing
		// the old cursor once we return.)
		mAdapter.swapCursor(data);

		// The list should now be shown.
		// if (isResumed()) {
		// setListShown(true);
		// } else {
		// setListShownNoAnimation(true);
		// }
	}

	public void onLoaderReset(Loader<Cursor> loader) {
		// This is called when the last Cursor provided to onLoadFinished()
		// above is about to be closed. We need to make sure we are no
		// longer using it.
		mAdapter.swapCursor(null);
	}

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

			View view = LayoutInflater.from(context).inflate(R.layout.track_list_item, null);
			ViewHolder viewholder = new ViewHolder(view);
			view.setTag(viewholder);
			return view;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {

			ViewHolder viewholder = (ViewHolder) view.getTag();

			String track_name = cursor.getString(1);
			viewholder.track_name.setText(track_name);

			String artist_name = cursor.getString(4);
			if (artist_name == null || MediaStore.UNKNOWN_STRING.equals(artist_name)) {
				viewholder.artist_name.setText(R.string.unknown_artist);
			} else {
				viewholder.artist_name.setText(artist_name);
			}

			long secs = cursor.getLong(6) / 1000;

			if (secs <= 0) {
				viewholder.track_duration.setText("");
			} else {
				viewholder.track_duration.setText(MusicUtils.makeTimeString(context, secs));
			}

			long aid = cursor.getLong(0);

			long currentaudioid = MusicUtils.getCurrentAudioId();
			if (currentaudioid == aid) {
				viewholder.track_name.setCompoundDrawablesWithIntrinsicBounds(0, 0,
						R.drawable.ic_indicator_nowplaying_small, 0);
			} else {
				viewholder.track_name.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
			}

		}

	}
}