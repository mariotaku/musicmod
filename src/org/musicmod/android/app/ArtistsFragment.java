package org.musicmod.android.app;

import org.musicmod.android.Constants;
import org.musicmod.android.R;
import org.musicmod.android.util.MusicUtils;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
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
import android.widget.CursorTreeAdapter;
import android.widget.ExpandableListView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class ArtistsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,
		Constants {

	private ArtistsAdapter mArtistsAdapter;
	private ExpandableListView mListView;
	private String mCurFilter;

	private int mGroupArtistIdIdx, mGroupArtistIdx, mGroupAlbumIdx, mGroupSongIdx;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		// We have a menu item to show in action bar.
		setHasOptionsMenu(true);

		mArtistsAdapter = new ArtistsAdapter(null, getActivity(), false);

		mListView = (ExpandableListView) getView().findViewById(R.id.artist_expandable_list);

		mListView.setAdapter(mArtistsAdapter);

		// Prepare the loader. Either re-connect with an existing one,
		// or start a new one.
		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.artist_album_browser, container, false);
		return view;
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {

		String[] cols = new String[] { MediaStore.Audio.Artists._ID,
				MediaStore.Audio.Artists.ARTIST, MediaStore.Audio.Artists.NUMBER_OF_ALBUMS,
				MediaStore.Audio.Artists.NUMBER_OF_TRACKS };

		// StringBuilder where = new StringBuilder();
		// where.append(MediaStore.Audio.Media.TITLE + " != ''");

		// This is called when a new Loader needs to be created. This
		// sample only has one Loader, so we don't care about the ID.
		// First, pick the base URI to use depending on whether we are
		// currently filtering.
		Uri uri = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI;
		// if (mCurFilter != null) {
		// uri = uri.buildUpon().appendQueryParameter("filter",
		// Uri.encode(mCurFilter))
		// .build();
		// }
		// where.append(" AND " + MediaStore.Audio.Media.IS_MUSIC + "=1");

		// Now create and return a CursorLoader that will take care of
		// creating a Cursor for the data being displayed.
		return new CursorLoader(getActivity(), uri, cols, null, null,
				MediaStore.Audio.Artists.DEFAULT_SORT_ORDER);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		// Swap the new cursor in. (The framework will take care of closing
		// the old cursor once we return.)

		mGroupArtistIdIdx = data.getColumnIndexOrThrow(MediaStore.Audio.Artists._ID);
		mGroupArtistIdx = data.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST);
		mGroupAlbumIdx = data.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS);
		mGroupSongIdx = data.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_TRACKS);

		mArtistsAdapter.changeCursor(data);

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
		mArtistsAdapter.setGroupCursor(null);
	}

	private class ArtistsAdapter extends CursorTreeAdapter {

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

			View view = getLayoutInflater(getArguments()).inflate(R.layout.artist_list_item_group, null);
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

			View view = getLayoutInflater(getArguments()).inflate(R.layout.artist_list_item_child, null);
			ViewHolderChild viewholder = new ViewHolderChild(view);
			view.setTag(viewholder);
			return view;
		}

		@Override
		public void bindChildView(View view, Context context, Cursor cursor, boolean isLastChild) {

			ViewHolderChild viewholder = (ViewHolderChild) view.getTag();
			viewholder.gridview.setAdapter(new AlbumChildAdapter(context, cursor, false));

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