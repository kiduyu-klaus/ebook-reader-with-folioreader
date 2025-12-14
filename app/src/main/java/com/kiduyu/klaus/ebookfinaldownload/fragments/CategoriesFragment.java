package com.kiduyu.klaus.ebookfinaldownload.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
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

import com.kiduyu.klaus.ebookfinaldownload.R;
import com.kiduyu.klaus.ebookfinaldownload.adapters.GenreAdapter;
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

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CategoriesFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CategoriesFragment extends Fragment {

    private static final String TAG = "GenresFragment";
    private static final String GENRES_URL = "https://oceanofpdf.com/books-by-genre/";
    private static final int DEFAULT_TIMEOUT = 15000;

    private OkHttpClient client;
    private Handler mainHandler;
    private DownloadUtils downloadUtils;

    // UI Elements
    private EditText searchInput;
    private RecyclerView genresRecyclerView;
    private ProgressBar progressBar;
    private TextView statusText;
    private LinearLayout emptyStateLayout;

    // Adapter
    private GenreAdapter genreAdapter;
    private List<Genre> allGenres;
    private List<Genre> filteredGenres;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_categories, container, false);

        initializeViews(view);
        initializeClient();
        setupRecyclerView();
        setupSearchFilter();

        this.mainHandler = new Handler(Looper.getMainLooper());
        this.downloadUtils = new DownloadUtils(getContext());

        loadGenres();

        return view;
    }

    private void initializeViews(View view) {
        searchInput = view.findViewById(R.id.genreSearchInput);
        genresRecyclerView = view.findViewById(R.id.genresRecyclerView);
        progressBar = view.findViewById(R.id.genreProgressBar);
        statusText = view.findViewById(R.id.genreStatusText);
        emptyStateLayout = view.findViewById(R.id.genreEmptyStateLayout);
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
        allGenres = new ArrayList<>();
        filteredGenres = new ArrayList<>();
        genreAdapter = new GenreAdapter(getContext(), filteredGenres, genre -> {
            // Handle genre click - navigate to GenreBooksFragment
            navigateToGenreBooks(genre);
        });
        genresRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        genresRecyclerView.setAdapter(genreAdapter);
    }

    private void navigateToGenreBooks(Genre genre) {
        Fragment genreBooksFragment = GenreBooksFragment.newInstance(genre);
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, genreBooksFragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void setupSearchFilter() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterGenres(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterGenres(String query) {
        filteredGenres.clear();

        if (query.isEmpty()) {
            filteredGenres.addAll(allGenres);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (Genre genre : allGenres) {
                if (genre.getName().toLowerCase().contains(lowerCaseQuery)) {
                    filteredGenres.add(genre);
                }
            }
        }

        genreAdapter.notifyDataSetChanged();

        // Show/hide empty state
        if (filteredGenres.isEmpty() && !allGenres.isEmpty()) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            genresRecyclerView.setVisibility(View.GONE);
            statusText.setText("No genres found matching '" + query + "'");
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            genresRecyclerView.setVisibility(View.VISIBLE);
            statusText.setText(filteredGenres.size() + " genres");
        }
    }

    private void loadGenres() {
        progressBar.setVisibility(View.VISIBLE);
        statusText.setText("Loading genres...");
        genresRecyclerView.setVisibility(View.GONE);

        new Thread(() -> {
            try {
                Document doc = downloadUtils.fetchPage(GENRES_URL, client);

                if (doc == null) {
                    updateStatus("Failed to load genres page");
                    showEmptyState();
                    return;
                }

                List<Genre> genres = parseGenres(doc);

                if (genres.isEmpty()) {
                    updateStatus("No genres found");
                    showEmptyState();
                    return;
                }

                // Update UI on main thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        allGenres.clear();
                        allGenres.addAll(genres);
                        filteredGenres.clear();
                        filteredGenres.addAll(genres);
                        genreAdapter.notifyDataSetChanged();

                        progressBar.setVisibility(View.GONE);
                        genresRecyclerView.setVisibility(View.VISIBLE);
                        statusText.setText(genres.size() + " genres available");

                        Log.d(TAG, "Loaded " + genres.size() + " genres");
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "Error loading genres: " + e.getMessage(), e);
                updateStatus("Error: " + e.getMessage());
                showEmptyState();
            }
        }).start();
    }

    private List<Genre> parseGenres(Document doc) {
        List<Genre> genres = new ArrayList<>();

        Element entryContent = doc.selectFirst("div.entry-content");
        if (entryContent == null) {
            Log.e(TAG, "entry-content div not found");
            return genres;
        }

        Elements genreHeaders = entryContent.select("h3.h3genres");

        for (Element header : genreHeaders) {
            Element link = header.selectFirst("a[href]");
            if (link != null) {
                String name = link.text().trim();
                String url = link.attr("href");

                // Extract count from text (e.g., "Mystery (123)")
                String fullText = header.text();
                int count = extractCount(fullText);

                Genre genre = new Genre(name, url, count);
                genres.add(genre);
            }
        }

        return genres;
    }

    private int extractCount(String text) {
        try {
            // Extract number from parentheses, e.g., "(123)"
            int startIdx = text.lastIndexOf('(');
            int endIdx = text.lastIndexOf(')');

            if (startIdx != -1 && endIdx != -1 && startIdx < endIdx) {
                String countStr = text.substring(startIdx + 1, endIdx).trim();
                return Integer.parseInt(countStr);
            }
        } catch (Exception e) {
            Log.d(TAG, "Could not extract count from: " + text);
        }
        return 0;
    }

    private void updateStatus(String message) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> statusText.setText(message));
        }
    }

    private void showEmptyState() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                genresRecyclerView.setVisibility(View.GONE);
                emptyStateLayout.setVisibility(View.VISIBLE);
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