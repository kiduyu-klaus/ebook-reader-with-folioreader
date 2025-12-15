package com.kiduyu.klaus.ebookfinaldownload.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.kiduyu.klaus.ebookfinaldownload.R;
import com.kiduyu.klaus.ebookfinaldownload.adapters.BookAdapter;
import com.kiduyu.klaus.ebookfinaldownload.models.BookInfo;
import com.kiduyu.klaus.ebookfinaldownload.models.DownloadLink;
import com.kiduyu.klaus.ebookfinaldownload.utils.DownloadEpub;
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

public class NewReleasesFragment extends Fragment {

    private static final String TAG = "NewReleasesFragment";
    private static final String NEW_RELEASES_URL = "https://oceanofpdf.com/new-releases/";
    private static final int DEFAULT_TIMEOUT = 15000;
    private static final int MAX_PARALLEL_PAGES = 5;

    private OkHttpClient client;
    private ExecutorService pageExecutor;
    private Handler mainHandler;

    // UI Elements
    private TextView statusText;
    private TextView bookCountText;
    private ProgressBar progressBar;
    private RecyclerView booksRecyclerView;
    private LinearLayout emptyStateLayout;
    private SwipeRefreshLayout swipeRefreshLayout;
    private MaterialButton loadMoreButton;

    // Adapter
    private BookAdapter bookAdapter;
    private List<BookInfo> booksList;

    private AtomicInteger booksFound;
    private AtomicInteger currentPage;
    private DownloadEpub downloadEpub;
    private DownloadUtils downloadUtils;
    private boolean isLoading = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_new_releases, container, false);

        initializeViews(view);
        initializeExecutors();
        setupRecyclerView();
        setupListeners();

        this.mainHandler = new Handler(Looper.getMainLooper());
        this.booksFound = new AtomicInteger(0);
        this.currentPage = new AtomicInteger(1);

        if (getActivity() != null) {
            downloadEpub = new DownloadEpub(getActivity());
            downloadUtils = new DownloadUtils(getContext());
            downloadEpub.checkAndRequestPermissions();
        }

        // Auto-load new releases on fragment creation
        loadNewReleases(false);

        return view;
    }

    private void initializeViews(View view) {
        statusText = view.findViewById(R.id.statusText);
        bookCountText = view.findViewById(R.id.downloadCountText);
        progressBar = view.findViewById(R.id.progressBar);
        booksRecyclerView = view.findViewById(R.id.booksRecyclerView);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        loadMoreButton = view.findViewById(R.id.loadMoreButton);
    }

    private void initializeExecutors() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)
                .readTimeout(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)
                .followRedirects(true)
                .connectionPool(new ConnectionPool(10, 5, TimeUnit.MINUTES))
                .build();

        this.pageExecutor = Executors.newFixedThreadPool(MAX_PARALLEL_PAGES);
    }

    private void setupRecyclerView() {
        booksList = new ArrayList<>();
        bookAdapter = new BookAdapter(getContext(), booksList);
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 1);
        booksRecyclerView.setLayoutManager(layoutManager);
        booksRecyclerView.setAdapter(bookAdapter);
    }

    private void setupListeners() {
        // Swipe to refresh
        swipeRefreshLayout.setOnRefreshListener(() -> {
            currentPage.set(1);
            booksFound.set(0);
            bookAdapter.clearBooks();
            loadNewReleases(false);
        });

        // Load more button
        loadMoreButton.setOnClickListener(v -> {
            int nextPage = currentPage.incrementAndGet();
            loadNewReleases(true);
        });
    }

    private void loadNewReleases(boolean isLoadMore) {
        if (isLoading) return;

        isLoading = true;
        loadMoreButton.setEnabled(false);
        progressBar.setVisibility(ProgressBar.VISIBLE);

        if (!isLoadMore) {
            emptyStateLayout.setVisibility(View.GONE);
            booksRecyclerView.setVisibility(View.VISIBLE);
        }

        updateStatus("Loading new releases...");

        new Thread(() -> {
            try {
                fetchNewReleases(currentPage.get());
            } catch (Exception e) {
                updateStatus("Error: " + e.getMessage());
                Log.e(TAG, "Error loading new releases", e);
            } finally {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        isLoading = false;
                        loadMoreButton.setEnabled(true);
                        progressBar.setVisibility(ProgressBar.GONE);
                        swipeRefreshLayout.setRefreshing(false);

                        // Show empty state if no books found
                        if (booksList.isEmpty()) {
                            booksRecyclerView.setVisibility(View.GONE);
                            emptyStateLayout.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }
        }).start();
    }

    private void fetchNewReleases(int page) throws Exception {
        String pageUrl = page == 1 ? NEW_RELEASES_URL : NEW_RELEASES_URL + "page/" + page + "/";

        updateStatus("Fetching page " + page + "...");

        Document doc = downloadUtils.fetchPage(pageUrl, client);
        if (doc == null) {
            updateStatus("Failed to load page");
            return;
        }

        // Select all book elements from the new releases page
        Elements bookElements = doc.select("div.col-lg-2.col-sm-3.col-6");

        if (bookElements.isEmpty()) {
            updateStatus("No more books found");
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> loadMoreButton.setVisibility(View.GONE));
            }
            return;
        }

        updateStatus("Found " + bookElements.size() + " books on page " + page);

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
        boolean processingComplete = false;
        while (!processingComplete || !bookQueue.isEmpty()) {
            BookTask task = bookQueue.poll(1, TimeUnit.SECONDS);

            if (task != null) {
                processBookInfo(task);
            }

            // Check if all books have been processed
            processingComplete = latch.getCount() == 0;
        }

        updateStatus("✅ Loaded " + booksFound.get() + " books");

        // Show load more button
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> loadMoreButton.setVisibility(View.VISIBLE));
        }
    }

    private void extractBookInfo(Element bookElement, BlockingQueue<BookTask> bookQueue) {
        try {
            // Extract the book URL and title from the widget-event div
            Element widgetEvent = bookElement.selectFirst("div.widget-event");
            if (widgetEvent == null) return;

            Element titleLink = widgetEvent.selectFirst("a.title-image[href]");
            if (titleLink == null) return;

            String bookUrl = titleLink.attr("href");
            String bookTitle = titleLink.attr("title");

            // Extract image URL (prefer data-src for lazy loaded images)
            Element imgTag = titleLink.selectFirst("img");
            String imgUrl = null;
            if (imgTag != null) {
                imgUrl = imgTag.attr("data-src");
                if (imgUrl == null || imgUrl.isEmpty()) {
                    imgUrl = imgTag.attr("src");
                }
            }

            // Also try to get title from the widget-event__info div
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

    private void processBookInfo(BookTask task) {
        try {
            BookInfo bookInfo = downloadUtils.getBookInfo(task.bookUrl, client);

            if (bookInfo == null) {
                Log.w(TAG, "Failed to get book info for: " + task.bookUrl);
                return;
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

            // Add book to adapter on main thread
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    bookAdapter.addBook(bookInfo);
                    updateBookCount(count);
                    updateStatus("Loaded: " + bookInfo.getTitle());
                });
            }

        } catch (Exception e) {
            Log.e(TAG, "Error processing book: " + task.bookUrl, e);
            updateStatus("Error processing book: " + e.getMessage());
        }
    }

    private void updateStatus(String message) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> statusText.setText(message));
        }
    }

    private void updateBookCount(int count) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> bookCountText.setText("Books Loaded: " + count));
        }
    }

    // Helper class
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (pageExecutor != null && !pageExecutor.isShutdown()) {
            pageExecutor.shutdown();
        }
    }
}