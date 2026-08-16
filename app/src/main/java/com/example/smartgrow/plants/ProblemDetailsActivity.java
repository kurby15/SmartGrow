package com.example.smartgrow.plants;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.smartgrow.R;

public class ProblemDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_problem_detail);

        // Bind XML views
        ImageButton ibBack = findViewById(R.id.ib_back);
        ImageView ivHeader = findViewById(R.id.iv_header);
        TextView tvTitle = findViewById(R.id.tv_title);
        TextView tvDesc = findViewById(R.id.tv_desc);
        TextView tvSymptoms = findViewById(R.id.tv_symptoms);
        TextView tvCause = findViewById(R.id.tv_cause);
        TextView tvSolutions = findViewById(R.id.tv_solutions);
        TextView tvPrevention = findViewById(R.id.tv_prevention);

        // Define your default fallback drawable resource ID
        int defaultDrawableRes = R.drawable.disease;

        // Back navigation
        if (ibBack != null) {
            ibBack.setOnClickListener(v -> finish());
        }

        // Get Intent extras (Checking both potential key variations for full compatibility)
        String title = getIntent().hasExtra("problem_title") ? getIntent().getStringExtra("problem_title") : getIntent().getStringExtra("title");
        String desc = getIntent().hasExtra("problem_description") ? getIntent().getStringExtra("problem_description") : getIntent().getStringExtra("description");

        // Key Fix: Check both "symptom_analysis" and "symptoms"
        String symptoms = getIntent().hasExtra("symptom_analysis") ? getIntent().getStringExtra("symptom_analysis") : getIntent().getStringExtra("symptoms");

        // Key Fix: Check both "disease_cause" and "cause"
        String cause = getIntent().hasExtra("disease_cause") ? getIntent().getStringExtra("disease_cause") : getIntent().getStringExtra("cause");

        String solutions = getIntent().getStringExtra("solutions");
        String prevention = getIntent().getStringExtra("prevention");
        String imgUrl = getIntent().getStringExtra("image_url");

        // Populate TextViews
        if (tvTitle != null) {
            tvTitle.setText(title != null && !title.isEmpty() ? title : "Common Issue");
        }

        if (tvDesc != null) {
            if (desc != null && !desc.isEmpty()) {
                tvDesc.setText(desc);
                tvDesc.setVisibility(View.VISIBLE);
            } else {
                tvDesc.setVisibility(View.GONE);
            }
        }

        if (tvSymptoms != null) {
            tvSymptoms.setText(symptoms != null && !symptoms.isEmpty() ? symptoms : "No symptom information available.");
        }

        if (tvCause != null) {
            tvCause.setText(cause != null && !cause.isEmpty() ? cause : "No cause information available.");
        }

        if (tvSolutions != null) {
            tvSolutions.setText(solutions != null && !solutions.isEmpty() ? solutions : "No solution information available.");
        }

        if (tvPrevention != null) {
            tvPrevention.setText(prevention != null && !prevention.isEmpty() ? prevention : "No prevention steps available.");
        }

        // Load image into iv_header with placeholder and error fallbacks
        if (ivHeader != null) {
            if (imgUrl != null && imgUrl.startsWith("http")) {
                Glide.with(this)
                        .load(imgUrl)
                        .placeholder(defaultDrawableRes) // Shown while loading
                        .error(defaultDrawableRes)       // Fallback on HTTP or network failure
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(ivHeader);
            } else {
                // Set default drawable if imgUrl is null or not a valid URL
                ivHeader.setImageResource(defaultDrawableRes);
            }
        }
    }
}