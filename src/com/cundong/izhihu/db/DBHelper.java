package com.cundong.izhihu.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public final class DBHelper extends SQLiteOpenHelper {
    public static final String TABLE_NAME = "news_list";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_DATE = "date";
    public static final String COLUMN_CONTENT = "content";

    public static final String DATABASE_NAME = "zhihu_daily.db";
    public static final int DATABASE_VERSION = 2;

    private static final String DATABASE_CREATE
            = "CREATE TABLE " + TABLE_NAME
            + "(" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COLUMN_DATE + " CHAR(32) UNIQUE, "
            + COLUMN_CONTENT + " TEXT NOT NULL);";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }
}
