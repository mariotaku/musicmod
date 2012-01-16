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

import android.graphics.Paint;

public class LyricsSplitter {

	public static String split(String line, float pixels) {

		if (measureString(line, pixels) <= 294)
			return line;
		else {
			int half = line.length() / 2;
			for (int i = 0; i < half / 2; i++) {
				int pos = half - i;
				char c = line.charAt(pos);
				if (c == ' ' || c == '\u3000') {
					return line.substring(0, pos).trim() + "\n"
							+ line.substring(pos + 1, line.length()).trim();
				} else if (c == '(' || c == '<' || c == '[' || c == '{' || c == '\uFF08'
						|| c == '\u3010' || c == '\u3016' || c == '\u300C' || c == '/') {
					return line.substring(0, pos).trim() + "\n"
							+ line.substring(pos, line.length()).trim();
				} else if (c == ')' || c == '>' || c == ']' || c == '}' || c == '\uFF09'
						|| c == '\u3011' || c == '\u3017' || c == '\u300D' || c == ','
						|| c == '\uFF0C' || c == '\u3002') {
					return line.substring(0, pos + 1).trim() + "\n"
							+ line.substring(pos + 1, line.length()).trim();
				}
				pos = half + i + 1;
				c = line.charAt(pos);
				if (c == ' ' || c == '\u3000') {
					return line.substring(0, pos).trim() + "\n"
							+ line.substring(pos + 1, line.length()).trim();
				} else if (c == '(' || c == '<' || c == '[' || c == '{' || c == '\uFF08'
						|| c == '\u3010' || c == '\u3016' || c == '\u300C' || c == '/') {
					return line.substring(0, pos).trim() + "\n"
							+ line.substring(pos, line.length()).trim();
				} else if (c == ')' || c == '>' || c == ']' || c == '}' || c == '\uFF09'
						|| c == '\u3011' || c == '\u3017' || c == '\u300D' || c == ','
						|| c == '\uFF0C' || c == '\u3002') {
					return line.substring(0, pos + 1).trim() + "\n"
							+ line.substring(pos + 1, line.length()).trim();
				}
			}
			return line.substring(0, half) + "\n" + line.substring(half, line.length());
		}
	}

	private static float measureString(String line, float pixels) {

		Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mTextPaint.setTextSize(pixels);
		return mTextPaint.measureText(line);
	}
}