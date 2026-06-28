package com.example.smartgrow;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {

    private MaterialCardView cardNavChatAssistant;

    // 🟢 SINALONG GLOBAL HEADER CONTROLS MULA SA DASHBOARD
    private MaterialCardView cardActionNotification, cardActionGlobal, cardActionProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation_bar);
        cardNavChatAssistant = findViewById(R.id.card_nav_chat_assistant);

        // 🟢 BINDING NG MGA GLOBAL HEADER BUTTONS
        cardActionNotification = findViewById(R.id.card_action_notification);
        cardActionGlobal = findViewById(R.id.card_action_global);
        cardActionProfile = findViewById(R.id.card_action_profile);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }

        if (bottomNav != null) {
            bottomNav.setOnItemSelectedListener(item -> {
                Fragment selectedFragment = null;
                int itemId = item.getItemId();

                if (itemId == R.id.nav_home) {
                    selectedFragment = new HomeFragment();
                } else if (itemId == R.id.nav_diary) {
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
        }

        // I-setup ang mga click functions ng global header
        setupGlobalHeaderListeners();

        if (cardNavChatAssistant != null) {
            cardNavChatAssistant.setOnClickListener(v -> showAiChatAssistantBottomSheet());
        }
    }

    // 🟢 GLOBAL LISTENERS: Iisang deklarasyon, gumagana sa kahit anong active tab!
    private void setupGlobalHeaderListeners() {
        if (cardActionNotification != null) {
            cardActionNotification.setOnClickListener(v -> showNotificationDialog());
        }
        if (cardActionGlobal != null) {
            cardActionGlobal.setOnClickListener(v -> {
                // Pagpapalit ng fragment gamit ang FragmentManager
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new CommunityForumFragment()) // Palitan ang R.id.fragment_container kung iba ang ID ng lalagyan mo
                        .addToBackStack(null) // Opsyonal: Para kapag pinindot ang back button, babalik sa dating screen
                        .commit();
            });
        }
        if (cardActionProfile != null) {
            cardActionProfile.setOnClickListener(v -> {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new ProfileFragment()) // Dito ka dideretso
                        .addToBackStack(null)
                        .commit();
            });
        }
    }
    // 🟢 NOTIFICATION DIALOG ENGINE
    private void showNotificationDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_notification);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        MaterialCardView btnClose = dialog.findViewById(R.id.btn_close_notification);
        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    public void showAiChatAssistantBottomSheet() {
        final BottomSheetDialog chatDialog = new BottomSheetDialog(this);
        View chatView = LayoutInflater.from(this).inflate(R.layout.dialog_chat_assistant, null);
        chatDialog.setContentView(chatView);

        chatDialog.getBehavior().setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);

        RecyclerView rvChatMessages = chatView.findViewById(R.id.rv_chat_messages_list);
        EditText etChatInput = chatView.findViewById(R.id.et_chat_input);
        FloatingActionButton fabSend = chatView.findViewById(R.id.fab_send_message);
        ImageButton ibMenu = chatView.findViewById(R.id.ib_chat_menu);
        ImageButton ibAttach = chatView.findViewById(R.id.ib_chat_attach);

        final ArrayList<ChatMessageModel> chatList = new ArrayList<>();
        chatList.add(new ChatMessageModel("Hi! I'm your Plant Smart Care Assistant. How can I help you today?", getCurrentPhTime(), ChatMessageModel.TYPE_AI));

        final ChatAdapter chatAdapter = new ChatAdapter(chatList);
        if (rvChatMessages != null) {
            rvChatMessages.setLayoutManager(new LinearLayoutManager(this));
            rvChatMessages.setAdapter(chatAdapter);
        }

        // SLOT 1: TATLONG DOTS MENU
        if (ibMenu != null) {
            ibMenu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    View dropdownView = LayoutInflater.from(MainActivity.this).inflate(R.layout.layout_custom_dropdown, null);
                    LinearLayout llNewChat = dropdownView.findViewById(R.id.item_dropdown_new_chat);
                    LinearLayout llHistory = dropdownView.findViewById(R.id.item_dropdown_history);

                    final PopupWindow customMenuWindow = new PopupWindow(dropdownView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
                    customMenuWindow.setOutsideTouchable(true);
                    customMenuWindow.setFocusable(true);
                    customMenuWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

                    if (llNewChat != null) {
                        llNewChat.setOnClickListener(view -> {
                            chatList.clear();
                            chatList.add(new ChatMessageModel("Hi! This is a fresh new chat session. Ask me anything about your plant diagnostics!", getCurrentPhTime(), ChatMessageModel.TYPE_AI));
                            chatAdapter.notifyDataSetChanged();
                            Toast.makeText(MainActivity.this, "New Chat Started", Toast.LENGTH_SHORT).show();
                            customMenuWindow.dismiss();
                        });
                    }

                    if (llHistory != null) {
                        llHistory.setOnClickListener(view -> {
                            customMenuWindow.dismiss();
                            openRecentsHistoryPanel(chatList, chatAdapter);
                        });
                    }
                    customMenuWindow.showAsDropDown(v, -280, 10);
                }
            });
        }

        // SLOT 2: PLUS ATTACHMENT SIGN UPWARD ENGINE
        if (ibAttach != null) {
            ibAttach.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    View attachView = LayoutInflater.from(MainActivity.this).inflate(R.layout.layout_attach_dropdown, null);
                    LinearLayout llTakePhoto = attachView.findViewById(R.id.item_dropdown_take_photo);
                    LinearLayout llUploadGallery = attachView.findViewById(R.id.item_dropdown_upload_gallery);

                    final PopupWindow attachMenuWindow = new PopupWindow(attachView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
                    attachMenuWindow.setOutsideTouchable(true);
                    attachMenuWindow.setFocusable(true);
                    attachMenuWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

                    if (llTakePhoto != null) {
                        llTakePhoto.setOnClickListener(view -> {
                            Toast.makeText(MainActivity.this, "Opening Camera Client...", Toast.LENGTH_SHORT).show();
                            attachMenuWindow.dismiss();
                        });
                    }

                    if (llUploadGallery != null) {
                        llUploadGallery.setOnClickListener(view -> {
                            Toast.makeText(MainActivity.this, "Opening Media Storage Gallery...", Toast.LENGTH_SHORT).show();
                            attachMenuWindow.dismiss();
                        });
                    }

                    attachView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                    int dropdownHeight = attachView.getMeasuredHeight();
                    int buttonHeight = v.getHeight();

                    int yOffset = -(dropdownHeight + buttonHeight + 15);
                    int xOffset = 10;

                    attachMenuWindow.showAsDropDown(v, xOffset, yOffset);
                }
            });
        }

        // Send Message logic
        if (fabSend != null && etChatInput != null) {
            fabSend.setOnClickListener(v -> {
                String userText = etChatInput.getText().toString().trim();
                if (!userText.isEmpty()) {
                    String timeStamp = getCurrentPhTime();
                    chatList.add(new ChatMessageModel(userText, timeStamp, ChatMessageModel.TYPE_USER));
                    chatAdapter.notifyItemInserted(chatList.size() - 1);
                    if (rvChatMessages != null) rvChatMessages.scrollToPosition(chatList.size() - 1);
                    etChatInput.setText("");

                    // Add loading indicator
                    chatList.add(new ChatMessageModel("", "", ChatMessageModel.TYPE_LOADING));
                    chatAdapter.notifyItemInserted(chatList.size() - 1);
                    if (rvChatMessages != null) rvChatMessages.scrollToPosition(chatList.size() - 1);

                    new Handler().postDelayed(() -> {
                        // Remove loading indicator
                        if (!chatList.isEmpty() && chatList.get(chatList.size() - 1).getMessageType() == ChatMessageModel.TYPE_LOADING) {
                            chatList.remove(chatList.size() - 1);
                            chatAdapter.notifyItemRemoved(chatList.size());
                        }

                        String aiReply = "I have noted that. Let me look up the optimal growth patterns and diagnostics for your plant updates.";
                        chatList.add(new ChatMessageModel(aiReply, getCurrentPhTime(), ChatMessageModel.TYPE_AI));
                        chatAdapter.notifyItemInserted(chatList.size() - 1);
                        if (rvChatMessages != null) rvChatMessages.scrollToPosition(chatList.size() - 1);
                    }, 2000);
                }
            });
        }
        chatDialog.show();
    }

    private void openRecentsHistoryPanel(ArrayList<ChatMessageModel> activeChatList, ChatAdapter activeAdapter) {
        final BottomSheetDialog historySheet = new BottomSheetDialog(this);
        View historyView = LayoutInflater.from(this).inflate(R.layout.dialog_chat_history_panel, null);
        historySheet.setContentView(historyView);

        RecyclerView rvPastChats = historyView.findViewById(R.id.rv_past_conversations);

        ArrayList<String> recentTitles = new ArrayList<>();
        recentTitles.add("Pagsisimula ng SmartGrow UI Develo...");
        recentTitles.add("GitHub Collaboration Setup for Smart...");
        recentTitles.add("Integrated Chat and Camera UI");
        recentTitles.add("Android Studio SDK Versions Explained");
        recentTitles.add("Logo Enhancement to 4K");
        recentTitles.add("Pag-adjust ng Calorie Intake sa Tanghal...");
        recentTitles.add("Store Crew Hiring Process Explained");
        recentTitles.add("Building Your IT Student Portfolio");

        if (rvPastChats != null) {
            rvPastChats.setLayoutManager(new LinearLayoutManager(this));
            rvPastChats.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                @NonNull
                @Override
                public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                    View row = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_history_row, parent, false);
                    return new RecyclerView.ViewHolder(row) {};
                }

                @Override
                public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                    TextView tvTitle = holder.itemView.findViewById(R.id.tv_history_title);
                    if (tvTitle != null) {
                        tvTitle.setText(recentTitles.get(position));
                    }

                    holder.itemView.setOnClickListener(view -> {
                        if (activeChatList != null && activeAdapter != null) {
                            activeChatList.clear();
                            activeChatList.add(new ChatMessageModel("Loaded History session for: " + recentTitles.get(position), getCurrentPhTime(), ChatMessageModel.TYPE_AI));
                            activeAdapter.notifyDataSetChanged();
                        }
                        historySheet.dismiss();
                    });
                }

                @Override
                public int getItemCount() { return recentTitles.size(); }
            });
        }
        historySheet.show();
    }

    private String getCurrentPhTime() {
        TimeZone tz = TimeZone.getTimeZone("Asia/Manila");
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        timeFormat.setTimeZone(tz);
        return timeFormat.format(Calendar.getInstance(tz).getTime());
    }
}