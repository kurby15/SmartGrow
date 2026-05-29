package com.example.smartgrow;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private MaterialCardView cardActionNotification, cardActionGlobal, cardActionProfile, cardActionAddPlant;
    private RecyclerView rvMyPlantsList;
    private TextView tvTaskReminder;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // 🔗 1. Bind structural Top Header controls
        cardActionNotification = view.findViewById(R.id.card_action_notification);
        cardActionGlobal = view.findViewById(R.id.card_action_global);
        cardActionProfile = view.findViewById(R.id.card_action_profile);
        cardActionAddPlant = view.findViewById(R.id.card_action_add_plant);
        tvTaskReminder = view.findViewById(R.id.tv_task_reminder);

        // 🔗 2. Bind the Target List View
        rvMyPlantsList = view.findViewById(R.id.rv_my_plants_list);
        rvMyPlantsList.setLayoutManager(new LinearLayoutManager(getContext()));
        rvMyPlantsList.setHasFixedSize(true);

        // 🛠️ SETUP UX INTERACTION LISTENERS
        setupClickListeners();

        // ✨ SMARTGROW LINK STYLING
        setupTaskReminderLink();

        // 🌿 SETUP SIMULATED MOCK LIST VIEW FOR PRE-BACKEND TESTING
        setupMockPlantAdapter();

        return view;
    }

    private void setupClickListeners() {

        // 🔔 Trigger Click sa Notification Bell Icon!
        cardActionNotification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNotificationDialog();
            }
        });

        // 🟢 Trigger Click sa "See Task" link para mag-Slide Up ang To-Do Layout!
        tvTaskReminder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTodoBottomSheetDialog();
            }
        });

        // Global Community Trigger Link
        cardActionGlobal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(), "Connecting to Global Community...", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 🌟 Slide-Up Window Panel Handler para sa AI Assistant Chat System
    // INIWAN NATIN ITO DITO para kung sakaling kakailanganin mong tawagin ito locally.
    public void showAiChatAssistantBottomSheet() {
        final BottomSheetDialog chatDialog = new BottomSheetDialog(getContext());
        chatDialog.setContentView(R.layout.dialog_chat_assistant);

        // Force maximum layout behavior to enable smooth fullscreen slide dynamics
        chatDialog.getBehavior().setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);

        RecyclerView rvChatMessages = chatDialog.findViewById(R.id.rv_chat_messages_list);
        EditText etChatInput = chatDialog.findViewById(R.id.et_chat_input);
        FloatingActionButton fabSend = chatDialog.findViewById(R.id.fab_send_message);
        ImageButton ibMenu = chatDialog.findViewById(R.id.ib_chat_menu);

        // ➕ BAGONG DAGDAG: I-bind ang Plus/Attach ImageButton control mula sa XML
        ImageButton ibAttach = chatDialog.findViewById(R.id.ib_chat_attach);

        final ArrayList<ChatMessageModel> chatList = new ArrayList<>();

        // 💬 Nakaabang na initial text message galing kay AI Assistant
        chatList.add(new ChatMessageModel("Hi! I'm your Plant Smart Care Assistant. How can I help you today?", getCurrentTime(), ChatMessageModel.TYPE_AI));

        final ChatAssistantAdapter chatAdapter = new ChatAssistantAdapter(chatList);
        if (rvChatMessages != null) {
            rvChatMessages.setLayoutManager(new LinearLayoutManager(getContext()));
            rvChatMessages.setAdapter(chatAdapter);
        }

        // 📌 Tatlong Dote Click Listener para sa "See Chat History" Popup Menu!
        if (ibMenu != null) {
            ibMenu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PopupMenu popup = new PopupMenu(getContext(), v);
                    popup.getMenu().add("History");
                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            if (item.getTitle().equals("History")) {
                                Toast.makeText(getContext(), "Opening Chat History Logs...", Toast.LENGTH_SHORT).show();
                                return true;
                            }
                            return false;
                        }
                    });
                    popup.show();
                }
            });
        }

        // 📸 BAGONG DAGDAG: Plus Sign Click Listener para sa Camera at Gallery Operations!
        if (ibAttach != null) {
            ibAttach.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PopupMenu attachMenu = new PopupMenu(getContext(), v);
                    attachMenu.getMenu().add("Take a Photo");
                    attachMenu.getMenu().add("Upload from Gallery");

                    attachMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            if (item.getTitle().equals("Take a Photo")) {
                                Toast.makeText(getContext(), "Opening Camera Client...", Toast.LENGTH_SHORT).show();
                                return true;
                            } else if (item.getTitle().equals("Upload from Gallery")) {
                                Toast.makeText(getContext(), "Opening Media Storage Gallery...", Toast.LENGTH_SHORT).show();
                                return true;
                            }
                            return false;
                        }
                    });
                    attachMenu.show();
                }
            });
        }

        // 🚀 Real-time typing message sender block execution
        if (fabSend != null && etChatInput != null) {
            fabSend.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String userText = etChatInput.getText().toString().trim();
                    if (!userText.isEmpty()) {
                        chatList.add(new ChatMessageModel(userText, getCurrentTime(), ChatMessageModel.TYPE_USER));
                        chatAdapter.notifyItemInserted(chatList.size() - 1);
                        if (rvChatMessages != null) rvChatMessages.scrollToPosition(chatList.size() - 1);

                        etChatInput.setText("");

                        chatList.add(new ChatMessageModel("", "", ChatMessageModel.TYPE_LOADING));
                        chatAdapter.notifyItemInserted(chatList.size() - 1);
                        if (rvChatMessages != null) rvChatMessages.scrollToPosition(chatList.size() - 1);

                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                int loadingIndex = chatList.size() - 1;
                                if (loadingIndex >= 0 && chatList.get(loadingIndex).getMessageType() == ChatMessageModel.TYPE_LOADING) {
                                    chatList.remove(loadingIndex);
                                    chatAdapter.notifyItemRemoved(loadingIndex);
                                }

                                String aiReply = "I have noted that. Let me look up the optimal growth patterns and diagnostics for your plant updates.";
                                chatList.add(new ChatMessageModel(aiReply, getCurrentTime(), ChatMessageModel.TYPE_AI));
                                chatAdapter.notifyItemInserted(chatList.size() - 1);
                                if (rvChatMessages != null) rvChatMessages.scrollToPosition(chatList.size() - 1);
                            }
                        }, 2000);
                    }
                }
            });
        }

        chatDialog.show();
    }

    private String getCurrentTime() {
        return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
    }

    private void setupTaskReminderLink() {
        String fullText = "You have 2 tasks pending today. See Task";
        android.text.SpannableString spannableString = new android.text.SpannableString(fullText);
        int startIndex = fullText.indexOf("See Task");
        int endIndex = startIndex + "See Task".length();

        if (startIndex != -1) {
            spannableString.setSpan(new android.text.style.ForegroundColorSpan(Color.parseColor("#0C6211")), startIndex, endIndex, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannableString.setSpan(new android.text.style.UnderlineSpan(), startIndex, endIndex, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannableString.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), startIndex, endIndex, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        tvTaskReminder.setText(spannableString);
    }

    private void showTodoBottomSheetDialog() {
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext());
        bottomSheetDialog.setContentView(R.layout.dialog_todo_sheet);
        RecyclerView rvTodoTasks = bottomSheetDialog.findViewById(R.id.rv_todo_tasks_list);
        if (rvTodoTasks != null) {
            rvTodoTasks.setLayoutManager(new LinearLayoutManager(getContext()));
            rvTodoTasks.setHasFixedSize(true);
            setupMockTaskAdapter(rvTodoTasks);
        }
        bottomSheetDialog.show();
    }

    private void setupMockTaskAdapter(RecyclerView recyclerView) { }

    private void showNotificationDialog() {
        final Dialog dialog = new Dialog(getContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_notification);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        MaterialCardView btnClose = dialog.findViewById(R.id.btn_close_notification);
        if (btnClose != null) {
            btnClose.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) { dialog.dismiss(); }
            });
        }
        dialog.show();
    }

    private void setupMockPlantAdapter() { }


    // 🛠️ HAKBANG 2: DAGDAG NA HELPER METHOD PARA SA MAIN ACTIVITY CLICK OPERATION
    // Pinapagana nito ang slide-up layout kahit nasaan mang fragment tab ang user
    public void showAiChatAssistantBottomSheetFromActivity(android.content.Context context) {
        final BottomSheetDialog chatDialog = new BottomSheetDialog(context);
        chatDialog.setContentView(R.layout.dialog_chat_assistant);

        chatDialog.getBehavior().setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);

        RecyclerView rvChatMessages = chatDialog.findViewById(R.id.rv_chat_messages_list);
        EditText etChatInput = chatDialog.findViewById(R.id.et_chat_input);
        FloatingActionButton fabSend = chatDialog.findViewById(R.id.fab_send_message);
        ImageButton ibMenu = chatDialog.findViewById(R.id.ib_chat_menu);
        ImageButton ibAttach = chatDialog.findViewById(R.id.ib_chat_attach);

        final ArrayList<ChatMessageModel> chatList = new ArrayList<>();
        chatList.add(new ChatMessageModel("Hi! I'm your Plant Smart Care Assistant. How can I help you today?", getCurrentTime(), ChatMessageModel.TYPE_AI));

        final ChatAssistantAdapter chatAdapter = new ChatAssistantAdapter(chatList);
        if (rvChatMessages != null) {
            rvChatMessages.setLayoutManager(new LinearLayoutManager(context));
            rvChatMessages.setAdapter(chatAdapter);
        }

        // Pop-up menu para sa Chat History
        if (ibMenu != null) {
            ibMenu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(context, v);
                    popup.getMenu().add("History");
                    popup.setOnMenuItemClickListener(new androidx.appcompat.widget.PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            if (item.getTitle().equals("History")) {
                                Toast.makeText(context, "Opening Chat History Logs...", Toast.LENGTH_SHORT).show();
                                return true;
                            }
                            return false;
                        }
                    });
                    popup.show();
                }
            });
        }

        // Attach operations menu (Camera & Media Upload)
        if (ibAttach != null) {
            ibAttach.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    androidx.appcompat.widget.PopupMenu attachMenu = new androidx.appcompat.widget.PopupMenu(context, v);
                    attachMenu.getMenu().add("Take a Photo");
                    attachMenu.getMenu().add("Upload from Gallery");

                    attachMenu.setOnMenuItemClickListener(new androidx.appcompat.widget.PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            if (item.getTitle().equals("Take a Photo")) {
                                Toast.makeText(context, "Opening Camera Client...", Toast.LENGTH_SHORT).show();
                                return true;
                            } else if (item.getTitle().equals("Upload from Gallery")) {
                                Toast.makeText(context, "Opening Media Storage Gallery...", Toast.LENGTH_SHORT).show();
                                return true;
                            }
                            return false;
                        }
                    });
                    attachMenu.show();
                }
            });
        }

        // Typing sender logic
        if (fabSend != null && etChatInput != null) {
            fabSend.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String userText = etChatInput.getText().toString().trim();
                    if (!userText.isEmpty()) {
                        chatList.add(new ChatMessageModel(userText, getCurrentTime(), ChatMessageModel.TYPE_USER));
                        chatAdapter.notifyItemInserted(chatList.size() - 1);
                        if (rvChatMessages != null) rvChatMessages.scrollToPosition(chatList.size() - 1);

                        etChatInput.setText("");

                        chatList.add(new ChatMessageModel("", "", ChatMessageModel.TYPE_LOADING));
                        chatAdapter.notifyItemInserted(chatList.size() - 1);

                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                int loadingIndex = chatList.size() - 1;
                                if (loadingIndex >= 0 && chatList.get(loadingIndex).getMessageType() == ChatMessageModel.TYPE_LOADING) {
                                    chatList.remove(loadingIndex);
                                    chatAdapter.notifyItemRemoved(loadingIndex);
                                }

                                String aiReply = "I have noted that. Let me look up the optimal growth patterns and diagnostics for your plant updates.";
                                chatList.add(new ChatMessageModel(aiReply, getCurrentTime(), ChatMessageModel.TYPE_AI));
                                chatAdapter.notifyItemInserted(chatList.size() - 1);
                                if (rvChatMessages != null) rvChatMessages.scrollToPosition(chatList.size() - 1);
                            }
                        }, 2000);
                    }
                }
            });
        }

        chatDialog.show();
    }
}