package com.example.tugasku;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import android.webkit.URLUtil;

public class AddTaskActivity extends AppCompatActivity {

    private EditText etNamaTugas, etMataKuliah, etDeskripsi;
    private TextView tvSelectedDate;
    private RelativeLayout datePickerLayout;
    private Button btnSimpan, btnAttachImage, btnAttachFile, btnAddLink;
    private ImageView btnBack, ivPreviewImage;
    private Calendar selectedDate;
    private String selectedImagePath = "";
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<Intent> filePickerLauncher;
    private RecyclerView rvAttachments;
    private AttachmentAdapter attachmentAdapter;
    private List<Attachment> attachments = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

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
                            
                            Toast.makeText(this, getString(R.string.toast_image_attached), Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(this, getString(R.string.toast_image_attach_failed), Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(this, getString(R.string.toast_file_attached), Toast.LENGTH_SHORT).show();
                    } else {
                        Uri fileUri = data.getData();
                        if (fileUri != null) {
                            handlePickedFileUri(fileUri);
                            attachmentAdapter.notifyItemInserted(attachments.size() - 1);
                            Toast.makeText(this, getString(R.string.toast_file_attached), Toast.LENGTH_SHORT).show();
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
        datePickerLayout = findViewById(R.id.tv_selected_date).getParent() instanceof RelativeLayout 
            ? (RelativeLayout) findViewById(R.id.tv_selected_date).getParent() 
            : null;
        btnSimpan = findViewById(R.id.btn_simpan);
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

        // Initialize calendar with current date
        selectedDate = Calendar.getInstance();
        updateDateDisplay();

        // Back button listener
        btnBack.setOnClickListener(v -> finish());

        // Attach image button listener
        btnAttachImage.setOnClickListener(v -> openImagePicker());

        // Open preview image in fullscreen
        ivPreviewImage.setOnClickListener(v -> {
            if (selectedImagePath != null && !selectedImagePath.isEmpty()) {
                Intent i = new Intent(this, FullscreenImageActivity.class);
                i.putExtra("image_path", selectedImagePath);
                startActivity(i);
            }
        });

        // Attach file button listener
        btnAttachFile.setOnClickListener(v -> openFilePicker());

        // Add link button listener
        btnAddLink.setOnClickListener(v -> showAddLinkDialog());

        // Date picker click listener
        View datePickerView = findViewById(R.id.tv_selected_date).getParent() instanceof View 
            ? (View) findViewById(R.id.tv_selected_date).getParent() 
            : null;
        
        if (datePickerView != null) {
            datePickerView.setOnClickListener(v -> showDatePicker());
        }
        
        tvSelectedDate.setOnClickListener(v -> showDatePicker());

        // Save button listener
        btnSimpan.setOnClickListener(v -> saveTask());
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            (view, year, month, dayOfMonth) -> {
                selectedDate.set(Calendar.YEAR, year);
                selectedDate.set(Calendar.MONTH, month);
                selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                // After choosing date, open time picker
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
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy, HH:mm", getAppLocale());
        tvSelectedDate.setText(sdf.format(selectedDate.getTime()));
    }

    private Locale getAppLocale() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return getResources().getConfiguration().getLocales().get(0);
        }
        //noinspection deprecation
        return getResources().getConfiguration().locale;
    }

    private void handlePickedFileUri(Uri fileUri) {
        try {
            // Persist read permission for future access
            getContentResolver().takePersistableUriPermission(
                fileUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            );

            String displayName = getString(R.string.attachment_default_file);
            String mime = getContentResolver().getType(fileUri);
            // Try to query display name
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

    private void saveTask() {
        String namaTugas = etNamaTugas.getText().toString().trim();
        String mataKuliah = etMataKuliah.getText().toString().trim();
        String deskripsi = etDeskripsi.getText().toString().trim();

        // Validation
        if (namaTugas.isEmpty()) {
            etNamaTugas.setError(getString(R.string.error_task_name_required));
            etNamaTugas.requestFocus();
            return;
        }

        if (mataKuliah.isEmpty()) {
            etMataKuliah.setError(getString(R.string.error_course_required));
            etMataKuliah.requestFocus();
            return;
        }

        // Generate random color for task indicator
        int[] colors = {
            Color.parseColor("#FFA726"), // Orange
            Color.parseColor("#29B6F6"), // Blue
            Color.parseColor("#FFEE58"), // Yellow
            Color.parseColor("#66BB6A"), // Green
            Color.parseColor("#EF5350"), // Red
            Color.parseColor("#AB47BC")  // Purple
        };
        int randomColor = colors[new Random().nextInt(colors.length)];

        // Format date and time
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy, HH:mm", getAppLocale());
        String deadline = sdf.format(selectedDate.getTime());

        // Create intent to return data
        Intent resultIntent = new Intent();
        resultIntent.putExtra("task_title", namaTugas);
        resultIntent.putExtra("task_course", mataKuliah);
        resultIntent.putExtra("task_deadline", deadline);
        resultIntent.putExtra("task_color", randomColor);
        resultIntent.putExtra("task_description", deskripsi);
        resultIntent.putExtra("task_image_path", selectedImagePath);
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

        setResult(RESULT_OK, resultIntent);
        finish();

        Toast.makeText(this, getString(R.string.toast_task_added), Toast.LENGTH_SHORT).show();
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
        input.setHint(getString(R.string.hint_add_link_input));
        new AlertDialog.Builder(this)
            .setTitle(R.string.dialog_add_link_title)
            .setView(input)
            .setPositiveButton(R.string.dialog_add_link_positive, (DialogInterface dialog, int which) -> {
                String url = input.getText().toString().trim();
                if (!url.isEmpty() && URLUtil.isValidUrl(url) && (url.startsWith("http://") || url.startsWith("https://"))) {
                    attachments.add(new Attachment(Attachment.TYPE_LINK, url, url, "text/uri-list"));
                    attachmentAdapter.notifyItemInserted(attachments.size() - 1);
                } else {
                    Toast.makeText(this, getString(R.string.toast_invalid_link), Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton(R.string.cancel, null)
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
