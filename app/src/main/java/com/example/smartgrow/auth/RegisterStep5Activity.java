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

    // ============================================================
    // PASSWORD VALIDATION PATTERNS
    // ============================================================

    private static final Pattern UPPERCASE_PATTERN =
            Pattern.compile(".*[A-Z].*");

    private static final Pattern NUMBER_PATTERN =
            Pattern.compile(".*[0-9].*");

    private static final Pattern SPECIAL_CHAR_PATTERN =
            Pattern.compile(
                    ".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*"
            );

    // ============================================================
    // PASSWORD REQUIREMENT COLORS
    // ============================================================

    private static final int COLOR_INVALID =
            Color.parseColor("#E53935");

    private static final int COLOR_VALID =
            Color.parseColor("#43A047");

    private static final int COLOR_NEUTRAL =
            Color.parseColor("#9E9E9E");

    // ============================================================
    // ON CREATE
    // ============================================================

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_register_step5
        );

        mAuth =
                FirebaseAuth.getInstance();

        db =
                FirebaseFirestore.getInstance();

        // ========================================================
        // GET USER DATA FROM PREVIOUS STEP
        // ========================================================

        userData =
                (User) getIntent()
                        .getSerializableExtra(
                                "user_data"
                        );

        // ========================================================
        // BIND VIEWS
        // ========================================================

        etFullName =
                findViewById(
                        R.id.et_signup_fullname
                );

        etEmail =
                findViewById(
                        R.id.et_signup_email
                );

        etPassword =
                findViewById(
                        R.id.et_signup_password
                );

        etConfirmPassword =
                findViewById(
                        R.id.et_confirm_password
                );

        btnSignUp =
                findViewById(
                        R.id.btn_sign_up
                );

        // ========================================================
        // PASSWORD REQUIREMENTS
        // ========================================================

        layoutPasswordRequirements =
                findViewById(
                        R.id.layout_password_requirements
                );

        tvReqLength =
                findViewById(
                        R.id.tv_req_length
                );

        tvReqUppercase =
                findViewById(
                        R.id.tv_req_uppercase
                );

        tvReqNumber =
                findViewById(
                        R.id.tv_req_number
                );

        tvReqSpecial =
                findViewById(
                        R.id.tv_req_special
                );

        // ========================================================
        // PASSWORD REQUIREMENT ICONS
        // ========================================================

        tvReqLengthIcon =
                findViewById(
                        R.id.tv_req_length_icon
                );

        tvReqUppercaseIcon =
                findViewById(
                        R.id.tv_req_uppercase_icon
                );

        tvReqNumberIcon =
                findViewById(
                        R.id.tv_req_number_icon
                );

        tvReqSpecialIcon =
                findViewById(
                        R.id.tv_req_special_icon
                );

        // ========================================================
        // PASSWORD REQUIREMENT ROWS
        // ========================================================

        rowReqLength =
                findViewById(
                        R.id.row_req_length
                );

        rowReqUppercase =
                findViewById(
                        R.id.row_req_uppercase
                );

        rowReqNumber =
                findViewById(
                        R.id.row_req_number
                );

        rowReqSpecial =
                findViewById(
                        R.id.row_req_special
                );

        // ========================================================
        // HIDE REQUIREMENTS INITIALLY
        // ========================================================

        if (layoutPasswordRequirements != null) {

            layoutPasswordRequirements.setVisibility(
                    View.GONE
            );

            layoutPasswordRequirements.setAlpha(1f);
            layoutPasswordRequirements.setTranslationY(0f);
        }

        // ========================================================
        // SETUP
        // ========================================================

        ImageView btnBack =
                findViewById(
                        R.id.btn_register_back
                );

        setupLoadingDialog();

        setupPasswordRealTimeValidation();

        // ========================================================
        // SIGN UP BUTTON
        // ========================================================

        btnSignUp.setOnClickListener(
                v -> validateAndSubmitForm()
        );

        // ========================================================
        // BACK BUTTON
        // ========================================================

        btnBack.setOnClickListener(
                v -> goBack()
        );

        // ========================================================
        // SYSTEM BACK
        // ========================================================

        getOnBackPressedDispatcher()
                .addCallback(
                        this,
                        new OnBackPressedCallback(true) {

                            @Override
                            public void handleOnBackPressed() {

                                goBack();
                            }
                        }
                );
    }

    // ============================================================
    // PASSWORD REAL-TIME VALIDATION
    // ============================================================

    private void setupPasswordRealTimeValidation() {

        if (etPassword == null)
            return;

        // ========================================================
        // PASSWORD FOCUS
        // ========================================================

        etPassword.setOnFocusChangeListener(
                (v, hasFocus) -> {

                    if (hasFocus) {

                        showPasswordRequirements();

                        String password =
                                etPassword
                                        .getText()
                                        .toString();

                        updatePasswordRequirementsList(
                                password
                        );

                    } else {

                        String password =
                                etPassword
                                        .getText()
                                        .toString();

                        if (isPasswordFullyValid(password)) {

                            hidePasswordRequirements();
                        }
                    }
                }
        );

        // ========================================================
        // PASSWORD TEXT CHANGED
        // ========================================================

        etPassword.addTextChangedListener(
                new TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count) {

                        updatePasswordRequirementsList(
                                s.toString()
                        );
                    }

                    @Override
                    public void afterTextChanged(
                            Editable s) {
                    }
                }
        );
    }

    // ============================================================
    // SHOW PASSWORD REQUIREMENTS
    // ============================================================

    private void showPasswordRequirements() {

        if (layoutPasswordRequirements == null)
            return;

        if (layoutPasswordRequirements.getVisibility()
                == View.VISIBLE) {

            return;
        }

        layoutPasswordRequirements.setVisibility(
                View.VISIBLE
        );

        layoutPasswordRequirements.setAlpha(0f);

        layoutPasswordRequirements.setTranslationY(
                -8f
        );

        layoutPasswordRequirements.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(220)
                .start();
    }

    // ============================================================
    // UPDATE PASSWORD REQUIREMENTS
    // ============================================================

    private void updatePasswordRequirementsList(
            String password) {

        if (layoutPasswordRequirements == null)
            return;

        // --------------------------------------------------------
        // Always show while password field is active
        // --------------------------------------------------------

        if (etPassword.hasFocus()) {

            if (layoutPasswordRequirements.getVisibility()
                    != View.VISIBLE) {

                showPasswordRequirements();
            }
        }

        // --------------------------------------------------------
        // CHECK REQUIREMENTS
        // --------------------------------------------------------

        boolean isEmpty =
                password.isEmpty();

        boolean lengthValid = password.length() >= 8;

        boolean uppercaseValid =
                UPPERCASE_PATTERN
                        .matcher(password)
                        .matches();

        boolean numberValid =
                NUMBER_PATTERN
                        .matcher(password)
                        .matches();

        boolean specialValid =
                SPECIAL_CHAR_PATTERN
                        .matcher(password)
                        .matches();

        // ========================================================
        // EMPTY PASSWORD
        // ========================================================

        if (isEmpty) {

            showRequirement(
                    rowReqLength,
                    tvReqLength,
                    tvReqLengthIcon,
                    false,
                    true
            );

            showRequirement(
                    rowReqUppercase,
                    tvReqUppercase,
                    tvReqUppercaseIcon,
                    false,
                    true
            );

            showRequirement(
                    rowReqNumber,
                    tvReqNumber,
                    tvReqNumberIcon,
                    false,
                    true
            );

            showRequirement(
                    rowReqSpecial,
                    tvReqSpecial,
                    tvReqSpecialIcon,
                    false,
                    true
            );

            return;
        }

        // ========================================================
        // LENGTH
        // ========================================================

        if (lengthValid) {

            hideRequirement(
                    rowReqLength
            );

        } else {

            showRequirement(
                    rowReqLength,
                    tvReqLength,
                    tvReqLengthIcon,
                    false,
                    false
            );
        }

        // ========================================================
        // UPPERCASE
        // ========================================================

        if (uppercaseValid) {

            hideRequirement(
                    rowReqUppercase
            );

        } else {

            showRequirement(
                    rowReqUppercase,
                    tvReqUppercase,
                    tvReqUppercaseIcon,
                    false,
                    false
            );
        }

        // ========================================================
        // NUMBER
        // ========================================================

        if (numberValid) {

            hideRequirement(
                    rowReqNumber
            );

        } else {

            showRequirement(
                    rowReqNumber,
                    tvReqNumber,
                    tvReqNumberIcon,
                    false,
                    false
            );
        }

        // ========================================================
        // SPECIAL CHARACTER
        // ========================================================

        if (specialValid) {

            hideRequirement(
                    rowReqSpecial
            );

        } else {

            showRequirement(
                    rowReqSpecial,
                    tvReqSpecial,
                    tvReqSpecialIcon,
                    false,
                    false
            );
        }

        // ========================================================
        // EVERYTHING VALID
        // ========================================================

        if (lengthValid
                && uppercaseValid
                && numberValid
                && specialValid) {

            hidePasswordRequirements();
        }
    }

    // ============================================================
    // SHOW ONE REQUIREMENT
    // ============================================================

    private void showRequirement(
            View row,
            TextView text,
            TextView icon,
            boolean valid,
            boolean neutral) {

        if (row == null
                || text == null
                || icon == null) {

            return;
        }

        // --------------------------------------------------------
        // Make row visible if currently hidden
        // --------------------------------------------------------

        if (row.getVisibility()
                != View.VISIBLE) {

            row.setAlpha(0f);

            row.setTranslationX(
                    -8f
            );

            row.setVisibility(
                    View.VISIBLE
            );

            row.animate()
                    .alpha(1f)
                    .translationX(0f)
                    .setDuration(180)
                    .start();
        }

        // ========================================================
        // NEUTRAL
        // ========================================================

        if (neutral) {

            text.setTextColor(
                    COLOR_NEUTRAL
            );

            icon.setText("•");

            icon.setTextColor(
                    COLOR_NEUTRAL
            );

            setRequirementIconBackground(
                    icon,
                    COLOR_NEUTRAL,
                    false
            );

            return;
        }

        // ========================================================
        // VALID
        // ========================================================

        if (valid) {

            text.setTextColor(
                    COLOR_VALID
            );

            icon.setText("✓");

            icon.setTextColor(
                    Color.WHITE
            );

            setRequirementIconBackground(
                    icon,
                    COLOR_VALID,
                    true
            );

            return;
        }

        // ========================================================
        // INVALID
        // ========================================================

        text.setTextColor(
                COLOR_INVALID
        );

        icon.setText("!");

        icon.setTextColor(
                COLOR_INVALID
        );

        setRequirementIconBackground(
                icon,
                COLOR_INVALID,
                false
        );
    }

    // ============================================================
    // HIDE ONE REQUIREMENT
    // ============================================================

    private void hideRequirement(
            View row) {

        if (row == null)
            return;

        if (row.getVisibility()
                != View.VISIBLE) {

            return;
        }

        // Prevent repeated animation calls
        if (row.getTag() != null
                && row.getTag()
                .equals("hiding")) {

            return;
        }

        row.setTag("hiding");

        row.animate()
                .alpha(0f)
                .translationX(10f)
                .setDuration(180)
                .withEndAction(() -> {

                    row.setVisibility(
                            View.GONE
                    );

                    row.setAlpha(1f);

                    row.setTranslationX(
                            0f
                    );

                    row.setTag(null);
                })
                .start();
    }

    // ============================================================
    // HIDE ENTIRE REQUIREMENTS BOX
    // ============================================================

    private void hidePasswordRequirements() {

        if (layoutPasswordRequirements == null)
            return;

        if (layoutPasswordRequirements.getVisibility()
                != View.VISIBLE) {

            return;
        }

        layoutPasswordRequirements
                .animate()
                .alpha(0f)
                .translationY(-8f)
                .setDuration(220)
                .withEndAction(() -> {

                    layoutPasswordRequirements
                            .setVisibility(
                                    View.GONE
                            );

                    layoutPasswordRequirements
                            .setAlpha(1f);

                    layoutPasswordRequirements
                            .setTranslationY(
                                    0f
                            );
                })
                .start();
    }

    // ============================================================
    // REQUIREMENT ICON BACKGROUND
    // ============================================================

    private void setRequirementIconBackground(
            TextView icon,
            int color,
            boolean filled) {

        GradientDrawable drawable =
                new GradientDrawable();

        drawable.setShape(
                GradientDrawable.OVAL
        );

        if (filled) {

            drawable.setColor(
                    color
            );

        } else {

            drawable.setColor(
                    Color.TRANSPARENT
            );

            drawable.setStroke(
                    1,
                    color
            );
        }

        icon.setBackground(
                drawable
        );
    }

    // ============================================================
    // CHECK FULL PASSWORD
    // ============================================================

    private boolean isPasswordFullyValid(
            String password) {

        return password.length() >= 8
                && password.length() <= 12
                && UPPERCASE_PATTERN
                .matcher(password)
                .matches()
                && NUMBER_PATTERN
                .matcher(password)
                .matches()
                && SPECIAL_CHAR_PATTERN
                .matcher(password)
                .matches();
    }

    // ============================================================
    // VALIDATE SIGN UP FORM
    // ============================================================

    private void validateAndSubmitForm() {

        String fullName =
                etFullName != null
                        ? etFullName
                        .getText()
                        .toString()
                        .trim()
                        : "";

        String email =
                etEmail != null
                        ? etEmail
                        .getText()
                        .toString()
                        .trim()
                        : "";

        String password =
                etPassword != null
                        ? etPassword
                        .getText()
                        .toString()
                        : "";

        String confirmPassword =
                etConfirmPassword != null
                        ? etConfirmPassword
                        .getText()
                        .toString()
                        : "";

        // ========================================================
        // FULL NAME
        // ========================================================

        if (fullName.isEmpty()) {

            etFullName.setError(
                    "Full name is required"
            );

            etFullName.requestFocus();

            return;
        }

        // ========================================================
        // EMAIL
        // ========================================================

        if (email.isEmpty()
                || !Patterns.EMAIL_ADDRESS
                .matcher(email)
                .matches()) {

            etEmail.setError(
                    "Valid email is required"
            );

            etEmail.requestFocus();

            return;
        }

        // ========================================================
        // PASSWORD
        // ========================================================

        if (!isPasswordFullyValid(
                password
        )) {

            showPasswordRequirements();

            updatePasswordRequirementsList(
                    password
            );

            etPassword.requestFocus();

            Toast.makeText(
                    this,
                    "Password does not meet all requirements",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // ========================================================
        // CONFIRM PASSWORD
        // ========================================================

        if (!password.equals(
                confirmPassword
        )) {

            etConfirmPassword.setError(
                    "Passwords do not match"
            );

            etConfirmPassword.requestFocus();

            return;
        }

        // ========================================================
        // DISABLE BUTTON
        // ========================================================

        btnSignUp.setEnabled(
                false
        );

        // ========================================================
        // SHOW LOADING DIALOG
        // ========================================================

        loadingDialog.show();

        TextView tvPercentage =
                loadingDialog.findViewById(
                        R.id.tv_progress_percentage
                );

        TextView tvLoadingMessage =
                loadingDialog.findViewById(
                        R.id.tv_loading_message
                );

        ImageView leaf1 =
                loadingDialog.findViewById(
                        R.id.leaf_bottom_left
                );

        ImageView leaf2 =
                loadingDialog.findViewById(
                        R.id.leaf_top_right
                );

        ImageView leaf3 =
                loadingDialog.findViewById(
                        R.id.leaf_top_left
                );

        ImageView leaf4 =
                loadingDialog.findViewById(
                        R.id.leaf_bottom_right
                );

        resetLeaves(
                leaf1,
                leaf2,
                leaf3,
                leaf4
        );

        // ========================================================
        // LOADING ANIMATION
        // ========================================================

        ValueAnimator animator =
                ValueAnimator.ofInt(
                        0,
                        100
                );

        animator.setDuration(
                4000
        );

        animator.addUpdateListener(
                animation -> {

                    int progressValue =
                            (int) animation
                                    .getAnimatedValue();

                    // --------------------------------------------
                    // Percentage
                    // --------------------------------------------

                    if (tvPercentage != null) {

                        tvPercentage.setText(
                                String.format(
                                        "%d%%",
                                        progressValue
                                )
                        );
                    }

                    // --------------------------------------------
                    // Loading Message
                    // --------------------------------------------

                    if (tvLoadingMessage != null) {

                        if (progressValue <= 25) {

                            tvLoadingMessage.setText(
                                    "Creating your account..."
                            );

                        } else if (progressValue <= 50) {

                            tvLoadingMessage.setText(
                                    "Setting up your dashboard..."
                            );

                        } else if (progressValue <= 75) {

                            tvLoadingMessage.setText(
                                    "Readying your AI companion..."
                            );

                        } else if (progressValue <= 95) {

                            tvLoadingMessage.setText(
                                    "Almost there! Finalizing updates..."
                            );

                        } else {

                            tvLoadingMessage.setText(
                                    "Start chatting soon! 🎉"
                            );
                        }
                    }

                    // --------------------------------------------
                    // Sprout Leaves
                    // --------------------------------------------

                    if (progressValue >= 20) {

                        sproutLeafAnimation(
                                leaf1
                        );
                    }

                    if (progressValue >= 45) {

                        sproutLeafAnimation(
                                leaf2
                        );
                    }

                    if (progressValue >= 70) {

                        sproutLeafAnimation(
                                leaf3
                        );
                    }

                    if (progressValue >= 90) {

                        sproutLeafAnimation(
                                leaf4
                        );
                    }
                }
        );

        // ========================================================
        // ANIMATION COMPLETE
        // ========================================================

        animator.addListener(
                new AnimatorListenerAdapter() {

                    @Override
                    public void onAnimationEnd(
                            Animator animation) {

                        super.onAnimationEnd(
                                animation
                        );

                        registerUser(
                                fullName,
                                email,
                                password
                        );
                    }
                }
        );

        animator.start();
    }

    // ============================================================
    // FIREBASE REGISTRATION
    // ============================================================

    private void registerUser(
            String fullName,
            String email,
            String password) {

        mAuth.createUserWithEmailAndPassword(
                        email,
                        password
                )
                .addOnCompleteListener(
                        task -> {

                            if (task.isSuccessful()
                                    && mAuth.getCurrentUser()
                                    != null) {

                                String uid =
                                        mAuth.getCurrentUser()
                                                .getUid();

                                // --------------------------------
                                // ENCRYPT USER DATA
                                // --------------------------------

                                String encryptedEmail =
                                        FirebaseCryptoUtils.encrypt(
                                                email,
                                                uid
                                        );

                                String encryptedFullName =
                                        FirebaseCryptoUtils.encrypt(
                                                fullName,
                                                uid
                                        );

                                String encryptedPassword =
                                        FirebaseCryptoUtils.encrypt(
                                                password,
                                                uid
                                        );

                                // --------------------------------
                                // USER MAP
                                // --------------------------------

                                Map<String, Object> userMap =
                                        new HashMap<>();

                                userMap.put(
                                        "uid",
                                        uid
                                );

                                userMap.put(
                                        "fullName",
                                        encryptedFullName
                                );

                                userMap.put(
                                        "email",
                                        encryptedEmail
                                );

                                userMap.put(
                                        "password",
                                        encryptedPassword
                                );

                                userMap.put(
                                        "profilePic",
                                        ""
                                );

                                userMap.put(
                                        "bio",
                                        ""
                                );

                                userMap.put(
                                        "followersCount",
                                        0
                                );

                                userMap.put(
                                        "followingCount",
                                        0
                                );

                                userMap.put(
                                        "followers",
                                        new HashMap<String, Boolean>()
                                );

                                userMap.put(
                                        "following",
                                        new HashMap<String, Boolean>()
                                );

                                // --------------------------------
                                // SAVE TO FIRESTORE
                                // --------------------------------

                                db.collection(
                                                "users"
                                        )
                                        .document(uid)
                                        .set(userMap)
                                        .addOnSuccessListener(
                                                aVoid -> {

                                                    mAuth.signOut();

                                                    dismissLoadingDialog();

                                                    Toast.makeText(
                                                            RegisterStep5Activity.this,
                                                            "Account Created Successfully!",
                                                            Toast.LENGTH_SHORT
                                                    ).show();

                                                    Intent intent =
                                                            new Intent(
                                                                    RegisterStep5Activity.this,
                                                                    LoginActivity.class
                                                            );

                                                    startActivity(
                                                            intent
                                                    );

                                                    overridePendingTransition(
                                                            R.anim.slide_in_right,
                                                            R.anim.slide_out_left
                                                    );

                                                    finishAffinity();
                                                }
                                        )
                                        .addOnFailureListener(
                                                e -> {

                                                    if (isFinishing())
                                                        return;

                                                    dismissLoadingDialog();

                                                    btnSignUp.setEnabled(
                                                            true
                                                    );

                                                    Toast.makeText(
                                                            RegisterStep5Activity.this,
                                                            "Database error: "
                                                                    + e.getMessage(),
                                                            Toast.LENGTH_SHORT
                                                    ).show();
                                                }
                                        );

                            } else {

                                if (isFinishing())
                                    return;

                                dismissLoadingDialog();

                                btnSignUp.setEnabled(
                                        true
                                );

                                String errorMsg =
                                        task.getException()
                                                != null
                                                ? task.getException()
                                                .getMessage()
                                                : "Authentication failed.";

                                Toast.makeText(
                                        RegisterStep5Activity.this,
                                        "Auth failed: "
                                                + errorMsg,
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }
                );
    }

    // ============================================================
    // LOADING DIALOG
    // ============================================================

    private void setupLoadingDialog() {

        loadingDialog =
                new Dialog(
                        this,
                        R.style.CustomLoadingDialog
                );

        loadingDialog.setContentView(
                R.layout.dialog_loading
        );

        loadingDialog.setCancelable(
                false
        );

        if (loadingDialog.getWindow()
                != null) {

            loadingDialog.getWindow()
                    .setBackgroundDrawable(
                            new ColorDrawable(
                                    Color.TRANSPARENT
                            )
                    );
        }
    }

    // ============================================================
    // SPROUT LEAF ANIMATION
    // ============================================================

    private void sproutLeafAnimation(
            ImageView leaf) {

        if (leaf == null
                || leaf.getVisibility()
                == View.VISIBLE) {

            return;
        }

        leaf.setVisibility(
                View.VISIBLE
        );

        leaf.setScaleX(
                0f
        );

        leaf.setScaleY(
                0f
        );

        leaf.setAlpha(
                0f
        );

        leaf.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(500)
                .setInterpolator(
                        new OvershootInterpolator()
                )
                .start();
    }

    // ============================================================
    // RESET LEAVES
    // ============================================================

    private void resetLeaves(
            ImageView... leaves) {

        for (ImageView leaf : leaves) {

            if (leaf != null) {

                leaf.setVisibility(
                        View.INVISIBLE
                );

                leaf.setScaleX(
                        0f
                );

                leaf.setScaleY(
                        0f
                );

                leaf.setAlpha(
                        0f
                );
            }
        }
    }

    // ============================================================
    // DISMISS LOADING
    // ============================================================

    private void dismissLoadingDialog() {

        if (loadingDialog != null
                && loadingDialog.isShowing()) {

            loadingDialog.dismiss();
        }
    }

    // ============================================================
    // GO BACK
    // ============================================================

    private void goBack() {

        finish();

        overridePendingTransition(
                R.anim.slide_in_left,
                R.anim.slide_out_right
        );
    }

    // ============================================================
    // ON DESTROY
    // ============================================================

    @Override
    protected void onDestroy() {

        dismissLoadingDialog();

        super.onDestroy();
    }
}

