package com.kiduyu.klaus.ebookfinaldownload.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

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
import com.kiduyu.klaus.ebookfinaldownload.repository.BookRepository;
import com.kiduyu.klaus.ebookfinaldownload.ReadBook;
import com.kiduyu.klaus.ebookfinaldownload.adapters.BookListAdapter;
import com.kiduyu.klaus.ebookfinaldownload.models.BookItem;
import com.kiduyu.klaus.ebookfinaldownload.utils.EpubCoverExtractor;
import androidx.core.content.FileProvider;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MyBooksFragment extends Fragment {

    private RecyclerView recyclerView;
    private List<BookItem> bookList;
    private LinearLayout emptyStateLayout;
    private TextView tvBookCount;
    private MaterialButton btnAddFirst;
    private FloatingActionButton fabSearch;
    BookListAdapter bookAdapter;
    private BookRepository bookRepository;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_books, container, false);

        initializeViews(view);
        setupRecyclerView();
        setupListeners();

        bookRepository = BookRepository.getInstance(requireContext());

        loadBooksFromDatabase();
        syncLocalBooks(); // Start background sync of local files

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadBooksFromDatabase();
    }

    private void initializeViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerViewBooks);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        tvBookCount = view.findViewById(R.id.tvBookCount);
        btnAddFirst = view.findViewById(R.id.btnAddFirst);
        fabSearch = view.findViewById(R.id.fab_search);
    }

    private void setupRecyclerView() {
        bookList = new ArrayList<>();
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

    private void loadBooksFromDatabase() {
        if (getContext() == null) return;

        showLoadingState();

        bookRepository.executeAsync(() -> {
            List<BookItem> books = bookRepository.getAllMyBooks();
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    bookList.clear();
                    bookList.addAll(books);
                    if (books.isEmpty()) {
                        showEmptyState();
                    } else {
                        showBooksState();
                    }
                    updateBookCount(books.size());
                });
            }
            return null;
        });
    }

    private void syncLocalBooks() {
        if (getContext() == null) return;

        bookRepository.executeAsync(() -> {
            File booksDir = getContext().getExternalFilesDir(null);
            if (booksDir != null && booksDir.exists()) {
                File[] files = booksDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".epub"));

                if (files != null) {
                    List<BookItem> booksFromFiles = new ArrayList<>();
                    for (File file : files) {
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

                        booksFromFiles.add(bookItem);
                    }

                    // Save all found books to the database (will replace existing ones)
                    for (BookItem book : booksFromFiles) {
                        bookRepository.saveMyBook(book);
                    }

                    // Check for books in DB that no longer exist on the file system and delete them
                    List<BookItem> booksInDb = bookRepository.getAllMyBooks();
                    for (BookItem dbBook : booksInDb) {
                        File file = new File(dbBook.getFilePath());
                        if (!file.exists()) {
                            bookRepository.deleteMyBook(dbBook.getFilePath());
                        }
                    }

                    // Reload from database after sync
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(this::loadBooksFromDatabase);
                    }
                }
            }
            return null;
        });
    }

    private void showLoadingState() {
        recyclerView.setVisibility(View.VISIBLE);
        emptyStateLayout.setVisibility(View.GONE);
        tvBookCount.setText("Loading...");
    }

    private void showBooksState() {
        recyclerView.setVisibility(View.VISIBLE);
        emptyStateLayout.setVisibility(View.GONE);

        bookAdapter = new BookListAdapter(bookList, new BookListAdapter.OnBookClickListener() {
            @Override
            public void onBookClick(BookItem book) {
                openBook(book);
            }

            @Override
            public void onDeleteClick(BookItem book) {
                showDeleteConfirmation(book);
            }

            @Override
            public void onShareClick(BookItem book) {
                shareBook(book);
            }
        });
        recyclerView.setAdapter(bookAdapter);
    }

    private void showEmptyState() {
        recyclerView.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.VISIBLE);
        tvBookCount.setText("0 books");
    }

    private void updateBookCount(int count) {
        if (count == 0) {
            tvBookCount.setText("0 books");
        } else if (count == 1) {
            tvBookCount.setText("1 book");
        } else {
            tvBookCount.setText(count + " books");
        }
    }

    private void openBook(BookItem book) {
        Intent intent = new Intent(getActivity(), ReadBook.class);
        intent.putExtra("EPUB_PATH", book.getFilePath());
        intent.putExtra("BOOK_TITLE", book.getTitle());
        startActivity(intent);
    }

	    private void shareBook(BookItem book) {
	        File file = new File(book.getFilePath());
	        if (!file.exists()) {
	            Toast.makeText(getContext(), "File not found", Toast.LENGTH_SHORT).show();
	            return;
	        }

	        try {
	            Intent shareIntent = new Intent(Intent.ACTION_SEND);
	            shareIntent.setType("application/epub+zip");
	            shareIntent.putExtra(Intent.EXTRA_STREAM, androidx.core.content.FileProvider.getUriForFile(
	                    requireContext(),
	                    requireContext().getApplicationContext().getPackageName() + ".provider",
	                    file));
	            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
	            startActivity(Intent.createChooser(shareIntent, "Share " + book.getTitle()));
	        } catch (Exception e) {
	            Toast.makeText(getContext(), "Error sharing file: " + e.getMessage(), Toast.LENGTH_LONG).show();
	            e.printStackTrace();
	        }
	    }

	    private void showDeleteConfirmation(BookItem book) {
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Book")
                .setMessage("Are you sure you want to delete '" + book.getTitle() + "'?\n\nThis action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteBook(book))
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void deleteBook(BookItem book) {
        File file = new File(book.getFilePath());
        boolean deleted = false;

        if (file.exists()) {
            deleted = file.delete();

            if (book.getCoverImagePath() != null && !book.getCoverImagePath().isEmpty()) {
                File coverFile = new File(book.getCoverImagePath());
                if (coverFile.exists()) {
                    coverFile.delete();
                }
            }
        }

	        if (deleted) {
	            // Also delete from database
	            bookRepository.deleteMyBook(book.getFilePath());
	            Toast.makeText(getContext(), "Book deleted", Toast.LENGTH_SHORT).show();
	            loadBooksFromDatabase();
	        } else {
	            Toast.makeText(getContext(), "Failed to delete book", Toast.LENGTH_SHORT).show();
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

    public void updateFileList(List<BookItem> newFiles) {
        if (newFiles == null || newFiles.isEmpty()) {
            showEmptyState();
            return;
        }

        // Show RecyclerView if it was hidden
        recyclerView.setVisibility(View.VISIBLE);
        emptyStateLayout.setVisibility(View.GONE);

        bookList.clear();

        for (BookItem book : newFiles) {
            try {
                long timestamp = Long.parseLong(book.getDate());
                String formatted = formatDate(timestamp);
                book.setDate(formatted);
            } catch (Exception e) {
                // if parsing fails, keep original
                book.setDate(book.getDate());
            }

            bookList.add(book);
        }

        bookAdapter.notifyDataSetChanged();
    }

    // This method is no longer needed as the fragment now loads directly from the database
	    // and the sync logic handles file system changes.
	    /*
	    public void updateFileList(List<BookItem> newFiles) {
	
	        if (newFiles == null || newFiles.isEmpty()) {
	            showEmptyState();
	            return;
	        }
	
	        // Show RecyclerView if it was hidden
	        recyclerView.setVisibility(View.VISIBLE);
	        emptyStateLayout.setVisibility(View.GONE);
	
	        bookList.clear();
	
	        for (BookItem book : newFiles) {
	            try {
	                long timestamp = Long.parseLong(book.getDate());
	                String formatted = formatDate(timestamp);
	                book.setDate(formatted);
	            } catch (Exception e) {
	                // if parsing fails, keep original
	                book.setDate(book.getDate());
	            }
	
	            bookList.add(book);
	        }
	
	        bookAdapter.notifyDataSetChanged();
	    }
	    */



}