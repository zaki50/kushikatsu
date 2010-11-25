/*
 * Copyright 2010 Android DEvelopers' cluB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * $Id$
 */
package jp.andeb.kushikatsu;

import static jp.andeb.kushikatsu.util.MediaPlayerUtil.RELEASE_PLAYER_LISTENER;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.view.Window;

public class PrefActivity extends PreferenceActivity {

    /** サウンドモード */
    public static final String KEY_SOUND_MODE = "sound_mode";
    /** 着信音のボリュームを使用 */
    public static final String KEY_SAME_RINGER_MODE = "same_ringer_mode";
    /** 振動モード */
    public static final String KEY_VIBRATION_MODE = "vibration_mode";
    /** バージョン番号 */
    public static final String KEY_VERSION_NUMBER = "version_number";
    /** サウンドパターン */
    public static final String KEY_SOUND_PATTERN = "sound_pattern";

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_LEFT_ICON);
        super.onCreate(savedInstanceState);
        setFeatureDrawableResource(Window.FEATURE_LEFT_ICON,
                R.drawable.icon);

        addPreferencesFromResource(R.xml.pref);


        // バージョン情報設定
        String versionName = "";
        PackageManager pm = getPackageManager();
        try {
            PackageInfo info = null;
            info = pm.getPackageInfo(this.getPackageName(), 0);
            versionName = info.versionName;
        } catch (NameNotFoundException e) {
        }

        final PreferenceScreen screenPref;
        screenPref = (PreferenceScreen) findPreference(KEY_VERSION_NUMBER);
        screenPref.setSummary(versionName);

        // サウンドパターンのリスナー登録
        final ListPreference soundPatternList;
        soundPatternList = (ListPreference) findPreference(KEY_SOUND_PATTERN);
        soundPatternList
                .setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(
                            final Preference preference, final Object newValue) {
                        CharSequence cs = (CharSequence) newValue;
                        int id = getResources().getIdentifier(cs.toString(),
                                "raw", getPackageName());

                        MediaPlayer mediaPlayer = MediaPlayer.create(
                                PrefActivity.this, id);
                        mediaPlayer.start();
                        mediaPlayer
                                .setOnCompletionListener(RELEASE_PLAYER_LISTENER);
                        mediaPlayer = null;
                        return true;
                    }
                });
    }
}
