package com.example.tugasku;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

public class AudioRecordActivity extends AppCompatActivity {

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private TextView tvTimer, tvStatus;
    private Button btnStopProcess, btnImportAudio, btnStartRecord;
    private WaveformView waveformView;
    
    private MediaRecorder recorder = null;
    private SpeechRecognizer speechRecognizer;
    private StringBuilder fullTranscript = new StringBuilder();
    private String lastPartialTranscript = "";
    
    private long startTime = 0L;
    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private boolean isRecording = false;
    private String audioFileName;
    private ActivityResultLauncher<Intent> importAudioLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_record);

        tvTimer = findViewById(R.id.tv_timer);
        tvStatus = findViewById(R.id.tv_status);
        btnStartRecord = findViewById(R.id.btn_start_record);
        btnStopProcess = findViewById(R.id.btn_stop_process);
        btnImportAudio = findViewById(R.id.btn_import_audio);
        waveformView = findViewById(R.id.waveform_view);
        
        audioFileName = getExternalCacheDir().getAbsolutePath() + "/audiorecord.3gp";

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        
        btnStartRecord.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
            } else {
                startRecording();
            }
        });

        btnStopProcess.setOnClickListener(v -> stopRecordingAndProcess());

        importAudioLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    android.net.Uri audioUri = result.getData().getData();
                    if (audioUri != null) {
                        Intent intent = new Intent(this, AudioResultActivity.class);
                        intent.putExtra("audioUri", audioUri.toString());
                        intent.putExtra("isImported", true);
                        startActivity(intent);
                        finish();
                    }
                }
            }
        );

        btnImportAudio.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("audio/*");
            importAudioLauncher.launch(intent);
        });
    }

    private void startRecording() {
        btnStartRecord.setVisibility(View.GONE);
        btnImportAudio.setVisibility(View.GONE);
        btnStopProcess.setVisibility(View.VISIBLE);
        waveformView.setVisibility(View.VISIBLE);
        waveformView.clear();

        fullTranscript = new StringBuilder();
        lastPartialTranscript = "";

        recorder = null;
        isRecording = true;
        startTime = System.currentTimeMillis();
        timerHandler.postDelayed(timerRunnable, 0);
        initSpeechRecognizer();
    }

    private void initSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            return;
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID");
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {}
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {
                if (isRecording) {
                    // rmsdB biasanya range -2 sampai 10+, kita kali biar lebih terlihat
                    waveformView.addAmplitude(rmsdB * 10f);
                }
            }
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onError(int error) {
                if (isRecording) speechRecognizer.startListening(intent);
            }
            @Override public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    fullTranscript.append(matches.get(0)).append(" ");
                }
                if (isRecording) speechRecognizer.startListening(intent);
            }
            @Override public void onPartialResults(Bundle partialResults) {
                ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    lastPartialTranscript = matches.get(0);
                }
            }
            @Override public void onEvent(int eventType, Bundle params) {}
        });
        speechRecognizer.startListening(intent);
    }

    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            long millis = System.currentTimeMillis() - startTime;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            int hours = minutes / 60;
            seconds = seconds % 60;
            minutes = minutes % 60;

            tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds));
            timerHandler.postDelayed(this, 500);
        }
    };

    private void stopRecordingAndProcess() {
        isRecording = false;
        timerHandler.removeCallbacks(timerRunnable);
        
        if (recorder != null) {
            recorder.stop();
            recorder.release();
            recorder = null;
        }

        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
            speechRecognizer.cancel();
            speechRecognizer.destroy();
        }

        String transcriptToSend = fullTranscript.toString().trim();
        if (transcriptToSend.isEmpty() && lastPartialTranscript != null && !lastPartialTranscript.trim().isEmpty()) {
            transcriptToSend = lastPartialTranscript.trim();
        }

        Intent intent = new Intent(this, AudioResultActivity.class);
        intent.putExtra("transcript", transcriptToSend);
        intent.putExtra("audioPath", audioFileName);
        startActivity(intent);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startRecording();
            } else {
                Toast.makeText(this, "Izin mik diperlukan", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}
