package com.kiduyu.klaus.ebookfinaldownload.adapters;


import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.kiduyu.klaus.ebookfinaldownload.R;
import com.kiduyu.klaus.ebookfinaldownload.ReadBook;
import com.kiduyu.klaus.ebookfinaldownload.models.BookInfo;

import java.util.List;

public class BookAdapter extends RecyclerView.Adapter<BookAdapter.ViewHolder> {

    private List<BookInfo> books;
    private Context context;

    public BookAdapter(Context context, List<BookInfo> books) {
        this.context = context;
        this.books = books;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_book, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BookInfo book = books.get(position);

        // Set book number
        holder.bookNumber.setText(String.valueOf(position + 1));

        // Set basic info
        holder.bookTitle.setText(book.getTitle());
        holder.bookAuthor.setText(book.getAuthor());
        holder.bookLanguage.setText(book.getLanguage());

        // Handle file sizes
        boolean hasEpub = book.getEpubSize() != null && !book.getEpubSize().isEmpty();
        boolean hasPdf = book.getPdfSize() != null && !book.getPdfSize().isEmpty();

        if (hasEpub || hasPdf) {
            holder.fileSizesContainer.setVisibility(View.VISIBLE);

            if (hasEpub) {
                holder.epubSizeLayout.setVisibility(View.VISIBLE);
                holder.epubSize.setText(book.getEpubSize());
            } else {
                holder.epubSizeLayout.setVisibility(View.GONE);
            }

            if (hasPdf) {
                holder.pdfSizeLayout.setVisibility(View.VISIBLE);
                holder.pdfSize.setText(book.getPdfSize());
            } else {
                holder.pdfSizeLayout.setVisibility(View.GONE);
            }
        } else {
            holder.fileSizesContainer.setVisibility(View.GONE);
        }

        // Handle download links
        if (book.getDownloadLinks() != null && !book.getDownloadLinks().isEmpty()) {
            holder.downloadLinksHeader.setVisibility(View.VISIBLE);
            holder.downloadLinksRecycler.setVisibility(View.VISIBLE);

            DownloadLinkAdapter linkAdapter = new DownloadLinkAdapter(context, book.getDownloadLinks());
            holder.downloadLinksRecycler.setLayoutManager(new LinearLayoutManager(context));
            holder.downloadLinksRecycler.setAdapter(linkAdapter);
        } else {
            holder.downloadLinksHeader.setVisibility(View.GONE);
            holder.downloadLinksRecycler.setVisibility(View.GONE);
        }

        // View book button
        holder.viewBookButton.setOnClickListener(v -> {
            String downloadUrl = book.getDownlink();

            if (downloadUrl != null && !downloadUrl.isEmpty() &&
                    !downloadUrl.equals(book.getBookUrl())) {
                // Open with FolioReader
                Intent intent = new Intent(context, ReadBook.class);
                intent.putExtra("DOWNLOAD_URL", downloadUrl);
                intent.putExtra("BOOK_TITLE", book.getTitle());
                intent.putExtra("BOOK_AUTHOR", book.getAuthor());
                context.startActivity(intent);
            } else {
                // Fallback to browser if no download link
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(book.getBookUrl()));
                context.startActivity(browserIntent);
                Toast.makeText(context, "Opening book page in browser", Toast.LENGTH_SHORT).show();
            }
        });

        // Update button text based on download availability
        if (book.getDownlink() != null && !book.getDownlink().isEmpty() &&
                !book.getDownlink().equals(book.getBookUrl())) {
            holder.viewBookButton.setText("Read Book");
            holder.viewBookButton.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_book_open, 0, 0, 0);
        } else {
            holder.viewBookButton.setText("View on Website");
            holder.viewBookButton.setCompoundDrawablesWithIntrinsicBounds(
                    android.R.drawable.ic_menu_view, 0, 0, 0);
        }
    }


    @Override
    public int getItemCount() {
        return books.size();
    }

    public void addBook(BookInfo book) {
        books.add(book);
        notifyItemInserted(books.size() - 1);
    }

    public void clearBooks() {
        int size = books.size();
        books.clear();
        notifyItemRangeRemoved(0, size);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView bookNumber;
        TextView bookTitle;
        TextView bookAuthor;
        TextView bookLanguage;
        LinearLayout fileSizesContainer;
        LinearLayout epubSizeLayout;
        LinearLayout pdfSizeLayout;
        TextView epubSize;
        TextView pdfSize;
        TextView downloadLinksHeader;
        RecyclerView downloadLinksRecycler;
        Button viewBookButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            bookNumber = itemView.findViewById(R.id.bookNumber);
            bookTitle = itemView.findViewById(R.id.bookTitle);
            bookAuthor = itemView.findViewById(R.id.bookAuthor);
            bookLanguage = itemView.findViewById(R.id.bookLanguage);
            fileSizesContainer = itemView.findViewById(R.id.fileSizesContainer);
            epubSizeLayout = itemView.findViewById(R.id.epubSizeLayout);
            pdfSizeLayout = itemView.findViewById(R.id.pdfSizeLayout);
            epubSize = itemView.findViewById(R.id.epubSize);
            pdfSize = itemView.findViewById(R.id.pdfSize);
            downloadLinksHeader = itemView.findViewById(R.id.downloadLinksHeader);
            downloadLinksRecycler = itemView.findViewById(R.id.downloadLinksRecycler);
            viewBookButton = itemView.findViewById(R.id.viewBookButton);
        }
    }
}