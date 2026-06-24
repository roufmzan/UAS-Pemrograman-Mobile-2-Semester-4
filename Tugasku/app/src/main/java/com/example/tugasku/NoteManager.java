package com.example.tugasku;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class NoteManager {
    private static final String PREF_NAME = "TugaskuNotesPrefs";
    private static final String KEY_NOTES = "all_notes";
    private SharedPreferences sharedPreferences;

    public NoteManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveNotes(List<Note> notes) {
        try {
            JSONArray jsonArray = new JSONArray();
            for (Note note : notes) {
                JSONObject obj = new JSONObject();
                obj.put("id", note.getId());
                obj.put("title", note.getTitle());
                obj.put("content", note.getContent());
                obj.put("timestamp", note.getTimestamp());
                obj.put("color", note.getColor());
                jsonArray.put(obj);
            }
            sharedPreferences.edit().putString(KEY_NOTES, jsonArray.toString()).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public List<Note> loadNotes() {
        List<Note> notes = new ArrayList<>();
        try {
            String json = sharedPreferences.getString(KEY_NOTES, "[]");
            JSONArray jsonArray = new JSONArray(json);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                notes.add(new Note(
                    obj.getString("id"),
                    obj.getString("title"),
                    obj.getString("content"),
                    obj.getLong("timestamp"),
                    obj.getInt("color")
                ));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return notes;
    }
}
