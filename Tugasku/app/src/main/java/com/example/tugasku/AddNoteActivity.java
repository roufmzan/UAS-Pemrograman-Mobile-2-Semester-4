package com.example.tugasku;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;
import java.util.UUID;

public class AddNoteActivity extends AppCompatActivity {

    private EditText etTitle, etContent;
    private String noteId;
    private int position;
    private boolean isEdit = false;

    private NoteManager noteManager;
    private final Handler autoSaveHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingAutoSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_note);

        etTitle = findViewById(R.id.et_note_title);
        etContent = findViewById(R.id.et_note_content);
        ImageView btnSave = findViewById(R.id.btn_save);
        ImageView btnBack = findViewById(R.id.btn_back);
        ImageView btnDelete = findViewById(R.id.btn_delete);
        TextView tvHeader = findViewById(R.id.tv_title);

        noteManager = new NoteManager(this);

        Intent intent = getIntent();
        if (intent.hasExtra("note_id")) {
            isEdit = true;
            noteId = intent.getStringExtra("note_id");
            etTitle.setText(intent.getStringExtra("note_title"));
            etContent.setText(intent.getStringExtra("note_content"));
            position = intent.getIntExtra("note_position", -1);
            tvHeader.setText("Edit Catatan");
            btnDelete.setVisibility(View.VISIBLE);
            btnDelete.setOnClickListener(v -> showDeleteDialog());
        } else {
            noteId = UUID.randomUUID().toString();
            tvHeader.setText("Tulis Catatan");
        }

        btnBack.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveNote());

        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                scheduleAutoSave();
            }
        };
        etTitle.addTextChangedListener(watcher);
        etContent.addTextChangedListener(watcher);
    }

    private void scheduleAutoSave() {
        if (pendingAutoSave != null) {
            autoSaveHandler.removeCallbacks(pendingAutoSave);
        }
        pendingAutoSave = this::autoSaveToStorage;
        autoSaveHandler.postDelayed(pendingAutoSave, 700);
    }

    private void autoSaveToStorage() {
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();

        if (title.isEmpty() && content.isEmpty()) {
            return;
        }
        if (title.isEmpty()) title = "Tanpa Judul";

        List<Note> list = noteManager.loadNotes();
        int idx = -1;
        for (int i = 0; i < list.size(); i++) {
            if (noteId.equals(list.get(i).getId())) {
                idx = i;
                break;
            }
        }

        if (idx >= 0) {
            Note note = list.get(idx);
            note.setTitle(title);
            note.setContent(content);
            note.setTimestamp(System.currentTimeMillis());
        } else {
            Note newNote = new Note(noteId, title, content, System.currentTimeMillis(), 0);
            list.add(0, newNote);
            position = -1;
        }
        noteManager.saveNotes(list);
    }

    private void saveNote() {
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();

        if (title.isEmpty() && content.isEmpty()) {
            finish();
            return;
        }

        if (title.isEmpty()) title = "Tanpa Judul";

        Intent result = new Intent();
        result.putExtra("note_id", noteId);
        result.putExtra("note_title", title);
        result.putExtra("note_content", content);
        result.putExtra("note_position", position);
        result.putExtra("action", "save");
        setResult(RESULT_OK, result);
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (pendingAutoSave != null) {
            autoSaveHandler.removeCallbacks(pendingAutoSave);
        }
        autoSaveToStorage();
    }

    private void showDeleteDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Hapus Catatan")
            .setMessage("Apakah Anda yakin ingin menghapus catatan ini?")
            .setPositiveButton("Hapus", (dialog, which) -> {
                Intent result = new Intent();
                result.putExtra("note_id", noteId);
                result.putExtra("note_position", position);
                result.putExtra("action", "delete");
                setResult(RESULT_OK, result);
                finish();
            })
            .setNegativeButton("Batal", null)
            .show();
    }
}
