/*
 * Copyright (C) 2008 The Android Open Source Project
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

package org.musicmod.android.dialog;

import org.musicmod.android.R;
import org.musicmod.android.app.MusicBrowserActivity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;

public class ScanningProgress extends FragmentActivity {

	private DialogFragment mFragment;

	@Override
	public void onCreate(Bundle icicle) {

		super.onCreate(icicle);
		setVolumeControlStream(AudioManager.STREAM_MUSIC);

		mFragment = new ScanningDialogFragment();
		mFragment.show(getSupportFragmentManager(), "scanning_dialog");

	}

	@Override
	public void onStart() {
		super.onStart();
		registerReceiver(mMountStateReceiver, new IntentFilter(Intent.ACTION_MEDIA_MOUNTED));
	}

	@Override
	public void onStop() {
		unregisterReceiver(mMountStateReceiver);
		super.onStop();
	}

	private BroadcastReceiver mMountStateReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (Intent.ACTION_MEDIA_MOUNTED.equals(intent.getAction())) {
				startActivity(new Intent(getApplicationContext(), MusicBrowserActivity.class));
				finish();
			}
		}

	};

	public static class ScanningDialogFragment extends DialogFragment implements OnClickListener,
			OnCancelListener {

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {

			String mount_state = Environment.getExternalStorageState();
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

			builder.setIcon(android.R.drawable.ic_dialog_alert);
			builder.setTitle(R.string.media_storage_error_title);

			if (Environment.MEDIA_MOUNTED.equals(mount_state)
					|| Environment.MEDIA_MOUNTED_READ_ONLY.equals(mount_state)) {
				getActivity().finish();
			} else if (Environment.MEDIA_CHECKING.equals(mount_state)) {
				builder.setMessage(R.string.media_storage_error_checking);
			} else if (Environment.MEDIA_BAD_REMOVAL.equals(mount_state)
					|| Environment.MEDIA_REMOVED.equals(mount_state)) {
				builder.setMessage(R.string.media_storage_error_removed);
			} else if (Environment.MEDIA_NOFS.equals(mount_state)
					|| Environment.MEDIA_UNMOUNTABLE.equals(mount_state)) {
				builder.setMessage(R.string.media_storage_error_corrupted);
			} else if (Environment.MEDIA_SHARED.equals(mount_state)) {
				builder.setMessage(R.string.media_storage_error_shared);
			} else if (Environment.MEDIA_UNMOUNTED.equals(mount_state)) {
				builder.setMessage(R.string.media_storage_error_unmounted);
			} else {
				builder.setMessage(R.string.media_storage_error_unknown);
			}

			return builder.create();

		}

		@Override
		public void onCancel(DialogInterface dialog) {
			getActivity().finish();
		}

		@Override
		public void onClick(DialogInterface dialog, int which) {

		}
	}
}
