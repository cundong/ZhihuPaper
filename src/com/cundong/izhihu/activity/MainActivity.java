package com.cundong.izhihu.activity;

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

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

public class MainActivity extends BaseActivity implements ResponseListener {

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		
		Fragment newFragment = getFragment();
		
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
			
			new OfflineDownloadTask(this, this).executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);
			
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
	public void onPreExecute() {
		Crouton.makeText(this, "正在离线最新内容", Style.INFO).show();
	}

	@Override
	public void onPostExecute(String content, boolean isRefreshSuccess,
			boolean isContentSame) {
		
		if (!TextUtils.isEmpty(content) && content.equals("success")) {
			Crouton.makeText(this, "离线最新内容完成", Style.INFO).show();
		} 
	}

	@Override
	public void onFail(final Exception e) {
		if (!isFinishing()) {
			Crouton.makeText(this, "离线最新内容失败", Style.ALERT).show();
		}
	}

	@Override
	public void onProgressUpdate(String value) {
		// TODO Auto-generated method stub
		
	}
}