package com.kiduyu.klaus.ebookfinaldownload.fragments;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
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
import com.kiduyu.klaus.ebookfinaldownload.repository.BookRepository;
import com.kiduyu.klaus.ebookfinaldownload.services.BookSyncService;
import com.kiduyu.klaus.ebookfinaldownload.utils.DownloadEpub;
import com.kiduyu.klaus.ebookfinaldownload.utils.DownloadUtils;

import java.util.ArrayList;
import java.util.List;

public class NewReleasesFragment extends Fragment implements BookSyncService.SyncCallback {

    private static final String TAG = "NewReleasesFragment";

    // UI Elements
    private TextView statusText;
    private TextView bookCountText;
    private ProgressBar progressBar;
    private RecyclerView booksRecyclerView;
    private LinearLayout emptyStateLayout;
    private SwipeRefreshLayout swipeRefreshLayout;
    private MaterialButton loadMoreButton;

    // Adapter and data
    private BookAdapter bookAdapter;
    private List<BookInfo> booksList;

    // Repository and service
    private BookRepository bookRepository;
    private BookSyncService syncService;
    private boolean isSyncServiceBound = false;
    private Handler mainHandler;

    // Utilities
    private DownloadEpub downloadEpub;
    private DownloadUtils downloadUtils;

    private boolean isLoadingFromDatabase = false;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BookSyncService.LocalBinder binder = (BookSyncService.LocalBinder) service;
            syncService = binder.getService();
            syncService.setSyncCallback(NewReleasesFragment.this);
            isSyncServiceBound = true;
            Log.d(TAG, "BookSyncService connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isSyncServiceBound = false;
            Log.d(TAG, "BookSyncService disconnected");
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_new_releases, container, false);

        initializeViews(view);
        setupRecyclerView();
        setupListeners();

        this.mainHandler = new Handler(Looper.getMainLooper());
        this.bookRepository = BookRepository.getInstance(requireContext());

        if (getActivity() != null) {
            downloadEpub = new DownloadEpub(getActivity());
            downloadUtils = new DownloadUtils(getContext());
            downloadEpub.checkAndRequestPermissions();
        }

        // Bind to BookSyncService
        bindToSyncService();

        // Load books from database on fragment creation
        loadBooksFromDatabase();

        // Start background sync
        startBackgroundSync();

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

    private void setupRecyclerView() {
        booksList = new ArrayList<>();
        bookAdapter = new BookAdapter(getContext(), booksList);
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 1);
        booksRecyclerView.setLayoutManager(layoutManager);
        booksRecyclerView.setAdapter(bookAdapter);
    }

    private void setupListeners() {
        // Swipe to refresh - triggers background sync
        swipeRefreshLayout.setOnRefreshListener(() -> {
            Log.d(TAG, "Swipe to refresh triggered");
            if (syncService != null && isSyncServiceBound) {
                syncService.startSync();
            } else {
                startBackgroundSync();
            }
        });

        // Load more button - not used with database, but kept for future pagination
        loadMoreButton.setOnClickListener(v -> {
            Log.d(TAG, "Load more button clicked");
            loadMoreButton.setEnabled(false);
            if (syncService != null && isSyncServiceBound) {
                syncService.startSync();
            }
        });
    }

    /**
     * Load books from local database
     */
    private void loadBooksFromDatabase() {
        if (isLoadingFromDatabase) return;

        isLoadingFromDatabase = true;
        progressBar.setVisibility(ProgressBar.VISIBLE);
        updateStatus("Loading books from database...");

        new Thread(() -> {
            try {
                List<BookInfo> books = bookRepository.getAllBooksFromDatabase();

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        booksList.clear();
                        booksList.addAll(books);
                        bookAdapter.notifyDataSetChanged();

                        if (books.isEmpty()) {
                            emptyStateLayout.setVisibility(View.VISIBLE);
                            booksRecyclerView.setVisibility(View.GONE);
                            updateStatus("No books in database. Syncing...");
                        } else {
                            emptyStateLayout.setVisibility(View.GONE);
                            booksRecyclerView.setVisibility(View.VISIBLE);
                            updateBookCount(books.size());
                            updateStatus("Loaded " + books.size() + " books from database");
                        }

                        progressBar.setVisibility(ProgressBar.GONE);
                        isLoadingFromDatabase = false;
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "Error loading books from database", e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        updateStatus("Error loading books: " + e.getMessage());
                        progressBar.setVisibility(ProgressBar.GONE);
                        isLoadingFromDatabase = false;
                    });
                }
            }
        }).start();
    }

    /**
     * Start background sync service
     */
    private void startBackgroundSync() {
        Log.d(TAG, "Starting background sync");
        updateStatus("Syncing with server...");

        if (syncService != null && isSyncServiceBound) {
            syncService.startSync();
        } else {
            // Start the service if not already bound
            Intent intent = new Intent(getContext(), BookSyncService.class);
            if (getContext() != null) {
                getContext().startService(intent);
            }
        }
    }

    /**
     * Bind to BookSyncService
     */
    private void bindToSyncService() {
        Intent intent = new Intent(getContext(), BookSyncService.class);
        if (getContext() != null) {
            getContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    /**
     * Unbind from BookSyncService
     */
    private void unbindFromSyncService() {
        if (isSyncServiceBound && getContext() != null) {
            getContext().unbindService(serviceConnection);
            isSyncServiceBound = false;
        }
    }

    /**
     * Update status text
     */
    private void updateStatus(String message) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> statusText.setText(message));
        }
    }

    /**
     * Update book count text
     */
    private void updateBookCount(int count) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> bookCountText.setText("Books Loaded: " + count));
        }
    }

    // ==================== BookSyncService.SyncCallback Implementation ====================

    @Override
    public void onSyncStarted() {
        Log.d(TAG, "Sync started");
        updateStatus("Syncing new releases...");
        progressBar.setVisibility(ProgressBar.VISIBLE);
    }

    @Override
    public void onSyncProgress(String message) {
        Log.d(TAG, "Sync progress: " + message);
        updateStatus(message);
    }

    @Override
    public void onSyncCompleted(int booksCount) {
        Log.d(TAG, "Sync completed with " + booksCount + " books");
        updateStatus("✅ Sync completed!");

        // Reload books from database
        loadBooksFromDatabase();

        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                swipeRefreshLayout.setRefreshing(false);
                loadMoreButton.setEnabled(true);
                progressBar.setVisibility(ProgressBar.GONE);
            });
        }
    }

    @Override
    public void onSyncError(String error) {
        Log.e(TAG, "Sync error: " + error);
        updateStatus("❌ Sync error: " + error);

        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                swipeRefreshLayout.setRefreshing(false);
                loadMoreButton.setEnabled(true);
                progressBar.setVisibility(ProgressBar.GONE);
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbindFromSyncService();

        if (bookRepository != null) {
            bookRepository.shutdown();
        }
    }
}
