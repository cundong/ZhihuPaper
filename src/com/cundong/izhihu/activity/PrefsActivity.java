package com.cundong.izhihu.activity;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.cundong.izhihu.R;

//http://stackoverflow.com/questions/10186697/preferenceactivity-android-4-0-and-earlier
@SuppressWarnings("deprecation")
public class PrefsActivity extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);

		addPreferencesFromResource(R.xml.prefs);
	}
}