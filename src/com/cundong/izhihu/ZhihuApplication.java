package com.cundong.izhihu;

import android.app.Application;
import android.content.Context;

import com.cundong.izhihu.db.DatabaseHelper;
import com.cundong.izhihu.db.NewsDataSource;
import com.cundong.izhihu.db.NewsFavoriteDataSource;
import com.cundong.izhihu.db.NewsReadDataSource;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;

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
		
		initImageLoader(getApplicationContext());
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
	
	public static void initImageLoader(Context context) {
		// This configuration tuning is custom. You can tune every option, you
		// may tune some of them,
		// or you can create default configuration by
		// ImageLoaderConfiguration.createDefault(this);
		// method.
		ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(
				context).threadPriority(Thread.NORM_PRIORITY - 2)
				.denyCacheImageMultipleSizesInMemory()
				.diskCacheFileNameGenerator(new Md5FileNameGenerator())
				.diskCacheSize(50 * 1024 * 1024)
				// 50 Mb
				.tasksProcessingOrder(QueueProcessingType.LIFO)
				.writeDebugLogs() // Remove for release app
				.build();
		// Initialize ImageLoader with configuration.
		ImageLoader.getInstance().init(config);
	}
}