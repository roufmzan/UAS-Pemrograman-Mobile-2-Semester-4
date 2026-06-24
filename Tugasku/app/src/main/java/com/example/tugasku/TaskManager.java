package com.example.tugasku;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class TaskManager {
    private static final String PREF_NAME = "TugaskuPreferences";
    private static final String KEY_ACTIVE_TASKS = "active_tasks";
    private static final String KEY_COMPLETED_TASKS = "completed_tasks";
    
    private SharedPreferences sharedPreferences;
    
    public TaskManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    // Save active tasks
    public void saveActiveTasks(List<Task> tasks) {
        saveTasks(KEY_ACTIVE_TASKS, tasks);
    }
    
    // Save completed tasks
    public void saveCompletedTasks(List<Task> tasks) {
        saveTasks(KEY_COMPLETED_TASKS, tasks);
    }
    
    // Load active tasks
    public List<Task> loadActiveTasks() {
        return loadTasks(KEY_ACTIVE_TASKS);
    }
    
    // Load completed tasks
    public List<Task> loadCompletedTasks() {
        return loadTasks(KEY_COMPLETED_TASKS);
    }
    
    // Generic method to save tasks
    private void saveTasks(String key, List<Task> tasks) {
        try {
            JSONArray jsonArray = new JSONArray();
            
            for (Task task : tasks) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("title", task.getTitle());
                jsonObject.put("course", task.getCourse());
                jsonObject.put("deadline", task.getDeadline());
                jsonObject.put("description", task.getDescription() != null ? task.getDescription() : "");
                jsonObject.put("imagePath", task.getImagePath() != null ? task.getImagePath() : "");
                jsonObject.put("color", task.getColorIndicator());
                jsonObject.put("completed", task.isCompleted());
                // attachments
                JSONArray atts = new JSONArray();
                if (task.getAttachments() != null) {
                    for (Attachment a : task.getAttachments()) {
                        JSONObject o = new JSONObject();
                        o.put("type", a.getType());
                        o.put("name", a.getName());
                        o.put("uri", a.getUri());
                        o.put("mime", a.getMimeType());
                        atts.put(o);
                    }
                }
                jsonObject.put("attachments", atts);
                
                jsonArray.put(jsonObject);
            }
            
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(key, jsonArray.toString());
            editor.apply();
            
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    
    // Generic method to load tasks
    private List<Task> loadTasks(String key) {
        List<Task> tasks = new ArrayList<>();
        
        try {
            String jsonString = sharedPreferences.getString(key, "[]");
            JSONArray jsonArray = new JSONArray(jsonString);
            
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                
                String title = jsonObject.getString("title");
                String course = jsonObject.getString("course");
                String deadline = jsonObject.getString("deadline");
                String description = jsonObject.optString("description", "");
                String imagePath = jsonObject.optString("imagePath", "");
                int color = jsonObject.getInt("color");
                boolean completed = jsonObject.getBoolean("completed");
                
                Task task = new Task(title, course, deadline, description, imagePath, color, completed);
                // load attachments if exists
                if (jsonObject.has("attachments")) {
                    JSONArray atts = jsonObject.optJSONArray("attachments");
                    if (atts != null) {
                        java.util.List<Attachment> list = new java.util.ArrayList<>();
                        for (int j = 0; j < atts.length(); j++) {
                            JSONObject o = atts.getJSONObject(j);
                            list.add(new Attachment(
                                o.optString("type", Attachment.TYPE_FILE),
                                o.optString("name", ""),
                                o.optString("uri", ""),
                                o.optString("mime", "")
                            ));
                        }
                        task.setAttachments(list);
                    }
                }
                tasks.add(task);
            }
            
        } catch (JSONException e) {
            e.printStackTrace();
        }
        
        return tasks;
    }
    
    // Clear all tasks (optional, for debugging or reset)
    public void clearAllTasks() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_ACTIVE_TASKS);
        editor.remove(KEY_COMPLETED_TASKS);
        editor.apply();
    }
}
