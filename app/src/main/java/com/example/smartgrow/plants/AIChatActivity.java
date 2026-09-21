package com.example.smartgrow.plants;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartgrow.R;

public class AIChatActivity extends AppCompatActivity {

    private RecyclerView rvChatMessagesList;
    private EditText etChatInput;
    private ImageView ibChatAttach, ibBackArrow, ibChatMenu;
    private ImageButton ibSendMessage;
    private View layoutImagePreviewContainer;
    private String plantName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chat);

        if (getIntent() != null) {
            plantName = getIntent().getStringExtra("EXTRA_PLANT_NAME");
        }

        rvChatMessagesList = findViewById(R.id.rv_chat_messages_list);
        etChatInput = findViewById(R.id.et_chat_input);
        ibSendMessage = findViewById(R.id.ib_send_message); 
        ibChatAttach = findViewById(R.id.ib_chat_attach);
        ibBackArrow = findViewById(R.id.ib_back_arrow);
        ibChatMenu = findViewById(R.id.ib_chat_menu);
        layoutImagePreviewContainer = findViewById(R.id.layout_image_preview_container);

        if (ibBackArrow != null) {
            ibBackArrow.setOnClickListener(v -> finish());
        }

        setupChatFunctionality();
    }

    private void setupChatFunctionality() {
        if (ibSendMessage != null) {
            ibSendMessage.setOnClickListener(v -> {
                String messageText = etChatInput.getText().toString().trim();
                if (!messageText.isEmpty()) {
                    etChatInput.setText("");
                }
            });
        }
    }
}