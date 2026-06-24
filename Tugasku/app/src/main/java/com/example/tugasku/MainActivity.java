package com.example.tugasku;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TaskAdapter taskAdapter;
    private List<Task> activeTasks;
    private List<Task> completedTasks;
    private Button btnAktif, btnSelesai;
    private TextView subtitle;
    private FloatingActionButton fabAdd;
    private ImageView btnMenu;
    private boolean showingActiveTasks = true;
    
    private ActivityResultLauncher<Intent> addTaskLauncher;
    private ActivityResultLauncher<Intent> editTaskLauncher;
    private TaskManager taskManager;
    private ThemeManager themeManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Ensure notification channel and ask notification permission on first install (Android 13+)
        NotificationHelper.ensureChannel(this);
        promptNotificationPermissionOnFirstInstall();

        // Initialize TaskManager
        taskManager = new TaskManager(this);
        
        // Initialize ThemeManager and apply theme
        themeManager = new ThemeManager(this);
        applyTheme();

        // Initialize activity result launcher for adding task
        addTaskLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    String title = data.getStringExtra("task_title");
                    String course = data.getStringExtra("task_course");
                    String deadline = data.getStringExtra("task_deadline");
                    String description = data.getStringExtra("task_description");
                    String imagePath = data.getStringExtra("task_image_path");
                    int color = data.getIntExtra("task_color", Color.parseColor("#FFA726"));
                    String attsJson = data.getStringExtra("task_attachments");
                    
                    // Add new task to active tasks
                    Task newTask = new Task(title, course, deadline, description, imagePath != null ? imagePath : "", color, false);
                    // parse attachments
                    if (attsJson != null && !attsJson.isEmpty()) {
                        try {
                            org.json.JSONArray arr = new org.json.JSONArray(attsJson);
                            java.util.List<Attachment> list = new java.util.ArrayList<>();
                            for (int i = 0; i < arr.length(); i++) {
                                org.json.JSONObject o = arr.getJSONObject(i);
                                list.add(new Attachment(
                                    o.optString("type", Attachment.TYPE_FILE),
                                    o.optString("name", ""),
                                    o.optString("uri", ""),
                                    o.optString("mime", "")
                                ));
                            }
                            newTask.setAttachments(list);
                        } catch (Exception ignore) {}
                    }
                    activeTasks.add(0, newTask); // Add to beginning of list

                    // Schedule reminder for new task
                    ReminderScheduler.scheduleReminder(this, newTask.getTitle(), newTask.getCourse(), newTask.getDeadline());
                    
                    // Save to storage
                    taskManager.saveActiveTasks(activeTasks);
                    
                    // Update UI if showing active tasks
                    if (showingActiveTasks) {
                        taskAdapter.notifyItemInserted(0);
                        recyclerView.scrollToPosition(0);
                    }
                }
            }
        );

        // Initialize activity result launcher for editing task
        editTaskLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    int position = data.getIntExtra("task_position", -1);
                    String action = data.getStringExtra("action");
                    
                    if (position == -1) return;
                    
                    if ("delete".equals(action)) {
                        // Delete task
                        if (showingActiveTasks && position < activeTasks.size()) {
                            Task toDelete = activeTasks.get(position);
                            // Cancel reminder before deleting
                            ReminderScheduler.cancelReminder(this, toDelete.getTitle(), toDelete.getCourse(), toDelete.getDeadline());
                            activeTasks.remove(position);
                            taskAdapter.notifyItemRemoved(position);
                            taskManager.saveActiveTasks(activeTasks);
                        } else if (!showingActiveTasks && position < completedTasks.size()) {
                            Task toDelete = completedTasks.get(position);
                            // Cancel reminder (should already be canceled when completed, but safe)
                            ReminderScheduler.cancelReminder(this, toDelete.getTitle(), toDelete.getCourse(), toDelete.getDeadline());
                            completedTasks.remove(position);
                            taskAdapter.notifyItemRemoved(position);
                            taskManager.saveCompletedTasks(completedTasks);
                        }
                    } else if ("complete".equals(action)) {
                        // Move task to completed
                        if (position < activeTasks.size()) {
                            Task task = activeTasks.remove(position);
                            // Cancel reminder when completed
                            ReminderScheduler.cancelReminder(this, task.getTitle(), task.getCourse(), task.getDeadline());
                            task.setCompleted(true);
                            completedTasks.add(0, task);
                            
                            // Save both lists
                            taskManager.saveActiveTasks(activeTasks);
                            taskManager.saveCompletedTasks(completedTasks);
                            
                            if (showingActiveTasks) {
                                taskAdapter.notifyItemRemoved(position);
                            }
                        }
                    } else if ("edit".equals(action)) {
                        // Update task
                        String title = data.getStringExtra("task_title");
                        String course = data.getStringExtra("task_course");
                        String deadline = data.getStringExtra("task_deadline");
                        String description = data.getStringExtra("task_description");
                        String imagePath = data.getStringExtra("task_image_path");
                        int color = data.getIntExtra("task_color", 0);
                        boolean completed = data.getBooleanExtra("task_completed", false);
                        String attsJson = data.getStringExtra("task_attachments");
                        
                        Task task;
                        if (showingActiveTasks && position < activeTasks.size()) {
                            task = activeTasks.get(position);
                        } else if (!showingActiveTasks && position < completedTasks.size()) {
                            task = completedTasks.get(position);
                        } else {
                            return;
                        }
                        
                        // Cancel old reminder before updating
                        ReminderScheduler.cancelReminder(this, task.getTitle(), task.getCourse(), task.getDeadline());

                        task.setTitle(title);
                        task.setCourse(course);
                        task.setDeadline(deadline);
                        task.setDescription(description);
                        task.setImagePath(imagePath != null ? imagePath : "");
                        task.setColorIndicator(color);
                        task.setCompleted(completed);
                        // update attachments
                        if (attsJson != null && !attsJson.isEmpty()) {
                            try {
                                org.json.JSONArray arr = new org.json.JSONArray(attsJson);
                                java.util.List<Attachment> list = new java.util.ArrayList<>();
                                for (int i = 0; i < arr.length(); i++) {
                                    org.json.JSONObject o = arr.getJSONObject(i);
                                    list.add(new Attachment(
                                        o.optString("type", Attachment.TYPE_FILE),
                                        o.optString("name", ""),
                                        o.optString("uri", ""),
                                        o.optString("mime", "")
                                    ));
                                }
                                task.setAttachments(list);
                            } catch (Exception ignore) {}
                        }
                        
                        // Save to storage
                        if (showingActiveTasks) {
                            taskManager.saveActiveTasks(activeTasks);
                        } else {
                            taskManager.saveCompletedTasks(completedTasks);
                        }
                        
                        taskAdapter.notifyItemChanged(position);

                        // Schedule new reminder after update (only if still active)
                        if (!task.isCompleted()) {
                            ReminderScheduler.scheduleReminder(this, task.getTitle(), task.getCourse(), task.getDeadline());
                        }
                    }
                }
            }
        );

        // Initialize views
        recyclerView = findViewById(R.id.recycler_view_tasks);
        btnAktif = findViewById(R.id.btn_aktif);
        btnSelesai = findViewById(R.id.btn_selesai);
        subtitle = findViewById(R.id.subtitle);
        fabAdd = findViewById(R.id.fab_add);
        FloatingActionButton fabNotes = findViewById(R.id.fab_notes);
        btnMenu = findViewById(R.id.btn_menu);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize task lists
        initializeTasks();

        // Setup adapter with active tasks
        taskAdapter = new TaskAdapter(activeTasks, (task, position) -> {
            // Open PreviewTaskActivity when task is clicked
            Intent intent = new Intent(MainActivity.this, PreviewTaskActivity.class);
            intent.putExtra("task_position", position);
            intent.putExtra("task_title", task.getTitle());
            intent.putExtra("task_course", task.getCourse());
            intent.putExtra("task_deadline", task.getDeadline());
            intent.putExtra("task_description", task.getDescription());
            intent.putExtra("task_image_path", task.getImagePath());
            intent.putExtra("task_color", task.getColorIndicator());
            intent.putExtra("task_completed", task.isCompleted());
            // serialize attachments
            try {
                org.json.JSONArray arr = new org.json.JSONArray();
                if (task.getAttachments() != null) {
                    for (Attachment a : task.getAttachments()) {
                        org.json.JSONObject o = new org.json.JSONObject();
                        o.put("type", a.getType());
                        o.put("name", a.getName());
                        o.put("uri", a.getUri());
                        o.put("mime", a.getMimeType());
                        arr.put(o);
                    }
                }
                intent.putExtra("task_attachments", arr.toString());
            } catch (Exception ignore) {}
            editTaskLauncher.launch(intent);
        });
        recyclerView.setAdapter(taskAdapter);

        // Tab button listeners
        btnAktif.setOnClickListener(v -> showActiveTasks());
        btnSelesai.setOnClickListener(v -> showCompletedTasks());

        // FAB listener - open AddTaskActivity
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddTaskActivity.class);
            addTaskLauncher.launch(intent);
        });

        // FAB Notes listener - open NotesActivity
        fabNotes.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, NotesActivity.class);
            startActivity(intent);
        });

        // Menu button listener
        btnMenu.setOnClickListener(v -> showPopupMenu(v));
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
            }
        }
    }

    private void promptNotificationPermissionOnFirstInstall() {
        if (Build.VERSION.SDK_INT < 33) return;
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return;

        SharedPreferences prefs = getSharedPreferences("TugaskuPreferences", MODE_PRIVATE);
        boolean asked = prefs.getBoolean("notif_permission_asked", false);
        if (asked) return;

        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Izin Notifikasi")
            .setMessage("Agar kamu tidak ketinggalan tenggat, izinkan aplikasi mengirim notifikasi.")
            .setPositiveButton("Izinkan", (d, w) -> {
                requestNotificationPermissionIfNeeded();
                prefs.edit().putBoolean("notif_permission_asked", true).apply();
            })
            .setNegativeButton("Nanti", (d, w) -> {
                prefs.edit().putBoolean("notif_permission_asked", true).apply();
            })
            .show();
    }

    private void showReminderLeadDialog() {
        final String[] labels = new String[]{"30 menit", "1 jam", "1 hari"};
        final long[] values = new long[]{30L*60L*1000L, 60L*60L*1000L, 24L*60L*60L*1000L};

        long current = ReminderScheduler.getLeadMillis(this);
        int checked = 1; // default 1 jam
        for (int i = 0; i < values.length; i++) {
            if (current == values[i]) { checked = i; break; }
        }

        final int[] selected = new int[]{checked};

        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Atur Jarak Pengingat")
            .setSingleChoiceItems(labels, checked, (dialog, which) -> selected[0] = which)
            .setPositiveButton("Simpan", (dialog, which) -> {
                long chosen = values[selected[0]];
                ReminderScheduler.setLeadMillis(this, chosen);
                android.widget.Toast.makeText(this, "Jarak pengingat disetel ke " + labels[selected[0]], android.widget.Toast.LENGTH_SHORT).show();
                // Reschedule all active tasks
                if (activeTasks != null) {
                    for (Task t : activeTasks) {
                        if (!t.isCompleted()) {
                            ReminderScheduler.cancelReminder(this, t.getTitle(), t.getCourse(), t.getDeadline());
                            ReminderScheduler.scheduleReminder(this, t.getTitle(), t.getCourse(), t.getDeadline());
                        }
                    }
                }
            })
            .setNegativeButton("Batal", null)
            .show();
    }

    private void showPopupMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.getMenuInflater().inflate(R.menu.main_menu, popupMenu.getMenu());
        
        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            
            if (itemId == R.id.menu_clear_all_tasks) {
                confirmClearAllTasks();
                return true;
            } else if (itemId == R.id.menu_notes) {
                Intent intent = new Intent(this, NotesActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.menu_clear_completed) {
                confirmClearCompletedTasks();
                return true;
            } else if (itemId == R.id.menu_export_data) {
                exportData();
                return true;
            } else if (itemId == R.id.menu_reminder_lead) {
                showReminderLeadDialog();
                return true;
            } else if (itemId == R.id.menu_about) {
                showAboutDialog();
                return true;
            } else if (itemId == R.id.menu_theme_dark) {
                switchTheme(ThemeManager.THEME_DARK);
                return true;
            } else if (itemId == R.id.menu_theme_light) {
                switchTheme(ThemeManager.THEME_LIGHT);
                return true;
            }
            return false;
        });
        
        popupMenu.show();
    }

    private void confirmClearAllTasks() {
        new AlertDialog.Builder(this)
            .setTitle("Hapus Semua Tugas")
            .setMessage("Apakah Anda yakin ingin menghapus semua tugas (aktif dan selesai)?")
            .setPositiveButton("Hapus", (dialog, which) -> {
                activeTasks.clear();
                completedTasks.clear();
                taskManager.saveActiveTasks(activeTasks);
                taskManager.saveCompletedTasks(completedTasks);
                taskAdapter.notifyDataSetChanged();
                Toast.makeText(this, "Semua tugas berhasil dihapus", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Batal", null)
            .show();
    }

    private void confirmClearCompletedTasks() {
        new AlertDialog.Builder(this)
            .setTitle("Hapus Tugas Selesai")
            .setMessage("Apakah Anda yakin ingin menghapus semua tugas yang sudah selesai?")
            .setPositiveButton("Hapus", (dialog, which) -> {
                completedTasks.clear();
                taskManager.saveCompletedTasks(completedTasks);
                if (!showingActiveTasks) {
                    taskAdapter.notifyDataSetChanged();
                }
                Toast.makeText(this, "Tugas selesai berhasil dihapus", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Batal", null)
            .show();
    }

    private void exportData() {
        int totalTasks = activeTasks.size() + completedTasks.size();
        String message = "Total Tugas: " + totalTasks + "\n" +
                        "Tugas Aktif: " + activeTasks.size() + "\n" +
                        "Tugas Selesai: " + completedTasks.size();
        
        new AlertDialog.Builder(this)
            .setTitle("Export Data")
            .setMessage(message + "\n\nFitur export akan segera hadir!")
            .setPositiveButton("OK", null)
            .show();
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Tentang Aplikasi")
            .setMessage("Tugasku v1.0\n\nAplikasi manajemen tugas sederhana untuk membantu Anda mengorganisir tugas-tugas kuliah.\n\nDibuat dengan ❤️")
            .setPositiveButton("OK", null)
            .show();
    }

    private void applyTheme() {
        boolean isDarkMode = themeManager.isDarkMode();
        
        // Get root view
        View rootView = findViewById(R.id.main);
        View header = findViewById(R.id.header);
        
        // Apply colors based on theme
        if (isDarkMode) {
            rootView.setBackgroundColor(getResources().getColor(R.color.dark_background));
            if (header != null) {
                header.setBackgroundColor(getResources().getColor(R.color.dark_background));
            }
        } else {
            rootView.setBackgroundColor(getResources().getColor(R.color.light_background));
            if (header != null) {
                header.setBackgroundColor(getResources().getColor(R.color.light_background));
            }
        }
        
        // Update text colors if views are initialized
        if (subtitle != null) {
            updateTextColors(isDarkMode);
        }
    }

    private void updateTextColors(boolean isDarkMode) {
        TextView title = findViewById(R.id.title);
        ImageView btnMenu = findViewById(R.id.btn_menu);
        
        int textPrimary = isDarkMode ? 
            getResources().getColor(R.color.dark_text_primary) : 
            getResources().getColor(R.color.light_text_primary);
        int textSecondary = isDarkMode ? 
            getResources().getColor(R.color.dark_text_secondary) : 
            getResources().getColor(R.color.light_text_secondary);
        
        title.setTextColor(textPrimary);
        subtitle.setTextColor(textSecondary);
        
        // Update menu icon tint
        if (isDarkMode) {
            btnMenu.setColorFilter(getResources().getColor(R.color.dark_text_primary));
        } else {
            btnMenu.setColorFilter(getResources().getColor(R.color.light_text_primary));
        }
    }

    private void switchTheme(String theme) {
        themeManager.saveTheme(theme);
        
        // Show toast
        String message = theme.equals(ThemeManager.THEME_DARK) ? 
            "Mode Gelap diaktifkan" : "Mode Terang diaktifkan";
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        
        // Recreate activity to apply theme
        recreate();
    }

    private void initializeTasks() {
        // Load tasks from storage
        activeTasks = taskManager.loadActiveTasks();
        completedTasks = taskManager.loadCompletedTasks();
        
        // If no tasks loaded, initialize empty lists
        if (activeTasks == null) {
            activeTasks = new ArrayList<>();
        }
        if (completedTasks == null) {
            completedTasks = new ArrayList<>();
        }
    }

    private void showActiveTasks() {
        showingActiveTasks = true;
        taskAdapter.updateTasks(activeTasks);
        
        // Update button styles
        btnAktif.setBackgroundResource(R.drawable.tab_active);
        btnAktif.setTextColor(Color.WHITE);
        btnSelesai.setBackgroundResource(R.drawable.tab_inactive);
        btnSelesai.setTextColor(Color.BLACK);
        
        // Update subtitle
        subtitle.setText(R.string.subtitle_active);
    }

    private void showCompletedTasks() {
        showingActiveTasks = false;
        taskAdapter.updateTasks(completedTasks);
        
        // Update button styles
        btnSelesai.setBackgroundResource(R.drawable.tab_active);
        btnSelesai.setTextColor(Color.WHITE);
        btnAktif.setBackgroundResource(R.drawable.tab_inactive);
        btnAktif.setTextColor(Color.BLACK);
        
        // Update subtitle
        subtitle.setText(R.string.subtitle_completed);
    }
}