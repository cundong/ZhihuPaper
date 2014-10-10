package com.cundong.izhihu.activity;

import android.annotation.SuppressLint;
import android.os.Bundle;

import com.cundong.izhihu.fragment.PrefsFragment;

/**
 * 类说明： 	用于Android4.0+的设置页Activity
 * 
 * @date 	2014-9-20
 * @version 1.0
 */
@SuppressLint("NewApi")
public class OtherPrefsActivity extends BaseActivity {

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		getFragmentManager().beginTransaction()
				.replace(android.R.id.content, new PrefsFragment()).commit();
	}
}