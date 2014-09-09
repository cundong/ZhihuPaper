package com.cundong.izhihu.activity;

import java.util.Calendar;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.cundong.izhihu.R;
import com.cundong.izhihu.fragment.NewsListFragment;
import com.cundong.izhihu.task.MyAsyncTask;
import com.cundong.izhihu.task.OfflineDownloadTask;
import com.cundong.izhihu.task.ResponseListener;
import com.cundong.izhihu.util.DateUtils;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

public class MainActivity extends BaseActivity implements ResponseListener {

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
			
			new OfflineDownloadTask(this).executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);
			
			return true;
		case R.id.action_second:
			
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB ) {
			    startActivity(new Intent(this, PrefsActivity.class));
			} else {
			    startActivity(new Intent(this, OtherPrefsActivity.class));
			}
			
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onPre() {
		Crouton.makeText(this, "正在离线最新内容", Style.INFO).show();
	}

	@Override
	public void onComplete(String content, boolean isRefreshSuccess,
			boolean isContentSame) {
		
		if (!TextUtils.isEmpty(content) && content.equals("success")) {
			Crouton.makeText(this, "离线最新内容完成", Style.INFO).show();
		} 
	}

	@Override
	public void onFail(final Exception e) {
		if (!isFinishing()) {
			runOnUiThread(new Runnable() {
				public void run() {

					Crouton.makeText(
							mInstance,
							"error:" + e != null && e.fillInStackTrace() != null ? e.fillInStackTrace().toString() : "NULL",
							Style.ALERT).show();
				}
			});
		} else {
			mLogger.e("onFail() fuck added()==false");
		}
	}
}