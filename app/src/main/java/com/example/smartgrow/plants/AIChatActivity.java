package com.example.smartgrow.plants;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartgrow.R;
import com.example.smartgrow.camera.CameraScannerActivity;
import com.example.smartgrow.camera.ChatAdapter;
import com.example.smartgrow.camera.ChatMessageModel;
import com.example.smartgrow.camera.HistoryBottomSheet;
import com.example.smartgrow.camera.PlantAnalyzer;
import com.example.smartgrow.core.GeminiCacheService;
import com.example.smartgrow.core.SharedPrefManager;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class AIChatActivity extends AppCompatActivity {

    private static final String TAG = "AIChatActivity";
    public static final String EXTRA_IMAGE_PATH = "extra_image_path";
    public static final String EXTRA_IMAGE_BASE64 = "extra_image_base64";
    public static final String EXTRA_PLANT_NAME = "EXTRA_PLANT_NAME";
    public static final String EXTRA_PLANT_ID = "EXTRA_PLANT_ID";

    private RecyclerView rvChatMessagesList;
    private EditText etChatInput;
    private ImageButton ibSendMessage;

    // Welcome layout views
    private LinearLayout layoutCenterWelcome;
    private MaterialButton btnWelcomeWalkthrough, btnWelcomeScan;

    // Preview image views & pending bitmap state
    private View layoutImagePreviewContainer;
    private ImageView ivPreviewSelectedImage;
    private Bitmap pendingImageBitmap = null;

    private ArrayList<ChatMessageModel> chatList;
    private ChatAdapter chatAdapter;

    private String lastAnalyzedPlantProfile = "";
    private String currentNormalizedSpecies = "general";
    private String currentSpeciesName = "General";

    private long lastRequestTime = 0;
    private String currentSessionId = null;

    // Firebase References
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // Activity Result Launchers
    private ActivityResultLauncher<Intent> cameraScannerLauncher;
    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chat);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupLaunchers();
        setupChatFunctionality();

        // Start a new session
        startNewChatSession();

        // Check for incoming data
        if (getIntent() != null) {
            if (getIntent().hasExtra(EXTRA_IMAGE_PATH)) {
                String imagePath = getIntent().getStringExtra(EXTRA_IMAGE_PATH);
                if (imagePath != null && new File(imagePath).exists()) {
                    Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
                    if (bitmap != null) {
                        setPendingImageForChat(getResizedBitmap(bitmap, 1024));
                    }
                }
            }

            if (getIntent().hasExtra(EXTRA_IMAGE_BASE64)) {
                String base64 = getIntent().getStringExtra(EXTRA_IMAGE_BASE64);
                if (base64 != null && !base64.isEmpty()) {
                    Bitmap bitmap = decodeBase64ToBitmap(base64);
                    if (bitmap != null) {
                        setPendingImageForChat(getResizedBitmap(bitmap, 1024));
                    }
                }
            }

            if (getIntent().hasExtra(EXTRA_PLANT_NAME)) {
                currentSpeciesName = getIntent().getStringExtra(EXTRA_PLANT_NAME);
                currentNormalizedSpecies = GeminiCacheService.getInstance().normalizeSpecies(currentSpeciesName);
            }
        }
        
        ImageView ivPlantBot = findViewById(R.id.ivSproutAI);
        int[] botFrames = {
                R.drawable.bot_welcoming,
                R.drawable.bot_smiling,
                R.drawable.bot_mad,
                R.drawable.bot_shy,
                R.drawable.bot_feeling,
                R.drawable.bot_curious
        };

        final int[] currentIndex = {0};
        Handler handler = new Handler(Looper.getMainLooper());

        Runnable animateMascotTask = new Runnable() {
            @Override
            public void run() {
                if (layoutCenterWelcome != null && layoutCenterWelcome.getVisibility() == View.VISIBLE) {
                    currentIndex[0] = (currentIndex[0] + 1) % botFrames.length;
                    if (ivPlantBot != null) ivPlantBot.setImageResource(botFrames[currentIndex[0]]);
                }
                handler.postDelayed(this, 2500);
            }
        };

        handler.postDelayed(animateMascotTask, 2500);
    }

    private void initViews() {
        rvChatMessagesList = findViewById(R.id.rv_chat_messages_list);
        etChatInput = findViewById(R.id.et_chat_input);
        ibSendMessage = findViewById(R.id.ib_send_message);
        ImageView ibChatAttach = findViewById(R.id.ib_chat_attach);
        ImageView ibBackArrow = findViewById(R.id.ib_back_arrow);
        ImageView ibChatMenu = findViewById(R.id.ib_chat_menu);

        // Welcome layout views
        layoutCenterWelcome = findViewById(R.id.layout_center_welcome);
        btnWelcomeWalkthrough = findViewById(R.id.btn_welcome_walkthrough);
        btnWelcomeScan = findViewById(R.id.btn_welcome_scan);

        layoutImagePreviewContainer = findViewById(R.id.layout_image_preview_container);
        ivPreviewSelectedImage = findViewById(R.id.iv_preview_selected_image);
        ImageView ibRemovePreviewImage = findViewById(R.id.ib_remove_preview_image);

        if (ibBackArrow != null) {
            ibBackArrow.setOnClickListener(v -> finish());
        }

        if (ibRemovePreviewImage != null) {
            ibRemovePreviewImage.setOnClickListener(v -> clearPendingImage());
        }

        if (ibChatMenu != null) {
            ibChatMenu.setOnClickListener(this::showChatMenu);
        }

        if (ibChatAttach != null) {
            ibChatAttach.setOnClickListener(this::showAttachMenu);
        }

        // Welcome layout button actions
        if (btnWelcomeWalkthrough != null) {
            btnWelcomeWalkthrough.setOnClickListener(v -> sendChatMessageWithMedia("Walk through into SmartGrow", null));
        }
        if (btnWelcomeScan != null) {
            btnWelcomeScan.setOnClickListener(v -> sendChatMessageWithMedia("How do I scan a plant?", null));
        }

        chatList = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatList);
        rvChatMessagesList.setLayoutManager(new LinearLayoutManager(this));
        rvChatMessagesList.setAdapter(chatAdapter);
    }

    private void setupLaunchers() {
        cameraScannerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        Bitmap capturedBitmap = null;

                        String fullPath = data.getStringExtra(CameraScannerActivity.EXTRA_FULL_IMAGE_PATH);
                        if (fullPath != null && new File(fullPath).exists()) {
                            capturedBitmap = BitmapFactory.decodeFile(fullPath);
                        }

                        if (capturedBitmap == null && data.getData() != null) {
                            Uri imageUri = data.getData();
                            try {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                    capturedBitmap = ImageDecoder.decodeBitmap(
                                            ImageDecoder.createSource(getContentResolver(), imageUri));
                                } else {
                                    try (InputStream is = getContentResolver().openInputStream(imageUri)) {
                                        capturedBitmap = BitmapFactory.decodeStream(is);
                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        }

                        if (capturedBitmap != null) {
                            Bitmap resized = getResizedBitmap(capturedBitmap, 1024);
                            setPendingImageForChat(resized);
                        } else {
                            Toast.makeText(this, "Failed to load captured photo.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        launchCameraScannerForChat();
                    } else {
                        Toast.makeText(this, "Camera permission is required to capture photos.", Toast.LENGTH_SHORT).show();
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
                                Bitmap resized = getResizedBitmap(bitmap, 1024);
                                setPendingImageForChat(resized);
                            }
                        } catch (IOException e) {
                            Toast.makeText(this, "Failed to load image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    private void setupChatFunctionality() {
        if (ibSendMessage != null) {
            ibSendMessage.setOnClickListener(v -> {
                String userText = etChatInput.getText().toString().trim();

                if (userText.isEmpty() && pendingImageBitmap == null) return;

                long currentTime = System.currentTimeMillis();
                if (currentTime - lastRequestTime < 1500) {
                    return;
                }

                lastRequestTime = currentTime;

                Bitmap imageToSend = pendingImageBitmap;
                etChatInput.setText("");
                clearPendingImage();

                sendChatMessageWithMedia(userText, imageToSend);
            });
        }
    }

    private void setPendingImageForChat(Bitmap bitmap) {
        pendingImageBitmap = bitmap;
        if (layoutImagePreviewContainer != null && ivPreviewSelectedImage != null) {
            ivPreviewSelectedImage.setImageBitmap(bitmap);
            layoutImagePreviewContainer.setVisibility(View.VISIBLE);
        }
    }

    private void clearPendingImage() {
        pendingImageBitmap = null;
        if (ivPreviewSelectedImage != null) {
            ivPreviewSelectedImage.setImageBitmap(null);
        }
        if (layoutImagePreviewContainer != null) {
            layoutImagePreviewContainer.setVisibility(View.GONE);
        }
    }

    private void updateUiState() {
        if (chatList.isEmpty()) {
            layoutCenterWelcome.setVisibility(View.VISIBLE);
            rvChatMessagesList.setVisibility(View.GONE);
        } else {
            layoutCenterWelcome.setVisibility(View.GONE);
            rvChatMessagesList.setVisibility(View.VISIBLE);
        }
    }

    private void sendChatMessageWithMedia(String messageText, Bitmap imageBitmap) {
        String lowerMsg = (messageText != null) ? messageText.toLowerCase().trim() : "";
        boolean isAppGuidance = isAppRelatedQuestion(lowerMsg);

        // Validation for AI interaction limit
        if (!isAppGuidance) {
            SharedPrefManager prefManager = SharedPrefManager.getInstance(this);
            if (imageBitmap != null) {
                if (!prefManager.canUseScan()) {
                    long remaining = prefManager.getScanTimeRemaining();
                    showLimitReachedToast(remaining, "scans");
                    return;
                }
            } else {
                if (!prefManager.canUseChat()) {
                    long remaining = prefManager.getChatTimeRemaining();
                    showLimitReachedToast(remaining, "chat prompts");
                    return;
                }
            }
        }

        // Hide welcome layout
        if (layoutCenterWelcome.getVisibility() == View.VISIBLE) {
            layoutCenterWelcome.setVisibility(View.GONE);
            rvChatMessagesList.setVisibility(View.VISIBLE);
        }

        if (currentSessionId == null) {
            String title = (messageText != null && !messageText.isEmpty()) ? messageText : "Scanned Plant Image";
            saveSessionToHistory(title);
        }

        ChatMessageModel userMsg = new ChatMessageModel(messageText, getCurrentPhTime(), ChatMessageModel.TYPE_USER);
        if (imageBitmap != null) {
            userMsg.setImageBitmap(imageBitmap);
        }
        chatList.add(userMsg);
        chatAdapter.notifyItemInserted(chatList.size() - 1);
        rvChatMessagesList.scrollToPosition(chatList.size() - 1);

        appendMessageToFirestore(userMsg);

        // recipe check
        if (isRecipeCheck(messageText)) return;

        if (imageBitmap == null && isAppGuidance) {
            respondToAppQuestion(lowerMsg);
            return;
        }

        GeminiCacheService cacheService = GeminiCacheService.getInstance();
        String normalizedQuestion = cacheService.normalizeQuestion(messageText);
        PlantAnalyzer analyzer = new PlantAnalyzer();

        // SPEED OPTIMIZATION: Instant check before showing loading bubble
        if (imageBitmap == null) {
            // Instant Memory check
            DocumentSnapshot memDoc = cacheService.getChatResponseFromMemory(currentNormalizedSpecies, normalizedQuestion);
            if (memDoc != null && memDoc.exists()) {
                Log.d(TAG, "Chat Session Memory HIT. Instant display.");
                displayAiResponse(new ChatMessageModel(memDoc.getString("answer"), getCurrentPhTime(), ChatMessageModel.TYPE_AI));
                return;
            }

            // Quick Firestore check
            cacheService.getCachedChatResponse(currentNormalizedSpecies, normalizedQuestion).addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                    displayAiResponse(new ChatMessageModel(task.getResult().getString("answer"), getCurrentPhTime(), ChatMessageModel.TYPE_AI));
                } else {
                    // Missed cache, now show loading and go to AI
                    runOnUiThread(() -> {
                        chatList.add(new ChatMessageModel("", "", ChatMessageModel.TYPE_LOADING));
                        chatAdapter.notifyItemInserted(chatList.size() - 1);
                        rvChatMessagesList.scrollToPosition(chatList.size() - 1);
                        callGeminiChat(analyzer, messageText, cacheService, normalizedQuestion);
                    });
                }
            });
            return;
        } else {
            // Vision Track: Check Identification + Profile Caches first
            String hash = cacheService.computeImageHash(imageBitmap);
            cacheService.getCachedIdentification(hash).addOnCompleteListener(idTask -> {
                String speciesFromCache = (idTask.isSuccessful() && idTask.getResult() != null && idTask.getResult().exists()) ? idTask.getResult().getString("speciesName") : cacheService.getIdentificationFromMemory(hash);

                if (speciesFromCache != null) {
                    String norm = cacheService.normalizeSpecies(speciesFromCache);
                    cacheService.getCachedPlant(norm).addOnCompleteListener(plantTask -> {
                        if (plantTask.isSuccessful() && plantTask.getResult() != null && plantTask.getResult().exists()) {
                            currentSpeciesName = speciesFromCache;
                            currentNormalizedSpecies = norm;
                            String raw = plantTask.getResult().getString("rawAnalysisJson");
                            String formatted = analyzer.parseAndFormatPlantJson(raw);
                            runOnUiThread(() -> handleVisionSuccess(formatted, raw));
                        } else {
                            // Partial hit, show loading then finish process
                            runOnUiThread(() -> {
                                chatList.add(new ChatMessageModel("", "", ChatMessageModel.TYPE_LOADING));
                                chatAdapter.notifyItemInserted(chatList.size() - 1);
                                rvChatMessagesList.scrollToPosition(chatList.size() - 1);
                                processSpeciesCacheInChat(imageBitmap, speciesFromCache, messageText, analyzer, cacheService);
                            });
                        }
                    });
                } else {
                    // Full miss, show loading and call Gemini
                    runOnUiThread(() -> {
                        chatList.add(new ChatMessageModel("", "", ChatMessageModel.TYPE_LOADING));
                        chatAdapter.notifyItemInserted(chatList.size() - 1);
                        rvChatMessagesList.scrollToPosition(chatList.size() - 1);
                        callGeminiVision(analyzer, messageText, imageBitmap, cacheService, hash);
                    });
                }
            });
            return;
        }
    }

    private void processSpeciesCacheInChat(Bitmap bitmap, String speciesName, String messageText, PlantAnalyzer analyzer, GeminiCacheService cacheService) {
        currentSpeciesName = speciesName;
        currentNormalizedSpecies = cacheService.normalizeSpecies(speciesName);

        cacheService.getCachedPlant(currentNormalizedSpecies).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                String rawJson = task.getResult().getString("rawAnalysisJson");
                String formatted = analyzer.parseAndFormatPlantJson(rawJson);
                runOnUiThread(() -> {
                    removeLoadingIndicator();
                    handleVisionSuccess(formatted, rawJson);
                });
            } else {
                callGeminiVision(analyzer, messageText, bitmap, cacheService, null);
            }
        });
    }

    private void callGeminiVision(PlantAnalyzer analyzer, String messageText, Bitmap imageBitmap, GeminiCacheService cacheService, String imageHash) {
        PlantAnalyzer.PlantAnalysisCallback detailedCallback = new PlantAnalyzer.PlantAnalysisCallback() {
            @Override
            public void onSuccess(String formattedResult, String rawJson) {
                runOnUiThread(() -> {
                    removeLoadingIndicator();

                    if (rawJson != null && (rawJson.contains("\"is_plant\": false") || rawJson.contains("\"is_plant\":false"))) {
                        displayAiResponse(new ChatMessageModel("We couldn't detect a plant in the provided image.", getCurrentPhTime(), ChatMessageModel.TYPE_AI));
                        return;
                    }

                    String cName = analyzer.extractCommonName(rawJson);
                    if (cName != null) {
                        currentSpeciesName = cName;
                        currentNormalizedSpecies = cacheService.normalizeSpecies(cName);
                        cacheService.savePlantCache(currentNormalizedSpecies, cName, rawJson, analyzer.parseAnalysisToMap(null, rawJson));
                        if (imageHash != null) cacheService.saveIdentificationCache(imageHash, cName);
                    }

                    handleVisionSuccess(formattedResult, rawJson);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    removeLoadingIndicator();
                    displayAiResponse(new ChatMessageModel("I encountered an issue. Please try again.", getCurrentPhTime(), ChatMessageModel.TYPE_AI));
                });
            }
        };

        if (messageText != null && !messageText.trim().isEmpty()) {
            analyzer.analyzePlantWithQuestion(messageText, imageBitmap, detailedCallback);
        } else {
            analyzer.analyzePlantDetailed(imageBitmap, detailedCallback);
        }
    }

    private void callGeminiChat(PlantAnalyzer analyzer, String messageText, GeminiCacheService cacheService, String normalizedQuestion) {
        PlantAnalyzer.PlantCallback aiCallback = new PlantAnalyzer.PlantCallback() {
            @Override
            public void onSuccess(String rawAiReply) {
                runOnUiThread(() -> {
                    removeLoadingIndicator();
                    String cleanedReply = cleanAiResponseText(rawAiReply);

                    // Save to cache
                    cacheService.saveChatResponse(currentNormalizedSpecies, currentSpeciesName, messageText, normalizedQuestion, cleanedReply);

                    ChatMessageModel msg = new ChatMessageModel(cleanedReply, getCurrentPhTime(), ChatMessageModel.TYPE_AI);
                    msg.setFollowUpSuggestions(new ArrayList<>(List.of("Give me care tips for a Monstera.", "Tell me about ideal humidity.")));
                    displayAiResponse(msg);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    removeLoadingIndicator();
                    displayAiResponse(new ChatMessageModel("I encountered an issue. Please try again.", getCurrentPhTime(), ChatMessageModel.TYPE_AI));
                });
            }
        };

        String context = lastAnalyzedPlantProfile;
        String history = buildChatHistoryContext();
        if (!history.isEmpty()) context += "\n\nContext:\n" + history;

        if (context.trim().isEmpty()) analyzer.askQuestion(messageText, aiCallback);
        else analyzer.askFollowUpQuestion(messageText, context, aiCallback);
    }

    private void respondToAppQuestion(String msg) {
        String response;
        ArrayList<String> suggestions = new ArrayList<>();
        String m = msg.toLowerCase().trim();

        if (m.contains("scan") || m.contains("camera")) {
            response = "🌿 Plant Identification & Health Scanner\n\n" +
                    "To identify a plant, tap the Scan icon or use the Attach button in chat. Capture or upload a photo to:\n" +
                    "• Identify 10,000+ plant species instantly.\n" +
                    "• Detect diseases and pests using AI vision.\n" +
                    "• Get specific care recommendations based on the scan.";
            suggestions.add("How to set reminders?");
            suggestions.add("What is My Garden?");
        } else if (m.contains("garden") || m.contains("my garden")) {
            response = "🏡 My Garden & Digital Repository\n\n" +
                    "The 'My Garden' section stores all your scanned plants. Here you can:\n" +
                    "• Track growth history for each plant.\n" +
                    "• Access scientific names, common aliases, and distribution maps.\n" +
                    "• View detailed care guides saved from previous scans.";
            suggestions.add("How to scan a plant?");
            suggestions.add("Tell me about reminders.");
        } else if (m.contains("reminder") || m.contains("reminders")) {
            response = "⏰ Care Reminders & Notifications\n\n" +
                    "Never forget to care for your plants! Set schedules for:\n" +
                    "• Watering, Fertilizing, and Repotting.\n" +
                    "• Custom notification times to suit your routine.\n" +
                    "• Smart reminders automatically suggested for diseased plants.";
            suggestions.add("How the app works?");
            suggestions.add("What is the Forum?");
        } else if (m.contains("forum") || m.contains("community")) {
            response = "🤝 Global Community Forum\n\n" +
                    "Connect with other plant enthusiasts! Use the Forum to:\n" +
                    "• Share tips and success stories.\n" +
                    "• Post photos of your journey.\n" +
                    "• Ask questions to experienced gardeners.";
            suggestions.add("Walk through into SmartGrow");
        } else if (m.contains("walk through") || m.contains("app works") || m.contains("features")) {
            response = "🌟 SmartGrow App Features Walkthrough\n\n" +
                    "1. Scan/Camera: Identifies species and diagnoses health from photos.\n\n" +
                    "2. AI Chatbot: Your botanical assistant (SproutAI) for any advice.\n\n" +
                    "3. Home Dashboard: Shows weather, garden summary, and quick actions.\n\n" +
                    "4. My Garden: Track your plants' progress and health profile.\n\n" +
                    "5. Reminders: Personalized schedules for watering and maintenance.\n\n" +
                    "6. Community Forum: Connect with other gardeners and share tips.\n\n" +
                    "7. History: Access all your past scans and chat sessions anytime.\n\n" +
                    "What would you like to explore first?";
            suggestions.add("Tell more about Scanning.");
            suggestions.add("What is My Garden?");
        } else {
            response = "Welcome to SmartGrow! I am SproutAI, your botanical assistant.\n\n" +
                    "I can help you with plant identification, disease diagnosis, and care advice. I also guide you through the SmartGrow app features.\n\n" +
                    "What would you like to know about?";
            suggestions.add("Walk through into SmartGrow");
            suggestions.add("How to scan a plant?");
        }

        ChatMessageModel aiMsg = new ChatMessageModel(response, getCurrentPhTime(), ChatMessageModel.TYPE_AI);
        aiMsg.setFollowUpSuggestions(suggestions);
        displayAiResponse(aiMsg);
    }

    private boolean isRecipeCheck(String msg) {
        if (msg != null && (msg.toLowerCase().contains("recipe") || msg.toLowerCase().contains("cook"))) {
            displayAiResponse(new ChatMessageModel("I am sorry, but I can only assist with plant care, identification, and SmartGrow app features. I cannot provide food recipes or cooking instructions.", getCurrentPhTime(), ChatMessageModel.TYPE_AI));
            return true;
        }
        return false;
    }

    private void handleVisionSuccess(String formattedResult, String rawJson) {
        lastAnalyzedPlantProfile = formattedResult;
        ChatMessageModel aiMsg = new ChatMessageModel(formattedResult, getCurrentPhTime(), ChatMessageModel.TYPE_AI);
        aiMsg.setFollowUpSuggestions(new ArrayList<>(PlantAnalyzer.extractPlantSuggestionsFromRawJson(rawJson)));
        displayAiResponse(aiMsg);
    }

    private void displayAiResponse(ChatMessageModel aiMsg) {
        chatList.add(aiMsg);
        chatAdapter.notifyItemInserted(chatList.size() - 1);
        rvChatMessagesList.scrollToPosition(chatList.size() - 1);
        appendMessageToFirestore(aiMsg);
    }

    private void showLimitReachedToast(long millis, String type) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        String timeText = (hours > 0 ? hours + "h " : "") + minutes + "m";
        Toast.makeText(this, "Daily limit reached for " + type + ". Resets in " + timeText + ".", Toast.LENGTH_LONG).show();
    }

    private boolean isAppRelatedQuestion(String msg) {
        if (msg == null) return false;
        String m = msg.toLowerCase();
        return m.contains("how the app works") || m.contains("walk through") || m.contains("app details") || m.contains("features") || m.contains("scan") || m.contains("garden") || m.contains("reminder");
    }

    private void removeLoadingIndicator() {
        if (chatList != null && !chatList.isEmpty() && chatList.get(chatList.size() - 1).getMessageType() == ChatMessageModel.TYPE_LOADING) {
            int index = chatList.size() - 1;
            chatList.remove(index);
            chatAdapter.notifyItemRemoved(index);
        }
    }

    private void showChatMenu(View v) {
        View dropdownView = LayoutInflater.from(this).inflate(R.layout.layout_custom_dropdown, null);
        PopupWindow menu = new PopupWindow(dropdownView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        menu.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dropdownView.findViewById(R.id.item_dropdown_new_chat).setOnClickListener(view -> {
            menu.dismiss();
            startNewChatSession();
        });
        dropdownView.findViewById(R.id.item_dropdown_history).setOnClickListener(view -> {
            menu.dismiss();
            showHistoryBottomSheetDialog();
        });
        menu.showAsDropDown(v, -80, 10);
    }

    private void showAttachMenu(View v) {
        View attachView = LayoutInflater.from(this).inflate(R.layout.layout_attach_dropdown, null);
        PopupWindow attachMenu = new PopupWindow(attachView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        attachMenu.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        attachView.findViewById(R.id.item_dropdown_take_photo).setOnClickListener(view -> {
            attachMenu.dismiss();
            launchCameraScannerForChat();
        });
        attachView.findViewById(R.id.item_dropdown_upload_gallery).setOnClickListener(view -> {
            attachMenu.dismiss();
            galleryLauncher.launch("image/*");
        });
        attachView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        attachMenu.showAsDropDown(v, 10, -(attachView.getMeasuredHeight() + v.getHeight() + 15));
    }

    private void launchCameraScannerForChat() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(this, CameraScannerActivity.class);
            intent.putExtra(CameraScannerActivity.EXTRA_MODE, CameraScannerActivity.MODE_CHAT_ATTACHMENT);
            cameraScannerLauncher.launch(intent);
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void startNewChatSession() {
        currentSessionId = String.valueOf(System.currentTimeMillis());
        if (chatList != null) chatList.clear();
        lastAnalyzedPlantProfile = "";
        currentNormalizedSpecies = "general";
        currentSpeciesName = "General";
        clearPendingImage();

        if (layoutCenterWelcome != null) {
            layoutCenterWelcome.setVisibility(View.VISIBLE);
        }
        if (rvChatMessagesList != null) {
            rvChatMessagesList.setVisibility(View.GONE);
        }

        if (chatAdapter != null) chatAdapter.notifyDataSetChanged();
    }

    private void showHistoryBottomSheetDialog() {
        HistoryBottomSheet historySheet = HistoryBottomSheet.newInstance();
        historySheet.setOnSessionSelectedListener(session -> {
            if (session != null) {
                currentSessionId = session.getSessionId();
                loadUserChatHistoryFromFirestore();
            }
        });
        historySheet.show(getSupportFragmentManager(), "HistoryBottomSheet");
    }

    private void loadUserChatHistoryFromFirestore() {
        String userId = getCurrentUserId();
        if (userId == null || currentSessionId == null) return;
        db.collection("ai_chat_messages").document(currentSessionId).get().addOnSuccessListener(documentSnapshot -> {
            chatList.clear();
            if (documentSnapshot.exists()) {
                List<Map<String, Object>> msgArray = (List<Map<String, Object>>) documentSnapshot.get("messages");
                if (msgArray != null) {
                    for (Map<String, Object> doc : msgArray) {
                        ChatMessageModel msg = new ChatMessageModel((String) doc.get("messageText"), (String) doc.get("messageTime"), ((Long) doc.get("messageType")).intValue());
                        String base64 = (String) doc.get("imageBase64");
                        if (base64 != null) {
                            msg.setImageBase64(base64);
                            msg.setImageBitmap(decodeBase64ToBitmap(base64));
                        }
                        List<String> suggestions = (List<String>) doc.get("followUpSuggestions");
                        if (suggestions != null) msg.setFollowUpSuggestions(new ArrayList<>(suggestions));
                        chatList.add(msg);
                    }
                }
            }
            updateUiState();
            chatAdapter.notifyDataSetChanged();
            rvChatMessagesList.scrollToPosition(chatList.size() - 1);
        });
    }

    private String getCurrentUserId() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        return currentUser != null ? currentUser.getUid() : null;
    }

    private String getCurrentPhTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Manila"));
        return sdf.format(new Date());
    }

    private String cleanAiResponseText(String input) {
        if (input == null || input.isEmpty()) return "";
        String cleaned = input.replace("*", "").replaceAll("[#()\\[\\]_~`]", "").trim();
        return cleaned.replaceAll("\n{2,}", "\n\n");
    }

    private void saveSessionToHistory(String firstQuestion) {
        String userId = getCurrentUserId();
        if (userId == null || firstQuestion == null) return;
        String title = (firstQuestion.length() > 35) ? firstQuestion.substring(0, 32) + "..." : firstQuestion;
        Map<String, Object> sessionMap = new HashMap<>();
        sessionMap.put("uid", userId);
        sessionMap.put("sessionId", currentSessionId);
        sessionMap.put("title", title);
        sessionMap.put("timestamp", System.currentTimeMillis());
        db.collection("ai_chat_sessions").document(currentSessionId).set(sessionMap, SetOptions.merge());
    }

    private void appendMessageToFirestore(ChatMessageModel message) {
        String userId = getCurrentUserId();
        if (userId == null || currentSessionId == null) return;
        Map<String, Object> newMsgMap = new HashMap<>();
        newMsgMap.put("messageText", message.getMessageText());
        newMsgMap.put("messageTime", message.getMessageTime());
        newMsgMap.put("messageType", message.getMessageType());
        newMsgMap.put("timestamp", System.currentTimeMillis());
        String imgBase64 = message.getImageBitmap() != null ? encodeBitmapToBase64(message.getImageBitmap()) : message.getImageBase64();
        if (imgBase64 != null) newMsgMap.put("imageBase64", imgBase64);
        if (message.getFollowUpSuggestions() != null) newMsgMap.put("followUpSuggestions", message.getFollowUpSuggestions());

        db.collection("ai_chat_messages").document(currentSessionId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                db.collection("ai_chat_messages").document(currentSessionId).update("messages", FieldValue.arrayUnion(newMsgMap), "lastUpdated", System.currentTimeMillis());
            } else {
                Map<String, Object> sessionDoc = new HashMap<>();
                sessionDoc.put("uid", userId);
                sessionDoc.put("sessionId", currentSessionId);
                sessionDoc.put("messages", new ArrayList<>(List.of(newMsgMap)));
                sessionDoc.put("lastUpdated", System.currentTimeMillis());
                db.collection("ai_chat_messages").document(currentSessionId).set(sessionDoc);
            }
        });
    }

    private String encodeBitmapToBase64(Bitmap bitmap) {
        if (bitmap == null) return null;
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, os);
        return Base64.encodeToString(os.toByteArray(), Base64.DEFAULT);
    }

    private Bitmap decodeBase64ToBitmap(String base64Str) {
        if (base64Str == null || base64Str.isEmpty()) return null;
        try {
            byte[] decodedBytes = Base64.decode(base64Str, base64Str.startsWith("data:") ? base64Str.indexOf(",") + 1 : 0);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            return null;
        }
    }

    private Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth(), height = image.getHeight();
        float ratio = (float) width / height;
        if (ratio > 1) { width = maxSize; height = (int)(width/ratio); }
        else { height = maxSize; width = (int)(height*ratio); }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }

    private String buildChatHistoryContext() {
        if (chatList == null || chatList.isEmpty()) return "";
        StringBuilder historyBuilder = new StringBuilder();
        int startIndex = Math.max(0, chatList.size() - 10);
        for (int i = startIndex; i < chatList.size(); i++) {
            ChatMessageModel msg = chatList.get(i);
            if (msg.getMessageType() == ChatMessageModel.TYPE_USER) historyBuilder.append("User: ").append(msg.getMessageText()).append("\n");
            else if (msg.getMessageType() == ChatMessageModel.TYPE_AI) historyBuilder.append("Assistant: ").append(msg.getMessageText()).append("\n");
        }
        return historyBuilder.toString().trim();
    }

    public void submitFollowUpQuestion(String question) {
        sendChatMessageWithMedia(question, null);
    }
}
