package com.kiduyu.klaus.ebookfinaldownload.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.kiduyu.klaus.ebookfinaldownload.R;
import com.kiduyu.klaus.ebookfinaldownload.fragments.FavoritesFragment;
import com.kiduyu.klaus.ebookfinaldownload.models.BookItem;

import java.io.File;
import java.util.List;

public class BookListAdapter extends RecyclerView.Adapter<BookListAdapter.BookViewHolder> {

    private List<BookItem> bookList;
    private OnBookClickListener listener;
    private Context context;

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
        context = parent.getContext();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.list_book_item, parent, false);
        return new BookViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {
        BookItem book = bookList.get(position);
        holder.bind(book, listener, context);
    }

    @Override
    public int getItemCount() {
        return bookList.size();
    }

    static class BookViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivBookCover;
        private TextView tvBookTitle;
        private Chip chipSize;
        private Chip chipDate;
        private MaterialButton btnOpenBook;
        private MaterialButton btnDeleteBook;
        private MaterialButton btnFavorite;

        public BookViewHolder(@NonNull View itemView) {
            super(itemView);
            ivBookCover = itemView.findViewById(R.id.ivBookCover);
            tvBookTitle = itemView.findViewById(R.id.tvBookTitle);
            chipSize = itemView.findViewById(R.id.chipSize);
            chipDate = itemView.findViewById(R.id.chipDate);
            btnOpenBook = itemView.findViewById(R.id.btnOpenBook);
            btnDeleteBook = itemView.findViewById(R.id.btnDeleteBook);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
        }

        public void bind(BookItem book, OnBookClickListener listener, Context context) {
            tvBookTitle.setText(book.getTitle());
            chipSize.setText(book.getSize());
            chipDate.setText(book.getDate());

            // Check if book is favorite and update button
            boolean isFavorite = FavoritesFragment.isFavorite(context, book.getFilePath());
            updateFavoriteButton(isFavorite);

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
                    Glide.with(itemView.getContext())
                            .load(R.drawable.ic_launcher_background)
                            .into(ivBookCover);
                }
            } else {
                Glide.with(itemView.getContext())
                        .load(R.drawable.ic_launcher_background)
                        .into(ivBookCover);
            }

            // Open book button
            btnOpenBook.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onBookClick(book);
                }
            });

            // Delete button
            btnDeleteBook.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClick(book);
                }
            });

            // Favorite button
            btnFavorite.setOnClickListener(v -> {
                FavoritesFragment.toggleFavorite(context, book.getFilePath());
                boolean newFavoriteState = FavoritesFragment.isFavorite(context, book.getFilePath());
                updateFavoriteButton(newFavoriteState);
            });

            // Card click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onBookClick(book);
                }
            });
        }

        private void updateFavoriteButton(boolean isFavorite) {
            if (isFavorite) {
                btnFavorite.setIcon(itemView.getContext().getDrawable(R.drawable.ic_favorite));
                btnFavorite.setIconTint(android.content.res.ColorStateList.valueOf(
                        itemView.getContext().getResources().getColor(android.R.color.holo_red_dark)
                ));
            } else {
                btnFavorite.setIcon(itemView.getContext().getDrawable(R.drawable.ic_favorite_border));
                btnFavorite.setIconTint(android.content.res.ColorStateList.valueOf(
                        itemView.getContext().getResources().getColor(android.R.color.darker_gray)
                ));
            }
        }
    }
}