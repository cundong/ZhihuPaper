package com.cundong.izhihu.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.actionbarsherlock.view.Window;
import com.cundong.izhihu.R;
import com.cundong.izhihu.fragment.NewsDetailFragment;

public class NewsDetailActivity extends BaseActivity {

	@Override
	protected void onCreate(Bundle arg0) {
		
		setTheme(R.style.Theme_Sherlock); //Used for theme switching in samples
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        
		super.onCreate(arg0);
		
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		//Load partially transparent black background
        getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.ab_bg_black));
        
        long id = getIntent().getLongExtra("id", 0);
        
		Bundle bundle = new Bundle();
		bundle.putLong("id", id);
		
		// Add the Sample Fragment if there is one
		Fragment newFragment = getFragment();
		newFragment.setArguments(bundle);
		
		if (newFragment != null) {
			getSupportFragmentManager().beginTransaction().replace(android.R.id.content, newFragment).commit();
		}
	}
	
	@Override
	protected Fragment getFragment() {
		return new NewsDetailFragment();
	}
}