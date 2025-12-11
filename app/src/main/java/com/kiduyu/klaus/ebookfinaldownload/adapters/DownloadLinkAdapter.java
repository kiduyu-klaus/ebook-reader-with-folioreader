package com.kiduyu.klaus.ebookfinaldownload.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kiduyu.klaus.ebookfinaldownload.R;
import com.kiduyu.klaus.ebookfinaldownload.ReadBook;
import com.kiduyu.klaus.ebookfinaldownload.models.DownloadLink;

import java.util.List;

public class DownloadLinkAdapter extends RecyclerView.Adapter<DownloadLinkAdapter.ViewHolder> {

    private List<DownloadLink> downloadLinks;
    private Context context;

    public DownloadLinkAdapter(Context context, List<DownloadLink> downloadLinks) {
        this.context = context;
        this.downloadLinks = downloadLinks;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_download_link, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DownloadLink link = downloadLinks.get(position);

        holder.formatBadge.setText(link.getFormat().toUpperCase());
        holder.fileName.setText(link.getFilename());
        holder.fileId.setText("ID: " + link.getId());

        // Copy button click
        holder.readButton.setOnClickListener(v -> {
            String copyText = link.getDownlink();
            Intent intent = new Intent(context, ReadBook.class);
            intent.putExtra("DOWNLOAD_URL", copyText);
            context.startActivity(intent);


            Toast.makeText(context, "Copied: " + copyText, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return downloadLinks.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView formatBadge;
        TextView fileName;
        TextView fileId;
        ImageButton readButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            formatBadge = itemView.findViewById(R.id.formatBadge);
            fileName = itemView.findViewById(R.id.fileName);
            fileId = itemView.findViewById(R.id.fileId);
            readButton = itemView.findViewById(R.id.copyButton);
        }
    }
}