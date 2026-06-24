package com.example.tugasku;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import java.util.UUID;

public class AudioResultActivity extends AppCompatActivity {

    private EditText etTitle;
    private TextView tvResultContent, tabSummary, tabTranscript;
    private String transcript, summary, audioPath;
    private boolean showingSummary = true;
    private boolean summaryReadyToSave = false;
    private View btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_result);

        etTitle = findViewById(R.id.et_audio_note_title);
        tvResultContent = findViewById(R.id.tv_result_content);
        tabSummary = findViewById(R.id.tab_summary);
        tabTranscript = findViewById(R.id.tab_transcript);
        btnSave = findViewById(R.id.btn_save_audio_note);

        transcript = getIntent().getStringExtra("transcript");
        audioPath = getIntent().getStringExtra("audioPath");
        boolean isImported = getIntent().getBooleanExtra("isImported", false);
        String audioUri = getIntent().getStringExtra("audioUri");

        if (isImported && audioUri != null) {
            audioPath = audioUri;
            etTitle.setText("[Impor Audio - " + new java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault()).format(new java.util.Date()) + "] 🖉");
            // Di dunia nyata, di sini kita akan mengirim file ke API STT.
            // Untuk prototype, kita beri teks simulasi.
            transcript = "";
        }

        if (transcript == null || transcript.trim().isEmpty()) {
            summary = "• Transkrip belum tersedia. Rekam suara langsung (STT) atau tempelkan teks transkrip terlebih dahulu agar bisa diringkas.";
            summaryReadyToSave = true;
            if (btnSave != null) btnSave.setEnabled(true);
            updateUI();
        } else {
            summary = "Memproses ringkasan AI...";
            summaryReadyToSave = false;
            if (btnSave != null) btnSave.setEnabled(false);
            updateUI();
            enqueueGeminiSummaryWorker(transcript);
        }

        tabSummary.setOnClickListener(v -> {
            showingSummary = true;
            updateUI();
        });

        tabTranscript.setOnClickListener(v -> {
            showingSummary = false;
            updateUI();
        });

        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                if (!summaryReadyToSave) {
                    Toast.makeText(this, "Tunggu sampai ringkasan AI selesai dibuat.", Toast.LENGTH_SHORT).show();
                    return;
                }
                saveAudioNote();
            });
        }
    }

    private void enqueueGeminiSummaryWorker(String transcript) {
        Data input = new Data.Builder()
                .putString(GeminiSummarizeWorker.KEY_INPUT_TRANSCRIPT, transcript)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(GeminiSummarizeWorker.class)
                .setInputData(input)
                .build();

        WorkManager.getInstance(getApplicationContext()).enqueue(request);

        WorkManager.getInstance(getApplicationContext())
                .getWorkInfoByIdLiveData(request.getId())
                .observe(this, workInfo -> {
                    if (workInfo == null) return;

                    if (workInfo.getState() == WorkInfo.State.RUNNING) {
                        int progress = workInfo.getProgress().getInt(GeminiSummarizeWorker.KEY_PROGRESS, -1);
                        if (progress >= 0) {
                            summary = "Memproses ringkasan AI... (" + progress + "%)";
                            updateUI();
                        }
                        return;
                    }

                    if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                        String out = workInfo.getOutputData().getString(GeminiSummarizeWorker.KEY_OUTPUT_SUMMARY);
                        if (out != null && !out.trim().isEmpty()) {
                            summary = out;
                        } else {
                            summary = "Ringkasan kosong.";
                        }
                        summaryReadyToSave = true;
                        if (btnSave != null) btnSave.setEnabled(true);
                        updateUI();
                        return;
                    }

                    if (workInfo.getState() == WorkInfo.State.FAILED) {
                        String err = workInfo.getOutputData().getString(GeminiSummarizeWorker.KEY_OUTPUT_ERROR);
                        summary = "Gagal memproses AI" + (err != null ? (": " + err) : ".");
                        summaryReadyToSave = true;
                        if (btnSave != null) btnSave.setEnabled(true);
                        updateUI();
                    }
                });
    }

    private void updateUI() {
        if (showingSummary) {
            tvResultContent.setText(summary);
            tabSummary.setTextColor(0xFFFFD700); // Gold
            tabTranscript.setTextColor(0x88FFFFFF); // Muted
        } else {
            tvResultContent.setText(transcript != null && !transcript.isEmpty() ? transcript : "Transkrip kosong.");
            tabTranscript.setTextColor(0xFFFFD700);
            tabSummary.setTextColor(0x88FFFFFF);
        }
    }

    private void saveAudioNote() {
        String title = etTitle.getText().toString().trim();
        if (title.isEmpty()) title = "Tanpa Judul";
        AudioNote newNote = new AudioNote(
                UUID.randomUUID().toString(),
                title,
                transcript,
                summary,
                System.currentTimeMillis(),
                audioPath
        );

        AudioNoteManager manager = new AudioNoteManager(this);
        java.util.List<AudioNote> list = manager.loadAudioNotes();
        list.add(0, newNote);
        manager.saveAudioNotes(list);

        NoteManager noteManager = new NoteManager(this);
        java.util.List<Note> notes = noteManager.loadNotes();
        StringBuilder content = new StringBuilder();
        content.append("Ringkasan Penting:\n");
        content.append(summary != null ? summary : "");
        content.append("\n\nTranskrip Penuh:\n");
        content.append(transcript != null ? transcript : "");
        notes.add(0, new Note(UUID.randomUUID().toString(), title, content.toString(), System.currentTimeMillis(), 0));
        noteManager.saveNotes(notes);

        Toast.makeText(this, "Catatan AI berhasil disimpan!", Toast.LENGTH_SHORT).show();
        finish();
    }
}
