package com.cundong.izhihu.fragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import com.cundong.izhihu.R;

@SuppressLint("NewApi")
public class PrefsFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.prefs);
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		
		
		return false;
	}
}