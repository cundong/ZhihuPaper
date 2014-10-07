package com.cundong.izhihu.db;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

/**
 * 类说明： 新闻已读、未读数据表，数据库帮助类
 * 
 * @date 2014-10-7
 * @version 1.0
 */
public final class NewsReadDataSource {

	private SQLiteDatabase database;
	private DBHelper dbHelper;
	private String[] allColumns = { DBHelper.READ_COLUMN_ID,
			DBHelper.READ_COLUMN_NEWSID };

	public NewsReadDataSource(Context context) {
		dbHelper = new DBHelper(context);
		database = dbHelper.getWritableDatabase();
	}

	private void insert(String newsId) {
		ContentValues values = new ContentValues();
		values.put(DBHelper.READ_COLUMN_NEWSID, newsId);
		database.insert(DBHelper.READ_TABLE_NAME, null, values);
	}

	/**
	 * 设置为已读
	 * 
	 * @param newsId
	 * @return
	 */
	public boolean readNews(String newsId) {
		if (TextUtils.isEmpty(newsId))
			return false;

		if (!isNewsRead(newsId)) {
			insert(newsId);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 新闻是否已读
	 * 
	 * @param newsId
	 * @return
	 */
	private boolean isNewsRead(String newsId) {

		boolean result = false;

		Cursor cursor = database.query(DBHelper.READ_TABLE_NAME, allColumns,
				DBHelper.READ_COLUMN_NEWSID + " = '" + newsId + "'", null,
				null, null, null);

		if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
			result = true;
		}

		cursor.close();
		return result;
	}

	/**
	 * 获取所有已读新闻
	 * 
	 * @return
	 */
	public ArrayList<String> getNewsReadList() {
		
		ArrayList<String> newsList = new ArrayList<String>();
		
		Cursor cursor = database.query(DBHelper.READ_TABLE_NAME, allColumns,
				null, null, null, null, null);
		
		if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
			
			while (!cursor.isAfterLast()) {
				String newsId = cursor.getString(cursor.getColumnIndex(DBHelper.READ_COLUMN_NEWSID));
				newsList.add(newsId);
				
				cursor.moveToNext();
			}
		} 
		
		if (cursor != null) {
			cursor.close();
		}
		
		return newsList;
	}
}