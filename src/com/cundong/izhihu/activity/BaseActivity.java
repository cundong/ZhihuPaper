package com.cundong.izhihu.activity;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.cundong.izhihu.util.Logger;

public abstract class BaseActivity extends SherlockFragmentActivity {

	protected Activity mInstance = null;
	
	protected Logger mLogger = Logger.getLogger();
	
	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);

		mInstance = this;
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
}