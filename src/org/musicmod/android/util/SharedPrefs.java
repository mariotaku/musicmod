package org.musicmod.android.util;

import org.musicmod.android.Constants;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class SharedPrefs implements Constants {

	private Context context;

	public SharedPrefs(Context context) {

		this.context = context;
	}

	public short getEqualizerSetting(short band, short def) {

		SharedPreferences prefs = context.getSharedPreferences(SHAREDPREFS_EQUALIZER,
				Context.MODE_PRIVATE);
		return Short.valueOf(prefs.getString(String.valueOf(band), String.valueOf(def)));
	}

	public void setEqualizerSetting(short band, short value) {

		SharedPreferences prefs = context.getSharedPreferences(SHAREDPREFS_EQUALIZER,
				Context.MODE_PRIVATE);
		Editor ed = prefs.edit();
		ed.putString(String.valueOf(band), String.valueOf(value));
		ed.commit();
	}

	public int getIntState(String name, int def) {

		SharedPreferences prefs = context.getSharedPreferences(SHAREDPREFS_STATES,
				Context.MODE_PRIVATE);
		return prefs.getInt(name, def);
	}

	public void setIntState(String name, int value) {

		SharedPreferences prefs = context.getSharedPreferences(SHAREDPREFS_STATES,
				Context.MODE_PRIVATE);
		Editor ed = prefs.edit();
		ed.putInt(name, value);
		ed.commit();
	}

	public long getLongState(String name, long def) {

		SharedPreferences prefs = context.getSharedPreferences(SHAREDPREFS_STATES,
				Context.MODE_PRIVATE);
		return prefs.getLong(name, def);
	}

	public void setLongState(String name, long value) {

		SharedPreferences prefs = context.getSharedPreferences(SHAREDPREFS_STATES,
				Context.MODE_PRIVATE);
		Editor ed = prefs.edit();
		ed.putLong(name, value);
		ed.commit();
	}

	public boolean getBooleanState(String name, boolean def) {

		SharedPreferences prefs = context.getSharedPreferences(SHAREDPREFS_STATES,
				Context.MODE_PRIVATE);
		return prefs.getBoolean(name, def);
	}

	public void setBooleanState(String name, boolean value) {

		SharedPreferences preferences = context.getSharedPreferences(SHAREDPREFS_STATES,
				Context.MODE_PRIVATE);
		Editor editor = preferences.edit();
		editor.putBoolean(name, value);
		editor.commit();
	}

	public String getStringState(String name, String def) {

		SharedPreferences prefs = context.getSharedPreferences(SHAREDPREFS_STATES,
				Context.MODE_PRIVATE);
		return prefs.getString(name, def);
	}

	public void setStringState(String name, String value) {

		SharedPreferences preferences = context.getSharedPreferences(SHAREDPREFS_STATES,
				Context.MODE_PRIVATE);
		Editor editor = preferences.edit();
		editor.putString(name, value);
		editor.commit();
	}

	public int getIntPref(String name, int def) {

		SharedPreferences prefs = context.getSharedPreferences(SHAREDPREFS_PREFERENCES,
				Context.MODE_PRIVATE);
		return prefs.getInt(name, def);
	}

	public void setIntPref(String name, int value) {

		SharedPreferences prefs = context.getSharedPreferences(SHAREDPREFS_PREFERENCES,
				Context.MODE_PRIVATE);
		Editor ed = prefs.edit();
		ed.putInt(name, value);
		ed.commit();
	}

	public float getFloatPref(String name, float def) {

		SharedPreferences prefs = context.getSharedPreferences(SHAREDPREFS_PREFERENCES,
				Context.MODE_PRIVATE);
		return prefs.getFloat(name, def);
	}

	public void setFloatPref(String name, float value) {

		SharedPreferences prefs = context.getSharedPreferences(SHAREDPREFS_PREFERENCES,
				Context.MODE_PRIVATE);
		Editor ed = prefs.edit();
		ed.putFloat(name, value);
		ed.commit();
	}

	public boolean getBooleanPref(String name, boolean def) {

		SharedPreferences prefs = context.getSharedPreferences(SHAREDPREFS_PREFERENCES,
				Context.MODE_PRIVATE);
		return prefs.getBoolean(name, def);
	}

	public void setBooleanPref(String name, boolean value) {

		SharedPreferences preferences = context.getSharedPreferences(SHAREDPREFS_PREFERENCES,
				Context.MODE_PRIVATE);
		Editor editor = preferences.edit();
		editor.putBoolean(name, value);
		editor.commit();
	}

	public String getStringPref(String name, String def) {

		SharedPreferences prefs = context.getSharedPreferences(SHAREDPREFS_PREFERENCES,
				Context.MODE_PRIVATE);
		return prefs.getString(name, def);
	}

	public void setStringPref(String name, String value) {

		SharedPreferences preferences = context.getSharedPreferences(SHAREDPREFS_PREFERENCES,
				Context.MODE_PRIVATE);
		Editor editor = preferences.edit();
		editor.putString(name, value);
		editor.commit();
	}
}