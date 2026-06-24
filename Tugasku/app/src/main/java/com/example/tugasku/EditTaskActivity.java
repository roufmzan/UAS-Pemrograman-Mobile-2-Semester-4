package com.example.tugasku;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import android.webkit.URLUtil;

public class EditTaskActivity extends AppCompatActivity {

    private EditText etNamaTugas, etMataKuliah, etDeskripsi;
    private TextView tvSelectedDate;
    private RelativeLayout datePickerLayout;
    private Button btnSave, btnCancel, btnAttachImage, btnAttachFile, btnAddLink;
    private ImageView btnBack, ivPreviewImage;
    private Calendar selectedDate;
    
    private int taskPosition;
    private String originalTitle, originalCourse, originalDeadline, originalDescription, originalImagePath;
    private int originalColor;
    private boolean originalCompleted;
    private boolean isEditMode = true;
    private String selectedImagePath = "";
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<Intent> filePickerLauncher;
    private RecyclerView rvAttachments;
    private AttachmentAdapter attachmentAdapter;
    private List<Attachment> attachments = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_task);

        // Initialize image picker launcher
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        try {
                            // Save image to internal storage
                            selectedImagePath = saveImageToInternalStorage(imageUri);
                            
                            // Show preview
                            ivPreviewImage.setImageURI(Uri.parse(selectedImagePath));
                            ivPreviewImage.setVisibility(View.VISIBLE);
                            
                            Toast.makeText(this, "Gambar berhasil dilampirkan", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(this, "Gagal melampirkan gambar", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        );

        // Initialize file picker launcher
        filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    if (data.getClipData() != null) {
                        int count = data.getClipData().getItemCount();
                        for (int i = 0; i < count; i++) {
                            Uri uri = data.getClipData().getItemAt(i).getUri();
                            handlePickedFileUri(uri);
                        }
                        attachmentAdapter.notifyDataSetChanged();
                        Toast.makeText(this, "File berhasil dilampirkan", Toast.LENGTH_SHORT).show();
                    } else {
                        Uri fileUri = data.getData();
                        if (fileUri != null) {
                            handlePickedFileUri(fileUri);
                            attachmentAdapter.notifyItemInserted(attachments.size() - 1);
                            Toast.makeText(this, "File berhasil dilampirkan", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        );

        // Initialize views
        etNamaTugas = findViewById(R.id.et_nama_tugas);
        etMataKuliah = findViewById(R.id.et_mata_kuliah);
        etDeskripsi = findViewById(R.id.et_deskripsi);
        tvSelectedDate = findViewById(R.id.tv_selected_date);
        datePickerLayout = findViewById(R.id.date_picker_layout);
        btnSave = findViewById(R.id.btn_save);
        btnCancel = findViewById(R.id.btn_cancel);
        btnBack = findViewById(R.id.btn_back);
        btnAttachImage = findViewById(R.id.btn_attach_image);
        btnAttachFile = findViewById(R.id.btn_attach_file);
        btnAddLink = findViewById(R.id.btn_add_link);
        ivPreviewImage = findViewById(R.id.iv_preview_image);
        rvAttachments = findViewById(R.id.rv_attachments);

        // Setup attachments RecyclerView
        rvAttachments.setLayoutManager(new LinearLayoutManager(this));
        attachmentAdapter = new AttachmentAdapter(this, attachments);
        rvAttachments.setAdapter(attachmentAdapter);
        // Swipe to delete attachments
        ItemTouchHelper ith = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getAdapterPosition();
                if (pos >= 0 && pos < attachments.size()) {
                    attachments.remove(pos);
                    attachmentAdapter.notifyItemRemoved(pos);
                } else {
                    attachmentAdapter.notifyDataSetChanged();
                }
            }
        });
        ith.attachToRecyclerView(rvAttachments);

        // Initialize calendar
        selectedDate = Calendar.getInstance();

        // Get data from intent
        Intent intent = getIntent();
        taskPosition = intent.getIntExtra("task_position", -1);
        originalTitle = intent.getStringExtra("task_title");
        originalCourse = intent.getStringExtra("task_course");
        originalDeadline = intent.getStringExtra("task_deadline");
        originalDescription = intent.getStringExtra("task_description");
        originalImagePath = intent.getStringExtra("task_image_path");
        originalColor = intent.getIntExtra("task_color", 0);
        originalCompleted = intent.getBooleanExtra("task_completed", false);
        
        selectedImagePath = originalImagePath != null ? originalImagePath : "";

        // Load attachments from intent extra if provided
        String attsJson = intent.getStringExtra("task_attachments");
        if (attsJson != null && !attsJson.isEmpty()) {
            try {
                JSONArray arr = new JSONArray(attsJson);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    attachments.add(new Attachment(
                        o.optString("type", Attachment.TYPE_FILE),
                        o.optString("name", ""),
                        o.optString("uri", ""),
                        o.optString("mime", "")
                    ));
                }
                attachmentAdapter.notifyDataSetChanged();
            } catch (Exception ignore) {}
        }

        // Populate fields
        etNamaTugas.setText(originalTitle);
        etMataKuliah.setText(originalCourse);
        etDeskripsi.setText(originalDescription);
        tvSelectedDate.setText(originalDeadline);
        
        // Show image if exists
        if (selectedImagePath != null && !selectedImagePath.isEmpty()) {
            File imageFile = new File(selectedImagePath);
            if (imageFile.exists()) {
                ivPreviewImage.setImageURI(Uri.parse(selectedImagePath));
                ivPreviewImage.setVisibility(View.VISIBLE);
            }
        }

        // Parse deadline to calendar (supports with or without time)
        try {
            SimpleDateFormat withTime = new SimpleDateFormat("dd MMMM yyyy, HH:mm", new Locale("id", "ID"));
            SimpleDateFormat dateOnly = new SimpleDateFormat("dd MMMM yyyy", new Locale("id", "ID"));
            if (originalDeadline != null) {
                try {
                    selectedDate.setTime(withTime.parse(originalDeadline));
                } catch (ParseException e1) {
                    selectedDate.setTime(dateOnly.parse(originalDeadline));
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        // Fields are editable by default on edit screen
        setFieldsEditable(true);

        // Back button listener
        btnBack.setOnClickListener(v -> finish());

        // Date picker listener
        datePickerLayout.setOnClickListener(v -> {
            if (isEditMode) {
                showDatePicker();
            }
        });

        // Open preview image in fullscreen
        ivPreviewImage.setOnClickListener(v -> {
            if (selectedImagePath != null && !selectedImagePath.isEmpty()) {
                Intent i = new Intent(this, FullscreenImageActivity.class);
                i.putExtra("image_path", selectedImagePath);
                startActivity(i);
            }
        });

        // Attach image button listener
        btnAttachImage.setOnClickListener(v -> {
            if (isEditMode) {
                openImagePicker();
            }
        });

        // Attach file button listener
        btnAttachFile.setOnClickListener(v -> {
            if (isEditMode) {
                openFilePicker();
            }
        });

        // Add link button listener
        btnAddLink.setOnClickListener(v -> {
            if (isEditMode) {
                showAddLinkDialog();
            }
        });

        // Save button listener
        btnSave.setOnClickListener(v -> saveChanges());

        // Cancel button listener
        btnCancel.setOnClickListener(v -> finish());
    }

    private void setFieldsEditable(boolean editable) {
        etNamaTugas.setEnabled(editable);
        etMataKuliah.setEnabled(editable);
        etDeskripsi.setEnabled(editable);
        datePickerLayout.setEnabled(editable);
        
        // Change appearance based on edit mode
        if (editable) {
            etNamaTugas.setAlpha(1.0f);
            etMataKuliah.setAlpha(1.0f);
            etDeskripsi.setAlpha(1.0f);
        } else {
            etNamaTugas.setAlpha(0.7f);
            etMataKuliah.setAlpha(0.7f);
            etDeskripsi.setAlpha(0.7f);
        }
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            (view, year, month, dayOfMonth) -> {
                selectedDate.set(Calendar.YEAR, year);
                selectedDate.set(Calendar.MONTH, month);
                selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                // After choosing date, open time picker if editing
                showTimePicker();
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void showTimePicker() {
        int hour = selectedDate.get(Calendar.HOUR_OF_DAY);
        int minute = selectedDate.get(Calendar.MINUTE);
        TimePickerDialog timePickerDialog = new TimePickerDialog(
            this,
            (view, selectedHour, selectedMinute) -> {
                selectedDate.set(Calendar.HOUR_OF_DAY, selectedHour);
                selectedDate.set(Calendar.MINUTE, selectedMinute);
                selectedDate.set(Calendar.SECOND, 0);
                updateDateDisplay();
            },
            hour,
            minute,
            true
        );
        timePickerDialog.show();
    }

    private void updateDateDisplay() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy, HH:mm", new Locale("id", "ID"));
        tvSelectedDate.setText(sdf.format(selectedDate.getTime()));
    }

    private void handlePickedFileUri(Uri fileUri) {
        try {
            getContentResolver().takePersistableUriPermission(
                fileUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            );
            String displayName = "File";
            String mime = getContentResolver().getType(fileUri);
            try (android.database.Cursor c = getContentResolver().query(fileUri, new String[]{android.provider.OpenableColumns.DISPLAY_NAME}, null, null, null)) {
                if (c != null && c.moveToFirst()) {
                    int idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (idx >= 0) displayName = c.getString(idx);
                }
            }
            attachments.add(new Attachment(Attachment.TYPE_FILE, displayName, fileUri.toString(), mime));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveChanges() {
        String namaTugas = etNamaTugas.getText().toString().trim();
        String mataKuliah = etMataKuliah.getText().toString().trim();
        String deskripsi = etDeskripsi.getText().toString().trim();

        // Validation
        if (namaTugas.isEmpty()) {
            etNamaTugas.setError("Nama tugas harus diisi");
            etNamaTugas.requestFocus();
            return;
        }

        if (mataKuliah.isEmpty()) {
            etMataKuliah.setError("Mata kuliah harus diisi");
            etMataKuliah.requestFocus();
            return;
        }

        // Format date and time
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy, HH:mm", new Locale("id", "ID"));
        String deadline = sdf.format(selectedDate.getTime());

        // Create intent to return data
        Intent resultIntent = new Intent();
        resultIntent.putExtra("task_position", taskPosition);
        resultIntent.putExtra("task_title", namaTugas);
        resultIntent.putExtra("task_course", mataKuliah);
        resultIntent.putExtra("task_deadline", deadline);
        resultIntent.putExtra("task_description", deskripsi);
        resultIntent.putExtra("task_image_path", selectedImagePath);
        resultIntent.putExtra("task_color", originalColor);
        resultIntent.putExtra("task_completed", originalCompleted);
        // Serialize attachments to JSON
        try {
            JSONArray arr = new JSONArray();
            for (Attachment a : attachments) {
                JSONObject o = new JSONObject();
                o.put("type", a.getType());
                o.put("name", a.getName());
                o.put("uri", a.getUri());
                o.put("mime", a.getMimeType());
                arr.put(o);
            }
            resultIntent.putExtra("task_attachments", arr.toString());
        } catch (Exception ignore) {}
        resultIntent.putExtra("action", "edit");

        setResult(RESULT_OK, resultIntent);
        finish();

        Toast.makeText(this, "Tugas berhasil diperbarui", Toast.LENGTH_SHORT).show();
    }

    private void markTaskComplete() {
        // Create intent to return data
        Intent resultIntent = new Intent();
        resultIntent.putExtra("task_position", taskPosition);
        resultIntent.putExtra("task_title", originalTitle);
        resultIntent.putExtra("task_course", originalCourse);
        resultIntent.putExtra("task_deadline", originalDeadline);
        resultIntent.putExtra("task_description", originalDescription);
        resultIntent.putExtra("task_image_path", originalImagePath);
        resultIntent.putExtra("task_color", originalColor);
        resultIntent.putExtra("task_completed", true);
        resultIntent.putExtra("action", "complete");

        setResult(RESULT_OK, resultIntent);
        finish();

        Toast.makeText(this, "Tugas ditandai selesai", Toast.LENGTH_SHORT).show();
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
            .setTitle("Hapus Tugas")
            .setMessage("Apakah Anda yakin ingin menghapus tugas ini?")
            .setPositiveButton("Hapus", (dialog, which) -> deleteTask())
            .setNegativeButton("Batal", null)
            .show();
    }

    private void deleteTask() {
        // Create intent to return data
        Intent resultIntent = new Intent();
        resultIntent.putExtra("task_position", taskPosition);
        resultIntent.putExtra("action", "delete");

        setResult(RESULT_OK, resultIntent);
        finish();

        Toast.makeText(this, "Tugas berhasil dihapus", Toast.LENGTH_SHORT).show();
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        });
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        filePickerLauncher.launch(intent);
    }

    private void showAddLinkDialog() {
        final EditText input = new EditText(this);
        input.setHint("https://drive.google.com/...");
        new AlertDialog.Builder(this)
            .setTitle("Tambah Link")
            .setView(input)
            .setPositiveButton("Tambah", (DialogInterface dialog, int which) -> {
                String url = input.getText().toString().trim();
                if (!url.isEmpty() && URLUtil.isValidUrl(url) && (url.startsWith("http://") || url.startsWith("https://"))) {
                    attachments.add(new Attachment(Attachment.TYPE_LINK, url, url, "text/uri-list"));
                    attachmentAdapter.notifyItemInserted(attachments.size() - 1);
                } else {
                    Toast.makeText(this, "Link tidak valid", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Batal", null)
            .show();
    }

    private String saveImageToInternalStorage(Uri imageUri) throws Exception {
        InputStream inputStream = getContentResolver().openInputStream(imageUri);
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
        
        // Create file in internal storage
        String fileName = "task_image_" + System.currentTimeMillis() + ".jpg";
        File directory = new File(getFilesDir(), "task_images");
        if (!directory.exists()) {
            directory.mkdirs();
        }
        
        File file = new File(directory, fileName);
        FileOutputStream outputStream = new FileOutputStream(file);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
        outputStream.flush();
        outputStream.close();
        
        return file.getAbsolutePath();
    }
}
