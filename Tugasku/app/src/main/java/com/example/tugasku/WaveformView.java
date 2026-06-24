package com.example.tugasku;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WaveformView extends View {

    private Paint paint;
    private List<Float> amplitudes = new ArrayList<>();
    private int maxBars = 40;
    private float barWidth = 8f;
    private float barGap = 6f;
    private Random random = new Random();

    public WaveformView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.parseColor("#FFD700")); // Gold color
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setAntiAlias(true);
    }

    public void addAmplitude(float amplitude) {
        // Normalisasi amplitudo (biasanya 0-32767 untuk MediaRecorder, atau RMS dari Speech)
        float normalized = Math.max(10, Math.min(amplitude, 100)); 
        amplitudes.add(normalized);
        if (amplitudes.size() > maxBars) {
            amplitudes.remove(0);
        }
        invalidate();
    }

    public void clear() {
        amplitudes.clear();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (amplitudes.isEmpty()) return;

        float midY = getHeight() / 2f;
        float totalWidth = amplitudes.size() * (barWidth + barGap) - barGap;
        float startX = (getWidth() - totalWidth) / 2f; // Rata tengah horizontal

        for (int i = 0; i < amplitudes.size(); i++) {
            float h = (amplitudes.get(i) / 100f) * (getHeight() * 0.8f);
            float x = startX + i * (barWidth + barGap);
            canvas.drawRoundRect(x, midY - h / 2f, x + barWidth, midY + h / 2f, barWidth / 2f, barWidth / 2f, paint);
        }
    }
}
