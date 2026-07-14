package com.example.smartgrow;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterStep5Activity extends AppCompatActivity {

    private Dialog loadingDialog;
    private User userData;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_step5);

        // Initialize Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference("users");

        // Kunin ang naipong data mula sa Step 4
        userData = (User) getIntent().getSerializableExtra("user_data");

        // 🔗 Bind UI Components
        ImageButton btnBack = findViewById(R.id.btn_register_back);
        MaterialButton btnSignUp = findViewById(R.id.btn_signup);

        EditText etUsername = findViewById(R.id.et_username);
        EditText etFullName = findViewById(R.id.et_fullname);
        EditText etEmail = findViewById(R.id.et_email);

        // Inayos ang casting papuntang TextInputEditText base sa XML declaration
        TextInputEditText etPassword = findViewById(R.id.et_password);
        TextInputEditText etConfirmPassword = findViewById(R.id.et_confirm_password);

        // I-initialize ang custom loading animation dialog
        setupLoadingDialog();

        // Slide animation pabalik sa Step 4
        btnBack.setOnClickListener(v -> goBack());

        // Main Action: Simulan ang account creation at animation
        btnSignUp.setOnClickListener(v -> {
            String user = etUsername.getText().toString().trim();
            String fullName = etFullName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();

            String pass = "";
            String confirm = "";

            if (etPassword.getText() != null) {
                pass = etPassword.getText().toString().trim();
            }
            if (etConfirmPassword.getText() != null) {
                confirm = etConfirmPassword.getText().toString().trim();
            }

            // Form validations
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

            // I-disable muna ang sign-up button para iwas-double tap glitch habang naglo-load
            btnSignUp.setEnabled(false);

            // I-update ang User Object
            if (userData == null) {
                userData = new User();
            }
            userData.setUsername(user);
            userData.setFullName(fullName);
            userData.setEmail(email);
            userData.setPassword(pass);

            // Simulan ang custom loading screen
            loadingDialog.show();

            TextView tvPercentage = loadingDialog.findViewById(R.id.tv_progress_percentage);
            TextView tvLoadingMessage = loadingDialog.findViewById(R.id.tv_loading_message);

            ImageView leaf1 = loadingDialog.findViewById(R.id.leaf_bottom_left);
            ImageView leaf2 = loadingDialog.findViewById(R.id.leaf_top_right);
            ImageView leaf3 = loadingDialog.findViewById(R.id.leaf_top_left);
            ImageView leaf4 = loadingDialog.findViewById(R.id.leaf_bottom_right);

            // 📈 Core Progress Animation Engine (0 to 100 within 4 seconds)
            ValueAnimator animator = ValueAnimator.ofInt(0, 100);
            animator.setDuration(4000);
            animator.addUpdateListener(animation -> {
                int progressValue = (int) animation.getAnimatedValue();
                if (tvPercentage != null) {
                    tvPercentage.setText(String.format("%d%%", progressValue));
                }

                if (tvLoadingMessage != null) {
                    if (progressValue <= 25) {
                        tvLoadingMessage.setText("Creating your account...");
                    } else if (progressValue <= 50) {
                        tvLoadingMessage.setText("Setting up your dashboard...");
                    } else if (progressValue <= 75) {
                        tvLoadingMessage.setText("Readying your AI companion...");
                    } else if (progressValue <= 95) {
                        tvLoadingMessage.setText("Almost there! Finalizing updates...");
                    } else {
                        tvLoadingMessage.setText("Start chatting soon! 🎉");
                    }
                }

                // Sequential leaf sprouting benchmarks
                if (progressValue >= 20) sproutLeafAnimation(leaf1);
                if (progressValue >= 45) sproutLeafAnimation(leaf2);
                if (progressValue >= 70) sproutLeafAnimation(leaf3);
                if (progressValue >= 90) sproutLeafAnimation(leaf4);
            });

            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);

                    // I-save ang kabuuang user registration data sa Firebase Database
                    databaseReference.child(user).setValue(userData).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            if (loadingDialog != null && loadingDialog.isShowing()) {
                                loadingDialog.dismiss();
                            }

                            Toast.makeText(RegisterStep5Activity.this, "Account Created Successfully!", Toast.LENGTH_SHORT).show();

                            // Dadalhin na ang user sa Login Activity
                            Intent intent = new Intent(RegisterStep5Activity.this, LoginActivity.class);
                            startActivity(intent);
                            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);

                            // Lilinisin ang backstack para hindi na makabalik ang user sa registration gamit ang back button
                            finishAffinity();
                        } else {
                            if (loadingDialog != null && loadingDialog.isShowing()) {
                                loadingDialog.dismiss();
                            }
                            // Re-enable button para makasubok ulit sakaling mag-fail ang connection/Firebase
                            btnSignUp.setEnabled(true);
                            Toast.makeText(RegisterStep5Activity.this, "Failed to create account: " +
                                            (task.getException() != null ? task.getException().getMessage() : "Unknown Error"),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });

            animator.start();
        });

        // Ligtas na pag-handle sa physical back button ng device
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                goBack();
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

        leaf.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(500)
                .setInterpolator(new OvershootInterpolator())
                .start();
    }

    private void goBack() {
        finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}