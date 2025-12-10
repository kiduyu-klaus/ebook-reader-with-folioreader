package com.kiduyu.klaus.ebookfinaldownload.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kiduyu.klaus.ebookfinaldownload.R;
import com.kiduyu.klaus.ebookfinaldownload.models.BookItem;

import java.io.File;
import java.util.List;
public class BookListAdapter extends RecyclerView.Adapter<BookListAdapter.BookViewHolder> {


    private List<BookItem> bookList;
    private OnBookClickListener listener;

    public interface OnBookClickListener {
        void onBookClick(BookItem book);
        void onDeleteClick(BookItem book);
    }

    public BookListAdapter(List<BookItem> bookList, OnBookClickListener listener) {
        this.bookList = bookList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_book_item, parent, false);
        return new BookViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {
        BookItem book = bookList.get(position);
        holder.bind(book, listener);
    }

    @Override
    public int getItemCount() {
        return bookList.size();
    }

    static class BookViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivBookCover;
        private TextView tvBookTitle;
        private TextView tvBookSize;
        private TextView tvBookDate;
        private ImageButton btnDeleteBook;

        public BookViewHolder(@NonNull View itemView) {
            super(itemView);
            ivBookCover = itemView.findViewById(R.id.ivBookCover);
            tvBookTitle = itemView.findViewById(R.id.tvBookTitle);
            tvBookSize = itemView.findViewById(R.id.tvBookSize);
            tvBookDate = itemView.findViewById(R.id.tvBookDate);
            btnDeleteBook = itemView.findViewById(R.id.btnDeleteBook);
        }

        public void bind(BookItem book, OnBookClickListener listener) {
            tvBookTitle.setText(book.getTitle());
            tvBookSize.setText("Size: " + book.getSize());
            tvBookDate.setText("Date: " + book.getDate());

            // Load cover image using Glide
            if (book.getCoverImagePath() != null && !book.getCoverImagePath().isEmpty()) {
                File coverFile = new File(book.getCoverImagePath());
                if (coverFile.exists()) {
                    Glide.with(itemView.getContext())
                            .load(coverFile)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .placeholder(R.drawable.ic_launcher_background)
                            .error(R.drawable.ic_launcher_background)
                            .centerCrop()
                            .into(ivBookCover);
                } else {
                    // Fallback to default image
                    Glide.with(itemView.getContext())
                            .load(R.drawable.ic_launcher_background)
                            .into(ivBookCover);
                }
            } else {
                // No cover image, use default
                Glide.with(itemView.getContext())
                        .load(R.drawable.ic_launcher_background)
                        .into(ivBookCover);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onBookClick(book);
                }
            });

            btnDeleteBook.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClick(book);
                }
            });
        }
    }
}