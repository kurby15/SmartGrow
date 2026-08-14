package com.example.smartgrow.camera;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ScannerOverlayView extends View {

    private Paint bracketPaint;
    private Paint linePaint;
    private Paint dotPaint;
    private Paint scrimPaint;

    private float scanLineY = 0f;
    private ValueAnimator lineAnimator;
    private boolean isScanning = false;

    private final RectF scanRect = new RectF();
    private final List<PointFHolder> detectionPoints = new ArrayList<>();
    private final Random random = new Random();

    public ScannerOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // White Curved Focus Brackets
        bracketPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bracketPaint.setColor(Color.WHITE);
        bracketPaint.setStrokeWidth(12f);
        bracketPaint.setStyle(Paint.Style.STROKE);
        bracketPaint.setStrokeCap(Paint.Cap.ROUND);

        // Animated Laser Line
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(Color.parseColor("#8000C853"));
        linePaint.setStrokeWidth(6f);

        // AI Detection Dots
        dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setColor(Color.WHITE);
        dotPaint.setStyle(Paint.Style.FILL);

        // Background Scrim (Dimming outside the scan area)
        scrimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        scrimPaint.setColor(Color.parseColor("#99000000")); // 60% black overlay
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float frameSize = w * 0.70f;
        float left = (w - frameSize) / 2f;
        float top = (h - frameSize) / 2.3f;
        scanRect.set(left, top, left + frameSize, top + frameSize);
        setupAnimation();
    }

    private void setupAnimation() {
        if (lineAnimator != null) lineAnimator.cancel();

        lineAnimator = ValueAnimator.ofFloat(scanRect.top + 20, scanRect.bottom - 20);
        lineAnimator.setDuration(2200);
        lineAnimator.setRepeatCount(ValueAnimator.INFINITE);
        lineAnimator.setRepeatMode(ValueAnimator.REVERSE);
        lineAnimator.setInterpolator(new LinearInterpolator());
        lineAnimator.addUpdateListener(animation -> {
            scanLineY = (float) animation.getAnimatedValue();
            generateRandomDots();
            invalidate();
        });

        if (isScanning) {
            lineAnimator.start();
        }
    }

    public void startScanning() {
        isScanning = true;
        if (lineAnimator != null && !lineAnimator.isRunning()) {
            lineAnimator.start();
        }
    }

    public void stopScanning() {
        isScanning = false;
        if (lineAnimator != null) {
            lineAnimator.cancel();
        }
        detectionPoints.clear();
        invalidate();
    }

    private void generateRandomDots() {
        if (random.nextInt(10) > 7) {
            if (detectionPoints.size() > 8) detectionPoints.remove(0);
            float rx = scanRect.left + 30 + random.nextFloat() * (scanRect.width() - 60);
            float ry = scanRect.top + 30 + random.nextFloat() * (scanRect.height() - 60);
            detectionPoints.add(new PointFHolder(rx, ry));
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 1. Draw Background Scrim (Everything outside scanRect)
        canvas.drawRect(0, 0, getWidth(), scanRect.top, scrimPaint);
        canvas.drawRect(0, scanRect.bottom, getWidth(), getHeight(), scrimPaint);
        canvas.drawRect(0, scanRect.top, scanRect.left, scanRect.bottom, scrimPaint);
        canvas.drawRect(scanRect.right, scanRect.top, getWidth(), scanRect.bottom, scrimPaint);

        float cornerSize = 60f;
        float radius = 30f;

        // 2. Draw 4 Curved Corner Reticles
        // Top-Left Corner
        RectF arcTL = new RectF(scanRect.left, scanRect.top, scanRect.left + radius * 2, scanRect.top + radius * 2);
        canvas.drawArc(arcTL, 180, 90, false, bracketPaint);
        canvas.drawLine(scanRect.left + radius, scanRect.top, scanRect.left + cornerSize, scanRect.top, bracketPaint);
        canvas.drawLine(scanRect.left, scanRect.top + radius, scanRect.left, scanRect.top + cornerSize, bracketPaint);

        // Top-Right Corner
        RectF arcTR = new RectF(scanRect.right - radius * 2, scanRect.top, scanRect.right, scanRect.top + radius * 2);
        canvas.drawArc(arcTR, 270, 90, false, bracketPaint);
        canvas.drawLine(scanRect.right - cornerSize, scanRect.top, scanRect.right - radius, scanRect.top, bracketPaint);
        canvas.drawLine(scanRect.right, scanRect.top + radius, scanRect.right, scanRect.top + cornerSize, bracketPaint);

        // Bottom-Left Corner
        RectF arcBL = new RectF(scanRect.left, scanRect.bottom - radius * 2, scanRect.left + radius * 2, scanRect.bottom);
        canvas.drawArc(arcBL, 90, 90, false, bracketPaint);
        canvas.drawLine(scanRect.left + radius, scanRect.bottom, scanRect.left + cornerSize, scanRect.bottom, bracketPaint);
        canvas.drawLine(scanRect.left, scanRect.bottom - cornerSize, scanRect.left, scanRect.bottom - radius, bracketPaint);

        // Bottom-Right Corner
        RectF arcBR = new RectF(scanRect.right - radius * 2, scanRect.bottom - radius * 2, scanRect.right, scanRect.bottom);
        canvas.drawArc(arcBR, 0, 90, false, bracketPaint);
        canvas.drawLine(scanRect.right - cornerSize, scanRect.bottom, scanRect.right - radius, scanRect.bottom, bracketPaint);
        canvas.drawLine(scanRect.right, scanRect.bottom - cornerSize, scanRect.right, scanRect.bottom - radius, bracketPaint);

        if (!isScanning) return;

        // 3. Moving Scan Laser Line
        canvas.drawLine(scanRect.left + 15, scanLineY, scanRect.right - 15, scanLineY, linePaint);

        // 4. Floating AI Dots
        for (PointFHolder pt : detectionPoints) {
            canvas.drawCircle(pt.x, pt.y, 8f, dotPaint);
        }
    }

    private static class PointFHolder {
        float x, y;
        PointFHolder(float x, float y) { this.x = x; this.y = y; }
    }
}
