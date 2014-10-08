package com.cundong.izhihu.db;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.cundong.izhihu.entity.FavoriteEntity;

/**
 * 类说明： 	新闻收藏夹，数据库帮助类
 * 
 * @date 	2014-10-7
 * @version 1.0
 */
public final class NewsFavoriteDataSource {

	private SQLiteDatabase database;
	private DBHelper dbHelper;
	
	private String[] allColumns = { 
			DBHelper.FAVORITE_COLUMN_ID,
			DBHelper.FAVORITE_COLUMN_NEWS_ID,
			DBHelper.FAVORITE_COLUMN_NEWS_TITLE,
			DBHelper.FAVORITE_COLUMN_NEWS_LOGO
			};
	
	public NewsFavoriteDataSource(Context context) {
		dbHelper = new DBHelper(context);
		database = dbHelper.getWritableDatabase();
	}

	private void insert(String newsId, String newsTitle, String newsLogo) {
		ContentValues values = new ContentValues();
		values.put(DBHelper.FAVORITE_COLUMN_NEWS_ID, newsId);
		values.put(DBHelper.FAVORITE_COLUMN_NEWS_TITLE, newsTitle);
		values.put(DBHelper.FAVORITE_COLUMN_NEWS_LOGO, newsLogo);
		database.insert(DBHelper.FAVORITE_TABLE_NAME, null, values);
	}

	/**
	 * 收藏
	 * 
	 * @param newsId
	 * @param newsTitle
	 * @param newsLogo
	 * @return
	 */
	public boolean add2Favorite(String newsId, String newsTitle, String newsLogo) {
		if (TextUtils.isEmpty(newsId))
			return false;

		if (!isInFavorite(newsId)) {
			insert(newsId, newsTitle, newsLogo);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 新闻是否已收藏
	 * 
	 * @param newsId
	 * @return
	 */
	private boolean isInFavorite(String newsId) {

		boolean result = false;

		Cursor cursor = database.query(DBHelper.FAVORITE_TABLE_NAME, allColumns,
				DBHelper.FAVORITE_COLUMN_NEWS_ID + " = '" + newsId + "'", null,
				null, null, null);
		
		if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
			result = true;
		}
		
		cursor.close();
		return result;
	}

	/**
	 * 获取收藏夹中所有新闻
	 * 
	 * @return
	 */
	public ArrayList<FavoriteEntity> getFavoriteList() {
		
		ArrayList<FavoriteEntity> newsList = new ArrayList<FavoriteEntity>();
		
		Cursor cursor = database.query(DBHelper.FAVORITE_TABLE_NAME, allColumns,
				null, null, null, null, null);
		
		if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
			
			while (!cursor.isAfterLast()) {
				
				FavoriteEntity favoriteEntity = new FavoriteEntity();
				
				String newsId = cursor.getString(cursor.getColumnIndex(DBHelper.FAVORITE_COLUMN_NEWS_ID));
				String newsTitle = cursor.getString(cursor.getColumnIndex(DBHelper.FAVORITE_COLUMN_NEWS_TITLE));
				String newsLogo = cursor.getString(cursor.getColumnIndex(DBHelper.FAVORITE_COLUMN_NEWS_LOGO));
				
				favoriteEntity.newsId = newsId;
				favoriteEntity.newsTitle = newsTitle;
				favoriteEntity.newsLogo = newsLogo;
				
				newsList.add(favoriteEntity);
				
				cursor.moveToNext();
			}
		} 
		
		if (cursor != null) {
			cursor.close();
		}
		
		return newsList;
	}
}