package com.cundong.izhihu.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Owns the application's local database schema.
 *
 * <p>The favorite table intentionally remains named {@code news_favorite}. It is the compatibility
 * anchor for older installations, but from schema v8 onwards it stores every user-owned library
 * item (favorite, read-later, note or cached article body). Removing a favorite therefore only
 * clears a state bit and never destroys the user's other data.</p>
 */
public final class DatabaseHelper extends SQLiteOpenHelper {

	public static final String DB_NAME = "news_paper.db";
	public static final int DB_VERSION = 9;

	// 1. News list and news detail cache.
	public static final String NEWS_TABLE_NAME = "news_list";
	public static final String NEWS_COLUMN_ID = "_id";
	public static final String NEWS_COLUMN_TYPE = "type";
	public static final String NEWS_COLUMN_KEY = "key";
	public static final String NEWS_COLUMN_CONTENT = "content";

	private static final String NEWS_TABLE_CREATE = "CREATE TABLE IF NOT EXISTS "
			+ NEWS_TABLE_NAME + "(" + NEWS_COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
			+ NEWS_COLUMN_TYPE + " INTEGER NOT NULL, "
			+ NEWS_COLUMN_KEY + " TEXT UNIQUE NOT NULL, "
			+ NEWS_COLUMN_CONTENT + " TEXT NOT NULL)";

	// 2. Read state and reading position.
	public static final String READ_TABLE_NAME = "news_read";
	public static final String READ_COLUMN_ID = "_id";
	public static final String READ_COLUMN_NEWSID = "news_id";
	public static final String READ_COLUMN_READ_AT = "read_at";
	public static final String READ_COLUMN_LAST_READ_AT = "last_read_at";
	public static final String READ_COLUMN_PROGRESS = "progress";

	private static final String READ_TABLE_CREATE = "CREATE TABLE IF NOT EXISTS "
			+ READ_TABLE_NAME + "(" + READ_COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
			+ READ_COLUMN_NEWSID + " TEXT UNIQUE NOT NULL, "
			+ READ_COLUMN_READ_AT + " INTEGER NOT NULL DEFAULT 0, "
			+ READ_COLUMN_LAST_READ_AT + " INTEGER NOT NULL DEFAULT 0, "
			+ READ_COLUMN_PROGRESS + " REAL NOT NULL DEFAULT 0 CHECK("
			+ READ_COLUMN_PROGRESS + " >= 0 AND " + READ_COLUMN_PROGRESS + " <= 1))";

	// 3. Library item. The table name is retained for source and migration compatibility.
	public static final String FAVORITE_TABLE_NAME = "news_favorite";
	public static final String FAVORITE_COLUMN_ID = "_id";
	public static final String FAVORITE_COLUMN_NEWS_ID = "news_id";
	public static final String FAVORITE_COLUMN_NEWS_TITLE = "news_title";
	public static final String FAVORITE_COLUMN_NEWS_LOGO = "news_logo";
	public static final String FAVORITE_COLUMN_NEWS_SHARE_URL = "news_share_url";
	public static final String FAVORITE_COLUMN_IS_FAVORITE = "is_favorite";
	public static final String FAVORITE_COLUMN_IS_READ_LATER = "is_read_later";
	public static final String FAVORITE_COLUMN_NOTE = "note";
	public static final String FAVORITE_COLUMN_BODY = "article_body";
	public static final String FAVORITE_COLUMN_CREATED_AT = "created_at";
	public static final String FAVORITE_COLUMN_UPDATED_AT = "updated_at";

	private static final String FAVORITE_TABLE_CREATE = "CREATE TABLE IF NOT EXISTS "
			+ FAVORITE_TABLE_NAME + "(" + FAVORITE_COLUMN_ID
			+ " INTEGER PRIMARY KEY AUTOINCREMENT, "
			+ FAVORITE_COLUMN_NEWS_ID + " TEXT UNIQUE NOT NULL, "
			+ FAVORITE_COLUMN_NEWS_TITLE + " TEXT, "
			+ FAVORITE_COLUMN_NEWS_LOGO + " TEXT, "
			+ FAVORITE_COLUMN_NEWS_SHARE_URL + " TEXT, "
			+ FAVORITE_COLUMN_IS_FAVORITE + " INTEGER NOT NULL DEFAULT 0, "
			+ FAVORITE_COLUMN_IS_READ_LATER + " INTEGER NOT NULL DEFAULT 0, "
			+ FAVORITE_COLUMN_NOTE + " TEXT, "
			+ FAVORITE_COLUMN_BODY + " TEXT, "
			+ FAVORITE_COLUMN_CREATED_AT + " INTEGER NOT NULL DEFAULT 0, "
			+ FAVORITE_COLUMN_UPDATED_AT + " INTEGER NOT NULL DEFAULT 0)";

	// 4. Tags and item/tag relationships.
	public static final String TAG_TABLE_NAME = "tag";
	public static final String TAG_COLUMN_ID = "_id";
	public static final String TAG_COLUMN_NAME = "name";
	public static final String TAG_COLUMN_CREATED_AT = "created_at";

	private static final String TAG_TABLE_CREATE = "CREATE TABLE IF NOT EXISTS "
			+ TAG_TABLE_NAME + "(" + TAG_COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
			+ TAG_COLUMN_NAME + " TEXT COLLATE NOCASE UNIQUE NOT NULL, "
			+ TAG_COLUMN_CREATED_AT + " INTEGER NOT NULL DEFAULT 0)";

	public static final String ITEM_TAG_TABLE_NAME = "item_tag";
	public static final String ITEM_TAG_COLUMN_NEWS_ID = "news_id";
	public static final String ITEM_TAG_COLUMN_TAG_ID = "tag_id";

	private static final String ITEM_TAG_TABLE_CREATE = "CREATE TABLE IF NOT EXISTS "
			+ ITEM_TAG_TABLE_NAME + "(" + ITEM_TAG_COLUMN_NEWS_ID + " TEXT NOT NULL, "
			+ ITEM_TAG_COLUMN_TAG_ID + " INTEGER NOT NULL, PRIMARY KEY("
			+ ITEM_TAG_COLUMN_NEWS_ID + ", " + ITEM_TAG_COLUMN_TAG_ID + "), "
			+ "FOREIGN KEY(" + ITEM_TAG_COLUMN_NEWS_ID + ") REFERENCES "
			+ FAVORITE_TABLE_NAME + "(" + FAVORITE_COLUMN_NEWS_ID + ") ON DELETE CASCADE, "
			+ "FOREIGN KEY(" + ITEM_TAG_COLUMN_TAG_ID + ") REFERENCES "
			+ TAG_TABLE_NAME + "(" + TAG_COLUMN_ID + ") ON DELETE CASCADE)";

	// 5. Article highlights.
	public static final String HIGHLIGHT_TABLE_NAME = "highlight";
	public static final String HIGHLIGHT_COLUMN_ID = "_id";
	public static final String HIGHLIGHT_COLUMN_NEWS_ID = "news_id";
	public static final String HIGHLIGHT_COLUMN_QUOTE = "quote";
	public static final String HIGHLIGHT_COLUMN_NOTE = "note";
	public static final String HIGHLIGHT_COLUMN_START_OFFSET = "start_offset";
	public static final String HIGHLIGHT_COLUMN_END_OFFSET = "end_offset";
	public static final String HIGHLIGHT_COLUMN_CREATED_AT = "created_at";
	public static final String HIGHLIGHT_COLUMN_UPDATED_AT = "updated_at";

	private static final String HIGHLIGHT_TABLE_CREATE = "CREATE TABLE IF NOT EXISTS "
			+ HIGHLIGHT_TABLE_NAME + "(" + HIGHLIGHT_COLUMN_ID
			+ " INTEGER PRIMARY KEY AUTOINCREMENT, "
			+ HIGHLIGHT_COLUMN_NEWS_ID + " TEXT NOT NULL, "
			+ HIGHLIGHT_COLUMN_QUOTE + " TEXT NOT NULL, "
			+ HIGHLIGHT_COLUMN_NOTE + " TEXT, "
			+ HIGHLIGHT_COLUMN_START_OFFSET + " INTEGER NOT NULL DEFAULT -1, "
			+ HIGHLIGHT_COLUMN_END_OFFSET + " INTEGER NOT NULL DEFAULT -1, "
			+ HIGHLIGHT_COLUMN_CREATED_AT + " INTEGER NOT NULL DEFAULT 0, "
			+ HIGHLIGHT_COLUMN_UPDATED_AT + " INTEGER NOT NULL DEFAULT 0, "
			+ "FOREIGN KEY(" + HIGHLIGHT_COLUMN_NEWS_ID + ") REFERENCES "
			+ FAVORITE_TABLE_NAME + "(" + FAVORITE_COLUMN_NEWS_ID + ") ON DELETE CASCADE)";

	private static volatile DatabaseHelper mDBHelper;

	private DatabaseHelper(Context context) {
		this(context, DB_NAME);
	}

	private DatabaseHelper(Context context, String databaseName) {
		super(storageContext(context), databaseName, null, DB_VERSION);
	}

	public static DatabaseHelper getInstance(Context context) {
		if (mDBHelper == null) {
			synchronized (DatabaseHelper.class) {
				if (mDBHelper == null) {
					mDBHelper = new DatabaseHelper(context);
				}
			}
		}
		return mDBHelper;
	}

	/**
	 * Creates an isolated, non-singleton helper for instrumentation migration tests.
	 * The caller owns deleting the named test database after closing the helper.
	 */
	public static DatabaseHelper createForTesting(Context context, String databaseName) {
		if (context == null) {
			throw new IllegalArgumentException("context == null");
		}
		if (databaseName == null || databaseName.trim().length() == 0) {
			throw new IllegalArgumentException("databaseName is empty");
		}
		return new DatabaseHelper(context, databaseName);
	}

	@Override
	public void onConfigure(SQLiteDatabase db) {
		super.onConfigure(db);
		db.setForeignKeyConstraintsEnabled(true);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		createBaseTables(db);
		createLibraryTables(db);
		createIndexes(db);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// SQLiteOpenHelper wraps this callback in a transaction. Every migration below is additive;
		// user-owned rows are never dropped or replaced.
		if (oldVersion < 8) {
			migrateToVersion8(db);
		}
		if (oldVersion < 9) {
			migrateToVersion9(db);
		}
	}

	private static void createBaseTables(SQLiteDatabase db) {
		db.execSQL(NEWS_TABLE_CREATE);
		db.execSQL(READ_TABLE_CREATE);
		db.execSQL(FAVORITE_TABLE_CREATE);
	}

	private static void createLibraryTables(SQLiteDatabase db) {
		db.execSQL(TAG_TABLE_CREATE);
		db.execSQL(ITEM_TAG_TABLE_CREATE);
		db.execSQL(HIGHLIGHT_TABLE_CREATE);
	}

	private static void createIndexes(SQLiteDatabase db) {
		db.execSQL("CREATE INDEX IF NOT EXISTS idx_library_state ON " + FAVORITE_TABLE_NAME
				+ "(" + FAVORITE_COLUMN_IS_FAVORITE + ", "
				+ FAVORITE_COLUMN_IS_READ_LATER + ", " + FAVORITE_COLUMN_UPDATED_AT + ")");
		db.execSQL("CREATE INDEX IF NOT EXISTS idx_library_title ON " + FAVORITE_TABLE_NAME
				+ "(" + FAVORITE_COLUMN_NEWS_TITLE + ")");
		db.execSQL("CREATE INDEX IF NOT EXISTS idx_read_last_read_at ON " + READ_TABLE_NAME
				+ "(" + READ_COLUMN_LAST_READ_AT + ")");
		db.execSQL("CREATE INDEX IF NOT EXISTS idx_item_tag_tag ON " + ITEM_TAG_TABLE_NAME
				+ "(" + ITEM_TAG_COLUMN_TAG_ID + ")");
		db.execSQL("CREATE INDEX IF NOT EXISTS idx_highlight_news ON " + HIGHLIGHT_TABLE_NAME
				+ "(" + HIGHLIGHT_COLUMN_NEWS_ID + ", " + HIGHLIGHT_COLUMN_CREATED_AT + ")");
	}

	private static void migrateToVersion8(SQLiteDatabase db) {
		// Older versions are still handled defensively: create a missing base table and then add only
		// absent columns. Existing v7 favorite rows receive is_favorite=1 by the ALTER default.
		db.execSQL(NEWS_TABLE_CREATE);
		if (!tableExists(db, READ_TABLE_NAME)) {
			db.execSQL(READ_TABLE_CREATE);
		} else {
			addColumnIfMissing(db, READ_TABLE_NAME, READ_COLUMN_READ_AT,
					READ_COLUMN_READ_AT + " INTEGER NOT NULL DEFAULT 0");
			addColumnIfMissing(db, READ_TABLE_NAME, READ_COLUMN_LAST_READ_AT,
					READ_COLUMN_LAST_READ_AT + " INTEGER NOT NULL DEFAULT 0");
			addColumnIfMissing(db, READ_TABLE_NAME, READ_COLUMN_PROGRESS,
					READ_COLUMN_PROGRESS + " REAL NOT NULL DEFAULT 0");
		}

		if (!tableExists(db, FAVORITE_TABLE_NAME)) {
			db.execSQL(FAVORITE_TABLE_CREATE);
		} else {
			addColumnIfMissing(db, FAVORITE_TABLE_NAME, FAVORITE_COLUMN_IS_FAVORITE,
					FAVORITE_COLUMN_IS_FAVORITE + " INTEGER NOT NULL DEFAULT 1");
			addColumnIfMissing(db, FAVORITE_TABLE_NAME, FAVORITE_COLUMN_IS_READ_LATER,
					FAVORITE_COLUMN_IS_READ_LATER + " INTEGER NOT NULL DEFAULT 0");
			addColumnIfMissing(db, FAVORITE_TABLE_NAME, FAVORITE_COLUMN_NOTE,
					FAVORITE_COLUMN_NOTE + " TEXT");
			addColumnIfMissing(db, FAVORITE_TABLE_NAME, FAVORITE_COLUMN_BODY,
					FAVORITE_COLUMN_BODY + " TEXT");
			addColumnIfMissing(db, FAVORITE_TABLE_NAME, FAVORITE_COLUMN_CREATED_AT,
					FAVORITE_COLUMN_CREATED_AT + " INTEGER NOT NULL DEFAULT 0");
			addColumnIfMissing(db, FAVORITE_TABLE_NAME, FAVORITE_COLUMN_UPDATED_AT,
					FAVORITE_COLUMN_UPDATED_AT + " INTEGER NOT NULL DEFAULT 0");
		}

		long migrationTime = System.currentTimeMillis();
		db.execSQL("UPDATE " + FAVORITE_TABLE_NAME + " SET "
				+ FAVORITE_COLUMN_CREATED_AT + " = ?, " + FAVORITE_COLUMN_UPDATED_AT + " = ? WHERE "
				+ FAVORITE_COLUMN_CREATED_AT + " = 0 OR " + FAVORITE_COLUMN_UPDATED_AT + " = 0",
				new Object[] { migrationTime, migrationTime });
		db.execSQL("UPDATE " + READ_TABLE_NAME + " SET " + READ_COLUMN_READ_AT + " = ?, "
				+ READ_COLUMN_LAST_READ_AT + " = ? WHERE " + READ_COLUMN_READ_AT + " = 0 OR "
				+ READ_COLUMN_LAST_READ_AT + " = 0", new Object[] { migrationTime, migrationTime });
	}

	private static void migrateToVersion9(SQLiteDatabase db) {
		// A v8 database already has the expanded library columns. The new relational tables are empty
		// on first creation, so enabling foreign keys cannot invalidate pre-existing user data.
		createLibraryTables(db);
		createIndexes(db);
	}

	private static void addColumnIfMissing(SQLiteDatabase db, String table, String column,
			String definition) {
		if (!hasColumn(db, table, column)) {
			db.execSQL("ALTER TABLE " + table + " ADD COLUMN " + definition);
		}
	}

	private static boolean tableExists(SQLiteDatabase db, String table) {
		Cursor cursor = null;
		try {
			cursor = db.query("sqlite_master", new String[] { "name" }, "type=? AND name=?",
					new String[] { "table", table }, null, null, null);
			return cursor.moveToFirst();
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	private static boolean hasColumn(SQLiteDatabase db, String table, String column) {
		Cursor cursor = null;
		try {
			cursor = db.rawQuery("PRAGMA table_info(" + table + ")", null);
			int nameIndex = cursor.getColumnIndexOrThrow("name");
			while (cursor.moveToNext()) {
				if (column.equals(cursor.getString(nameIndex))) {
					return true;
				}
			}
			return false;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	private static Context storageContext(Context context) {
		if (context == null) {
			throw new IllegalArgumentException("context == null");
		}
		Context applicationContext = context.getApplicationContext();
		return applicationContext != null ? applicationContext : context;
	}
}
