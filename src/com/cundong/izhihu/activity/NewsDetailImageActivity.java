package com.cundong.izhihu.activity;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.cundong.izhihu.R;
import com.cundong.izhihu.fragment.NewsDetailImageFragment;
import com.cundong.izhihu.task.ImageToGalleryTask;
import com.cundong.izhihu.task.MyAsyncTask;
import com.cundong.izhihu.task.ResponseListener;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

public class NewsDetailImageActivity extends BaseActivity implements ResponseListener {

	private static final String NEWS_DETAIL_IMAGE= "com.cundong.izhihu.activity.NewsDetailImageActivity.news_detail_image";
	
	private Context mContext;
	
	private String mImageUrl = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		mContext = this.getApplicationContext();
		
		if (savedInstanceState == null) {
			mImageUrl = getIntent().getStringExtra("imageUrl");
			Bundle bundle = new Bundle();
			bundle.putString("imageUrl", mImageUrl);
			
			// Add the Sample Fragment if there is one
			Fragment newFragment = getFragment();
			newFragment.setArguments(bundle);

			if (newFragment != null) {
				getSupportFragmentManager().beginTransaction()
						.replace(android.R.id.content, newFragment).commit();
			}
		} else {
			mImageUrl = savedInstanceState.getString(NEWS_DETAIL_IMAGE);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putString(NEWS_DETAIL_IMAGE, mImageUrl);
		super.onSaveInstanceState(outState);
	}
	
	@Override
	protected Fragment getFragment() {
		return new NewsDetailImageFragment();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.detail_image, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_first:
			
			new ImageToGalleryTask(mContext, this).executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR, mImageUrl);

			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onPreExecute() {
		Crouton.makeText(this, "开始保存图片", Style.INFO).show();
	}

	@Override
	public void onPostExecute(String content, boolean isRefreshSuccess,
			boolean isContentSame) {
		
		if (!TextUtils.isEmpty(content) && content.equals("success")) {
			Crouton.makeText(this, "已保存图片至图册", Style.INFO).show();
		} 
	}

	@Override
	public void onProgressUpdate(String value) {
		
	}

	@Override
	public void onFail(Exception e) {
		if (!isFinishing()) {
			Crouton.makeText(this, "保存图片失败", Style.ALERT).show();
		} 
	}
}