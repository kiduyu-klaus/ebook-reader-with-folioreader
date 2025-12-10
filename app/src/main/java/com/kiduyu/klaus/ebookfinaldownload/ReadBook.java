package com.kiduyu.klaus.ebookfinaldownload;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.folioreader.Config;
import com.folioreader.FolioReader;
import com.folioreader.model.HighLight;
import com.folioreader.model.locators.ReadLocator;
import com.folioreader.util.AppUtil;
import com.folioreader.util.OnHighlightListener;
import com.folioreader.util.ReadLocatorListener;
import com.kiduyu.klaus.ebookfinaldownload.utils.ProgressDialog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ReadBook extends AppCompatActivity implements OnHighlightListener, ReadLocatorListener, FolioReader.OnClosedListener {
    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private FolioReader folioReader;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_read_book);

        folioReader = FolioReader.get()
                .setOnHighlightListener(this)
                .setReadLocatorListener(this)
                .setOnClosedListener(this);

//storage/emulated/0/Android/data/com.kiduyu.klaus.ebookfinaldownload/files

        String downloadUrl = getIntent().getStringExtra("DOWNLOAD_URL");
        String bookTitle = getIntent().getStringExtra("BOOK_TITLE");
        String bookAuthor = getIntent().getStringExtra("BOOK_AUTHOR");

        // Show progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.show("Downloading book...");

        // Download and open the book
        downloadAndOpenBook(downloadUrl, bookTitle);
    }

    private void downloadAndOpenBook(String url, String bookTitle) {
        new Thread(() -> {
            try {
                File epubFile = downloadEpubFile(url, bookTitle);

                runOnUiThread(() -> {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }

                    if (epubFile != null && epubFile.exists()) {
                        openBook(epubFile.getAbsolutePath());
                    } else {
                        Toast.makeText(this, "Failed to download book", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });

            } catch (Exception e) {
                Log.e(LOG_TAG, "Error downloading book", e);
                runOnUiThread(() -> {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        }).start();
    }

    private void openBook(String epubPath) {
        try {
            // Verify file exists and is readable
            File epubFile = new File(epubPath);
            if (!epubFile.exists()) {
                Toast.makeText(this, "Book file not found", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            if (!epubFile.canRead()) {
                Toast.makeText(this, "Cannot read book file. Check permissions.", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }


//            // Configure FolioReader
//            Config config = AppUtil.getSavedConfig(getApplicationContext());
//            if (config == null) {
//                config = new Config();
//            }
//
//            config.setAllowedDirection(Config.AllowedDirection.VERTICAL_AND_HORIZONTAL)
//                    .setThemeColorInt(getResources().getColor(R.color.purple_500))
//                    .setShowTts(true);
//
//            folioReader.setConfig(config, true);

            // Open the EPUB file using the actual file path
            Log.d(LOG_TAG, "Opening book: " + epubPath);
            Config config = AppUtil.getSavedConfig(getApplicationContext());
            if (config == null)
                config = new Config();
            config.setAllowedDirection(Config.AllowedDirection.VERTICAL_AND_HORIZONTAL);
            folioReader.setConfig(config, true)
                    .openBook(epubPath);  // Use the downloaded file path, not R.raw resource


        } catch (Exception e) {
            Log.e(LOG_TAG, "Error opening book", e);
            Toast.makeText(this, "Error opening book: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private File downloadEpubFile(String url, String bookTitle) throws IOException {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = client.newCall(request).execute();

        if (!response.isSuccessful()) {
            throw new IOException("Failed to download file: " + response);
        }

        File booksDir = getExternalFilesDir(null);
        if (booksDir != null && !booksDir.exists()) {
            booksDir.mkdirs();
        }

        // Create file with safe filename
        String safeTitle = bookTitle != null ?
                bookTitle.replaceAll("[^a-zA-Z0-9.-]", "_") : "book_" + System.currentTimeMillis();
        File epubFile = new File(booksDir, safeTitle + ".epub");

        // Download the file
        try (InputStream inputStream = response.body().byteStream();
             FileOutputStream outputStream = new FileOutputStream(epubFile)) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            long totalBytes = 0;
            long fileSize = response.body().contentLength();

            runOnUiThread(() -> {
                if (progressDialog != null) {
                    progressDialog.show("Downloading... 0%");
                }
            });

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;

                if (fileSize > 0) {
                    final int progress = (int) ((totalBytes * 100) / fileSize);
                    runOnUiThread(() -> {
                        if (progressDialog != null) {
                            progressDialog.show("Downloading... " + progress + "%");
                        }
                    });
                }
            }
        }

        // Set file permissions to readable and writable
        epubFile.setReadable(true, false);  // readable by all
        epubFile.setWritable(true, false);  // writable by all

        return epubFile;
    }
    @Override
    public void onFolioReaderClosed() {

    }

    @Override
    public void onHighlight(HighLight highlight, HighLight.HighLightAction type) {

    }

    @Override
    public void saveReadLocator(ReadLocator readLocator) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        //FolioReader.clear();
    }
}