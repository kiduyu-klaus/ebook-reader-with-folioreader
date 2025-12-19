package com.kiduyu.klaus.ebookfinaldownload.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;

import com.kiduyu.klaus.ebookfinaldownload.models.BookInfo;
import com.kiduyu.klaus.ebookfinaldownload.models.DownloadLink;
import com.kiduyu.klaus.ebookfinaldownload.repository.BookRepository;
import com.kiduyu.klaus.ebookfinaldownload.utils.DownloadUtils;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;

/**
 * Service for syncing new releases from the website to the local database
 * Runs in the background and updates the database periodically
 */
public class BookSyncService extends Service {

    private static final String TAG = "BookSyncService";
    private static final String NEW_RELEASES_URL = "https://oceanofpdf.com/new-releases/";
    private static final int DEFAULT_TIMEOUT = 15000;
    private static final int MAX_PARALLEL_PAGES = 5;

    private final IBinder binder = new LocalBinder();
    private BookRepository bookRepository;
    private DownloadUtils downloadUtils;
    private OkHttpClient client;
    private ExecutorService pageExecutor;
    private Handler mainHandler;
    private SyncCallback syncCallback;

    private boolean isSyncing = false;
    private AtomicInteger booksFound;

    public class LocalBinder extends Binder {
        BookSyncService getService() {
            return BookSyncService.this;
        }
    }

    public interface SyncCallback {
        void onSyncStarted();
        void onSyncProgress(String message);
        void onSyncCompleted(int booksCount);
        void onSyncError(String error);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "BookSyncService created");

        bookRepository = BookRepository.getInstance(this);
        downloadUtils = new DownloadUtils(this);
        mainHandler = new Handler(Looper.getMainLooper());
        booksFound = new AtomicInteger(0);

        initializeExecutors();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "BookSyncService started");

        // Start sync in background thread
        new Thread(() -> {
            try {
                syncNewReleases();
            } catch (Exception e) {
                Log.e(TAG, "Error syncing new releases", e);
                notifyError("Sync error: " + e.getMessage());
            }
        }).start();

        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    /**
     * Set the callback for sync events
     */
    public void setSyncCallback(SyncCallback callback) {
        this.syncCallback = callback;
    }

    /**
     * Start syncing new releases from the website
     */
    public void startSync() {
        if (isSyncing) {
            Log.w(TAG, "Sync already in progress");
            return;
        }

        new Thread(() -> {
            try {
                syncNewReleases();
            } catch (Exception e) {
                Log.e(TAG, "Error syncing new releases", e);
                notifyError("Sync error: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Sync new releases from the website to the database
     */
    private void syncNewReleases() throws Exception {
        isSyncing = true;
        booksFound.set(0);

        notifyProgress("Starting sync...");

        try {
            // Fetch first page to get all new releases
            String pageUrl = NEW_RELEASES_URL;
            notifyProgress("Fetching page 1...");

            Document doc = downloadUtils.fetchPage(pageUrl, client);
            if (doc == null) {
                notifyError("Failed to load page");
                return;
            }

            // Select all book elements from the new releases page
            Elements bookElements = doc.select("div.col-lg-2.col-sm-3.col-6");

            if (bookElements.isEmpty()) {
                notifyError("No books found on the page");
                return;
            }

            notifyProgress("Found " + bookElements.size() + " books on page 1");

            // Create a queue for book processing
            BlockingQueue<BookTask> bookQueue = new LinkedBlockingQueue<>();
            CountDownLatch latch = new CountDownLatch(bookElements.size());

            // Process each book element
            for (Element bookElement : bookElements) {
                pageExecutor.submit(() -> {
                    try {
                        extractBookInfo(bookElement, bookQueue);
                    } catch (Exception e) {
                        Log.e(TAG, "Error extracting book info", e);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // Process books as they come in
            List<BookInfo> booksToSave = new ArrayList<>();
            boolean processingComplete = false;

            while (!processingComplete || !bookQueue.isEmpty()) {
                BookTask task = bookQueue.poll(1, TimeUnit.SECONDS);

                if (task != null) {
                    BookInfo bookInfo = processBookInfo(task);
                    if (bookInfo != null) {
                        booksToSave.add(bookInfo);
                    }
                }

                // Check if all books have been processed
                processingComplete = latch.getCount() == 0;
            }

            // Save all books to database
            if (!booksToSave.isEmpty()) {
                notifyProgress("Saving " + booksToSave.size() + " books to database...");
                int savedCount = bookRepository.saveBooks(booksToSave);
                notifyProgress("Saved " + savedCount + " books to database");
            }

            notifyCompleted(booksFound.get());

        } finally {
            isSyncing = false;
        }
    }

    /**
     * Extract book info from HTML element
     */
    private void extractBookInfo(Element bookElement, BlockingQueue<BookTask> bookQueue) {
        try {
            Element widgetEvent = bookElement.selectFirst("div.widget-event");
            if (widgetEvent == null) return;

            Element titleLink = widgetEvent.selectFirst("a.title-image[href]");
            if (titleLink == null) return;

            String bookUrl = titleLink.attr("href");
            String bookTitle = titleLink.attr("title");

            Element imgTag = titleLink.selectFirst("img");
            String imgUrl = null;
            if (imgTag != null) {
                imgUrl = imgTag.attr("data-src");
                if (imgUrl == null || imgUrl.isEmpty()) {
                    imgUrl = imgTag.attr("src");
                }
            }

            Element infoDiv = widgetEvent.selectFirst("div.widget-event__info");
            if (infoDiv != null) {
                Element titleDiv = infoDiv.selectFirst("div.title a");
                if (titleDiv != null && (bookTitle == null || bookTitle.isEmpty())) {
                    bookTitle = titleDiv.text();
                }
            }

            if (bookUrl != null && !bookUrl.isEmpty()) {
                BookTask task = new BookTask(bookUrl, bookTitle, imgUrl);
                bookQueue.offer(task);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error extracting book element", e);
        }
    }

    /**
     * Process book info and fetch download links
     */
    private BookInfo processBookInfo(BookTask task) {
        try {
            BookInfo bookInfo = downloadUtils.getBookInfo(task.bookUrl, client);

            if (bookInfo == null) {
                Log.w(TAG, "Failed to get book info for: " + task.bookUrl);
                return null;
            }

            // Use the image URL from the new releases page if available
            if (task.imageUrl != null && !task.imageUrl.isEmpty()) {
                if (bookInfo.getBookimg() == null || bookInfo.getBookimg().isEmpty()) {
                    bookInfo.setBookimg(task.imageUrl);
                }
            }

            List<DownloadLink> downloadLinks = bookInfo.getDownloadLinks();

            if (downloadLinks != null && !downloadLinks.isEmpty()) {
                String result = downloadUtils.fetchAndDownload(downloadLinks, client, bookInfo, 3);
                if (result != null) {
                    Log.d(TAG, "Download link: " + result);
                    bookInfo.setDownlink(result);
                    downloadLinks.get(0).setDownlink(result);
                }
            }

            int count = booksFound.incrementAndGet();
            notifyProgress("Processed: " + bookInfo.getTitle() + " (" + count + ")");

            return bookInfo;

        } catch (Exception e) {
            Log.e(TAG, "Error processing book: " + task.bookUrl, e);
            return null;
        }
    }

    /**
     * Initialize executors
     */
    private void initializeExecutors() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)
                .readTimeout(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)
                .followRedirects(true)
                .connectionPool(new ConnectionPool(10, 5, TimeUnit.MINUTES))
                .build();

        this.pageExecutor = Executors.newFixedThreadPool(MAX_PARALLEL_PAGES);
    }

    /**
     * Notify sync started
     */
    private void notifyProgress(String message) {
        Log.d(TAG, message);
        if (syncCallback != null) {
            mainHandler.post(() -> syncCallback.onSyncProgress(message));
        }
    }

    /**
     * Notify sync completed
     */
    private void notifyCompleted(int booksCount) {
        Log.d(TAG, "Sync completed with " + booksCount + " books");
        if (syncCallback != null) {
            mainHandler.post(() -> syncCallback.onSyncCompleted(booksCount));
        }
    }

    /**
     * Notify sync error
     */
    private void notifyError(String error) {
        Log.e(TAG, error);
        if (syncCallback != null) {
            mainHandler.post(() -> syncCallback.onSyncError(error));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "BookSyncService destroyed");

        if (pageExecutor != null && !pageExecutor.isShutdown()) {
            pageExecutor.shutdown();
        }

        if (bookRepository != null) {
            bookRepository.shutdown();
        }
    }

    /**
     * Helper class for book tasks
     */
    private static class BookTask {
        String bookUrl;
        String bookTitle;
        String imageUrl;

        BookTask(String bookUrl, String bookTitle, String imageUrl) {
            this.bookUrl = bookUrl;
            this.bookTitle = bookTitle;
            this.imageUrl = imageUrl;
        }
    }
}
