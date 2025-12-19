package com.kiduyu.klaus.ebookfinaldownload.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.kiduyu.klaus.ebookfinaldownload.models.BookInfo;
import com.kiduyu.klaus.ebookfinaldownload.models.BookItem;

import java.util.ArrayList;
import java.util.List;

public class BookDao {

    private static final String TAG = "BookDao";
    private final AppDatabase dbHelper;

    public BookDao(AppDatabase dbHelper) {
        this.dbHelper = dbHelper;
    }

    /**
     * Insert or update a book in the new_releases table
     */
    public long insertOrUpdateBook(BookInfo book) {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();

            values.put(AppDatabase.COLUMN_BOOK_URL, book.getBookUrl());
            values.put(AppDatabase.COLUMN_TITLE, book.getTitle());
            values.put(AppDatabase.COLUMN_AUTHOR, book.getAuthor());
            values.put(AppDatabase.COLUMN_LANGUAGE, book.getLanguage());
            values.put(AppDatabase.COLUMN_BOOK_IMG, book.getBookimg());
            values.put(AppDatabase.COLUMN_EPUB_SIZE, book.getEpubSize());
            values.put(AppDatabase.COLUMN_PDF_SIZE, book.getPdfSize());
            values.put(AppDatabase.COLUMN_DOWNLINK, book.getDownlink());
            values.put(AppDatabase.COLUMN_UPDATED_AT, System.currentTimeMillis());

            // Try to insert, if it fails (due to unique constraint), update instead
            long result = db.insertWithOnConflict(
                    AppDatabase.TABLE_NEW_RELEASES,
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_REPLACE
            );

            Log.d(TAG, "Book inserted/updated: " + book.getTitle() + " (ID: " + result + ")");
            return result;

        } catch (Exception e) {
            Log.e(TAG, "Error inserting/updating book", e);
            return -1;
        }
    }

    /**
     * Insert multiple books in a batch
     */
    public int insertBooks(List<BookInfo> books) {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.beginTransaction();

            int count = 0;
            for (BookInfo book : books) {
                ContentValues values = new ContentValues();

                values.put(AppDatabase.COLUMN_BOOK_URL, book.getBookUrl());
                values.put(AppDatabase.COLUMN_TITLE, book.getTitle());
                values.put(AppDatabase.COLUMN_AUTHOR, book.getAuthor());
                values.put(AppDatabase.COLUMN_LANGUAGE, book.getLanguage());
                values.put(AppDatabase.COLUMN_BOOK_IMG, book.getBookimg());
                values.put(AppDatabase.COLUMN_EPUB_SIZE, book.getEpubSize());
                values.put(AppDatabase.COLUMN_PDF_SIZE, book.getPdfSize());
                values.put(AppDatabase.COLUMN_DOWNLINK, book.getDownlink());

                long result = db.insertWithOnConflict(
                        AppDatabase.TABLE_NEW_RELEASES,
                        null,
                        values,
                        SQLiteDatabase.CONFLICT_REPLACE
                );

                if (result != -1) {
                    count++;
                }
            }

            db.setTransactionSuccessful();
            db.endTransaction();

            Log.d(TAG, "Inserted " + count + " books in batch");
            return count;

        } catch (Exception e) {
            Log.e(TAG, "Error inserting books in batch", e);
            return 0;
        }
    }

    /**
     * Get all books from the new_releases table
     */
    public List<BookInfo> getAllBooks() {
        List<BookInfo> books = new ArrayList<>();

        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            String query = "SELECT * FROM " + AppDatabase.TABLE_NEW_RELEASES +
                    " ORDER BY " + AppDatabase.COLUMN_CREATED_AT + " DESC";

            Cursor cursor = db.rawQuery(query, null);

            if (cursor.moveToFirst()) {
                do {
                    BookInfo book = cursorToBook(cursor);
                    if (book != null) {
                        books.add(book);
                    }
                } while (cursor.moveToNext());
            }

            cursor.close();
            Log.d(TAG, "Retrieved " + books.size() + " books from database");

        } catch (Exception e) {
            Log.e(TAG, "Error retrieving all books", e);
        }

        return books;
    }

    /**
     * Get books with pagination
     */
    public List<BookInfo> getBooks(int limit, int offset) {
        List<BookInfo> books = new ArrayList<>();

        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            String query = "SELECT * FROM " + AppDatabase.TABLE_NEW_RELEASES +
                    " ORDER BY " + AppDatabase.COLUMN_CREATED_AT + " DESC" +
                    " LIMIT " + limit + " OFFSET " + offset;

            Cursor cursor = db.rawQuery(query, null);

            if (cursor.moveToFirst()) {
                do {
                    BookInfo book = cursorToBook(cursor);
                    if (book != null) {
                        books.add(book);
                    }
                } while (cursor.moveToNext());
            }

            cursor.close();
            Log.d(TAG, "Retrieved " + books.size() + " books (limit: " + limit + ", offset: " + offset + ")");

        } catch (Exception e) {
            Log.e(TAG, "Error retrieving books with pagination", e);
        }

        return books;
    }

    /**
     * Get a book by URL
     */
    public BookInfo getBookByUrl(String bookUrl) {
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            String query = "SELECT * FROM " + AppDatabase.TABLE_NEW_RELEASES +
                    " WHERE " + AppDatabase.COLUMN_BOOK_URL + " = ?";

            Cursor cursor = db.rawQuery(query, new String[]{bookUrl});

            if (cursor.moveToFirst()) {
                BookInfo book = cursorToBook(cursor);
                cursor.close();
                return book;
            }

            cursor.close();

        } catch (Exception e) {
            Log.e(TAG, "Error retrieving book by URL", e);
        }

        return null;
    }

    /**
     * Get the count of books in the database
     */
    public int getBookCount() {
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            String query = "SELECT COUNT(*) FROM " + AppDatabase.TABLE_NEW_RELEASES;

            Cursor cursor = db.rawQuery(query, null);

            if (cursor.moveToFirst()) {
                int count = cursor.getInt(0);
                cursor.close();
                Log.d(TAG, "Total books in database: " + count);
                return count;
            }

            cursor.close();

        } catch (Exception e) {
            Log.e(TAG, "Error getting book count", e);
        }

        return 0;
    }

    /**
     * Delete all books from the new_releases table
     */
    public int deleteAllBooks() {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            int deletedRows = db.delete(AppDatabase.TABLE_NEW_RELEASES, null, null);
            Log.d(TAG, "Deleted " + deletedRows + " books from database");
            return deletedRows;

        } catch (Exception e) {
            Log.e(TAG, "Error deleting all books", e);
            return 0;
        }
    }

    /**
     * Delete a book by URL
     */
    public int deleteBookByUrl(String bookUrl) {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            int deletedRows = db.delete(
                    AppDatabase.TABLE_NEW_RELEASES,
                    AppDatabase.COLUMN_BOOK_URL + " = ?",
                    new String[]{bookUrl}
            );
            Log.d(TAG, "Deleted book: " + bookUrl);
            return deletedRows;

        } catch (Exception e) {
            Log.e(TAG, "Error deleting book by URL", e);
            return 0;
        }
    }

    /**
     * Update a book's download link
     */
    public int updateDownloadLink(String bookUrl, String downloadLink) {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(AppDatabase.COLUMN_DOWNLINK, downloadLink);
            values.put(AppDatabase.COLUMN_UPDATED_AT, System.currentTimeMillis());

            int updatedRows = db.update(
                    AppDatabase.TABLE_NEW_RELEASES,
                    values,
                    AppDatabase.COLUMN_BOOK_URL + " = ?",
                    new String[]{bookUrl}
            );

            Log.d(TAG, "Updated download link for: " + bookUrl);
            return updatedRows;

        } catch (Exception e) {
            Log.e(TAG, "Error updating download link", e);
            return 0;
        }
    }

    /**
     * Convert cursor to BookInfo object
     */
    /**
     * Convert cursor to BookItem object
     */
    private BookItem cursorToMyBook(Cursor cursor) {
        try {
            BookItem book = new BookItem();

            book.setFilePath(cursor.getString(cursor.getColumnIndexOrThrow(AppDatabase.COLUMN_FILE_PATH)));
            book.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(AppDatabase.COLUMN_TITLE)));
            book.setSize(cursor.getString(cursor.getColumnIndexOrThrow(AppDatabase.COLUMN_SIZE)));
            book.setDate(cursor.getString(cursor.getColumnIndexOrThrow(AppDatabase.COLUMN_DATE)));
            book.setCoverImagePath(cursor.getString(cursor.getColumnIndexOrThrow(AppDatabase.COLUMN_COVER_PATH)));

            return book;

        } catch (Exception e) {
            Log.e(TAG, "Error converting cursor to my book", e);
            return null;
        }
    }

    /**
     * Insert or update a book in the my_books table
     */
    public long insertOrUpdateMyBook(BookItem book) {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();

            values.put(AppDatabase.COLUMN_FILE_PATH, book.getFilePath());
            values.put(AppDatabase.COLUMN_TITLE, book.getTitle());
            values.put(AppDatabase.COLUMN_SIZE, book.getSize());
            values.put(AppDatabase.COLUMN_DATE, book.getDate());
            values.put(AppDatabase.COLUMN_COVER_PATH, book.getCoverImagePath());

            // Use CONFLICT_REPLACE since file_path is the primary key
            long result = db.insertWithOnConflict(
                    AppDatabase.TABLE_MY_BOOKS,
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_REPLACE
            );

            Log.d(TAG, "My Book inserted/updated: " + book.getTitle() + " (ID: " + result + ")");
            return result;

        } catch (Exception e) {
            Log.e(TAG, "Error inserting/updating my book", e);
            return -1;
        }
    }

    /**
     * Get all books from the my_books table
     */
    public List<BookItem> getAllMyBooks() {
        List<BookItem> books = new ArrayList<>();

        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            String query = "SELECT * FROM " + AppDatabase.TABLE_MY_BOOKS +
                    " ORDER BY " + AppDatabase.COLUMN_DATE + " DESC"; // Order by date (last modified/added)

            Cursor cursor = db.rawQuery(query, null);

            if (cursor.moveToFirst()) {
                do {
                    BookItem book = cursorToMyBook(cursor);
                    if (book != null) {
                        books.add(book);
                    }
                } while (cursor.moveToNext());
            }

            cursor.close();
            Log.d(TAG, "Retrieved " + books.size() + " books from my_books table");

        } catch (Exception e) {
            Log.e(TAG, "Error retrieving all my books", e);
        }

        return books;
    }

    /**
     * Delete a book by file path from the my_books table
     */
    public int deleteMyBook(String filePath) {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            int deletedRows = db.delete(
                    AppDatabase.TABLE_MY_BOOKS,
                    AppDatabase.COLUMN_FILE_PATH + " = ?",
                    new String[]{filePath}
            );
            Log.d(TAG, "Deleted my book: " + filePath);
            return deletedRows;

        } catch (Exception e) {
            Log.e(TAG, "Error deleting my book by file path", e);
            return 0;
        }
    }

    /**
     * Convert cursor to BookInfo object
     */
    private BookInfo cursorToBook(Cursor cursor) {
        try {
            BookInfo book = new BookInfo();

            book.setBookUrl(cursor.getString(cursor.getColumnIndexOrThrow(AppDatabase.COLUMN_BOOK_URL)));
            book.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(AppDatabase.COLUMN_TITLE)));
            book.setAuthor(cursor.getString(cursor.getColumnIndexOrThrow(AppDatabase.COLUMN_AUTHOR)));
            book.setLanguage(cursor.getString(cursor.getColumnIndexOrThrow(AppDatabase.COLUMN_LANGUAGE)));
            book.setBookimg(cursor.getString(cursor.getColumnIndexOrThrow(AppDatabase.COLUMN_BOOK_IMG)));
            book.setEpubSize(cursor.getString(cursor.getColumnIndexOrThrow(AppDatabase.COLUMN_EPUB_SIZE)));
            book.setPdfSize(cursor.getString(cursor.getColumnIndexOrThrow(AppDatabase.COLUMN_PDF_SIZE)));
            book.setDownlink(cursor.getString(cursor.getColumnIndexOrThrow(AppDatabase.COLUMN_DOWNLINK)));

            return book;

        } catch (Exception e) {
            Log.e(TAG, "Error converting cursor to book", e);
            return null;
        }
    }
}
