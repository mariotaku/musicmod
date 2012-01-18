package org.musicmod.android.app;

import org.musicmod.android.Constants;
import org.musicmod.android.R;
import org.musicmod.android.util.MusicUtils;
import org.musicmod.android.widget.SeparatedListAdapter;

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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class PlaylistsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,
		Constants {

	private AlbumListAdapter mPlaylistAdapter;
	private SeparatedListAdapter mAdapter;
	private String mCurFilter;

	private int mIdIdx, mNameIdx;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		// We have a menu item to show in action bar.
		setHasOptionsMenu(true);
		
		mPlaylistAdapter = new AlbumListAdapter(getActivity(), null, false);

		mAdapter = new SeparatedListAdapter(getActivity());
		mAdapter.addSection("My Playlists", mPlaylistAdapter);
		
		View fragmentView = getView();
		ListView mListView = (ListView) fragmentView.findViewById(R.id.playlists_listview);
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
		View view = inflater.inflate(R.layout.playlists_browser, container, false);
		return view;
	}

	public Loader<Cursor> onCreateLoader(int id, Bundle args) {

		String[] cols = new String[] { MediaStore.Audio.Playlists._ID,
				MediaStore.Audio.Playlists.NAME };

		// StringBuilder where = new StringBuilder();
		// where.append(MediaStore.Audio.Media.TITLE + " != ''");

		// This is called when a new Loader needs to be created. This
		// sample only has one Loader, so we don't care about the ID.
		// First, pick the base URI to use depending on whether we are
		// currently filtering.
		Uri baseUri = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI;
		// if (mCurFilter != null) {
		// baseUri = baseUri.buildUpon().appendQueryParameter("filter",
		// Uri.encode(mCurFilter))
		// .build();
		// }
		// where.append(" AND " + MediaStore.Audio.Media.IS_MUSIC + "=1");

		// Now create and return a CursorLoader that will take care of
		// creating a Cursor for the data being displayed.
		return new CursorLoader(getActivity(), baseUri, cols, null, null,
				MediaStore.Audio.Playlists.DEFAULT_SORT_ORDER);
	}

	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		// Swap the new cursor in. (The framework will take care of closing
		// the old cursor once we return.)

		mIdIdx = data.getColumnIndexOrThrow(MediaStore.Audio.Playlists._ID);
		mNameIdx = data.getColumnIndexOrThrow(MediaStore.Audio.Playlists.NAME);
		
		mPlaylistAdapter.swapCursor(data);

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
		mPlaylistAdapter.swapCursor(null);
	}

	private class AlbumListAdapter extends CursorAdapter {

		private class ViewHolder {

			TextView playlist_name;

			public ViewHolder(View view) {
				playlist_name = (TextView) view.findViewById(R.id.playlist_name);
			}
		}

		private AlbumListAdapter(Context context, Cursor cursor, boolean autoRequery) {
			super(context, cursor, autoRequery);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			
			View view = LayoutInflater.from(context).inflate(R.layout.playlist_list_item, null);
			ViewHolder viewholder = new ViewHolder(view);
			view.setTag(viewholder);
			return view;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {

			ViewHolder viewholder = (ViewHolder) view.getTag();

			String playlist_name = cursor.getString(mNameIdx);
			viewholder.playlist_name.setText(playlist_name);

			long id = cursor.getLong(mIdIdx);

			if (id == ID_PLAYLIST_RECENT) {
				viewholder.playlist_name.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_mp_list_playlist_recent, 0, 0, 0);
			} else {
				viewholder.playlist_name.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_mp_list_playlist, 0, 0, 0);
			}

		}

	}
}