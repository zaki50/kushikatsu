/*
 * Copyright 2010-2011 Android DEvelopers' cluB
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

import java.util.HashMap;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;

import jp.andeb.kushikatsu.helper.KushikatsuHelper;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.view.Window;
import android.widget.Toast;

/**
 * 設定画面のための {@link Activity} です。
 */
@DefaultAnnotation(NonNull.class)
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

    /** 擬似デバイスモード */
    public static final String KEY_MOCK_DEVICE_ENABLED = "mock_device_enabled";
    /** 擬似デバイスリザルトコード */
    public static final String KEY_MOCK_DEVICE_RESULT_CODE = "mock_device_result_code";

    /** セルフテスト */
    public static final String KEY_SELFTEST_MESSAGE_TYPE = "selftest_message_type";

    /** インテント送信要求のリクエストコード */
    private static final int SEND_REQUEST_CODE = 1;

    private Map<CharSequence, Intent> selfTestMap_;
    private Map<CharSequence, Intent> buildSelftestMap() {
        final Map<CharSequence, Intent> map = new HashMap<CharSequence, Intent>();
        map.put("intent", KushikatsuHelper
                .buildIntentForSendIntent(KushikatsuHelper
                        .buildIntentForKushikatsuInstall()));
        map.put("browser",
                KushikatsuHelper.buildIntentForStartBrowser(
                        getString(R.string.site_url).toString(), null));
        map.put("mailer", KushikatsuHelper.buildIntentForStartMailer(null,
                null, null, null, null));

        return map;
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_LEFT_ICON);
        super.onCreate(savedInstanceState);
        setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.icon);

        addPreferencesFromResource(R.xml.pref);

        selfTestMap_ = buildSelftestMap();

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

        final CheckBoxPreference mockDeviceEnabledBox;
        mockDeviceEnabledBox = (CheckBoxPreference) findPreference(KEY_MOCK_DEVICE_ENABLED);
        mockDeviceEnabledBox
                .setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference,
                            Object newValue) {
                        final ListPreference selftestTypeList;
                        selftestTypeList = (ListPreference) findPreference(KEY_SELFTEST_MESSAGE_TYPE);
                        selftestTypeList.setEnabled(!((Boolean) newValue)
                                .booleanValue());
                        return true;
                    }
                });
        final boolean mockDeviceEnabled = mockDeviceEnabledBox.isChecked();

        final ListPreference selftestTypeList;
        selftestTypeList = (ListPreference) findPreference(KEY_SELFTEST_MESSAGE_TYPE);
        selftestTypeList
                .setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(
                            final Preference preference, final Object newValue) {
                        CharSequence cs = (CharSequence) newValue;

                        final Intent i = selfTestMap_.get(cs);
                        if (i == null) {
                            Toast.makeText(PrefActivity.this,
                                    R.string.failed_to_send, Toast.LENGTH_LONG)
                                    .show();
                        } else {
                            KushikatsuHelper
                                    .startKushikatsuForResult(
                                            PrefActivity.this, i,
                                            SEND_REQUEST_CODE, -1);
                        }
                        return true;
                    }
                });
        selftestTypeList.setEnabled(!mockDeviceEnabled);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != SEND_REQUEST_CODE) {
            return;
        }

        final Toast toast;
        switch (resultCode) {
        case KushikatsuHelper.RESULT_OK:
            toast = Toast.makeText(this, R.string.send_result_ok,
                    Toast.LENGTH_SHORT);
            break;
        case KushikatsuHelper.RESULT_CANCELED:
            toast = Toast.makeText(this, R.string.send_result_canceled,
                    Toast.LENGTH_SHORT);
            break;
        case KushikatsuHelper.RESULT_UNEXPECTED_ERROR:
            toast = Toast.makeText(this, R.string.send_result_unexpected_error,
                    Toast.LENGTH_SHORT);
            break;
        case KushikatsuHelper.RESULT_INVALID_EXTRA:
            toast = Toast.makeText(this, R.string.send_result_invalid_extra,
                    Toast.LENGTH_SHORT);
            break;
        case KushikatsuHelper.RESULT_DEVICE_NOT_FOUND:
            toast = Toast.makeText(this, R.string.send_result_device_not_found,
                    Toast.LENGTH_SHORT);
            break;
        case KushikatsuHelper.RESULT_DEVICE_IN_USE:
            toast = Toast.makeText(this, R.string.send_result_device_in_use,
                    Toast.LENGTH_SHORT);
            break;
        case KushikatsuHelper.RESULT_TOO_BIG:
            toast = Toast.makeText(this, R.string.send_result_too_big,
                    Toast.LENGTH_SHORT);
            break;
        case KushikatsuHelper.RESULT_TIMEOUT:
            toast = Toast.makeText(this, R.string.send_result_timeout,
                    Toast.LENGTH_SHORT);
            break;
        case KushikatsuHelper.RESULT_NOT_INITIALIZED:
            toast = Toast.makeText(this, R.string.send_result_not_initialized,
                    Toast.LENGTH_SHORT);
            break;
        case KushikatsuHelper.RESULT_DEVICE_LOCKED:
            toast = Toast.makeText(this, R.string.send_result_device_locked,
                    Toast.LENGTH_SHORT);
            break;
        case KushikatsuHelper.RESULT_PUSH_REGISTERED:
        toast = Toast.makeText(this, R.string.send_result_push_registered,
                Toast.LENGTH_SHORT);
        break;
        default:
            toast = Toast.makeText(this,
                    getString(R.string.send_result_unknown_error, resultCode),
                    Toast.LENGTH_SHORT);
            break;
        }
        toast.show();
    }
}
