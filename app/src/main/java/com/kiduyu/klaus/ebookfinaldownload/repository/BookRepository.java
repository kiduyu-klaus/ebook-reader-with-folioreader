package com.kiduyu.klaus.ebookfinaldownload.repository;

import android.content.Context;
import android.util.Log;

import com.kiduyu.klaus.ebookfinaldownload.database.AppDatabase;
import com.kiduyu.klaus.ebookfinaldownload.database.BookDao;
import com.kiduyu.klaus.ebookfinaldownload.models.BookInfo;
import com.kiduyu.klaus.ebookfinaldownload.models.BookItem;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Repository pattern for abstracting data access
 * Provides a single point of access for local database and remote API
 */
public class BookRepository {

    private static final String TAG = "BookRepository";
    private static BookRepository instance;

    private final BookDao bookDao;
    private final ExecutorService executorService;

    private BookRepository(Context context) {
        AppDatabase dbHelper = AppDatabase.getInstance(context);
        this.bookDao = new BookDao(dbHelper);
        this.executorService = Executors.newFixedThreadPool(2);
    }

    public static synchronized BookRepository getInstance(Context context) {
        if (instance == null) {
            instance = new BookRepository(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Get all books from local database
     */
    public List<BookInfo> getAllBooksFromDatabase() {
        return bookDao.getAllBooks();
    }

    /**
     * Get books from database with pagination
     */
    public List<BookInfo> getBooksFromDatabase(int limit, int offset) {
        return bookDao.getBooks(limit, offset);
    }

    /**
     * Get a specific book by URL from database
     */
    public BookInfo getBookFromDatabase(String bookUrl) {
        return bookDao.getBookByUrl(bookUrl);
    }

    /**
     * Get total count of books in database
     */
    public int getBookCountFromDatabase() {
        return bookDao.getBookCount();
    }

    /**
     * Check if a book with the given title exists in the database
     */
    public boolean isBookTitleExists(String title) {
        return bookDao.isBookTitleExists(title);
    }

    /**
     * Insert or update a single book in database
     */
    public long saveBook(BookInfo book) {
        return bookDao.insertOrUpdateBook(book);
    }

    /**
     * Save multiple books in database (batch operation)
     */
    public int saveBooks(List<BookInfo> books) {
        return bookDao.insertBooks(books);
    }

    /**
     * Save books asynchronously
     */
    public Future<Integer> saveBooksAsync(List<BookInfo> books) {
        return executorService.submit(() -> {
            Log.d(TAG, "Saving " + books.size() + " books asynchronously");
            return bookDao.insertBooks(books);
        });
    }

    /**
     * Update download link for a book
     */
    public int updateDownloadLink(String bookUrl, String downloadLink) {
        return bookDao.updateDownloadLink(bookUrl, downloadLink);
    }

    /**
     * Delete all books from database
     */
    public int deleteAllBooks() {
        return bookDao.deleteAllBooks();
    }

    /**
     * Delete a specific book from database
     */
    public int deleteBook(String bookUrl) {
        return bookDao.deleteBookByUrl(bookUrl);
    }

    /**
     * Clear all books and refresh from remote (for full sync)
     */
    public void clearAndRefresh() {
        Log.d(TAG, "Clearing all books from database");
        deleteAllBooks();
    }

    // ==================== My Books Methods ====================

    /**
     * Get all books from the my_books table
     */
    public List<BookItem> getAllMyBooks() {
        return bookDao.getAllMyBooks();
    }

    /**
     * Insert or update a book in the my_books table
     */
    public long saveMyBook(BookItem book) {
        return bookDao.insertOrUpdateMyBook(book);
    }

    /**
     * Delete a book by file path from the my_books table
     */
    public int deleteMyBook(String filePath) {
        return bookDao.deleteMyBook(filePath);
    }

    /**
     * Execute a task asynchronously
     */
    public <T> Future<T> executeAsync(Callable<T> task) {
        return executorService.submit(task);
    }

    /**
     * Shutdown the executor service
     */
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
