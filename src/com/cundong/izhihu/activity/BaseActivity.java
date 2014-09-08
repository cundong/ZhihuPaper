package com.cundong.izhihu.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.cundong.izhihu.R;

public abstract class BaseActivity extends SherlockFragmentActivity {

	protected Activity mInstance = null;

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);

		initBaseParam();
	}

	protected void initBaseParam() {
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
	         Intent intent = new Intent(this, MainActivity.class);            
	         intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); 
	         startActivity(intent);            
	         return true;   
		}
		
		return super.onOptionsItemSelected(item);
	}
}