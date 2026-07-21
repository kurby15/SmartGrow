package com.example.smartgrow;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

public class GetStartedActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 🔄 FIX: Gamitin ang SharedPrefManager para sa tamang login check
        if (SharedPrefManager.getInstance(this).isLoggedIn()) {
            startActivity(new Intent(GetStartedActivity.this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_get_started);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        ImageView imgLogo = findViewById(R.id.img_logo);
        TextView tvAppName = findViewById(R.id.tv_app_name); 
        TextView tvTagline = findViewById(R.id.tv_tagline);
        MaterialButton btnGetStarted = findViewById(R.id.btn_get_started);

        ImageView leafTop = findViewById(R.id.leaf_bg_top);
        ImageView leafBottom = findViewById(R.id.leaf_bg_bottom);
        View viewGlow = findViewById(R.id.view_glow);

        if (leafTop != null) startFloatingAnimation(leafTop, 15f, 4000);
        if (leafBottom != null) startFloatingAnimation(leafBottom, -15f, 4500);

        if (viewGlow != null) {
            AlphaAnimation glowPulse = new AlphaAnimation(0.15f, 0.45f);
            glowPulse.setDuration(1600);
            glowPulse.setRepeatMode(Animation.REVERSE);
            glowPulse.setRepeatCount(Animation.INFINITE);
            viewGlow.startAnimation(glowPulse);
        }

        Animation fadeSlideUpFast = AnimationUtils.loadAnimation(this, R.anim.fade_slide_up_fast);
        Animation fadeSlideUpDelayed = AnimationUtils.loadAnimation(this, R.anim.fade_slide_up_delayed);
        final Animation floatingLogo = AnimationUtils.loadAnimation(this, R.anim.floating_effect);

        if (imgLogo != null) imgLogo.startAnimation(fadeSlideUpFast);
        if (tvAppName != null) tvAppName.startAnimation(fadeSlideUpFast);
        if (tvTagline != null) tvTagline.startAnimation(fadeSlideUpDelayed);
        if (btnGetStarted != null) btnGetStarted.startAnimation(fadeSlideUpDelayed);

        fadeSlideUpFast.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                if (imgLogo != null) {
                    imgLogo.startAnimation(floatingLogo);
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        if (btnGetStarted != null) {
            btnGetStarted.setOnClickListener(v -> {
                Intent intent = new Intent(GetStartedActivity.this, LoginActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }
    }

    private void startFloatingAnimation(View view, float translationY, int duration) {
        android.animation.ObjectAnimator animator = android.animation.ObjectAnimator.ofFloat(
                view, "translationY", 0f, translationY);
        animator.setDuration(duration);
        animator.setRepeatMode(android.animation.ValueAnimator.REVERSE);
        animator.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.start();
    }
}
