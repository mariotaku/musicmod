/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.musicmod.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;

/**
 * 
 */
public class MediaButtonIntentReceiver extends BroadcastReceiver implements Constants {

	private static final int MSG_PRESSED = 1, MSG_TIMEOUT = 2;
	private static final long CLICK_DELAY = 300, LONG_PRESS_DELAY = 1000;
	private static final int SINGLE_CLICK = 1, DOUBLE_CLICK = 2, TRIPLE_CLICK = 3;

	private static boolean mPressedDown, mLongPressed = false;
	private static long mFirstTime;
	private static int mPressedCount = 0;

	@Override
	public void onReceive(Context context, Intent intent) {

		String intentAction = intent.getAction();
		if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intentAction)) {
			Intent i = new Intent(context, MusicPlaybackService.class);
			i.setAction(SERVICECMD);
			i.putExtra(CMDNAME, CMDPAUSE);
			context.startService(i);
		} else if (Intent.ACTION_MEDIA_BUTTON.equals(intentAction)) {
			KeyEvent event = (KeyEvent) intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);

			if (event == null) {
				return;
			}

			int keycode = event.getKeyCode();
			int action = event.getAction();
			long eventtime = event.getEventTime();

			switch (keycode) {
				case KeyEvent.KEYCODE_MEDIA_STOP:
					sendMediaCommand(context, CMDSTOP);
					break;
				case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
					sendMediaCommand(context, CMDTOGGLEPAUSE);
					break;
				case KeyEvent.KEYCODE_MEDIA_NEXT:
					sendMediaCommand(context, CMDNEXT);
					break;
				case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
					sendMediaCommand(context, CMDPREVIOUS);
					break;
				case KeyEvent.KEYCODE_HEADSETHOOK:
					processHeadsetHookEvent(context, action, eventtime);
					break;
			}
		}
	}

	private void sendMediaCommand(Context context, String command) {
		Intent i = new Intent(context, MusicPlaybackService.class);
		i.setAction(SERVICECMD);
		i.putExtra(CMDNAME, command);
		context.startService(i);
	}

	private void processHeadsetHookEvent(Context context, int action, long eventtime) {
		switch (action) {
			case KeyEvent.ACTION_DOWN:
				if (!mPressedDown) {
					mPressedDown = true;
					mFirstTime = eventtime;
					mHandler.sendEmptyMessage(MSG_PRESSED);
				} else if (!mLongPressed) {
					if (eventtime - mFirstTime >= LONG_PRESS_DELAY) {
						mPressedCount = 0;
						mLongPressed = true;
						sendMediaCommand(context, CMDTOGGLEFAVORITE);
						mHandler.removeCallbacksAndMessages(null);
					}
				}
				break;
			case KeyEvent.ACTION_UP:
				if (!mLongPressed) {
					mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_TIMEOUT, context),
							CLICK_DELAY);
				}
				mPressedDown = false;
				mLongPressed = false;
				break;
			default:
				break;
		}
	}

	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {

			switch (msg.what) {
				case MSG_PRESSED:
					mHandler.removeMessages(MSG_TIMEOUT);
					if (mPressedCount < Integer.MAX_VALUE) mPressedCount++;
					break;
				case MSG_TIMEOUT:
					mHandler.removeCallbacksAndMessages(null);
					switch (mPressedCount) {
						case SINGLE_CLICK:
							sendMediaCommand((Context) msg.obj, CMDTOGGLEPAUSE);
							break;
						case DOUBLE_CLICK:
							sendMediaCommand((Context) msg.obj, CMDNEXT);
							break;
						case TRIPLE_CLICK:
							sendMediaCommand((Context) msg.obj, CMDPREVIOUS);
							break;
					}
					mPressedCount = 0;
					break;
			}

		}
	};
}
