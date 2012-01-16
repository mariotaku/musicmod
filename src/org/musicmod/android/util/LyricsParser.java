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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mozilla.universalchardet.UniversalDetector;
import org.musicmod.android.Constants;

public class LyricsParser implements Constants {

	private HashMap<Long, String> lyricsMap = new HashMap<Long, String>();
	private ArrayList<Long> mTimestampList = new ArrayList<Long>();
	private ArrayList<String> mLyricsList = new ArrayList<String>();
	private long offset = 0;

	public LyricsParser() {

	}

	private void parseLyricsString(String lyrics) {

		Pattern pattern, patternOffset, patternTimestamp;

		// lyrics offset tag
		patternOffset = Pattern.compile("\\[offset:(\\d+)\\]", Pattern.CASE_INSENSITIVE);
		Matcher matcherOffset = patternOffset.matcher(lyrics);
		if (matcherOffset.find()) {
			offset = Long.valueOf(matcherOffset.group(1));
		}

		// lyrics timestamp tag
		pattern = Pattern.compile("^(\\[[0-9:\\.\\[\\]]+\\])+(.*)$", Pattern.MULTILINE);

		// split each timestamp tag
		patternTimestamp = Pattern.compile("\\[(\\d+):([0-9\\.]+)\\]");
		Matcher matcher = pattern.matcher(lyrics);
		while (matcher.find()) {
			Matcher matcherTimestamp = patternTimestamp.matcher(matcher.group(1));
			while (matcherTimestamp.find()) {
				String content = matcher.group(2).trim();
				if (content.length() == 0) continue;
				long timestamp = Long.valueOf(matcherTimestamp.group(1)) * 60000
						+ (long) (Float.valueOf(matcherTimestamp.group(2)) * 1000) + offset;
				mTimestampList.add(timestamp);
				lyricsMap.put(timestamp, content);
			}
		}
		// Sort timestamp tag
		Collections.sort(mTimestampList);

		for (int i = 0; i < mTimestampList.size(); i++) {
			mLyricsList.add(lyricsMap.get(mTimestampList.get(i)));
		}

	}

	public int parseLyrics(String path) {

		return parseLyrics(new File(path));
	}

	public int parseLyrics(File file) {

		lyricsMap = new HashMap<Long, String>();
		mTimestampList = new ArrayList<Long>();
		mLyricsList = new ArrayList<String>();

		if (!file.exists()) {
			return LYRICS_STATUS_NOT_FOUND;
		}

		try {
			BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
			ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
			byte[] temp = new byte[1024];
			int size = 0;
			while ((size = in.read(temp)) != -1) {
				out.write(temp, 0, size);
			}
			in.close();
			byte[] content = out.toByteArray();
			String encoding;
			UniversalDetector detector = new UniversalDetector(null);
			detector.handleData(content, 0, content.length);
			detector.dataEnd();
			encoding = detector.getDetectedCharset();
			if (encoding == null) encoding = "UTF-8";

			parseLyricsString(new String(content, 0, content.length, encoding));
		} catch (IOException e) {
			e.printStackTrace();
			return LYRICS_STATUS_INVALID;
		}
		if (lyricsMap.size() == 0) {
			return LYRICS_STATUS_INVALID;
		}
		return LYRICS_STATUS_OK;
	}

	public ArrayList<String> getAllLyrics() {

		return mLyricsList;
	}

	public ArrayList<Long> getAllTimestamp() {

		return mTimestampList;
	}

	public int getId(long timestamp) {

		for (int i = mTimestampList.size() - 1; i >= 0; i--) {

			if (mTimestampList.get(i) <= timestamp) {
				return i;
			}

		}

		return 0;
	}

	public long getTimestamp(int id) {

		if (mTimestampList.size() > 0) {
			if (id >= mTimestampList.size()) {
				return mTimestampList.get(mTimestampList.size() - 1);
			}
			if (id < 0) {
				return 0;
			}
			return mTimestampList.get(id);
		}
		return 0;
	}
}
