package com.cundong.izhihu.activity;

import android.annotation.SuppressLint;
import android.os.Bundle;

import com.cundong.izhihu.fragment.PrefsFragment;
import com.cundong.izhihu.fragment.PrefsFragment.OnPreChangeListener;

/**
 * 类说明： 	用于Android4.0+的设置页Activity
 * 
 * @date 	2014-9-20
 * @version 1.0
 */
@SuppressLint("NewApi")
public class OtherPrefsActivity extends BaseActivity implements OnPreChangeListener{

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		getFragmentManager().beginTransaction().replace(android.R.id.content, new PrefsFragment()).commit();
	}
	
	@Override
	public void onChanged(boolean result) {
		
		recreateActivity();
	}
}