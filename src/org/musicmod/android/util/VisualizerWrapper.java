package org.musicmod.android.util;

import java.lang.reflect.Method;

import android.media.audiofx.Visualizer;
import android.os.CountDownTimer;

public class VisualizerWrapper {

	CountDownTimer mCountDownTimer;
	OnDataChangedListener mListener;

	public static boolean isScoopSupported() {
		try {
			Class.forName("android.media.MediaPlayer").getMethod("snoop", short[].class, int.class);
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	public static boolean isAudioFXSupported() {
		try {
			Class.forName("android.media.audiofx.Visualizer");
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	public VisualizerWrapper(long duration, final boolean wave, final boolean fft) {

		mCountDownTimer = new CountDownTimer(60000, duration) {

			@Override
			public void onTick(long millisUntilFinished) {

				short[] wave_data = new short[1024];
				short[] fft_data = new short[1024];
				if (mListener != null) {
					if (wave) {
						int len = snoop(wave_data, 0);
						if (len != 0) {
							mListener.onWaveDataChanged(wave_data, len);
						}
					}
					if (fft) {
						int len = snoop(fft_data, 1);
						if (len != 0) {
							mListener.onFftDataChanged(fft_data, len);
						}
					}
				}
			}

			@Override
			public void onFinish() {

				start();
			}
		};
	}

	public VisualizerWrapper(long duration, final boolean wave, final boolean fft,
			final int audioSessionId) {

		final Visualizer mVisualizer = new Visualizer(audioSessionId);
		mVisualizer.setEnabled(true);

		mCountDownTimer = new CountDownTimer(60000, duration) {

			@Override
			public void onTick(long millisUntilFinished) {

				byte[] wave_data = new byte[mVisualizer.getCaptureSize()];
				byte[] fft_data = new byte[mVisualizer.getCaptureSize()];
				if (mListener != null) {
					if (wave) {
						int ret = mVisualizer.getWaveForm(wave_data);
						if (ret == Visualizer.SUCCESS && wave_data != null) {
							mListener.onWaveDataChanged(wave_data, wave_data.length);
						}
					}
					if (fft) {
						int ret = mVisualizer.getFft(fft_data);
						if (ret == Visualizer.SUCCESS && fft_data != null) {
							mListener.onFftDataChanged(fft_data, fft_data.length);
						}
					}
				}
			}

			@Override
			public void onFinish() {

				start();
			}

		};
	}

	public interface OnDataChangedListener {

		public void onWaveDataChanged(short[] data, int len);

		public void onWaveDataChanged(byte[] data, int len);

		public void onFftDataChanged(short[] data, int len);

		public void onFftDataChanged(byte[] data, int len);
	}

	public void start() {

		if (mCountDownTimer != null) {
			mCountDownTimer.start();
		}
	}

	public void stop() {

		if (mCountDownTimer != null) {
			mCountDownTimer.cancel();
		}
	}

	public void setOnDataChangedListener(OnDataChangedListener listener) {

		mListener = listener;
	}

	private int snoop(short[] outData, int kind) {

		try {
			Method m = Class.forName("android.media.MediaPlayer").getMethod("snoop",
					outData.getClass(), Integer.TYPE);
			m.setAccessible(true);
			return (Integer) m.invoke(Class.forName("android.media.MediaPlayer"), outData, kind);
		} catch (Exception e) {
			return 0;
		}
	}
}
