package com.cundong.izhihu.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.cundong.izhihu.R;
import com.cundong.izhihu.fragment.NewsDetailImageFragment;

public class NewsDetailImageActivity extends BaseActivity {

	private static final String NEWS_DETAIL_IMAGE= "com.cundong.izhihu.activity.NewsDetailImageActivity.news_detail_image";
	
	private String mImageUrl = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

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
			
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}