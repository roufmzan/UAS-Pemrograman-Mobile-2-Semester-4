package com.example.tugasku;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    private final List<Note> allNotes;
    private final List<Note> notes;
    private final Context context;
    private final OnNoteClickListener listener;

    public interface OnNoteClickListener {
        void onNoteClick(Note note, int position);
    }

    public NoteAdapter(Context context, List<Note> notes, OnNoteClickListener listener) {
        this.context = context;
        List<Note> initial = notes != null ? new ArrayList<>(notes) : new ArrayList<>();
        this.allNotes = new ArrayList<>(initial);
        this.notes = new ArrayList<>(initial);
        this.listener = listener;
    }

    public void replaceAll(List<Note> newNotes) {
        List<Note> copy = newNotes != null ? new ArrayList<>(newNotes) : new ArrayList<>();
        notes.clear();
        notes.addAll(copy);
        allNotes.clear();
        allNotes.addAll(copy);
        notifyDataSetChanged();
    }

    public void filter(String query) {
        String q = query != null ? query.trim().toLowerCase(Locale.ROOT) : "";
        notes.clear();
        if (q.isEmpty()) {
            notes.addAll(allNotes);
            notifyDataSetChanged();
            return;
        }

        for (Note n : allNotes) {
            String title = n.getTitle() != null ? n.getTitle().toLowerCase(Locale.ROOT) : "";
            String content = n.getContent() != null ? n.getContent().toLowerCase(Locale.ROOT) : "";
            if (title.contains(q) || content.contains(q)) {
                notes.add(n);
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = notes.get(position);
        holder.tvTitle.setText(note.getTitle());
        holder.tvPreview.setText(note.getContent());
        
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
        holder.tvDate.setText(sdf.format(new Date(note.getTimestamp())));

        holder.itemView.setOnClickListener(v -> listener.onNoteClick(note, position));
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvPreview, tvDate;
        NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_note_title);
            tvPreview = itemView.findViewById(R.id.tv_note_preview);
            tvDate = itemView.findViewById(R.id.tv_note_date);
        }
    }
}
