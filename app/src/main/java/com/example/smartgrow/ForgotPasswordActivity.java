package com.example.smartgrow;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Random;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etForgotEmail;
    private MaterialButton btnSendCode;
    private TextView tvBackToLogin;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference("users");

        Window window = getWindow();
        window.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );
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
        Toast.makeText(this, "Verifying email...", Toast.LENGTH_SHORT).show();

        // 🔍 Hahanapin natin ang user sa Firebase gamit ang Email na ininput
        databaseReference.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Email found! Generate 4-digit code
                    String otpCode = String.valueOf(new Random().nextInt(9000) + 1000);
                    
                    // 🚀 SEND REAL EMAIL gamit ang totoong email na nahanap sa DB
                    GMailSender.sendOTP(email, otpCode, new GMailSender.EmailListener() {
                        @Override
                        public void onSuccess() {
                            btnSendCode.setEnabled(true);
                            Toast.makeText(ForgotPasswordActivity.this, "Verification code sent to " + email, Toast.LENGTH_SHORT).show();
                            
                            Intent intent = new Intent(ForgotPasswordActivity.this, VerifyOTPActivity.class);
                            intent.putExtra("email", email);
                            intent.putExtra("otp_code", otpCode); // Ipapasa ang code para ma-verify sa next screen
                            startActivity(intent);
                            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                        }

                        @Override
                        public void onFailure(Exception e) {
                            btnSendCode.setEnabled(true);
                            Toast.makeText(ForgotPasswordActivity.this, "Email error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            Log.e("ForgotPassword", "Failed to send email", e);
                        }
                    });
                } else {
                    btnSendCode.setEnabled(true);
                    Toast.makeText(ForgotPasswordActivity.this, "This email is not registered in SmartGrow.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                btnSendCode.setEnabled(true);
                Toast.makeText(ForgotPasswordActivity.this, "Database Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}