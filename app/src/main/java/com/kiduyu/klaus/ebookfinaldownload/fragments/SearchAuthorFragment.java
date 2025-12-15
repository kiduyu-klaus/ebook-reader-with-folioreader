package com.kiduyu.klaus.ebookfinaldownload.fragments;

import android.content.Context;
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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
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

public class SearchAuthorFragment extends Fragment {

    private static final String TAG = "SearchAuthorFragment";
    private static final String BASE_URL = "https://oceanofpdf.com/category/authors/";
    private static final int DEFAULT_TIMEOUT = 15000;
    private static final int MAX_PARALLEL_PAGES = 5;

    private OkHttpClient client;
    private ExecutorService pageExecutor;
    private Handler mainHandler;

    // UI Elements
    private EditText authorNameInput;
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
    private DownloadUtils downloadUtils;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search_author, container, false);

        initializeViews(view);
        initializeExecutors();
        setupRecyclerView();
        setupListeners();

        this.mainHandler = new Handler(Looper.getMainLooper());
        this.booksFound = new AtomicInteger(0);

        if (getActivity() != null) {
            downloadEpub = new DownloadEpub(getActivity());
            downloadUtils = new DownloadUtils(getContext());
            downloadEpub.checkAndRequestPermissions();
        }

        return view;
    }

    private void initializeViews(View view) {
        authorNameInput = view.findViewById(R.id.authorNameInput);
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
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 1);
        booksRecyclerView.setLayoutManager(layoutManager);
        booksRecyclerView.setAdapter(bookAdapter);
    }

    private void setupListeners() {
        searchButton.setOnClickListener(v -> {
            hideKeyboard();
            booksFound.set(0);

            String authorName = authorNameInput.getText().toString().trim();
            String numBooksStr = numBooksInput.getText().toString().trim();

            // Validate author name has at least 2 words
            if (authorName.isEmpty()) {
                Toast.makeText(getContext(), "Please enter an author name", Toast.LENGTH_SHORT).show();
                return;
            }

            String[] words = authorName.split("\\s+");
            if (words.length < 2) {
                Toast.makeText(getContext(), "Please enter at least 2 words (first and last name)", Toast.LENGTH_LONG).show();
                return;
            }

            Integer numBooks = null;
            if (!numBooksStr.isEmpty()) {
                try {
                    numBooks = Integer.parseInt(numBooksStr);
                    if (numBooks <= 0) {
                        Toast.makeText(getContext(), "Number of books must be greater than 0", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(getContext(), "Invalid number of books", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            startAuthorSearch(authorName, numBooks);
        });
    }

    private void startAuthorSearch(String authorName, Integer maxBooks) {
        searchButton.setEnabled(false);
        progressBar.setVisibility(ProgressBar.VISIBLE);
        booksFound.set(0);

        // Clear previous results
        bookAdapter.clearBooks();
        emptyStateLayout.setVisibility(View.GONE);
        booksRecyclerView.setVisibility(View.VISIBLE);

        // Convert author name to URL format (replace spaces with -)
        String authorSlug = authorName.toLowerCase().trim().replaceAll("\\s+", "-");
        String authorUrl = BASE_URL + authorSlug + "/";

        updateStatus("Searching for books by: " + authorName);

        new Thread(() -> {
            try {
                searchAuthorBooks(authorUrl, authorName, maxBooks);
            } catch (Exception e) {
                updateStatus("Error: " + e.getMessage());
                Log.e(TAG, "Search error", e);
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

    private void searchAuthorBooks(String authorUrl, String authorName, Integer maxBooks) throws Exception {
        updateStatus("Fetching author page...");

        // Get the last page number
        int lastPage = downloadUtils.getLastPage(authorUrl, client);
        updateStatus("Detected " + lastPage + " pages for " + authorName);

        // Create a queue for book processing
        BlockingQueue<BookTask> bookQueue = new LinkedBlockingQueue<>();
        CountDownLatch latch = new CountDownLatch(lastPage);

        // Submit page crawling tasks in parallel
        for (int page = 1; page <= lastPage; page++) {
            final int currentPage = page;

            pageExecutor.submit(() -> {
                try {
                    crawlAuthorPage(currentPage, authorUrl, bookQueue, maxBooks);
                } catch (Exception e) {
                    updateStatus("Error on page " + currentPage + ": " + e.getMessage());
                    Log.e(TAG, "Error crawling page " + currentPage, e);
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
                if (maxBooks != null && booksFound.get() >= maxBooks) {
                    break;
                }
            }

            // Check if all pages have been crawled
            searchComplete = latch.getCount() == 0;
        }

        updateStatus("✅ Search complete! Found " + booksFound.get() + " books by " + authorName);
    }

    private void crawlAuthorPage(int page, String baseUrl, BlockingQueue<BookTask> bookQueue, Integer maxBooks) throws Exception {
        String pageUrl = page == 1 ? baseUrl : baseUrl + "page/" + page + "/";

        updateStatus("Crawling page " + page + "...");
        Document doc = downloadUtils.fetchPage(pageUrl, client);
        if (doc == null) {
            Log.w(TAG, "Failed to fetch page: " + pageUrl);
            return;
        }

        Elements articles = doc.select("article");

        if (articles.isEmpty()) {
            Log.w(TAG, "No articles found on page: " + pageUrl);
            return;
        }

        for (Element article : articles) {
            // Check if we've reached the limit
            if (maxBooks != null && booksFound.get() >= maxBooks) {
                break;
            }

            Element header = article.selectFirst("header.entry-header");
            if (header == null) continue;

            Element aTag = header.selectFirst("a.entry-title-link[href]");
            if (aTag == null) continue;

            Element postmetainfo = article.selectFirst("div.postmetainfo");
            if (postmetainfo == null) continue;

            // Language filtering
            if (!downloadUtils.isEnglish(postmetainfo)) {
                continue;
            }

            String bookUrl = aTag.attr("href");
            String bookTitle = aTag.text();
            BookTask task = new BookTask(bookUrl, bookTitle, page);
            bookQueue.offer(task);
        }

        Thread.sleep(2000); // Rate limiting
    }

    private void processBookInfo(BookTask task) {
        try {
            BookInfo bookInfo = downloadUtils.getBookInfo(task.bookUrl, client);

            if (bookInfo == null) {
                Log.w(TAG, "Failed to get book info for: " + task.bookUrl);
                return;
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
                    updateStatus("Found: " + bookInfo.getTitle());
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