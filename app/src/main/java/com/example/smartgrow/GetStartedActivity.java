package com.example.smartgrow;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import android.content.SharedPreferences;

public class GetStartedActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if user is already logged in
        SharedPreferences preferences = getSharedPreferences("SmartGrowPrefs", MODE_PRIVATE);
        if (preferences.getBoolean("is_logged_in", false)) {
            Intent intent = new Intent(GetStartedActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // 🌟 1. Transparent status bar para sumabay sa kulay ng iyong background gradient
        Window window = getWindow();
        window.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );
        setContentView(R.layout.activity_get_started);

        // Itago ang default action bar kung mayroon man
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // 🌿 2. Bind ng iyong Original Views
        ImageView imgLogo = findViewById(R.id.img_logo);
        LinearLayout layoutLogoTitle = findViewById(R.id.layout_logo_title);
        TextView tvTagline = findViewById(R.id.tv_tagline);
        MaterialButton btnGetStarted = findViewById(R.id.btn_get_started);

        // 🍃 3. Bind ng mga Bagong Dagdag na Decorative Views
        ImageView leafTop = findViewById(R.id.leaf_bg_top);
        ImageView leafBottom = findViewById(R.id.leaf_bg_bottom);
        View viewGlow = findViewById(R.id.view_glow);

        // 🔄 4. Patakbuhin AGAD ang banayad na floating loops para sa mga dahon sa likod
        if (leafTop != null) startFloatingAnimation(leafTop, 15f, 4000);
        if (leafBottom != null) startFloatingAnimation(leafBottom, -15f, 4500);

        // 🌞 5. Buhayin ang umiindayog o nag-p-pulse na solar glow ring sa likod ng logo niyo
        if (viewGlow != null) {
            AlphaAnimation glowPulse = new AlphaAnimation(0.15f, 0.45f);
            glowPulse.setDuration(1600);
            glowPulse.setRepeatMode(Animation.REVERSE);
            glowPulse.setRepeatCount(Animation.INFINITE);
            viewGlow.startAnimation(glowPulse);
        }

        // 🎬 6. I-load ang iyong mga orihinal na XML Animations mula sa res/anim
        Animation fadeSlideUpFast = AnimationUtils.loadAnimation(this, R.anim.fade_slide_up_fast);
        Animation fadeSlideUpDelayed = AnimationUtils.loadAnimation(this, R.anim.fade_slide_up_delayed);
        final Animation floatingLogo = AnimationUtils.loadAnimation(this, R.anim.floating_effect);

        // 🚀 7. Patakbuhin ang iyong mga orihinal na Text at Button Animations
        if (layoutLogoTitle != null) layoutLogoTitle.startAnimation(fadeSlideUpFast);
        if (tvTagline != null) tvTagline.startAnimation(fadeSlideUpDelayed);
        if (btnGetStarted != null) btnGetStarted.startAnimation(fadeSlideUpDelayed);

        // 🔄 8. Iyong orihinal na Animation Listener para sa logo
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

        // 🖱️ 9. Iyong orihinal na Click Listener papuntang Login Screen
        if (btnGetStarted != null) {
            btnGetStarted.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(GetStartedActivity.this, LoginActivity.class);
                    startActivity(intent);
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                }
            });
        }
    }

    // ✨ Ang helper function para sa walang katapusang dahan-dahang paggalaw ng mga dahon sa background
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