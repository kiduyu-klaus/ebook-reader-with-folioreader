package com.kiduyu.klaus.ebookfinaldownload.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.kiduyu.klaus.ebookfinaldownload.MainActivity;
import com.kiduyu.klaus.ebookfinaldownload.R;

import java.io.File;

public class HomeFragment extends Fragment {

    private MaterialCardView cardSearch;
    private MaterialCardView cardMyBooks;
    private MaterialCardView cardRaw;

    private NavigationView navigationView;
    private MaterialCardView cardAssets;
    private TextView tvTotalBooks;
    private TextView tvReadingNow;
    private TextView tvCompleted;
    private ExtendedFloatingActionButton fabAdd;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        initializeViews(view);
        setupClickListeners();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateStats();
    }

    private void initializeViews(View view) {
        cardSearch = view.findViewById(R.id.card_search);
        cardMyBooks = view.findViewById(R.id.card_my_books);
        cardRaw = view.findViewById(R.id.card_raw);
        cardAssets = view.findViewById(R.id.card_assets);
        tvTotalBooks = view.findViewById(R.id.tv_total_books);
        tvReadingNow = view.findViewById(R.id.tv_reading_now);
        tvCompleted = view.findViewById(R.id.tv_completed);
        fabAdd = view.findViewById(R.id.fab_add);
        navigationView = getActivity().findViewById(R.id.nav_view);
    }

    private void setupClickListeners() {
        cardSearch.setOnClickListener(v -> {
            if (getActivity() != null) {
                ((MainActivity) getActivity()).loadFragment(new SearchFragment());
                navigationView.setCheckedItem(R.id.nav_search);
            }
        });

        cardMyBooks.setOnClickListener(v -> {
            if (getActivity() != null) {
                ((MainActivity) getActivity()).loadFragment(new MyBooksFragment());
                navigationView.setCheckedItem(R.id.nav_my_books);
            }
        });

        cardRaw.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Opening from raw folder", Toast.LENGTH_SHORT).show();
        });

        cardAssets.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Opening from assets folder", Toast.LENGTH_SHORT).show();
        });

        fabAdd.setOnClickListener(v -> {
            if (getActivity() != null) {
                ((MainActivity) getActivity()).loadFragment(new SearchFragment());
            }
        });
    }

    private void updateStats() {
        if (getContext() == null) return;

        File booksDir = getContext().getExternalFilesDir(null);
        int totalBooks = 0;

        if (booksDir != null && booksDir.exists()) {
            File[] files = booksDir.listFiles((dir, name) ->
                    name.toLowerCase().endsWith(".epub"));
            if (files != null) {
                totalBooks = files.length;
            }
        }

        tvTotalBooks.setText(String.valueOf(totalBooks));
        tvReadingNow.setText("0");
        tvCompleted.setText("0");
    }
}