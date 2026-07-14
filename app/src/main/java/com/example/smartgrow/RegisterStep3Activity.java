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

public class RegisterStep3Activity extends AppCompatActivity {

    private MaterialCardView selectedCard = null;
    private String selectedChoiceText = "";
    private User userData;

    // HashMap para i-ugnay ang Card at ang kaniyang katapat na TextView
    private final HashMap<MaterialCardView, TextView> cardTextMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_step3);

        // Kuhanin ang pinasa mong data mula sa Step 2
        userData = (User) getIntent().getSerializableExtra("user_data");

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

        // Mapping para sa iwas class cast at null pointer crash
        cardTextMap.put(card1, tvChoice1);
        cardTextMap.put(card2, tvChoice2);
        cardTextMap.put(card3, tvChoice3);
        cardTextMap.put(card4, tvChoice4);

        // Click listeners para sa mapagpipilian
        card1.setOnClickListener(v -> selectChoice(card1));
        card2.setOnClickListener(v -> selectChoice(card2));
        card3.setOnClickListener(v -> selectChoice(card3));
        card4.setOnClickListener(v -> selectChoice(card4));

        btnBack.setOnClickListener(v -> goBack());

        btnNext.setOnClickListener(v -> {
            if (selectedCard == null) {
                Toast.makeText(this, "Please select an answer to continue!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (userData != null) {
                userData.setChoice3(selectedChoiceText);
            }

            // Lilipad na sa Step 4 bitbit ang naipong configuration data
            Intent intent = new Intent(RegisterStep3Activity.this, RegisterStep4Activity.class);
            intent.putExtra("user_data", userData);
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
        // Ibalik sa puti at gray stroke ang dating pinili
        if (selectedCard != null) {
            selectedCard.setCardBackgroundColor(Color.parseColor("#FFFFFF"));
            selectedCard.setStrokeColor(Color.parseColor("#EAEAEA"));

            TextView previousText = cardTextMap.get(selectedCard);
            if (previousText != null) {
                previousText.setTextColor(Color.parseColor("#333333"));
            }
        }

        // Ilapat ang Green Theme sa bagong pinili ng user
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