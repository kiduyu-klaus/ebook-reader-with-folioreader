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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.kiduyu.klaus.ebookfinaldownload.R;
import com.kiduyu.klaus.ebookfinaldownload.adapters.BookAdapter;
import com.kiduyu.klaus.ebookfinaldownload.models.BookInfo;
import com.kiduyu.klaus.ebookfinaldownload.models.Genre;
import com.kiduyu.klaus.ebookfinaldownload.utils.DownloadUtils;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;

public class GenreBooksFragment extends Fragment {

    private static final String ARG_GENRE = "genre";
    private static final String TAG = "GenreBooksFragment";
    private static final int DEFAULT_TIMEOUT = 15000;
    private static final int MAX_PAGES = 20; // Maximum pages available on website

    private Genre genre;
    private OkHttpClient client;
    private Handler mainHandler;
    private DownloadUtils downloadUtils;

    // UI Elements
    private RecyclerView booksRecyclerView;
    private ProgressBar progressBar;
    private TextView statusText;
    private LinearLayout emptyStateLayout;
    private LinearLayout paginationLayout;
    private MaterialButton previousButton;
    private MaterialButton nextButton;
    private TextView pageInfoText;

    // Adapter
    private BookAdapter bookAdapter;
    private List<BookInfo> booksList;

    // Pagination state
    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean hasMorePages = true;

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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_genre_books, container, false);

        initializeViews(view);
        initializeClient();
        setupRecyclerView();
        setupPaginationButtons();

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
    }

    private void initializeClient() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)
                .readTimeout(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)
                .followRedirects(true)
                .connectionPool(new ConnectionPool(10, 5, TimeUnit.MINUTES))
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

        isLoading = true;
        progressBar.setVisibility(View.VISIBLE);
        statusText.setText("Loading page " + page + " for: " + genre.getName());
        booksRecyclerView.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.GONE);

        // Update pagination controls
        updatePaginationControls();

        new Thread(() -> {
            try {
                List<BookInfo> books = fetchBooksFromGenre(genre.getUrl(), page);

                if (books.isEmpty()) {
                    if (page == 1) {
                        updateStatus("No English books found in this genre");
                        showEmptyState();
                    } else {
                        // No more pages available
                        hasMorePages = false;
                        currentPage--; // Go back to previous page
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

                // Update UI on main thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        booksList.clear();
                        booksList.addAll(books);
                        bookAdapter.notifyDataSetChanged();

                        // Scroll to top
                        booksRecyclerView.scrollToPosition(0);

                        progressBar.setVisibility(View.GONE);
                        booksRecyclerView.setVisibility(View.VISIBLE);
                        paginationLayout.setVisibility(View.VISIBLE);

                        statusText.setText(books.size() + " English books found on page " + page);
                        isLoading = false;

                        // Update pagination controls
                        updatePaginationControls();

                        Log.d(TAG, "Loaded " + books.size() + " books for page " + page);
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "Error loading books for page " + page + ": " + e.getMessage(), e);
                updateStatus("Error: " + e.getMessage());
                if (page == 1) {
                    showEmptyState();
                } else {
                    currentPage--; // Go back to previous page
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            updatePaginationControls();
                        });
                    }
                }
                isLoading = false;
            }
        }).start();
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

        for (Element article : articles) {
            Element header = article.selectFirst("header.entry-header");
            if (header == null) continue;

            Element aTag = header.selectFirst("a.entry-title-link[href]");
            if (aTag == null) continue;

            Element postmetainfo = article.selectFirst("div.postmetainfo");
            if (postmetainfo == null) continue;

            // Filter for English language only
            if (!downloadUtils.isEnglish(postmetainfo)) {
                continue;
            }

            String bookUrl = aTag.attr("href");
            String bookTitle = aTag.text();

            try {
                BookInfo bookInfo = downloadUtils.getBookInfo(bookUrl, client);
                if (bookInfo != null) {
                    books.add(bookInfo);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching book info for: " + bookTitle, e);
            }
        }

        // If we got books, assume there might be more pages
        if (!books.isEmpty()) {
            hasMorePages = true;
        }

        Thread.sleep(2000); // Rate limiting

        return books;
    }

    private void updatePaginationControls() {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            // Update page info
            String pageInfo = "Page " + currentPage;
            if (currentPage >= MAX_PAGES) {
                pageInfo += " (Last)";
            }
            pageInfoText.setText(pageInfo);

            // Update button states
            previousButton.setEnabled(currentPage > 1 && !isLoading);
            previousButton.setAlpha((currentPage > 1 && !isLoading) ? 1.0f : 0.5f);

            nextButton.setEnabled(hasMorePages && currentPage < MAX_PAGES && !isLoading);
            nextButton.setAlpha((hasMorePages && currentPage < MAX_PAGES && !isLoading) ? 1.0f : 0.5f);

            // Show loading state
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
        if (client != null) {
            new Thread(() -> {
                client.dispatcher().executorService().shutdown();
                client.connectionPool().evictAll();
            }).start();
        }
    }
}