package org.musicmod.android.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import android.graphics.Bitmap;
import android.graphics.Color;

public class ColorAnalyser {

	public static int analyse(Bitmap bitmap) {

		return analyse(bitmap, 18, 28);
	}

	public static int analyse(Bitmap bitmap, int sampleX, int sampleY) {

		if (bitmap == null) {
			return Color.WHITE;
		}

		int color = 0;

		HashMap<Float, Integer> colorsMap = new HashMap<Float, Integer>();
		ArrayList<Float> colorsScore = new ArrayList<Float>();

		Bitmap resized = Bitmap.createScaledBitmap(bitmap, sampleX, sampleY, false);

		for (int y = 0; y < resized.getHeight(); y++) {
			for (int x = 0; x < resized.getWidth(); x++) {
				color = resized.getPixel(x, y);
				float[] hsv = new float[3];
				Color.colorToHSV(color, hsv);

				float score = (hsv[1] * hsv[1] * hsv[2] * hsv[2]);

				colorsMap.put(score, color);
				colorsScore.add(score);
			}
		}

		Collections.sort(colorsScore);
		bitmap.recycle();
		resized.recycle();
		return colorsMap.get(colorsScore.get(colorsScore.size() - 1));

	}
}