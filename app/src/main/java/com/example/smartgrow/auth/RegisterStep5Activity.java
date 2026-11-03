package com.example.smartgrow.auth;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smartgrow.R;
import com.example.smartgrow.core.SecurityUtils;
import com.example.smartgrow.core.SharedPrefManager;
import com.example.smartgrow.profile.User;
import com.example.smartgrow.utils.FirebaseCryptoUtils;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class RegisterStep5Activity extends AppCompatActivity {

    private EditText etFullName;
    private EditText etEmail;
    private EditText etPassword;
    private EditText etConfirmPassword;

    private LinearLayout layoutPasswordRequirements;

    private TextView tvReqLength;
    private TextView tvReqUppercase;
    private TextView tvReqNumber;
    private TextView tvReqSpecial;

    private TextView tvReqLengthIcon;
    private TextView tvReqUppercaseIcon;
    private TextView tvReqNumberIcon;
    private TextView tvReqSpecialIcon;

    private LinearLayout rowReqLength;
    private LinearLayout rowReqUppercase;
    private LinearLayout rowReqNumber;
    private LinearLayout rowReqSpecial;

    private MaterialButton btnSignUp;

    private User userData;

    private Dialog loadingDialog;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private static final Pattern UPPERCASE_PATTERN = Pattern.compile(".*[A-Z].*");
    private static final Pattern NUMBER_PATTERN = Pattern.compile(".*[0-9].*");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*");

    private static final int COLOR_INVALID = Color.parseColor("#E53935");
    private static final int COLOR_VALID = Color.parseColor("#43A047");
    private static final int COLOR_NEUTRAL = Color.parseColor("#9E9E9E");

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_step5);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        userData = (User) getIntent().getSerializableExtra("user_data");

        etFullName = findViewById(R.id.et_signup_fullname);
        etEmail = findViewById(R.id.et_signup_email);
        etPassword = findViewById(R.id.et_signup_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        btnSignUp = findViewById(R.id.btn_sign_up);

        layoutPasswordRequirements = findViewById(R.id.layout_password_requirements);
        tvReqLength = findViewById(R.id.tv_req_length);
        tvReqUppercase = findViewById(R.id.tv_req_uppercase);
        tvReqNumber = findViewById(R.id.tv_req_number);
        tvReqSpecial = findViewById(R.id.tv_req_special);

        tvReqLengthIcon = findViewById(R.id.tv_req_length_icon);
        tvReqUppercaseIcon = findViewById(R.id.tv_req_uppercase_icon);
        tvReqNumberIcon = findViewById(R.id.tv_req_number_icon);
        tvReqSpecialIcon = findViewById(R.id.tv_req_special_icon);

        rowReqLength = findViewById(R.id.row_req_length);
        rowReqUppercase = findViewById(R.id.row_req_uppercase);
        rowReqNumber = findViewById(R.id.row_req_number);
        rowReqSpecial = findViewById(R.id.row_req_special);

        if (layoutPasswordRequirements != null) {
            layoutPasswordRequirements.setVisibility(View.GONE);
            layoutPasswordRequirements.setAlpha(1f);
            layoutPasswordRequirements.setTranslationY(0f);
        }

        ImageView btnBack = findViewById(R.id.btn_register_back);
        setupLoadingDialog();
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
        etPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                showPasswordRequirements();
                updatePasswordRequirementsList(etPassword.getText().toString());
            } else if (isPasswordFullyValid(etPassword.getText().toString())) {
                hidePasswordRequirements();
            }
        });

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

    private void showPasswordRequirements() {
        if (layoutPasswordRequirements == null || layoutPasswordRequirements.getVisibility() == View.VISIBLE) return;
        layoutPasswordRequirements.setVisibility(View.VISIBLE);
        layoutPasswordRequirements.setAlpha(0f);
        layoutPasswordRequirements.setTranslationY(-8f);
        layoutPasswordRequirements.animate().alpha(1f).translationY(0f).setDuration(220).start();
    }

    private void updatePasswordRequirementsList(String password) {
        if (layoutPasswordRequirements == null) return;
        if (etPassword.hasFocus() && layoutPasswordRequirements.getVisibility() != View.VISIBLE) showPasswordRequirements();

        boolean isEmpty = password.isEmpty();
        boolean lengthValid = password.length() >= 8;
        boolean uppercaseValid = UPPERCASE_PATTERN.matcher(password).matches();
        boolean numberValid = NUMBER_PATTERN.matcher(password).matches();
        boolean specialValid = SPECIAL_CHAR_PATTERN.matcher(password).matches();

        if (isEmpty) {
            showRequirement(rowReqLength, tvReqLength, tvReqLengthIcon, false, true);
            showRequirement(rowReqUppercase, tvReqUppercase, tvReqUppercaseIcon, false, true);
            showRequirement(rowReqNumber, tvReqNumber, tvReqNumberIcon, false, true);
            showRequirement(rowReqSpecial, tvReqSpecial, tvReqSpecialIcon, false, true);
            return;
        }

        if (lengthValid) hideRequirement(rowReqLength);
        else showRequirement(rowReqLength, tvReqLength, tvReqLengthIcon, false, false);

        if (uppercaseValid) hideRequirement(rowReqUppercase);
        else showRequirement(rowReqUppercase, tvReqUppercase, tvReqUppercaseIcon, false, false);

        if (numberValid) hideRequirement(rowReqNumber);
        else showRequirement(rowReqNumber, tvReqNumber, tvReqNumberIcon, false, false);

        if (specialValid) hideRequirement(rowReqSpecial);
        else showRequirement(rowReqSpecial, tvReqSpecial, tvReqSpecialIcon, false, false);

        if (lengthValid && uppercaseValid && numberValid && specialValid) hidePasswordRequirements();
    }

    private void showRequirement(View row, TextView text, TextView icon, boolean valid, boolean neutral) {
        if (row == null || text == null || icon == null) return;
        if (row.getVisibility() != View.VISIBLE) {
            row.setAlpha(0f);
            row.setTranslationX(-8f);
            row.setVisibility(View.VISIBLE);
            row.animate().alpha(1f).translationX(0f).setDuration(180).start();
        }

        if (neutral) {
            text.setTextColor(COLOR_NEUTRAL);
            icon.setText("•");
            icon.setTextColor(COLOR_NEUTRAL);
            setRequirementIconBackground(icon, COLOR_NEUTRAL, false);
            return;
        }

        if (valid) {
            text.setTextColor(COLOR_VALID);
            icon.setText("✓");
            icon.setTextColor(Color.WHITE);
            setRequirementIconBackground(icon, COLOR_VALID, true);
            return;
        }

        text.setTextColor(COLOR_INVALID);
        icon.setText("!");
        icon.setTextColor(COLOR_INVALID);
        setRequirementIconBackground(icon, COLOR_INVALID, false);
    }

    private void hideRequirement(View row) {
        if (row == null || row.getVisibility() != View.VISIBLE) return;
        if ("hiding".equals(row.getTag())) return;
        row.setTag("hiding");
        row.animate().alpha(0f).translationX(10f).setDuration(180).withEndAction(() -> {
            row.setVisibility(View.GONE);
            row.setAlpha(1f);
            row.setTranslationX(0f);
            row.setTag(null);
        }).start();
    }

    private void hidePasswordRequirements() {
        if (layoutPasswordRequirements == null || layoutPasswordRequirements.getVisibility() != View.VISIBLE) return;
        layoutPasswordRequirements.animate().alpha(0f).translationY(-8f).setDuration(220).withEndAction(() -> {
            layoutPasswordRequirements.setVisibility(View.GONE);
            layoutPasswordRequirements.setAlpha(1f);
            layoutPasswordRequirements.setTranslationY(0f);
        }).start();
    }

    private void setRequirementIconBackground(TextView icon, int color, boolean filled) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        if (filled) drawable.setColor(color);
        else {
            drawable.setColor(Color.TRANSPARENT);
            drawable.setStroke(1, color);
        }
        icon.setBackground(drawable);
    }

    private boolean isPasswordFullyValid(String password) {
        return password.length() >= 8 && password.length() <= 12 &&
                UPPERCASE_PATTERN.matcher(password).matches() &&
                NUMBER_PATTERN.matcher(password).matches() &&
                SPECIAL_CHAR_PATTERN.matcher(password).matches();
    }

    private void validateAndSubmitForm() {
        String fullName = etFullName != null ? etFullName.getText().toString().trim() : "";
        String email = etEmail != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword != null ? etPassword.getText().toString() : "";
        String confirmPassword = etConfirmPassword != null ? etConfirmPassword.getText().toString() : "";

        if (fullName.isEmpty()) { etFullName.setError("Full name is required"); etFullName.requestFocus(); return; }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) { etEmail.setError("Valid email is required"); etEmail.requestFocus(); return; }
        if (!isPasswordFullyValid(password)) { showPasswordRequirements(); updatePasswordRequirementsList(password); etPassword.requestFocus(); return; }
        if (!password.equals(confirmPassword)) { etConfirmPassword.setError("Passwords do not match"); etConfirmPassword.requestFocus(); return; }

        btnSignUp.setEnabled(false);
        loadingDialog.show();
        
        ValueAnimator animator = ValueAnimator.ofInt(0, 100);
        animator.setDuration(4000);
        animator.addUpdateListener(animation -> {
            int progress = (int) animation.getAnimatedValue();
            updateLoadingUI(progress);
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                registerUser(fullName, email, password);
            }
        });
        animator.start();
    }

    private void updateLoadingUI(int progress) {
        TextView tvPercentage = loadingDialog.findViewById(R.id.tv_progress_percentage);
        TextView tvMsg = loadingDialog.findViewById(R.id.tv_loading_message);
        if (tvPercentage != null) tvPercentage.setText(String.format("%d%%", progress));
        if (tvMsg != null) {
            if (progress <= 25) tvMsg.setText("Creating your account...");
            else if (progress <= 50) tvMsg.setText("Setting up your dashboard...");
            else if (progress <= 75) tvMsg.setText("Readying your AI companion...");
            else tvMsg.setText("Almost there! 🎉");
        }
        if (progress >= 20) sproutLeafAnimation(loadingDialog.findViewById(R.id.leaf_bottom_left));
        if (progress >= 45) sproutLeafAnimation(loadingDialog.findViewById(R.id.leaf_top_right));
        if (progress >= 70) sproutLeafAnimation(loadingDialog.findViewById(R.id.leaf_top_left));
        if (progress >= 90) sproutLeafAnimation(loadingDialog.findViewById(R.id.leaf_bottom_right));
    }

    private void registerUser(String fullName, String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                String uid = mAuth.getCurrentUser().getUid();
                
                // SECURE CREDENTIAL PROTECTION:
                // 1. Sensitive data is ENCRYPTED (AES-256 GCM)
                String encryptedEmail = FirebaseCryptoUtils.encrypt(email, uid);
                String encryptedFullName = FirebaseCryptoUtils.encrypt(fullName, uid);
                
                // 2. Passwords MUST be HASHED (one-way), not encrypted.
                // We use SecurityUtils which uses PBKDF2WithHmacSHA256 (600,000 iterations)
                String securePasswordHash = SecurityUtils.hashPassword(password);

                Map<String, Object> userMap = new HashMap<>();
                userMap.put("uid", uid);
                userMap.put("fullName", encryptedFullName);
                userMap.put("email", encryptedEmail);
                userMap.put("password", securePasswordHash); // Store the secure hash
                userMap.put("profilePic", "");
                userMap.put("followersCount", 0);
                userMap.put("followingCount", 0);

                db.collection("users").document(uid).set(userMap).addOnSuccessListener(aVoid -> {
                    mAuth.signOut();
                    dismissLoadingDialog();
                    SharedPrefManager.getInstance(this).resetLoginAttempts();
                    Toast.makeText(this, "Account Created Successfully!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, LoginActivity.class));
                    finishAffinity();
                }).addOnFailureListener(e -> handleRegistrationError());
            } else handleRegistrationError();
        });
    }

    private void handleRegistrationError() {
        dismissLoadingDialog();
        btnSignUp.setEnabled(true);
        Toast.makeText(this, R.string.registration_error_generic, Toast.LENGTH_SHORT).show();
    }

    private void setupLoadingDialog() {
        loadingDialog = new Dialog(this, R.style.CustomLoadingDialog);
        loadingDialog.setContentView(R.layout.dialog_loading);
        loadingDialog.setCancelable(false);
        if (loadingDialog.getWindow() != null) loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    private void sproutLeafAnimation(ImageView leaf) {
        if (leaf == null || leaf.getVisibility() == View.VISIBLE) return;
        leaf.setVisibility(View.VISIBLE);
        leaf.setScaleX(0f); leaf.setScaleY(0f); leaf.setAlpha(0f);
        leaf.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(500).setInterpolator(new OvershootInterpolator()).start();
    }

    private void dismissLoadingDialog() { if (loadingDialog != null && loadingDialog.isShowing()) loadingDialog.dismiss(); }
    private void goBack() { finish(); overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right); }
    @Override protected void onDestroy() { dismissLoadingDialog(); super.onDestroy(); }
}
