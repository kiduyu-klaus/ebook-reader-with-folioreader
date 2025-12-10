package com.kiduyu.klaus.ebookfinaldownload.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import androidx.appcompat.app.AppCompatActivity;

public class FileAccessPermissionHelper {

    private Context context;
    private static final int REQUEST_CODE_MANAGE_STORAGE = 1001;

    public FileAccessPermissionHelper(Context context) {
        this.context = context;
    }

    /**
     * Check if the app has "Manage All Files" permission
     */
    public boolean hasManageAllFilesPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }
        return true; // Android 10 and below use regular storage permissions
    }

    /**
     * Show dialog to request "Manage All Files" permission
     */
    public void requestManageAllFilesPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                showPermissionDialog();
            }
        }
    }

    /**
     * Show custom dialog explaining the permission requirement
     */
    private void showPermissionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Allow Access to Manage All Files");
        builder.setMessage("To access the files on your device, please manually grant \"Allow access to manage all files\" to this app.\n\n" +
                "Press OK to enter the config interface, select this app and activate the permission.");

        builder.setPositiveButton("OK", (dialog, which) -> {
            openManageAllFilesSettings();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.dismiss();
            // Optionally handle cancellation
        });

        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Open the system settings page for "Manage All Files" permission
     */
    private void openManageAllFilesSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                // Try to open the specific app settings page
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", context.getPackageName(), null);
                intent.setData(uri);

                if (context instanceof AppCompatActivity) {
                    ((AppCompatActivity) context).startActivityForResult(intent, REQUEST_CODE_MANAGE_STORAGE);
                } else {
                    context.startActivity(intent);
                }
            } catch (Exception e) {
                // Fallback to general manage all files settings
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    context.startActivity(intent);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    /**
     * Handle the result from the settings activity
     * Call this from your Activity's onActivityResult
     */
    public void onActivityResult(int requestCode, int resultCode) {
        if (requestCode == REQUEST_CODE_MANAGE_STORAGE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    // Permission granted
                    onPermissionGranted();
                } else {
                    // Permission denied
                    onPermissionDenied();
                }
            }
        }
    }

    /**
     * Override this method to handle when permission is granted
     */
    protected void onPermissionGranted() {
        // Implement your logic here
        // For example: proceed with file operations
    }

    /**
     * Override this method to handle when permission is denied
     */
    protected void onPermissionDenied() {
        // Implement your logic here
        // For example: show error message or disable features
    }
}