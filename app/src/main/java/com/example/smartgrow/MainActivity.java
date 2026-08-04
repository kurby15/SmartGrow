package com.example.smartgrow;

import android.Manifest;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {

    private MaterialCardView cardNavChatAssistant;
    private MaterialCardView cardActionNotification, cardActionGlobal, cardActionProfile;

    private ArrayList<ChatMessageModel> activeChatList;
    private ChatAdapter activeChatAdapter;
    private RecyclerView activeRvChatMessages;

    // Persistent tracking container keeping follow-up models linked to current context rules
    private String lastAnalyzedPlantProfile = "";

    // Throttle tracking variable to prevent rapid request loops causing 429 Rate Limits
    private long lastRequestTime = 0;

    private ActivityResultLauncher<Void> cameraLauncher;
    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupImageLaunchers();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation_bar);
        cardNavChatAssistant = findViewById(R.id.card_nav_chat_assistant);
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

        setupGlobalHeaderListeners();

        if (cardNavChatAssistant != null) {
            cardNavChatAssistant.setOnClickListener(v -> showAiChatAssistantBottomSheet());
        }

        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (current instanceof UserProfileFragment || current instanceof ArchiveFragment) {
                hideSystemBars();
            } else {
                showSystemBars();
            }
        });
    }

    private void setupImageLaunchers() {
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicturePreview(),
                bitmap -> {
                    if (bitmap != null) {
                        handleImageAnalysis(getResizedBitmap(bitmap, 1024));
                    } else {
                        Toast.makeText(this, "No image captured.", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        cameraLauncher.launch(null);
                    } else {
                        Toast.makeText(this, "Camera permission is required.", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        try {
                            Bitmap bitmap;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                bitmap = ImageDecoder.decodeBitmap(
                                        ImageDecoder.createSource(getContentResolver(), uri));
                            } else {
                                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                            }
                            handleImageAnalysis(getResizedBitmap(bitmap, 1024));
                        } catch (IOException e) {
                            Toast.makeText(this, "Failed to load image: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    private Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();
        float ratio = (float) width / height;

        if (ratio > 1) {
            width = maxSize;
            height = (int) (width / ratio);
        } else {
            height = maxSize;
            width = (int) (height * ratio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }

    private void handleImageAnalysis(Bitmap bitmap) {
        if (activeChatList == null || activeChatAdapter == null) {
            Toast.makeText(this, "Please open the Chat Assistant first.", Toast.LENGTH_SHORT).show();
            return;
        }

        activeChatList.add(new ChatMessageModel(
                bitmap,
                getCurrentPhTime(),
                ChatMessageModel.TYPE_USER));
        activeChatAdapter.notifyItemInserted(activeChatList.size() - 1);

        activeChatList.add(new ChatMessageModel("", "", ChatMessageModel.TYPE_LOADING));
        activeChatAdapter.notifyItemInserted(activeChatList.size() - 1);
        if (activeRvChatMessages != null) {
            activeRvChatMessages.scrollToPosition(activeChatList.size() - 1);
        }

        PlantAnalyzer analyzer = new PlantAnalyzer();
        analyzer.analyzePlant(bitmap, new PlantAnalyzer.PlantCallback() {
            @Override
            public void onSuccess(String structuredResult) {
                runOnUiThread(() -> {
                    removeLoadingIndicator();

                    // Save the result into context history unconditionally unless it's the reject message
                    lastAnalyzedPlantProfile = structuredResult != null ? structuredResult : "Uploaded Plant Image Context";

                    ArrayList<String> suggestions = new ArrayList<>();

                    // ✅ Synchronized with PlantAnalyzer's structural rejection strategy
                    if (structuredResult.contains("does not appear to contain a plant")) {
                        suggestions.add("How to scan correctly?");
                        suggestions.add("See sample plant image");
                    } else {
                        // --- INITIAL CONTEXTUAL GENERATION ---
                        String plantName = "this plant";
                        String majorSymptom = "";

                        try {
                            // Prefer Local Name for chips if available in layout profile
                            if (structuredResult.contains("• Local Name: ")) {
                                int localStart = structuredResult.indexOf("• Local Name: ") + "• Local Name: ".length();
                                int localEnd = structuredResult.indexOf("\n", localStart);
                                if (localEnd > localStart) {
                                    plantName = structuredResult.substring(localStart, localEnd).trim();
                                }
                            } else if (structuredResult.contains("• Name: ")) {
                                int nameStart = structuredResult.indexOf("• Name: ") + "• Name: ".length();
                                int nameEnd = structuredResult.indexOf("\n", nameStart);
                                if (nameEnd > nameStart) {
                                    plantName = structuredResult.substring(nameStart, nameEnd).trim();
                                }
                            }

                            if (structuredResult.contains("⚠️ Problems Detected")) {
                                int problemSectionStart = structuredResult.indexOf("⚠️ Problems Detected");
                                int lineStart = structuredResult.indexOf("• ", problemSectionStart);
                                if (lineStart != -1) {
                                    int lineEnd = structuredResult.indexOf("\n", lineStart);
                                    if (lineEnd > lineStart) {
                                        majorSymptom = structuredResult.substring(lineStart + 2, lineEnd).trim();
                                    }
                                }
                            }
                        } catch (Exception e) {
                            plantName = "this plant";
                        }

                        suggestions.add("Is " + plantName + " safe for pets?");
                        if (!majorSymptom.isEmpty()) {
                            suggestions.add("How to treat " + majorSymptom + "?");
                        } else {
                            suggestions.add("What fertilizer does " + plantName + " like?");
                        }
                        suggestions.add("How often should I water it?");
                    }

                    ChatMessageModel aiMessage = new ChatMessageModel(structuredResult, getCurrentPhTime(), ChatMessageModel.TYPE_AI);
                    aiMessage.setFollowUpSuggestions(suggestions);

                    activeChatList.add(aiMessage);
                    activeChatAdapter.notifyItemInserted(activeChatList.size() - 1);
                    if (activeRvChatMessages != null) {
                        activeRvChatMessages.scrollToPosition(activeChatList.size() - 1);
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    removeLoadingIndicator();

                    // Provide a default fallback context state on network error
                    lastAnalyzedPlantProfile = "Context recovery: Vision parsing error/timeout occurred.";

                    activeChatList.add(new ChatMessageModel(
                            "Sorry, I encountered an internal communication issue: " + error,
                            getCurrentPhTime(),
                            ChatMessageModel.TYPE_AI));
                    activeChatAdapter.notifyItemInserted(activeChatList.size() - 1);
                    if (activeRvChatMessages != null) {
                        activeRvChatMessages.scrollToPosition(activeChatList.size() - 1);
                    }
                });
            }
        });
    }

    public void submitFollowUpQuestion(String question) {
        if (activeChatList == null || activeChatAdapter == null) return;

        // Add the user message to the UI
        activeChatList.add(new ChatMessageModel(question, getCurrentPhTime(), ChatMessageModel.TYPE_USER));
        activeChatAdapter.notifyItemInserted(activeChatList.size() - 1);
        if (activeRvChatMessages != null) {
            activeRvChatMessages.scrollToPosition(activeChatList.size() - 1);
        }

        // Add the loading indicator to the UI
        activeChatList.add(new ChatMessageModel("", "", ChatMessageModel.TYPE_LOADING));
        activeChatAdapter.notifyItemInserted(activeChatList.size() - 1);
        if (activeRvChatMessages != null) {
            activeRvChatMessages.scrollToPosition(activeChatList.size() - 1);
        }

        PlantAnalyzer analyzer = new PlantAnalyzer();

        PlantAnalyzer.PlantCallback aiCallback = new PlantAnalyzer.PlantCallback() {
            @Override
            public void onSuccess(String aiReply) {
                runOnUiThread(() -> {
                    removeLoadingIndicator();

                    ArrayList<String> suggestions = new ArrayList<>();

                    // ✅ Synchronized verification for system assistant guardrail blocks
                    if (aiReply.contains("only answer questions related to plants")) {
                        suggestions.add("Give me care tips for a Monstera.");
                        suggestions.add("How often should I water succulents?");
                    } else if (!aiReply.contains("does not appear to contain a plant") && !aiReply.startsWith("Please upload")) {
                        String plantName = "it";
                        try {
                            if (lastAnalyzedPlantProfile != null) {
                                if (lastAnalyzedPlantProfile.contains("• Local Name: ")) {
                                    int localStart = lastAnalyzedPlantProfile.indexOf("• Local Name: ") + "• Local Name: ".length();
                                    int localEnd = lastAnalyzedPlantProfile.indexOf("\n", localStart);
                                    if (localEnd > localStart) {
                                        plantName = lastAnalyzedPlantProfile.substring(localStart, localEnd).trim();
                                    }
                                } else if (lastAnalyzedPlantProfile.contains("• Name: ")) {
                                    int nameStart = lastAnalyzedPlantProfile.indexOf("• Name: ") + "• Name: ".length();
                                    int nameEnd = lastAnalyzedPlantProfile.indexOf("\n", nameStart);
                                    if (nameEnd > nameStart) {
                                        plantName = lastAnalyzedPlantProfile.substring(nameStart, nameEnd).trim();
                                    }
                                }
                            }
                        } catch (Exception e) {
                            plantName = "it";
                        }

                        String lowerReply = aiReply.toLowerCase();
                        String lowerQuestion = question.toLowerCase();

                        if (lowerQuestion.contains("water") || lowerReply.contains("watering") || lowerReply.contains("moisture")) {
                            suggestions.add("What type of soil drains best for " + plantName + "?");
                            suggestions.add("Signs of overwatering vs underwatering?");
                            suggestions.add("What are its sunlight needs?");
                        } else if (lowerQuestion.contains("treat") || lowerQuestion.contains("pest") || lowerReply.contains("disease") || lowerReply.contains("fungus")) {
                            suggestions.add("How do I prevent this returning?");
                            suggestions.add("Should I cut off damaged leaves?");
                            suggestions.add("Is it safe to use Neem oil?");
                        } else if (lowerQuestion.contains("fertilizer") || lowerReply.contains("nutrient") || lowerReply.contains("growth")) {
                            suggestions.add("How often to fertilize in winter?");
                            suggestions.add("What is the ideal NPK ratio?");
                            suggestions.add("When should I repot " + plantName + "?");
                        } else {
                            suggestions.add("Tell me about ideal humidity/temperature.");
                            suggestions.add("Can I propagate this plant?");
                            suggestions.add("What is the general care checklist?");
                        }
                    }

                    ChatMessageModel msg = new ChatMessageModel(aiReply, getCurrentPhTime(), ChatMessageModel.TYPE_AI);
                    msg.setFollowUpSuggestions(suggestions);

                    activeChatList.add(msg);
                    activeChatAdapter.notifyItemInserted(activeChatList.size() - 1);
                    if (activeRvChatMessages != null) {
                        activeRvChatMessages.scrollToPosition(activeChatList.size() - 1);
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    removeLoadingIndicator();
                    activeChatList.add(new ChatMessageModel(
                            "Sorry, I couldn't process your request: " + error,
                            getCurrentPhTime(),
                            ChatMessageModel.TYPE_AI));
                    activeChatAdapter.notifyItemInserted(activeChatList.size() - 1);
                    if (activeRvChatMessages != null) {
                        activeRvChatMessages.scrollToPosition(activeChatList.size() - 1);
                    }
                });
            }
        };

        // Router engine setup
        if (lastAnalyzedPlantProfile == null || lastAnalyzedPlantProfile.trim().isEmpty()) {
            analyzer.askQuestion(question, aiCallback);
        } else {
            analyzer.askFollowUpQuestion(question, lastAnalyzedPlantProfile, aiCallback);
        }
    }

    private void hideSystemBars() {
        if (findViewById(R.id.layout_top_header) != null)
            findViewById(R.id.layout_top_header).setVisibility(View.GONE);
        if (findViewById(R.id.bottom_navigation_bar) != null)
            findViewById(R.id.bottom_navigation_bar).setVisibility(View.GONE);
        if (cardNavChatAssistant != null)
            cardNavChatAssistant.setVisibility(View.GONE);
        if (findViewById(R.id.view_nav_shadow) != null)
            findViewById(R.id.view_nav_shadow).setVisibility(View.GONE);
    }

    private void showSystemBars() {
        if (findViewById(R.id.layout_top_header) != null)
            findViewById(R.id.layout_top_header).setVisibility(View.VISIBLE);
        if (findViewById(R.id.bottom_navigation_bar) != null)
            findViewById(R.id.bottom_navigation_bar).setVisibility(View.VISIBLE);
        if (cardNavChatAssistant != null)
            cardNavChatAssistant.setVisibility(View.VISIBLE);
        if (findViewById(R.id.view_nav_shadow) != null)
            findViewById(R.id.view_nav_shadow).setVisibility(View.VISIBLE);
    }

    private void setupGlobalHeaderListeners() {
        if (cardActionNotification != null) {
            cardActionNotification.setOnClickListener(v -> showNotificationDialog());
        }
        if (cardActionGlobal != null) {
            cardActionGlobal.setOnClickListener(v -> {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new CommunityForumFragment())
                        .addToBackStack(null)
                        .commit();
            });
        }
        if (cardActionProfile != null) {
            cardActionProfile.setOnClickListener(v -> {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new ProfileFragment())
                        .addToBackStack(null)
                        .commit();
            });
        }
    }

    private void showNotificationDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_notification);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        MaterialCardView btnClose = dialog.findViewById(R.id.btn_close_notification);
        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    public void showAiChatAssistantBottomSheet() {
        final BottomSheetDialog chatDialog = new BottomSheetDialog(this);
        View chatView = LayoutInflater.from(this).inflate(R.layout.dialog_chat_assistant, null);
        chatDialog.setContentView(chatView);
        chatDialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);

        activeRvChatMessages = chatView.findViewById(R.id.rv_chat_messages_list);
        EditText etChatInput = chatView.findViewById(R.id.et_chat_input);
        FloatingActionButton fabSend = chatView.findViewById(R.id.fab_send_message);
        ImageButton ibMenu = chatView.findViewById(R.id.ib_chat_menu);
        ImageButton ibAttach = chatView.findViewById(R.id.ib_chat_attach);

        activeChatList = new ArrayList<>();

        ArrayList<String> welcomeSuggestions = new ArrayList<>();
        welcomeSuggestions.add("How do I scan a plant?");
        welcomeSuggestions.add("Give me care tips for a Monstera.");

        ChatMessageModel welcomeMessage = new ChatMessageModel(
                "Hi! I'm SmartGrow AI, your Plant Smart Care Assistant. How can I help you today?",
                getCurrentPhTime(),
                ChatMessageModel.TYPE_AI);
        welcomeMessage.setFollowUpSuggestions(welcomeSuggestions);
        activeChatList.add(welcomeMessage);

        activeChatAdapter = new ChatAdapter(activeChatList);
        if (activeRvChatMessages != null) {
            activeRvChatMessages.setLayoutManager(new LinearLayoutManager(this));
            activeRvChatMessages.setAdapter(activeChatAdapter);
        }

        if (ibMenu != null) {
            ibMenu.setOnClickListener(v -> {
                View dropdownView = LayoutInflater.from(this)
                        .inflate(R.layout.layout_custom_dropdown, null);
                LinearLayout llNewChat = dropdownView.findViewById(R.id.item_dropdown_new_chat);
                LinearLayout llHistory = dropdownView.findViewById(R.id.item_dropdown_history);

                PopupWindow menu = new PopupWindow(dropdownView,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT, true);
                menu.setOutsideTouchable(true);
                menu.setFocusable(true);
                menu.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

                if (llNewChat != null) {
                    llNewChat.setOnClickListener(view -> {
                        activeChatList.clear();
                        lastAnalyzedPlantProfile = "";
                        activeChatList.add(new ChatMessageModel(
                                "Hi! This is a fresh new chat session. Ask me anything about your plants!",
                                getCurrentPhTime(),
                                ChatMessageModel.TYPE_AI));
                        activeChatAdapter.notifyDataSetChanged();
                        Toast.makeText(this, "New Chat Started", Toast.LENGTH_SHORT).show();
                        menu.dismiss();
                    });
                }

                if (llHistory != null) {
                    llHistory.setOnClickListener(view -> {
                        menu.dismiss();
                        openRecentsHistoryPanel();
                    });
                }
                menu.showAsDropDown(v, -280, 10);
            });
        }

        if (ibAttach != null) {
            ibAttach.setOnClickListener(v -> {
                View attachView = LayoutInflater.from(this)
                        .inflate(R.layout.layout_attach_dropdown, null);
                LinearLayout llTakePhoto = attachView.findViewById(R.id.item_dropdown_take_photo);
                LinearLayout llUploadGallery = attachView.findViewById(R.id.item_dropdown_upload_gallery);

                PopupWindow attachMenu = new PopupWindow(attachView,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT, true);
                attachMenu.setOutsideTouchable(true);
                attachMenu.setFocusable(true);
                attachMenu.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

                if (llTakePhoto != null) {
                    llTakePhoto.setOnClickListener(view -> {
                        attachMenu.dismiss();
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                                == PackageManager.PERMISSION_GRANTED) {
                            cameraLauncher.launch(null);
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
                        }
                    });
                }

                if (llUploadGallery != null) {
                    llUploadGallery.setOnClickListener(view -> {
                        attachMenu.dismiss();
                        galleryLauncher.launch("image/*");
                    });
                }

                attachView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                int yOffset = -(attachView.getMeasuredHeight() + v.getHeight() + 15);
                attachMenu.showAsDropDown(v, 10, yOffset);
            });
        }

        if (fabSend != null && etChatInput != null) {
            fabSend.setOnClickListener(v -> {
                String userText = etChatInput.getText().toString().trim();
                if (userText.isEmpty()) return;

                // ⏳ COOLDOWN CHECK: Enforce a 3000ms delay to prevent 429 requests
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastRequestTime < 3000) {
                    Toast.makeText(this, "Please wait a moment before sending another message.", Toast.LENGTH_SHORT).show();
                    return;
                }

                lastRequestTime = currentTime;
                etChatInput.setText("");
                submitFollowUpQuestion(userText);
            });
        }

        chatDialog.show();
    }

    private void removeLoadingIndicator() {
        if (activeChatList != null && !activeChatList.isEmpty()
                && activeChatList.get(activeChatList.size() - 1).getMessageType()
                == ChatMessageModel.TYPE_LOADING) {
            int index = activeChatList.size() - 1;
            activeChatList.remove(index);
            if (activeChatAdapter != null) {
                activeChatAdapter.notifyItemRemoved(index);
            }
        }
    }

    private void openRecentsHistoryPanel() {
        BottomSheetDialog historySheet = new BottomSheetDialog(this);
        View historyView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_chat_history_panel, null);
        historySheet.setContentView(historyView);

        RecyclerView rvPastChats = historyView.findViewById(R.id.rv_past_conversations);

        ArrayList<String> recentTitles = new ArrayList<>();
        recentTitles.add("Monstera Care Routine");
        recentTitles.add("Succulent Overwatering Fixes");
        recentTitles.add("Tomato Blight Management");
        recentTitles.add("Bonsai Soil Configuration");

        if (rvPastChats != null) {
            rvPastChats.setLayoutManager(new LinearLayoutManager(this));
            rvPastChats.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                @NonNull
                @Override
                public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                    View row = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.item_chat_history_row, parent, false);
                    return new RecyclerView.ViewHolder(row) {};
                }

                @Override
                public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                    TextView tvTitle = holder.itemView.findViewById(R.id.tv_history_title);
                    if (tvTitle != null) {
                        tvTitle.setText(recentTitles.get(position));
                    }
                    holder.itemView.setOnClickListener(view -> {
                        if (activeChatList != null && activeChatAdapter != null) {
                            activeChatList.clear();
                            String dummyHistoryContext = "🌿 Plant Profile\n• Name: " + recentTitles.get(position);
                            lastAnalyzedPlantProfile = dummyHistoryContext;

                            activeChatList.add(new ChatMessageModel(
                                    "Loaded History session for: " + recentTitles.get(position),
                                    getCurrentPhTime(),
                                    ChatMessageModel.TYPE_AI));
                            activeChatAdapter.notifyDataSetChanged();
                        }
                        historySheet.dismiss();
                    });
                }

                @Override
                public int getItemCount() {
                    return recentTitles.size();
                }
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