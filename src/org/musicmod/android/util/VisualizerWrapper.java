package org.musicmod.android.util;

public class VisualizerWrapper {

	public static VisualizerCompat getInstance(int audioSessionId, long duration) {
		if (isAudioFXSupported()) {
			return new VisualizerCompatAudioFX(audioSessionId, duration);
		} else if (isScoopSupported()) {
			return new VisualizerCompatScoop(audioSessionId, duration);
		}
		return null;
	}

	private static boolean isScoopSupported() {
		try {
			Class.forName("android.media.MediaPlayer").getMethod("snoop", short[].class, int.class);
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	private static boolean isAudioFXSupported() {
		try {
			Class.forName("android.media.audiofx.Visualizer");
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	public interface OnDataChangedListener {

		public void onWaveDataChanged(byte[] data, int len, boolean scoop);

		public void onFftDataChanged(byte[] data, int len);
	}

}
