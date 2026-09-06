package com.cundong.izhihu.activity;

import android.os.Bundle;

import com.cundong.izhihu.R;
import com.cundong.izhihu.fragment.PrefsFragment;
import com.cundong.izhihu.fragment.PrefsFragment.OnPreChangeListener;

/**
 * 类说明： 	设置页Activity
 *
 * Hosts the androidx PreferenceFragmentCompat. The old PrefsActivity (a
 * SherlockPreferenceActivity kept around for Android 2.3) is gone: minSdk is 21
 * and androidx.preference works on every supported release.
 * 
 * @date 	2014-9-20
 * @version 1.0
 */
public class OtherPrefsActivity extends BaseActivity implements OnPreChangeListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setupToolbar(R.layout.activity_prefs, true);
		if (getSupportActionBar() != null) {
			getSupportActionBar().setTitle(R.string.actionbar_title_setting);
			getSupportActionBar().setSubtitle(R.string.settings_category_reading);
		}

		if (savedInstanceState == null) {
			getSupportFragmentManager()
					.beginTransaction()
					.replace(R.id.content_container, new PrefsFragment())
					.commit();
		}
	}
	
	@Override
	public void onChanged(boolean result) {
		
		recreateActivity();
	}
}
