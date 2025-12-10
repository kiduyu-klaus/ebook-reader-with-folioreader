package com.kiduyu.klaus.ebookfinaldownload;


import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.kiduyu.klaus.ebookfinaldownload.adapters.BookAdapter;
import com.kiduyu.klaus.ebookfinaldownload.adapters.BookListAdapter;
import com.kiduyu.klaus.ebookfinaldownload.models.BookItem;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BookListActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private List<BookItem> bookList;
    private TextView tvEmptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_list);

        recyclerView = findViewById(R.id.recyclerViewBooks);
        tvEmptyState = findViewById(R.id.tvEmptyState);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadBooks();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBooks(); // Refresh the list when returning to this activity
    }

    private void loadBooks() {
        bookList = new ArrayList<>();
        File booksDir = getExternalFilesDir(null);

        if (booksDir != null && booksDir.exists()) {
            File[] files = booksDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".epub"));

            if (files != null && files.length > 0) {
                for (File file : files) {
                    BookItem bookItem = new BookItem();
                    bookItem.setFilePath(file.getAbsolutePath());
                    bookItem.setTitle(file.getName().replace(".epub", ""));
                    bookItem.setSize(formatFileSize(file.length()));
                    bookItem.setDate(formatDate(file.lastModified()));
                    bookList.add(bookItem);
                }

                // Show RecyclerView, hide empty state
                recyclerView.setVisibility(View.VISIBLE);
                tvEmptyState.setVisibility(View.GONE);

                setupAdapter();
            } else {
                // Show empty state
                recyclerView.setVisibility(View.GONE);
                tvEmptyState.setVisibility(View.VISIBLE);
            }
        } else {
            // Show empty state
            recyclerView.setVisibility(View.GONE);
            tvEmptyState.setVisibility(View.VISIBLE);
        }
    }

    private void setupAdapter() {
        BookListAdapter bookAdapter = new BookListAdapter(bookList, new BookListAdapter.OnBookClickListener() {
            @Override
            public void onBookClick(BookItem book) {
                openBook(book);
            }

            @Override
            public void onDeleteClick(BookItem book) {
                showDeleteConfirmation(book);
            }
        });
        recyclerView.setAdapter(bookAdapter);
    }

    private void openBook(BookItem book) {
        Intent intent = new Intent(this, ReadBook.class);
        intent.putExtra("EPUB_PATH", book.getFilePath());
        intent.putExtra("BOOK_TITLE", book.getTitle());
        startActivity(intent);
    }

    private void showDeleteConfirmation(BookItem book) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Book")
                .setMessage("Are you sure you want to delete '" + book.getTitle() + "'?")
                .setPositiveButton("Delete", (dialog, which) -> deleteBook(book))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteBook(BookItem book) {
        File file = new File(book.getFilePath());
        if (file.exists() && file.delete()) {
            Toast.makeText(this, "Book deleted", Toast.LENGTH_SHORT).show();
            loadBooks(); // Refresh the list
        } else {
            Toast.makeText(this, "Failed to delete book", Toast.LENGTH_SHORT).show();
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