package com.kiduyu.klaus.ebookfinaldownload.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class AppDatabase extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "ebook_reader.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_NEW_RELEASES = "new_releases";
    public static final String TABLE_BOOKS = "books";

    // New Releases Table Columns
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_BOOK_URL = "book_url";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_AUTHOR = "author";
    public static final String COLUMN_LANGUAGE = "language";
    public static final String COLUMN_BOOK_IMG = "book_img";
    public static final String COLUMN_EPUB_SIZE = "epub_size";
    public static final String COLUMN_PDF_SIZE = "pdf_size";
    public static final String COLUMN_DOWNLINK = "downlink";
    public static final String COLUMN_CREATED_AT = "created_at";
    public static final String COLUMN_UPDATED_AT = "updated_at";

    private static AppDatabase instance;

    public AppDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = new AppDatabase(context.getApplicationContext());
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create New Releases table
        String CREATE_NEW_RELEASES_TABLE = "CREATE TABLE " + TABLE_NEW_RELEASES + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_BOOK_URL + " TEXT UNIQUE NOT NULL, " +
                COLUMN_TITLE + " TEXT NOT NULL, " +
                COLUMN_AUTHOR + " TEXT, " +
                COLUMN_LANGUAGE + " TEXT, " +
                COLUMN_BOOK_IMG + " TEXT, " +
                COLUMN_EPUB_SIZE + " TEXT, " +
                COLUMN_PDF_SIZE + " TEXT, " +
                COLUMN_DOWNLINK + " TEXT, " +
                COLUMN_CREATED_AT + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                COLUMN_UPDATED_AT + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";

        db.execSQL(CREATE_NEW_RELEASES_TABLE);

        // Create index on book_url for faster lookups
        db.execSQL("CREATE INDEX idx_book_url ON " + TABLE_NEW_RELEASES + "(" + COLUMN_BOOK_URL + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // For future database schema upgrades
        if (oldVersion < newVersion) {
            // Handle migration here
        }
    }

    public void close() {
        if (instance != null) {
            super.close();
            instance = null;
        }
    }
}
