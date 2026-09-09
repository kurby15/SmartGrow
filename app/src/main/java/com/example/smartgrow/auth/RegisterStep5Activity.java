package com.example.smartgrow.auth;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smartgrow.R;
import com.example.smartgrow.profile.User;
import com.example.smartgrow.utils.FirebaseCryptoUtils;
import com.google.android.material.button.MaterialButton;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class RegisterStep5Activity extends AppCompatActivity {

    private EditText etUsername, etFullName, etEmail, etPassword, etConfirmPassword, etPhone, etAddress;
    private LinearLayout layoutPasswordRequirements;
    private TextView tvReqLength, tvReqUppercase, tvReqNumber, tvReqSpecial;
    private MaterialButton btnSignUp;
    private User userData;

    // Loading Dialog Component
    private Dialog loadingDialog;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Strict Password Validation Regex Patterns
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile(".*[A-Z].*");
    private static final Pattern NUMBER_PATTERN = Pattern.compile(".*[0-9].*");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*");

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_step5);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Retrieve user data from previous onboarding steps
        userData = (User) getIntent().getSerializableExtra("user_data");

        // Bind layout views
        etUsername = findViewById(R.id.et_signup_username);
        etFullName = findViewById(R.id.et_signup_fullname);
        etEmail = findViewById(R.id.et_signup_email);
        etPassword = findViewById(R.id.et_signup_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        etPhone = findViewById(R.id.et_signup_phone);
        etAddress = findViewById(R.id.et_signup_address);

        // Bind layout views for password requirements
        layoutPasswordRequirements = findViewById(R.id.layout_password_requirements);
        tvReqLength = findViewById(R.id.tv_req_length);
        tvReqUppercase = findViewById(R.id.tv_req_uppercase);
        tvReqNumber = findViewById(R.id.tv_req_number);
        tvReqSpecial = findViewById(R.id.tv_req_special);

        // Hide requirement container by default on load
        if (layoutPasswordRequirements != null) {
            layoutPasswordRequirements.setVisibility(View.GONE);
        }

        btnSignUp = findViewById(R.id.btn_sign_up);
        ImageView btnBack = findViewById(R.id.btn_register_back);

        setupLoadingDialog();

        // Setup real-time dynamic password requirement feedback
        setupPasswordRealTimeValidation();

        btnSignUp.setOnClickListener(v -> validateAndSubmitForm());
        btnBack.setOnClickListener(v -> goBack());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                goBack();
            }
        });
    }

    private void setupPasswordRealTimeValidation() {
        if (etPassword == null) return;

        // Show requirements container when user focuses on the password field
        etPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                String currentPassword = etPassword.getText().toString();
                if (!isPasswordFullyValid(currentPassword)) {
                    layoutPasswordRequirements.setVisibility(View.VISIBLE);
                }
            } else {
                // Hide container when user focuses out
                layoutPasswordRequirements.setVisibility(View.GONE);
            }
        });

        // Dynamic checks as user types
        etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updatePasswordRequirementsList(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void updatePasswordRequirementsList(String password) {
        if (layoutPasswordRequirements == null) return;

        if (etPassword.hasFocus() && !isPasswordFullyValid(password)) {
            layoutPasswordRequirements.setVisibility(View.VISIBLE);
        }

        boolean lengthValid = password.length() >= 8 && password.length() <= 12;
        tvReqLength.setVisibility(lengthValid ? View.GONE : View.VISIBLE);

        boolean uppercaseValid = UPPERCASE_PATTERN.matcher(password).matches();
        tvReqUppercase.setVisibility(uppercaseValid ? View.GONE : View.VISIBLE);

        boolean numberValid = NUMBER_PATTERN.matcher(password).matches();
        tvReqNumber.setVisibility(numberValid ? View.GONE : View.VISIBLE);

        boolean specialValid = SPECIAL_CHAR_PATTERN.matcher(password).matches();
        tvReqSpecial.setVisibility(specialValid ? View.GONE : View.VISIBLE);

        if (lengthValid && uppercaseValid && numberValid && specialValid) {
            layoutPasswordRequirements.setVisibility(View.GONE);
        }
    }

    private boolean isPasswordFullyValid(String password) {
        return (password.length() >= 8 && password.length() <= 12)
                && UPPERCASE_PATTERN.matcher(password).matches()
                && NUMBER_PATTERN.matcher(password).matches()
                && SPECIAL_CHAR_PATTERN.matcher(password).matches();
    }

    private void validateAndSubmitForm() {
        String username = etUsername != null ? etUsername.getText().toString().trim() : "";
        String fullName = etFullName != null ? etFullName.getText().toString().trim() : "";
        String email = etEmail != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword != null ? etPassword.getText().toString() : "";
        String confirmPassword = etConfirmPassword != null ? etConfirmPassword.getText().toString() : "";
        String phone = etPhone != null ? etPhone.getText().toString().trim() : "";
        String address = etAddress != null ? etAddress.getText().toString().trim() : "";

        if (username.isEmpty()) {
            etUsername.setError("Username is required");
            etUsername.requestFocus();
            return;
        }

        if (fullName.isEmpty()) {
            etFullName.setError("Full name is required");
            etFullName.requestFocus();
            return;
        }

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Valid email is required");
            etEmail.requestFocus();
            return;
        }

        if (!isPasswordFullyValid(password)) {
            layoutPasswordRequirements.setVisibility(View.VISIBLE);
            etPassword.requestFocus();
            Toast.makeText(this, "Password does not meet all requirements", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return;
        }

        btnSignUp.setEnabled(false);

        String choice1 = (userData != null) ? userData.getChoice1() : "";
        String choice2 = (userData != null) ? userData.getChoice2() : "";
        String choice3 = (userData != null) ? userData.getChoice3() : "";
        String choice4 = (userData != null) ? userData.getChoice4() : "";

        loadingDialog.show();

        TextView tvPercentage = loadingDialog.findViewById(R.id.tv_progress_percentage);
        TextView tvLoadingMessage = loadingDialog.findViewById(R.id.tv_loading_message);

        ImageView leaf1 = loadingDialog.findViewById(R.id.leaf_bottom_left);
        ImageView leaf2 = loadingDialog.findViewById(R.id.leaf_top_right);
        ImageView leaf3 = loadingDialog.findViewById(R.id.leaf_top_left);
        ImageView leaf4 = loadingDialog.findViewById(R.id.leaf_bottom_right);

        resetLeaves(leaf1, leaf2, leaf3, leaf4);

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

            if (progressValue >= 20) sproutLeafAnimation(leaf1);
            if (progressValue >= 45) sproutLeafAnimation(leaf2);
            if (progressValue >= 70) sproutLeafAnimation(leaf3);
            if (progressValue >= 90) sproutLeafAnimation(leaf4);
        });

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);

                registerUser(username, fullName, email, password, phone, address, choice1, choice2, choice3, choice4);
            }
        });

        animator.start();
    }

    private void registerUser(String username, String fullName, String email, String password,
                              String phone, String address, String choice1, String choice2,
                              String choice3, String choice4) {

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                        String uid = mAuth.getCurrentUser().getUid();

                        String encryptedUsername = FirebaseCryptoUtils.encrypt(username, uid);
                        String encryptedEmail = FirebaseCryptoUtils.encrypt(email, uid);
                        String encryptedFullName = FirebaseCryptoUtils.encrypt(fullName, uid);
                        String encryptedPhone = FirebaseCryptoUtils.encrypt(phone, uid);
                        String encryptedAddress = FirebaseCryptoUtils.encrypt(address, uid);
                        String encryptedPassword = FirebaseCryptoUtils.encrypt(password, uid);

                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("uid", uid);
                        userMap.put("username", encryptedUsername);
                        userMap.put("fullName", encryptedFullName);
                        userMap.put("email", encryptedEmail);
                        userMap.put("password", encryptedPassword);
                        userMap.put("phone", encryptedPhone);
                        userMap.put("address", encryptedAddress);
                        userMap.put("choice1", choice1);
                        userMap.put("choice2", choice2);
                        userMap.put("choice3", choice3);
                        userMap.put("choice4", choice4);
                        userMap.put("profilePic", "");
                        userMap.put("bio", "");
                        userMap.put("followersCount", 0);
                        userMap.put("followingCount", 0);
                        userMap.put("followers", new HashMap<String, Boolean>());
                        userMap.put("following", new HashMap<String, Boolean>());

                        db.collection("users").document(uid)
                                .set(userMap)
                                .addOnSuccessListener(aVoid -> {
                                    mAuth.signOut();
                                    dismissLoadingDialog();

                                    Toast.makeText(RegisterStep5Activity.this, "Account Created Successfully!", Toast.LENGTH_SHORT).show();

                                    Intent intent = new Intent(RegisterStep5Activity.this, LoginActivity.class);
                                    startActivity(intent);
                                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                                    finishAffinity();
                                })
                                .addOnFailureListener(e -> {
                                    if (isFinishing()) return;
                                    dismissLoadingDialog();
                                    btnSignUp.setEnabled(true);
                                    Toast.makeText(RegisterStep5Activity.this, "Database error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        if (isFinishing()) return;
                        dismissLoadingDialog();
                        btnSignUp.setEnabled(true);
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Authentication failed.";
                        Toast.makeText(RegisterStep5Activity.this, "Auth failed: " + errorMsg, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /* --- Custom Loading Dialog Setup & Leaf Animation --- */

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

    private void resetLeaves(ImageView... leaves) {
        for (ImageView leaf : leaves) {
            if (leaf != null) {
                leaf.setVisibility(View.INVISIBLE);
                leaf.setScaleX(0f);
                leaf.setScaleY(0f);
                leaf.setAlpha(0f);
            }
        }
    }

    private void dismissLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    private void goBack() {
        finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dismissLoadingDialog();
    }
}