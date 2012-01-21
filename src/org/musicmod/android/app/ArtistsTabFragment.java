package org.musicmod.android.app;

import org.musicmod.android.Constants;
import org.musicmod.android.R;
import org.musicmod.android.util.MusicUtils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CursorTreeAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.AdapterView;
import android.widget.ExpandableListView.OnGroupExpandListener;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ArtistsTabFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,
		Constants, OnGroupExpandListener {

	private ArtistsAdapter mArtistsAdapter;
	private ExpandableListView mListView;

	private int mGroupArtistIdIdx, mGroupArtistIdx, mGroupAlbumIdx, mGroupSongIdx;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		setHasOptionsMenu(true);

		mArtistsAdapter = new ArtistsAdapter(null, getActivity(), false);
		mListView = (ExpandableListView) getView().findViewById(R.id.artist_expandable_list);
		mListView.setAdapter(mArtistsAdapter);
		mListView.setOnGroupExpandListener(this);

		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.artist_album_browser, container, false);
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

		String[] cols = new String[] { Audio.Artists._ID, Audio.Artists.ARTIST,
				Audio.Artists.NUMBER_OF_ALBUMS, Audio.Artists.NUMBER_OF_TRACKS };

		Uri uri = Audio.Artists.EXTERNAL_CONTENT_URI;
		return new CursorLoader(getActivity(), uri, cols, null, null,
				Audio.Artists.DEFAULT_SORT_ORDER);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

		mGroupArtistIdIdx = data.getColumnIndexOrThrow(MediaStore.Audio.Artists._ID);
		mGroupArtistIdx = data.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST);
		mGroupAlbumIdx = data.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS);
		mGroupSongIdx = data.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_TRACKS);

		mArtistsAdapter.changeCursor(data);

	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mArtistsAdapter.setGroupCursor(null);
	}

	@Override
	public void onGroupExpand(int position) {
		long id = mArtistsAdapter.getGroupId(position);
		showGroupDetails(position, id);
	}

	private void showGroupDetails(int groupPosition, long id) {

		View detailsFrame = getActivity().findViewById(R.id.frame_details);
		boolean mDualPane = detailsFrame != null && detailsFrame.getVisibility() == View.VISIBLE;

		if (mDualPane) {

			mListView.setSelectedGroup(groupPosition);

			TrackBrowserFragment fragment = new TrackBrowserFragment();
			Bundle args = new Bundle();
			args.putString(INTENT_KEY_MIMETYPE, MediaStore.Audio.Artists.CONTENT_TYPE);
			args.putLong(Audio.Artists._ID, id);

			fragment.setArguments(args);

			FragmentTransaction ft = getFragmentManager().beginTransaction();
			ft.replace(R.id.frame_details, fragment);
			ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
			ft.commit();

		}
	}

	private BroadcastReceiver mMediaStatusReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			mListView.invalidateViews();
		}

	};

	private class ArtistsAdapter extends CursorTreeAdapter implements OnItemClickListener {

		public ArtistsAdapter(Cursor cursor, Context context, boolean autoRequery) {
			super(cursor, context, autoRequery);
		}

		private class ViewHolderGroup {

			TextView artist_name;
			TextView album_track_count;

			public ViewHolderGroup(View view) {
				artist_name = (TextView) view.findViewById(R.id.artist_name);
				album_track_count = (TextView) view.findViewById(R.id.album_track_count);
			}
		}

		private class ViewHolderChild {

			GridView gridview;

			public ViewHolderChild(View view) {
				gridview = (GridView) view.findViewById(R.id.artist_child_grid_view);
			}
		}

		@Override
		public int getChildrenCount(int groupPosition) {

			return 1;
		}

		@Override
		protected Cursor getChildrenCursor(Cursor groupCursor) {

			long id = groupCursor.getLong(groupCursor
					.getColumnIndexOrThrow(MediaStore.Audio.Artists._ID));

			String[] cols = new String[] { MediaStore.Audio.Albums._ID,
					MediaStore.Audio.Albums.ALBUM, MediaStore.Audio.Albums.NUMBER_OF_SONGS,
					MediaStore.Audio.Albums.NUMBER_OF_SONGS_FOR_ARTIST,
					MediaStore.Audio.Albums.ALBUM_ART };
			Cursor c = MusicUtils.query(getActivity(),
					MediaStore.Audio.Artists.Albums.getContentUri("external", id), cols, null,
					null, MediaStore.Audio.Albums.DEFAULT_SORT_ORDER);

			class ChildCursorWrapper extends CursorWrapper {

				String mArtistName;
				int mMagicColumnIdx;

				ChildCursorWrapper(Cursor c, String artist) {

					super(c);
					mArtistName = artist;
					if (mArtistName == null || MediaStore.UNKNOWN_STRING.equals(mArtistName)) {
						mArtistName = getString(R.string.unknown_artist);
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
			return new ChildCursorWrapper(c, groupCursor.getString(mGroupArtistIdx));
		}

		@Override
		public View newGroupView(Context context, Cursor cursor, boolean isExpanded,
				ViewGroup parent) {

			View view = getLayoutInflater(getArguments()).inflate(R.layout.artist_list_item_group,
					null);
			ViewHolderGroup viewholder = new ViewHolderGroup(view);
			view.setTag(viewholder);
			return view;
		}

		@Override
		public void bindGroupView(View view, Context context, Cursor cursor, boolean isexpanded) {

			ViewHolderGroup viewholder = (ViewHolderGroup) view.getTag();

			String artist = cursor.getString(mGroupArtistIdx);
			String displayartist = artist;
			boolean unknown = artist == null || MediaStore.UNKNOWN_STRING.equals(artist);
			if (unknown) {
				viewholder.artist_name.setText(R.string.unknown_artist);
			}
			viewholder.artist_name.setText(displayartist);

			int numalbums = cursor.getInt(mGroupAlbumIdx);
			int numsongs = cursor.getInt(mGroupSongIdx);

			String songs_albums = MusicUtils.makeAlbumsLabel(context, numalbums, numsongs, unknown);

			viewholder.album_track_count.setText(songs_albums);

			long currentartistid = MusicUtils.getCurrentArtistId();
			long aid = cursor.getLong(mGroupArtistIdIdx);
			if (currentartistid == aid) {
				viewholder.artist_name.setCompoundDrawablesWithIntrinsicBounds(0, 0,
						R.drawable.ic_indicator_nowplaying_small, 0);
			} else {
				viewholder.artist_name.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
			}
		}

		@Override
		public View newChildView(Context context, Cursor cursor, boolean isLastChild,
				ViewGroup parent) {

			View view = getLayoutInflater(getArguments()).inflate(R.layout.artist_list_item_child,
					null);
			ViewHolderChild viewholder = new ViewHolderChild(view);
			view.setTag(viewholder);

			return view;
		}

		@Override
		public void bindChildView(View view, Context context, Cursor cursor, boolean isLastChild) {

			ViewHolderChild viewholder = (ViewHolderChild) view.getTag();
			viewholder.gridview.setAdapter(new AlbumChildAdapter(context, cursor, false));
			viewholder.gridview.setOnItemClickListener(this);
			viewholder.gridview.setVerticalScrollBarEnabled(false);

			int item_width = getResources().getDimensionPixelOffset(R.dimen.gridview_item_width);
			int item_height = getResources().getDimensionPixelOffset(R.dimen.gridview_item_height);

			int parent_width = mListView.getWidth();
			int albums_count = cursor.getCount();
			int columns_count = (int) Math.floor(parent_width / item_width);
			int gridview_rows = (int) Math.ceil((float) albums_count / columns_count);

			int default_padding = getResources().getDimensionPixelOffset(
					R.dimen.default_element_spacing);
			int paddings_count = default_padding * gridview_rows * 2 * 2;

			viewholder.gridview.getLayoutParams().height = item_height * gridview_rows
					+ paddings_count;

		}

		@Override
		public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
			showDetails(position, id);
		}

		private void showDetails(int childPosition, long id) {

			View detailsFrame = getActivity().findViewById(R.id.frame_details);
			boolean mDualPane = detailsFrame != null
					&& detailsFrame.getVisibility() == View.VISIBLE;

			Bundle bundle = new Bundle();
			bundle.putString(INTENT_KEY_MIMETYPE, MediaStore.Audio.Albums.CONTENT_TYPE);
			bundle.putLong(MediaStore.Audio.Albums._ID, id);

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

	}

	private class AlbumChildAdapter extends CursorAdapter {

		private int mAlbumIndex;
		private int mArtistIndex;
		private int mAlbumArtIndex;
		private final String mUnknownAlbum;
		private final String mUnknownArtist;

		private class ViewHolderItem {

			TextView album_name;
			TextView artist_name;
			ImageView album_art;

			public ViewHolderItem(View view) {
				album_name = (TextView) view.findViewById(R.id.album_name);
				artist_name = (TextView) view.findViewById(R.id.artist_name);
				album_art = (ImageView) view.findViewById(R.id.album_art);
			}
		}

		private AlbumChildAdapter(Context context, Cursor cursor, boolean autoRequery) {

			super(context, cursor, autoRequery);

			mUnknownAlbum = context.getString(R.string.unknown_album);
			mUnknownArtist = context.getString(R.string.unknown_artist);

			getColumnIndices(cursor);
		}

		private void getColumnIndices(Cursor cursor) {

			if (cursor != null) {
				mAlbumIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM);
				mArtistIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST);
				mAlbumArtIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM_ART);

			}
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {

			View view = getLayoutInflater(getArguments()).inflate(R.layout.album_grid_item, null);
			ViewHolderItem mViewHolder = new ViewHolderItem(view);
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
				viewholder.album_art.setImageResource(R.drawable.ic_mp_albumart_unknown);
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

	}

}