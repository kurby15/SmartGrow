package com.example.smartgrow;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterStep5Activity extends AppCompatActivity {

    private Dialog loadingDialog;
    private User userData;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference("users");

        // Get user data from previous step
        userData = (User) getIntent().getSerializableExtra("user_data");

        setContentView(R.layout.activity_register_step5);

        // 🔗 Bind UI Components
        ImageButton btnBack = findViewById(R.id.btn_register_back);
        MaterialButton btnSignUp = findViewById(R.id.btn_signup);

        EditText etUsername = findViewById(R.id.et_username);
        EditText etFullName = findViewById(R.id.et_fullname);
        EditText etEmail = findViewById(R.id.et_email);
        EditText etPassword = findViewById(R.id.et_password);
        EditText etConfirmPassword = findViewById(R.id.et_confirm_password);

        // Initialize the beautiful customized dialog box
        setupLoadingDialog();

        // Left sliding transition kapag bumalik sa nakaraang step
        btnBack.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });

        // Main Action: Trigger account computation and layout transitions
        btnSignUp.setOnClickListener(v -> {
            String user = etUsername.getText().toString().trim();
            String fullName = etFullName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String pass = etPassword.getText().toString().trim();
            String confirm = etConfirmPassword.getText().toString().trim();

            // Form validation checks
            if (user.isEmpty() || fullName.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!pass.equals(confirm)) {
                Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show();
                return;
            }

            // 🛑 PROTEKSYON: I-disable muna ang sign up para maiwasan ang double-tap glitch habang naglo-load
            btnSignUp.setEnabled(false);

            // Update userData
            if (userData == null) userData = new User();
            userData.setUsername(user);
            userData.setFullName(fullName);
            userData.setEmail(email);
            userData.setPassword(pass);

            // Buksan ang custom progress dialog engine
            loadingDialog.show();

            TextView tvPercentage = loadingDialog.findViewById(R.id.tv_progress_percentage);
            TextView tvLoadingMessage = loadingDialog.findViewById(R.id.tv_loading_message);

            ImageView leaf1 = loadingDialog.findViewById(R.id.leaf_bottom_left);
            ImageView leaf2 = loadingDialog.findViewById(R.id.leaf_top_right);
            ImageView leaf3 = loadingDialog.findViewById(R.id.leaf_top_left);
            ImageView leaf4 = loadingDialog.findViewById(R.id.leaf_bottom_right);

            // 📈 Core Animation Engine: Counts 0 to 100 within 4 full seconds
            ValueAnimator animator = ValueAnimator.ofInt(0, 100);
            animator.setDuration(4000);
            animator.addUpdateListener(animation -> {
                int progressValue = (int) animation.getAnimatedValue();
                if (tvPercentage != null) tvPercentage.setText(progressValue + "%");

                // Dynamic updates reflecting current app features (Dashboard, Care Assistant, Plants Log)
                if (tvLoadingMessage != null) {
                    if (progressValue >= 0 && progressValue <= 25) {
                        tvLoadingMessage.setText("Creating your account...");
                    } else if (progressValue > 25 && progressValue <= 50) {
                        tvLoadingMessage.setText("Setting up your dashboard...");
                    } else if (progressValue > 50 && progressValue <= 75) {
                        tvLoadingMessage.setText("Readying your AI companion...");
                    } else if (progressValue > 75 && progressValue <= 95) {
                        tvLoadingMessage.setText("Almost there! Finalizing updates...");
                    } else if (progressValue > 95) {
                        tvLoadingMessage.setText("Start chatting soon! 🎉");
                    }
                }

                // Sequential leaf sprouting benchmarks - Using ranges to handle frame skips
                if (progressValue >= 20) sproutLeafAnimation(leaf1);
                if (progressValue >= 45) sproutLeafAnimation(leaf2);
                if (progressValue >= 70) sproutLeafAnimation(leaf3);
                if (progressValue >= 90) sproutLeafAnimation(leaf4);
            });

            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);

                    // Save to Firebase
                    databaseReference.child(user).setValue(userData).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Isara ang loading frame nang ligtas
                            if (loadingDialog != null && loadingDialog.isShowing()) {
                                loadingDialog.dismiss();
                            }

                            Toast.makeText(RegisterStep5Activity.this, "Account Created Successfully!", Toast.LENGTH_SHORT).show();

                            // 🚀 SUCCESS TRANSITION: Pag-lipat papuntang MainActivity na may kalakip na clear stack protection
                            Intent intent = new Intent(RegisterStep5Activity.this, LoginActivity.class);
                            startActivity(intent);
                            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);

                            // Sinisigurong burado ang lahat ng registration steps sa background stack para hindi na pwedeng i-back ng user
                            finishAffinity();
                        } else {
                            if (loadingDialog != null && loadingDialog.isShowing()) {
                                loadingDialog.dismiss();
                            }
                            btnSignUp.setEnabled(true);
                            Toast.makeText(RegisterStep5Activity.this, "Failed to create account: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });

            animator.start();
        });

        // Handle structural system device hardware back key presses safely
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            }
        });
    }

    private void setupLoadingDialog() {
        loadingDialog = new Dialog(this, R.style.CustomLoadingDialog);
        loadingDialog.setContentView(R.layout.dialog_loading);
        loadingDialog.setCancelable(false);
        if (loadingDialog.getWindow() != null) {
            loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    private void sproutLeafAnimation(ImageView leaf) {
        if (leaf == null || leaf.getVisibility() == View.VISIBLE) return;

        leaf.setVisibility(View.VISIBLE);
        leaf.setScaleX(0f);
        leaf.setScaleY(0f);
        leaf.setAlpha(0f);

        // Smooth spring physics overshoot leaf animation logic
        leaf.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(500)
                .setInterpolator(new OvershootInterpolator())
                .start();
    }
}