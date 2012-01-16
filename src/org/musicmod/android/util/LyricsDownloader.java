/*
 *			Copyright (C) 2012 The MusicMod Open Source Project
 *
 *	Licensed under the Apache License, Version 2.0 (the "License");
 *	you may not use this file except in compliance with the License.
 *	You may obtain a copy of the License at
 *
 *				http://www.apache.org/licenses/LICENSE-2.0
 *
 *	Unless required by applicable law or agreed to in writing, software
 *	distributed under the License is distributed on an "AS IS" BASIS,
 *	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *	See the License for the specific language governing permissions and
 *	limitations under the License.
 */

package org.musicmod.android.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class LyricsDownloader {

	private final static char[] digit = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A',
			'B', 'C', 'D', 'E', 'F' };
	private final static String UTF_8 = "utf-8";

	private static String urlSearch(String artist, String track) {

		return "http://ttlrcct.qianqian.com/dll/lyricsvr.dll?sh?Artist=" + artist + "&Title="
				+ track + "&Flags=0";
	}

	private static String urlDownload(int id, String code) {

		return "http://ttlrcct.qianqian.com/dll/lyricsvr.dll?dl?Id=" + id + "&Code=" + code;
	}

	private List<Integer> mIdList = new ArrayList<Integer>();
	private List<String> mVerifyCodeList = new ArrayList<String>();

	public LyricsDownloader() {

	}

	public interface OnProgressChangeListener {

		void onProgressChange(int progress, int total);
	}

	public void setOnProgressChangeListener(OnProgressChangeListener listener) {

		mListener = listener;
	}

	public void removeOnProgressChangeListener(OnProgressChangeListener listener) {

		mListener = null;
	}

	public OnProgressChangeListener mListener;

	/**
	 * Search lyrics from server
	 * 
	 * @return result list in string array.
	 * 
	 * @param artist
	 *            The artist of sound track.
	 * @param track
	 *            The name of sound track.
	 */
	public String[] search(String artist, String track) throws UnsupportedEncodingException,
			XmlPullParserException, IOException {

		String url = urlSearch(encode(artist), encode(track));
		return parseResult(get(url, UTF_8));
	}

	/**
	 * Download lyrics from server
	 * 
	 * @param id
	 *            Id of selected item.
	 * @param file
	 *            Destination file path.
	 */
	public void download(int id, String path) throws MalformedURLException, IOException {

		download(id, new File(path));
	}

	/**
	 * Download lyrics from server
	 * 
	 * @param id
	 *            Id of selected item.
	 * @param file
	 *            Destination file.
	 */
	public void download(int id, File file) throws MalformedURLException, IOException {

		String url_string = urlDownload(mIdList.get(id), mVerifyCodeList.get(id));

		URL url = new URL(url_string);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");
		connection.setDoOutput(true);
		connection.connect();
		FileOutputStream output = new FileOutputStream(file);
		InputStream input = connection.getInputStream();
		int total_size = connection.getContentLength();
		int downloaded_size = 0;
		byte[] buffer = new byte[1024];
		int buffer_length = 0;
		while ((buffer_length = input.read(buffer)) > 0) {
			output.write(buffer, 0, buffer_length);
			downloaded_size += buffer_length;
			if (mListener != null) {
				mListener.onProgressChange(downloaded_size, total_size);
			}
		}
		output.close();
	}

	private String encode(String source) {

		final String UTF_16LE = "utf-16le";

		if (source == null) {
			source = "";
		}

		source = source.replaceAll("[\\p{P} ]", "").toLowerCase();
		byte[] bytes = null;

		try {
			bytes = source.getBytes(UTF_16LE);
		} catch (Exception e) {
			e.printStackTrace();
			bytes = source.getBytes();
		}

		char[] charactor = new char[2];
		StringBuilder builder = new StringBuilder();
		for (byte byteValue : bytes) {
			charactor[0] = digit[(byteValue >>> 4) & 0X0F];
			charactor[1] = digit[byteValue & 0X0F];
			builder.append(charactor);
		}
		return builder.toString();
	}

	private String verify(String artist, String track, int id) {

		try {
			byte[] bytes = (artist + track).getBytes(UTF_8);
			int[] song = new int[bytes.length];
			for (int i = 0; i < bytes.length; i++) {
				song[i] = bytes[i] & 0xff;
			}
			int intVal1 = 0, intVal2 = 0, intVal3 = 0;
			intVal1 = (id & 0xFF00) >> 8;
			if ((id & 0xFF0000) == 0) {
				intVal3 = 0xFF & ~intVal1;
			} else {
				intVal3 = 0xFF & ((id & 0x00FF0000) >> 16);
			}

			intVal3 = intVal3 | ((0xFF & id) << 8);
			intVal3 = intVal3 << 8;
			intVal3 = intVal3 | (0xFF & intVal1);
			intVal3 = intVal3 << 8;

			if ((id & 0xFF000000) == 0) {
				intVal3 = intVal3 | (0xFF & (~id));
			} else {
				intVal3 = intVal3 | (0xFF & (id >> 24));
			}

			int uBound = bytes.length - 1;
			while (uBound >= 0) {
				int c = song[uBound];
				if (c >= 0x80) c = c - 0x100;
				intVal1 = c + intVal2;
				intVal2 = intVal2 << (uBound % 2 + 4);
				intVal2 = intVal1 + intVal2;
				uBound -= 1;
			}

			uBound = 0;
			intVal1 = 0;

			while (uBound <= bytes.length - 1) {
				int c = song[uBound];
				if (c >= 128) {
					c -= 256;
				}
				int intVal4 = c + intVal1;
				intVal1 = intVal1 << (uBound % 2 + 3);
				intVal1 = intVal1 + intVal4;
				uBound += 1;
			}

			int intVal5 = intVal2 ^ intVal3;
			intVal5 = intVal5 + (intVal1 | id);
			intVal5 = intVal5 * (intVal1 | intVal3);
			intVal5 = intVal5 * (intVal2 ^ id);

			return String.valueOf(intVal5);
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}

	private String[] parseResult(String xml) throws XmlPullParserException, IOException {

		if (xml == null || "".equals(xml)) {
			return new String[] {};
		}

		mIdList = new ArrayList<Integer>();
		mVerifyCodeList = new ArrayList<String>();
		List<String> mNameList = new ArrayList<String>();
		String TAG_RESULT = "result", TAG_LRC = "lrc";
		String ATTR_ID = "id", ATTR_ARTIST = "artist", ATTR_TITLE = "title";
		String verify_code = "", name = "";
		int id = 0;

		XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
		factory.setNamespaceAware(true);

		XmlPullParser parser = factory.newPullParser();
		parser.setInput(new StringReader(xml));

		int eventType = parser.getEventType();
		String tagName;
		boolean lookingForEndOfUnknownTag = false;
		String unknownTagName = null;

		// This loop will skip to the result start tag
		do {
			if (eventType == XmlPullParser.START_TAG) {
				tagName = parser.getName();
				if (TAG_RESULT.equals(tagName)) {
					// Go to next tag
					eventType = parser.next();
					break;
				}
				throw new RuntimeException("Expecting result, got " + tagName);
			}
			eventType = parser.next();
		} while (eventType != XmlPullParser.END_DOCUMENT);

		boolean reachedEndOfResult = false;
		while (!reachedEndOfResult) {
			switch (eventType) {
				case XmlPullParser.START_TAG:
					if (lookingForEndOfUnknownTag) {
						break;
					}
					tagName = parser.getName();
					if (TAG_LRC.equals(tagName)) {
						String artist = parser
								.getAttributeValue(parser.getNamespace(), ATTR_ARTIST);
						String title = parser.getAttributeValue(parser.getNamespace(), ATTR_TITLE);
						id = Integer.valueOf(parser.getAttributeValue(parser.getNamespace(),
								ATTR_ID));

						name = artist + "\n" + title;
						verify_code = verify(artist, title, id);
					} else {
						lookingForEndOfUnknownTag = true;
						unknownTagName = tagName;
					}
					break;
				case XmlPullParser.END_TAG:
					tagName = parser.getName();
					if (lookingForEndOfUnknownTag && tagName.equals(unknownTagName)) {
						lookingForEndOfUnknownTag = false;
						unknownTagName = null;
					} else if (TAG_LRC.equals(tagName)) {
						mNameList.add(name);
						mIdList.add(id);
						mVerifyCodeList.add(verify_code);
					} else if (TAG_RESULT.equals(tagName)) {
						reachedEndOfResult = true;
					}
					break;
			}
			eventType = parser.next();
		}
		return mNameList.toArray(new String[mNameList.size()]);
	}

	// get text from url
	private String get(String url, String encoding) throws UnsupportedEncodingException,
			IOException {

		if (url == null) {
			return null;
		}
		URLConnection conn = new URL(url).openConnection();
		conn.connect();
		String contentType = conn.getContentType();
		if (contentType == null) contentType = encoding;

		Pattern pattern = Pattern.compile("(?i)\\bcharset=([^\\s;]+)");
		Matcher matcher = pattern.matcher(contentType);
		String encoder = encoding;
		if (matcher.find()) {
			encoder = matcher.group(1);
		}

		BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(),
				encoder));
		char[] str = new char[4096];
		StringBuilder builder = new StringBuilder();
		for (int len; (len = reader.read(str)) > -1;) {
			builder.append(str, 0, len);
		}
		return builder.toString();
	}
}
