/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.musicmod.android.app;

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.musicmod.android.Constants;
import org.musicmod.android.R;
import org.musicmod.android.util.MusicUtils;

public class QueryBrowserFragment extends ListFragment implements Constants,
		LoaderCallbacks<Cursor> {

	private QueryListAdapter mAdapter;
	private String mFilterString = "";
	private Cursor mQueryCursor;
	private ListView mTrackList;

	public QueryBrowserFragment() {

	}

	public QueryBrowserFragment(Bundle arguments) {
		setArguments(arguments);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		// We have a menu item to show in action bar.
		setHasOptionsMenu(true);

		mAdapter = new QueryListAdapter(getActivity(), null, false);

		setListAdapter(mAdapter);

		getListView().setOnCreateContextMenuListener(this);

		// Prepare the loader. Either re-connect with an existing one,
		// or start a new one.
		getLoaderManager().initLoader(0, getArguments(), this);

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.query_browser, container, false);
		return view;
	}

	public void onServiceConnected(ComponentName name, IBinder service) {

		Bundle bundle = getArguments();

		String action = bundle != null ? bundle.getString(INTENT_KEY_ACTION) : null;
		String data = bundle != null ? bundle.getString(INTENT_KEY_DATA) : null;

		if (Intent.ACTION_VIEW.equals(action)) {
			// this is something we got from the search bar
			Uri uri = Uri.parse(data);
			if (data.startsWith("content://media/external/audio/media/")) {
				// This is a specific file
				String id = uri.getLastPathSegment();
				long[] list = new long[] { Long.valueOf(id) };
				MusicUtils.playAll(getActivity(), list, 0);
				getActivity().finish();
				return;
			} else if (data.startsWith("content://media/external/audio/albums/")) {
				// This is an album, show the songs on it
				Intent i = new Intent(Intent.ACTION_PICK);
				i.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/track");
				i.putExtra("album", uri.getLastPathSegment());
				startActivity(i);
				return;
			} else if (data.startsWith("content://media/external/audio/artists/")) {
				// This is an artist, show the albums for that artist
				Intent i = new Intent(Intent.ACTION_PICK);
				i.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/album");
				i.putExtra("artist", uri.getLastPathSegment());
				startActivity(i);
				return;
			}
		}

		mFilterString = bundle != null ? bundle.getString(SearchManager.QUERY) : null;
		if (MediaStore.INTENT_ACTION_MEDIA_SEARCH.equals(action)) {
			String focus = bundle != null ? bundle.getString(MediaStore.EXTRA_MEDIA_FOCUS) : null;
			String artist = bundle != null ? bundle.getString(MediaStore.EXTRA_MEDIA_ARTIST) : null;
			String album = bundle != null ? bundle.getString(MediaStore.EXTRA_MEDIA_ALBUM) : null;
			String title = bundle != null ? bundle.getString(MediaStore.EXTRA_MEDIA_TITLE) : null;
			if (focus != null) {
				if (focus.startsWith("audio/") && title != null) {
					mFilterString = title;
				} else if (Audio.Albums.ENTRY_CONTENT_TYPE.equals(focus)) {
					if (album != null) {
						mFilterString = album;
						if (artist != null) {
							mFilterString = mFilterString + " " + artist;
						}
					}
				} else if (Audio.Artists.ENTRY_CONTENT_TYPE.equals(focus)) {
					if (artist != null) {
						mFilterString = artist;
					}
				}
			}
		}

		mTrackList = getListView();
		mTrackList.setTextFilterEnabled(true);
	}

	public void onServiceDisconnected(ComponentName name) {

	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {

		String filter = args.getString(INTENT_KEY_FILTER);

		String[] cols = new String[] {
				BaseColumns._ID, // this will be the artist, album or track ID
				MediaStore.Audio.Media.MIME_TYPE, // mimetype of audio file, or
													// "artist" or "album"
				MediaStore.Audio.Artists.ARTIST, MediaStore.Audio.Albums.ALBUM,
				MediaStore.Audio.Media.TITLE, "data1", "data2" };

		Uri uri = Uri.parse("content://media/external/audio/search/fancy/" + Uri.encode(filter));

		// Now create and return a CursorLoader that will take care of
		// creating a Cursor for the data being displayed.
		return new CursorLoader(getActivity(), uri, cols, null, null, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

		mQueryCursor = data;
		mAdapter.swapCursor(data);

	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.swapCursor(null);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {

		// Dialog doesn't allow us to wait for a result, so we need to store
		// the info we need for when the dialog posts its result
		mQueryCursor.moveToPosition(position);
		if (mQueryCursor.isBeforeFirst() || mQueryCursor.isAfterLast()) {
			return;
		}
		String selectedType = mQueryCursor.getString(mQueryCursor
				.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE));

		if ("artist".equals(selectedType)) {
			Intent intent = new Intent(Intent.ACTION_PICK);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			intent.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/album");
			intent.putExtra("artist", Long.valueOf(id).toString());
			startActivity(intent);
		} else if ("album".equals(selectedType)) {
			Intent intent = new Intent(Intent.ACTION_PICK);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			intent.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/track");
			intent.putExtra("album", Long.valueOf(id).toString());
			startActivity(intent);
		} else if (position >= 0 && id >= 0) {
			long[] list = new long[] { id };
			MusicUtils.playAll(getActivity(), list, 0);
		} else {
			Log.e("QueryBrowser", "invalid position/id: " + position + "/" + id);
		}
	}

	private class QueryListAdapter extends CursorAdapter {

		private class ViewHolder {

			ImageView result_icon;
			TextView query_result;
			TextView result_summary;

			public ViewHolder(View view) {
				result_icon = (ImageView) view.findViewById(R.id.result_icon);
				query_result = (TextView) view.findViewById(R.id.query_result);
				result_summary = (TextView) view.findViewById(R.id.result_summary);
			}

		}

		private QueryListAdapter(Context context, Cursor cursor, boolean autoRequery) {
			super(context, cursor, autoRequery);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {

			View view = LayoutInflater.from(context).inflate(R.layout.query_list_item, null);
			ViewHolder viewholder = new ViewHolder(view);
			view.setTag(viewholder);
			return view;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {

			ViewHolder viewholder = (ViewHolder) view.getTag();

			String mimetype = cursor.getString(cursor
					.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE));

			if (mimetype == null) {
				mimetype = "audio/";
			}
			if (mimetype.equals("artist")) {
				viewholder.result_icon.setImageResource(R.drawable.ic_mp_list_artist);
				String name = cursor.getString(cursor
						.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST));
				String displayname = name;
				boolean isunknown = false;
				if (name == null || name.equals(MediaStore.UNKNOWN_STRING)) {
					displayname = context.getString(R.string.unknown_artist);
					isunknown = true;
				}
				viewholder.query_result.setText(displayname);

				int numalbums = cursor.getInt(cursor.getColumnIndexOrThrow("data1"));
				int numsongs = cursor.getInt(cursor.getColumnIndexOrThrow("data2"));

				String songs_albums = MusicUtils.makeAlbumsSongsLabel(context, numalbums, numsongs,
						isunknown);

				viewholder.result_summary.setText(songs_albums);

			} else if (mimetype.equals("album")) {
				viewholder.result_icon.setImageResource(R.drawable.ic_mp_list_album);
				String name = cursor.getString(cursor
						.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM));
				String displayname = name;
				if (name == null || name.equals(MediaStore.UNKNOWN_STRING)) {
					displayname = context.getString(R.string.unknown_album);
				}
				viewholder.query_result.setText(displayname);

				name = cursor.getString(cursor
						.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST));
				displayname = name;
				if (name == null || name.equals(MediaStore.UNKNOWN_STRING)) {
					displayname = context.getString(R.string.unknown_artist);
				}
				viewholder.result_summary.setText(displayname);

			} else if (mimetype.startsWith("audio/") || mimetype.equals("application/ogg")
					|| mimetype.equals("application/x-ogg")) {
				viewholder.result_icon.setImageResource(R.drawable.ic_mp_list_song);
				String name = cursor.getString(cursor
						.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
				viewholder.query_result.setText(name);

				String displayname = cursor.getString(cursor
						.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST));
				if (displayname == null || displayname.equals(MediaStore.UNKNOWN_STRING)) {
					displayname = context.getString(R.string.unknown_artist);
				}
				name = cursor
						.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM));
				if (name == null || name.equals(MediaStore.UNKNOWN_STRING)) {
					name = context.getString(R.string.unknown_album);
				}
				viewholder.result_summary.setText(displayname + " - " + name);
			}
		}

	}

}
