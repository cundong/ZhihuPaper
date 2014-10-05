package com.cundong.izhihu.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.actionbarsherlock.widget.ShareActionProvider;
import com.cundong.izhihu.R;
import com.cundong.izhihu.entity.NewsListEntity.NewsEntity;
import com.cundong.izhihu.fragment.NewsDetailFragment;

public class NewsDetailActivity extends BaseActivity {

	private static final String NEWS_ENTIRY = "com.cundong.izhihu.activity.NewsDetailActivity.news_entity";

	//手指在屏幕滑动，X轴最小变化值
	private static final int FLING_MIN_DISTANCE_X = 200;
	
	//手指在屏幕滑动，Y轴最小变化值
	private static final int FLING_MIN_DISTANCE = 10;
	
	//手指在屏幕滑动，最小速度
	private static final int FLING_MIN_VELOCITY = 1;
	
	private GestureDetector mGestureDetector;
	
	private NewsEntity mNewsEntity;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		// Used for theme switching in samples
		setTheme(R.style.Theme_Sherlock); 
		requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

		super.onCreate(savedInstanceState);
		
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		// Load partially transparent black background
		getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.ab_bg_black));
		
		mGestureDetector = new GestureDetector(this, mOnGestureListener);
		
		if (savedInstanceState == null) {
			long id = getIntent().getLongExtra("id", 0);
			mNewsEntity = (NewsEntity) getIntent().getSerializableExtra(
					"newsEntity");

			Bundle bundle = new Bundle();
			bundle.putLong("id", id);

			// Add the Sample Fragment if there is one
			Fragment newFragment = getFragment();
			newFragment.setArguments(bundle);

			if (newFragment != null) {
				getSupportFragmentManager().beginTransaction().replace(android.R.id.content, newFragment).commit();
			}
		} else {
			mNewsEntity = (NewsEntity) savedInstanceState.getSerializable(NEWS_ENTIRY);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putSerializable(NEWS_ENTIRY, mNewsEntity);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected Fragment getFragment() {
		return new NewsDetailFragment();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate your menu.
		getSupportMenuInflater().inflate(R.menu.share_action_provider, menu);

		// Set file with share history to the provider and set the share intent.
		MenuItem actionItem = menu
				.findItem(R.id.menu_item_share_action_provider_action_bar);
		ShareActionProvider actionProvider = (ShareActionProvider) actionItem
				.getActionProvider();
		actionProvider.setShareHistoryFileName(ShareActionProvider.DEFAULT_SHARE_HISTORY_FILE_NAME);
		
		actionProvider.setShareIntent(prepareIntent());

		return true;
	}
	
	private Intent prepareIntent() {
		Intent share = new Intent(android.content.Intent.ACTION_SEND);
		share.setType("text/plain");
		share.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
		StringBuilder shareText = new StringBuilder();
		shareText.append(mNewsEntity.title + " " + mNewsEntity.share_url);
		share.putExtra(Intent.EXTRA_TEXT, shareText.toString());
		return share;
	}
	
	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		
		try {
			mGestureDetector.onTouchEvent(ev);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return super.dispatchTouchEvent(ev);
	}
	
	private OnGestureListener mOnGestureListener = new OnGestureListener() {

		@Override
		public boolean onDown(MotionEvent e) {
			return false;
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {
			boolean isXWell = Math.abs(e2.getX() - e1.getX()) < FLING_MIN_DISTANCE_X ? true : false;

			if (isXWell && e1.getY() - e2.getY() > FLING_MIN_DISTANCE && Math.abs(velocityY) > FLING_MIN_VELOCITY) {
				getSupportActionBar().hide();
			} else if (isXWell && e2.getY() - e1.getY() > FLING_MIN_DISTANCE && Math.abs(velocityY) > FLING_MIN_VELOCITY) {
				getSupportActionBar().show();
			}

			return false;
		}

		@Override
		public void onLongPress(MotionEvent e) {
			
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2,
				float distanceX, float distanceY) {
			return false;
		}

		@Override
		public void onShowPress(MotionEvent e) {
		}

		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			return false;
		}
	};
}