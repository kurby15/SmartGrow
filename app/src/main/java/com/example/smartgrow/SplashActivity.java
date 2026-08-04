package com.example.smartgrow;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        View layoutText = findViewById(R.id.layout_text);
        View viewGlow = findViewById(R.id.view_background_aura);

        if (layoutText != null) {
            AlphaAnimation fadeInText = new AlphaAnimation(0.0f, 1.0f);
            fadeInText.setDuration(1000);
            layoutText.startAnimation(fadeInText);
        }

        if (viewGlow != null) {
            AlphaAnimation glowPulse = new AlphaAnimation(0.15f, 0.45f);
            glowPulse.setDuration(1500);
            glowPulse.setRepeatMode(Animation.REVERSE);
            glowPulse.setRepeatCount(Animation.INFINITE);
            viewGlow.startAnimation(glowPulse);
        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, GetStartedActivity.class);
            startActivity(intent);

            // Fixing deprecated overridePendingTransition for API 34+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, android.R.anim.fade_in, android.R.anim.fade_out, 0);
            } else {
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
            
            finish();
        }, 3500);
    }
}
