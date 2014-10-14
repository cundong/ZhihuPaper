package com.cundong.izhihu.activity;

import java.util.List;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.actionbarsherlock.widget.ShareActionProvider;
import com.cundong.izhihu.R;
import com.cundong.izhihu.ZhihuApplication;
import com.cundong.izhihu.entity.NewsDetailEntity;
import com.cundong.izhihu.entity.NewsListEntity.NewsEntity;
import com.cundong.izhihu.fragment.NewsDetailFragment;
import com.cundong.izhihu.fragment.NewsDetailFragment.OnContentLoadListener;
import com.cundong.izhihu.task.MyAsyncTask;

/**
 * 类说明： 	新闻详情页，Activity
 * 
 * @date 	2014-9-20
 * @version 1.0
 */
public class NewsDetailActivity extends BaseActivity implements OnContentLoadListener {

	private static final String NEWS_ID = "com.cundong.izhihu.activity.NewsDetailActivity.news_id";
	private static final String NEWS_ENTIRY = "com.cundong.izhihu.activity.NewsDetailActivity.news_entity";

	//手指在屏幕滑动，X轴最小变化值
	private static final int FLING_MIN_DISTANCE_X = 200;
	
	//手指在屏幕滑动，Y轴最小变化值
	private static final int FLING_MIN_DISTANCE = 10;
	
	//手指在屏幕滑动，最小速度
	private static final int FLING_MIN_VELOCITY = 1;
	
	private Menu mOptionsMenu;
	private MenuItem mFavActionItem;
	
	private GestureDetector mGestureDetector;
	
	private long mNewsId = 0;
	private NewsEntity mNewsEntity = null;
	private NewsDetailEntity mNewsDetailEntity = null;
	
	private boolean isInFavorite = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
		super.onCreate(savedInstanceState);
		
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		// Load partially transparent black background
		getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.ab_bg_black));
		
		mGestureDetector = new GestureDetector(this, mOnGestureListener);
		
		if (savedInstanceState == null) {
			
			/**
			 * deal such scheme: <a href="http://daily.zhihu.com/story/4115152">go</>
			 * 
			 * AndroidMainfext.xml config:
			 * <data android:scheme="http" android:host="daily.zhihu.com" android:pathPattern="/story/.*" />
			 */
			Uri data = getIntent().getData();
			String scheme = data!=null ? data.getScheme() : ""; // "http"
			String host = data!=null ?  data.getHost() : ""; // "daily.zhihu.com"
			List<String> params = data!=null ? data.getPathSegments() : null;
			
			if (scheme.equals("http") && host.equals("daily.zhihu.com")
					&& params != null && params.size() == 2) {
				String storyId = params.get(1); 
				mNewsId = Long.parseLong(storyId);
			} else {
				mNewsId = getIntent().getLongExtra("id", 0);
				mNewsEntity = (NewsEntity) getIntent().getSerializableExtra("newsEntity");
			}
			
			Bundle bundle = new Bundle();
			bundle.putLong("id", mNewsId);

			// Add the Sample Fragment if there is one
			Fragment newFragment = getFragment();
			newFragment.setArguments(bundle);

			if (newFragment != null) {
				getSupportFragmentManager().beginTransaction().replace(android.R.id.content, newFragment).commit();
			}
		} else {
			mNewsEntity = (NewsEntity) savedInstanceState.getSerializable(NEWS_ENTIRY);
			mNewsId = savedInstanceState.getLong(NEWS_ID);
		}
		
		new FavoriteStatusGetTask().executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		updateCreateMenu();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		outState.putLong(NEWS_ID, mNewsId);
		outState.putSerializable(NEWS_ENTIRY, mNewsEntity);
	}

	@Override
	protected Fragment getFragment() {
		return new NewsDetailFragment();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_item_fav_action_bar:
			
			if (isInFavorite) {
				ZhihuApplication.getNewsFavoriteDataSource().deleteFromFavorite(String.valueOf(mNewsDetailEntity.id));

				Toast.makeText(this, R.string.fav_cancel_success, Toast.LENGTH_SHORT).show();
				
				mFavActionItem.setIcon(R.drawable.ab_fav_normal);
				mFavActionItem.setTitle(R.string.actionbar_item_fav_add);
				
				isInFavorite = false;
				
			} else {
				ZhihuApplication.getNewsFavoriteDataSource().add2Favorite(
						String.valueOf(mNewsDetailEntity.id),
						mNewsDetailEntity.title, mNewsDetailEntity.image,
						mNewsDetailEntity.share_url);
				
				Toast.makeText(this, R.string.fav_add_success, Toast.LENGTH_SHORT).show();
				
				mFavActionItem.setIcon(R.drawable.ab_fav_active);
				mFavActionItem.setTitle(R.string.actionbar_item_fav_cancel);
				
				isInFavorite = true;
			}
			
			break;
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		mOptionsMenu = menu;

		// Inflate your menu.
		getSupportMenuInflater().inflate(R.menu.share_action_provider, menu);

		// Set file with share history to the provider and set the share intent.
		MenuItem shareActionItem = menu.findItem(R.id.menu_item_share_action_provider_action_bar);
		ShareActionProvider actionProvider = (ShareActionProvider) shareActionItem.getActionProvider();
		actionProvider.setShareHistoryFileName(ShareActionProvider.DEFAULT_SHARE_HISTORY_FILE_NAME);
		actionProvider.setShareIntent(prepareIntent());

		mFavActionItem = menu.findItem(R.id.menu_item_fav_action_bar);

		if (isInFavorite) {
			mFavActionItem.setIcon(R.drawable.ab_fav_active);
			mFavActionItem.setTitle(R.string.actionbar_item_fav_cancel);
		} else {
			mFavActionItem.setIcon(R.drawable.ab_fav_normal);
			mFavActionItem.setTitle(R.string.actionbar_item_fav_add);
		}

		return true;
	}
	
	@SuppressLint("NewApi")
	private void updateCreateMenu() {
		if (Build.VERSION.SDK_INT >= 11) {
			invalidateOptionsMenu();
		} else if (mOptionsMenu != null) {
			mOptionsMenu.clear();
			onCreateOptionsMenu(mOptionsMenu);
		}
	}
	
	private Intent prepareIntent() {
		
		Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
		shareIntent.setType("text/plain");
		shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
		
		if (mNewsDetailEntity != null) {
			StringBuilder sb = new StringBuilder();
			sb.append(mNewsDetailEntity.title).append(" ").append(mNewsDetailEntity.share_url);
			shareIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());
		} else if (mNewsEntity != null) {
			StringBuilder sb = new StringBuilder();
			sb.append(mNewsEntity.title).append(" ").append(mNewsEntity.share_url);
			shareIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());
		}
		
		return shareIntent;
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

	@Override
	public void onComplete(NewsDetailEntity newsDetailEntity) {
		
		mNewsDetailEntity = newsDetailEntity;
		
		updateCreateMenu();
	}
	
	//获取当前新闻是否已被收藏过
	private class FavoriteStatusGetTask extends MyAsyncTask<Void, Void, Boolean> {

		@Override
		protected Boolean doInBackground(Void... params) {

			return ZhihuApplication.getNewsFavoriteDataSource().isInFavorite(String.valueOf(mNewsId));
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			
			isInFavorite = result;
			updateCreateMenu();
		}
	}
}