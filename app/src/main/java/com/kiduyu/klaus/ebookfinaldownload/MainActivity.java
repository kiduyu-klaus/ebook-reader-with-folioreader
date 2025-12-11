package com.kiduyu.klaus.ebookfinaldownload;

import android.content.Intent;
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
import com.kiduyu.klaus.ebookfinaldownload.fragments.HomeFragment;
import com.kiduyu.klaus.ebookfinaldownload.fragments.MyBooksFragment;

import java.io.File;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private MaterialToolbar toolbar;
    private ActionBarDrawerToggle toggle;

    private FragmentManager fragmentManager;
    private Fragment currentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fragmentManager = getSupportFragmentManager();

        initializeViews();
        setupToolbar();
        setupNavigationDrawer();

        // Load home fragment by default
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
            navigationView.setCheckedItem(R.id.nav_home);
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
            Intent intent = new Intent(this, SearchBook.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_refresh) {
            refreshCurrentFragment();
            Toast.makeText(this, "Refreshed", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_settings) {
            loadFragment(new MyBooksFragment());
            navigationView.setCheckedItem(R.id.nav_settings);
            return true;
        } else if (id == R.id.filter_all) {
            Toast.makeText(this, "Show All Books", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.filter_epub) {
            Toast.makeText(this, "Show EPUB Only", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.filter_pdf) {
            Toast.makeText(this, "Show PDF Only", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.sort_name) {
            Toast.makeText(this, "Sort by Name", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.sort_date) {
            Toast.makeText(this, "Sort by Date", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.sort_size) {
            Toast.makeText(this, "Sort by Size", Toast.LENGTH_SHORT).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
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
            Intent intent = new Intent(this, SearchBook.class);
            startActivity(intent);
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        } else if (id == R.id.nav_my_books) {
            selectedFragment = new MyBooksFragment();
            toolbar.setTitle("My Books");
        } else if (id == R.id.nav_downloads) {
            selectedFragment = new MyBooksFragment();
            toolbar.setTitle("Downloads");
        } else if (id == R.id.nav_favorites) {
            selectedFragment = new MyBooksFragment();
            toolbar.setTitle("Favorites");
        } else if (id == R.id.nav_recent) {
            selectedFragment = new MyBooksFragment();
            toolbar.setTitle("Recently Read");
        } else if (id == R.id.nav_categories) {
            selectedFragment = new MyBooksFragment();
            toolbar.setTitle("Categories");
        } else if (id == R.id.nav_settings) {
            selectedFragment = new MyBooksFragment();
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