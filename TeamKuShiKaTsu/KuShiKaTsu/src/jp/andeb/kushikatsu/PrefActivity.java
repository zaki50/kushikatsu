package jp.andeb.kushikatsu;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;

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
        super.onCreate(savedInstanceState);
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

        PreferenceScreen screenPref = (PreferenceScreen) findPreference(KEY_VERSION_NUMBER);
        screenPref.setSummary(versionName);

        // サウンドパターンのリスナー登録
        ListPreference soundPatternList = (ListPreference) findPreference(KEY_SOUND_PATTERN);
        soundPatternList.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(final Preference preference, final Object newValue) {
                CharSequence cs = (CharSequence) newValue;
                int id = getResources().getIdentifier(cs.toString(), "raw", getPackageName());

                MediaPlayer mediaPlayer = MediaPlayer.create(PrefActivity.this, id);
                mediaPlayer.start();
                mediaPlayer = null;
                return true;
            }
        });
    }
}
