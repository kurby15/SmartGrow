package com.example.smartgrow;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartgrow.camera.CameraScannerActivity;
import com.example.smartgrow.camera.ChatAdapter;
import com.example.smartgrow.camera.ChatMessageModel;
import com.example.smartgrow.camera.PlantAnalyzer;
import com.example.smartgrow.community.ArchiveFragment;
import com.example.smartgrow.community.CommunityForumFragment;
import com.example.smartgrow.plants.DiaryFragment;
import com.example.smartgrow.plants.HomeFragment;
import com.example.smartgrow.profile.ProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {

    private MaterialCardView cardNavChatAssistant;
    private MaterialCardView cardActionNotification, cardActionGlobal, cardActionProfile;

    // Persisted Chat Memory and Dialog State
    private BottomSheetDialog activeChatDialog;
    private ArrayList<ChatMessageModel> activeChatList;
    private ChatAdapter activeChatAdapter;
    private RecyclerView activeRvChatMessages;

    private String lastAnalyzedPlantProfile = "";
    private long lastRequestTime = 0;

    // Activity Result Launchers
    private ActivityResultLauncher<Intent> cameraScannerLauncher;
    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupLaunchers();

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

        // Show choice popup on center assistant click (Scan or Chat)
        if (cardNavChatAssistant != null) {
            cardNavChatAssistant.setOnClickListener(this::showAssistantPopupMenu);
        }

        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (current instanceof ProfileFragment || current instanceof ArchiveFragment) {
                hideSystemBars();
            } else {
                showSystemBars();
            }
        });
    }

    private void showAssistantPopupMenu(View anchorView) {
        View popupView = LayoutInflater.from(this).inflate(R.layout.layout_modern_popup, null);

        PopupWindow popupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );

        popupWindow.setOutsideTouchable(true);
        popupWindow.setFocusable(true);
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            popupWindow.setElevation(20f);
        }

        LinearLayout itemScanPlant = popupView.findViewById(R.id.item_scan_plant);
        LinearLayout itemAiChatbot = popupView.findViewById(R.id.item_ai_chatbot);

        if (itemScanPlant != null) {
            itemScanPlant.setOnClickListener(v -> {
                popupWindow.dismiss();
                launchCameraScanner();
            });
        }

        if (itemAiChatbot != null) {
            itemAiChatbot.setOnClickListener(v -> {
                popupWindow.dismiss();
                showAiChatAssistantBottomSheet();
            });
        }

        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int popupWidth = popupView.getMeasuredWidth();
        int popupHeight = popupView.getMeasuredHeight();

        int xOffset = (anchorView.getWidth() - popupWidth) / 2;
        int yOffset = -(popupHeight + anchorView.getHeight() + 16);

        popupWindow.showAsDropDown(anchorView, xOffset, yOffset);
    }

    private void setupLaunchers() {
        cameraScannerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        String imagePath = result.getData().getStringExtra(CameraScannerActivity.EXTRA_IMAGE_PATH);
                        if (imagePath != null) {
                            try (FileInputStream is = openFileInput(imagePath)) {
                                Bitmap bitmap = BitmapFactory.decodeStream(is);
                                if (bitmap != null) {
                                    showAiChatAssistantBottomSheet();
                                    Bitmap resized = getResizedBitmap(bitmap, 1024);
                                    handleImageAnalysis(resized);
                                    // Recycle the original high-res bitmap if a new one was created
                                    if (resized != bitmap) {
                                        bitmap.recycle();
                                    }
                                } else {
                                    Toast.makeText(this, "Error: Could not decode image.", Toast.LENGTH_SHORT).show();
                                }
                            } catch (IOException e) {
                                Toast.makeText(this, "Failed to load scanned image.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
        );

        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        launchCameraScanner();
                    } else {
                        Toast.makeText(this, "Camera permission is required to launch AI scanner.", Toast.LENGTH_SHORT).show();
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
                                try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
                                    bitmap = BitmapFactory.decodeStream(inputStream);
                                }
                            }

                            if (bitmap != null) {
                                showAiChatAssistantBottomSheet();
                                Bitmap resized = getResizedBitmap(bitmap, 1024);
                                handleImageAnalysis(resized);
                                if (resized != bitmap) {
                                    bitmap.recycle();
                                }
                            }
                        } catch (IOException e) {
                            Toast.makeText(this, "Failed to load image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    private void launchCameraScanner() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(this, CameraScannerActivity.class);
            cameraScannerLauncher.launch(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        if (image == null) return null;
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

    private String getCurrentPhTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Manila"));
        return sdf.format(new Date());
    }

    public void showAiChatAssistantBottomSheet() {
        if (activeChatDialog != null && activeChatDialog.isShowing()) {
            activeChatDialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
            return;
        }

        if (activeChatList == null) {
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
        }

        if (activeChatAdapter == null) {
            activeChatAdapter = new ChatAdapter(activeChatList);
        }

        activeChatDialog = new BottomSheetDialog(this);
        View chatView = LayoutInflater.from(this).inflate(R.layout.dialog_chat_assistant, null);
        activeChatDialog.setContentView(chatView);
        activeChatDialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);

        activeRvChatMessages = chatView.findViewById(R.id.rv_chat_messages_list);
        EditText etChatInput = chatView.findViewById(R.id.et_chat_input);
        FloatingActionButton fabSend = chatView.findViewById(R.id.fab_send_message);
        ImageButton ibMenu = chatView.findViewById(R.id.ib_chat_menu);
        ImageButton ibAttach = chatView.findViewById(R.id.ib_chat_attach);

        if (activeRvChatMessages != null) {
            activeRvChatMessages.setLayoutManager(new LinearLayoutManager(this));
            activeRvChatMessages.setAdapter(activeChatAdapter);
            activeRvChatMessages.scrollToPosition(activeChatList.size() - 1);
        }

        activeChatDialog.setOnDismissListener(dialog -> activeRvChatMessages = null);

        if (ibMenu != null) {
            ibMenu.setOnClickListener(v -> {
                View dropdownView = LayoutInflater.from(this).inflate(R.layout.layout_custom_dropdown, null);
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
                        menu.dismiss();
                        startNewChatSession();
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
                View attachView = LayoutInflater.from(this).inflate(R.layout.layout_attach_dropdown, null);
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
                        launchCameraScanner();
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

        activeChatDialog.show();
    }

    private void startNewChatSession() {
        if (activeChatList != null) {
            activeChatList.clear();
            lastAnalyzedPlantProfile = "";
            ChatMessageModel newWelcomeMsg = new ChatMessageModel(
                    "Hi! This is a fresh new chat session. Ask me anything about your plants!",
                    getCurrentPhTime(),
                    ChatMessageModel.TYPE_AI);
            activeChatList.add(newWelcomeMsg);

            if (activeChatAdapter != null) {
                activeChatAdapter.notifyDataSetChanged();
            }
        }
        Toast.makeText(this, "New Chat Started", Toast.LENGTH_SHORT).show();
    }

    private void handleImageAnalysis(Bitmap bitmap) {
        if (bitmap == null) return;

        if (activeChatList == null || activeChatAdapter == null) {
            showAiChatAssistantBottomSheet();
        }

        activeChatList.add(new ChatMessageModel(bitmap, getCurrentPhTime(), ChatMessageModel.TYPE_USER));
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
                    if (activeChatList == null || activeChatAdapter == null) return;

                    lastAnalyzedPlantProfile = structuredResult != null ? structuredResult : "Uploaded Plant Image Context";
                    ArrayList<String> suggestions = new ArrayList<>();

                    if (structuredResult != null && structuredResult.contains("does not appear to contain a plant")) {
                        suggestions.add("How to scan correctly?");
                        suggestions.add("See sample plant image");
                    } else {
                        String plantName = "this plant";
                        String majorSymptom = "";

                        try {
                            if (structuredResult != null && structuredResult.contains("• Local Name: ")) {
                                int localStart = structuredResult.indexOf("• Local Name: ") + "• Local Name: ".length();
                                int localEnd = structuredResult.indexOf("\n", localStart);
                                if (localEnd == -1) localEnd = structuredResult.length();
                                plantName = structuredResult.substring(localStart, localEnd).trim();
                            } else if (structuredResult != null && structuredResult.contains("• Name: ")) {
                                int nameStart = structuredResult.indexOf("• Name: ") + "• Name: ".length();
                                int nameEnd = structuredResult.indexOf("\n", nameStart);
                                if (nameEnd == -1) nameEnd = structuredResult.length();
                                plantName = structuredResult.substring(nameStart, nameEnd).trim();
                            }

                            if (structuredResult != null && structuredResult.contains("⚠️ Problems Detected")) {
                                int problemSectionStart = structuredResult.indexOf("⚠️ Problems Detected");
                                int lineStart = structuredResult.indexOf("• ", problemSectionStart);
                                if (lineStart != -1) {
                                    int lineEnd = structuredResult.indexOf("\n", lineStart);
                                    if (lineEnd == -1) lineEnd = structuredResult.length();
                                    majorSymptom = structuredResult.substring(lineStart + 2, lineEnd).trim();
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
                    if (activeChatList == null || activeChatAdapter == null) return;

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

        activeChatList.add(new ChatMessageModel(question, getCurrentPhTime(), ChatMessageModel.TYPE_USER));
        activeChatAdapter.notifyItemInserted(activeChatList.size() - 1);

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
                    if (activeChatList == null || activeChatAdapter == null) return;

                    ArrayList<String> suggestions = new ArrayList<>();

                    if (aiReply.contains("only answer questions related to plants")) {
                        suggestions.add("Give me care tips for a Monstera.");
                        suggestions.add("How often should I water succulents?");
                    } else if (!aiReply.contains("does not appear to contain a plant") && !aiReply.startsWith("Please upload")) {
                        String plantName = "it";
                        try {
                            if (lastAnalyzedPlantProfile != null && !lastAnalyzedPlantProfile.isEmpty()) {
                                if (lastAnalyzedPlantProfile.contains("• Local Name: ")) {
                                    int localStart = lastAnalyzedPlantProfile.indexOf("• Local Name: ") + "• Local Name: ".length();
                                    int localEnd = lastAnalyzedPlantProfile.indexOf("\n", localStart);
                                    if (localEnd == -1) localEnd = lastAnalyzedPlantProfile.length();
                                    plantName = lastAnalyzedPlantProfile.substring(localStart, localEnd).trim();
                                } else if (lastAnalyzedPlantProfile.contains("• Name: ")) {
                                    int nameStart = lastAnalyzedPlantProfile.indexOf("• Name: ") + "• Name: ".length();
                                    int nameEnd = lastAnalyzedPlantProfile.indexOf("\n", nameStart);
                                    if (nameEnd == -1) nameEnd = lastAnalyzedPlantProfile.length();
                                    plantName = lastAnalyzedPlantProfile.substring(nameStart, nameEnd).trim();
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
                    if (activeChatList == null || activeChatAdapter == null) return;

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

        if (lastAnalyzedPlantProfile == null || lastAnalyzedPlantProfile.trim().isEmpty()) {
            analyzer.askQuestion(question, aiCallback);
        } else {
            analyzer.askFollowUpQuestion(question, lastAnalyzedPlantProfile, aiCallback);
        }
    }

    private void removeLoadingIndicator() {
        if (activeChatList != null && !activeChatList.isEmpty()
                && activeChatList.get(activeChatList.size() - 1).getMessageType() == ChatMessageModel.TYPE_LOADING) {
            int index = activeChatList.size() - 1;
            activeChatList.remove(index);
            if (activeChatAdapter != null) {
                activeChatAdapter.notifyItemRemoved(index);
            }
        }
    }

    private void openRecentsHistoryPanel() {
        BottomSheetDialog historySheet = new BottomSheetDialog(this);
        View historyView = LayoutInflater.from(this).inflate(R.layout.dialog_chat_history_panel, null);
        historySheet.setContentView(historyView);

        RecyclerView rvPastChats = historyView.findViewById(R.id.rv_past_conversations);
        if (rvPastChats != null) {
            rvPastChats.setLayoutManager(new LinearLayoutManager(this));
        }

        historySheet.show();
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
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        MaterialCardView btnClose = dialog.findViewById(R.id.btn_close_notification);
        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}
