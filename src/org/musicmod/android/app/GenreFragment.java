package org.musicmod.android.app;

import org.musicmod.android.Constants;
import org.musicmod.android.R;
import org.musicmod.android.util.MusicUtils;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore.Audio;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

public class GenreFragment extends ListFragment implements LoaderCallbacks<Cursor>, Constants {

	private GenresAdapter mAdapter;

	private int mNameIdx;

	public GenreFragment() {

	}

	public GenreFragment(Bundle args) {
		setArguments(args);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		setHasOptionsMenu(true);

		mAdapter = new GenresAdapter(getActivity(), null, false);

		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.playlists_browser, container, false);
		return view;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putAll(getArguments() != null ? getArguments() : new Bundle());
		super.onSaveInstanceState(outState);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {

		String[] cols = new String[] { Audio.Genres._ID, Audio.Genres.NAME };

		String where = MusicUtils.getBetterGenresWhereClause(getActivity());

		Uri uri = Audio.Genres.EXTERNAL_CONTENT_URI;

		return new CursorLoader(getActivity(), uri, cols, where, null,
				Audio.Genres.DEFAULT_SORT_ORDER);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

		if (data == null) {
			getActivity().finish();
			return;
		}

		mNameIdx = data.getColumnIndexOrThrow(Audio.Genres.NAME);

		mAdapter.changeCursor(data);

		setListAdapter(mAdapter);

	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.swapCursor(null);
	}

	@Override
	public void onListItemClick(ListView listview, View view, int position, long id) {

		showDetails(position, id);
	}

	private void showDetails(int index, long id) {

		View detailsFrame = getActivity().findViewById(R.id.frame_details);
		boolean mDualPane = detailsFrame != null && detailsFrame.getVisibility() == View.VISIBLE;

		Bundle bundle = new Bundle();
		bundle.putString(INTENT_KEY_TYPE, Audio.Genres.CONTENT_TYPE);
		bundle.putLong(Audio.Genres._ID, id);

		if (mDualPane) {

			TrackFragment fragment = new TrackFragment();
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

	private class GenresAdapter extends CursorAdapter {

		private class ViewHolder {

			TextView genre_name;

			public ViewHolder(View view) {
				genre_name = (TextView) view.findViewById(R.id.playlist_name);
			}
		}

		private GenresAdapter(Context context, Cursor cursor, boolean autoRequery) {
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

			String genre_name = cursor.getString(mNameIdx);
			viewholder.genre_name.setText(MusicUtils.parseGenreName(genre_name));

		}

	}

}