package com.example.tugasku;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;

public class PreviewTaskActivity extends AppCompatActivity {

    private TextView tvTitle, tvCourse, tvDeadline, tvDescription;
    private ImageView btnBack, ivPreviewImage;
    private Button btnEdit, btnDelete, btnMarkComplete;
    private LinearLayout attachmentsContainer;

    private int taskPosition;
    private String title, course, deadline, description, imagePath, attsJson;
    private int color;
    private boolean completed;

    private ActivityResultLauncher<Intent> editLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview_task);

        editLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    // Pass result back to MainActivity and close preview
                    setResult(RESULT_OK, result.getData());
                    finish();
                }
            }
        );

        tvTitle = findViewById(R.id.tv_title);
        tvCourse = findViewById(R.id.tv_course);
        tvDeadline = findViewById(R.id.tv_deadline);
        tvDescription = findViewById(R.id.tv_description);
        btnBack = findViewById(R.id.btn_back);
        btnEdit = findViewById(R.id.btn_edit_task);
        btnDelete = findViewById(R.id.btn_delete_task);
        btnMarkComplete = findViewById(R.id.btn_mark_complete);
        ivPreviewImage = findViewById(R.id.iv_preview_image);
        attachmentsContainer = findViewById(R.id.attachments_container);

        Intent intent = getIntent();
        taskPosition = intent.getIntExtra("task_position", -1);
        title = intent.getStringExtra("task_title");
        course = intent.getStringExtra("task_course");
        deadline = intent.getStringExtra("task_deadline");
        description = intent.getStringExtra("task_description");
        imagePath = intent.getStringExtra("task_image_path");
        color = intent.getIntExtra("task_color", 0);
        completed = intent.getBooleanExtra("task_completed", false);
        attsJson = intent.getStringExtra("task_attachments");

        tvTitle.setText(title);
        tvCourse.setText("Mata Kuliah: " + course);
        tvDeadline.setText("Tenggat Waktu: " + deadline);
        tvDescription.setText(description);

        if (imagePath != null && !imagePath.isEmpty()) {
            File f = new File(imagePath);
            if (f.exists()) {
                ivPreviewImage.setVisibility(View.VISIBLE);
                ivPreviewImage.setImageURI(Uri.parse(imagePath));
                ivPreviewImage.setOnClickListener(v -> openImage(imagePath));
            }
        }

        // Render simple attachments list as text chips
        renderAttachments();

        btnBack.setOnClickListener(v -> finish());

        btnEdit.setOnClickListener(v -> {
            Intent i = new Intent(this, EditTaskActivity.class);
            i.putExtra("task_position", taskPosition);
            i.putExtra("task_title", title);
            i.putExtra("task_course", course);
            i.putExtra("task_deadline", deadline);
            i.putExtra("task_description", description);
            i.putExtra("task_image_path", imagePath);
            i.putExtra("task_color", color);
            i.putExtra("task_completed", completed);
            i.putExtra("task_attachments", attsJson);
            editLauncher.launch(i);
        });

        btnDelete.setOnClickListener(v -> {
            Intent result = new Intent();
            result.putExtra("task_position", taskPosition);
            result.putExtra("action", "delete");
            setResult(RESULT_OK, result);
            finish();
        });

        btnMarkComplete.setOnClickListener(v -> {
            Intent result = new Intent();
            result.putExtra("task_position", taskPosition);
            result.putExtra("task_title", title);
            result.putExtra("task_course", course);
            result.putExtra("task_deadline", deadline);
            result.putExtra("task_description", description);
            result.putExtra("task_image_path", imagePath);
            result.putExtra("task_color", color);
            result.putExtra("task_completed", true);
            result.putExtra("action", "complete");
            setResult(RESULT_OK, result);
            finish();
        });
    }

    private void renderAttachments() {
        if (attsJson == null || attsJson.isEmpty()) return;
        try {
            JSONArray arr = new JSONArray(attsJson);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = new JSONObject();
                o = arr.getJSONObject(i);
                final String type = o.optString("type", "");
                final String name = o.optString("name", "Lampiran");
                final String uriStr = o.optString("uri", "");
                final String mime = o.optString("mime", "");

                TextView chip = new TextView(this);
                chip.setText(name);
                chip.setTextColor(0xFFFFFFFF);
                chip.setPadding(24, 12, 24, 12);
                chip.setBackgroundResource(R.drawable.input_background);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.setMargins(0, 0, 12, 12);
                chip.setLayoutParams(lp);
                chip.setOnClickListener(v -> onAttachmentClick(type, uriStr, mime));
                attachmentsContainer.addView(chip);
            }
        } catch (Exception ignore) {}
    }

    private void onAttachmentClick(String type, String uriStr, String mime) {
        try {
            if (type != null && (type.equals("link") || uriStr.startsWith("http://") || uriStr.startsWith("https://"))) {
                openLink(uriStr);
                return;
            }
            // Treat images
            if ((type != null && type.equals("image")) || (mime != null && mime.startsWith("image/"))) {
                openImage(uriStr);
                return;
            }
            // Default: open as file
            openFile(uriStr, mime);
        } catch (Exception ignored) {}
    }

    private void openLink(String url) {
        try {
            Intent view = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(view);
        } catch (Exception ignored) {}
    }

    private void openImage(String uriStr) {
        try {
            // If it's a local absolute path, open fullscreen activity
            if (uriStr != null && (uriStr.startsWith("/") || uriStr.startsWith("file:"))) {
                Intent i = new Intent(this, FullscreenImageActivity.class);
                if (uriStr.startsWith("file:")) {
                    i.putExtra("image_path", Uri.parse(uriStr).getPath());
                } else {
                    i.putExtra("image_path", uriStr);
                }
                startActivity(i);
            } else {
                // Otherwise try generic viewer
                Intent view = new Intent(Intent.ACTION_VIEW);
                Uri u = uriStr != null ? Uri.parse(uriStr) : null;
                view.setDataAndType(u, "image/*");
                view.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(view);
            }
        } catch (Exception ignored) {}
    }

    private void openFile(String uriStr, String mime) {
        try {
            Intent view = new Intent(Intent.ACTION_VIEW);
            Uri u;
            if (uriStr != null && uriStr.startsWith("/")) {
                u = Uri.fromFile(new File(uriStr));
            } else {
                u = Uri.parse(uriStr);
            }
            if (mime == null || mime.isEmpty()) mime = "application/*";
            view.setDataAndType(u, mime);
            view.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(view, "Buka lampiran dengan"));
        } catch (Exception ignored) {}
    }
}
