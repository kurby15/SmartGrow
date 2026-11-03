package com.example.smartgrow.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import com.example.smartgrow.R;
import com.example.smartgrow.core.GMailSender;
import com.example.smartgrow.utils.FirebaseCryptoUtils;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Random;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etForgotEmail;
    private MaterialButton btnSendCode;
    private TextView tvBackToLogin;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();
        setContentView(R.layout.activity_forgot_password);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        etForgotEmail = findViewById(R.id.et_forgot_email);
        btnSendCode = findViewById(R.id.btn_send_code);
        tvBackToLogin = findViewById(R.id.tv_back_to_login);

        btnSendCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String emailInput = etForgotEmail.getText().toString().trim();

                if (emailInput.isEmpty()) {
                    Toast.makeText(ForgotPasswordActivity.this, "Please enter your email address", Toast.LENGTH_SHORT).show();
                } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
                    Toast.makeText(ForgotPasswordActivity.this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
                } else {
                    checkEmailExists(emailInput);
                }
            }
        });

        tvBackToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });
    }

    private void checkEmailExists(String email) {
        btnSendCode.setEnabled(false);
        btnSendCode.setText("Verifying...");

        db.collection("users").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                boolean found = false;
                String foundUid = null;

                for (DocumentSnapshot doc : task.getResult()) {
                    String uid = doc.getId();
                    String encEmail = doc.getString("email");
                    if (encEmail != null) {
                        try {
                            String decryptedEmail = FirebaseCryptoUtils.decrypt(encEmail, uid);
                            if (email.equalsIgnoreCase(decryptedEmail)) {
                                found = true;
                                foundUid = uid;
                                break;
                            }
                        } catch (Exception e) {
                            Log.e("ForgotPassword", "Decryption error: " + e.getMessage());
                        }
                    }
                }

                if (found) {
                    sendOtpCode(email, foundUid);
                } else {
                    resetButton();
                    Toast.makeText(ForgotPasswordActivity.this, "This email is not registered.", Toast.LENGTH_SHORT).show();
                }
            } else {
                resetButton();
                String error = task.getException() != null ? task.getException().getMessage() : "Database Error";
                Log.e("ForgotPassword", "Firestore Error: " + error);
                Toast.makeText(ForgotPasswordActivity.this, "Verification failed. Check Firestore rules.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void sendOtpCode(String email, String uid) {
        String otpCode = String.valueOf(new Random().nextInt(9000) + 1000);

        GMailSender.sendOTP(email, otpCode, new GMailSender.EmailListener() {
            @Override
            public void onSuccess() {
                resetButton();
                Toast.makeText(ForgotPasswordActivity.this, "Verification code sent to " + email, Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(ForgotPasswordActivity.this, VerifyOTPActivity.class);
                intent.putExtra("email", email);
                intent.putExtra("uid", uid);
                intent.putExtra("otp_code", otpCode);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }

            @Override
            public void onFailure(Exception e) {
                resetButton();
                Toast.makeText(ForgotPasswordActivity.this, "Failed to send email: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void resetButton() {
        btnSendCode.setEnabled(true);
        btnSendCode.setText("Send Code");
    }
}