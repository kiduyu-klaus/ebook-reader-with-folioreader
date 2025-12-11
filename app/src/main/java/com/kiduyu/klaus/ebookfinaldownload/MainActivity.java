package com.kiduyu.klaus.ebookfinaldownload;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.Menu;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;
import com.kiduyu.klaus.ebookfinaldownload.fragments.CategoriesFragment;
import com.kiduyu.klaus.ebookfinaldownload.fragments.FavoritesFragment;
import com.kiduyu.klaus.ebookfinaldownload.fragments.HomeFragment;
import com.kiduyu.klaus.ebookfinaldownload.fragments.MyBooksFragment;
import com.kiduyu.klaus.ebookfinaldownload.fragments.RecentFragment;
import com.kiduyu.klaus.ebookfinaldownload.fragments.SearchFragment;
import com.kiduyu.klaus.ebookfinaldownload.fragments.SettingsFragment;
import com.kiduyu.klaus.ebookfinaldownload.models.BookItem;
import com.kiduyu.klaus.ebookfinaldownload.utils.EpubCoverExtractor;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private MaterialToolbar toolbar;
    private ActionBarDrawerToggle toggle;

    private FragmentManager fragmentManager;
    private Fragment currentFragment;
    private List<BookItem> allBooks = new ArrayList<>();
    private List<BookItem> filteredBooks = new ArrayList<>();

    private String currentFilter = "all";    // all | epub | pdf
    private String currentSort = "name";     // name | date | size


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fragmentManager = getSupportFragmentManager();

        initializeViews();
        setupToolbar();
        setupNavigationDrawer();
        loadAllBooks();
        // Load home fragment by default
        if (savedInstanceState == null) {
            String fragmentToOpen = getIntent().getStringExtra("open_fragment");

            if ("my_books".equals(fragmentToOpen)) {
                loadFragment(new MyBooksFragment());
                navigationView.setCheckedItem(R.id.nav_my_books);
            } else {
                loadFragment(new HomeFragment());
                navigationView.setCheckedItem(R.id.nav_home);
            }
        }
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }
    private void loadAllBooks() {
        File dir = new File("/storage/emulated/0/Android/data/com.kiduyu.klaus.ebookfinaldownload/files");

        allBooks.clear();

        if (dir.exists() && dir.isDirectory()) {

            File[] files = dir.listFiles();
            if (files != null) {

                for (File file : files) {
                    if (!file.isFile()) continue;

                    String name = file.getName();
                    String path = file.getAbsolutePath();
                    String size = formatSize(file.length());
                    String date = String.valueOf(file.lastModified());

                    // No cover image file? Then leave null or set default.
                    String coverImage = EpubCoverExtractor.extractCoverImage(
                            MainActivity.this,
                            file.getAbsolutePath()
                    );

                    BookItem book = new BookItem(name, path, size, date, coverImage);
                    allBooks.add(book);
                }
            }
        }
    }
    private String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
        return sdf.format(new Date(timestamp));
    }
    private String formatSize(long bytes) {
        float kb = bytes / 1024f;
        float mb = kb / 1024f;

        if (mb >= 1)
            return String.format("%.2f MB", mb);
        else
            return String.format("%.2f KB", kb);
    }


    private void setupNavigationDrawer() {
        navigationView.setNavigationItemSelectedListener(this);

        toggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        ) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                hideKeyboard();
            }
        };

        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
    }

    public void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out
        );
        transaction.replace(R.id.content_frame, fragment);
        transaction.commit();
        currentFragment = fragment;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_search) {
            loadFragment(new SearchFragment());
            navigationView.setCheckedItem(R.id.nav_search);
            toolbar.setTitle("Search Books");
            return true;
        } else if (id == R.id.action_refresh) {
            refreshCurrentFragment();
            Toast.makeText(this, "Refreshed", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_settings) {
            loadFragment(new SettingsFragment());
            navigationView.setCheckedItem(R.id.nav_settings);
            toolbar.setTitle("Settings");
            return true;
        } else if (id == R.id.filter_all) {
            currentFilter = "all";
            applyFilterAndSort();
            Toast.makeText(this, "Showing All Books", Toast.LENGTH_SHORT).show();
            return true;

        } else if (id == R.id.filter_epub) {
            currentFilter = "epub";
            applyFilterAndSort();
            Toast.makeText(this, "Showing EPUB Only", Toast.LENGTH_SHORT).show();
            return true;

        } else if (id == R.id.filter_pdf) {
            currentFilter = "pdf";
            applyFilterAndSort();
            Toast.makeText(this, "Showing PDF Only", Toast.LENGTH_SHORT).show();
            return true;

        } else if (id == R.id.sort_name) {
            currentSort = "name";
            applyFilterAndSort();
            Toast.makeText(this, "Sorted by Name", Toast.LENGTH_SHORT).show();
            return true;

        } else if (id == R.id.sort_date) {
            currentSort = "date";
            applyFilterAndSort();
            Toast.makeText(this, "Sorted by Date", Toast.LENGTH_SHORT).show();
            return true;

        } else if (id == R.id.sort_size) {
            currentSort = "size";
            applyFilterAndSort();
            Toast.makeText(this, "Sorted by Size", Toast.LENGTH_SHORT).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void applyFilterAndSort() {

        // FILTER
        filteredBooks.clear();

        for (BookItem book : allBooks) {
            String title = book.getTitle().toLowerCase();

            if (currentFilter.equals("epub") && !title.endsWith(".epub"))
                continue;

            if (currentFilter.equals("pdf") && !title.endsWith(".pdf"))
                continue;

            filteredBooks.add(book);
        }

        // SORT
        switch (currentSort) {

            case "name":
                Collections.sort(filteredBooks, (b1, b2) ->
                        b1.getTitle().compareToIgnoreCase(b2.getTitle()));
                break;

            case "date":
                Collections.sort(filteredBooks, (b1, b2) ->
                        Long.compare(Long.parseLong(b2.getDate()), Long.parseLong(b1.getDate())));
                break;

            case "size":
                Collections.sort(filteredBooks, (b1, b2) -> {

                    long size1 = extractSizeFromString(b1.getSize());
                    long size2 = extractSizeFromString(b2.getSize());

                    return Long.compare(size2, size1); // largest first
                });
                break;
        }

        updateCurrentFragmentList(filteredBooks);
    }
    private long extractSizeFromString(String sizeText) {
        try {
            if (sizeText.contains("MB")) {
                return (long)(Float.parseFloat(sizeText.replace("MB", "").trim()) * 1024 * 1024);
            }
            if (sizeText.contains("KB")) {
                return (long)(Float.parseFloat(sizeText.replace("KB", "").trim()) * 1024);
            }
        } catch (Exception ignored) {}

        return 0;
    }

    private void updateCurrentFragmentList(List<BookItem> files) {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.content_frame);

        if (fragment instanceof MyBooksFragment) {
            ((MyBooksFragment) fragment).updateFileList(files);
        }
    }

    private void refreshCurrentFragment() {
        if (currentFragment != null) {
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.detach(currentFragment);
            transaction.attach(currentFragment);
            transaction.commit();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Fragment selectedFragment = null;

        if (id == R.id.nav_home) {
            selectedFragment = new HomeFragment();
            toolbar.setTitle("EBook Reader");
        } else if (id == R.id.nav_search) {
            selectedFragment = new SearchFragment();
            toolbar.setTitle("Search Books");
        } else if (id == R.id.nav_my_books) {
            selectedFragment = new MyBooksFragment();
            toolbar.setTitle("My Books");
        } else if (id == R.id.nav_downloads) {
            selectedFragment = new MyBooksFragment();
            toolbar.setTitle("Downloads");
        } else if (id == R.id.nav_favorites) {
            selectedFragment = new FavoritesFragment();
            toolbar.setTitle("Favorites");
        } else if (id == R.id.nav_recent) {
            selectedFragment = new RecentFragment();
            toolbar.setTitle("Recently Read");
        } else if (id == R.id.nav_categories) {
            selectedFragment = new CategoriesFragment();
            toolbar.setTitle("Categories");
        } else if (id == R.id.nav_settings) {
            selectedFragment = new SettingsFragment();
            toolbar.setTitle("Settings");
        } else if (id == R.id.nav_storage) {
            showStorageInfo();
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        } else if (id == R.id.nav_about) {
            showAboutDialog();
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        }

        if (selectedFragment != null) {
            loadFragment(selectedFragment);
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showStorageInfo() {
        File booksDir = getExternalFilesDir(null);
        long totalSize = 0;
        int fileCount = 0;

        if (booksDir != null && booksDir.exists()) {
            File[] files = booksDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().toLowerCase().endsWith(".epub")) {
                        totalSize += file.length();
                        fileCount++;
                    }
                }
            }
        }

        double sizeMB = totalSize / (1024.0 * 1024.0);
        String message = String.format(
                "Storage Information\n\n" +
                        "Total Books: %d\n" +
                        "Total Size: %.2f MB\n" +
                        "Location: %s",
                fileCount,
                sizeMB,
                booksDir != null ? booksDir.getAbsolutePath() : "Unknown"
        );

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Storage Management")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .setNeutralButton("Clear Cache", (dialog, which) -> {
                    Toast.makeText(this, "Cache cleared", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void showAboutDialog() {
        String aboutMessage =
                "EBook Reader\n" +
                        "Version 1.0\n\n" +
                        "A modern ebook reader application for Android.\n\n" +
                        "Features:\n" +
                        "• Search and download books\n" +
                        "• Read EPUB files\n" +
                        "• Manage your library\n" +
                        "• Beautiful modern UI\n\n" +
                        "© 2024 All rights reserved";

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("About")
                .setMessage(aboutMessage)
                .setPositiveButton("OK", null)
                .setNeutralButton("Rate App", (dialog, which) -> {
                    Toast.makeText(this, "Opening Play Store...", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager)
                            getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else if (!(currentFragment instanceof HomeFragment)) {
            // If not on home fragment, go back to home
            loadFragment(new HomeFragment());
            navigationView.setCheckedItem(R.id.nav_home);
            toolbar.setTitle("EBook Reader");
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (toggle != null) {
            drawerLayout.removeDrawerListener(toggle);
        }
    }
}