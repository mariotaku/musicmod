/*
 *              Copyright (C) 2011 The MusicMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *            http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.musicmod.android.util;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import org.musicmod.android.Constants;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import net.roarsoftware.lastfm.Album;
import net.roarsoftware.lastfm.Artist;
import net.roarsoftware.lastfm.Image;
import net.roarsoftware.lastfm.ImageSize;

public class ImageDownloader implements Constants {

	public static String getCoverUrl(String artist, String album) throws Exception {

		Album albumInfo = Album.getInfo(artist, album, LASTFM_APIKEY);
		return albumInfo.getImageURL(ImageSize.MEGA);
	}

	public void getArtistImage(String artist) {

		Image[] images = Artist
				.getImages(artist, LASTFM_APIKEY)
				.getPageResults()
				.toArray(new Image[Artist.getImages(artist, LASTFM_APIKEY).getPageResults().size()]);
		for (Image image : images) {
			Log.d("ImageDownloader", "url = " + image.getImageURL(ImageSize.ORIGINAL));
		}
	}

	public static Bitmap getCoverBitmap(String urlString) {

		try {
			URL url = new URL(urlString);
			return getCoverBitmap(url);
		} catch (Exception e) {
			return null;
		}
	}

	public static Bitmap getCoverBitmap(URL url) {

		try {
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setDoInput(true);
			connection.connect();
			InputStream is = connection.getInputStream();
			return BitmapFactory.decodeStream(is);
		} catch (Exception e) {
			return null;
		}
	}

	public String getArtistQueryURL(String mArtistTitle, int limit) {

		return "http://ws.audioscrobbler.com/2.0/?method=artist.getimages&artist="
				+ URLEncoder.encode(mArtistTitle) + "&api_key=" + LASTFM_APIKEY + "&limit=" + limit;
	}
}
