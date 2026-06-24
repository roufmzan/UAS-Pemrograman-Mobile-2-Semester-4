package com.example.tugasku;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GeminiSummarizeWorker extends Worker {

    public static final String KEY_INPUT_TRANSCRIPT = "input_transcript";
    public static final String KEY_OUTPUT_SUMMARY = "output_summary";
    public static final String KEY_OUTPUT_ERROR = "output_error";
    public static final String KEY_PROGRESS = "progress";

    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent";

    public GeminiSummarizeWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String transcript = getInputData().getString(KEY_INPUT_TRANSCRIPT);
        if (transcript == null || transcript.trim().isEmpty()) {
            return Result.failure(new Data.Builder().putString(KEY_OUTPUT_ERROR, "Transkrip kosong.").build());
        }

        try {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(180, TimeUnit.SECONDS)
                    .callTimeout(180, TimeUnit.SECONDS)
                    .build();

            List<String> chunks = chunkText(transcript, 2500);
            List<String> chunkSummaries = new ArrayList<>();

            for (int i = 0; i < chunks.size(); i++) {
                setProgressAsync(new Data.Builder()
                        .putInt(KEY_PROGRESS, (int) (((i) / (float) Math.max(chunks.size(), 1)) * 80f))
                        .build());

                String chunkPrompt = "Ringkas bagian catatan kuliah berikut menjadi poin-poin penting dalam Bahasa Indonesia. " +
                        "Buat ringkas, jelas, dan gunakan bullet.\n\n" + chunks.get(i);
                String summary = callGemini(client, chunkPrompt);
                chunkSummaries.add(summary);
            }

            setProgressAsync(new Data.Builder().putInt(KEY_PROGRESS, 85).build());

            StringBuilder combined = new StringBuilder();
            for (int i = 0; i < chunkSummaries.size(); i++) {
                combined.append("Bagian ").append(i + 1).append(":\n");
                combined.append(chunkSummaries.get(i)).append("\n\n");
            }

            String finalPrompt = "Gabungkan dan rapikan ringkasan-ringkasan berikut menjadi satu ringkasan final " +
                    "dalam Bahasa Indonesia. Hasil akhir wajib berbentuk bullet poin dan berisi hal-hal paling penting saja.\n\n" + combined;

            String finalSummary = callGemini(client, finalPrompt);

            setProgressAsync(new Data.Builder().putInt(KEY_PROGRESS, 100).build());

            return Result.success(new Data.Builder().putString(KEY_OUTPUT_SUMMARY, finalSummary).build());
        } catch (Exception e) {
            return Result.failure(new Data.Builder().putString(KEY_OUTPUT_ERROR, e.getMessage()).build());
        }
    }

    private static List<String> chunkText(String text, int maxChars) {
        List<String> out = new ArrayList<>();
        String t = text.trim();
        int start = 0;
        while (start < t.length()) {
            int end = Math.min(start + maxChars, t.length());
            if (end < t.length()) {
                int lastSpace = t.lastIndexOf(' ', end);
                if (lastSpace > start + (maxChars / 2)) {
                    end = lastSpace;
                }
            }
            out.add(t.substring(start, end).trim());
            start = end;
        }
        return out;
    }

    private static String callGemini(OkHttpClient client, String prompt) throws Exception {
        JSONObject body = new JSONObject();
        JSONArray contents = new JSONArray();
        JSONObject content0 = new JSONObject();
        JSONArray parts = new JSONArray();
        JSONObject part0 = new JSONObject();
        part0.put("text", prompt);
        parts.put(part0);
        content0.put("parts", parts);
        contents.put(content0);
        body.put("contents", contents);

        MediaType json = MediaType.get("application/json; charset=utf-8");
        RequestBody requestBody = RequestBody.create(body.toString(), json);

        Request request = new Request.Builder()
                .url(GEMINI_URL)
                .addHeader("Content-Type", "application/json")
                .addHeader("X-goog-api-key", BuildConfig.GEMINI_API_KEY)
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new Exception("Gemini error (" + response.code() + "): " + responseBody);
            }

            JSONObject root = new JSONObject(responseBody);
            JSONArray candidates = root.optJSONArray("candidates");
            if (candidates != null && candidates.length() > 0) {
                JSONObject c0 = candidates.optJSONObject(0);
                if (c0 != null) {
                    JSONObject content = c0.optJSONObject("content");
                    if (content != null) {
                        JSONArray respParts = content.optJSONArray("parts");
                        if (respParts != null && respParts.length() > 0) {
                            JSONObject p0 = respParts.optJSONObject(0);
                            if (p0 != null) {
                                String text = p0.optString("text", "");
                                if (text != null && !text.trim().isEmpty()) return text;
                            }
                        }
                    }
                }
            }

            return responseBody;
        }
    }
}
