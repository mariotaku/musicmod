package org.musicmod.android.util;

import android.util.Log;

/**
 * An Equalizer is used to alter the frequency response of a particular music
 * source or of the main output mix.
 * <p>
 * An application creates an Equalizer object to instantiate and control an
 * Equalizer engine in the audio framework. The application can either simply
 * use predefined presets or have a more precise control of the gain in each
 * frequency band controlled by the equalizer.
 * <p>
 * The methods, parameter types and units exposed by the Equalizer
 * implementation are directly mapping those defined by the OpenSL ES 1.0.1
 * Specification (http://www.khronos.org/opensles/) for the SLEqualizerItf
 * interface. Please refer to this specification for more details.
 * <p>
 * To attach the Equalizer to a particular AudioTrack or MediaPlayer, specify
 * the audio session ID of this AudioTrack or MediaPlayer when constructing the
 * Equalizer. If the audio session ID 0 is specified, the Equalizer applies to
 * the main audio output mix.
 * <p>
 * Creating an Equalizer on the output mix (audio session 0) requires permission
 * {@link android.Manifest.permission#MODIFY_AUDIO_SETTINGS}
 * <p>
 * See {@link android.media.MediaPlayer#getAudioSessionId()} for details on
 * audio sessions.
 * <p>
 * See {@link android.media.audiofx.AudioEffect} class for more details on
 * controlling audio effects.
 * 
 * @author mariotaku
 * 
 */
public class EqualizerWrapper {

	private Object mEqualizer = null;

	/**
	 * Detect whether equalizer is supported.
	 * 
	 * @return whether equalizer is supported.
	 */
	public static boolean isSupported() {

		try {
			Class.forName("android.media.audiofx.Equalizer");
		} catch (Exception e) {
			Log.w("EuqalizerWrapper", "Equalizer is not supported!");
			return false;
		}
		return true;
	}

	/**
	 * Class constructor.
	 * 
	 * @param priority
	 *            the priority level requested by the application for
	 *            controlling the Equalizer engine. As the same engine can be
	 *            shared by several applications, this parameter indicates how
	 *            much the requesting application needs control of effect
	 *            parameters. The normal priority is 0, above normal is a
	 *            positive number, below normal a negative number.
	 * @param audioSession
	 *            system wide unique audio session identifier. The Equalizer
	 *            will be attached to the MediaPlayer or AudioTrack in the same
	 *            audio session.
	 * 
	 * @throws RuntimeException
	 * @throws UnsupportedOperationException
	 * @throws IllegalStateException
	 * @throws IllegalArgumentException
	 */
	public EqualizerWrapper(int priority, int audioSession) {

		try {
			mEqualizer = Class.forName("android.media.audiofx.Equalizer")
					.getConstructor(new Class[] { int.class, int.class })
					.newInstance(new Object[] { priority, audioSession });
		} catch (Exception e) {
			if (e.getCause() == null) {
				e.printStackTrace();
			} else {
				e.getCause().printStackTrace();
			}
		}
	}

	/**
	 * Gets the number of frequency bands supported by the Equalizer engine.
	 * 
	 * @return the number of bands
	 * @throws IllegalStateException
	 * @throws IllegalArgumentException
	 * @throws UnsupportedOperationException
	 */
	public short getNumberOfBands() {

		try {
			return (Short) mEqualizer.getClass().getMethod("getNumberOfBands", new Class[] {})
					.invoke(mEqualizer, new Object[] {});
		} catch (Exception e) {
			if (e.getCause() == null) {
				e.printStackTrace();
			} else {
				e.getCause().printStackTrace();
			}
		}
		return 0;
	}

	/**
	 * Gets the level range for use by {@link #setBandLevel(short,short)}. The
	 * level is expressed in milliBel.
	 * 
	 * @return the band level range in an array of short integers. The first
	 *         element is the lower limit of the range, the second element the
	 *         upper limit.
	 * @throws IllegalStateException
	 * @throws IllegalArgumentException
	 * @throws UnsupportedOperationException
	 */
	public short[] getBandLevelRange() {

		try {
			return (short[]) mEqualizer.getClass().getMethod("getBandLevelRange", new Class[] {})
					.invoke(mEqualizer, new Object[] {});
		} catch (Exception e) {
			if (e.getCause() == null) {
				e.printStackTrace();
			} else {
				e.getCause().printStackTrace();
			}
		}
		return null;
	}

	/**
	 * Sets the given equalizer band to the given gain value.
	 * 
	 * @param band
	 *            frequency band that will have the new gain. The numbering of
	 *            the bands starts from 0 and ends at (number of bands - 1).
	 * @param level
	 *            new gain in millibels that will be set to the given band.
	 *            getBandLevelRange() will define the maximum and minimum
	 *            values.
	 * @throws IllegalStateException
	 * @throws IllegalArgumentException
	 * @throws UnsupportedOperationException
	 * @see #getNumberOfBands()
	 */
	public void setBandLevel(short band, short level) {

		try {
			mEqualizer.getClass()
					.getMethod("setBandLevel", new Class[] { short.class, short.class })
					.invoke(mEqualizer, new Object[] { band, level });
		} catch (Exception e) {
			if (e.getCause() == null) {
				e.printStackTrace();
			} else {
				e.getCause().printStackTrace();
			}
		}
	}

	/**
	 * Gets the gain set for the given equalizer band.
	 * 
	 * @param band
	 *            frequency band whose gain is requested. The numbering of the
	 *            bands starts from 0 and ends at (number of bands - 1).
	 * @return the gain in millibels of the given band.
	 * @throws IllegalStateException
	 * @throws IllegalArgumentException
	 * @throws UnsupportedOperationException
	 */
	public short getBandLevel(short band) {

		try {
			return (Short) mEqualizer.getClass()
					.getMethod("getBandLevel", new Class[] { short.class })
					.invoke(mEqualizer, new Object[] { band });
		} catch (Exception e) {
			if (e.getCause() == null) {
				e.printStackTrace();
			} else {
				e.getCause().printStackTrace();
			}
		}
		return 0;
	}

	/**
	 * Gets the center frequency of the given band.
	 * 
	 * @param band
	 *            frequency band whose center frequency is requested. The
	 *            numbering of the bands starts from 0 and ends at (number of
	 *            bands - 1).
	 * @return the center frequency in milliHertz
	 * @throws IllegalStateException
	 * @throws IllegalArgumentException
	 * @throws UnsupportedOperationException
	 */
	public int getCenterFreq(short band) {

		try {
			return (Integer) mEqualizer.getClass()
					.getMethod("getCenterFreq", new Class[] { short.class })
					.invoke(mEqualizer, new Object[] { band });
		} catch (Exception e) {
			if (e.getCause() == null) {
				e.printStackTrace();
			} else {
				e.getCause().printStackTrace();
			}
		}
		return 0;
	}

	/**
	 * Gets the frequency range of the given frequency band.
	 * 
	 * @param band
	 *            frequency band whose frequency range is requested. The
	 *            numbering of the bands starts from 0 and ends at (number of
	 *            bands - 1).
	 * @return the frequency range in millHertz in an array of integers. The
	 *         first element is the lower limit of the range, the second element
	 *         the upper limit.
	 * @throws IllegalStateException
	 * @throws IllegalArgumentException
	 * @throws UnsupportedOperationException
	 */
	public int[] getBandFreqRange(short band) {

		try {
			return (int[]) mEqualizer.getClass()
					.getMethod("getBandFreqRange", new Class[] { short.class })
					.invoke(mEqualizer, new Object[] { band });
		} catch (Exception e) {
			if (e.getCause() == null) {
				e.printStackTrace();
			} else {
				e.getCause().printStackTrace();
			}
		}
		return null;
	}

	/**
	 * Gets the band that has the most effect on the given frequency.
	 * 
	 * @param frequency
	 *            frequency in milliHertz which is to be equalized via the
	 *            returned band.
	 * @return the frequency band that has most effect on the given frequency.
	 * @throws IllegalStateException
	 * @throws IllegalArgumentException
	 * @throws UnsupportedOperationException
	 */
	public short getBand(int frequency) {

		try {
			return (Short) mEqualizer.getClass().getMethod("getBand", new Class[] { int.class })
					.invoke(mEqualizer, new Object[] { frequency });
		} catch (Exception e) {
			if (e.getCause() == null) {
				e.printStackTrace();
			} else {
				e.getCause().printStackTrace();
			}
		}
		return 0;
	}

	/**
	 * Gets current preset.
	 * 
	 * @return the preset that is set at the moment.
	 * @throws IllegalStateException
	 * @throws IllegalArgumentException
	 * @throws UnsupportedOperationException
	 */
	public short getCurrentPreset() {

		try {
			return (Short) mEqualizer.getClass().getMethod("getCurrentPreset", new Class[] {})
					.invoke(mEqualizer, new Object[] {});
		} catch (Exception e) {
			if (e.getCause() == null) {
				e.printStackTrace();
			} else {
				e.getCause().printStackTrace();
			}
		}
		return 0;
	}

	/**
	 * Sets the equalizer according to the given preset.
	 * 
	 * @param preset
	 *            new preset that will be taken into use. The valid range is [0,
	 *            number of presets-1].
	 * @throws IllegalStateException
	 * @throws IllegalArgumentException
	 * @throws UnsupportedOperationException
	 * @see #getNumberOfPresets()
	 */
	public void usePreset(short preset) {

		try {
			mEqualizer.getClass().getMethod("usePreset", new Class[] { short.class })
					.invoke(mEqualizer, new Object[] { preset });
		} catch (Exception e) {
			if (e.getCause() == null) {
				e.printStackTrace();
			} else {
				e.getCause().printStackTrace();
			}
		}
	}

	/**
	 * Gets the total number of presets the equalizer supports. The presets will
	 * have indices [0, number of presets-1].
	 * 
	 * @return the number of presets the equalizer supports.
	 * @throws IllegalStateException
	 * @throws IllegalArgumentException
	 * @throws UnsupportedOperationException
	 */
	public short getNumberOfPresets() {

		try {
			return (Short) mEqualizer.getClass().getMethod("getNumberOfPresets", new Class[] {})
					.invoke(mEqualizer, new Object[] {});
		} catch (Exception e) {
			if (e.getCause() == null) {
				e.printStackTrace();
			} else {
				e.getCause().printStackTrace();
			}
		}
		return 0;
	}

	/**
	 * Gets the preset name based on the index.
	 * 
	 * @param preset
	 *            index of the preset. The valid range is [0, number of
	 *            presets-1].
	 * @return a string containing the name of the given preset.
	 * @throws IllegalStateException
	 * @throws IllegalArgumentException
	 * @throws UnsupportedOperationException
	 */
	public String getPresetName(short preset) {

		try {
			return (String) mEqualizer.getClass()
					.getMethod("getPresetName", new Class[] { short.class })
					.invoke(mEqualizer, new Object[] { preset });
		} catch (Exception e) {
			if (e.getCause() == null) {
				e.printStackTrace();
			} else {
				e.getCause().printStackTrace();
			}
		}
		return null;
	}

	/**
	 * Enable or disable the effect. Creating an audio effect does not
	 * automatically apply this effect on the audio source. It creates the
	 * resources necessary to process this effect but the audio signal is still
	 * bypassed through the effect engine. Calling this method will make that
	 * the effect is actually applied or not to the audio content being played
	 * in the corresponding audio session.
	 * 
	 * @param enabled
	 *            the requested enable state
	 * @return {@link android.media.audiofx.AudioEffect#SUCCESS} in case of
	 *         success,
	 *         {@link android.media.audiofx.AudioEffect#ERROR_INVALID_OPERATION}
	 *         or {@link android.media.audiofx.AudioEffect#ERROR_DEAD_OBJECT} in
	 *         case of failure.
	 * @throws IllegalStateException
	 */
	public int setEnabled(boolean enabled) {

		try {
			return (Integer) mEqualizer.getClass()
					.getMethod("setEnabled", new Class[] { boolean.class })
					.invoke(mEqualizer, new Object[] { enabled });
		} catch (Exception e) {
			if (e.getCause() == null) {
				e.printStackTrace();
			} else {
				e.getCause().printStackTrace();
			}
		}
		return -1;
	}

	/**
	 * Releases the native AudioEffect resources. It is a good practice to
	 * release the effect engine when not in use as control can be returned to
	 * other applications or the native resources released.
	 */
	public void release() {

		try {
			mEqualizer.getClass().getMethod("release", new Class[] {})
					.invoke(mEqualizer, new Object[] {});
		} catch (Exception e) {
			if (e.getCause() == null) {
				e.printStackTrace();
			} else {
				e.getCause().printStackTrace();
			}
		}
	}

}