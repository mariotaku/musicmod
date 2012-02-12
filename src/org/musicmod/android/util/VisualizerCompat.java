package org.musicmod.android.util;

import org.musicmod.android.util.VisualizerWrapper.OnDataChangedListener;

public abstract class VisualizerCompat {

	public final static int WAVE_CHANGED = 0;
	public final static int FFT_CHANGED = 1;
	public float accuracy = 1.0f;
	public long duration = 50;

	public VisualizerCompat(int audioSessionId, int fps) {

	}

	public abstract void setFftEnabled(boolean fft);

	public abstract void setWaveFormEnabled(boolean wave);

	public abstract void setOnDataChangedListener(OnDataChangedListener listener);

	public abstract void setEnabled(boolean enabled);

	public abstract boolean getEnabled();

	public abstract void release();

	public abstract void setAccuracy(float accuracy);

}
