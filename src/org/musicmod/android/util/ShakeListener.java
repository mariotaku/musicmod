/*
 *			  Copyright (C) 2011 The MusicMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *			http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.musicmod.android.util;

import java.util.ArrayList;

import org.musicmod.android.Constants;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.FloatMath;

public class ShakeListener implements SensorEventListener, Constants {

	private boolean mFirstSensorUpdate, mFirstDeltaUpdate = true;
	private long mCurrentTimeStamp, mLastShakeTimeStamp;
	private long mShakeInterval = 250;
	private long mLastUpdateTime;
	private float mLastX, mLastY, mLastZ;
	private float mCurrentSpeed, mLastSpeed;

	private SensorManager mSensorManager;
	private ArrayList<OnShakeListener> mListeners;
	private float mShakeThreshold = DEFAULT_SHAKING_THRESHOLD;

	public float getShakeThreshold() {

		return mShakeThreshold;
	}

	public void setShakeThreshold(float threshold) {

		mShakeThreshold = threshold;
	}

	public ShakeListener(Context context) {

		mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		mListeners = new ArrayList<OnShakeListener>();
	}

	public interface OnShakeListener {

		void onShake();
	}

	public void registerOnShakeListener(OnShakeListener listener) {

		if (!mListeners.contains(listener)) mListeners.add(listener);
	}

	public void unregisterOnShakeListener(OnShakeListener listener) {

		mListeners.remove(listener);
	}

	public void start() throws UnsupportedOperationException {

		Sensor sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		if (sensor == null) {
			throw new UnsupportedOperationException();
		}
		boolean success = mSensorManager.registerListener(this, sensor,
				SensorManager.SENSOR_DELAY_UI);
		if (!success) {
			throw new UnsupportedOperationException();
		}
		mFirstSensorUpdate = true;
	}

	public void stop() {

		if (mSensorManager != null) mSensorManager.unregisterListener(this);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}

	@Override
	public void onSensorChanged(SensorEvent event) {

		mCurrentTimeStamp = event.timestamp / 1000 / 1000;

		float eventX = event.values[SensorManager.DATA_X], eventY = event.values[SensorManager.DATA_Y], eventZ = event.values[SensorManager.DATA_Z];

		if (mFirstSensorUpdate) {
			mLastX = eventX;
			mLastY = eventY;
			mLastZ = eventZ;
			mLastUpdateTime = mCurrentTimeStamp;
			mFirstDeltaUpdate = true;
			mFirstSensorUpdate = false;
			return;
		}

		float deltaX = eventX - mLastX, deltaY = eventY - mLastY, deltaZ = eventZ - mLastZ;

		mLastX = eventX;
		mLastY = eventY;
		mLastZ = eventZ;

		float deltaTimeForSpeed = mCurrentTimeStamp - mLastUpdateTime;

		mLastUpdateTime = mCurrentTimeStamp;

		if (mFirstDeltaUpdate) {
			mCurrentSpeed = FloatMath.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ)
					/ deltaTimeForSpeed;
			mFirstDeltaUpdate = false;
			return;
		}

		mLastSpeed = mCurrentSpeed;

		mCurrentSpeed = FloatMath.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ)
				/ deltaTimeForSpeed;

		float delta = Math.abs(mLastSpeed - mCurrentSpeed) / deltaTimeForSpeed * 1000 * 1000;

		if (delta > mShakeThreshold) {
			if (mCurrentTimeStamp > mLastShakeTimeStamp + mShakeInterval) {
				mLastShakeTimeStamp = mCurrentTimeStamp;
				notifyListeners();
			}
		}
	}

	private void notifyListeners() {

		for (OnShakeListener listener : mListeners) {
			listener.onShake();
		}
	}

}
