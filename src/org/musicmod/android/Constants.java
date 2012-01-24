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

package org.musicmod.android;

public interface Constants {

	public final static int NOW = 1;
	public final static int NEXT = 2;
	public final static int LAST = 3;
	public final static int PLAYBACKSERVICE_STATUS = 1;
	public final static int SLEEPTIMER_STATUS = 2;

	public final static int SHUFFLE_NONE = 0;
	public final static int SHUFFLE_NORMAL = 1;
	public final static int SHUFFLE_AUTO = 2;

	public final static int REPEAT_NONE = 0;
	public final static int REPEAT_CURRENT = 1;
	public final static int REPEAT_ALL = 2;

	public final static int PAGE_ARTIST = 0;
	public final static int PAGE_ALBUM = 1;
	public final static int PAGE_TRACK = 2;
	public final static int PAGE_PLAYLIST = 3;

	public final static long PLAYLIST_UNKNOWN = -1;
	public final static long PLAYLIST_ALL_SONGS = -2;
	public final static long PLAYLIST_QUEUE = -3;
	public final static long PLAYLIST_NEW = -4;
	public final static long PLAYLIST_FAVORITES = -5;
	public final static long PLAYLIST_RECENTLY_ADDED = -6;
	public final static long PLAYLIST_PODCASTS = -7;

	public static final String INTERNAL_VOLUME = "internal";
	public static final String EXTERNAL_VOLUME = "external";

	public final static String PLAYLIST_NAME_FAVORITES = "MusicMod Favorites";

	public final static String TYPE_ARTIST_ALBUM = "artist_album";
	public final static String TYPE_ALBUM = "album";
	public final static String TYPE_TRACK = "track";

	public final static String LOGTAG_SERVICE = "MusicMod.Service";
	public final static String LOGTAG_TEST = "MusicMod.Test";
	public final static String LOGTAG_MUSICUTILS = "MusicMod.MusicUtils";
	public final static String LOGTAG_WIDGET_4x1 = "MusicAppWidgetProvider4x1";
	public final static String LOGTAG_WIDGET_2x2 = "MusicAppWidgetProvider2x2";

	public final static String SCROBBLE_SLS_API = "com.adam.aslfms.notify.playstatechanged";

	public final static int SCROBBLE_PLAYSTATE_START = 0;
	public final static int SCROBBLE_PLAYSTATE_RESUME = 1;
	public final static int SCROBBLE_PLAYSTATE_PAUSE = 2;
	public final static int SCROBBLE_PLAYSTATE_COMPLETE = 3;

	public final static int VISUALIZER_TYPE_WAVE_FORM = 1;
	public final static int VISUALIZER_TYPE_FFT_SPECTRUM = 2;

	public final static int LYRICS_STATUS_OK = 0;
	public final static int LYRICS_STATUS_NOT_FOUND = 1;
	public final static int LYRICS_STATUS_INVALID = 2;

	public final static String APP_NAME = "MusicMod";
	public final static String PACKAGE_NAME = "org.musicmod.android";

	public final static String PLUGINS_PNAME_PATTERN = "org.musicmod.plugin";
	public final static String THEMES_PNAME_PATTERN = "org.musicmod.theme";

	public final static String SERVICECMD = "org.musicmod.android.musicservicecommand";
	public final static String CMDNAME = "command";
	public final static String CMDTOGGLEPAUSE = "togglepause";
	public final static String CMDSTOP = "stop";
	public final static String CMDPAUSE = "pause";
	public final static String CMDPREVIOUS = "previous";
	public final static String CMDNEXT = "next";
	public static final String CMDCYCLEREPEAT = "cyclerepeat";
	public static final String CMDTOGGLESHUFFLE = "toggleshuffle";
	public final static String CMDREFRESHLYRICS = "refreshlyrics";
	public final static String CMDRESENDALLLYRICS = "resendalllyrics";
	public final static String CMDREFRESHMETADATA = "refreshmetadata";
	public final static String CMDMUSICWIDGETUPDATE_4x1 = "musicwidgetupdate4x1";
	public final static String CMDMUSICWIDGETUPDATE_2x2 = "musicwidgetupdate2x2";

	public final static String TOGGLEPAUSE_ACTION = "org.musicmod.android.musicservicecommand.togglepause";
	public final static String PAUSE_ACTION = "org.musicmod.android.musicservicecommand.pause";
	public final static String PREVIOUS_ACTION = "org.musicmod.android.musicservicecommand.previous";
	public final static String NEXT_ACTION = "org.musicmod.android.musicservicecommand.next";
	public static final String CYCLEREPEAT_ACTION = "org.musicmod.android.musicservicecommand.cyclerepeat";
	public static final String TOGGLESHUFFLE_ACTION = "org.musicmod.android.musicservicecommand.toggleshuffle";

	public final static String TESTCMD_MUSICPLAYBACKACTIVITY = "org.musicmod.test.musicplaybackactivity";

	public final static String SHAREDPREFS_PREFERENCES = "preferences";
	public final static String SHAREDPREFS_EQUALIZER = "equalizer";
	public final static String SHAREDPREFS_STATES = "states";

	public final static String MEDIASTORE_EXTERNAL_AUDIO_ALBUMART_URI = "content://media/external/audio/albumart";
	public final static String MEDIASTORE_EXTERNAL_AUDIO_MEDIA_URI = "content://media/external/audio/media/";
	public final static String MEDIASTORE_EXTERNAL_AUDIO_ALBUMS_URI = "content://media/external/audio/albums/";
	public final static String MEDIASTORE_EXTERNAL_AUDIO_ARTISTS_URI = "content://media/external/audio/artists/";
	public final static String MEDIASTORE_EXTERNAL_AUDIO_SEARCH_FANCY_URI = "content://media/external/audio/search/fancy/";

	public final static String LASTFM_APIKEY = "e682ad43038e19de1e33f583b191f5b2";

	public final static String BEHAVIOR_NEXT_SONG = "next_song";
	public final static String BEHAVIOR_PLAY_PAUSE = "play_pause";
	public final static String DEFAULT_SHAKING_BEHAVIOR = BEHAVIOR_NEXT_SONG;

	public final static String LYRICS_CHARSET = "auto";
	public final static long LYRICS_REFRESH_RATE = 200;
	public final static long LYRICS_TIMER_DELAY = 50;

	public final static boolean DEFAULT_LYRICS_WAKELOCK = false;
	public final static boolean DEFAULT_SPLIT_LYRICS = true;
	public final static boolean DEFAULT_SKIP_BLANK = true;
	public final static boolean DEFAULT_DISPLAY_LYRICS = true;
	public final static boolean DEFAULT_DISPLAY_VISUALIZER = true;
	public final static int DEFAULT_VISUALIZER_TYPE = VISUALIZER_TYPE_WAVE_FORM;
	public final static int DEFAULT_VISUALIZER_REFRESHRATE = 1;
	public final static int DEFAULT_VISUALIZER_ACCURACY = 1;
	public final static boolean DEFAULT_VISUALIZER_ANTIALIAS = true;

	public final static String STATE_KEY_CURRENTTAB = "currenttab";
	public final static String STATE_KEY_CURRPOS = "curpos";
	public final static String STATE_KEY_CARDID = "cardid";
	public final static String STATE_KEY_QUEUE = "queue";
	public final static String STATE_KEY_HISTORY = "history";
	public final static String STATE_KEY_SEEKPOS = "seekpos";
	public final static String STATE_KEY_REPEATMODE = "repeatmode";
	public final static String STATE_KEY_SHUFFLEMODE = "shufflemode";

	public final static String PREF_KEY_NUMWEEKS = "numweeks";

	public final static String KEY_RESCAN_MEDIA = "rescan_media";
	public final static String KEY_LYRICS_WAKELOCK = "lyrics_wakelock";
	public final static String KEY_ALBUMART_SIZE = "albumart_size";
	public final static String KEY_DISPLAY_LYRICS = "display_lyrics";
	public final static String KEY_PLUGINS_MANAGER = "plugins_manager";
	public final static String KEY_ENABLE_SCROBBLING = "enable_scrobbling";
	public final static String KEY_GENTLE_SLEEPTIMER = "gentle_sleeptimer";
	public final static String KEY_DISPLAY_VISUALIZER = "display_visualizer";
	public final static String KEY_VISUALIZER_TYPE = "visualizer_type";
	public final static String KEY_VISUALIZER_REFRESHRATE = "visualizer_refreshrate";
	public final static String KEY_VISUALIZER_ACCURACY = "visualizer_accuracy";
	public final static String KEY_VISUALIZER_ANTIALIAS = "visualizer_antialias";
	public final static String KEY_UI_COLOR = "ui_color";
	public final static String KEY_AUTO_COLOR = "auto_color";
	public final static String KEY_CUSTOMIZED_COLOR = "customized_color";
	public final static String KEY_EQUALIZER_ENABLED = "equalizer_enabled";
	public final static String KEY_EQUALIZER_SETTINGS = "equalizer_settings";
	public final static String KEY_SHAKE_ENABLED = "shake_enabled";
	public final static String KEY_SHAKING_THRESHOLD = "shaking_threshold";
	public final static String KEY_SHAKING_BEHAVIOR = "shaking_behavior";
	public final static String KEY_BLUR_BACKGROUND = "blur_background";

	public final static float DEFAULT_SHAKING_THRESHOLD = 5000f;

	public final static int RESULT_DELETE_MUSIC = 1;
	public final static int RESULT_DELETE_ART = 2;
	public final static int RESULT_DELETE_LYRICS = 3;

	public final static String BROADCAST_KEY_ID = "id";
	public final static String BROADCAST_KEY_ARTIST = "artist";
	public final static String BROADCAST_KEY_ALBUM = "album";
	public final static String BROADCAST_KEY_TRACK = "track";
	public final static String BROADCAST_KEY_PLAYING = "playing";
	public final static String BROADCAST_KEY_SONGID = "songid";
	public final static String BROADCAST_KEY_ALBUMID = "albumid";
	public final static String BROADCAST_KEY_POSITION = "pos";
	public final static String BROADCAST_KEY_DURATION = "dur";
	public final static String BROADCAST_KEY_SLS_DURATION = "duration";
	public final static String BROADCAST_KEY_SLS_STATE = "state";
	public final static String BROADCAST_KEY_LYRICS_STATUS = "lyrics_status";
	public final static String BROADCAST_KEY_LYRICS_ID = "lyrics_id";
	public final static String BROADCAST_KEY_LYRICS = "lyrics";

	public final static String INTENT_KEY_CONTENT = "content";
	public final static String INTENT_KEY_ITEMS = "items";
	public final static String INTENT_KEY_ALBUM = "album";
	public final static String INTENT_KEY_ARTIST = "artist";
	public final static String INTENT_KEY_TRACK = "track";
	public final static String INTENT_KEY_PLAYLIST = "playlist";
	public final static String INTENT_KEY_PATH = "path";
	public final static String INTENT_KEY_LIST = "list";
	public final static String INTENT_KEY_RENAME = "rename";
	public final static String INTENT_KEY_DEFAULT_NAME = "default_name";
	public final static String INTENT_KEY_FILTER = "filter";

	public final static String INTENT_KEY_TYPE = "type";
	public final static String INTENT_KEY_ACTION = "action";
	public final static String INTENT_KEY_DATA = "data";

	public final static String MAP_KEY_NAME = "name";
	public final static String MAP_KEY_ID = "id";

	public final static String INTENT_SEARCH_LYRICS = "org.musicmod.android.SEARCH_LYRICS";
	public final static String INTENT_SEARCH_ALBUMART = "org.musicmod.android.SEARCH_ALBUMART";
	public final static String INTENT_DELETE_ITEMS = "org.musicmod.android.DELETE_ITEMS";
	public final static String INTENT_CONFIGURE_PLUGIN = "org.musicmod.android.CONFIGURE_PLUGIN";
	public final static String INTENT_OPEN_PLUGIN = "org.musicmod.android.OPEN_PLUGIN";
	public final static String INTENT_CONFIGURE_THEME = "org.musicmod.android.CONFIGURE_THEME";
	public final static String INTENT_PREVIEW_THEME = "org.musicmod.android.PREVIEW_THEME";
	public final static String INTENT_APPEARANCE_SETTINGS = "org.musicmod.android.APPEARANCE_SETTINGS";
	public final static String INTENT_MUSIC_SETTINGS = "org.musicmod.android.MUSIC_SETTINGS";
	public final static String INTENT_PLAYBACK_VIEWER = "org.musicmod.android.PLAYBACK_VIEWER";
	public final static String INTENT_MUSIC_BROWSER = "org.musicmod.android.MUSIC_BROWSER";
	public final static String INTENT_STREAM_PLAYER = "org.musicmod.android.STREAM_PLAYER";
	public final static String INTENT_ADD_TO_PLAYLIST = "org.musicmod.android.ADD_TO_PLAYLIST";
	public final static String INTENT_CREATE_PLAYLIST = "org.musicmod.android.CREATE_PLAYLIST";
	public final static String INTENT_RENAME_PLAYLIST = "org.musicmod.android.RENAME_PLAYLIST";
	public final static String INTENT_WEEK_SELECTOR = "org.musicmod.android.WEEK_SELECTOR";
	public final static String INTENT_SLEEP_TIMER = "org.musicmod.android.SLEEP_TIMER";
	public final static String INTENT_EQUALIZER = "org.musicmod.android.EQUALIZER";
	public final static String INTENT_PLAY_SHORTCUT = "org.musicmod.android.PLAY_SHORTCUT";
	public final static String INTENT_PLUGINS_MANAGER = "org.musicmod.android.PLUGINS_MANAGER";

	public final static String BROADCAST_PLAYSTATE_CHANGED = "org.musicmod.android.playstatechanged";
	public final static String BROADCAST_META_CHANGED = "org.musicmod.android.metachanged";
	public final static String BROADCAST_NEW_LYRICS_LOADED = "org.musicmod.android.newlyricsloaded";
	public final static String BROADCAST_LYRICS_REFRESHED = "org.musicmod.android.lyricsrefreshed";
	public final static String BROADCAST_QUEUE_CHANGED = "org.musicmod.android.queuechanged";
	public final static String BROADCAST_REPEATMODE_CHANGED = "org.musicmod.android.repeatmodechanged";
	public final static String BROADCAST_SHUFFLEMODE_CHANGED = "org.musicmod.android.shufflemodechanged";
	public final static String BROADCAST_PLAYBACK_COMPLETE = "org.musicmod.android.playbackcomplete";
	public final static String BROADCAST_ASYNC_OPEN_COMPLETE = "org.musicmod.android.asyncopencomplete";
	public final static String BROADCAST_REFRESH_PROGRESSBAR = "org.musicmod.android.refreshui";
	public final static String BROADCAST_PLAYSTATUS_REQUEST = "org.musicmod.android.playstatusrequest";
	public final static String BROADCAST_PLAYSTATUS_RESPONSE = "org.musicmod.android.playstatusresponse";

	public final static int OPEN_URL = R.id.open_url;
	public final static int ADD_TO_PLAYLIST = R.id.add_to_playlist;
	public final static int SLEEP_TIMER = R.id.sleep_timer;
	public final static int SAVE_AS_PLAYLIST = R.id.save_as_playlist;
	public final static int CLEAR_PLAYLIST = R.id.clear_playlist;
	public final static int PLAYLIST_SELECTED = R.id.playlist_selected;
	public final static int NEW_PLAYLIST = R.id.new_playlist;
	public final static int PLAY_SELECTION = R.id.play_selection;
	public final static int GOTO_PLAYBACK = R.id.goto_playback;
	public final static int GOTO_HOME = android.R.id.home;
	public final static int PARTY_SHUFFLE = R.id.party_shuffle;
	public final static int SHUFFLE_ALL = R.id.shuffle_all;
	public final static int PLAY_ALL = R.id.play_all;
	public final static int DELETE_ITEMS = R.id.delete_items;
	public final static int EQUALIZER = R.id.equalizer;
	public final static int EQUALIZER_PRESETS = R.id.equalizer_presets;
	public final static int EQUALIZER_RESET = R.id.equalizer_reset;
	public final static int SCAN_DONE = R.id.scan_done;
	public final static int QUEUE = R.id.queue;
	public final static int SETTINGS = R.id.settings;
	public final static int SEARCH = R.id.search;
	public final static int REMOVE = R.id.remove;
	public final static int CHILD_MENU_BASE = 15; // this should be the last

}
