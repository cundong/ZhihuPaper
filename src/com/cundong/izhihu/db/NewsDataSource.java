package com.cundong.izhihu.db;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.cundong.izhihu.Constants;
import com.cundong.izhihu.entity.NewsListEntity;
import com.cundong.izhihu.entity.NewsListEntity.NewsEntity;
import com.cundong.izhihu.util.GsonUtils;

/**
 * 类说明： 	新闻列表数据表，数据库帮助类
 * 
 * @date 	2014-9-20
 * @version 1.0
 */
public final class NewsDataSource extends BaseDataSource {

	private SQLiteDatabase database;
	private DBHelper dbHelper;
	private String[] allColumns = { 
			DBHelper.NEWS_COLUMN_ID, 
			DBHelper.NEWS_COLUMN_TYPE,
			DBHelper.NEWS_COLUMN_KEY,
			DBHelper.NEWS_COLUMN_CONTENT };

	public NewsDataSource(Context context) {   
		dbHelper = new DBHelper(context);
		database = dbHelper.getWritableDatabase();
	}

	private ArrayList<NewsEntity> insertDailyNewsList(int type, String key, String content) {
		ContentValues values = new ContentValues();
		values.put(DBHelper.NEWS_COLUMN_TYPE, type);
		values.put(DBHelper.NEWS_COLUMN_KEY, key);
		values.put(DBHelper.NEWS_COLUMN_CONTENT, content);

		long insertId = database.insert(DBHelper.NEWS_TABLE_NAME, null, values);
		Cursor cursor = database.query(DBHelper.NEWS_TABLE_NAME, allColumns,
				DBHelper.NEWS_COLUMN_ID + " = " + insertId, null, null, null, null);
		ArrayList<NewsEntity> newsList = cursorToNewsList(cursor);
		cursor.close();
		return newsList;
	}

	private void updateNewsList(int type, String key, String content) {
		ContentValues values = new ContentValues();
		values.put(DBHelper.NEWS_COLUMN_TYPE, type);
		values.put(DBHelper.NEWS_COLUMN_KEY, key);
		values.put(DBHelper.NEWS_COLUMN_CONTENT, content);
		database.update(DBHelper.NEWS_TABLE_NAME, values, DBHelper.NEWS_COLUMN_KEY
				+ "='" + key + "'", null);
	}
	
	public void insertOrUpdateNewsList(int type, String key, String content) {
		if (TextUtils.isEmpty(content))
			return;

		if (isContentExist(key)) {
			updateNewsList(type, key, content);
		} else {
			insertDailyNewsList(type, key, content);
		}
	}

	private boolean isContentExist(String key) {
		
		boolean result = false;
		
		Cursor cursor = database.query(DBHelper.NEWS_TABLE_NAME, allColumns,
				DBHelper.NEWS_COLUMN_KEY + " = '" + key + "'", null, null, null, null);
		
		if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
			String content = cursor.getString(cursor.getColumnIndex(DBHelper.NEWS_COLUMN_CONTENT));
			result  = !TextUtils.isEmpty(content);
		} 
		
		cursor.close();
		return result;
	}
	
	public String getContent(String key) {
		Cursor cursor = database.query(DBHelper.NEWS_TABLE_NAME, allColumns,
				DBHelper.NEWS_COLUMN_KEY + " = '" + key + "'", null, null, null,
				null);

		if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
			String content = cursor.getString(cursor.getColumnIndex(DBHelper.NEWS_COLUMN_CONTENT));
			cursor.close();
			return content;
		} else {
			return null;
		}
	}
	
	public ArrayList<NewsEntity> getNewsList(String key) {
		Cursor cursor = database.query(DBHelper.NEWS_TABLE_NAME, allColumns,
				DBHelper.NEWS_COLUMN_KEY + " = '" + key + "'", null, null, null,
				null);

		ArrayList<NewsEntity> newsList = cursorToNewsList(cursor);

		cursor.close();
		return newsList;
	}
	
	private ArrayList<NewsEntity> cursorToNewsList(Cursor cursor) {
		if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
			String content = cursor.getString(cursor.getColumnIndex(DBHelper.NEWS_COLUMN_CONTENT));
			return GsonUtils.getNewsList(content);
		} else {
			return null;
		}
	}
	
	/**
	 * 获取新闻表中，日期最新那天的数据
	 * 
	 * @return
	 */
	public NewsListEntity getLatestNews() {

		NewsListEntity newsListEntity = null;

		String orderBy = DBHelper.NEWS_COLUMN_KEY + " desc";

		Cursor cursor = database.query(DBHelper.NEWS_TABLE_NAME, allColumns, DBHelper.NEWS_COLUMN_TYPE + "=" + Constants.NEWS_LIST, null, null, null, orderBy);

		if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
			String content = cursor.getString(cursor.getColumnIndex(DBHelper.NEWS_COLUMN_CONTENT));
			newsListEntity = (NewsListEntity) GsonUtils.getEntity(content, NewsListEntity.class);
		}
		
		cursor.close();

		return newsListEntity;
	}
}