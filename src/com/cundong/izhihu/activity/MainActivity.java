package com.cundong.izhihu.activity;

import java.util.Calendar;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.cundong.izhihu.R;
import com.cundong.izhihu.fragment.NewsListFragment;
import com.cundong.izhihu.util.DateUtils;

public class MainActivity extends BaseActivity{

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);

		Calendar dateToGetUrl = Calendar.getInstance();
		dateToGetUrl.add(Calendar.DAY_OF_YEAR, 1);
		String date = DateUtils.getCurrentDate(DateUtils.YYYYMMDD);
				
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