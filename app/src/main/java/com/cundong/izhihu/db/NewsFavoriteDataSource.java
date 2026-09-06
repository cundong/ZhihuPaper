package com.cundong.izhihu.db;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.Map;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.cundong.izhihu.entity.HighlightEntity;
import com.cundong.izhihu.entity.LibraryItemEntity;
import com.cundong.izhihu.entity.NewsListEntity.NewsEntity;

/**
 * Favorite-compatible facade over the local reading library.
 *
 * <p>The legacy methods are retained for the existing activities. New methods expose notes,
 * read-later state, tags, article bodies and highlights without forcing a screen-level rewrite.</p>
 */
public final class NewsFavoriteDataSource extends BaseDataSource {
	public static final int LIBRARY_FILTER_ALL = 0;
	public static final int LIBRARY_FILTER_FAVORITES = 1;
	public static final int LIBRARY_FILTER_READ_LATER = 2;

	private static final String[] LIBRARY_COLUMNS = {
			DatabaseHelper.FAVORITE_COLUMN_ID,
			DatabaseHelper.FAVORITE_COLUMN_NEWS_ID,
			DatabaseHelper.FAVORITE_COLUMN_NEWS_TITLE,
			DatabaseHelper.FAVORITE_COLUMN_NEWS_LOGO,
			DatabaseHelper.FAVORITE_COLUMN_NEWS_SHARE_URL,
			DatabaseHelper.FAVORITE_COLUMN_IS_FAVORITE,
			DatabaseHelper.FAVORITE_COLUMN_IS_READ_LATER,
			DatabaseHelper.FAVORITE_COLUMN_NOTE,
			DatabaseHelper.FAVORITE_COLUMN_BODY,
			DatabaseHelper.FAVORITE_COLUMN_CREATED_AT,
			DatabaseHelper.FAVORITE_COLUMN_UPDATED_AT
	};

	private static final String ACTIVE_LIBRARY_SELECTION = "("
			+ DatabaseHelper.FAVORITE_COLUMN_IS_FAVORITE + " = 1 OR "
			+ DatabaseHelper.FAVORITE_COLUMN_IS_READ_LATER + " = 1 OR COALESCE("
			+ DatabaseHelper.FAVORITE_COLUMN_NOTE + ", '') <> '' OR COALESCE("
			+ DatabaseHelper.FAVORITE_COLUMN_BODY + ", '') <> '' OR EXISTS (SELECT 1 FROM "
			+ DatabaseHelper.ITEM_TAG_TABLE_NAME + " WHERE "
			+ DatabaseHelper.ITEM_TAG_TABLE_NAME + "." + DatabaseHelper.ITEM_TAG_COLUMN_NEWS_ID
			+ " = " + DatabaseHelper.FAVORITE_TABLE_NAME + "."
			+ DatabaseHelper.FAVORITE_COLUMN_NEWS_ID + ") OR EXISTS (SELECT 1 FROM "
			+ DatabaseHelper.HIGHLIGHT_TABLE_NAME + " WHERE "
			+ DatabaseHelper.HIGHLIGHT_TABLE_NAME + "." + DatabaseHelper.HIGHLIGHT_COLUMN_NEWS_ID
			+ " = " + DatabaseHelper.FAVORITE_TABLE_NAME + "."
			+ DatabaseHelper.FAVORITE_COLUMN_NEWS_ID + "))";

	private final SQLiteDatabase database;

	public NewsFavoriteDataSource(DatabaseHelper dbHelper) {
		database = dbHelper.getWritableDatabase();
	}

	/** Preserves the original API: returns true only when favorite state changed to selected. */
	public boolean add2Favorite(String newsId, String newsTitle, String newsLogo,
			String newsShareUrl) {
		String normalizedId = normalizeNewsId(newsId);
		if (normalizedId == null) {
			return false;
		}

		boolean alreadyFavorite = isInFavorite(normalizedId);
		if (!ensureItem(normalizedId, newsTitle, newsLogo, newsShareUrl)) {
			return false;
		}

		ContentValues values = new ContentValues();
		values.put(DatabaseHelper.FAVORITE_COLUMN_IS_FAVORITE, 1);
		values.put(DatabaseHelper.FAVORITE_COLUMN_UPDATED_AT, now());
		int changed = database.update(DatabaseHelper.FAVORITE_TABLE_NAME, values,
				DatabaseHelper.FAVORITE_COLUMN_NEWS_ID + " = ?",
				new String[] { normalizedId });
		return !alreadyFavorite && changed > 0;
	}

	/** Returns whether the item is currently selected as a favorite. */
	public boolean isInFavorite(String newsId) {
		String normalizedId = normalizeNewsId(newsId);
		if (normalizedId == null) {
			return false;
		}

		Cursor cursor = null;
		try {
			cursor = database.query(DatabaseHelper.FAVORITE_TABLE_NAME,
					new String[] { DatabaseHelper.FAVORITE_COLUMN_ID },
					DatabaseHelper.FAVORITE_COLUMN_NEWS_ID + " = ? AND "
							+ DatabaseHelper.FAVORITE_COLUMN_IS_FAVORITE + " = 1",
					new String[] { normalizedId }, null, null, null, "1");
			return cursor.moveToFirst();
		} finally {
			close(cursor);
		}
	}

	/** Existing favorite screen model. Malformed non-numeric IDs are retained in the database but skipped. */
	public ArrayList<NewsEntity> getFavoriteList() {
		ArrayList<NewsEntity> newsList = new ArrayList<NewsEntity>();
		Cursor cursor = null;
		try {
			cursor = database.query(DatabaseHelper.FAVORITE_TABLE_NAME, LIBRARY_COLUMNS,
					DatabaseHelper.FAVORITE_COLUMN_IS_FAVORITE + " = 1", null, null, null,
					DatabaseHelper.FAVORITE_COLUMN_UPDATED_AT + " DESC, "
							+ DatabaseHelper.FAVORITE_COLUMN_ID + " DESC");
			while (cursor.moveToNext()) {
				Long numericId = parseNewsId(cursor.getString(cursor.getColumnIndexOrThrow(
						DatabaseHelper.FAVORITE_COLUMN_NEWS_ID)));
				if (numericId == null) {
					continue;
				}
				NewsEntity newsEntity = new NewsEntity();
				newsEntity.id = numericId;
				newsEntity.title = cursor.getString(cursor.getColumnIndexOrThrow(
						DatabaseHelper.FAVORITE_COLUMN_NEWS_TITLE));
				String logo = cursor.getString(cursor.getColumnIndexOrThrow(
						DatabaseHelper.FAVORITE_COLUMN_NEWS_LOGO));
				if (!TextUtils.isEmpty(logo)) {
					newsEntity.images = new ArrayList<String>();
					newsEntity.images.add(logo);
				}
				newsEntity.share_url = cursor.getString(cursor.getColumnIndexOrThrow(
						DatabaseHelper.FAVORITE_COLUMN_NEWS_SHARE_URL));
				newsList.add(newsEntity);
			}
		} finally {
			close(cursor);
		}
		return newsList;
	}

	/** Clears favorite state only; notes, tags, highlights and read-later state remain intact. */
	public boolean deleteFromFavorite(String newsId) {
		String normalizedId = normalizeNewsId(newsId);
		if (normalizedId == null) {
			return false;
		}
		ContentValues values = new ContentValues();
		values.put(DatabaseHelper.FAVORITE_COLUMN_IS_FAVORITE, 0);
		values.put(DatabaseHelper.FAVORITE_COLUMN_UPDATED_AT, now());
		return database.update(DatabaseHelper.FAVORITE_TABLE_NAME, values,
				DatabaseHelper.FAVORITE_COLUMN_NEWS_ID + " = ?", new String[] { normalizedId }) > 0;
	}

	/** Clears every favorite bit while preserving all library rows and user-authored data. */
	public void deleteFromFavorite() {
		ContentValues values = new ContentValues();
		values.put(DatabaseHelper.FAVORITE_COLUMN_IS_FAVORITE, 0);
		values.put(DatabaseHelper.FAVORITE_COLUMN_UPDATED_AT, now());
		database.update(DatabaseHelper.FAVORITE_TABLE_NAME, values,
				DatabaseHelper.FAVORITE_COLUMN_IS_FAVORITE + " = 1", null);
	}

	/** Ensures a library row exists and refreshes any supplied article metadata. */
	public boolean ensureItem(String newsId, String newsTitle, String newsLogo,
			String newsShareUrl) {
		String normalizedId = normalizeNewsId(newsId);
		if (normalizedId == null) {
			return false;
		}

		long timestamp = now();
		ContentValues insertValues = new ContentValues();
		insertValues.put(DatabaseHelper.FAVORITE_COLUMN_NEWS_ID, normalizedId);
		putNullable(insertValues, DatabaseHelper.FAVORITE_COLUMN_NEWS_TITLE, newsTitle);
		putNullable(insertValues, DatabaseHelper.FAVORITE_COLUMN_NEWS_LOGO, newsLogo);
		putNullable(insertValues, DatabaseHelper.FAVORITE_COLUMN_NEWS_SHARE_URL, newsShareUrl);
		insertValues.put(DatabaseHelper.FAVORITE_COLUMN_IS_FAVORITE, 0);
		insertValues.put(DatabaseHelper.FAVORITE_COLUMN_IS_READ_LATER, 0);
		insertValues.put(DatabaseHelper.FAVORITE_COLUMN_CREATED_AT, timestamp);
		insertValues.put(DatabaseHelper.FAVORITE_COLUMN_UPDATED_AT, timestamp);
		long inserted = database.insertWithOnConflict(DatabaseHelper.FAVORITE_TABLE_NAME, null,
				insertValues, SQLiteDatabase.CONFLICT_IGNORE);

		ContentValues updates = new ContentValues();
		putIfSupplied(updates, DatabaseHelper.FAVORITE_COLUMN_NEWS_TITLE, newsTitle);
		putIfSupplied(updates, DatabaseHelper.FAVORITE_COLUMN_NEWS_LOGO, newsLogo);
		putIfSupplied(updates, DatabaseHelper.FAVORITE_COLUMN_NEWS_SHARE_URL, newsShareUrl);
		if (updates.size() > 0) {
			updates.put(DatabaseHelper.FAVORITE_COLUMN_UPDATED_AT, timestamp);
			int updated = database.update(DatabaseHelper.FAVORITE_TABLE_NAME, updates,
					DatabaseHelper.FAVORITE_COLUMN_NEWS_ID + " = ?",
					new String[] { normalizedId });
			// A newly inserted row already contains the supplied metadata. Existing rows must report
			// a successful metadata update; otherwise an atomic caller needs to roll everything back.
			if (inserted == -1 && updated <= 0) {
				return false;
			}
		}
		return inserted != -1 || itemExists(normalizedId);
	}

	/**
	 * Saves every field edited by the reading-library dialog as one unit.
	 *
	 * <p>Calling the individual compatibility methods would allow a failed tag or body write to
	 * leave a partially updated item. This API deliberately owns the transaction so creating the
	 * row, refreshing metadata, changing read-later/note/tags and caching the article body either
	 * all commit or all roll back. A {@code null} article body means that the caller has no newly
	 * loaded body and therefore preserves any body already stored for offline reading.</p>
	 */
	public boolean saveLibraryItemAtomically(String newsId, String newsTitle, String newsLogo,
			String newsShareUrl, boolean readLater, String note, Collection<String> tags,
			String articleBody) {
		String normalizedId = normalizeNewsId(newsId);
		if (normalizedId == null) {
			return false;
		}
		LinkedHashSet<String> normalizedTags = normalizeTags(tags);

		try {
			database.beginTransaction();
			try {
				if (!ensureItem(normalizedId, newsTitle, newsLogo, newsShareUrl)) {
					return false;
				}

				ContentValues values = new ContentValues();
				values.put(DatabaseHelper.FAVORITE_COLUMN_IS_READ_LATER, readLater ? 1 : 0);
				putNullable(values, DatabaseHelper.FAVORITE_COLUMN_NOTE, note);
				if (articleBody != null) {
					values.put(DatabaseHelper.FAVORITE_COLUMN_BODY, articleBody);
				}
				values.put(DatabaseHelper.FAVORITE_COLUMN_UPDATED_AT, now());
				if (database.update(DatabaseHelper.FAVORITE_TABLE_NAME, values,
						DatabaseHelper.FAVORITE_COLUMN_NEWS_ID + " = ?",
						new String[] { normalizedId }) <= 0
						|| !replaceTagMappings(normalizedId, normalizedTags)) {
					return false;
				}

				database.setTransactionSuccessful();
				return true;
			} finally {
				database.endTransaction();
			}
		} catch (RuntimeException storageFailure) {
			// The dialog expects a boolean result. endTransaction rolls back unless the complete set of
			// writes reached setTransactionSuccessful().
			storageFailure.printStackTrace();
			return false;
		}
	}

	public boolean updateNote(String newsId, String note) {
		return updateLibraryText(newsId, DatabaseHelper.FAVORITE_COLUMN_NOTE, note);
	}

	public boolean setNote(String newsId, String note) {
		return updateNote(newsId, note);
	}

	public boolean saveNote(String newsId, String note) {
		return updateNote(newsId, note);
	}

	public boolean updateArticleBody(String newsId, String body) {
		return updateLibraryText(newsId, DatabaseHelper.FAVORITE_COLUMN_BODY, body);
	}

	public boolean setArticleBody(String newsId, String body) {
		return updateArticleBody(newsId, body);
	}

	public boolean setReadLater(String newsId, boolean readLater) {
		String normalizedId = normalizeNewsId(newsId);
		if (normalizedId == null || !ensureItem(normalizedId, null, null, null)) {
			return false;
		}
		ContentValues values = new ContentValues();
		values.put(DatabaseHelper.FAVORITE_COLUMN_IS_READ_LATER, readLater ? 1 : 0);
		values.put(DatabaseHelper.FAVORITE_COLUMN_UPDATED_AT, now());
		return database.update(DatabaseHelper.FAVORITE_TABLE_NAME, values,
				DatabaseHelper.FAVORITE_COLUMN_NEWS_ID + " = ?",
				new String[] { normalizedId }) > 0;
	}

	public LibraryItemEntity getLibraryItem(String newsId) {
		String normalizedId = normalizeNewsId(newsId);
		if (normalizedId == null) {
			return null;
		}
		Cursor cursor = null;
		try {
			cursor = database.query(DatabaseHelper.FAVORITE_TABLE_NAME, LIBRARY_COLUMNS,
					DatabaseHelper.FAVORITE_COLUMN_NEWS_ID + " = ?",
					new String[] { normalizedId }, null, null, null, "1");
			if (!cursor.moveToFirst()) {
				return null;
			}
			LibraryItemEntity item = toLibraryItem(cursor);
			item.tags = getTags(item.newsId);
			return item;
		} finally {
			close(cursor);
		}
	}

	public ArrayList<LibraryItemEntity> getLibraryItems() {
		return queryLibrary(ACTIVE_LIBRARY_SELECTION, null);
	}

	/** Searches title, note, stored body and tag names using a literal substring query. */
	public ArrayList<LibraryItemEntity> searchLibrary(String query) {
		return searchLibrary(query, LIBRARY_FILTER_ALL);
	}

	/** Supports the all/favorites/read-later filters without exposing SQL to the UI. */
	public ArrayList<LibraryItemEntity> searchLibrary(String query, int filter) {
		String normalizedQuery = query == null ? "" : query.trim();
		String stateSelection = ACTIVE_LIBRARY_SELECTION;
		if (filter == LIBRARY_FILTER_FAVORITES) {
			stateSelection += " AND " + DatabaseHelper.FAVORITE_COLUMN_IS_FAVORITE + " = 1";
		} else if (filter == LIBRARY_FILTER_READ_LATER) {
			stateSelection += " AND " + DatabaseHelper.FAVORITE_COLUMN_IS_READ_LATER + " = 1";
		}
		if (normalizedQuery.length() == 0) {
			return queryLibrary(stateSelection, null);
		}
		String pattern = "%" + escapeLike(normalizedQuery) + "%";
		String match = "(" + DatabaseHelper.FAVORITE_COLUMN_NEWS_TITLE
				+ " LIKE ? ESCAPE '\\' OR " + DatabaseHelper.FAVORITE_COLUMN_NOTE
				+ " LIKE ? ESCAPE '\\' OR " + DatabaseHelper.FAVORITE_COLUMN_BODY
				+ " LIKE ? ESCAPE '\\' OR EXISTS (SELECT 1 FROM "
				+ DatabaseHelper.ITEM_TAG_TABLE_NAME + " INNER JOIN " + DatabaseHelper.TAG_TABLE_NAME
				+ " ON " + DatabaseHelper.TAG_TABLE_NAME + "." + DatabaseHelper.TAG_COLUMN_ID + " = "
				+ DatabaseHelper.ITEM_TAG_TABLE_NAME + "." + DatabaseHelper.ITEM_TAG_COLUMN_TAG_ID
				+ " WHERE " + DatabaseHelper.ITEM_TAG_TABLE_NAME + "."
				+ DatabaseHelper.ITEM_TAG_COLUMN_NEWS_ID + " = "
				+ DatabaseHelper.FAVORITE_TABLE_NAME + "." + DatabaseHelper.FAVORITE_COLUMN_NEWS_ID
				+ " AND " + DatabaseHelper.TAG_TABLE_NAME + "." + DatabaseHelper.TAG_COLUMN_NAME
				+ " LIKE ? ESCAPE '\\'))";
		return queryLibrary(stateSelection + " AND " + match,
				new String[] { pattern, pattern, pattern, pattern });
	}

	public boolean addTag(String newsId, String tag) {
		String normalizedId = normalizeNewsId(newsId);
		String normalizedTag = normalizeTag(tag);
		if (normalizedId == null || normalizedTag == null) {
			return false;
		}

		database.beginTransaction();
		try {
			if (!ensureItem(normalizedId, null, null, null)
					|| !addTagMapping(normalizedId, normalizedTag)) {
				return false;
			}
			touchItem(normalizedId);
			database.setTransactionSuccessful();
			return true;
		} finally {
			database.endTransaction();
		}
	}

	public boolean removeTag(String newsId, String tag) {
		String normalizedId = normalizeNewsId(newsId);
		String normalizedTag = normalizeTag(tag);
		if (normalizedId == null || normalizedTag == null) {
			return false;
		}
		String where = DatabaseHelper.ITEM_TAG_COLUMN_NEWS_ID + " = ? AND "
				+ DatabaseHelper.ITEM_TAG_COLUMN_TAG_ID + " IN (SELECT "
				+ DatabaseHelper.TAG_COLUMN_ID + " FROM " + DatabaseHelper.TAG_TABLE_NAME + " WHERE "
				+ DatabaseHelper.TAG_COLUMN_NAME + " = ? COLLATE NOCASE)";
		int deleted = database.delete(DatabaseHelper.ITEM_TAG_TABLE_NAME, where,
				new String[] { normalizedId, normalizedTag });
		if (deleted > 0) {
			touchItem(normalizedId);
		}
		return deleted > 0;
	}

	public boolean setTags(String newsId, Collection<String> tags) {
		String normalizedId = normalizeNewsId(newsId);
		if (normalizedId == null) {
			return false;
		}
		LinkedHashSet<String> normalizedTags = normalizeTags(tags);

		database.beginTransaction();
		try {
			if (!ensureItem(normalizedId, null, null, null)
					|| !replaceTagMappings(normalizedId, normalizedTags)) {
				return false;
			}
			touchItem(normalizedId);
			database.setTransactionSuccessful();
			return true;
		} finally {
			database.endTransaction();
		}
	}

	public boolean replaceTags(String newsId, Collection<String> tags) {
		return setTags(newsId, tags);
	}

	public ArrayList<String> getTags(String newsId) {
		ArrayList<String> tags = new ArrayList<String>();
		String normalizedId = normalizeNewsId(newsId);
		if (normalizedId == null) {
			return tags;
		}
		Cursor cursor = null;
		try {
			String sql = "SELECT " + DatabaseHelper.TAG_TABLE_NAME + "."
					+ DatabaseHelper.TAG_COLUMN_NAME + " FROM " + DatabaseHelper.TAG_TABLE_NAME
					+ " INNER JOIN " + DatabaseHelper.ITEM_TAG_TABLE_NAME + " ON "
					+ DatabaseHelper.ITEM_TAG_TABLE_NAME + "."
					+ DatabaseHelper.ITEM_TAG_COLUMN_TAG_ID + " = " + DatabaseHelper.TAG_TABLE_NAME
					+ "." + DatabaseHelper.TAG_COLUMN_ID + " WHERE "
					+ DatabaseHelper.ITEM_TAG_TABLE_NAME + "."
					+ DatabaseHelper.ITEM_TAG_COLUMN_NEWS_ID + " = ? ORDER BY "
					+ DatabaseHelper.TAG_TABLE_NAME + "." + DatabaseHelper.TAG_COLUMN_NAME
					+ " COLLATE NOCASE";
			cursor = database.rawQuery(sql, new String[] { normalizedId });
			while (cursor.moveToNext()) {
				tags.add(cursor.getString(0));
			}
		} finally {
			close(cursor);
		}
		return tags;
	}

	public ArrayList<String> getAllTags() {
		ArrayList<String> tags = new ArrayList<String>();
		Cursor cursor = null;
		try {
			String sql = "SELECT DISTINCT " + DatabaseHelper.TAG_TABLE_NAME + "."
					+ DatabaseHelper.TAG_COLUMN_NAME + " FROM " + DatabaseHelper.TAG_TABLE_NAME
					+ " INNER JOIN " + DatabaseHelper.ITEM_TAG_TABLE_NAME + " ON "
					+ DatabaseHelper.ITEM_TAG_TABLE_NAME + "."
					+ DatabaseHelper.ITEM_TAG_COLUMN_TAG_ID + " = " + DatabaseHelper.TAG_TABLE_NAME
					+ "." + DatabaseHelper.TAG_COLUMN_ID + " ORDER BY "
					+ DatabaseHelper.TAG_TABLE_NAME + "." + DatabaseHelper.TAG_COLUMN_NAME
					+ " COLLATE NOCASE";
			cursor = database.rawQuery(sql, null);
			while (cursor.moveToNext()) {
				tags.add(cursor.getString(0));
			}
		} finally {
			close(cursor);
		}
		return tags;
	}

	public long addHighlight(String newsId, String quote, String note, int startOffset,
			int endOffset) {
		String normalizedId = normalizeNewsId(newsId);
		String normalizedQuote = quote == null ? null : quote.trim();
		if (normalizedId == null || TextUtils.isEmpty(normalizedQuote)
				|| !ensureItem(normalizedId, null, null, null)) {
			return -1;
		}
		if (startOffset < 0 || endOffset < startOffset) {
			startOffset = -1;
			endOffset = -1;
		}
		long timestamp = now();
		ContentValues values = new ContentValues();
		values.put(DatabaseHelper.HIGHLIGHT_COLUMN_NEWS_ID, normalizedId);
		values.put(DatabaseHelper.HIGHLIGHT_COLUMN_QUOTE, normalizedQuote);
		putNullable(values, DatabaseHelper.HIGHLIGHT_COLUMN_NOTE, note);
		values.put(DatabaseHelper.HIGHLIGHT_COLUMN_START_OFFSET, startOffset);
		values.put(DatabaseHelper.HIGHLIGHT_COLUMN_END_OFFSET, endOffset);
		values.put(DatabaseHelper.HIGHLIGHT_COLUMN_CREATED_AT, timestamp);
		values.put(DatabaseHelper.HIGHLIGHT_COLUMN_UPDATED_AT, timestamp);
		long id = database.insert(DatabaseHelper.HIGHLIGHT_TABLE_NAME, null, values);
		if (id != -1) {
			touchItem(normalizedId);
		}
		return id;
	}

	public ArrayList<HighlightEntity> getHighlights(String newsId) {
		ArrayList<HighlightEntity> highlights = new ArrayList<HighlightEntity>();
		String normalizedId = normalizeNewsId(newsId);
		if (normalizedId == null) {
			return highlights;
		}
		Cursor cursor = null;
		try {
			cursor = database.query(DatabaseHelper.HIGHLIGHT_TABLE_NAME, null,
					DatabaseHelper.HIGHLIGHT_COLUMN_NEWS_ID + " = ?",
					new String[] { normalizedId }, null, null,
					DatabaseHelper.HIGHLIGHT_COLUMN_CREATED_AT + " ASC, "
							+ DatabaseHelper.HIGHLIGHT_COLUMN_ID + " ASC");
			while (cursor.moveToNext()) {
				HighlightEntity highlight = new HighlightEntity();
				highlight.id = cursor.getLong(cursor.getColumnIndexOrThrow(
						DatabaseHelper.HIGHLIGHT_COLUMN_ID));
				highlight.newsId = cursor.getString(cursor.getColumnIndexOrThrow(
						DatabaseHelper.HIGHLIGHT_COLUMN_NEWS_ID));
				highlight.quote = cursor.getString(cursor.getColumnIndexOrThrow(
						DatabaseHelper.HIGHLIGHT_COLUMN_QUOTE));
				highlight.note = cursor.getString(cursor.getColumnIndexOrThrow(
						DatabaseHelper.HIGHLIGHT_COLUMN_NOTE));
				highlight.startOffset = cursor.getInt(cursor.getColumnIndexOrThrow(
						DatabaseHelper.HIGHLIGHT_COLUMN_START_OFFSET));
				highlight.endOffset = cursor.getInt(cursor.getColumnIndexOrThrow(
						DatabaseHelper.HIGHLIGHT_COLUMN_END_OFFSET));
				highlight.createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(
						DatabaseHelper.HIGHLIGHT_COLUMN_CREATED_AT));
				highlight.updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow(
						DatabaseHelper.HIGHLIGHT_COLUMN_UPDATED_AT));
				highlights.add(highlight);
			}
		} finally {
			close(cursor);
		}
		return highlights;
	}

	public boolean updateHighlightNote(long highlightId, String note) {
		if (highlightId <= 0) {
			return false;
		}
		ContentValues values = new ContentValues();
		putNullable(values, DatabaseHelper.HIGHLIGHT_COLUMN_NOTE, note);
		values.put(DatabaseHelper.HIGHLIGHT_COLUMN_UPDATED_AT, now());
		return database.update(DatabaseHelper.HIGHLIGHT_TABLE_NAME, values,
				DatabaseHelper.HIGHLIGHT_COLUMN_ID + " = ?",
				new String[] { String.valueOf(highlightId) }) > 0;
	}

	public boolean deleteHighlight(long highlightId) {
		if (highlightId <= 0) {
			return false;
		}
		return database.delete(DatabaseHelper.HIGHLIGHT_TABLE_NAME,
				DatabaseHelper.HIGHLIGHT_COLUMN_ID + " = ?",
				new String[] { String.valueOf(highlightId) }) > 0;
	}

	private boolean updateLibraryText(String newsId, String column, String value) {
		String normalizedId = normalizeNewsId(newsId);
		if (normalizedId == null || !ensureItem(normalizedId, null, null, null)) {
			return false;
		}
		ContentValues values = new ContentValues();
		putNullable(values, column, value);
		values.put(DatabaseHelper.FAVORITE_COLUMN_UPDATED_AT, now());
		return database.update(DatabaseHelper.FAVORITE_TABLE_NAME, values,
				DatabaseHelper.FAVORITE_COLUMN_NEWS_ID + " = ?",
				new String[] { normalizedId }) > 0;
	}

	private ArrayList<LibraryItemEntity> queryLibrary(String selection, String[] selectionArgs) {
		ArrayList<LibraryItemEntity> items = new ArrayList<LibraryItemEntity>();
		Cursor cursor = null;
		try {
			cursor = database.query(DatabaseHelper.FAVORITE_TABLE_NAME, LIBRARY_COLUMNS,
					selection, selectionArgs, null, null,
					DatabaseHelper.FAVORITE_COLUMN_UPDATED_AT + " DESC, "
							+ DatabaseHelper.FAVORITE_COLUMN_ID + " DESC");
			while (cursor.moveToNext()) {
				items.add(toLibraryItem(cursor));
			}
		} finally {
			close(cursor);
		}
		loadTags(items);
		return items;
	}

	private LibraryItemEntity toLibraryItem(Cursor cursor) {
		LibraryItemEntity item = new LibraryItemEntity();
		item.newsId = cursor.getString(cursor.getColumnIndexOrThrow(
				DatabaseHelper.FAVORITE_COLUMN_NEWS_ID));
		item.title = cursor.getString(cursor.getColumnIndexOrThrow(
				DatabaseHelper.FAVORITE_COLUMN_NEWS_TITLE));
		item.logo = cursor.getString(cursor.getColumnIndexOrThrow(
				DatabaseHelper.FAVORITE_COLUMN_NEWS_LOGO));
		item.shareUrl = cursor.getString(cursor.getColumnIndexOrThrow(
				DatabaseHelper.FAVORITE_COLUMN_NEWS_SHARE_URL));
		item.favorite = cursor.getInt(cursor.getColumnIndexOrThrow(
				DatabaseHelper.FAVORITE_COLUMN_IS_FAVORITE)) == 1;
		item.readLater = cursor.getInt(cursor.getColumnIndexOrThrow(
				DatabaseHelper.FAVORITE_COLUMN_IS_READ_LATER)) == 1;
		item.note = cursor.getString(cursor.getColumnIndexOrThrow(
				DatabaseHelper.FAVORITE_COLUMN_NOTE));
		item.body = cursor.getString(cursor.getColumnIndexOrThrow(
				DatabaseHelper.FAVORITE_COLUMN_BODY));
		item.createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(
				DatabaseHelper.FAVORITE_COLUMN_CREATED_AT));
		item.updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow(
				DatabaseHelper.FAVORITE_COLUMN_UPDATED_AT));
		item.tags = new ArrayList<String>();
		return item;
	}

	/** Loads tags for a result set in one query instead of one query per row. */
	private void loadTags(ArrayList<LibraryItemEntity> items) {
		if (items == null || items.isEmpty()) {
			return;
		}
		Map<String, LibraryItemEntity> byNewsId = new HashMap<String, LibraryItemEntity>();
		for (LibraryItemEntity item : items) {
			byNewsId.put(item.newsId, item);
		}
		Cursor cursor = null;
		try {
			String sql = "SELECT " + DatabaseHelper.ITEM_TAG_TABLE_NAME + "."
					+ DatabaseHelper.ITEM_TAG_COLUMN_NEWS_ID + ", "
					+ DatabaseHelper.TAG_TABLE_NAME + "." + DatabaseHelper.TAG_COLUMN_NAME
					+ " FROM " + DatabaseHelper.ITEM_TAG_TABLE_NAME + " INNER JOIN "
					+ DatabaseHelper.TAG_TABLE_NAME + " ON " + DatabaseHelper.TAG_TABLE_NAME + "."
					+ DatabaseHelper.TAG_COLUMN_ID + " = " + DatabaseHelper.ITEM_TAG_TABLE_NAME + "."
					+ DatabaseHelper.ITEM_TAG_COLUMN_TAG_ID + " ORDER BY "
					+ DatabaseHelper.TAG_TABLE_NAME + "." + DatabaseHelper.TAG_COLUMN_NAME
					+ " COLLATE NOCASE";
			cursor = database.rawQuery(sql, null);
			while (cursor.moveToNext()) {
				LibraryItemEntity item = byNewsId.get(cursor.getString(0));
				if (item != null) {
					item.tags.add(cursor.getString(1));
				}
			}
		} finally {
			close(cursor);
		}
	}

	private boolean addTagMapping(String newsId, String tag) {
		ContentValues tagValues = new ContentValues();
		tagValues.put(DatabaseHelper.TAG_COLUMN_NAME, tag);
		tagValues.put(DatabaseHelper.TAG_COLUMN_CREATED_AT, now());
		database.insertWithOnConflict(DatabaseHelper.TAG_TABLE_NAME, null, tagValues,
				SQLiteDatabase.CONFLICT_IGNORE);

		Long tagId = findTagId(tag);
		if (tagId == null) {
			return false;
		}
		ContentValues mapping = new ContentValues();
		mapping.put(DatabaseHelper.ITEM_TAG_COLUMN_NEWS_ID, newsId);
		mapping.put(DatabaseHelper.ITEM_TAG_COLUMN_TAG_ID, tagId);
		return database.insertWithOnConflict(DatabaseHelper.ITEM_TAG_TABLE_NAME, null, mapping,
				SQLiteDatabase.CONFLICT_IGNORE) != -1 || hasTagMapping(newsId, tagId);
	}

	private boolean replaceTagMappings(String newsId, Collection<String> tags) {
		database.delete(DatabaseHelper.ITEM_TAG_TABLE_NAME,
				DatabaseHelper.ITEM_TAG_COLUMN_NEWS_ID + " = ?",
				new String[] { newsId });
		for (String tag : tags) {
			if (!addTagMapping(newsId, tag)) {
				return false;
			}
		}
		return true;
	}

	private Long findTagId(String tag) {
		Cursor cursor = null;
		try {
			cursor = database.query(DatabaseHelper.TAG_TABLE_NAME,
					new String[] { DatabaseHelper.TAG_COLUMN_ID },
					DatabaseHelper.TAG_COLUMN_NAME + " = ? COLLATE NOCASE",
					new String[] { tag }, null, null, null, "1");
			return cursor.moveToFirst() ? cursor.getLong(0) : null;
		} finally {
			close(cursor);
		}
	}

	private boolean hasTagMapping(String newsId, long tagId) {
		Cursor cursor = null;
		try {
			cursor = database.query(DatabaseHelper.ITEM_TAG_TABLE_NAME,
					new String[] { DatabaseHelper.ITEM_TAG_COLUMN_TAG_ID },
					DatabaseHelper.ITEM_TAG_COLUMN_NEWS_ID + " = ? AND "
							+ DatabaseHelper.ITEM_TAG_COLUMN_TAG_ID + " = ?",
					new String[] { newsId, String.valueOf(tagId) }, null, null, null, "1");
			return cursor.moveToFirst();
		} finally {
			close(cursor);
		}
	}

	private boolean itemExists(String newsId) {
		Cursor cursor = null;
		try {
			cursor = database.query(DatabaseHelper.FAVORITE_TABLE_NAME,
					new String[] { DatabaseHelper.FAVORITE_COLUMN_ID },
					DatabaseHelper.FAVORITE_COLUMN_NEWS_ID + " = ?",
					new String[] { newsId }, null, null, null, "1");
			return cursor.moveToFirst();
		} finally {
			close(cursor);
		}
	}

	private void touchItem(String newsId) {
		ContentValues values = new ContentValues();
		values.put(DatabaseHelper.FAVORITE_COLUMN_UPDATED_AT, now());
		database.update(DatabaseHelper.FAVORITE_TABLE_NAME, values,
				DatabaseHelper.FAVORITE_COLUMN_NEWS_ID + " = ?", new String[] { newsId });
	}

	private static String normalizeNewsId(String newsId) {
		if (newsId == null) {
			return null;
		}
		String normalized = newsId.trim();
		return normalized.length() == 0 ? null : normalized;
	}

	private static String normalizeTag(String tag) {
		if (tag == null) {
			return null;
		}
		String normalized = tag.trim();
		return normalized.length() == 0 ? null : normalized;
	}

	private static LinkedHashSet<String> normalizeTags(Collection<String> tags) {
		LinkedHashSet<String> normalizedTags = new LinkedHashSet<String>();
		if (tags != null) {
			for (String tag : tags) {
				String normalized = normalizeTag(tag);
				if (normalized != null) {
					normalizedTags.add(normalized);
				}
			}
		}
		return normalizedTags;
	}

	private static Long parseNewsId(String newsId) {
		try {
			return newsId == null ? null : Long.valueOf(newsId);
		} catch (NumberFormatException ignored) {
			return null;
		}
	}

	private static String escapeLike(String value) {
		return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

	private static void putNullable(ContentValues values, String key, String value) {
		if (value == null) {
			values.putNull(key);
		} else {
			values.put(key, value);
		}
	}

	private static void putIfSupplied(ContentValues values, String key, String value) {
		if (value != null) {
			values.put(key, value);
		}
	}

	private static long now() {
		return System.currentTimeMillis();
	}

	private static void close(Cursor cursor) {
		if (cursor != null) {
			cursor.close();
		}
	}
}
