package com.example.smartgrow.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.smartgrow.R;
import com.google.android.material.button.MaterialButton;

import java.util.Locale;

public class VerifyOTPActivity extends AppCompatActivity {

    private EditText etOtp1, etOtp2, etOtp3, etOtp4;
    private MaterialButton btnVerifyCode;
    private TextView tvResendTimer;
    private String expectedOtpCode;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        expectedOtpCode = getIntent().getStringExtra("otp_code");
        userEmail = getIntent().getStringExtra("email");

        setContentView(R.layout.activity_verify_otp);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        etOtp1 = findViewById(R.id.et_otp_1);
        etOtp2 = findViewById(R.id.et_otp_2);
        etOtp3 = findViewById(R.id.et_otp_3);
        etOtp4 = findViewById(R.id.et_otp_4);
        btnVerifyCode = findViewById(R.id.btn_verify_code);
        tvResendTimer = findViewById(R.id.tv_resend_timer);

        setupOtpJumping();
        startResendCountdown();

        btnVerifyCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String enteredCode = etOtp1.getText().toString() + etOtp2.getText().toString()
                        + etOtp3.getText().toString() + etOtp4.getText().toString();

                if (enteredCode.length() < 4) {
                    Toast.makeText(VerifyOTPActivity.this, "Please complete the 4-digit code", Toast.LENGTH_SHORT).show();
                } else if (expectedOtpCode != null && !enteredCode.equals(expectedOtpCode)) {
                    Toast.makeText(VerifyOTPActivity.this, "Incorrect code. Please try again.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(VerifyOTPActivity.this, "Code verified!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(VerifyOTPActivity.this, ResetPasswordActivity.class);
                    intent.putExtra("email", userEmail);
                    startActivity(intent);
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                }
            }
        });
    }

    private void setupOtpJumping() {
        etOtp1.addTextChangedListener(new GenericTextWatcher(etOtp1, etOtp2));
        etOtp2.addTextChangedListener(new GenericTextWatcher(etOtp2, etOtp3));
        etOtp3.addTextChangedListener(new GenericTextWatcher(etOtp3, etOtp4));
        etOtp4.addTextChangedListener(new GenericTextWatcher(etOtp4, null));
    }

    private void startResendCountdown() {
        new CountDownTimer(60000, 1000) {
            public void onTick(long millisUntilFinished) {
                tvResendTimer.setText(String.format(Locale.getDefault(), "Resend code in 0:%02d", millisUntilFinished / 1000));
                tvResendTimer.setClickable(false);
            }
            public void onFinish() {
                tvResendTimer.setText("Resend Code");
                tvResendTimer.setClickable(true);
            }
        }.start();
    }

    private class GenericTextWatcher implements TextWatcher {
        private View currentView;
        private View nextView;

        public GenericTextWatcher(View currentView, View nextView) {
            this.currentView = currentView;
            this.nextView = nextView;
        }

        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            String text = s.toString();
            if (text.length() == 1 && nextView != null) {
                nextView.requestFocus();
            }
        }
    }
}
