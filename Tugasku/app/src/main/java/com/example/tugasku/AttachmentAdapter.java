package com.example.tugasku;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AttachmentAdapter extends RecyclerView.Adapter<AttachmentAdapter.AttachmentViewHolder> {

    private final List<Attachment> attachments;
    private final Context context;

    public AttachmentAdapter(Context context, List<Attachment> attachments) {
        this.context = context;
        this.attachments = attachments;
    }

    @NonNull
    @Override
    public AttachmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_attachment, parent, false);
        return new AttachmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AttachmentViewHolder holder, int position) {
        Attachment a = attachments.get(position);
        holder.tvName.setText(a.getName() != null && !a.getName().isEmpty() ? a.getName() : a.getUri());

        if (Attachment.TYPE_LINK.equals(a.getType())) {
            holder.ivIcon.setImageResource(android.R.drawable.ic_menu_share);
        } else if (a.getMimeType() != null && a.getMimeType().contains("pdf")) {
            holder.ivIcon.setImageResource(android.R.drawable.ic_menu_agenda);
        } else {
            holder.ivIcon.setImageResource(android.R.drawable.ic_menu_view);
        }

        View.OnClickListener openListener = v -> {
            try {
                Intent chooser;
                if (Attachment.TYPE_LINK.equals(a.getType())) {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(a.getUri()));
                    chooser = Intent.createChooser(i, "Buka tautan dengan...");
                } else {
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setDataAndType(Uri.parse(a.getUri()), a.getMimeType() != null ? a.getMimeType() : "application/*");
                    i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    chooser = Intent.createChooser(i, "Buka lampiran dengan...");
                }
                context.startActivity(chooser);
            } catch (Exception e) {
                // silently ignore to avoid crash; optionally could show a toast if context permits
            }
        };

        holder.itemView.setOnClickListener(openListener);
        holder.ivOpen.setOnClickListener(openListener);
    }

    @Override
    public int getItemCount() {
        return attachments.size();
    }

    static class AttachmentViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon, ivOpen;
        TextView tvName;
        AttachmentViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_icon);
            ivOpen = itemView.findViewById(R.id.iv_open);
            tvName = itemView.findViewById(R.id.tv_name);
        }
    }
}
