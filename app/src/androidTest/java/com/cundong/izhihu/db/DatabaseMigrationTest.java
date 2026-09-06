package com.cundong.izhihu.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.cundong.izhihu.entity.LibraryItemEntity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.UUID;

@RunWith(AndroidJUnit4.class)
public class DatabaseMigrationTest {

	private Context context;
	private String databaseName;
	private DatabaseHelper helper;

	@Before
	public void setUp() {
		context = ApplicationProvider.getApplicationContext();
		databaseName = "zhihu-paper-migration-" + UUID.randomUUID() + ".db";
		context.deleteDatabase(databaseName);
	}

	@After
	public void tearDown() {
		if (helper != null) {
			helper.close();
		}
		context.deleteDatabase(databaseName);
	}

	@Test
	public void version7DatabaseUpgradesWithoutLosingCacheFavoriteOrReadState() {
		createVersion7Database();

		helper = DatabaseHelper.createForTesting(context, databaseName);
		SQLiteDatabase database = helper.getWritableDatabase();

		assertEquals(9, database.getVersion());
		assertCacheRowPreserved(database);
		assertFavoriteRowsMigrated(database);
		assertReadRowsMigrated(database);
		assertVersion9Schema(database);
		assertForeignKeysValid(database);
	}

	@Test
	public void clearingFavoritesPreservesNoteReadLaterTagsAndHighlights() {
		helper = DatabaseHelper.createForTesting(context, databaseName);
		NewsFavoriteDataSource library = new NewsFavoriteDataSource(helper);

		assertTrue(library.add2Favorite("123", "Saved title", "logo", "https://example.test/123"));
		assertTrue(library.saveNote("123", "My private note"));
		assertTrue(library.setReadLater("123", true));
		assertTrue(library.replaceTags("123", Arrays.asList("技术", "稍后精读")));
		long highlightId = library.addHighlight("123", "Important paragraph", "Why it matters", 4, 23);
		assertTrue(highlightId > 0);

		library.deleteFromFavorite();

		LibraryItemEntity item = library.getLibraryItem("123");
		assertNotNull(item);
		assertFalse(item.favorite);
		assertTrue(item.readLater);
		assertEquals("My private note", item.note);
		assertEquals(Arrays.asList("技术", "稍后精读"), item.tags);
		assertEquals(1, library.getHighlights("123").size());
		assertEquals("Important paragraph", library.getHighlights("123").get(0).quote);
	}

	@Test
	public void freshInstallCreatesCompleteVersion9Schema() {
		helper = DatabaseHelper.createForTesting(context, databaseName);
		SQLiteDatabase database = helper.getWritableDatabase();

		assertEquals(9, database.getVersion());
		assertVersion9Schema(database);
		assertForeignKeysValid(database);

		NewsFavoriteDataSource library = new NewsFavoriteDataSource(helper);
		assertNull(library.getLibraryItem("missing"));
		assertFalse(library.ensureItem("  ", "ignored", null, null));
		assertTrue(library.ensureItem("non-numeric-id", "Safe dirty ID", null, null));
		assertNotNull(library.getLibraryItem("non-numeric-id"));
		// The legacy favorite screen must skip non-numeric IDs rather than crashing.
		assertTrue(library.add2Favorite("non-numeric-id", "Safe dirty ID", null, null));
		assertTrue(library.getFavoriteList().isEmpty());
	}

	@Test
	public void atomicLibrarySaveCommitsAllDialogFieldsTogether() {
		helper = DatabaseHelper.createForTesting(context, databaseName);
		NewsFavoriteDataSource library = new NewsFavoriteDataSource(helper);

		assertTrue(library.saveLibraryItemAtomically("321", "Saved title",
				"https://pic.example.com/321.jpg", "https://daily.zhihu.com/story/321",
				true, "My note", Arrays.asList(" Android ", "离线", "Android"),
				"<p>Offline body</p>"));

		LibraryItemEntity item = library.getLibraryItem("321");
		assertNotNull(item);
		assertEquals("Saved title", item.title);
		assertEquals("https://pic.example.com/321.jpg", item.logo);
		assertEquals("https://daily.zhihu.com/story/321", item.shareUrl);
		assertTrue(item.readLater);
		assertEquals("My note", item.note);
		assertEquals("<p>Offline body</p>", item.body);
		assertEquals(Arrays.asList("Android", "离线"), item.tags);
	}

	@Test
	public void atomicLibrarySaveRollsBackEveryFieldWhenTagWriteFails() {
		helper = DatabaseHelper.createForTesting(context, databaseName);
		NewsFavoriteDataSource library = new NewsFavoriteDataSource(helper);
		assertTrue(library.saveLibraryItemAtomically("654", "Original title",
				"https://pic.example.com/original.jpg",
				"https://daily.zhihu.com/story/654", false, "Original note",
				Arrays.asList("original"), "<p>Original body</p>"));

		SQLiteDatabase database = helper.getWritableDatabase();
		database.execSQL("CREATE TRIGGER fail_atomic_library_tag BEFORE INSERT ON "
				+ DatabaseHelper.ITEM_TAG_TABLE_NAME + " BEGIN "
				+ "SELECT RAISE(ABORT, 'forced tag failure'); END");

		assertFalse(library.saveLibraryItemAtomically("654", "Changed title",
				"https://pic.example.com/changed.jpg",
				"https://daily.zhihu.com/story/changed", true, "Changed note",
				Arrays.asList("changed"), "<p>Changed body</p>"));

		LibraryItemEntity item = library.getLibraryItem("654");
		assertNotNull(item);
		assertEquals("Original title", item.title);
		assertEquals("https://pic.example.com/original.jpg", item.logo);
		assertEquals("https://daily.zhihu.com/story/654", item.shareUrl);
		assertFalse(item.readLater);
		assertEquals("Original note", item.note);
		assertEquals("<p>Original body</p>", item.body);
		assertEquals(Arrays.asList("original"), item.tags);
		assertEquals(1, rowCount(database, DatabaseHelper.TAG_TABLE_NAME));
		assertEquals(1, rowCount(database, DatabaseHelper.ITEM_TAG_TABLE_NAME));

		assertFalse(library.saveLibraryItemAtomically("999", "New row", null, null,
				true, "New note", Arrays.asList("new"), "<p>New body</p>"));
		assertNull(library.getLibraryItem("999"));
		assertEquals(1, rowCount(database, DatabaseHelper.TAG_TABLE_NAME));
		assertEquals(1, rowCount(database, DatabaseHelper.ITEM_TAG_TABLE_NAME));
	}

	@Test
	public void atomicLibrarySaveRollsBackWhenMetadataEnsureIsSilentlyIgnored() {
		helper = DatabaseHelper.createForTesting(context, databaseName);
		NewsFavoriteDataSource library = new NewsFavoriteDataSource(helper);
		assertTrue(library.saveLibraryItemAtomically("777", "Original title", null, null,
				false, "Original note", Arrays.asList("original"),
				"<p>Original body</p>"));

		SQLiteDatabase database = helper.getWritableDatabase();
		database.execSQL("CREATE TRIGGER ignore_atomic_library_metadata BEFORE UPDATE OF "
				+ DatabaseHelper.FAVORITE_COLUMN_NEWS_TITLE + " ON "
				+ DatabaseHelper.FAVORITE_TABLE_NAME + " WHEN OLD."
				+ DatabaseHelper.FAVORITE_COLUMN_NEWS_ID + " = '777' BEGIN "
				+ "SELECT RAISE(IGNORE); END");

		assertFalse(library.saveLibraryItemAtomically("777", "Changed title", null, null,
				true, "Changed note", Arrays.asList("changed"),
				"<p>Changed body</p>"));

		LibraryItemEntity item = library.getLibraryItem("777");
		assertNotNull(item);
		assertEquals("Original title", item.title);
		assertFalse(item.readLater);
		assertEquals("Original note", item.note);
		assertEquals("<p>Original body</p>", item.body);
		assertEquals(Arrays.asList("original"), item.tags);
	}

	private void createVersion7Database() {
		SQLiteDatabase database = context.openOrCreateDatabase(databaseName,
				Context.MODE_PRIVATE, null);
		try {
			database.execSQL("CREATE TABLE news_list(_id INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ "type INTEGER NOT NULL, key CHAR(256) UNIQUE NOT NULL, content TEXT NOT NULL)");
			database.execSQL("CREATE TABLE news_read(_id INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ "news_id CHAR(256) UNIQUE)");
			database.execSQL("CREATE TABLE news_favorite(_id INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ "news_id CHAR(256) UNIQUE, news_title CHAR(1024), news_logo CHAR(1024), "
					+ "news_share_url CHAR(1024))");
			database.execSQL("INSERT INTO news_list(type, key, content) VALUES(?, ?, ?)",
					new Object[] { 1, "latest", "cached-json" });
			database.execSQL("INSERT INTO news_favorite(news_id, news_title, news_logo, "
					+ "news_share_url) VALUES(?, ?, ?, ?)", new Object[] {
							"123", "Saved title", "logo", "https://example.test/123" });
			database.execSQL("INSERT INTO news_favorite(news_id, news_title) VALUES(?, ?)",
					new Object[] { "dirty-id", "Malformed legacy ID" });
			database.execSQL("INSERT INTO news_read(news_id) VALUES(?)", new Object[] { "123" });
			database.execSQL("INSERT INTO news_read(news_id) VALUES(?)",
					new Object[] { "dirty-id" });
			database.setVersion(7);
		} finally {
			database.close();
		}
	}

	private void assertCacheRowPreserved(SQLiteDatabase database) {
		Cursor cursor = database.query(DatabaseHelper.NEWS_TABLE_NAME,
				new String[] { DatabaseHelper.NEWS_COLUMN_TYPE, DatabaseHelper.NEWS_COLUMN_CONTENT },
				DatabaseHelper.NEWS_COLUMN_KEY + " = ?", new String[] { "latest" },
				null, null, null);
		try {
			assertTrue(cursor.moveToFirst());
			assertEquals(1, cursor.getInt(0));
			assertEquals("cached-json", cursor.getString(1));
		} finally {
			cursor.close();
		}
	}

	private void assertFavoriteRowsMigrated(SQLiteDatabase database) {
		Cursor cursor = database.query(DatabaseHelper.FAVORITE_TABLE_NAME,
				new String[] {
						DatabaseHelper.FAVORITE_COLUMN_NEWS_TITLE,
						DatabaseHelper.FAVORITE_COLUMN_IS_FAVORITE,
						DatabaseHelper.FAVORITE_COLUMN_IS_READ_LATER,
						DatabaseHelper.FAVORITE_COLUMN_NOTE,
						DatabaseHelper.FAVORITE_COLUMN_CREATED_AT,
						DatabaseHelper.FAVORITE_COLUMN_UPDATED_AT
				}, DatabaseHelper.FAVORITE_COLUMN_NEWS_ID + " = ?", new String[] { "123" },
				null, null, null);
		try {
			assertTrue(cursor.moveToFirst());
			assertEquals("Saved title", cursor.getString(0));
			assertEquals(1, cursor.getInt(1));
			assertEquals(0, cursor.getInt(2));
			assertNull(cursor.getString(3));
			assertTrue(cursor.getLong(4) > 0);
			assertTrue(cursor.getLong(5) > 0);
		} finally {
			cursor.close();
		}
		assertEquals(2, rowCount(database, DatabaseHelper.FAVORITE_TABLE_NAME));
	}

	private void assertReadRowsMigrated(SQLiteDatabase database) {
		Cursor cursor = database.query(DatabaseHelper.READ_TABLE_NAME,
				new String[] {
						DatabaseHelper.READ_COLUMN_READ_AT,
						DatabaseHelper.READ_COLUMN_LAST_READ_AT,
						DatabaseHelper.READ_COLUMN_PROGRESS
				}, DatabaseHelper.READ_COLUMN_NEWSID + " = ?", new String[] { "dirty-id" },
				null, null, null);
		try {
			assertTrue(cursor.moveToFirst());
			assertTrue(cursor.getLong(0) > 0);
			assertTrue(cursor.getLong(1) > 0);
			assertEquals(0f, cursor.getFloat(2), 0f);
		} finally {
			cursor.close();
		}
		assertEquals(2, rowCount(database, DatabaseHelper.READ_TABLE_NAME));
	}

	private void assertVersion9Schema(SQLiteDatabase database) {
		assertTableHasColumns(database, DatabaseHelper.NEWS_TABLE_NAME,
				DatabaseHelper.NEWS_COLUMN_KEY, DatabaseHelper.NEWS_COLUMN_CONTENT);
		assertTableHasColumns(database, DatabaseHelper.READ_TABLE_NAME,
				DatabaseHelper.READ_COLUMN_NEWSID, DatabaseHelper.READ_COLUMN_READ_AT,
				DatabaseHelper.READ_COLUMN_LAST_READ_AT, DatabaseHelper.READ_COLUMN_PROGRESS);
		assertTableHasColumns(database, DatabaseHelper.FAVORITE_TABLE_NAME,
				DatabaseHelper.FAVORITE_COLUMN_NEWS_ID,
				DatabaseHelper.FAVORITE_COLUMN_IS_FAVORITE,
				DatabaseHelper.FAVORITE_COLUMN_IS_READ_LATER,
				DatabaseHelper.FAVORITE_COLUMN_NOTE,
				DatabaseHelper.FAVORITE_COLUMN_BODY,
				DatabaseHelper.FAVORITE_COLUMN_CREATED_AT,
				DatabaseHelper.FAVORITE_COLUMN_UPDATED_AT);
		assertTableHasColumns(database, DatabaseHelper.TAG_TABLE_NAME,
				DatabaseHelper.TAG_COLUMN_NAME, DatabaseHelper.TAG_COLUMN_CREATED_AT);
		assertTableHasColumns(database, DatabaseHelper.ITEM_TAG_TABLE_NAME,
				DatabaseHelper.ITEM_TAG_COLUMN_NEWS_ID, DatabaseHelper.ITEM_TAG_COLUMN_TAG_ID);
		assertTableHasColumns(database, DatabaseHelper.HIGHLIGHT_TABLE_NAME,
				DatabaseHelper.HIGHLIGHT_COLUMN_NEWS_ID, DatabaseHelper.HIGHLIGHT_COLUMN_QUOTE,
				DatabaseHelper.HIGHLIGHT_COLUMN_NOTE,
				DatabaseHelper.HIGHLIGHT_COLUMN_START_OFFSET,
				DatabaseHelper.HIGHLIGHT_COLUMN_END_OFFSET);
	}

	private void assertTableHasColumns(SQLiteDatabase database, String table, String... columns) {
		assertTrue("missing table " + table, tableExists(database, table));
		Cursor cursor = database.rawQuery("PRAGMA table_info(" + table + ")", null);
		try {
			for (String column : columns) {
				boolean found = false;
				cursor.moveToPosition(-1);
				while (cursor.moveToNext()) {
					if (column.equals(cursor.getString(cursor.getColumnIndexOrThrow("name")))) {
						found = true;
						break;
					}
				}
				assertTrue("missing column " + table + "." + column, found);
			}
		} finally {
			cursor.close();
		}
	}

	private boolean tableExists(SQLiteDatabase database, String table) {
		Cursor cursor = database.query("sqlite_master", new String[] { "name" },
				"type = ? AND name = ?", new String[] { "table", table },
				null, null, null);
		try {
			return cursor.moveToFirst();
		} finally {
			cursor.close();
		}
	}

	private int rowCount(SQLiteDatabase database, String table) {
		Cursor cursor = database.rawQuery("SELECT COUNT(*) FROM " + table, null);
		try {
			assertTrue(cursor.moveToFirst());
			return cursor.getInt(0);
		} finally {
			cursor.close();
		}
	}

	private void assertForeignKeysValid(SQLiteDatabase database) {
		Cursor enabled = database.rawQuery("PRAGMA foreign_keys", null);
		try {
			assertTrue(enabled.moveToFirst());
			assertEquals(1, enabled.getInt(0));
		} finally {
			enabled.close();
		}

		Cursor violations = database.rawQuery("PRAGMA foreign_key_check", null);
		try {
			assertEquals(0, violations.getCount());
		} finally {
			violations.close();
		}
	}
}
