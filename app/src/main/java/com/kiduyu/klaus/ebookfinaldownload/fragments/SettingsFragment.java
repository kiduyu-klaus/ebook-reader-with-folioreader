package com.kiduyu.klaus.ebookfinaldownload.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.kiduyu.klaus.ebookfinaldownload.R;

import java.io.File;

public class SettingsFragment extends Fragment {

    private static final String PREFS_NAME = "EBookReaderPrefs";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_AUTO_DOWNLOAD = "auto_download";
    private static final String KEY_WIFI_ONLY = "wifi_only";
    private static final String KEY_NOTIFICATIONS = "notifications";
    private static final String KEY_EPUB_ONLY = "epub_only";

    private SharedPreferences prefs;

    // UI Elements
    private SwitchMaterial switchDarkMode;
    private SwitchMaterial switchAutoDownload;
    private SwitchMaterial switchWifiOnly;
    private SwitchMaterial switchNotifications;
    private SwitchMaterial switchEpubOnly;

    private MaterialCardView cardClearCache;
    private MaterialCardView cardClearHistory;
    private MaterialCardView cardAbout;
    private MaterialCardView cardPrivacy;
    private MaterialCardView cardHelp;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        initializePreferences();
        initializeViews(view);
        loadSettings();
        setupListeners();

        return view;
    }

    private void initializePreferences() {
        if (getContext() != null) {
            prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        }
    }

    private void initializeViews(View view) {
        // Switches
        switchDarkMode = view.findViewById(R.id.switchDarkMode);
        switchAutoDownload = view.findViewById(R.id.switchAutoDownload);
        switchWifiOnly = view.findViewById(R.id.switchWifiOnly);
        switchNotifications = view.findViewById(R.id.switchNotifications);
        switchEpubOnly = view.findViewById(R.id.switchEpubOnly);

        // Cards
        cardClearCache = view.findViewById(R.id.cardClearCache);
        cardClearHistory = view.findViewById(R.id.cardClearHistory);
        cardAbout = view.findViewById(R.id.cardAbout);
        cardPrivacy = view.findViewById(R.id.cardPrivacy);
        cardHelp = view.findViewById(R.id.cardHelp);
    }

    private void loadSettings() {
        if (prefs == null) return;

        switchDarkMode.setChecked(prefs.getBoolean(KEY_DARK_MODE, false));
        switchAutoDownload.setChecked(prefs.getBoolean(KEY_AUTO_DOWNLOAD, true));
        switchWifiOnly.setChecked(prefs.getBoolean(KEY_WIFI_ONLY, true));
        switchNotifications.setChecked(prefs.getBoolean(KEY_NOTIFICATIONS, true));
        switchEpubOnly.setChecked(prefs.getBoolean(KEY_EPUB_ONLY, true));
    }

    private void setupListeners() {
        // Dark Mode Switch
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveSetting(KEY_DARK_MODE, isChecked);
            applyDarkMode(isChecked);
        });

        // Auto Download Switch
        switchAutoDownload.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveSetting(KEY_AUTO_DOWNLOAD, isChecked);
            Toast.makeText(getContext(),
                    isChecked ? "Auto-download enabled" : "Auto-download disabled",
                    Toast.LENGTH_SHORT).show();
        });

        // WiFi Only Switch
        switchWifiOnly.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveSetting(KEY_WIFI_ONLY, isChecked);
            Toast.makeText(getContext(),
                    isChecked ? "Downloads only on WiFi" : "Downloads on any network",
                    Toast.LENGTH_SHORT).show();
        });

        // Notifications Switch
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveSetting(KEY_NOTIFICATIONS, isChecked);
            Toast.makeText(getContext(),
                    isChecked ? "Notifications enabled" : "Notifications disabled",
                    Toast.LENGTH_SHORT).show();
        });

        // EPUB Only Switch
        switchEpubOnly.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveSetting(KEY_EPUB_ONLY, isChecked);
            Toast.makeText(getContext(),
                    isChecked ? "Showing EPUB only" : "Showing all formats",
                    Toast.LENGTH_SHORT).show();
        });

        // Clear Cache
        cardClearCache.setOnClickListener(v -> showClearCacheDialog());

        // Clear History
        cardClearHistory.setOnClickListener(v -> showClearHistoryDialog());

        // About
        cardAbout.setOnClickListener(v -> showAboutDialog());

        // Privacy
        cardPrivacy.setOnClickListener(v -> showPrivacyDialog());

        // Help
        cardHelp.setOnClickListener(v -> showHelpDialog());
    }

    private void saveSetting(String key, boolean value) {
        if (prefs != null) {
            prefs.edit().putBoolean(key, value).apply();
        }
    }

    private void applyDarkMode(boolean enabled) {
        if (enabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        Toast.makeText(getContext(), "Theme will be applied on next launch", Toast.LENGTH_SHORT).show();
    }

    private void showClearCacheDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Clear Cache")
                .setMessage("This will clear all cached data including cover images. Books will not be deleted.\n\nDo you want to continue?")
                .setPositiveButton("Clear", (dialog, which) -> {
                    clearCache();
                })
                .setNegativeButton("Cancel", null)
                .setIcon(R.drawable.ic_menu_manage)
                .show();
    }

    private void clearCache() {
        if (getContext() == null) return;

        File cacheDir = getContext().getCacheDir();
        long deletedSize = deleteDirectory(cacheDir);

        double sizeMB = deletedSize / (1024.0 * 1024.0);
        Toast.makeText(getContext(),
                String.format("Cache cleared: %.2f MB freed", sizeMB),
                Toast.LENGTH_SHORT).show();
    }

    private long deleteDirectory(File directory) {
        long deletedSize = 0;
        if (directory != null && directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deletedSize += deleteDirectory(file);
                    } else {
                        deletedSize += file.length();
                        file.delete();
                    }
                }
            }
        }
        return deletedSize;
    }

    private void showClearHistoryDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Clear Search History")
                .setMessage("This will clear your search history and reading history.\n\nDo you want to continue?")
                .setPositiveButton("Clear", (dialog, which) -> {
                    clearHistory();
                })
                .setNegativeButton("Cancel", null)
                .setIcon(R.drawable.ic_menu_info_details)
                .show();
    }

    private void clearHistory() {
        if (prefs != null) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove("search_history");
            editor.remove("reading_history");
            editor.apply();
            Toast.makeText(getContext(), "History cleared", Toast.LENGTH_SHORT).show();
        }
    }

    private void showAboutDialog() {
        String aboutMessage =
                "EBook Reader\n" +
                        "Version 1.0.0\n\n" +
                        "A modern ebook reader application for Android.\n\n" +
                        "Features:\n" +
                        "• Search and download books\n" +
                        "• Read EPUB files\n" +
                        "• Manage your library\n" +
                        "• Dark mode support\n" +
                        "• Customizable settings\n\n" +
                        "© 2024 All rights reserved";

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("About EBook Reader")
                .setMessage(aboutMessage)
                .setPositiveButton("OK", null)
                .setNeutralButton("Rate App", (dialog, which) -> {
                    Toast.makeText(getContext(), "Opening Play Store...", Toast.LENGTH_SHORT).show();
                })
                .setIcon(R.drawable.ic_menu_info_details)
                .show();
    }

    private void showPrivacyDialog() {
        String privacyMessage =
                "Privacy Policy\n\n" +
                        "We respect your privacy. This app:\n\n" +
                        "• Does not collect personal data\n" +
                        "• Does not share information with third parties\n" +
                        "• Stores books locally on your device\n" +
                        "• Does not track your reading habits\n" +
                        "• Does not use analytics services\n\n" +
                        "All data remains on your device and under your control.";

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Privacy Policy")
                .setMessage(privacyMessage)
                .setPositiveButton("OK", null)
                .setIcon(R.drawable.ic_menu_info_details)
                .show();
    }

    private void showHelpDialog() {
        String helpMessage =
                "Help & Support\n\n" +
                        "Getting Started:\n" +
                        "1. Search for books using the search feature\n" +
                        "2. Download books to your library\n" +
                        "3. Open books from 'My Books'\n" +
                        "4. Customize settings to your preference\n\n" +
                        "Features:\n" +
                        "• Dark Mode: Enable for comfortable night reading\n" +
                        "• Auto Download: Automatically save downloaded books\n" +
                        "• WiFi Only: Save mobile data\n" +
                        "• EPUB Only: Filter for EPUB format only\n\n" +
                        "Need more help? Contact support at:\n" +
                        "support@ebookreader.com";

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Help & Support")
                .setMessage(helpMessage)
                .setPositiveButton("OK", null)
                .setNeutralButton("Contact Support", (dialog, which) -> {
                    Toast.makeText(getContext(), "Opening email client...", Toast.LENGTH_SHORT).show();
                })
                .setIcon(R.drawable.ic_menu_info_details)
                .show();
    }
}