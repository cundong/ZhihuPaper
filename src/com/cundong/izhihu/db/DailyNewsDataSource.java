package com.cundong.izhihu.db;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.cundong.izhihu.entity.NewsListEntity.NewsEntity;
import com.cundong.izhihu.util.GsonUtils;

public final class DailyNewsDataSource {

	private SQLiteDatabase database;
	private DBHelper dbHelper;
	private String[] allColumns = { DBHelper.COLUMN_ID, DBHelper.COLUMN_KEY,
			DBHelper.COLUMN_CONTENT };

	public DailyNewsDataSource(Context context) {   
		dbHelper = new DBHelper(context);
		database = dbHelper.getWritableDatabase();
	}

	public ArrayList<NewsEntity> insertDailyNewsList(String key, String content) {
		ContentValues values = new ContentValues();
		values.put(DBHelper.COLUMN_KEY, key);
		values.put(DBHelper.COLUMN_CONTENT, content);

		long insertId = database.insert(DBHelper.TABLE_NAME, null, values);
		Cursor cursor = database.query(DBHelper.TABLE_NAME, allColumns,
				DBHelper.COLUMN_ID + " = " + insertId, null, null, null, null);
		ArrayList<NewsEntity> newsList = cursorToNewsList(cursor);
		cursor.close();
		return newsList;
	}

	public void updateNewsList(String key, String content) {
		ContentValues values = new ContentValues();
		values.put(DBHelper.COLUMN_KEY, key);
		values.put(DBHelper.COLUMN_CONTENT, content);
		database.update(DBHelper.TABLE_NAME, values, DBHelper.COLUMN_KEY
				+ "='" + key + "'", null);
	}
	
	public void insertOrUpdateNewsList(String key, String content) {
		if (TextUtils.isEmpty(content))
			return;

		if (isContentExist(key)) {
			updateNewsList(key, content);
		} else {
			insertDailyNewsList(key, content);
		}
	}

	private boolean isContentExist(String key) {
		
		boolean result = false;
		
		Cursor cursor = database.query(DBHelper.TABLE_NAME, allColumns,
				DBHelper.COLUMN_KEY + " = '" + key + "'", null, null, null, null);
		
		if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
			String content = cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_CONTENT));
			result  = !TextUtils.isEmpty(content);
		} 
		
		cursor.close();
		return result;
	}
	
	public String getContent(String key) {
		Cursor cursor = database.query(DBHelper.TABLE_NAME, allColumns,
				DBHelper.COLUMN_KEY + " = '" + key + "'", null, null, null,
				null);

		if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
			String content = cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_CONTENT));
			cursor.close();
			return content;
		} else {
			return null;
		}
	}
	
	public ArrayList<NewsEntity> getNewsList(String key) {
		Cursor cursor = database.query(DBHelper.TABLE_NAME, allColumns,
				DBHelper.COLUMN_KEY + " = '" + key + "'", null, null, null,
				null);

		ArrayList<NewsEntity> newsList = cursorToNewsList(cursor);

		cursor.close();
		return newsList;
	}

	private ArrayList<NewsEntity> cursorToNewsList(Cursor cursor) {
		if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
			String content = cursor.getString(cursor
					.getColumnIndex(DBHelper.COLUMN_CONTENT));
			return GsonUtils.getNewsList(content);
		} else {
			return null;
		}
	}
}