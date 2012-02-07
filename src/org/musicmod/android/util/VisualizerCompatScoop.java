package org.musicmod.android.util;

import java.lang.reflect.Method;

import org.musicmod.android.util.VisualizerWrapper.OnDataChangedListener;

import android.os.CountDownTimer;

public class VisualizerCompatScoop extends VisualizerCompat {

	private OnDataChangedListener mListener;
	private CountDownTimer mCountDownTimer;
	private boolean mWaveEnabled, mFftEnabled;
	private boolean mVisualizerEnabled;
	private int mAccuracy = 1024;

	public VisualizerCompatScoop(final int audioSessionId, final long duration) {
		super(audioSessionId, duration);
		mCountDownTimer = new CountDownTimer(60000, duration) {

			@Override
			public void onTick(long millisUntilFinished) {

				short[] wave_data = new short[1024];
				short[] fft_data = new short[1024];
				if (mListener != null) {
					if (mWaveEnabled) {
						int len = snoop(wave_data, 0);
						if (len != 0) {
							mListener.onWaveDataChanged(transform(wave_data, 64),
									(int) (((float) len / 1024) * mAccuracy), true);
						}
					}
					if (mFftEnabled) {
						int len = snoop(fft_data, 1);
						if (len != 0) {
							mListener.onFftDataChanged(transform(fft_data, 16),
									(int) (((float) len / 1024) * mAccuracy));
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

	@Override
	public void setFftEnabled(boolean fft) {
		mFftEnabled = fft;

	}

	@Override
	public void setWaveFormEnabled(boolean wave) {
		mWaveEnabled = wave;

	}

	@Override
	public void setOnDataChangedListener(OnDataChangedListener listener) {

		mListener = listener;
	}

	@Override
	public void setEnabled(boolean enabled) {
		if (mCountDownTimer != null) {
			if (enabled) {
				mCountDownTimer.start();
			} else {
				mCountDownTimer.cancel();
			}
		}
		mVisualizerEnabled = enabled;
	}

	@Override
	public boolean getEnabled() {
		return mVisualizerEnabled;
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

	private byte[] transform(short[] orig, int divider) {
		byte[] result = new byte[orig.length];
		for (int i = 0; i < orig.length; i++) {
			short temp = (short) (orig[i] / divider);
			if (temp > Byte.MAX_VALUE) temp = Byte.MAX_VALUE;
			if (temp < Byte.MIN_VALUE) temp = Byte.MIN_VALUE;
			result[i] = (byte) temp;
		}
		return result;
	}

	@Override
	public void release() {

	}

	@Override
	public void setAccuracy(int accuracy) {
		mAccuracy = accuracy;

	}

}
