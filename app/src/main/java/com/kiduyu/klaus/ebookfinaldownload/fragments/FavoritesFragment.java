package com.kiduyu.klaus.ebookfinaldownload.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.kiduyu.klaus.ebookfinaldownload.MainActivity;
import com.kiduyu.klaus.ebookfinaldownload.R;
import com.kiduyu.klaus.ebookfinaldownload.ReadBook;
import com.kiduyu.klaus.ebookfinaldownload.adapters.BookListAdapter;
import com.kiduyu.klaus.ebookfinaldownload.models.BookItem;
import com.kiduyu.klaus.ebookfinaldownload.utils.EpubCoverExtractor;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class FavoritesFragment extends Fragment {

    private static final String PREFS_NAME = "EBookReaderPrefs";
    private static final String KEY_FAVORITES = "favorites";

    private RecyclerView recyclerView;
    private List<BookItem> favoritesList;
    private LinearLayout emptyStateLayout;
    private TextView tvFavoriteCount;
    private MaterialButton btnAddFirst;
    private FloatingActionButton fabSearch;
    private SharedPreferences prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorites, container, false);

        initializePreferences();
        initializeViews(view);
        setupRecyclerView();
        setupListeners();
        loadFavorites();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFavorites();
    }

    private void initializePreferences() {
        if (getContext() != null) {
            prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        }
    }

    private void initializeViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerViewFavorites);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        tvFavoriteCount = view.findViewById(R.id.tvFavoriteCount);
        btnAddFirst = view.findViewById(R.id.btnAddFirst);
        fabSearch = view.findViewById(R.id.fab_search);
    }

    private void setupRecyclerView() {
        favoritesList = new ArrayList<>();
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    private void setupListeners() {
        btnAddFirst.setOnClickListener(v -> {
            if (getActivity() != null) {
                ((MainActivity) getActivity()).loadFragment(new SearchFragment());
            }
        });

        fabSearch.setOnClickListener(v -> {
            if (getActivity() != null) {
                ((MainActivity) getActivity()).loadFragment(new SearchFragment());
            }
        });
    }

    private void loadFavorites() {
        if (getContext() == null || prefs == null) return;

        favoritesList = new ArrayList<>();
        Set<String> favoritePaths = prefs.getStringSet(KEY_FAVORITES, new HashSet<>());

        if (favoritePaths.isEmpty()) {
            showEmptyState();
            return;
        }

        showLoadingState();

        new Thread(() -> {
            List<BookItem> tempList = new ArrayList<>();

            for (String filePath : favoritePaths) {
                File file = new File(filePath);
                if (file.exists()) {
                    BookItem bookItem = new BookItem();
                    bookItem.setFilePath(file.getAbsolutePath());
                    bookItem.setTitle(EpubCoverExtractor.extractBookTitle(file.getAbsolutePath()));
                    bookItem.setSize(formatFileSize(file.length()));
                    bookItem.setDate(formatDate(file.lastModified()));

                    String coverPath = EpubCoverExtractor.extractCoverImage(
                            getContext(),
                            file.getAbsolutePath()
                    );
                    bookItem.setCoverImagePath(coverPath);

                    tempList.add(bookItem);
                }
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    favoritesList.clear();
                    favoritesList.addAll(tempList);

                    if (tempList.isEmpty()) {
                        showEmptyState();
                    } else {
                        showFavoritesState();
                        updateFavoriteCount(tempList.size());
                    }
                });
            }
        }).start();
    }

    private void showLoadingState() {
        recyclerView.setVisibility(View.VISIBLE);
        emptyStateLayout.setVisibility(View.GONE);
        tvFavoriteCount.setText("Loading...");
    }

    private void showFavoritesState() {
        recyclerView.setVisibility(View.VISIBLE);
        emptyStateLayout.setVisibility(View.GONE);

        BookListAdapter bookAdapter = new BookListAdapter(favoritesList, new BookListAdapter.OnBookClickListener() {
            @Override
            public void onBookClick(BookItem book) {
                openBook(book);
            }

            @Override
            public void onDeleteClick(BookItem book) {
                showRemoveFavoriteConfirmation(book);
            }
        });
        recyclerView.setAdapter(bookAdapter);
    }

    private void showEmptyState() {
        recyclerView.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.VISIBLE);
        tvFavoriteCount.setText("0 favorites");
    }

    private void updateFavoriteCount(int count) {
        if (count == 0) {
            tvFavoriteCount.setText("0 favorites");
        } else if (count == 1) {
            tvFavoriteCount.setText("1 favorite");
        } else {
            tvFavoriteCount.setText(count + " favorites");
        }
    }

    private void openBook(BookItem book) {
        Intent intent = new Intent(getActivity(), ReadBook.class);
        intent.putExtra("EPUB_PATH", book.getFilePath());
        intent.putExtra("BOOK_TITLE", book.getTitle());
        startActivity(intent);
    }

    private void showRemoveFavoriteConfirmation(BookItem book) {
        new AlertDialog.Builder(getContext())
                .setTitle("Remove from Favorites")
                .setMessage("Remove '" + book.getTitle() + "' from your favorites?\n\nThe book will not be deleted.")
                .setPositiveButton("Remove", (dialog, which) -> removeFromFavorites(book))
                .setNegativeButton("Cancel", null)
                .setIcon(R.drawable.ic_favorite)
                .show();
    }

    private void removeFromFavorites(BookItem book) {
        if (prefs == null) return;

        Set<String> favoritePaths = new HashSet<>(prefs.getStringSet(KEY_FAVORITES, new HashSet<>()));
        favoritePaths.remove(book.getFilePath());

        prefs.edit().putStringSet(KEY_FAVORITES, favoritePaths).apply();

        Toast.makeText(getContext(), "Removed from favorites", Toast.LENGTH_SHORT).show();
        loadFavorites();
    }

    // Public method to add to favorites (can be called from other fragments)
    public static void addToFavorites(Context context, String bookPath) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> favoritePaths = new HashSet<>(prefs.getStringSet(KEY_FAVORITES, new HashSet<>()));
        favoritePaths.add(bookPath);
        prefs.edit().putStringSet(KEY_FAVORITES, favoritePaths).apply();
    }

    // Public method to check if book is favorite
    public static boolean isFavorite(Context context, String bookPath) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> favoritePaths = prefs.getStringSet(KEY_FAVORITES, new HashSet<>());
        return favoritePaths.contains(bookPath);
    }

    // Public method to remove from favorites
    public static void removeFromFavorites(Context context, String bookPath) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> favoritePaths = new HashSet<>(prefs.getStringSet(KEY_FAVORITES, new HashSet<>()));
        favoritePaths.remove(bookPath);
        prefs.edit().putStringSet(KEY_FAVORITES, favoritePaths).apply();
    }

    // Public method to toggle favorite status
    public static void toggleFavorite(Context context, String bookPath) {
        if (isFavorite(context, bookPath)) {
            removeFromFavorites(context, bookPath);
        } else {
            addToFavorites(context, bookPath);
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format(Locale.US, "%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    private String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
        return sdf.format(new Date(timestamp));
    }
}