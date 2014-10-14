package com.cundong.izhihu.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.cundong.izhihu.R;
import com.cundong.izhihu.util.Logger;
import com.umeng.analytics.MobclickAgent;

public abstract class BaseActivity extends SherlockFragmentActivity {

	protected Activity mInstance = null;

	protected Logger mLogger = Logger.getLogger();

	protected boolean isDarkTheme = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		SharedPreferences mPerferences = PreferenceManager.getDefaultSharedPreferences(this);
		isDarkTheme = mPerferences.getBoolean("dark_theme?", false); 
		
		if (isDarkTheme) {
			setTheme(R.style.Theme_Daily_AppTheme_Dark);
		} else {
			setTheme(R.style.Theme_Daily_AppTheme_Light);
		}
		
		super.onCreate(savedInstanceState);
		
		mInstance = this;
	}

	@Override
	protected void onResume() {
		super.onResume();
		MobclickAgent.onResume(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		MobclickAgent.onPause(this);
	}

	protected Fragment getFragment() {
		return null;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@SuppressLint("NewApi")
	public void recreateActivity() {

		Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
					finish();
					startActivity(getIntent());
				} else {
					recreate();
				}
			}
		}, 1);
	}
}