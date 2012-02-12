package org.musicmod.android.util;

import java.lang.reflect.Method;
import java.util.Timer;
import java.util.TimerTask;

import org.musicmod.android.util.VisualizerWrapper.OnDataChangedListener;

import android.os.Handler;
import android.os.Message;

public class VisualizerCompatScoop extends VisualizerCompat {

	private OnDataChangedListener mListener;
	private boolean mWaveEnabled, mFftEnabled;
	private boolean mVisualizerEnabled;
	private Timer mTimer;

	public VisualizerCompatScoop(int audioSessionId, int fps) {
		super(audioSessionId, fps);
		duration = 1000 / fps;
		mTimer = new Timer();

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
		if (mTimer != null) {
			if (enabled) {
				mTimer = new Timer();
				mTimer.scheduleAtFixedRate(new VisualizerTimer(), 0, duration);
			} else {
				mTimer.cancel();
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
	public void setAccuracy(float accuracy) {
		if (accuracy > 1.0f || accuracy <= 0.0f)
			throw new IllegalArgumentException(
					"Invalid accuracy value! Allowed value range is \"0 < accuracy <= 1.0\"!");
		this.accuracy = accuracy;

	}

	private class VisualizerTimer extends TimerTask {

		@Override
		public void run() {
			short[] wave_data = new short[1024];
			short[] fft_data = new short[1024];
			if (mWaveEnabled) {
				int len = snoop(wave_data, 0);
				if (len != 0) {
					Message msg = new Message();
					msg.what = WAVE_CHANGED;
					msg.obj = new Object[] { wave_data, len };
					mVisualizerHandler.sendMessage(msg);
				}
			}
			if (mFftEnabled) {
				int len = snoop(fft_data, 1);
				if (len != 0) {
					Message msg = new Message();
					msg.what = FFT_CHANGED;
					msg.obj = new Object[] { fft_data, len };
					mVisualizerHandler.sendMessage(msg);
				}
			}

		}

	}

	private Handler mVisualizerHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case WAVE_CHANGED:
					if (mListener != null) {
						short[] wave_data = (short[]) ((Object[]) msg.obj)[0];
						int len = (Integer) ((Object[]) msg.obj)[1];
						mListener.onWaveDataChanged(transform(wave_data, 128),
								(int) (len * accuracy), true);
					}
					break;
				case FFT_CHANGED:
					if (mListener != null) {
						short[] fft_data = (short[]) ((Object[]) msg.obj)[0];
						int len = (Integer) ((Object[]) msg.obj)[1];
						mListener.onFftDataChanged(transform(fft_data, 32), (int) (len * accuracy));
					}
					break;
			}
		}
	};

}
