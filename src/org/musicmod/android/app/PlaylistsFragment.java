package org.musicmod.android.app;

import java.util.ArrayList;
import java.util.List;

import org.musicmod.android.Constants;
import org.musicmod.android.R;
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
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class PlaylistsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,
		Constants {

	private PlaylistsAdapter mPlaylistsAdapter;
	private SmartPlaylistsAdapter mSmartPlaylistsAdapter;
	private SeparatedListAdapter mAdapter;
	private ListView mListView;
	private String mCurFilter;
	private Long[] mSmartPlaylists = new Long[] { PLAYLIST_RECENTLY_ADDED, PLAYLIST_FAVORITES,
			PLAYLIST_PODCASTS };

	int ITEM_ID = 0;
	int ITEM_ICON = 1;
	int ITEM_NAME = 2;

	private int mIdIdx, mNameIdx;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		// We have a menu item to show in action bar.
		setHasOptionsMenu(true);

		mPlaylistsAdapter = new PlaylistsAdapter(getActivity(), null, false);
		mSmartPlaylistsAdapter = new SmartPlaylistsAdapter(getActivity(),
				R.layout.playlist_list_item, mSmartPlaylists);

		mListView = (ListView) getView().findViewById(R.id.playlists_listview);

		// Prepare the loader. Either re-connect with an existing one,
		// or start a new one.
		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.playlists_browser, container, false);
		return view;
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {

		String[] cols = new String[] { MediaStore.Audio.Playlists._ID,
				MediaStore.Audio.Playlists.NAME };

		// StringBuilder where = new StringBuilder();
		// where.append(MediaStore.Audio.Media.TITLE + " != ''");

		// This is called when a new Loader needs to be created. This
		// sample only has one Loader, so we don't care about the ID.
		// First, pick the base URI to use depending on whether we are
		// currently filtering.
		Uri uri = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI;
		// if (mCurFilter != null) {
		// uri = uri.buildUpon().appendQueryParameter("filter",
		// Uri.encode(mCurFilter))
		// .build();
		// }
		// where.append(" AND " + MediaStore.Audio.Media.IS_MUSIC + "=1");

		// Now create and return a CursorLoader that will take care of
		// creating a Cursor for the data being displayed.
		return new CursorLoader(getActivity(), uri, cols, null, null,
				MediaStore.Audio.Playlists.DEFAULT_SORT_ORDER);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		// Swap the new cursor in. (The framework will take care of closing
		// the old cursor once we return.)

		mIdIdx = data.getColumnIndexOrThrow(MediaStore.Audio.Playlists._ID);
		mNameIdx = data.getColumnIndexOrThrow(MediaStore.Audio.Playlists.NAME);

		mPlaylistsAdapter.changeCursor(data);

		mAdapter = new SeparatedListAdapter(getActivity());
		mAdapter.addSection(getString(R.string.my_playlists), mPlaylistsAdapter);
		mAdapter.addSection(getString(R.string.smart_playlists), mSmartPlaylistsAdapter);

		mListView.setAdapter(mAdapter);
		// mGridView.setOnItemClickListener(this);
		// mGridView.setOnCreateContextMenuListener(this);
		mListView.setTextFilterEnabled(true);

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
		// longer using it.
		mPlaylistsAdapter.swapCursor(null);
	}

	private class PlaylistsAdapter extends CursorAdapter {

		private class ViewHolder {

			TextView playlist_name;

			public ViewHolder(View view) {
				playlist_name = (TextView) view.findViewById(R.id.playlist_name);
			}
		}

		private PlaylistsAdapter(Context context, Cursor cursor, boolean autoRequery) {
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

		}

	}

	private class SmartPlaylistsAdapter extends ArrayAdapter<Long> {

		Long[] playlists = new Long[] {};

		private SmartPlaylistsAdapter(Context context, int resid, Long[] playlists) {
			super(context, resid, playlists);
			this.playlists = playlists;
		}

		private class ViewHolder {

			TextView playlist_name;

			public ViewHolder(View view) {
				playlist_name = (TextView) view.findViewById(R.id.playlist_name);
			}
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			View view = convertView;
			ViewHolder viewholder = view != null ? (ViewHolder) view.getTag() : null;

			if (viewholder == null) {
				view = getLayoutInflater(getArguments()).inflate(R.layout.playlist_list_item, null);
				viewholder = new ViewHolder(view);
				view.setTag(viewholder);
			}

			switch (playlists[position].intValue()) {
				case (int) PLAYLIST_RECENTLY_ADDED:
					viewholder.playlist_name.setText(R.string.recently_added);
					viewholder.playlist_name.setCompoundDrawablesWithIntrinsicBounds(
							R.drawable.ic_mp_list_playlist_recent, 0, 0, 0);
					break;
				case (int) PLAYLIST_FAVORITES:
					viewholder.playlist_name.setText(R.string.favorites);
					viewholder.playlist_name.setCompoundDrawablesWithIntrinsicBounds(
							R.drawable.ic_mp_list_playlist_favorite, 0, 0, 0);
					break;
				case (int) PLAYLIST_PODCASTS:
					viewholder.playlist_name.setText(R.string.podcasts);
					viewholder.playlist_name.setCompoundDrawablesWithIntrinsicBounds(
							R.drawable.ic_mp_list_playlist_podcast, 0, 0, 0);
					break;
			}

			// viewholder.playlist_name.setCompoundDrawablesWithIntrinsicBounds(resid,
			// 0, 0, 0);

			return view;

		}
	}

}