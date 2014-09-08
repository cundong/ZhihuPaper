package com.cundong.izhihu.activity;

import java.util.Calendar;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.cundong.izhihu.Constants;
import com.cundong.izhihu.R;
import com.cundong.izhihu.fragment.NewsListFragment;

public class MainActivity extends BaseActivity{

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);

		Calendar dateToGetUrl = Calendar.getInstance();
		dateToGetUrl.add(Calendar.DAY_OF_YEAR, 1);
		String date = Constants.Date.simpleDateFormat.format(dateToGetUrl.getTime());
		
		Bundle bundle = new Bundle();
		bundle.putString("date", date);
		
		// Add the Sample Fragment if there is one
		Fragment newFragment = getFragment();
		newFragment.setArguments(bundle);
		
		if (newFragment != null) {
			getSupportFragmentManager().beginTransaction().replace(android.R.id.content, newFragment).commit();
		}
	}
	
	@Override
	protected Fragment getFragment() {
		return new NewsListFragment();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_first:
			Toast.makeText(this, "First Action Item", Toast.LENGTH_SHORT)
					.show();
			return true;
		case R.id.action_second:
			Toast.makeText(this, "Second Action Item", Toast.LENGTH_SHORT)
					.show();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}