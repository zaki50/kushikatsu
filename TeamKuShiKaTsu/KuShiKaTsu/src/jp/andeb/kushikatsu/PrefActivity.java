package jp.andeb.kushikatsu;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

public class PrefActivity extends PreferenceActivity {

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
		
		PreferenceScreen screenPref = (PreferenceScreen) findPreference(PrefConst.KEY_VERSION_NUMBER);
		screenPref.setSummary(versionName);

	}
}
