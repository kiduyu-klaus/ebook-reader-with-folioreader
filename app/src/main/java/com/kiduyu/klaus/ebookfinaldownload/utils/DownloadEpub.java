package com.kiduyu.klaus.ebookfinaldownload.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.github.lizhangqu.coreprogress.ProgressHelper;
import io.github.lizhangqu.coreprogress.ProgressUIListener;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.textview.MaterialTextView;
import com.kiduyu.klaus.ebookfinaldownload.R;


public class DownloadEpub {
    private Dialog dialog;
    MaterialTextView textViewCancel;
    private OkHttpClient client;
    private static final String CANCEL_TAG = "c_tag_epub";
    public static final int STORAGE_PERMISSION_CODE = 101;
    Activity activity;

    public DownloadEpub(Activity activity) {
        this.activity = activity;
    }

    public void checkAndRequestPermissions() {
        String[] permissions = getPermissions();

        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p : permissions) {
            if (ContextCompat.checkSelfPermission(activity, p) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }

        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(activity, listPermissionsNeeded.toArray(new String[0]), STORAGE_PERMISSION_CODE);
        }
    }

    public boolean hasAllPermissions() {
        String[] permissions = getPermissions();
        for (String p : permissions) {
            if (ContextCompat.checkSelfPermission(activity, p) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private static String[] getPermissions() {
        List<String> permissionsList = new ArrayList<>();

        // Storage permissions based on Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33+
            permissionsList.add(Manifest.permission.READ_MEDIA_IMAGES);
            permissionsList.add(Manifest.permission.READ_MEDIA_VIDEO);
            permissionsList.add(Manifest.permission.READ_MEDIA_AUDIO);
        } else { // API 32 and below
            permissionsList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) { // API 28 and below
                permissionsList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }

        // Post notifications permission for API 33+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsList.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        return permissionsList.toArray(new String[0]);
    }



    // Add your download methods here
}