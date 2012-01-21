package org.musicmod.android.app;

import org.musicmod.android.Constants;
import org.musicmod.android.R;
import org.musicmod.android.widget.SeparatedListAdapter;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
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

public class PlaylistsTabFragment extends ListFragment implements
		LoaderManager.LoaderCallbacks<Cursor>, Constants {

	private PlaylistsAdapter mPlaylistsAdapter;
	private SmartPlaylistsAdapter mSmartPlaylistsAdapter;
	private SeparatedListAdapter mAdapter;
	private Long[] mSmartPlaylists = new Long[] { PLAYLIST_FAVORITES, PLAYLIST_RECENTLY_ADDED,
			PLAYLIST_PODCASTS };

	private int mIdIdx, mNameIdx;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		setHasOptionsMenu(true);

		mPlaylistsAdapter = new PlaylistsAdapter(getActivity(), null, false);
		mSmartPlaylistsAdapter = new SmartPlaylistsAdapter(getActivity(),
				R.layout.playlist_list_item, mSmartPlaylists);

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

		Uri uri = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI;

		StringBuilder where = new StringBuilder();

		where.append(MediaStore.Audio.Playlists.NAME + " != '" + PLAYLIST_NAME_FAVORITES + "'");

		return new CursorLoader(getActivity(), uri, cols, where.toString(), null,
				MediaStore.Audio.Playlists.DEFAULT_SORT_ORDER);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

		mIdIdx = data.getColumnIndexOrThrow(MediaStore.Audio.Playlists._ID);
		mNameIdx = data.getColumnIndexOrThrow(MediaStore.Audio.Playlists.NAME);

		mPlaylistsAdapter.changeCursor(data);

		mAdapter = new SeparatedListAdapter(getActivity());
		mAdapter.addSection(getString(R.string.my_playlists), mPlaylistsAdapter);
		mAdapter.addSection(getString(R.string.smart_playlists), mSmartPlaylistsAdapter);

		setListAdapter(mAdapter);

	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mPlaylistsAdapter.swapCursor(null);
	}

	@Override
	public void onListItemClick(ListView listview, View view, int position, long id) {

		long playlist_id = (Long) ((Object[]) view.getTag())[1];

		showDetails(position, playlist_id);
	}

	private void showDetails(int index, long id) {

		View detailsFrame = getActivity().findViewById(R.id.frame_details);
		boolean mDualPane = detailsFrame != null && detailsFrame.getVisibility() == View.VISIBLE;

		long playlist_id = id;

		Bundle bundle = new Bundle();
		bundle.putString(INTENT_KEY_MIMETYPE, MediaStore.Audio.Playlists.CONTENT_TYPE);
		bundle.putLong(MediaStore.Audio.Playlists._ID, playlist_id);

		if (mDualPane) {

			TrackBrowserFragment fragment = new TrackBrowserFragment();
			fragment.setArguments(bundle);

			FragmentTransaction ft = getFragmentManager().beginTransaction();
			ft.replace(R.id.frame_details, fragment);
			ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
			ft.commit();

		} else {

			Intent intent = new Intent(getActivity(), TrackBrowserActivity.class);
			intent.putExtras(bundle);
			startActivity(intent);
		}
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
			view.setTag(new Object[] { viewholder, cursor.getLong(mIdIdx) });
			return view;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {

			ViewHolder viewholder = (ViewHolder) ((Object[]) view.getTag())[0];

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
			ViewHolder viewholder = view != null ? (ViewHolder) ((Object[]) view.getTag())[0]
					: null;

			if (viewholder == null) {
				view = getLayoutInflater(getArguments()).inflate(R.layout.playlist_list_item, null);
				viewholder = new ViewHolder(view);
				view.setTag(new Object[] { viewholder, playlists[position] });
			}

			switch (playlists[position].intValue()) {
				case (int) PLAYLIST_FAVORITES:
					viewholder.playlist_name.setText(R.string.favorites);
					viewholder.playlist_name.setCompoundDrawablesWithIntrinsicBounds(
							R.drawable.ic_mp_list_playlist_favorite, 0, 0, 0);
					break;
				case (int) PLAYLIST_RECENTLY_ADDED:
					viewholder.playlist_name.setText(R.string.recently_added);
					viewholder.playlist_name.setCompoundDrawablesWithIntrinsicBounds(
							R.drawable.ic_mp_list_playlist_recent, 0, 0, 0);
					break;
				case (int) PLAYLIST_PODCASTS:
					viewholder.playlist_name.setText(R.string.podcasts);
					viewholder.playlist_name.setCompoundDrawablesWithIntrinsicBounds(
							R.drawable.ic_mp_list_playlist_podcast, 0, 0, 0);
					break;
			}

			return view;

		}
	}

}