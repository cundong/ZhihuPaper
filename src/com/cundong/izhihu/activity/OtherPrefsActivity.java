package com.cundong.izhihu.activity;

import android.annotation.SuppressLint;
import android.os.Bundle;

import com.cundong.izhihu.fragment.PrefsFragment;

@SuppressLint("NewApi")
public class OtherPrefsActivity extends BaseActivity {

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);

		getFragmentManager().beginTransaction()
				.replace(android.R.id.content, new PrefsFragment()).commit();
	}
}