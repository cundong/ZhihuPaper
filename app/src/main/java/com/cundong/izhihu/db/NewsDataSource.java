package com.cundong.izhihu.db;

import java.util.ArrayList;

import android.content.ContentValues;
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
	
	private String[] allColumns = { 
			DatabaseHelper.NEWS_COLUMN_ID, 
			DatabaseHelper.NEWS_COLUMN_TYPE,
			DatabaseHelper.NEWS_COLUMN_KEY,
			DatabaseHelper.NEWS_COLUMN_CONTENT };

	public NewsDataSource(DatabaseHelper dbHelper) {  
		database = dbHelper.getWritableDatabase();
	}

	private ArrayList<NewsEntity> insertDailyNewsList(int type, String key, String content) {
		ContentValues values = new ContentValues();
		values.put(DatabaseHelper.NEWS_COLUMN_TYPE, type);
		values.put(DatabaseHelper.NEWS_COLUMN_KEY, key);
		values.put(DatabaseHelper.NEWS_COLUMN_CONTENT, content);

		long insertId = database.insert(DatabaseHelper.NEWS_TABLE_NAME, null, values);
		Cursor cursor = database.query(DatabaseHelper.NEWS_TABLE_NAME, allColumns,
				DatabaseHelper.NEWS_COLUMN_ID + " = ?", new String[] { String.valueOf(insertId) }, null, null, null);
		ArrayList<NewsEntity> newsList = cursorToNewsList(cursor);
		cursor.close();
		return newsList;
	}

	private void updateNewsList(int type, String key, String content) {
		ContentValues values = new ContentValues();
		values.put(DatabaseHelper.NEWS_COLUMN_TYPE, type);
		values.put(DatabaseHelper.NEWS_COLUMN_KEY, key);
		values.put(DatabaseHelper.NEWS_COLUMN_CONTENT, content);
		database.update(DatabaseHelper.NEWS_TABLE_NAME, values,
				DatabaseHelper.NEWS_COLUMN_KEY + " = ?", new String[] { key });
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
		
		Cursor cursor = database.query(DatabaseHelper.NEWS_TABLE_NAME, allColumns,
				DatabaseHelper.NEWS_COLUMN_KEY + " = ?", new String[] { key }, null, null, null);
		
		if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
			String content = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.NEWS_COLUMN_CONTENT));
			result  = !TextUtils.isEmpty(content);
		} 
		
		cursor.close();
		return result;
	}
	
	public String getContent(String key) {
		Cursor cursor = database.query(DatabaseHelper.NEWS_TABLE_NAME, allColumns,
				DatabaseHelper.NEWS_COLUMN_KEY + " = ?", new String[] { key }, null, null,
				null);

		try {
			return cursor.moveToFirst()
					? cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.NEWS_COLUMN_CONTENT))
					: null;
		} finally {
			cursor.close();
		}
	}

	/** Deletes replaceable cache rows while leaving favorites and read state untouched. */
	public int deleteContentByType(int type) {
		return database.delete(DatabaseHelper.NEWS_TABLE_NAME,
				DatabaseHelper.NEWS_COLUMN_TYPE + " = ?",
				new String[] { String.valueOf(type) });
	}
	
	public ArrayList<NewsEntity> getNewsList(String key) {
		Cursor cursor = database.query(DatabaseHelper.NEWS_TABLE_NAME, allColumns,
				DatabaseHelper.NEWS_COLUMN_KEY + " = ?", new String[] { key }, null, null,
				null);

		ArrayList<NewsEntity> newsList = cursorToNewsList(cursor);

		cursor.close();
		return newsList;
	}
	
	private ArrayList<NewsEntity> cursorToNewsList(Cursor cursor) {
		if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
			String content = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.NEWS_COLUMN_CONTENT));
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

		String orderBy = DatabaseHelper.NEWS_COLUMN_KEY + " desc";

		Cursor cursor = database.query(DatabaseHelper.NEWS_TABLE_NAME, allColumns,
				DatabaseHelper.NEWS_COLUMN_TYPE + " = ?", new String[] { String.valueOf(Constants.NEWS_LIST) }, null, null, orderBy);

		if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
			String content = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.NEWS_COLUMN_CONTENT));
			newsListEntity = (NewsListEntity) GsonUtils.getEntity(content, NewsListEntity.class);
		}
		
		cursor.close();

		return newsListEntity;
	}

	/**
	 * Returns recent list-cache payloads newest first. Callers must still validate
	 * every payload; this lets recovery skip a corrupted newest row.
	 */
	public ArrayList<String> getRecentNewsListContents(int limit) {
		ArrayList<String> contents = new ArrayList<String>();
		if (limit <= 0) {
			return contents;
		}
		Cursor cursor = database.query(DatabaseHelper.NEWS_TABLE_NAME, allColumns,
				DatabaseHelper.NEWS_COLUMN_TYPE + " = ?",
				new String[] { String.valueOf(Constants.NEWS_LIST) }, null, null,
				DatabaseHelper.NEWS_COLUMN_KEY + " desc", String.valueOf(limit));
		try {
			int contentColumn = cursor.getColumnIndexOrThrow(DatabaseHelper.NEWS_COLUMN_CONTENT);
			while (cursor.moveToNext()) {
				contents.add(cursor.getString(contentColumn));
			}
		} finally {
			cursor.close();
		}
		return contents;
	}
}
