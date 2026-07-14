package com.example.smartgrow;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import java.util.HashMap;

public class RegisterStep1Activity extends AppCompatActivity {

    private MaterialCardView selectedCard = null;
    private TextView selectedTextView = null;
    private String selectedChoiceText = "";

    // Gagamit tayo ng HashMap para mabilis mahanap kung aling TextView ang kapares ng Card
    private final HashMap<MaterialCardView, TextView> cardTextMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_step1);

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

        // I-map natin ang mga cards sa kanilang text views para iwas ClassCastException crash
        cardTextMap.put(card1, tvChoice1);
        cardTextMap.put(card2, tvChoice2);
        cardTextMap.put(card3, tvChoice3);
        cardTextMap.put(card4, tvChoice4);

        // Click listeners para sa pagpili
        card1.setOnClickListener(v -> selectChoice(card1));
        card2.setOnClickListener(v -> selectChoice(card2));
        card3.setOnClickListener(v -> selectChoice(card3));
        card4.setOnClickListener(v -> selectChoice(card4));

        btnBack.setOnClickListener(v -> goBack());

        btnNext.setOnClickListener(v -> {
            if (selectedCard == null) {
                Toast.makeText(this, "Please select an option first!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Dito lilipad ang data papunta sa Database niyo via intent object
            User user = new User();
            user.setChoice1(selectedChoiceText);

            Intent intent = new Intent(RegisterStep1Activity.this, RegisterStep2Activity.class);
            intent.putExtra("user_data", user);
            startActivity(intent);

            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                goBack();
            }
        });
    }

    private void selectChoice(MaterialCardView targetCard) {
        // Ibalik sa dating kulay ang dating napiling card gamit ang map natin
        if (selectedCard != null) {
            selectedCard.setCardBackgroundColor(Color.parseColor("#FFFFFF"));
            selectedCard.setStrokeColor(Color.parseColor("#EAEAEA"));

            TextView previousText = cardTextMap.get(selectedCard);
            if (previousText != null) {
                previousText.setTextColor(Color.parseColor("#333333"));
            }
        }

        // Ilapat ang bagong kulay sa bagong napiling card
        TextView targetText = cardTextMap.get(targetCard);
        if (targetText != null) {
            targetCard.setCardBackgroundColor(Color.parseColor("#0C6211"));
            targetCard.setStrokeColor(Color.parseColor("#0C6211"));
            targetText.setTextColor(Color.parseColor("#FFFFFF"));

            selectedCard = targetCard;
            selectedChoiceText = targetText.getText().toString();
        }
    }

    private void goBack() {
        finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}