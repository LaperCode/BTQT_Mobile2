package com.dangngu.myapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.Random;

public class MatrixRainView extends View {

    private static final int FRAME_DELAY_MS = 42;
    private static final char[] CHARSET = "01ABCDEFGHIJKLMNOPQRSTUVWXYZ#$%&*+-?".toCharArray();

    private final Paint leadPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trailPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random();

    private float textSizePx;
    private int columnCount;
    private float[] yPositions = new float[0];
    private float[] speeds = new float[0];
    private int[] tailLengths = new int[0];
    private boolean running;

    private final Runnable frameRunnable = new Runnable() {
        @Override
        public void run() {
            if (!running) {
                return;
            }
            updateMatrix();
            invalidate();
            postDelayed(this, FRAME_DELAY_MS);
        }
    };

    public MatrixRainView(Context context) {
        super(context);
        init();
    }

    public MatrixRainView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MatrixRainView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        textSizePx = dp(15f);
        leadPaint.setColor(0xFF6CFFB6);
        leadPaint.setTextSize(textSizePx);
        leadPaint.setFakeBoldText(true);

        trailPaint.setColor(0xFF21A55F);
        trailPaint.setTextSize(textSizePx);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w <= 0 || h <= 0) {
            return;
        }
        columnCount = Math.max(12, (int) (w / (textSizePx * 0.82f)));
        yPositions = new float[columnCount];
        speeds = new float[columnCount];
        tailLengths = new int[columnCount];

        for (int i = 0; i < columnCount; i++) {
            resetColumn(i, h);
        }
    }

    private void resetColumn(int index, int height) {
        yPositions[index] = -(random.nextFloat() * height);
        speeds[index] = dp(2.6f + random.nextFloat() * 4.6f);
        tailLengths[index] = 7 + random.nextInt(10);
    }

    private void updateMatrix() {
        int height = getHeight();
        if (height <= 0 || yPositions.length == 0) {
            return;
        }
        for (int i = 0; i < yPositions.length; i++) {
            yPositions[i] += speeds[i];
            if (yPositions[i] - (tailLengths[i] * textSizePx) > height + textSizePx) {
                resetColumn(i, height);
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (yPositions.length == 0) {
            return;
        }

        float xStep = getWidth() / (float) columnCount;
        for (int column = 0; column < columnCount; column++) {
            float x = (column * xStep) + dp(2f);
            for (int i = 0; i < tailLengths[column]; i++) {
                float y = yPositions[column] - (i * textSizePx);
                if (y < -textSizePx || y > getHeight() + textSizePx) {
                    continue;
                }
                char c = CHARSET[random.nextInt(CHARSET.length)];
                if (i == 0) {
                    leadPaint.setAlpha(245);
                    canvas.drawText(String.valueOf(c), x, y, leadPaint);
                } else {
                    int alpha = Math.max(30, 180 - (i * 16));
                    trailPaint.setAlpha(alpha);
                    canvas.drawText(String.valueOf(c), x, y, trailPaint);
                }
            }
        }
    }

    public void start() {
        if (running) {
            return;
        }
        running = true;
        removeCallbacks(frameRunnable);
        post(frameRunnable);
    }

    public void stop() {
        running = false;
        removeCallbacks(frameRunnable);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        start();
    }

    @Override
    protected void onDetachedFromWindow() {
        stop();
        super.onDetachedFromWindow();
    }
}
