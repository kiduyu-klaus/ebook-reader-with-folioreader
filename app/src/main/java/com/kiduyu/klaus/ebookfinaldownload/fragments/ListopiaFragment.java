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
import com.kiduyu.klaus.ebookfinaldownload.adapters.ListopiaAdapter;
import com.kiduyu.klaus.ebookfinaldownload.models.BookInfo;
import com.kiduyu.klaus.ebookfinaldownload.models.DownloadLink;
import com.kiduyu.klaus.ebookfinaldownload.models.Listopia;
import com.kiduyu.klaus.ebookfinaldownload.utils.DownloadEpub;
import com.kiduyu.klaus.ebookfinaldownload.utils.DownloadUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;

public class ListopiaFragment extends Fragment implements ListopiaAdapter.OnListopiaClickListener {

    private static final String TAG = "ListopiaFragment";
    private static final int DEFAULT_TIMEOUT = 15000;

    private OkHttpClient client;
    private ExecutorService executor;
    private Handler mainHandler;

    // UI Elements - Category View
    private RecyclerView categoryRecyclerView;
    private ProgressBar categoryProgressBar;
    private TextView categoryStatusText;
    private LinearLayout categoryEmptyState;
    private MaterialButton refreshCategoriesButton;

    // UI Elements - Books View
    private LinearLayout booksViewLayout;
    private TextView selectedCategoryTitle;
    private MaterialButton backToCategoriesButton;
    private RecyclerView booksRecyclerView;
    private ProgressBar booksProgressBar;
    private TextView booksStatusText;
    private TextView booksCountText;
    private LinearLayout booksEmptyState;

    // Adapters
    private ListopiaAdapter categoryAdapter;
    private BookAdapter bookAdapter;
    private List<Listopia> categoryList;
    private List<BookInfo> booksList;

    private AtomicInteger booksFound;
    private DownloadEpub downloadEpub;
    private DownloadUtils downloadUtils;

    private Listopia currentListopia;
    private boolean showingCategories = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_listopia, container, false);

        initializeViews(view);
        initializeExecutors();
        setupRecyclerViews();
        setupListeners();

        this.mainHandler = new Handler(Looper.getMainLooper());
        this.booksFound = new AtomicInteger(0);

        if (getActivity() != null) {
            downloadEpub = new DownloadEpub(getActivity());
            downloadUtils = new DownloadUtils(getContext());
            downloadEpub.checkAndRequestPermissions();
        }

        // Auto-load categories on fragment creation
        loadCategories();

        return view;
    }

    private void initializeViews(View view) {
        // Category views
        categoryRecyclerView = view.findViewById(R.id.categoryRecyclerView);
        categoryProgressBar = view.findViewById(R.id.categoryProgressBar);
        categoryStatusText = view.findViewById(R.id.categoryStatusText);
        categoryEmptyState = view.findViewById(R.id.categoryEmptyState);
        refreshCategoriesButton = view.findViewById(R.id.refreshCategoriesButton);

        // Books views
        booksViewLayout = view.findViewById(R.id.booksViewLayout);
        selectedCategoryTitle = view.findViewById(R.id.selectedCategoryTitle);
        backToCategoriesButton = view.findViewById(R.id.backToCategoriesButton);
        booksRecyclerView = view.findViewById(R.id.booksRecyclerView);
        booksProgressBar = view.findViewById(R.id.booksProgressBar);
        booksStatusText = view.findViewById(R.id.booksStatusText);
        booksCountText = view.findViewById(R.id.booksCountText);
        booksEmptyState = view.findViewById(R.id.booksEmptyState);
    }

    private void initializeExecutors() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)
                .readTimeout(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)
                .followRedirects(true)
                .connectionPool(new ConnectionPool(10, 5, TimeUnit.MINUTES))
                .build();

        this.executor = Executors.newFixedThreadPool(4);
    }

    private void setupRecyclerViews() {
        // Setup category RecyclerView
        categoryList = new ArrayList<>();
        categoryAdapter = new ListopiaAdapter(getContext(), categoryList, this);
        categoryRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        categoryRecyclerView.setAdapter(categoryAdapter);

        // Setup books RecyclerView
        booksList = new ArrayList<>();
        bookAdapter = new BookAdapter(getContext(), booksList);
        booksRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        booksRecyclerView.setAdapter(bookAdapter);
    }

    private void setupListeners() {
        refreshCategoriesButton.setOnClickListener(v -> loadCategories());

        backToCategoriesButton.setOnClickListener(v -> {
            showCategoriesView();
        });
    }

    private void loadCategories() {
        categoryProgressBar.setVisibility(View.VISIBLE);
        categoryEmptyState.setVisibility(View.GONE);
        updateCategoryStatus("Loading Listopia categories...");

        new Thread(() -> {
            try {
                List<Listopia> categories = downloadUtils.getAllListopia(client);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        categoryAdapter.clearListopia();

                        for (Listopia listopia : categories) {
                            categoryAdapter.addListopia(listopia);
                        }

                        categoryProgressBar.setVisibility(View.GONE);

                        if (categories.isEmpty()) {
                            categoryEmptyState.setVisibility(View.VISIBLE);
                            updateCategoryStatus("No categories found");
                        } else {
                            updateCategoryStatus("Found " + categories.size() + " categories");
                        }
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "Error loading categories", e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        categoryProgressBar.setVisibility(View.GONE);
                        categoryEmptyState.setVisibility(View.VISIBLE);
                        updateCategoryStatus("Error loading categories: " + e.getMessage());
                    });
                }
            }
        }).start();
    }

    @Override
    public void onListopiaClick(Listopia listopia) {
        currentListopia = listopia;
        showBooksView();
        loadBooksFromCategory(listopia);
    }

    private void loadBooksFromCategory(Listopia listopia) {
        booksProgressBar.setVisibility(View.VISIBLE);
        booksEmptyState.setVisibility(View.GONE);
        booksFound.set(0);
        bookAdapter.clearBooks();

        selectedCategoryTitle.setText(listopia.getTitle());
        updateBooksStatus("Loading books from " + listopia.getTitle() + "...");

        new Thread(() -> {
            try {
                // Fetch books from the category (limit to first 50 for performance)
                List<BookInfo> books = downloadUtils.getBooksFromListopia(
                        listopia.getUrl(),
                        client,
                        10  // Limit to 50 books
                );

                // Process each book to get download links
                for (BookInfo bookInfo : books) {
                    if (getActivity() == null) break;

                    try {
                        List<DownloadLink> downloadLinks = bookInfo.getDownloadLinks();

                        if (downloadLinks != null && !downloadLinks.isEmpty()) {
                            String result = downloadUtils.fetchAndDownload(
                                    downloadLinks,
                                    client,
                                    bookInfo,
                                    3
                            );

                            if (result != null) {
                                bookInfo.setDownlink(result);
                                downloadLinks.get(0).setDownlink(result);
                            }
                        }

                        int count = booksFound.incrementAndGet();

                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                bookAdapter.addBook(bookInfo);
                                updateBooksCount(count);
                                updateBooksStatus("Loaded: " + bookInfo.getTitle());
                            });
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "Error processing book", e);
                    }
                }

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        booksProgressBar.setVisibility(View.GONE);

                        if (booksList.isEmpty()) {
                            booksEmptyState.setVisibility(View.VISIBLE);
                            updateBooksStatus("No books found in this category");
                        } else {
                            updateBooksStatus("✅ Loaded " + booksList.size() + " books");
                        }
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "Error loading books from category", e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        booksProgressBar.setVisibility(View.GONE);
                        booksEmptyState.setVisibility(View.VISIBLE);
                        updateBooksStatus("Error loading books: " + e.getMessage());
                    });
                }
            }
        }).start();
    }

    private void showCategoriesView() {
        showingCategories = true;
        booksViewLayout.setVisibility(View.GONE);
        categoryRecyclerView.setVisibility(View.VISIBLE);
        categoryStatusText.setVisibility(View.VISIBLE);
        refreshCategoriesButton.setVisibility(View.VISIBLE);
    }

    private void showBooksView() {
        showingCategories = false;
        categoryRecyclerView.setVisibility(View.GONE);
        categoryStatusText.setVisibility(View.GONE);
        categoryEmptyState.setVisibility(View.GONE);
        refreshCategoriesButton.setVisibility(View.GONE);
        booksViewLayout.setVisibility(View.VISIBLE);
    }

    private void updateCategoryStatus(String message) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> categoryStatusText.setText(message));
        }
    }

    private void updateBooksStatus(String message) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> booksStatusText.setText(message));
        }
    }

    private void updateBooksCount(int count) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> booksCountText.setText("Books Loaded: " + count));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }

    public void syncListopia() {
    }
}