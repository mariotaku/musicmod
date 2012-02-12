package org.musicmod.android.util;

import java.util.Timer;
import java.util.TimerTask;

import org.musicmod.android.util.VisualizerWrapper.OnDataChangedListener;

import android.media.audiofx.Visualizer;
import android.os.Handler;
import android.os.Message;

public class VisualizerCompatAudioFX extends VisualizerCompat {

	private OnDataChangedListener mListener;
	private Timer mTimer;
	private boolean mWaveEnabled, mFftEnabled;
	private Visualizer mVisualizer;

	public VisualizerCompatAudioFX(int audioSessionId, int fps) {
		super(audioSessionId, fps);
		mVisualizer = new Visualizer(audioSessionId);
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
	public void setAccuracy(float accuracy) {
		if (accuracy > 1.0f || accuracy <= 0.0f)
			throw new IllegalArgumentException(
					"Invalid accuracy value! Allowed value range is \"0 < accuracy <= 1.0\"!");
		mVisualizer.setCaptureSize((int) (Visualizer.getMaxCaptureRate() * accuracy));
	}

	@Override
	public void setEnabled(boolean enabled) {
		mVisualizer.setEnabled(enabled);
		if (mTimer != null) {
			if (enabled) {
				mTimer = new Timer();
				mTimer.scheduleAtFixedRate(new VisualizerTimer(), 0, duration);
			} else {
				mTimer.cancel();
			}
		}
	}

	@Override
	public boolean getEnabled() {
		return mVisualizer.getEnabled();
	}

	@Override
	public void release() {
		mVisualizer.setEnabled(false);
		mVisualizer.release();

	}

	private class VisualizerTimer extends TimerTask {

		@Override
		public void run() {
			byte[] wave_data = new byte[mVisualizer.getCaptureSize()];
			byte[] fft_data = new byte[mVisualizer.getCaptureSize()];
			if (mWaveEnabled) {
				int ret = mVisualizer.getWaveForm(wave_data);
				if (ret == Visualizer.SUCCESS && wave_data != null) {
					Message msg = new Message();
					msg.what = WAVE_CHANGED;
					msg.obj = new Object[] { wave_data, wave_data.length };
					mVisualizerHandler.sendMessage(msg);
				}
			}
			if (mFftEnabled) {
				int ret = mVisualizer.getFft(fft_data);
				if (ret == Visualizer.SUCCESS && fft_data != null) {
					Message msg = new Message();
					msg.what = FFT_CHANGED;
					msg.obj = new Object[] { fft_data, fft_data.length };
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
						byte[] wave_data = (byte[]) ((Object[]) msg.obj)[0];
						int len = (Integer) ((Object[]) msg.obj)[1];
						mListener.onWaveDataChanged(wave_data, (int) (len * accuracy), false);
					}
					break;
				case FFT_CHANGED:
					if (mListener != null) {
						byte[] fft_data = (byte[]) ((Object[]) msg.obj)[0];
						int len = (Integer) ((Object[]) msg.obj)[1];
						mListener.onFftDataChanged(fft_data, (int) (len * accuracy));
					}
					break;
			}
		}
	};

}
