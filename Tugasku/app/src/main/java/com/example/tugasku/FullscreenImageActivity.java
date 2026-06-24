package com.example.tugasku;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class FullscreenImageActivity extends AppCompatActivity {

    private ImageView imageView;
    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;
    private float scaleFactor = 1f;
    private static final float MIN_SCALE = 1f;
    private static final float MAX_SCALE = 4f;
    private float translateX = 0f;
    private float translateY = 0f;
    private float lastX = 0f;
    private float lastY = 0f;
    private boolean dragging = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen_image);

        // Hide system UI for fullscreen
        View decor = getWindow().getDecorView();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            decor.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        }

        imageView = findViewById(R.id.fullscreen_image);
        String imagePath = getIntent().getStringExtra("image_path");
        if (imagePath != null && !imagePath.isEmpty()) {
            imageView.setImageURI(Uri.parse(imagePath));
        }
        findViewById(R.id.btn_back_fullscreen).setOnClickListener(v -> finish());

        // Scale detector for pinch zoom
        scaleDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                // Make pinch more sensitive by amplifying scale factor slightly
                float factor = detector.getScaleFactor();
                factor = (float) Math.pow(factor, 1.3f);
                scaleFactor *= factor;
                if (scaleFactor < MIN_SCALE) scaleFactor = MIN_SCALE;
                if (scaleFactor > MAX_SCALE) scaleFactor = MAX_SCALE;
                imageView.setScaleX(scaleFactor);
                imageView.setScaleY(scaleFactor);
                return true;
            }
        });

        // Gesture detector for swipe down to dismiss
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_THRESHOLD = 120; // pixels (more sensitive)
            private static final int SWIPE_VELOCITY_THRESHOLD = 500; // px/s (more sensitive)

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null) return false;
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();
                if (Math.abs(diffY) > Math.abs(diffX)
                        && diffY > SWIPE_THRESHOLD
                        && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD
                        && scaleFactor <= 1.05f) {
                    finish();
                    return true;
                }
                return false;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                if (e1 == null || e2 == null) return false;
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();
                if (Math.abs(diffY) > Math.abs(diffX) && diffY > 150 && scaleFactor <= 1.05f) {
                    finish();
                    return true;
                }
                return false;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                // Toggle zoom on double tap (1x <-> 2x)
                if (scaleFactor < 1.5f) {
                    scaleFactor = 2.5f; // slightly higher on double-tap zoom-in
                    // Zoom around tap point
                    imageView.setPivotX(e.getX());
                    imageView.setPivotY(e.getY());
                } else {
                    scaleFactor = 1f;
                    translateX = 0f;
                    translateY = 0f;
                    imageView.setTranslationX(translateX);
                    imageView.setTranslationY(translateY);
                    // Reset pivot to center
                    imageView.setPivotX(imageView.getWidth() / 2f);
                    imageView.setPivotY(imageView.getHeight() / 2f);
                }
                imageView.setScaleX(scaleFactor);
                imageView.setScaleY(scaleFactor);
                return true;
            }
        });

        imageView.setOnTouchListener((v, event) -> {
            boolean sd = scaleDetector.onTouchEvent(event);
            boolean gd = gestureDetector.onTouchEvent(event);

            if (!scaleDetector.isInProgress()) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        lastX = event.getX();
                        lastY = event.getY();
                        dragging = scaleFactor > 1f;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (dragging) {
                            float dx = event.getX() - lastX;
                            float dy = event.getY() - lastY;
                            lastX = event.getX();
                            lastY = event.getY();
                            translateX += dx;
                            translateY += dy;
                            // Clamp translation so image edges don't move too far
                            float maxTransX = (imageView.getWidth() * (scaleFactor - 1f)) / 2f;
                            float maxTransY = (imageView.getHeight() * (scaleFactor - 1f)) / 2f;
                            if (maxTransX < 0) maxTransX = 0; // safety
                            if (maxTransY < 0) maxTransY = 0;
                            if (translateX > maxTransX) translateX = maxTransX;
                            if (translateX < -maxTransX) translateX = -maxTransX;
                            if (translateY > maxTransY) translateY = maxTransY;
                            if (translateY < -maxTransY) translateY = -maxTransY;
                            imageView.setTranslationX(translateX);
                            imageView.setTranslationY(translateY);
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        dragging = false;
                        break;
                }
            }

            return sd || gd || dragging;
        });
    }
}
