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
import com.google.android.material.card.MaterialCardView;
import com.kiduyu.klaus.ebookfinaldownload.R;
import com.kiduyu.klaus.ebookfinaldownload.models.Listopia;

import java.util.List;

public class ListopiaAdapter extends RecyclerView.Adapter<ListopiaAdapter.ViewHolder> {

    private List<Listopia> listopiaList;
    private Context context;
    private OnListopiaClickListener listener;

    public interface OnListopiaClickListener {
        void onListopiaClick(Listopia listopia);
    }

    public ListopiaAdapter(Context context, List<Listopia> listopiaList, OnListopiaClickListener listener) {
        this.context = context;
        this.listopiaList = listopiaList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_listopia, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Listopia listopia = listopiaList.get(position);

        holder.categoryTitle.setText(listopia.getTitle());
        holder.bookCount.setText(listopia.getBookCount() + " books");

        // Load thumbnail image
        if (listopia.getThumbnailUrl() != null && !listopia.getThumbnailUrl().isEmpty()) {
            Glide.with(context)
                    .load(listopia.getThumbnailUrl())
                    .placeholder(R.drawable.ic_book_placeholder)
                    .error(R.drawable.ic_book_placeholder)
                    .centerCrop()
                    .into(holder.thumbnailImage);
        } else {
            holder.thumbnailImage.setImageResource(R.drawable.ic_book_placeholder);
        }

        // Click listener
        holder.cardView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onListopiaClick(listopia);
            }
        });
    }

    @Override
    public int getItemCount() {
        return listopiaList.size();
    }

    public void addListopia(Listopia listopia) {
        listopiaList.add(listopia);
        notifyItemInserted(listopiaList.size() - 1);
    }

    public void clearListopia() {
        int size = listopiaList.size();
        listopiaList.clear();
        notifyItemRangeRemoved(0, size);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        ImageView thumbnailImage;
        TextView categoryTitle;
        TextView bookCount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.listopiaCard);
            thumbnailImage = itemView.findViewById(R.id.thumbnailImage);
            categoryTitle = itemView.findViewById(R.id.categoryTitle);
            bookCount = itemView.findViewById(R.id.bookCount);
        }
    }
}