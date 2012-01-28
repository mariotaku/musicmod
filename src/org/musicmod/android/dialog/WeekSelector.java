/*
 *              Copyright (C) 2011 The MusicMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *            http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.musicmod.android.dialog;

import org.musicmod.android.Constants;
import org.musicmod.android.R;
import org.musicmod.android.util.PreferencesEditor;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.widget.LinearLayout;
import android.widget.Toast;

public class WeekSelector extends FragmentActivity implements Constants {

	private String action;
	private static VerticalTextSpinnerDialog mAlert;
	private static PreferencesEditor mPrefs;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		setContentView(new LinearLayout(this));
		mPrefs = new PreferencesEditor(this);
		int def = mPrefs.getIntPref(PREF_KEY_NUMWEEKS, 2);
		int pos = savedInstanceState != null ? savedInstanceState.getInt(PREF_KEY_NUMWEEKS, def) : def;
		
		mAlert = new VerticalTextSpinnerDialog(this, getResources().getStringArray(R.array.weeklist), pos - 1);
		action = getIntent().getAction();

		if (INTENT_WEEK_SELECTOR.equals(action)) {
			DialogFragment fragment = new WeekSelectorDialogFragment();
	        fragment.show(getSupportFragmentManager(), "dialog");
			
		} else {
			Toast.makeText(this, R.string.error_bad_parameters, Toast.LENGTH_SHORT).show();
			finish();
		}

	}
    
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
    	savedInstanceState.putInt(PREF_KEY_NUMWEEKS, mAlert.getCurrentSelectedPos() + 1);
    	super.onSaveInstanceState(savedInstanceState);
    }
	
	public static class WeekSelectorDialogFragment extends DialogFragment implements OnClickListener, OnCancelListener {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
        	
			mAlert.setVolumeControlStream(AudioManager.STREAM_MUSIC);
			mAlert.setIcon(android.R.drawable.ic_dialog_info);
			mAlert.setTitle(R.string.set_time);
			mAlert.setButton(Dialog.BUTTON_POSITIVE, getString(android.R.string.ok), this);
			mAlert.setButton(Dialog.BUTTON_NEGATIVE, getString(android.R.string.cancel),this);
			mAlert.setOnCancelListener(this);
            
            return mAlert;
            
        }
        
    	@Override
    	public void onClick(DialogInterface dialog, int which) {

    		switch (which) {
    			case DialogInterface.BUTTON_POSITIVE:
    				int numweeks = mAlert.getCurrentSelectedPos() + 1;
    				mPrefs.setIntPref(PREF_KEY_NUMWEEKS, numweeks);
    				getActivity().setResult(RESULT_OK);
    				break;
    			case DialogInterface.BUTTON_NEGATIVE:
    				break;
    				
    		}
    		getActivity().finish();
    	}
    	
    	@Override
    	public void onCancel(DialogInterface dialog) {
    		getActivity().finish();
    	}
    }
}
