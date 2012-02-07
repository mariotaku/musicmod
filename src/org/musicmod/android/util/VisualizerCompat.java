package org.musicmod.android.util;

import org.musicmod.android.util.VisualizerWrapper.OnDataChangedListener;

public abstract class VisualizerCompat {

	public VisualizerCompat(final int audioSessionId, final long duration) {

	}

	public abstract void setFftEnabled(boolean fft);

	public abstract void setWaveFormEnabled(boolean wave);

	public abstract void setOnDataChangedListener(OnDataChangedListener listener);

	public abstract void setEnabled(boolean enabled);

	public abstract boolean getEnabled();

	public abstract void release();

	public abstract void setAccuracy(int accuracy);

}
