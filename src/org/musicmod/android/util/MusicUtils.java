/*
 * Copyright (C) 2008 The Android Open Source Project
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

package org.musicmod.android.util;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.musicmod.android.Constants;
import org.musicmod.android.IMusicPlaybackService;
import org.musicmod.android.MusicPlaybackService;
import org.musicmod.android.R;
import org.musicmod.android.ScanningProgress;
import org.musicmod.android.app.MusicPlaybackActivity;

public class MusicUtils implements Constants {

	public static String makeAlbumsLabel(Context context, int numalbums, int numsongs,
			boolean isUnknown) {

		// There are two formats for the albums/songs information:
		// "N Song(s)" - used for unknown artist/album
		// "N Album(s)" - used for known albums

		StringBuilder songs_albums = new StringBuilder();

		Resources r = context.getResources();
		if (isUnknown) {
			String f = r.getQuantityText(R.plurals.Nsongs, numsongs).toString();
			sFormatBuilder.setLength(0);
			sFormatter.format(f, Integer.valueOf(numsongs));
			songs_albums.append(sFormatBuilder);
		} else {
			String f = r.getQuantityText(R.plurals.Nalbums, numalbums).toString();
			sFormatBuilder.setLength(0);
			sFormatter.format(f, Integer.valueOf(numalbums));
			songs_albums.append(sFormatBuilder);
			songs_albums.append(context.getString(R.string.albumsongseparator));
		}
		return songs_albums.toString();
	}

	/**
	 * This is now only used for the query screen
	 */
	public static String makeAlbumsSongsLabel(Context context, int numalbums, int numsongs,
			boolean isUnknown) {

		// There are several formats for the albums/songs information:
		// "1 Song" - used if there is only 1 song
		// "N Songs" - used for the "unknown artist" item
		// "1 Album"/"N Songs"
		// "N Album"/"M Songs"
		// Depending on locale, these may need to be further subdivided

		StringBuilder songs_albums = new StringBuilder();

		Resources r = context.getResources();
		if (!isUnknown) {
			String f = r.getQuantityText(R.plurals.Nalbums, numalbums).toString();
			sFormatBuilder.setLength(0);
			sFormatter.format(f, Integer.valueOf(numalbums));
			songs_albums.append(sFormatBuilder);
			songs_albums.append(context.getString(R.string.albumsongseparator));
		}
		String f = r.getQuantityText(R.plurals.Nsongs, numsongs).toString();
		sFormatBuilder.setLength(0);
		sFormatter.format(f, Integer.valueOf(numsongs));
		songs_albums.append(sFormatBuilder);
		return songs_albums.toString();
	}

	public static IMusicPlaybackService sService = null;
	private static HashMap<Context, ServiceBinder> sConnectionMap = new HashMap<Context, ServiceBinder>();

	public static ServiceToken bindToService(Activity context) {

		return bindToService(context, null);
	}

	public static ServiceToken bindToService(Activity context, ServiceConnection callback) {

		Activity realActivity = context.getParent();
		if (realActivity == null) {
			realActivity = context;
		}
		ContextWrapper cw = new ContextWrapper(realActivity);
		cw.startService(new Intent(cw, MusicPlaybackService.class));
		ServiceBinder sb = new ServiceBinder(callback);
		if (cw.bindService((new Intent()).setClass(cw, MusicPlaybackService.class), sb, 0)) {
			sConnectionMap.put(cw, sb);
			return new ServiceToken(cw);
		}
		Log.e("Music", "Failed to bind to service");
		return null;
	}

	public static void unbindFromService(ServiceToken token) {

		if (token == null) {
			Log.e(LOGTAG_MUSICUTILS, "Trying to unbind with null token");
			return;
		}
		ContextWrapper cw = token.mWrappedContext;
		ServiceBinder sb = sConnectionMap.remove(cw);
		if (sb == null) {
			Log.e(LOGTAG_MUSICUTILS, "Trying to unbind for unknown Context");
			return;
		}
		cw.unbindService(sb);
		if (sConnectionMap.isEmpty()) {
			// presumably there is nobody interested in the service at this
			// point,
			// so don't hang on to the ServiceConnection
			sService = null;
		}
	}

	private static class ServiceBinder implements ServiceConnection {

		ServiceConnection mCallback;

		ServiceBinder(ServiceConnection callback) {

			mCallback = callback;
		}

		public void onServiceConnected(ComponentName className, android.os.IBinder service) {

			sService = IMusicPlaybackService.Stub.asInterface(service);
			initAlbumArtCache();
			if (mCallback != null) {
				mCallback.onServiceConnected(className, service);
			}
		}

		public void onServiceDisconnected(ComponentName className) {

			if (mCallback != null) {
				mCallback.onServiceDisconnected(className);
			}
			sService = null;
		}
	}

	public static long getCurrentAlbumId() {

		if (sService != null) {
			try {
				return sService.getAlbumId();
			} catch (RemoteException ex) {
			}
		}
		return -1;
	}

	public static long getCurrentArtistId() {

		if (MusicUtils.sService != null) {
			try {
				return sService.getArtistId();
			} catch (RemoteException ex) {
			}
		}
		return -1;
	}

	public static long getCurrentAudioId() {

		if (MusicUtils.sService != null) {
			try {
				return sService.getAudioId();
			} catch (RemoteException ex) {
			}
		}
		return -1;
	}

	public static int getCurrentShuffleMode() {

		int mode = MusicPlaybackService.SHUFFLE_NONE;
		if (sService != null) {
			try {
				mode = sService.getShuffleMode();
			} catch (RemoteException ex) {
			}
		}
		return mode;
	}

	public static void togglePartyShuffle() {

		if (sService != null) {
			int shuffle = getCurrentShuffleMode();
			try {
				if (shuffle == MusicPlaybackService.SHUFFLE_AUTO) {
					sService.setShuffleMode(MusicPlaybackService.SHUFFLE_NONE);
				} else {
					sService.setShuffleMode(MusicPlaybackService.SHUFFLE_AUTO);
				}
			} catch (RemoteException ex) {
			}
		}
	}

	public static void setPartyShuffleMenuIcon(Menu menu) {

		MenuItem item = menu.findItem(PARTY_SHUFFLE);
		if (item != null) {
			int shuffle = MusicUtils.getCurrentShuffleMode();
			if (shuffle == MusicPlaybackService.SHUFFLE_AUTO) {
				item.setIcon(R.drawable.ic_menu_party_shuffle);
				item.setTitle(R.string.party_shuffle_off);
			} else {
				item.setIcon(R.drawable.ic_menu_party_shuffle);
				item.setTitle(R.string.party_shuffle);
			}
		}
	}

	/*
	 * Returns true if a file is currently opened for playback (regardless of
	 * whether it's playing or paused).
	 */
	public static boolean isMusicLoaded() {

		if (MusicUtils.sService != null) {
			try {
				return sService.getPath() != null;
			} catch (RemoteException ex) {
			}
		}
		return false;
	}

	private final static long[] sEmptyList = new long[0];

	public static long[] getSongListForCursor(Cursor cursor) {

		if (cursor == null) {
			return sEmptyList;
		}
		int len = cursor.getCount();
		long[] list = new long[len];
		cursor.moveToFirst();
		int colidx = -1;
		try {
			colidx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.AUDIO_ID);
		} catch (IllegalArgumentException ex) {
			colidx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
		}
		for (int i = 0; i < len; i++) {
			list[i] = cursor.getLong(colidx);
			cursor.moveToNext();
		}
		return list;
	}

	public static long[] getSongListForArtist(Context context, long id) {

		final String[] ccols = new String[] { MediaStore.Audio.Media._ID };
		String where = MediaStore.Audio.Media.ARTIST_ID + "=" + id + " AND "
				+ MediaStore.Audio.Media.IS_MUSIC + "=1";
		Cursor cursor = query(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, ccols, where,
				null, MediaStore.Audio.Media.ALBUM_KEY + "," + MediaStore.Audio.Media.TRACK);

		if (cursor != null) {
			long[] list = getSongListForCursor(cursor);
			cursor.close();
			return list;
		}
		return sEmptyList;
	}

	public static long[] getSongListForAlbum(Context context, long id) {

		final String[] ccols = new String[] { MediaStore.Audio.Media._ID };
		String where = MediaStore.Audio.Media.ALBUM_ID + "=" + id + " AND "
				+ MediaStore.Audio.Media.IS_MUSIC + "=1";
		Cursor cursor = query(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, ccols, where,
				null, MediaStore.Audio.Media.TRACK);

		if (cursor != null) {
			long[] list = getSongListForCursor(cursor);
			cursor.close();
			return list;
		}
		return sEmptyList;
	}

	public static long[] getSongListForPlaylist(Context context, long plid) {

		final String[] ccols = new String[] { MediaStore.Audio.Playlists.Members.AUDIO_ID };
		Cursor cursor = query(context,
				MediaStore.Audio.Playlists.Members.getContentUri("external", plid), ccols, null,
				null, MediaStore.Audio.Playlists.Members.DEFAULT_SORT_ORDER);

		if (cursor != null) {
			long[] list = getSongListForCursor(cursor);
			cursor.close();
			return list;
		}
		return sEmptyList;
	}

	public static void playPlaylist(Context context, long plid) {

		long[] list = getSongListForPlaylist(context, plid);
		if (list != null) {
			playAll(context, list, -1, false);
		}
	}

	public static void playRecentlyAdded(Context context) {

		// do a query for all songs added in the last X weeks
		int weekX = new PreferencesEditor(context).getIntPref(PREF_KEY_NUMWEEKS, 2)
				* (3600 * 24 * 7);
		final String[] ccols = new String[] { MediaStore.Audio.Media._ID };
		String where = MediaStore.MediaColumns.DATE_ADDED + ">"
				+ (System.currentTimeMillis() / 1000 - weekX);
		Cursor cursor = query(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, ccols, where,
				null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);

		if (cursor == null) {
			// Todo: show a message
			return;
		}
		try {
			int len = cursor.getCount();
			long[] list = new long[len];
			for (int i = 0; i < len; i++) {
				cursor.moveToNext();
				list[i] = cursor.getLong(0);
			}
			MusicUtils.playAll(context, list, 0);
		} catch (SQLiteException ex) {
		} finally {
			cursor.close();
		}
	}

	public static long[] getAllSongs(Context context) {

		Cursor c = query(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
				new String[] { MediaStore.Audio.Media._ID },
				MediaStore.Audio.Media.IS_MUSIC + "=1", null, null);
		try {
			if (c == null || c.getCount() == 0) {
				return null;
			}
			int len = c.getCount();
			long[] list = new long[len];
			for (int i = 0; i < len; i++) {
				c.moveToNext();
				list[i] = c.getLong(0);
			}

			return list;
		} finally {
			if (c != null) {
				c.close();
			}
		}
	}

	public static void setQueuePosition(int index) {
		if (sService == null) return;
		try {
			sService.setQueuePosition(index);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Fills out the given submenu with items for "new playlist" and any
	 * existing playlists. When the user selects an item, the application will
	 * receive PLAYLIST_SELECTED with the Uri of the selected playlist,
	 * NEW_PLAYLIST if a new playlist should be created, and QUEUE if the
	 * "current playlist" was selected.
	 * 
	 * @param context
	 *            The context to use for creating the menu items
	 * @param sub
	 *            The submenu to add the items to.
	 */
	public static void makePlaylistMenu(Context context, SubMenu sub) {

		String[] cols = new String[] { MediaStore.Audio.Playlists._ID,
				MediaStore.Audio.Playlists.NAME };
		ContentResolver resolver = context.getContentResolver();
		if (resolver == null) {
			System.out.println("resolver = null");
		} else {
			String whereclause = MediaStore.Audio.Playlists.NAME + " != ''";
			Cursor cur = resolver.query(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, cols,
					whereclause, null, MediaStore.Audio.Playlists.NAME);
			sub.clear();
			sub.add(1, QUEUE, 0, R.string.queue);
			sub.add(1, NEW_PLAYLIST, 0, R.string.new_playlist);
			if (cur != null && cur.getCount() > 0) {
				// sub.addSeparator(1, 0);
				cur.moveToFirst();
				while (!cur.isAfterLast()) {
					Intent intent = new Intent();
					intent.putExtra("playlist", cur.getLong(0));
					// if (cur.getInt(0) == mLastPlaylistSelected) {
					// sub.add(0, MusicBaseActivity.PLAYLIST_SELECTED,
					// cur.getString(1)).setIntent(intent);
					// } else {
					sub.add(1, PLAYLIST_SELECTED, 0, cur.getString(1)).setIntent(intent);
					// }
					cur.moveToNext();
				}
			}
			if (cur != null) {
				cur.close();
			}
		}
	}

	public static void makePlaylistList(Context context, boolean create_shortcut,
			List<Map<String, String>> list) {

		Map<String, String> map;

		String[] cols = new String[] { MediaStore.Audio.Playlists._ID,
				MediaStore.Audio.Playlists.NAME };
		ContentResolver resolver = context.getContentResolver();
		if (resolver == null) {
			System.out.println("resolver = null");
		} else {
			String whereclause = MediaStore.Audio.Playlists.NAME + " != ''";
			Cursor cur = resolver.query(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, cols,
					whereclause, null, MediaStore.Audio.Playlists.NAME);
			list.clear();

			if (create_shortcut) {
				map = new HashMap<String, String>();
				map.put(MAP_KEY_ID, String.valueOf(PLAYLIST_ALL_SONGS));
				map.put(MAP_KEY_NAME, context.getString(R.string.play_all));
				list.add(map);

				map = new HashMap<String, String>();
				map.put(MAP_KEY_ID, String.valueOf(PLAYLIST_RECENTLY_ADDED));
				map.put(MAP_KEY_NAME, context.getString(R.string.recently_added));
				list.add(map);
			} else {
				map = new HashMap<String, String>();
				map.put(MAP_KEY_ID, String.valueOf(PLAYLIST_QUEUE));
				map.put(MAP_KEY_NAME, context.getString(R.string.queue));
				list.add(map);

				map = new HashMap<String, String>();
				map.put(MAP_KEY_ID, String.valueOf(PLAYLIST_NEW));
				map.put(MAP_KEY_NAME, context.getString(R.string.new_playlist));
				list.add(map);
			}

			if (cur != null && cur.getCount() > 0) {
				// sub.addSeparator(1, 0);
				cur.moveToFirst();
				while (!cur.isAfterLast()) {
					map = new HashMap<String, String>();
					map.put(MAP_KEY_ID, String.valueOf(cur.getLong(0)));
					map.put(MAP_KEY_NAME, cur.getString(1));
					list.add(map);
					cur.moveToNext();
				}

			}
			if (cur != null) {
				cur.close();
			}
		}
	}

	public static void clearPlaylist(Context context, int plid) {

		Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", plid);
		context.getContentResolver().delete(uri, null, null);
		return;
	}

	public static void deleteTracks(Context context, long[] list) {

		String[] cols = new String[] { MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DATA,
				MediaStore.Audio.Media.ALBUM_ID };
		StringBuilder where = new StringBuilder();
		where.append(MediaStore.Audio.Media._ID + " IN (");
		for (int i = 0; i < list.length; i++) {
			where.append(list[i]);
			if (i < list.length - 1) {
				where.append(",");
			}
		}
		where.append(")");
		Cursor c = query(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cols,
				where.toString(), null, null);

		if (c != null) {

			// step 1: remove selected tracks from the current playlist, as well
			// as from the album art cache
			try {
				c.moveToFirst();
				while (!c.isAfterLast()) {
					// remove from current playlist
					long id = c.getLong(0);
					sService.removeTrack(id);
					// remove from album art cache
					long artIndex = c.getLong(2);
					synchronized (sArtCache) {
						sArtCache.remove(artIndex);
					}
					c.moveToNext();
				}
			} catch (RemoteException ex) {
			}

			// step 2: remove selected tracks from the database
			context.getContentResolver().delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
					where.toString(), null);

			// step 3: remove files from card
			c.moveToFirst();
			while (!c.isAfterLast()) {
				String name = c.getString(1);
				File f = new File(name);
				try { // File.delete can throw a security exception
					if (!f.delete()) {
						// I'm not sure if we'd ever get here (deletion would
						// have to fail, but no exception thrown)
						Log.e(LOGTAG_MUSICUTILS, "Failed to delete file " + name);
					}
					c.moveToNext();
				} catch (SecurityException ex) {
					c.moveToNext();
				}
			}
			c.close();
		}

		String message = context.getResources().getQuantityString(R.plurals.NNNtracksdeleted,
				list.length, Integer.valueOf(list.length));

		Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
		// We deleted a number of tracks, which could affect any number of
		// things
		// in the media content domain, so update everything.
		context.getContentResolver().notifyChange(Uri.parse("content://media"), null);
	}

	public static void deleteLyrics(Context context, long[] list) {

		String[] cols = new String[] { MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DATA,
				MediaStore.Audio.Media.ALBUM_ID };
		StringBuilder where = new StringBuilder();
		where.append(MediaStore.Audio.Media._ID + " IN (");
		for (int i = 0; i < list.length; i++) {
			where.append(list[i]);
			if (i < list.length - 1) {
				where.append(",");
			}
		}
		where.append(")");
		Cursor c = query(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cols,
				where.toString(), null, null);

		int mDeletedLyricsCount = 0;

		if (c != null) {
			// remove files from card
			c.moveToFirst();
			while (!c.isAfterLast()) {
				String name = c.getString(1);
				String lyrics = name.substring(0, name.lastIndexOf(".")) + ".lrc";
				File f = new File(lyrics);
				try { // File.delete can throw a security exception
					if (!f.delete()) {
						// I'm not sure if we'd ever get here (deletion would
						// have to fail, but no exception thrown)
						Log.e(LOGTAG_MUSICUTILS, "Failed to delete file " + lyrics);
					} else {
						mDeletedLyricsCount += 1;
					}
					c.moveToNext();
				} catch (SecurityException ex) {
					c.moveToNext();
				}
			}
			c.close();
		}

		String message = context.getResources().getQuantityString(R.plurals.NNNlyricsdeleted,
				mDeletedLyricsCount, Integer.valueOf(mDeletedLyricsCount));

		reloadLyrics();
		Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
	}

	public static void reloadLyrics() {

		if (sService == null) {
			return;
		}
		try {
			sService.reloadLyrics();
		} catch (Exception e) {
			// e.printStackTrace();
		}
	}

	public static void startSleepTimer(long milliseconds, boolean gentle) {

		if (sService == null) {
			return;
		}
		try {
			sService.startSleepTimer(milliseconds, gentle);
		} catch (Exception e) {
			// do nothing
		}
	}

	public static void stopSleepTimer() {

		if (sService == null) {
			return;
		}
		try {
			sService.stopSleepTimer();
		} catch (Exception e) {
			// do nothing
		}
	}

	public static long getSleepTimerRemained() {

		long remained = 0;
		if (sService == null) {
			return remained;
		}
		try {
			remained = sService.getSleepTimerRemained();
		} catch (Exception e) {
			// do nothing
		}
		return remained;
	}

	public static void reloadSettings() {

		if (sService == null) {
			return;
		}
		try {
			sService.reloadSettings();
		} catch (Exception e) {
			// e.printStackTrace();
		}
	}

	public static void addToCurrentPlaylist(Context context, long[] list) {

		if (sService == null) {
			return;
		}
		try {
			sService.enqueue(list, MusicPlaybackService.LAST);
			String message = context.getResources().getQuantityString(
					R.plurals.NNNtrackstoplaylist, list.length, Integer.valueOf(list.length));
			Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
		} catch (RemoteException ex) {
		}
	}

	private static ContentValues[] sContentValuesCache = null;

	/**
	 * @param ids
	 *            The source array containing all the ids to be added to the
	 *            playlist
	 * @param offset
	 *            Where in the 'ids' array we start reading
	 * @param len
	 *            How many items to copy during this pass
	 * @param base
	 *            The play order offset to use for this pass
	 */
	private static void makeInsertItems(long[] ids, int offset, int len, int base) {

		// adjust 'len' if would extend beyond the end of the source array
		if (offset + len > ids.length) {
			len = ids.length - offset;
		}
		// allocate the ContentValues array, or reallocate if it is the wrong
		// size
		if (sContentValuesCache == null || sContentValuesCache.length != len) {
			sContentValuesCache = new ContentValues[len];
		}
		// fill in the ContentValues array with the right values for this pass
		for (int i = 0; i < len; i++) {
			if (sContentValuesCache[i] == null) {
				sContentValuesCache[i] = new ContentValues();
			}

			sContentValuesCache[i].put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, base + offset
					+ i);
			sContentValuesCache[i]
					.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, ids[offset + i]);
		}
	}

	public static void renamePlaylist(Context context, long id, String name) {

		if (name != null && name.length() > 0) {
			ContentResolver resolver = context.getContentResolver();
			ContentValues values = new ContentValues(1);
			values.put(MediaStore.Audio.Playlists.NAME, name);
			resolver.update(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, values,
					MediaStore.Audio.Playlists._ID + "=?", new String[] { String.valueOf(id) });

			Toast.makeText(context, R.string.playlist_renamed, Toast.LENGTH_SHORT).show();
		}
	}

	public static long createPlaylist(Context context, String name) {

		if (name != null && name.length() > 0) {
			ContentResolver resolver = context.getContentResolver();
			String[] cols = new String[] { MediaStore.Audio.Playlists.NAME };
			String whereclause = MediaStore.Audio.Playlists.NAME + " = '" + name + "'";
			Cursor cur = resolver.query(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, cols,
					whereclause, null, null);
			if (cur.getCount() <= 0) {
				ContentValues values = new ContentValues(1);
				values.put(MediaStore.Audio.Playlists.NAME, name);
				Uri uri = resolver.insert(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, values);
				return Long.parseLong(uri.getLastPathSegment());
			}
			return -1;
		}
		return -1;
	}

	public static void addToPlaylist(Context context, long[] ids, long playlistid) {

		if (ids == null) {
			// this shouldn't happen (the menuitems shouldn't be visible
			// unless the selected item represents something playable
			Log.e("MusicBase", "ListSelection null");
		} else {
			int size = ids.length;
			ContentResolver resolver = context.getContentResolver();
			// need to determine the number of items currently in the playlist,
			// so the play_order field can be maintained.
			String[] cols = new String[] { "count(*)" };
			Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistid);
			Cursor cur = resolver.query(uri, cols, null, null, null);
			cur.moveToFirst();
			int base = cur.getInt(0);
			cur.close();
			int numinserted = 0;
			for (int i = 0; i < size; i += 1000) {
				makeInsertItems(ids, i, 1000, base);
				numinserted += resolver.bulkInsert(uri, sContentValuesCache);
			}
			String message = context.getResources().getQuantityString(
					R.plurals.NNNtrackstoplaylist, numinserted, numinserted);
			Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
			// mLastPlaylistSelected = playlistid;
		}
	}

	public static Cursor query(Context context, Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder, int limit) {

		try {
			ContentResolver resolver = context.getContentResolver();
			if (resolver == null) {
				return null;
			}
			if (limit > 0) {
				uri = uri.buildUpon().appendQueryParameter("limit", "" + limit).build();
			}
			return resolver.query(uri, projection, selection, selectionArgs, sortOrder);
		} catch (UnsupportedOperationException ex) {
			return null;
		}

	}

	public static Cursor query(Context context, Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {

		return query(context, uri, projection, selection, selectionArgs, sortOrder, 0);
	}

	public static boolean isMediaScannerScanning(Context context) {

		boolean result = false;
		Cursor cursor = query(context, MediaStore.getMediaScannerUri(),
				new String[] { MediaStore.MEDIA_SCANNER_VOLUME }, null, null, null);
		if (cursor != null) {
			if (cursor.getCount() == 1) {
				cursor.moveToFirst();
				result = "external".equals(cursor.getString(0));
			}
			cursor.close();
		}

		return result;
	}

	public static void setSpinnerState(Activity a) {

		if (isMediaScannerScanning(a)) {
			// start the progress spinner
			a.getWindow().setFeatureInt(Window.FEATURE_INDETERMINATE_PROGRESS,
					Window.PROGRESS_INDETERMINATE_ON);

			a.getWindow().setFeatureInt(Window.FEATURE_INDETERMINATE_PROGRESS,
					Window.PROGRESS_VISIBILITY_ON);
		} else {
			// stop the progress spinner
			a.getWindow().setFeatureInt(Window.FEATURE_INDETERMINATE_PROGRESS,
					Window.PROGRESS_VISIBILITY_OFF);
		}
	}

	private static String mLastSdStatus;

	public static void displayDatabaseError(Activity a) {

		if (a.isFinishing()) {
			// When switching tabs really fast, we can end up with a null
			// cursor (not sure why), which will bring us here.
			// Don't bother showing an error message in that case.
			return;
		}

		String status = Environment.getExternalStorageState();
		int title, message;

		title = R.string.sdcard_error_title;
		message = R.string.sdcard_error;

		if (status.equals(Environment.MEDIA_SHARED) || status.equals(Environment.MEDIA_UNMOUNTED)) {
			title = R.string.sdcard_busy_title;
			message = R.string.sdcard_busy;
		} else if (status.equals(Environment.MEDIA_REMOVED)) {
			title = R.string.sdcard_missing_title;
			message = R.string.sdcard_missing;
		} else if (status.equals(Environment.MEDIA_MOUNTED)) {
			// The card is mounted, but we didn't get a valid cursor.
			// This probably means the mediascanner hasn't started scanning the
			// card yet (there is a small window of time during boot where this
			// will happen).
			a.setTitle("");
			Intent intent = new Intent();
			intent.setClass(a, ScanningProgress.class);
			a.startActivityForResult(intent, SCAN_DONE);
		} else if (!TextUtils.equals(mLastSdStatus, status)) {
			mLastSdStatus = status;
			Log.d(LOGTAG_MUSICUTILS, "sd card: " + status);
		}

		a.setTitle(title);
		View v = a.findViewById(R.id.sd_message);
		if (v != null) {
			v.setVisibility(View.VISIBLE);
		}
		v = a.findViewById(R.id.sd_icon);
		if (v != null) {
			v.setVisibility(View.VISIBLE);
		}
		v = a.findViewById(android.R.id.list);
		if (v != null) {
			v.setVisibility(View.GONE);
		}
		v = a.findViewById(R.id.buttonbar);
		if (v != null) {
			v.setVisibility(View.GONE);
		}
		TextView tv = (TextView) a.findViewById(R.id.sd_message);
		tv.setText(message);
	}

	public static void hideDatabaseError(Activity a) {

		View v = a.findViewById(R.id.sd_message);
		if (v != null) {
			v.setVisibility(View.GONE);
		}
		v = a.findViewById(R.id.sd_icon);
		if (v != null) {
			v.setVisibility(View.GONE);
		}
		v = a.findViewById(android.R.id.list);
		if (v != null) {
			v.setVisibility(View.VISIBLE);
		}
	}

	static protected Uri getContentURIForPath(String path) {

		return Uri.fromFile(new File(path));
	}

	/*
	 * Try to use String.format() as little as possible, because it creates a
	 * new Formatter every time you call it, which is very inefficient. Reusing
	 * an existing Formatter more than tripled the speed of makeTimeString().
	 * This Formatter/StringBuilder are also used by makeAlbumSongsLabel()
	 */
	private static StringBuilder sFormatBuilder = new StringBuilder();
	private static Formatter sFormatter = new Formatter(sFormatBuilder, Locale.getDefault());
	private static final Object[] sTimeArgs = new Object[5];

	public static String makeTimeString(Context context, long secs) {

		String durationformat = context.getString(secs < 3600 ? R.string.durationformatshort
				: R.string.durationformatlong);

		/*
		 * Provide multiple arguments so the format can be changed easily by
		 * modifying the xml.
		 */
		sFormatBuilder.setLength(0);

		final Object[] timeArgs = sTimeArgs;
		timeArgs[0] = secs / 3600;
		timeArgs[1] = secs / 60;
		timeArgs[2] = (secs / 60) % 60;
		timeArgs[3] = secs;
		timeArgs[4] = secs % 60;

		return sFormatter.format(durationformat, timeArgs).toString();
	}

	public static void shuffleAll(Context context) {

		playAll(context, getAllSongs(context), 0, true);
	}

	public static void shuffleAll(Context context, Cursor cursor) {

		playAll(context, cursor, 0, true);
	}

	public static void playAll(Context context) {

		playAll(context, getAllSongs(context), 0);
	}

	public static void playAll(Context context, Cursor cursor) {

		playAll(context, cursor, 0, false);
	}

	public static void playAll(Context context, Cursor cursor, int position) {

		playAll(context, cursor, position, false);
	}

	public static void playAll(Context context, long[] list, int position) {

		playAll(context, list, position, false);
	}

	private static void playAll(Context context, Cursor cursor, int position, boolean force_shuffle) {

		long[] list = getSongListForCursor(cursor);
		playAll(context, list, position, force_shuffle);
	}

	private static void playAll(Context context, long[] list, int position, boolean force_shuffle) {

		if (list.length == 0 || sService == null) {
			Log.d(LOGTAG_MUSICUTILS, "attempt to play empty song list");
			// Don't try to play empty playlists. Nothing good will come of it.
			String message = context.getString(R.string.emptyplaylist, list.length);
			Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
			return;
		}
		try {
			if (force_shuffle) {
				sService.setShuffleMode(MusicPlaybackService.SHUFFLE_NORMAL);
			}
			long curid = sService.getAudioId();
			int curpos = sService.getQueuePosition();
			if (position != -1 && curpos == position && curid == list[position]) {
				// The selected file is the file that's currently playing;
				// figure out if we need to restart with a new playlist,
				// or just launch the playback activity.
				long[] playlist = sService.getQueue();
				if (Arrays.equals(list, playlist)) {
					// we don't need to set a new list, but we should resume
					// playback if needed
					sService.play();
					return; // the 'finally' block will still run
				}
			}
			if (position < 0) {
				position = 0;
			}
			sService.open(list, force_shuffle ? -1 : position);
			sService.play();
		} catch (RemoteException ex) {
		} finally {
			Intent intent = new Intent(INTENT_PLAYBACK_VIEWER)
					.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(intent);
		}
	}

	public static void clearQueue() {

		try {
			sService.removeTracks(0, Integer.MAX_VALUE);
		} catch (RemoteException ex) {
		}
	}

	private static final BitmapFactory.Options sBitmapOptionsCache = new BitmapFactory.Options();
	private static final BitmapFactory.Options sBitmapOptions = new BitmapFactory.Options();
	private static final Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");
	private static final HashMap<Long, Bitmap> sArtCache = new HashMap<Long, Bitmap>();
	private static int sArtCacheId = -1;

	static {
		// for the cache,
		// 565 is faster to decode and display
		// and we don't want to dither here because the image will be scaled
		// down later
		sBitmapOptionsCache.inPreferredConfig = Bitmap.Config.RGB_565;
		sBitmapOptionsCache.inDither = false;

		sBitmapOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
		sBitmapOptions.inDither = false;
	}

	public static void initAlbumArtCache() {

		try {
			int id = sService.getMediaMountedCount();
			if (id != sArtCacheId) {
				clearAlbumArtCache();
				sArtCacheId = id;
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	public static void clearAlbumArtCache() {

		synchronized (sArtCache) {
			sArtCache.clear();
		}
	}

	public static Bitmap getCachedArtwork(Context context, long artIndex, int width, int height) {

		Bitmap bitmap = null;
		synchronized (sArtCache) {
			bitmap = sArtCache.get(artIndex);
		}
		if (bitmap == null) {
			bitmap = MusicUtils.getArtworkQuick(context, artIndex, width, height);
			if (bitmap != null) {
				synchronized (sArtCache) {
					sArtCache.put(artIndex, bitmap);
				}
			}
		}
		return bitmap;
	}

	// Get album art for specified album. This method will not try to
	// fall back to getting artwork directly from the file, nor will
	// it attempt to repair the database.
	public static Bitmap getArtworkQuick(Context context, long album_id, int w, int h) {

		// NOTE: There is in fact a 1 pixel border on the right side in the
		// ImageView
		// used to display this drawable. Take it into account now, so we don't
		// have to
		// scale later.
		w -= 1;
		ContentResolver res = context.getContentResolver();
		Uri uri = ContentUris.withAppendedId(sArtworkUri, album_id);
		if (uri != null) {
			ParcelFileDescriptor fd = null;
			try {
				fd = res.openFileDescriptor(uri, "r");
				int sampleSize = 1;

				// Compute the closest power-of-two scale factor
				// and pass that to sBitmapOptionsCache.inSampleSize, which will
				// result in faster decoding and better quality
				sBitmapOptionsCache.inJustDecodeBounds = true;
				BitmapFactory.decodeFileDescriptor(fd.getFileDescriptor(), null,
						sBitmapOptionsCache);
				int nextWidth = sBitmapOptionsCache.outWidth >> 1;
				int nextHeight = sBitmapOptionsCache.outHeight >> 1;
				while (nextWidth > w && nextHeight > h) {
					sampleSize <<= 1;
					nextWidth >>= 1;
					nextHeight >>= 1;
				}

				sBitmapOptionsCache.inSampleSize = sampleSize;
				sBitmapOptionsCache.inJustDecodeBounds = false;
				Bitmap b = BitmapFactory.decodeFileDescriptor(fd.getFileDescriptor(), null,
						sBitmapOptionsCache);

				if (b != null) {
					// finally rescale to exactly the size we need
					if (sBitmapOptionsCache.outWidth != w || sBitmapOptionsCache.outHeight != h) {
						Bitmap tmp = Bitmap.createScaledBitmap(b, w, h, true);
						// Bitmap.createScaledBitmap() can return the same
						// bitmap
						if (tmp != b) b.recycle();
						b = tmp;
					}
				}

				return b;
			} catch (FileNotFoundException e) {
			} finally {
				try {
					if (fd != null) fd.close();
				} catch (IOException e) {
				}
			}
		}
		return null;
	}

	/**
	 * Get album art for specified album. You should not pass in the album id
	 * for the "unknown" album here (use -1 instead) This method always returns
	 * the default album art icon when no album art is found.
	 */
	public static Bitmap getArtwork(Context context, long song_id, long album_id) {

		return getArtwork(context, song_id, album_id, true);
	}

	/**
	 * Get album art for specified album. You should not pass in the album id
	 * for the "unknown" album here (use -1 instead)
	 */
	public static Bitmap getArtwork(Context context, long song_id, long album_id,
			boolean allowdefault) {

		if (album_id < 0) {
			// This is something that is not in the database, so get the album
			// art directly
			// from the file.
			if (song_id >= 0) {
				Bitmap bm = getArtworkFromFile(context, song_id, -1);
				if (bm != null) {
					return bm;
				}
			}
			if (allowdefault) {
				return getDefaultArtwork(context);
			}
			return null;
		}

		ContentResolver res = context.getContentResolver();
		Uri uri = ContentUris.withAppendedId(sArtworkUri, album_id);
		if (uri != null) {
			InputStream in = null;
			try {
				in = res.openInputStream(uri);
				return BitmapFactory.decodeStream(in, null, sBitmapOptions);
			} catch (FileNotFoundException ex) {
				// The album art thumbnail does not actually exist. Maybe the
				// user deleted it, or
				// maybe it never existed to begin with.
				Bitmap bm = getArtworkFromFile(context, song_id, album_id);
				if (bm != null) {
					if (bm.getConfig() == null) {
						bm = bm.copy(Bitmap.Config.ARGB_8888, false);
						if (bm == null && allowdefault) {
							return getDefaultArtwork(context);
						}
					}
				} else if (allowdefault) {
					bm = getDefaultArtwork(context);
				}
				return bm;
			} finally {
				try {
					if (in != null) {
						in.close();
					}
				} catch (IOException ex) {
				}
			}
		}

		return null;
	}

	// get album art for specified file
	private static Bitmap getArtworkFromFile(Context context, long songid, long albumid) {

		Bitmap bm = null;

		if (albumid < 0 && songid < 0) {
			throw new IllegalArgumentException("Must specify an album or a song id");
		}

		try {
			if (albumid < 0) {
				Uri uri = Uri.parse("content://media/external/audio/media/" + songid + "/albumart");
				ParcelFileDescriptor pfd = context.getContentResolver()
						.openFileDescriptor(uri, "r");
				if (pfd != null) {
					FileDescriptor fd = pfd.getFileDescriptor();
					bm = BitmapFactory.decodeFileDescriptor(fd);
				}
			} else {
				Uri uri = ContentUris.withAppendedId(sArtworkUri, albumid);
				ParcelFileDescriptor pfd = context.getContentResolver()
						.openFileDescriptor(uri, "r");
				if (pfd != null) {
					FileDescriptor fd = pfd.getFileDescriptor();
					bm = BitmapFactory.decodeFileDescriptor(fd);
				}
			}
		} catch (IllegalStateException ex) {
		} catch (FileNotFoundException ex) {
		}
		return bm;
	}

	private static Bitmap getDefaultArtwork(Context context) {

		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
		return BitmapFactory.decodeStream(
				context.getResources().openRawResource(R.drawable.ic_mp_albumart_unknown), null,
				opts);
	}

	public static Uri getArtworkUri(Context context, long song_id, long album_id) {

		if (album_id < 0) {
			// This is something that is not in the database, so get the album
			// art directly
			// from the file.
			if (song_id >= 0) {
				return getArtworkUriFromFile(context, song_id, -1);
			}
			return null;
		}

		ContentResolver res = context.getContentResolver();
		Uri uri = ContentUris.withAppendedId(sArtworkUri, album_id);
		if (uri != null) {
			InputStream in = null;
			try {
				in = res.openInputStream(uri);
				return uri;
			} catch (FileNotFoundException ex) {
				// The album art thumbnail does not actually exist. Maybe the
				// user deleted it, or
				// maybe it never existed to begin with.
				return getArtworkUriFromFile(context, song_id, album_id);
			} finally {
				try {
					if (in != null) {
						in.close();
					}
				} catch (IOException ex) {
				}
			}
		}
		return null;
	}

	private static Uri getArtworkUriFromFile(Context context, long songid, long albumid) {

		if (albumid < 0 && songid < 0) {
			return null;
		}

		try {
			if (albumid < 0) {
				Uri uri = Uri.parse("content://media/external/audio/media/" + songid + "/albumart");
				ParcelFileDescriptor pfd = context.getContentResolver()
						.openFileDescriptor(uri, "r");
				if (pfd != null) {
					return uri;
				}
			} else {
				Uri uri = ContentUris.withAppendedId(sArtworkUri, albumid);
				ParcelFileDescriptor pfd = context.getContentResolver()
						.openFileDescriptor(uri, "r");
				if (pfd != null) {
					return uri;
				}
			}
		} catch (FileNotFoundException ex) {
			//
		}
		return null;
	}

	public static void updateNowPlaying(Activity a) {

		View nowPlayingView = a.findViewById(R.id.nowplaying);
		if (nowPlayingView == null) {
			return;
		}
		try {
			if (MusicUtils.sService != null && MusicUtils.sService.getAudioId() != -1) {
				TextView title = (TextView) nowPlayingView.findViewById(R.id.nowplaying_track_name);
				TextView artist = (TextView) nowPlayingView
						.findViewById(R.id.nowplaying_artist_name);
				title.setText(MusicUtils.sService.getTrackName());
				String artistName = MusicUtils.sService.getArtistName();
				if (MediaStore.UNKNOWN_STRING.equals(artistName)) {
					artistName = a.getString(R.string.unknown_artist);
				}
				artist.setText(artistName);
				// mNowPlayingView.setOnFocusChangeListener(mFocuser);
				// mNowPlayingView.setOnClickListener(this);
				nowPlayingView.setVisibility(View.VISIBLE);
				nowPlayingView.setOnClickListener(new View.OnClickListener() {

					public void onClick(View v) {

						Context c = v.getContext();
						c.startActivity(new Intent(c, MusicPlaybackActivity.class));
					}
				});
				return;
			}
		} catch (RemoteException ex) {
		}
		nowPlayingView.setVisibility(View.GONE);
	}

	public static void setBackground(View v, Bitmap bm) {

		if (bm == null) {
			v.setBackgroundResource(0);
			return;
		}

		int vwidth = v.getWidth();
		int vheight = v.getHeight();
		int bwidth = bm.getWidth();
		int bheight = bm.getHeight();
		float scalex = (float) vwidth / bwidth;
		float scaley = (float) vheight / bheight;
		float scale = Math.max(scalex, scaley) * 1.3f;

		Bitmap.Config config = Bitmap.Config.ARGB_8888;
		Bitmap bg = Bitmap.createBitmap(vwidth, vheight, config);
		Canvas c = new Canvas(bg);
		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setFilterBitmap(true);
		ColorMatrix greymatrix = new ColorMatrix();
		greymatrix.setSaturation(0);
		ColorMatrix darkmatrix = new ColorMatrix();
		darkmatrix.setScale(.3f, .3f, .3f, 1.0f);
		greymatrix.postConcat(darkmatrix);
		ColorFilter filter = new ColorMatrixColorFilter(greymatrix);
		paint.setColorFilter(filter);
		Matrix matrix = new Matrix();
		matrix.setTranslate(-bwidth / 2, -bheight / 2); // move bitmap center to
														// origin
		matrix.postRotate(10);
		matrix.postScale(scale, scale);
		matrix.postTranslate(vwidth / 2, vheight / 2); // Move bitmap center to
														// view center
		c.drawBitmap(bm, matrix, paint);
		v.setBackgroundDrawable(new BitmapDrawable(bg));
	}

	public static int getCardId(Context context) {

		ContentResolver res = context.getContentResolver();
		Cursor c = res.query(Uri.parse("content://media/external/fs_id"), null, null, null, null);
		int id = -1;
		if (c != null) {
			c.moveToFirst();
			id = c.getInt(0);
			c.close();
		}
		return id;
	}

	static class LogEntry {

		Object item;
		long time;

		LogEntry(Object o) {

			item = o;
			time = System.currentTimeMillis();
		}

		void dump(PrintWriter out) {

			sTime.set(time);
			out.print(sTime.toString() + " : ");
			if (item instanceof Exception) {
				((Exception) item).printStackTrace(out);
			} else {
				out.println(item);
			}
		}
	}

	private static LogEntry[] sMusicLog = new LogEntry[100];
	private static int sLogPtr = 0;
	private static Time sTime = new Time();

	public static void debugLog(Object o) {

		sMusicLog[sLogPtr] = new LogEntry(o);
		sLogPtr++;
		if (sLogPtr >= sMusicLog.length) {
			sLogPtr = 0;
		}
	}

	public static void debugDump(PrintWriter out) {

		for (int i = 0; i < sMusicLog.length; i++) {
			int idx = (sLogPtr + i);
			if (idx >= sMusicLog.length) {
				idx -= sMusicLog.length;
			}
			LogEntry entry = sMusicLog[idx];
			if (entry != null) {
				entry.dump(out);
			}
		}
	}
}
