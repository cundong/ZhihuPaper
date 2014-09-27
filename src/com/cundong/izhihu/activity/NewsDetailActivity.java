package com.cundong.izhihu.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.cundong.izhihu.R;
import com.cundong.izhihu.entity.NewsListEntity.NewsEntity;
import com.cundong.izhihu.fragment.NewsDetailFragment;

public class NewsDetailActivity extends BaseActivity {
	private static final String NEWS_ENTIRY = "com.cundong.izhihu.activity.NewsDetailActivity.news_entity";

	private NewsEntity mNewsEntity;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		setTheme(R.style.Theme_Sherlock); //Used for theme switching in samples
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        
		super.onCreate(savedInstanceState);
		
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		//Load partially transparent black background
        getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.ab_bg_black));
        if(savedInstanceState == null){
        	long id = getIntent().getLongExtra("id", 0);
            mNewsEntity = (NewsEntity) getIntent().getSerializableExtra("newsEntity");
            		
    		Bundle bundle = new Bundle();
    		bundle.putLong("id", id);
    		
    		// Add the Sample Fragment if there is one
    		Fragment newFragment = getFragment();
    		newFragment.setArguments(bundle);
    		
    		if (newFragment != null) {
    			getSupportFragmentManager().beginTransaction().replace(android.R.id.content, newFragment).commit();
    		}
        }else{
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
		getSupportMenuInflater().inflate(R.menu.detail, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_first:
			startActivity( prepareIntent() );
			return true;
		case R.id.action_second:
			Toast.makeText(this, "Second Action Item", Toast.LENGTH_SHORT)
					.show();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
    private Intent prepareIntent() {
        Intent share = new Intent(android.content.Intent.ACTION_SEND);
        share.setType("text/plain");
        share.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);

        StringBuilder shareText = new StringBuilder();
        
        shareText.append( mNewsEntity.title + " " + mNewsEntity.share_url );

        share.putExtra(Intent.EXTRA_TEXT, shareText.toString());
        return share;
    }
}