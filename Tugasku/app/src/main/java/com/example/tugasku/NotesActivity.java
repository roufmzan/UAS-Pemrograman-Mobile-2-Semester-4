package com.example.tugasku;

import android.content.Intent;
import android.os.Bundle;
import android.graphics.Color;
import android.widget.EditText;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.List;

public class NotesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private NoteAdapter adapter;
    private List<Note> noteList;
    private NoteManager noteManager;
    private ActivityResultLauncher<Intent> addNoteLauncher;
    private ActivityResultLauncher<Intent> importAudioLauncher;

    private int findIndexById(List<Note> list, String id) {
        if (list == null || id == null) return -1;
        for (int i = 0; i < list.size(); i++) {
            Note n = list.get(i);
            if (n != null && id.equals(n.getId())) return i;
        }
        return -1;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);

        noteManager = new NoteManager(this);
        noteList = noteManager.loadNotes();

        recyclerView = findViewById(R.id.rv_notes);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        adapter = new NoteAdapter(this, noteList, (note, position) -> {
            Intent intent = new Intent(NotesActivity.this, AddNoteActivity.class);
            intent.putExtra("note_id", note.getId());
            intent.putExtra("note_title", note.getTitle());
            intent.putExtra("note_content", note.getContent());
            intent.putExtra("note_position", position);
            addNoteLauncher.launch(intent);
        });
        recyclerView.setAdapter(adapter);

        SearchView searchView = findViewById(R.id.search_notes);
        if (searchView != null) {
            // Set text color and hint color programmatically to avoid XML attribute errors
            EditText searchEditText = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
            if (searchEditText != null) {
                searchEditText.setTextColor(Color.BLACK);
                searchEditText.setHintTextColor(Color.GRAY);
            }

            searchView.setOnClickListener(v -> searchView.requestFocusFromTouch());
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    adapter.filter(query);
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    adapter.filter(newText);
                    return true;
                }
            });
        }

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        addNoteLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    String action = data.getStringExtra("action");
                    int position = data.getIntExtra("note_position", -1);

                    if ("delete".equals(action)) {
                        String id = data.getStringExtra("note_id");
                        int idx = findIndexById(noteList, id);
                        if (idx != -1) {
                            noteList.remove(idx);
                            adapter.replaceAll(noteList);
                        }
                    } else {
                        String id = data.getStringExtra("note_id");
                        String title = data.getStringExtra("note_title");
                        String content = data.getStringExtra("note_content");

                        int idx = findIndexById(noteList, id);
                        if (idx != -1) {
                            Note note = noteList.get(idx);
                            note.setTitle(title);
                            note.setContent(content);
                            note.setTimestamp(System.currentTimeMillis());
                        } else {
                            Note newNote = new Note(id, title, content, System.currentTimeMillis(), 0);
                            noteList.add(0, newNote);
                        }

                        adapter.replaceAll(noteList);
                        recyclerView.scrollToPosition(0);
                    }
                    noteManager.saveNotes(noteList);
                }
            }
        );

        FloatingActionButton fab = findViewById(R.id.fab_add_note);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(NotesActivity.this, AddNoteActivity.class);
            addNoteLauncher.launch(intent);
        });

        FloatingActionButton fabRecord = findViewById(R.id.fab_record_audio);
        fabRecord.setOnClickListener(v -> {
            Intent intent = new Intent(NotesActivity.this, AudioRecordActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (noteManager == null) return;
        List<Note> latest = noteManager.loadNotes();
        if (latest == null) return;
        
        // Pastikan noteList lokal diperbarui agar sinkron dengan adapter
        noteList.clear();
        noteList.addAll(latest);
        
        if (adapter != null) {
            adapter.replaceAll(noteList);
        }
    }
}
