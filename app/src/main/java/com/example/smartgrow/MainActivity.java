package com.example.smartgrow;

import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // ➕ INAYOS: Idineklara ang MaterialCardView para mahanap natin ang nakaumbok na button mamaya
    private MaterialCardView cardNavChatAssistant;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation_bar);

        // 🔗 INAYOS: Binind na natin ang central structural button control galing sa XML
        cardNavChatAssistant = findViewById(R.id.card_nav_chat_assistant);

        // Load HomeFragment by default
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_diary) {
                // 🛠️ INAYOS: Ikisinabit na natin ang DiaryFragment niyo rito para lumitaw ang Plant Diary UI!
                selectedFragment = new DiaryFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
                return true;
            }
            return false;
        });

        // 💬 INAYOS: Nilagyan ng Click Listener ang central circle card button para bumukas ang Assistant Chat niyo!
        if (cardNavChatAssistant != null) {
            cardNavChatAssistant.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showAiChatAssistantBottomSheet();
                }
            });
        }
    }

    /**
     * 🌟 Slide-Up Window Panel Handler for the AI Assistant Chat System
     * Moved from HomeFragment to MainActivity as requested in development notes.
     */
    private void showAiChatAssistantBottomSheet() {
        final BottomSheetDialog chatDialog = new BottomSheetDialog(this);
        chatDialog.setContentView(R.layout.dialog_chat_assistant);

        // Force maximum layout behavior to enable smooth fullscreen slide dynamics
        chatDialog.getBehavior().setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);

        RecyclerView rvChatMessages = chatDialog.findViewById(R.id.rv_chat_messages_list);
        EditText etChatInput = chatDialog.findViewById(R.id.et_chat_input);
        FloatingActionButton fabSend = chatDialog.findViewById(R.id.fab_send_message);
        ImageButton ibMenu = chatDialog.findViewById(R.id.ib_chat_menu);
        ImageButton ibAttach = chatDialog.findViewById(R.id.ib_chat_attach);

        final ArrayList<ChatMessageModel> chatList = new ArrayList<>();

        // Initial AI message
        chatList.add(new ChatMessageModel("Hi! I'm your Plant Smart Care Assistant. How can I help you today?", getCurrentTime(), ChatMessageModel.TYPE_AI));

        final ChatAssistantAdapter chatAdapter = new ChatAssistantAdapter(chatList);
        if (rvChatMessages != null) {
            rvChatMessages.setLayoutManager(new LinearLayoutManager(this));
            rvChatMessages.setAdapter(chatAdapter);
        }

        // Chat History Menu
        if (ibMenu != null) {
            ibMenu.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(MainActivity.this, v);
                popup.getMenu().add("History");
                popup.setOnMenuItemClickListener(item -> {
                    if (item.getTitle().equals("History")) {
                        Toast.makeText(MainActivity.this, "Opening Chat History Logs...", Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    return false;
                });
                popup.show();
            });
        }

        // Attachment Menu
        if (ibAttach != null) {
            ibAttach.setOnClickListener(v -> {
                PopupMenu attachMenu = new PopupMenu(MainActivity.this, v);
                attachMenu.getMenu().add("Take a Photo");
                attachMenu.getMenu().add("Upload from Gallery");

                attachMenu.setOnMenuItemClickListener(item -> {
                    if (item.getTitle().equals("Take a Photo")) {
                        Toast.makeText(MainActivity.this, "Opening Camera Client...", Toast.LENGTH_SHORT).show();
                        return true;
                    } else if (item.getTitle().equals("Upload from Gallery")) {
                        Toast.makeText(MainActivity.this, "Opening Media Storage Gallery...", Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    return false;
                });
                attachMenu.show();
            });
        }

        // Send Message logic
        if (fabSend != null && etChatInput != null) {
            fabSend.setOnClickListener(v -> {
                String userText = etChatInput.getText().toString().trim();
                if (!userText.isEmpty()) {
                    chatList.add(new ChatMessageModel(userText, getCurrentTime(), ChatMessageModel.TYPE_USER));
                    chatAdapter.notifyItemInserted(chatList.size() - 1);
                    if (rvChatMessages != null) rvChatMessages.scrollToPosition(chatList.size() - 1);

                    etChatInput.setText("");

                    // Simulate AI thinking
                    chatList.add(new ChatMessageModel("", "", ChatMessageModel.TYPE_LOADING));
                    chatAdapter.notifyItemInserted(chatList.size() - 1);
                    if (rvChatMessages != null) rvChatMessages.scrollToPosition(chatList.size() - 1);

                    new Handler().postDelayed(() -> {
                        int loadingIndex = chatList.size() - 1;
                        if (loadingIndex >= 0 && chatList.get(loadingIndex).getMessageType() == ChatMessageModel.TYPE_LOADING) {
                            chatList.remove(loadingIndex);
                            chatAdapter.notifyItemRemoved(loadingIndex);
                        }

                        String aiReply = "I have noted that. Let me look up the optimal growth patterns and diagnostics for your plant updates.";
                        chatList.add(new ChatMessageModel(aiReply, getCurrentTime(), ChatMessageModel.TYPE_AI));
                        chatAdapter.notifyItemInserted(chatList.size() - 1);
                        if (rvChatMessages != null) rvChatMessages.scrollToPosition(chatList.size() - 1);
                    }, 2000);
                }
            });
        }

        chatDialog.show();
    }

    private String getCurrentTime() {
        return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
    }
}