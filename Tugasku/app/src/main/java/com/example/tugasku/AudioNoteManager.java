package com.example.tugasku;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class AudioNoteManager {
    private static final String PREF_NAME = "TugaskuAudioNotesPrefs";
    private static final String KEY_AUDIO_NOTES = "all_audio_notes";
    private SharedPreferences sharedPreferences;

    public AudioNoteManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveAudioNotes(List<AudioNote> notes) {
        try {
            JSONArray jsonArray = new JSONArray();
            for (AudioNote note : notes) {
                JSONObject obj = new JSONObject();
                obj.put("id", note.getId());
                obj.put("title", note.getTitle());
                obj.put("transcript", note.getTranscript());
                obj.put("summary", note.getSummary());
                obj.put("timestamp", note.getTimestamp());
                obj.put("audioPath", note.getAudioPath());
                jsonArray.put(obj);
            }
            sharedPreferences.edit().putString(KEY_AUDIO_NOTES, jsonArray.toString()).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public List<AudioNote> loadAudioNotes() {
        List<AudioNote> notes = new ArrayList<>();
        try {
            String json = sharedPreferences.getString(KEY_AUDIO_NOTES, "[]");
            JSONArray jsonArray = new JSONArray(json);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                notes.add(new AudioNote(
                    obj.getString("id"),
                    obj.getString("title"),
                    obj.getString("transcript"),
                    obj.getString("summary"),
                    obj.getLong("timestamp"),
                    obj.getString("audioPath")
                ));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return notes;
    }
}
