package com.kiduyu.klaus.ebookfinaldownload.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.kiduyu.klaus.ebookfinaldownload.R;
import com.kiduyu.klaus.ebookfinaldownload.adapters.BookAdapter;
import com.kiduyu.klaus.ebookfinaldownload.models.BookInfo;
import com.kiduyu.klaus.ebookfinaldownload.models.DownloadLink;
import com.kiduyu.klaus.ebookfinaldownload.models.Genre;
import com.kiduyu.klaus.ebookfinaldownload.utils.DownloadUtils;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;

public class GenreBooksFragment extends Fragment {

    private static final String ARG_GENRE = "genre";
    private static final String TAG = "GenreBooksFragment";
    private static final int DEFAULT_TIMEOUT = 10000; // Reduced timeout
    private static final int MAX_PAGES = 20;

    private Genre genre;
    private OkHttpClient client;
    private Handler mainHandler;
    private DownloadUtils downloadUtils;
    private ExecutorService executorService;

    // UI Elements
    private RecyclerView booksRecyclerView;
    private ProgressBar progressBar;
    private TextView statusText;
    private LinearLayout emptyStateLayout;
    private LinearLayout paginationLayout;
    private MaterialButton previousButton;
    private MaterialButton nextButton;
    private TextView pageInfoText;


    private DownloadUtils downloadutils;

    // Adapter
    private BookAdapter bookAdapter;
    private List<BookInfo> booksList;

    // Pagination state
    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean hasMorePages = true;

    // Cache for loaded pages
    private Map<Integer, List<BookInfo>> pageCache = new HashMap<>();
    private EditText searchInput;
    private TextView h1Text;
    // Prefetch state
    private Future<?> prefetchFuture;

    public GenreBooksFragment() {
        // Required empty public constructor
    }

    public static GenreBooksFragment newInstance(Genre genre) {
        GenreBooksFragment fragment = new GenreBooksFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_GENRE, genre);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            genre = (Genre) getArguments().getSerializable(ARG_GENRE);
        }
        executorService = Executors.newFixedThreadPool(3); // For parallel fetching
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_genre_books, container, false);

        initializeViews(view);
        initializeClient();
        setupRecyclerView();
        setupPaginationButtons();
        downloadutils = new DownloadUtils(getContext());

        this.mainHandler = new Handler(Looper.getMainLooper());
        this.downloadUtils = new DownloadUtils(getContext());

        if (genre != null) {
            loadBooksForPage(currentPage);
        } else {
            showError("Genre information not available");
        }

        return view;
    }

    private void initializeViews(View view) {
        booksRecyclerView = view.findViewById(R.id.genreBooksRecyclerView);
        progressBar = view.findViewById(R.id.genreBookProgressBar);
        statusText = view.findViewById(R.id.genreBookStatusText);
        emptyStateLayout = view.findViewById(R.id.genreBookEmptyStateLayout);
        paginationLayout = view.findViewById(R.id.paginationLayout);
        previousButton = view.findViewById(R.id.previousButton);
        nextButton = view.findViewById(R.id.nextButton);
        pageInfoText = view.findViewById(R.id.pageInfoText);
        h1Text = view.findViewById(R.id.genreSearch_genre);
        searchInput = view.findViewById(R.id.genreSearchInput_genre);
        searchInput.setText("genre > "+genre.getName());
        h1Text.setText(genre.getName());
    }

    private void initializeClient() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)
                .readTimeout(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)
                .followRedirects(true)
                .connectionPool(new ConnectionPool(20, 5, TimeUnit.MINUTES)) // Increased pool
                .build();
    }

    private void setupRecyclerView() {
        booksList = new ArrayList<>();
        bookAdapter = new BookAdapter(getContext(), booksList);
        booksRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        booksRecyclerView.setAdapter(bookAdapter);
    }

    private void setupPaginationButtons() {
        previousButton.setOnClickListener(v -> {
            if (currentPage > 1 && !isLoading) {
                currentPage--;
                loadBooksForPage(currentPage);
            }
        });

        nextButton.setOnClickListener(v -> {
            if (hasMorePages && currentPage < MAX_PAGES && !isLoading) {
                currentPage++;
                loadBooksForPage(currentPage);
            }
        });
    }

    private void loadBooksForPage(int page) {
        if (isLoading) return;

        // Check cache first
        if (pageCache.containsKey(page)) {
            Log.d(TAG, "Loading page " + page + " from cache");
            displayCachedPage(page);
            prefetchAdjacentPages(page);
            return;
        }

        isLoading = true;
        progressBar.setVisibility(View.VISIBLE);
        statusText.setText("Loading page " + page + "...");
        booksRecyclerView.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.GONE);

        updatePaginationControls();

        executorService.execute(() -> {
            try {
                List<BookInfo> books = fetchBooksFromGenre(genre.getUrl(), page);

                if (books.isEmpty()) {
                    if (page == 1) {
                        updateStatus("No English books found in this genre");
                        showEmptyState();
                    } else {
                        hasMorePages = false;
                        currentPage--;
                        updateStatus("No more books available");
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                progressBar.setVisibility(View.GONE);
                                updatePaginationControls();
                            });
                        }
                    }
                    isLoading = false;
                    return;
                }

                // Cache the page
                pageCache.put(page, books);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        booksList.clear();
                        booksList.addAll(books);
                        bookAdapter.notifyDataSetChanged();

                        booksRecyclerView.scrollToPosition(0);

                        progressBar.setVisibility(View.GONE);
                        booksRecyclerView.setVisibility(View.VISIBLE);
                        paginationLayout.setVisibility(View.VISIBLE);

                        statusText.setText(books.size() + " books on page " + page);
                        isLoading = false;

                        updatePaginationControls();

                        // Prefetch adjacent pages
                        prefetchAdjacentPages(page);

                        Log.d(TAG, "Loaded " + books.size() + " books for page " + page);
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "Error loading page " + page + ": " + e.getMessage(), e);
                updateStatus("Error: " + e.getMessage());
                if (page == 1) {
                    showEmptyState();
                } else {
                    currentPage--;
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            updatePaginationControls();
                        });
                    }
                }
                isLoading = false;
            }
        });
    }

    private void displayCachedPage(int page) {
        List<BookInfo> cachedBooks = pageCache.get(page);
        if (cachedBooks != null) {
            booksList.clear();
            booksList.addAll(cachedBooks);
            bookAdapter.notifyDataSetChanged();

            booksRecyclerView.scrollToPosition(0);
            booksRecyclerView.setVisibility(View.VISIBLE);
            paginationLayout.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);

            statusText.setText(cachedBooks.size() + " books on page " + page);
            updatePaginationControls();
        }
    }

    private void prefetchAdjacentPages(int currentPage) {
        // Cancel any existing prefetch
        if (prefetchFuture != null && !prefetchFuture.isDone()) {
            prefetchFuture.cancel(true);
        }

        prefetchFuture = executorService.submit(() -> {
            // Prefetch next page
            int nextPage = currentPage + 1;
            if (nextPage <= MAX_PAGES && !pageCache.containsKey(nextPage)) {
                try {
                    Log.d(TAG, "Prefetching page " + nextPage);
                    List<BookInfo> books = fetchBooksFromGenre(genre.getUrl(), nextPage);
                    if (!books.isEmpty()) {
                        pageCache.put(nextPage, books);
                        Log.d(TAG, "Prefetched page " + nextPage + " with " + books.size() + " books");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Prefetch failed for page " + nextPage, e);
                }
            }

            // Prefetch previous page
            int prevPage = currentPage - 1;
            if (prevPage > 0 && !pageCache.containsKey(prevPage)) {
                try {
                    Log.d(TAG, "Prefetching page " + prevPage);
                    List<BookInfo> books = fetchBooksFromGenre(genre.getUrl(), prevPage);
                    if (!books.isEmpty()) {
                        pageCache.put(prevPage, books);
                        Log.d(TAG, "Prefetched page " + prevPage + " with " + books.size() + " books");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Prefetch failed for page " + prevPage, e);
                }
            }
        });
    }

    private List<BookInfo> fetchBooksFromGenre(String genreUrl, int page) throws Exception {
        List<BookInfo> books = new ArrayList<>();

        Document doc = downloadUtils.fetchPage(genreUrl + "page/" + page, client);

        if (doc == null) {
            Log.d(TAG, "Failed to fetch page " + page);
            return books;
        }

        Elements articles = doc.select("article");

        if (articles.isEmpty()) {
            Log.d(TAG, "No articles found on page " + page);
            return books;
        }

        // Collect all book URLs first
        List<String> bookUrls = new ArrayList<>();
        for (Element article : articles) {
            Element header = article.selectFirst("header.entry-header");
            if (header == null) continue;

            Element aTag = header.selectFirst("a.entry-title-link[href]");
            if (aTag == null) continue;

            Element postmetainfo = article.selectFirst("div.postmetainfo");
            if (postmetainfo == null) continue;

            if (!downloadUtils.isEnglish(postmetainfo)) {
                continue;
            }

            bookUrls.add(aTag.attr("href"));
        }

        // Fetch book info in parallel
        List<Future<BookInfo>> futures = new ArrayList<>();
        for (String bookUrl : bookUrls) {
            futures.add(executorService.submit(() -> {
                try {
                    return downloadUtils.getBookInfo(bookUrl, client);
                } catch (Exception e) {
                    Log.e(TAG, "Error fetching book info for: " + bookUrl, e);
                    return null;
                }
            }));
        }

        // Collect results
        for (Future<BookInfo> future : futures) {
            try {
                BookInfo bookInfo = future.get(15, TimeUnit.SECONDS);
                List<DownloadLink> downloadLink = bookInfo.getDownloadLinks();

                String result = downloadutils.fetchAndDownload(downloadLink, client, bookInfo, 3);
                if (result != null) {
                    Log.e(TAG, "downloadLink: " + downloadLink);
                    Log.e(TAG, "processBookInfo: " + result);
                    bookInfo.setDownlink(result);
                    downloadLink.get(0).setDownlink(result);
                }
                if (bookInfo != null) {
                    books.add(bookInfo);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting book info from future", e);
            }
        }

        if (!books.isEmpty()) {
            hasMorePages = true;
        }

        return books;
    }

    private void updatePaginationControls() {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            String pageInfo = "Page " + currentPage;
            if (currentPage >= MAX_PAGES) {
                pageInfo += " (Last)";
            }
            pageInfoText.setText(pageInfo);

            previousButton.setEnabled(currentPage > 1 && !isLoading);
            previousButton.setAlpha((currentPage > 1 && !isLoading) ? 1.0f : 0.5f);

            nextButton.setEnabled(hasMorePages && currentPage < MAX_PAGES && !isLoading);
            nextButton.setAlpha((hasMorePages && currentPage < MAX_PAGES && !isLoading) ? 1.0f : 0.5f);

            if (isLoading) {
                previousButton.setText("Loading...");
                nextButton.setText("Loading...");
            } else {
                previousButton.setText("Previous");
                nextButton.setText("Next");
            }
        });
    }

    private void updateStatus(String message) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> statusText.setText(message));
        }
    }

    private void showError(String errorMessage) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                booksRecyclerView.setVisibility(View.GONE);
                emptyStateLayout.setVisibility(View.VISIBLE);
                paginationLayout.setVisibility(View.GONE);
                statusText.setText(errorMessage);
                isLoading = false;
            });
        }
    }

    private void showEmptyState() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                booksRecyclerView.setVisibility(View.GONE);
                emptyStateLayout.setVisibility(View.VISIBLE);
                paginationLayout.setVisibility(View.GONE);
                isLoading = false;
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Cancel prefetch
        if (prefetchFuture != null) {
            prefetchFuture.cancel(true);
        }

        // Shutdown executor
        if (executorService != null) {
            executorService.shutdown();
        }

        if (client != null) {
            new Thread(() -> {
                client.dispatcher().executorService().shutdown();
                client.connectionPool().evictAll();
            }).start();
        }

        // Clear cache
        pageCache.clear();
    }
}