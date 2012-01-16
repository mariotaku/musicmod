package org.musicmod.android.util;

import java.lang.reflect.Method;

import android.os.CountDownTimer;

public class VisualizerWrapper {

	CountDownTimer mCountDownTimer;
	OnDataChangedListener mListener;

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

	public interface OnDataChangedListener {

		public void onWaveDataChanged(short[] data, int len);

		public void onFftDataChanged(short[] data, int len);
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
