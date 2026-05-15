package com.dangngu.myapplication;

import android.app.Activity;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DURATION_MS = 4700L;
    private static final String BOOT_TEXT = "BOOTING GESTURE-DRIVEN IOT CORE...";

    private final Handler handler = new Handler(Looper.getMainLooper());

    private Runnable launchRunnable;
    private Runnable typingRunnable;
    private int typingIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        View splashCard = findViewById(R.id.splashCard);
        TextView bootTitle = findViewById(R.id.tvBootTitle);
        TextView tvTypewriter = findViewById(R.id.tvTypewriter);

        splashCard.setAlpha(0f);
        splashCard.setScaleX(0.86f);
        splashCard.setScaleY(0.86f);
        splashCard.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setInterpolator(new OvershootInterpolator(0.8f))
                .setDuration(1800)
                .start();

        ObjectAnimator glowAnimator = ObjectAnimator.ofFloat(bootTitle, View.ALPHA, 0.45f, 1f);
        glowAnimator.setDuration(1200);
        glowAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        glowAnimator.setRepeatMode(ObjectAnimator.REVERSE);
        glowAnimator.start();

        startTypewriter(tvTypewriter);

        launchRunnable = () -> {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            applyOpenTransition();
            finish();
        };
        handler.postDelayed(launchRunnable, SPLASH_DURATION_MS);
    }

    @SuppressWarnings("deprecation")
    private void applyOpenTransition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                    Activity.OVERRIDE_TRANSITION_OPEN,
                    R.anim.activity_open_enter,
                    R.anim.activity_open_exit
            );
        } else {
            overridePendingTransition(R.anim.activity_open_enter, R.anim.activity_open_exit);
        }
    }

    private void startTypewriter(TextView outputView) {
        outputView.setText("");
        typingIndex = 0;
        typingRunnable = new Runnable() {
            @Override
            public void run() {
                if (typingIndex > BOOT_TEXT.length()) {
                    return;
                }
                outputView.setText(BOOT_TEXT.substring(0, typingIndex));
                typingIndex++;
                handler.postDelayed(this, 45L);
            }
        };
        handler.post(typingRunnable);
    }

    @Override
    protected void onDestroy() {
        if (launchRunnable != null) {
            handler.removeCallbacks(launchRunnable);
        }
        if (typingRunnable != null) {
            handler.removeCallbacks(typingRunnable);
        }
        super.onDestroy();
    }
}
