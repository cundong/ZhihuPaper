package com.cundong.izhihu;

import android.app.Application;

import com.cundong.izhihu.db.DatabaseHelper;
import com.cundong.izhihu.db.NewsDataSource;
import com.cundong.izhihu.db.NewsFavoriteDataSource;
import com.cundong.izhihu.db.NewsReadDataSource;

public class ZhihuApplication extends Application {

	private static ZhihuApplication mApplication;
	
	private DatabaseHelper mDatabaseHelper;  
	
	private static NewsDataSource mNewsDataSource;
	private static NewsReadDataSource mNewsReadDataSource;
	private static NewsFavoriteDataSource mNewsFavoriteDataSource;
	
	@Override
	public void onCreate() {

		super.onCreate();

		mApplication = this;
		
		mDatabaseHelper = DatabaseHelper.getInstance(getApplicationContext());  
		
		mNewsDataSource = new NewsDataSource(mDatabaseHelper);
		mNewsReadDataSource = new NewsReadDataSource(mDatabaseHelper);
		mNewsFavoriteDataSource = new NewsFavoriteDataSource(mDatabaseHelper);
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
	}


	@Override
	public void onTerminate() {
		super.onTerminate();
		
		mDatabaseHelper.close();  
	}

	public static ZhihuApplication getInstance() {
		return mApplication;
	}

	public static NewsDataSource getDataSource() {
		return mNewsDataSource;
	}
	
	public static NewsReadDataSource getNewsReadDataSource() {
		return mNewsReadDataSource;
	}
	
	public static NewsFavoriteDataSource getNewsFavoriteDataSource() {
		return mNewsFavoriteDataSource;
	}
}
