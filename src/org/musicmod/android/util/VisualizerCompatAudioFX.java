package org.musicmod.android.util;

import org.musicmod.android.util.VisualizerWrapper.OnDataChangedListener;

import android.media.audiofx.Visualizer;
import android.os.CountDownTimer;

public class VisualizerCompatAudioFX extends VisualizerCompat {

	private OnDataChangedListener mListener;
	private CountDownTimer mCountDownTimer;
	private boolean mWaveEnabled, mFftEnabled;
	private Visualizer mVisualizer;

	public VisualizerCompatAudioFX(final int audioSessionId, final long duration) {
		super(audioSessionId, duration);
		mVisualizer = new Visualizer(audioSessionId);

		mCountDownTimer = new CountDownTimer(60000, duration) {

			@Override
			public void onTick(long millisUntilFinished) {

				byte[] wave_data = new byte[mVisualizer.getCaptureSize()];
				byte[] fft_data = new byte[mVisualizer.getCaptureSize()];
				if (mListener != null) {
					if (mWaveEnabled) {
						int ret = mVisualizer.getWaveForm(wave_data);
						if (ret == Visualizer.SUCCESS && wave_data != null) {
							mListener.onWaveDataChanged(wave_data, wave_data.length, false);
						}
					}
					if (mFftEnabled) {
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
	public void setAccuracy(int accuracy) {
		mVisualizer.setCaptureSize(accuracy);
	}

	@Override
	public void setEnabled(boolean enabled) {
		mVisualizer.setEnabled(enabled);
		if (mCountDownTimer != null) {
			if (enabled) {
				mCountDownTimer.start();
			} else {
				mCountDownTimer.cancel();
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

}
