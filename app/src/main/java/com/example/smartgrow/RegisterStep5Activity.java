package com.example.smartgrow;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

public class RegisterStep5Activity extends AppCompatActivity {

    private Dialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION.SDK_INT) {
            android.view.Window window = getWindow();


            window.setFlags(
                    android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            );
        }
        setContentView(R.layout.activity_register_step5);


        ImageButton btnBack = findViewById(R.id.btn_register_back);
        MaterialButton btnSignUp = findViewById(R.id.btn_signup);

        EditText etUsername = findViewById(R.id.et_username);
        EditText etFullName = findViewById(R.id.et_fullname);
        EditText etPassword = findViewById(R.id.et_password);
        EditText etConfirmPassword = findViewById(R.id.et_confirm_password);


        setupLoadingDialog();


        btnBack.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });


        btnSignUp.setOnClickListener(v -> {
            String user = etUsername.getText().toString().trim();
            String pass = etPassword.getText().toString().trim();
            String confirm = etConfirmPassword.getText().toString().trim();

            if (user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!pass.equals(confirm)) {
                Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show();
                return;
            }

            loadingDialog.show();


            TextView tvPercentage = loadingDialog.findViewById(R.id.tv_progress_percentage);
            TextView tvLoadingMessage = loadingDialog.findViewById(R.id.tv_loading_message);


            android.widget.ImageView leaf1 = loadingDialog.findViewById(R.id.leaf_bottom_left);
            android.widget.ImageView leaf2 = loadingDialog.findViewById(R.id.leaf_top_right);
            android.widget.ImageView leaf3 = loadingDialog.findViewById(R.id.leaf_top_left);
            android.widget.ImageView leaf4 = loadingDialog.findViewById(R.id.leaf_bottom_right);


            android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofInt(0, 100);
            animator.setDuration(4000);
            animator.addUpdateListener(animation -> {
                int progressValue = (int) animation.getAnimatedValue();
                tvPercentage.setText(progressValue + "%");


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


                if (progressValue == 20) {
                    sproutLeafAnimation(leaf1);
                } else if (progressValue == 45) {
                    sproutLeafAnimation(leaf2);
                } else if (progressValue == 70) {
                    sproutLeafAnimation(leaf3);
                } else if (progressValue == 90) {
                    sproutLeafAnimation(leaf4);
                }
            });

            animator.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    loadingDialog.dismiss();
                    Toast.makeText(RegisterStep5Activity.this, "Account Created Successfully!", Toast.LENGTH_SHORT).show();

                    // Intent intent = new Intent(RegisterStep5Activity.this, MainActivity.class);
                    // startActivity(intent);
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                    finishAffinity();
                }
            });

            animator.start();
        });

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
    }

   
    private void sproutLeafAnimation(android.widget.ImageView leaf) {
        leaf.setVisibility(android.view.View.VISIBLE);
        leaf.setScaleX(0f);
        leaf.setScaleY(0f);
        leaf.setAlpha(0f);


        leaf.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(500)
                .setInterpolator(new android.view.animation.OvershootInterpolator())
                .start();
    }
}
