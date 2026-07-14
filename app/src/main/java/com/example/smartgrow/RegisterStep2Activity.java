package com.example.smartgrow;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class RegisterStep2Activity extends AppCompatActivity {

    private MaterialCardView selectedCard = null;
    private String selectedChoiceText = "";
    private User userData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get user data from previous step
        userData = (User) getIntent().getSerializableExtra("user_data");

        setContentView(R.layout.activity_register_step2);

        ImageButton btnBack = findViewById(R.id.btn_register_back);
        MaterialButton btnNext = findViewById(R.id.btn_register_next);

        MaterialCardView card1 = findViewById(R.id.card_choice1);
        MaterialCardView card2 = findViewById(R.id.card_choice2);
        MaterialCardView card3 = findViewById(R.id.card_choice3);
        MaterialCardView card4 = findViewById(R.id.card_choice4);

        TextView tvChoice1 = findViewById(R.id.tv_choice1_text);
        TextView tvChoice2 = findViewById(R.id.tv_choice2_text);
        TextView tvChoice3 = findViewById(R.id.tv_choice3_text);
        TextView tvChoice4 = findViewById(R.id.tv_choice4_text);

        card1.setOnClickListener(v -> selectChoice(card1, tvChoice1));
        card2.setOnClickListener(v -> selectChoice(card2, tvChoice2));
        card3.setOnClickListener(v -> selectChoice(card3, tvChoice3));
        card4.setOnClickListener(v -> selectChoice(card4, tvChoice4));


        btnBack.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });


        btnNext.setOnClickListener(v -> {
            if (selectedCard == null) {
                Toast.makeText(this, "Please choose an option to proceed!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (userData != null) {
                userData.setChoice2(selectedChoiceText);
            }

            Intent intent = new Intent(RegisterStep2Activity.this, RegisterStep3Activity.class);
            intent.putExtra("user_data", userData);
            startActivity(intent);


            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });


        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            }
        });
    }

    private void selectChoice(MaterialCardView targetCard, TextView targetText) {
        if (selectedCard != null) {
            selectedCard.setCardBackgroundColor(Color.parseColor("#FFFFFF"));
            selectedCard.setStrokeColor(Color.parseColor("#EAEAEA"));

            TextView previousText = (TextView) selectedCard.getChildAt(0);
            previousText.setTextColor(Color.parseColor("#333333"));
        }

        targetCard.setCardBackgroundColor(Color.parseColor("#0C6211"));
        targetCard.setStrokeColor(Color.parseColor("#0C6211"));
        targetText.setTextColor(Color.parseColor("#FFFFFF"));

        selectedCard = targetCard;
        selectedChoiceText = targetText.getText().toString();
    }
}