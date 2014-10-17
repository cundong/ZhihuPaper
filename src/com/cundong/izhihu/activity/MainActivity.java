package com.cundong.izhihu.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.TextUtils;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.cundong.izhihu.Constants;
import com.cundong.izhihu.R;
import com.cundong.izhihu.fragment.NewsListFragment;
import com.cundong.izhihu.task.MyAsyncTask;
import com.cundong.izhihu.task.OfflineDownloadTask;
import com.cundong.izhihu.task.ResponseListener;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

/**
 * 类说明： 	主页面Activity
 * 
 * @date 	2014-9-20
 * @version 1.0
 */
public class MainActivity extends BaseActivity implements ResponseListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		
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
		case R.id.action_download:
			new OfflineDownloadTask(this, this).executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);
			return true;
		case R.id.action_favorite:
			startActivity(new Intent(this, FavoriteActivity.class));
			return true;
		case R.id.action_setting:
			
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
				startActivityForResult(new Intent(this, PrefsActivity.class), Constants.REQUESTCODE_SETTING);
			} else {
				startActivityForResult(new Intent(this, OtherPrefsActivity.class), Constants.REQUESTCODE_SETTING);
			}
			
			return true;
		}
		
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onPreExecute() {
		Crouton.makeText(this, R.string.offline_download_doing, Style.INFO).show();
	}

	@Override
	public void onPostExecute(String content, boolean isRefreshSuccess,
			boolean isContentSame) {
		
		if (!TextUtils.isEmpty(content) && content.equals("success")) {
			Crouton.makeText(this, R.string.offline_download_done, Style.INFO)
					.show();
		} else {
			Crouton.makeText(this, R.string.offline_download_fail, Style.ALERT)
					.show();
		}
	}

	@Override
	public void onFail(final Exception e) {
		if (!isFinishing()) {
			Crouton.makeText(this, R.string.offline_download_fail, Style.ALERT).show();
		}
	}

	@Override
	public void onProgressUpdate(String value) {
		
	}
	
	@SuppressLint("NewApi")
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if (requestCode == Constants.REQUESTCODE_SETTING) {

			//Activity关闭后，如果改变了主题，则需要recreate this Activity
			SharedPreferences mPerferences = PreferenceManager.getDefaultSharedPreferences(this);
			if (isDarkTheme != mPerferences.getBoolean("dark_theme?", false)) {
				recreateActivity();
			}
		}
	}
	
	@Override
	protected void onDestroy() {
		Crouton.clearCroutonsForActivity(this);
		super.onDestroy();
	}
}