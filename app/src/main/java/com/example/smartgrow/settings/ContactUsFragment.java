package com.example.smartgrow.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.smartgrow.R;

public class ContactUsFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    private ImageView btnBackContact;
    private LinearLayout btnContactEmail;

    private EditText etFeedbackMessage;
    private TextView btnSubmitFeedback;

    public ContactUsFragment() {
        // Required empty public constructor
    }

    public static ContactUsFragment newInstance(String param1, String param2) {
        ContactUsFragment fragment = new ContactUsFragment();

        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.fragment_contact_us,
                container,
                false
        );

        // Find Views
        btnBackContact = view.findViewById(R.id.btn_back_contact);
        btnContactEmail = view.findViewById(R.id.btn_contact_email);

        etFeedbackMessage = view.findViewById(R.id.et_feedback_message);
        btnSubmitFeedback = view.findViewById(R.id.btn_submit_feedback);


        // Back Button
        if (btnBackContact != null) {
            btnBackContact.setOnClickListener(v -> {

                if (getParentFragmentManager() != null) {
                    getParentFragmentManager().popBackStack();
                }

            });
        }


        // Email Support
        if (btnContactEmail != null) {
            btnContactEmail.setOnClickListener(v -> {

                Intent emailIntent = new Intent(Intent.ACTION_SENDTO);

                emailIntent.setData(
                        Uri.parse("mailto:supportsmartgrow@gmail.com")
                );

                emailIntent.putExtra(
                        Intent.EXTRA_SUBJECT,
                        "SmartGrow App Inquiry"
                );

                try {

                    startActivity(
                            Intent.createChooser(
                                    emailIntent,
                                    "Send email via..."
                            )
                    );

                } catch (Exception e) {

                    Toast.makeText(
                            getContext(),
                            "No email client found.",
                            Toast.LENGTH_SHORT
                    ).show();

                }

            });
        }


        // Submit Feedback
        if (btnSubmitFeedback != null) {
            btnSubmitFeedback.setOnClickListener(v -> {

                if (etFeedbackMessage != null) {

                    String feedbackText =
                            etFeedbackMessage
                                    .getText()
                                    .toString()
                                    .trim();

                    if (feedbackText.isEmpty()) {

                        Toast.makeText(
                                getContext(),
                                "Please enter your feedback message.",
                                Toast.LENGTH_SHORT
                        ).show();

                    } else {

                        Toast.makeText(
                                getContext(),
                                "Thank you! Feedback submitted.",
                                Toast.LENGTH_LONG
                        ).show();

                        etFeedbackMessage.setText("");
                    }
                }

            });
        }

        return view;
    }
}

