package com.cundong.izhihu.db;

import java.util.ArrayList;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.cundong.izhihu.entity.ReadingStateEntity;

/** Stores the legacy read marker together with a normalized article reading position. */
public final class NewsReadDataSource extends BaseDataSource {

	private static final String[] ALL_COLUMNS = {
			DatabaseHelper.READ_COLUMN_ID,
			DatabaseHelper.READ_COLUMN_NEWSID,
			DatabaseHelper.READ_COLUMN_READ_AT,
			DatabaseHelper.READ_COLUMN_LAST_READ_AT,
			DatabaseHelper.READ_COLUMN_PROGRESS
	};

	private final SQLiteDatabase database;

	public NewsReadDataSource(DatabaseHelper dbHelper) {
		database = dbHelper.getWritableDatabase();
	}

	/**
	 * Marks an article read. The original return contract is retained: true means a new marker was
	 * inserted, false means the ID was invalid or had already been marked read.
	 */
	public boolean readNews(String newsId) {
		String normalizedId = normalizeNewsId(newsId);
		if (normalizedId == null) {
			return false;
		}
		long timestamp = System.currentTimeMillis();
		ContentValues values = new ContentValues();
		values.put(DatabaseHelper.READ_COLUMN_NEWSID, normalizedId);
		values.put(DatabaseHelper.READ_COLUMN_READ_AT, timestamp);
		values.put(DatabaseHelper.READ_COLUMN_LAST_READ_AT, timestamp);
		values.put(DatabaseHelper.READ_COLUMN_PROGRESS, 0f);
		long inserted = database.insertWithOnConflict(DatabaseHelper.READ_TABLE_NAME, null, values,
				SQLiteDatabase.CONFLICT_IGNORE);
		if (inserted == -1) {
			ContentValues touch = new ContentValues();
			touch.put(DatabaseHelper.READ_COLUMN_LAST_READ_AT, timestamp);
			database.update(DatabaseHelper.READ_TABLE_NAME, touch,
					DatabaseHelper.READ_COLUMN_NEWSID + " = ?",
					new String[] { normalizedId });
		}
		return inserted != -1;
	}

	public boolean isNewsRead(String newsId) {
		String normalizedId = normalizeNewsId(newsId);
		if (normalizedId == null) {
			return false;
		}
		Cursor cursor = null;
		try {
			cursor = database.query(DatabaseHelper.READ_TABLE_NAME,
					new String[] { DatabaseHelper.READ_COLUMN_ID },
					DatabaseHelper.READ_COLUMN_NEWSID + " = ?",
					new String[] { normalizedId }, null, null, null, "1");
			return cursor.moveToFirst();
		} finally {
			close(cursor);
		}
	}

	/** Existing list-screen API; IDs remain strings so malformed legacy values cannot crash it. */
	public ArrayList<String> getNewsReadList() {
		ArrayList<String> newsList = new ArrayList<String>();
		Cursor cursor = null;
		try {
			cursor = database.query(DatabaseHelper.READ_TABLE_NAME,
					new String[] { DatabaseHelper.READ_COLUMN_NEWSID }, null, null, null, null, null);
			while (cursor.moveToNext()) {
				String newsId = cursor.getString(cursor.getColumnIndexOrThrow(
						DatabaseHelper.READ_COLUMN_NEWSID));
				if (newsId != null) {
					newsList.add(newsId);
				}
			}
		} finally {
			close(cursor);
		}
		return newsList;
	}

	/** Creates a read marker when needed, then stores a clamped 0..1 reading position. */
	public boolean updateReadingProgress(String newsId, float progress) {
		String normalizedId = normalizeNewsId(newsId);
		if (normalizedId == null || Float.isNaN(progress) || Float.isInfinite(progress)) {
			return false;
		}
		float normalizedProgress = Math.max(0f, Math.min(1f, progress));
		long timestamp = System.currentTimeMillis();

		ContentValues insertValues = new ContentValues();
		insertValues.put(DatabaseHelper.READ_COLUMN_NEWSID, normalizedId);
		insertValues.put(DatabaseHelper.READ_COLUMN_READ_AT, timestamp);
		insertValues.put(DatabaseHelper.READ_COLUMN_LAST_READ_AT, timestamp);
		insertValues.put(DatabaseHelper.READ_COLUMN_PROGRESS, normalizedProgress);
		database.insertWithOnConflict(DatabaseHelper.READ_TABLE_NAME, null, insertValues,
				SQLiteDatabase.CONFLICT_IGNORE);

		ContentValues updates = new ContentValues();
		updates.put(DatabaseHelper.READ_COLUMN_PROGRESS, normalizedProgress);
		updates.put(DatabaseHelper.READ_COLUMN_LAST_READ_AT, timestamp);
		return database.update(DatabaseHelper.READ_TABLE_NAME, updates,
				DatabaseHelper.READ_COLUMN_NEWSID + " = ?",
				new String[] { normalizedId }) > 0;
	}

	public boolean setReadingProgress(String newsId, float progress) {
		return updateReadingProgress(newsId, progress);
	}

	public float getReadingProgress(String newsId) {
		ReadingStateEntity state = getReadingState(newsId);
		return state == null ? 0f : state.progress;
	}

	public ReadingStateEntity getReadingState(String newsId) {
		String normalizedId = normalizeNewsId(newsId);
		if (normalizedId == null) {
			return null;
		}
		Cursor cursor = null;
		try {
			cursor = database.query(DatabaseHelper.READ_TABLE_NAME, ALL_COLUMNS,
					DatabaseHelper.READ_COLUMN_NEWSID + " = ?",
					new String[] { normalizedId }, null, null, null, "1");
			if (!cursor.moveToFirst()) {
				return null;
			}
			ReadingStateEntity state = new ReadingStateEntity();
			state.newsId = cursor.getString(cursor.getColumnIndexOrThrow(
					DatabaseHelper.READ_COLUMN_NEWSID));
			state.readAt = cursor.getLong(cursor.getColumnIndexOrThrow(
					DatabaseHelper.READ_COLUMN_READ_AT));
			state.lastReadAt = cursor.getLong(cursor.getColumnIndexOrThrow(
					DatabaseHelper.READ_COLUMN_LAST_READ_AT));
			state.progress = cursor.getFloat(cursor.getColumnIndexOrThrow(
					DatabaseHelper.READ_COLUMN_PROGRESS));
			return state;
		} finally {
			close(cursor);
		}
	}

	private static String normalizeNewsId(String newsId) {
		if (newsId == null) {
			return null;
		}
		String normalized = newsId.trim();
		return normalized.length() == 0 ? null : normalized;
	}

	private static void close(Cursor cursor) {
		if (cursor != null) {
			cursor.close();
		}
	}
}
