package com.kiduyu.klaus.ebookfinaldownload.fragments;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;

public class SearchFragment extends Fragment {

    private static final String TAG = "SearchFragment";
    private static final String BASE_URL = "https://oceanofpdf.com/page/1/?s=";
    private static final int DEFAULT_TIMEOUT = 15000;
    private static final int MAX_PARALLEL_PAGES = 5;

    private OkHttpClient client;
    private ExecutorService pageExecutor;
    private Handler mainHandler;

    // UI Elements
    private EditText searchInput;
    private EditText numBooksInput;
    private MaterialButton searchButton;
    private TextView statusText;
    private TextView bookCountText;
    private ProgressBar progressBar;
    private RecyclerView booksRecyclerView;
    private LinearLayout emptyStateLayout;

    // Adapter
    private BookAdapter bookAdapter;
    private List<BookInfo> booksList;

    private AtomicInteger booksFound;
    private DownloadEpub downloadEpub;
    private DownloadUtils downloadutils;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        initializeViews(view);
        initializeExecutors();
        setupRecyclerView();
        setupListeners();

        this.mainHandler = new Handler(Looper.getMainLooper());
        this.booksFound = new AtomicInteger(0);

        if (getActivity() != null) {
            downloadEpub = new DownloadEpub(getActivity());
            downloadutils = new DownloadUtils(getContext());
            downloadEpub.checkAndRequestPermissions();
        }

        return view;
    }

    private void initializeViews(View view) {
        searchInput = view.findViewById(R.id.searchInput);
        numBooksInput = view.findViewById(R.id.numBooksInput);
        searchButton = view.findViewById(R.id.searchButton);
        statusText = view.findViewById(R.id.statusText);
        bookCountText = view.findViewById(R.id.downloadCountText);
        progressBar = view.findViewById(R.id.progressBar);
        booksRecyclerView = view.findViewById(R.id.booksRecyclerView);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
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
        booksRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        booksRecyclerView.setAdapter(bookAdapter);
    }

    private void setupListeners() {
        searchButton.setOnClickListener(v -> {
            // Hide keyboard
            hideKeyboard();
            booksFound.set(0);

            String query = searchInput.getText().toString().trim();
            String numBooksStr = numBooksInput.getText().toString().trim();

            if (query.isEmpty()) {
                Toast.makeText(getContext(), "Please enter a search query", Toast.LENGTH_SHORT).show();
                return;
            }

            Integer numBooks = null;
            if (!numBooksStr.isEmpty()) {
                try {
                    numBooks = Integer.parseInt(numBooksStr);
                } catch (NumberFormatException e) {
                    Toast.makeText(getContext(), "Invalid number of books", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startSearch(query, numBooks);
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startSearch(String searchQuery, Integer firstNBooks) {
        searchButton.setEnabled(false);
        progressBar.setVisibility(ProgressBar.VISIBLE);
        booksFound.set(0);

        // Clear previous results
        bookAdapter.clearBooks();
        emptyStateLayout.setVisibility(View.GONE);
        booksRecyclerView.setVisibility(View.VISIBLE);

        updateStatus("Starting search for: " + searchQuery);

        new Thread(() -> {
            try {
                searchAndPrintBooksParallel(searchQuery, null, null, null, false, firstNBooks);
            } catch (Exception e) {
                updateStatus("Error: " + e.getMessage());
                e.printStackTrace();
            } finally {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        searchButton.setEnabled(true);
                        progressBar.setVisibility(ProgressBar.GONE);

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

    public void searchAndPrintBooksParallel(String searchQuery, Integer startPage,
                                            Integer stopPage, Integer maxPages,
                                            boolean firstOnly, Integer firstNBooks) throws Exception {

        String encodedQuery = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            encodedQuery = URLEncoder.encode(searchQuery, StandardCharsets.UTF_8);
        }
        String fullUrl = BASE_URL + encodedQuery;

        int lastPage = downloadutils.getLastPage(fullUrl, client);
        updateStatus("Detected " + lastPage + " pages");

        int start = (startPage != null) ? startPage : 1;
        int stop = (stopPage != null) ? stopPage : lastPage;
        if (maxPages != null) {
            stop = Math.min(start + maxPages - 1, stop);
        }

        updateStatus("Searching pages " + start + " to " + stop);

        // Create a queue for book processing
        BlockingQueue<BookTask> bookQueue = new LinkedBlockingQueue<>();
        CountDownLatch latch = new CountDownLatch(stop - start + 1);

        // Submit page crawling tasks in parallel
        for (int page = start; page <= stop; page++) {
            final int currentPage = page;
            final String currentQuery = encodedQuery;

            pageExecutor.submit(() -> {
                try {
                    crawlPage(currentPage, currentQuery, bookQueue, firstOnly, firstNBooks);
                } catch (Exception e) {
                    updateStatus("Error on page " + currentPage + ": " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        // Process books as they come in
        boolean searchComplete = false;
        while (!searchComplete || !bookQueue.isEmpty()) {
            BookTask task = bookQueue.poll(1, TimeUnit.SECONDS);

            if (task != null) {
                processBookInfo(task);

                // Check if we've reached the limit
                if (firstNBooks != null && booksFound.get() >= firstNBooks) {
                    break;
                }
            }

            // Check if all pages have been crawled
            searchComplete = latch.getCount() == 0;
        }

        updateStatus("✅ Search complete! Found " + booksFound.get() + " books");
    }

    private void processBookInfo(BookTask task) {
        try {
            BookInfo bookInfo = downloadutils.getBookInfo(task.bookUrl, client);
            List<DownloadLink> downloadLink = bookInfo.getDownloadLinks();

            String result = downloadutils.fetchAndDownload(downloadLink, client, bookInfo, 3);
            if (result != null) {
                Log.e(TAG, "downloadLink: " + downloadLink);
                Log.e(TAG, "processBookInfo: " + result);
                bookInfo.setDownlink(result);
                downloadLink.get(0).setDownlink(result);
            }

            if (bookInfo != null) {
                int count = booksFound.incrementAndGet();

                // Add book to adapter on main thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        bookAdapter.addBook(bookInfo);
                        updateBookCount(count);
                        updateStatus("Found: " + bookInfo.getTitle());
                    });
                }
            }
        } catch (Exception e) {
            updateStatus("Error processing book: " + e.getMessage());
        }
    }

    private void crawlPage(int page, String encodedQuery, BlockingQueue<BookTask> bookQueue,
                           boolean firstOnly, Integer firstNBooks) throws Exception {
        String pageUrl = "https://oceanofpdf.com/page/" + page + "/?s=" + encodedQuery;

        Document doc = downloadutils.fetchPage(pageUrl, client);
        if (doc == null) return;

        Elements articles = doc.select("article");

        if (firstOnly && !articles.isEmpty()) {
            articles = new Elements(articles.get(0));
        } else if (firstNBooks != null) {
            int remaining = firstNBooks - booksFound.get();
            if (remaining <= 0) return;
            articles = new Elements(articles.subList(0, Math.min(remaining, articles.size())));
        }

        for (Element article : articles) {
            if (firstNBooks != null && booksFound.get() >= firstNBooks) {
                break;
            }

            Element header = article.selectFirst("header.entry-header");
            if (header == null) continue;

            Element aTag = header.selectFirst("a.entry-title-link[href]");
            if (aTag == null) continue;

            Element postmetainfo = article.selectFirst("div.postmetainfo");
            if (postmetainfo == null) continue;

            // Language filtering
            if (!downloadutils.isEnglish(postmetainfo)) {
                continue;
            }

            String bookUrl = aTag.attr("href");
            String bookTitle = aTag.text();
            BookTask task = new BookTask(bookUrl, bookTitle, page);
            bookQueue.offer(task);
        }

        Thread.sleep(2000); // Rate limiting
    }

    private void updateStatus(String message) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> statusText.setText(message));
        }
    }

    private void updateBookCount(int count) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> bookCountText.setText("Books Found: " + count));
        }
    }

    private void hideKeyboard() {
        if (getActivity() != null && getView() != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
            }
        }
    }

    // Helper class
    private static class BookTask {
        String bookUrl;
        String bookTitle;
        int pageNumber;

        BookTask(String bookUrl, String bookTitle, int pageNumber) {
            this.bookUrl = bookUrl;
            this.bookTitle = bookTitle;
            this.pageNumber = pageNumber;
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