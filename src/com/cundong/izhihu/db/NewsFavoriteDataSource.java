package com.cundong.izhihu.db;

import java.util.ArrayList;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.cundong.izhihu.entity.NewsListEntity.NewsEntity;

/**
 * 类说明： 	新闻收藏夹，数据库帮助类
 * 
 * @date 	2014-10-7
 * @version 1.0
 */
public final class NewsFavoriteDataSource extends BaseDataSource {

	private SQLiteDatabase database;
	
	private String[] allColumns = { 
			DatabaseHelper.FAVORITE_COLUMN_ID,
			DatabaseHelper.FAVORITE_COLUMN_NEWS_ID,
			DatabaseHelper.FAVORITE_COLUMN_NEWS_TITLE,
			DatabaseHelper.FAVORITE_COLUMN_NEWS_LOGO,
			DatabaseHelper.FAVORITE_COLUMN_NEWS_SHARE_URL 
			};
	
	public NewsFavoriteDataSource(DatabaseHelper dbHelper) {
		database = dbHelper.getWritableDatabase();
	}

	private void insert(String newsId, String newsTitle, String newsLogo, String newsShareUrl) {
		ContentValues values = new ContentValues();
		values.put(DatabaseHelper.FAVORITE_COLUMN_NEWS_ID, newsId);
		values.put(DatabaseHelper.FAVORITE_COLUMN_NEWS_TITLE, newsTitle);
		values.put(DatabaseHelper.FAVORITE_COLUMN_NEWS_LOGO, newsLogo);
		values.put(DatabaseHelper.FAVORITE_COLUMN_NEWS_SHARE_URL, newsShareUrl);
		
		database.insert(DatabaseHelper.FAVORITE_TABLE_NAME, null, values);
	}

	/**
	 * 收藏
	 * 
	 * @param newsId
	 * @param newsTitle
	 * @param newsLogo
	 * @param newsShareUrl
	 * @return
	 */
	public boolean add2Favorite(String newsId, String newsTitle, String newsLogo, String newsShareUrl) {
		if (TextUtils.isEmpty(newsId))
			return false;

		if (!isInFavorite(newsId)) {
			insert(newsId, newsTitle, newsLogo, newsShareUrl);
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
	public boolean isInFavorite(String newsId) {

		boolean result = false;

		Cursor cursor = database.query(DatabaseHelper.FAVORITE_TABLE_NAME, allColumns,
				DatabaseHelper.FAVORITE_COLUMN_NEWS_ID + " = '" + newsId + "'", null,
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
	public ArrayList<NewsEntity> getFavoriteList() {
		
		ArrayList<NewsEntity> newsList = new ArrayList<NewsEntity>();
		
		String orderBy = DatabaseHelper.FAVORITE_COLUMN_ID + " DESC";
		
		Cursor cursor = database.query(DatabaseHelper.FAVORITE_TABLE_NAME, allColumns,
				null, null, null, null, orderBy);
		
		if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
			
			while (!cursor.isAfterLast()) {
				
				NewsEntity newsEntity = new NewsEntity();
				
				String newsId = cursor.getString(cursor.getColumnIndex(DatabaseHelper.FAVORITE_COLUMN_NEWS_ID));
				String newsTitle = cursor.getString(cursor.getColumnIndex(DatabaseHelper.FAVORITE_COLUMN_NEWS_TITLE));
				String newsLogo = cursor.getString(cursor.getColumnIndex(DatabaseHelper.FAVORITE_COLUMN_NEWS_LOGO));
				String newsShareUrl = cursor.getString(cursor.getColumnIndex(DatabaseHelper.FAVORITE_COLUMN_NEWS_SHARE_URL));
				
				newsEntity.id = Long.parseLong(newsId);
				newsEntity.title = newsTitle;
				
				if (!TextUtils.isEmpty(newsShareUrl)) {
					ArrayList<String> images = new ArrayList<String>();
					images.add(newsLogo);
					newsEntity.images = images;
				}
				
				newsEntity.share_url = newsShareUrl;
				
				newsList.add(newsEntity);
				
				cursor.moveToNext();
			}
		} 

		if (cursor != null) {
			cursor.close();
		}
		
		return newsList;
	}
	
	public void deleteFromFavorite(String newsId) {
		
		String whereClause = DatabaseHelper.FAVORITE_COLUMN_NEWS_ID + "=?";
		
		String [] whereArgs = { String.valueOf(newsId) };
		
		database.delete(DatabaseHelper.FAVORITE_TABLE_NAME, whereClause, whereArgs);
	}
	
	public void deleteFromFavorite() {
		
		database.delete(DatabaseHelper.FAVORITE_TABLE_NAME, null, null);
	}
}