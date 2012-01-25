package org.musicmod.android.app;

import org.musicmod.android.Constants;
import org.musicmod.android.R;
import org.musicmod.android.util.MusicUtils;

import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
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
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ExpandableListView;
import android.widget.AdapterView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.ExpandableListView.OnGroupExpandListener;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SimpleCursorTreeAdapter;
import android.widget.TextView;

public class ArtistFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,
		Constants, OnGroupExpandListener {

	private ArtistsAdapter mArtistsAdapter;
	private ExpandableListView mListView;
	private int mSelectedGroupPosition, mSelectedChildPosition;
	private long mSelectedGroupId, mSelectedChildId;
	private Cursor mGroupCursor, mChildCursor;
	private String mCurrentGroupArtistName, mCurrentChildArtistNameForAlbum,
			mCurrentChildAlbumName;
	private boolean mGroupSelected, mChildSelected = false;

	private int mGroupArtistIdIdx, mGroupArtistIdx, mGroupAlbumIdx, mGroupSongIdx;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		setHasOptionsMenu(true);

		mArtistsAdapter = new ArtistsAdapter(getActivity(), null, R.layout.artist_list_item_group,
				new String[] {}, new int[] {}, R.layout.artist_list_item_child, new String[] {},
				new int[] {});
		mListView = (ExpandableListView) getView().findViewById(R.id.artist_expandable_list);
		mListView.setAdapter(mArtistsAdapter);
		mListView.setOnGroupExpandListener(this);
		mListView.setOnCreateContextMenuListener(this);

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

		if (data == null) {
			getActivity().finish();
			return;
		}

		mGroupCursor = data;

		mGroupArtistIdIdx = data.getColumnIndexOrThrow(Audio.Artists._ID);
		mGroupArtistIdx = data.getColumnIndexOrThrow(Audio.Artists.ARTIST);
		mGroupAlbumIdx = data.getColumnIndexOrThrow(Audio.Artists.NUMBER_OF_ALBUMS);
		mGroupSongIdx = data.getColumnIndexOrThrow(Audio.Artists.NUMBER_OF_TRACKS);

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

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo info) {

		ExpandableListContextMenuInfo mi = (ExpandableListContextMenuInfo) info;

		int itemtype = ExpandableListView.getPackedPositionType(mi.packedPosition);
		mSelectedGroupPosition = ExpandableListView.getPackedPositionGroup(mi.packedPosition);
		int gpos = mSelectedGroupPosition;
		mSelectedGroupId = mGroupCursor.getLong(mGroupArtistIdIdx);
		if (itemtype == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
			mGroupSelected = true;
			mChildSelected = false;
			getActivity().getMenuInflater().inflate(R.menu.music_browser_item, menu);
			if (gpos == -1) {
				// this shouldn't happen
				Log.d("Artist/Album", "no group");
				return;
			}
			gpos = gpos - mListView.getHeaderViewsCount();
			mGroupCursor.moveToPosition(gpos);
			mCurrentGroupArtistName = mGroupCursor.getString(mGroupArtistIdx);
			if (mCurrentGroupArtistName == null
					|| MediaStore.UNKNOWN_STRING.equals(mCurrentGroupArtistName)) {
				menu.setHeaderTitle(getString(R.string.unknown_artist));
				menu.findItem(R.id.search).setEnabled(false);
				menu.findItem(R.id.search).setVisible(false);
			} else {
				menu.setHeaderTitle(mCurrentGroupArtistName);
			}
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {

		if (mGroupCursor == null) return false;

		Intent intent;

		switch (item.getItemId()) {
			case PLAY_SELECTION:
				if (mGroupSelected || !mChildSelected) {
					int position = mSelectedGroupPosition;
					long[] list = MusicUtils.getSongListForArtist(getActivity(), mSelectedGroupId);
					MusicUtils.playAll(getActivity(), list, position);
				}
				return true;
			case DELETE_ITEMS:
				intent = new Intent(INTENT_DELETE_ITEMS);
				Uri data = Uri.withAppendedPath(Audio.Artists.EXTERNAL_CONTENT_URI,
						String.valueOf(mSelectedGroupId));
				intent.setData(data);
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

		if (mCurrentGroupArtistName == null
				|| MediaStore.UNKNOWN_STRING.equals(mCurrentGroupArtistName)) {
			return;
		}

		Intent i = new Intent();
		i.setAction(MediaStore.INTENT_ACTION_MEDIA_SEARCH);
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		title = mCurrentGroupArtistName;

		query = mCurrentGroupArtistName;
		i.putExtra(MediaStore.EXTRA_MEDIA_ARTIST, mCurrentGroupArtistName);
		i.putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "audio/*");
		title = getString(R.string.mediasearch, title);
		i.putExtra(SearchManager.QUERY, query);

		startActivity(Intent.createChooser(i, title));
	}

	private void showGroupDetails(int groupPosition, long id) {

		View detailsFrame = getActivity().findViewById(R.id.frame_details);
		boolean mDualPane = detailsFrame != null && detailsFrame.getVisibility() == View.VISIBLE;

		if (mDualPane) {

			mListView.setSelectedGroup(groupPosition);

			TrackFragment fragment = new TrackFragment();
			Bundle args = new Bundle();
			args.putString(INTENT_KEY_TYPE, MediaStore.Audio.Artists.CONTENT_TYPE);
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

	private class ArtistsAdapter extends SimpleCursorTreeAdapter implements OnItemClickListener,
			OnCreateContextMenuListener {

		public ArtistsAdapter(Context context, Cursor cursor, int glayout, String[] gfrom,
				int[] gto, int clayout, String[] cfrom, int[] cto) {
			super(context, cursor, glayout, gfrom, gto, clayout, cfrom, cto);
		}

		private class ViewHolderGroup {

			TextView artist_name;
			TextView album_track_count;

			public ViewHolderGroup(View view) {
				artist_name = (TextView) view.findViewById(R.id.name);
				album_track_count = (TextView) view.findViewById(R.id.summary);
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

			long id = groupCursor.getLong(groupCursor.getColumnIndexOrThrow(Audio.Artists._ID));

			String[] cols = new String[] { Audio.Albums._ID, Audio.Albums.ALBUM,
					Audio.Albums.ARTIST, Audio.Albums.NUMBER_OF_SONGS,
					Audio.Albums.NUMBER_OF_SONGS_FOR_ARTIST, Audio.Albums.ALBUM_ART };
			Uri uri = Audio.Artists.Albums.getContentUri(EXTERNAL_VOLUME, id);
			Cursor c = getActivity().getContentResolver().query(uri, cols, null, null,
					Audio.Albums.DEFAULT_SORT_ORDER);

			return c;
		}

		@Override
		public View newGroupView(Context context, Cursor cursor, boolean isExpanded,
				ViewGroup parent) {

			View view = super.newGroupView(context, cursor, isExpanded, parent);
			ViewHolderGroup viewholder = new ViewHolderGroup(view);
			view.setTag(viewholder);
			return view;
		}

		@Override
		public void bindGroupView(View view, Context context, Cursor cursor, boolean isexpanded) {

			ViewHolderGroup viewholder = (ViewHolderGroup) view.getTag();

			String artist = cursor.getString(mGroupArtistIdx);
			boolean unknown = artist == null || MediaStore.UNKNOWN_STRING.equals(artist);
			if (unknown) {
				viewholder.artist_name.setText(R.string.unknown_artist);
			} else {
				viewholder.artist_name.setText(artist);
			}

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

			View view = super.newChildView(context, cursor, isLastChild, parent);
			ViewHolderChild viewholder = new ViewHolderChild(view);
			view.setTag(viewholder);

			return view;
		}

		@Override
		public void bindChildView(View view, Context context, Cursor cursor, boolean isLastChild) {

			ViewHolderChild viewholder = (ViewHolderChild) view.getTag();
			viewholder.gridview.setAdapter(new AlbumChildAdapter(context, R.layout.album_grid_item,
					cursor, new String[] {}, new int[] {}, 0));
			viewholder.gridview.setOnItemClickListener(this);
			viewholder.gridview.setOnCreateContextMenuListener(this);

			int item_width = getResources().getDimensionPixelOffset(R.dimen.gridview_item_width);
			int item_height = getResources().getDimensionPixelOffset(R.dimen.gridview_item_height);

			int parent_width = mListView.getWidth();
			int albums_count = cursor.getCount();
			int columns_count = (int) (Math.floor(parent_width / item_width) > 0 ? Math
					.floor(parent_width / item_width) : 1);
			int gridview_rows = (int) Math.ceil((float) albums_count / columns_count);

			int default_padding = getResources().getDimensionPixelOffset(
					R.dimen.default_element_spacing);
			int paddings_sum = default_padding * (gridview_rows + 2);

			viewholder.gridview.getLayoutParams().height = item_height * gridview_rows
					+ paddings_sum;

		}

		@Override
		public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
			showDetails(position, id);
		}

		@Override
		public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo info) {

			mGroupSelected = false;
			mChildSelected = true;

			// TODO create context menu
			getActivity().getMenuInflater().inflate(R.menu.music_browser_item, menu);

			AdapterContextMenuInfo mi = (AdapterContextMenuInfo) info;
			int cpos = mi.position;
			int gpos = mSelectedGroupPosition;

			Cursor c = (Cursor) mArtistsAdapter.getChild(gpos, cpos);
			// c.moveToPosition(cpos);
			// mSelectedChildId = mi.id;
			// mCurrentChildAlbumName = c.getString(c
			// .getColumnIndexOrThrow(Audio.Albums.ALBUM));
			// gpos = gpos - mListView.getHeaderViewsCount();
			// mGroupCursor.moveToPosition(gpos);
			// mCurrentChildArtistNameForAlbum =
			// mGroupCursor.getString(mGroupCursor
			// .getColumnIndexOrThrow(Audio.Artists.ARTIST));
			// boolean mIsUnknownArtist = mCurrentChildArtistNameForAlbum ==
			// null
			// || mCurrentChildArtistNameForAlbum
			// .equals(MediaStore.UNKNOWN_STRING);
			// boolean mIsUnknownAlbum = mCurrentChildAlbumName == null
			// || mCurrentChildAlbumName.equals(MediaStore.UNKNOWN_STRING);
			// if (mIsUnknownAlbum) {
			// menu.setHeaderTitle(getString(R.string.unknown_album));
			// } else {
			// menu.setHeaderTitle(mCurrentChildAlbumName);
			// }
			// if (mIsUnknownAlbum && mIsUnknownArtist) {
			// menu.findItem(SEARCH).setVisible(false);
			// menu.findItem(SEARCH).setEnabled(false);
			// }

		}

		private void showDetails(int childPosition, long id) {

			View detailsFrame = getActivity().findViewById(R.id.frame_details);
			boolean mDualPane = detailsFrame != null
					&& detailsFrame.getVisibility() == View.VISIBLE;

			Bundle bundle = new Bundle();
			bundle.putString(INTENT_KEY_TYPE, MediaStore.Audio.Albums.CONTENT_TYPE);
			bundle.putLong(MediaStore.Audio.Albums._ID, id);

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

	}

	private class AlbumChildAdapter extends SimpleCursorAdapter {

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

		private AlbumChildAdapter(Context context, int layout, Cursor cursor, String[] from,
				int[] to, int flags) {
			super(context, layout, cursor, from, to, flags);

			mUnknownAlbum = context.getString(R.string.unknown_album);
			mUnknownArtist = context.getString(R.string.unknown_artist);

			getColumnIndices(cursor);
		}

		private void getColumnIndices(Cursor cursor) {

			if (cursor != null) {
				mAlbumIndex = cursor.getColumnIndexOrThrow(Audio.Albums.ALBUM);
				mArtistIndex = cursor.getColumnIndexOrThrow(Audio.Albums.ARTIST);
				mAlbumArtIndex = cursor.getColumnIndexOrThrow(Audio.Albums.ALBUM_ART);

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
				viewholder.album_name.setCompoundDrawablesWithIntrinsicBounds(0, 0,
						R.drawable.ic_indicator_nowplaying_small, 0);
			} else {
				viewholder.album_name.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
			}
		}

	}

}