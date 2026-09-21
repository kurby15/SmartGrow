package com.example.smartgrow;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
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
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
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
import com.example.smartgrow.camera.HistoryBottomSheet;
import com.example.smartgrow.camera.PlantAnalyzer;
import com.example.smartgrow.community.ArchiveFragment;
import com.example.smartgrow.community.CommunityForumFragment;
import com.example.smartgrow.plants.AIChatActivity;
import com.example.smartgrow.plants.HomeFragment;
import com.example.smartgrow.plants.MyGardenFragment;
import com.example.smartgrow.profile.ProfileFragment;
import com.example.smartgrow.plants.SetReminderFragment;
import com.example.smartgrow.plants.PlantReminderBottomSheet;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

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

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private MaterialCardView cardNavChatAssistant;
    private MaterialCardView cardActionNotification, cardActionGlobal, cardActionProfile;
    private View mainHeaderBar;

    private BottomNavigationView bottomNav;

    // Loading Indicator for Plant Analysis
    private ProgressDialog loadingDialog;

    // Persisted Chat Memory and Dialog State
    private BottomSheetDialog activeChatDialog;
    private ArrayList<ChatMessageModel> activeChatList;
    private ChatAdapter activeChatAdapter;
    private RecyclerView activeRvChatMessages;

    // Preview image views & pending bitmap state
    private Bitmap pendingImageBitmap = null;
    private View layoutImagePreviewContainer;
    private ImageView ivPreviewSelectedImage;
    private ImageView ibRemovePreviewImage;

    private String lastAnalyzedPlantProfile = "";
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
        setContentView(R.layout.activity_main);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize Loading Dialog
        loadingDialog = new ProgressDialog(this);
        loadingDialog.setMessage("Analyzing plant...\nPlease wait.");
        loadingDialog.setCancelable(false);

        setupLaunchers();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        bottomNav = findViewById(R.id.bottom_navigation_bar);
        mainHeaderBar = findViewById(R.id.layout_top_header);
        cardActionGlobal = findViewById(R.id.card_action_global);
        cardActionProfile = findViewById(R.id.card_action_profile);

        if (savedInstanceState == null) {
            if (getIntent() != null && getIntent().hasExtra("plantId")) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new SetReminderFragment())
                        .commit();
                String pId = getIntent().getStringExtra("plantId");
                if (pId != null && !pId.isEmpty()) {
                    PlantReminderBottomSheet bottomSheet = PlantReminderBottomSheet.newInstance(pId);
                    bottomSheet.show(getSupportFragmentManager(), "PlantReminderBottomSheet");
                }
            } else {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new HomeFragment())
                        .commit();
            }
        }

        if (bottomNav != null) {
            bottomNav.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();

                if (itemId == R.id.nav_home) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, new HomeFragment())
                            .commit();
                    return true;
                } else if (itemId == R.id.nav_diary) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, new MyGardenFragment())
                            .commit();
                    return true;
                } else if (itemId == R.id.nav_scan) {
                    Intent intent = new Intent(MainActivity.this, CameraScannerActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    startActivity(intent);
                    return true;
                } else if (itemId == R.id.nav_chat) {
                    Intent intent = new Intent(MainActivity.this, AIChatActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    startActivity(intent);
                    return true;
                }

                return false;
            });
        }

        setupGlobalHeaderListeners();

        if (cardNavChatAssistant != null) {
            cardNavChatAssistant.setOnClickListener(this::showAssistantPopupMenu);
        }

        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            checkCurrentFragmentForVisibility();
        });
    }

    private void checkCurrentFragmentForVisibility() {
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);

        if (current == null
                || current instanceof ArchiveFragment
                || current instanceof com.example.smartgrow.settings.AppPreferencesFragment
                || current instanceof com.example.smartgrow.settings.AccountSecurityFragment
                || current instanceof com.example.smartgrow.settings.SupportInfoFragment
                || current instanceof com.example.smartgrow.profile.EditProfileFragment
                || current instanceof com.example.smartgrow.settings.PrivacyDataFragment
                || current instanceof com.example.smartgrow.settings.ChangePasswordFragment
                || current instanceof com.example.smartgrow.settings.HelpCenterFaqFragment
                || current instanceof com.example.smartgrow.settings.ContactUsFragment
                || current instanceof com.example.smartgrow.settings.PrivacyPolicyFragment
                || current instanceof com.example.smartgrow.settings.TermOfServiceFragment
                || current instanceof com.example.smartgrow.settings.AboutSmartGrowFragment
                || current instanceof com.example.smartgrow.plants.AllPlantsFragment
                || current instanceof com.example.smartgrow.plants.SetReminderFragment) {

            if (bottomNav != null) bottomNav.setVisibility(View.GONE);
            if (mainHeaderBar != null) mainHeaderBar.setVisibility(View.GONE);
            if (cardNavChatAssistant != null) cardNavChatAssistant.setVisibility(View.GONE);
        } else {
            if (bottomNav != null) bottomNav.setVisibility(View.VISIBLE);
            if (mainHeaderBar != null) mainHeaderBar.setVisibility(View.VISIBLE);
            if (cardNavChatAssistant != null) cardNavChatAssistant.setVisibility(View.VISIBLE);
        }
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
                            } catch (Exception ignored) {}
                        }

                        if (capturedBitmap == null) {
                            String imagePath = data.getStringExtra(CameraScannerActivity.EXTRA_IMAGE_PATH);
                            if (imagePath != null) {
                                File internalFile = new File(getFilesDir(), imagePath);
                                if (internalFile.exists()) {
                                    capturedBitmap = BitmapFactory.decodeFile(internalFile.getAbsolutePath());
                                }
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

    private void setPendingImageForChat(Bitmap bitmap) {
        showAiChatAssistantBottomSheet();
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

    private String buildChatHistoryContext() {
        if (activeChatList == null || activeChatList.isEmpty()) {
            return "";
        }

        StringBuilder historyBuilder = new StringBuilder();
        int startIndex = Math.max(0, activeChatList.size() - 10);

        for (int i = startIndex; i < activeChatList.size(); i++) {
            ChatMessageModel msg = activeChatList.get(i);
            if (msg.getMessageType() == ChatMessageModel.TYPE_USER) {
                if (msg.getMessageText() != null && !msg.getMessageText().trim().isEmpty()) {
                    historyBuilder.append("User: ").append(msg.getMessageText()).append("\n");
                }
            } else if (msg.getMessageType() == ChatMessageModel.TYPE_AI) {
                if (msg.getMessageText() != null && !msg.getMessageText().trim().isEmpty()) {
                    historyBuilder.append("Assistant: ").append(msg.getMessageText()).append("\n");
                }
            }
        }
        return historyBuilder.toString().trim();
    }

    private void sendChatMessageWithMedia(String messageText, Bitmap imageBitmap) {
        if (currentSessionId == null) {
            String title = (messageText != null && !messageText.isEmpty()) ? messageText : "Scanned Plant Image";
            saveSessionToHistory(title);
        }

        ChatMessageModel userMsg = new ChatMessageModel(messageText, getCurrentPhTime(), ChatMessageModel.TYPE_USER);
        if (imageBitmap != null) {
            userMsg.setImageBitmap(imageBitmap);
        }
        activeChatList.add(userMsg);

        if (activeChatAdapter != null) {
            activeChatAdapter.notifyItemInserted(activeChatList.size() - 1);
        }

        appendMessageToFirestore(userMsg);

        String lowerMsg = (messageText != null) ? messageText.toLowerCase().trim() : "";
        if (imageBitmap == null && isAppRelatedQuestion(lowerMsg)) {
            respondToAppQuestion(lowerMsg);
            return;
        }

        activeChatList.add(new ChatMessageModel("", "", ChatMessageModel.TYPE_LOADING));
        if (activeChatAdapter != null) {
            activeChatAdapter.notifyItemInserted(activeChatList.size() - 1);
        }

        if (activeRvChatMessages != null) {
            activeRvChatMessages.scrollToPosition(activeChatList.size() - 1);
        }

        PlantAnalyzer analyzer = new PlantAnalyzer();

        if (imageBitmap != null) {
            PlantAnalyzer.PlantAnalysisCallback detailedCallback = new PlantAnalyzer.PlantAnalysisCallback() {
                @Override
                public void onSuccess(String formattedResult, String rawJson) {
                    runOnUiThread(() -> {
                        removeLoadingIndicator();

                        if (rawJson != null && (rawJson.contains("\"is_plant\": false")
                                || rawJson.contains("\"is_plant\":false")
                                || rawJson.contains("plant_not_detected"))) {

                            ChatMessageModel notPlantMsg = new ChatMessageModel(
                                    "We couldn't detect a plant in the provided image. Please make sure the photo is clear, well-lit, and focused on a plant before trying again.",
                                    getCurrentPhTime(),
                                    ChatMessageModel.TYPE_AI);

                            activeChatList.add(notPlantMsg);
                            if (activeChatAdapter != null) {
                                activeChatAdapter.notifyItemInserted(activeChatList.size() - 1);
                            }
                            if (activeRvChatMessages != null) {
                                activeRvChatMessages.scrollToPosition(activeChatList.size() - 1);
                            }

                            appendMessageToFirestore(notPlantMsg);
                            return;
                        }

                        lastAnalyzedPlantProfile = formattedResult;
                        String cleanedResult = cleanAiResponseText(formattedResult);

                        ArrayList<String> suggestions = new ArrayList<>();
                        suggestions.add("What are its watering needs?");
                        suggestions.add("How much sunlight does it need?");
                        suggestions.add("Common diseases and treatments?");
                        suggestions.add("Walk through into SmartGrow");

                        ChatMessageModel aiMsg = new ChatMessageModel(cleanedResult, getCurrentPhTime(), ChatMessageModel.TYPE_AI);
                        aiMsg.setFollowUpSuggestions(suggestions);

                        activeChatList.add(aiMsg);
                        if (activeChatAdapter != null) {
                            activeChatAdapter.notifyItemInserted(activeChatList.size() - 1);
                        }

                        if (activeRvChatMessages != null) {
                            activeRvChatMessages.scrollToPosition(activeChatList.size() - 1);
                        }

                        appendMessageToFirestore(aiMsg);
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        removeLoadingIndicator();
                        Log.e(TAG, "Plant analysis error: " + error);

                        String errorMessageText = "I encountered an issue processing your request. Please check your connection or ask 'Walk through into SmartGrow' for guidance.";
                        if (error != null && (error.toLowerCase().contains("plant") || error.toLowerCase().contains("not_detected"))) {
                            errorMessageText = "We couldn't detect a plant in the provided image. Please make sure the photo is clear and focused on the plant.";
                        }

                        ChatMessageModel errorMsg = new ChatMessageModel(
                                errorMessageText,
                                getCurrentPhTime(),
                                ChatMessageModel.TYPE_AI);
                        
                        ArrayList<String> suggestions = new ArrayList<>();
                        suggestions.add("Walk through into SmartGrow");
                        errorMsg.setFollowUpSuggestions(suggestions);

                        activeChatList.add(errorMsg);
                        if (activeChatAdapter != null) {
                            activeChatAdapter.notifyItemInserted(activeChatList.size() - 1);
                        }

                        if (activeRvChatMessages != null) {
                            activeRvChatMessages.scrollToPosition(activeChatList.size() - 1);
                        }

                        appendMessageToFirestore(errorMsg);
                    });
                }
            };

            if (messageText != null && !messageText.trim().isEmpty()) {
                analyzer.analyzePlantWithQuestion(messageText, imageBitmap, detailedCallback);
            } else {
                analyzer.analyzePlantDetailed(imageBitmap, detailedCallback);
            }
        } else {
            PlantAnalyzer.PlantCallback aiCallback = new PlantAnalyzer.PlantCallback() {
                @Override
                public void onSuccess(String rawAiReply) {
                    runOnUiThread(() -> {
                        removeLoadingIndicator();
                        if (activeChatList == null || activeChatAdapter == null) return;

                        String cleanedReply = cleanAiResponseText(rawAiReply);
                        ArrayList<String> suggestions = new ArrayList<>();

                        if (cleanedReply.contains("only answer questions related to plants") || cleanedReply.toLowerCase().contains("unrelated")) {
                            cleanedReply = "I can only assist with plant-related topics and care advice. Please ask a question about plant care, identification, or gardening tips!";
                            suggestions.add("Give me care tips for a Monstera.");
                            suggestions.add("How often should I water succulents?");
                            suggestions.add("Walk through into SmartGrow");
                        } else if (!cleanedReply.contains("does not appear to contain a plant") && !cleanedReply.startsWith("Please upload")) {
                            String plantName = "it";
                            try {
                                if (lastAnalyzedPlantProfile != null && !lastAnalyzedPlantProfile.isEmpty()) {
                                    if (lastAnalyzedPlantProfile.contains("Local Name:")) {
                                        int localStart = lastAnalyzedPlantProfile.indexOf("Local Name:") + "Local Name:".length();
                                        int localEnd = lastAnalyzedPlantProfile.indexOf("\n", localStart);
                                        if (localEnd == -1) localEnd = lastAnalyzedPlantProfile.length();
                                        plantName = cleanAiResponseText(lastAnalyzedPlantProfile.substring(localStart, localEnd));
                                    } else if (lastAnalyzedPlantProfile.contains("Name:")) {
                                        int nameStart = lastAnalyzedPlantProfile.indexOf("Name:") + "Name:".length();
                                        int nameEnd = lastAnalyzedPlantProfile.indexOf("\n", nameStart);
                                        if (nameEnd == -1) nameEnd = lastAnalyzedPlantProfile.length();
                                        plantName = cleanAiResponseText(lastAnalyzedPlantProfile.substring(nameStart, nameEnd));
                                    }
                                }
                            } catch (Exception e) {
                                plantName = "it";
                            }

                            String lowerReply = cleanedReply.toLowerCase();
                            String lowerQuestion = messageText.toLowerCase();

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
                            suggestions.add("Walk through into SmartGrow");
                        }

                        ChatMessageModel msg = new ChatMessageModel(cleanedReply, getCurrentPhTime(), ChatMessageModel.TYPE_AI);
                        msg.setFollowUpSuggestions(suggestions);

                        activeChatList.add(msg);
                        activeChatAdapter.notifyItemInserted(activeChatList.size() - 1);

                        if (activeRvChatMessages != null) {
                            activeRvChatMessages.scrollToPosition(activeChatList.size() - 1);
                        }

                        appendMessageToFirestore(msg);
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        removeLoadingIndicator();
                        if (activeChatList == null || activeChatAdapter == null) return;
                        Log.e(TAG, "Chat AI error: " + error);

                        ChatMessageModel errorMsg = new ChatMessageModel(
                                "I encountered an issue processing your request. Please try again or ask 'Walk through into SmartGrow' for app details.",
                                getCurrentPhTime(),
                                ChatMessageModel.TYPE_AI);

                        ArrayList<String> suggestions = new ArrayList<>();
                        suggestions.add("Walk through into SmartGrow");
                        suggestions.add("How do I scan a plant?");
                        errorMsg.setFollowUpSuggestions(suggestions);

                        activeChatList.add(errorMsg);
                        activeChatAdapter.notifyItemInserted(activeChatList.size() - 1);

                        if (activeRvChatMessages != null) {
                            activeRvChatMessages.scrollToPosition(activeChatList.size() - 1);
                        }

                        appendMessageToFirestore(errorMsg);
                    });
                }
            };

            String chatHistoryContext = buildChatHistoryContext();
            String fullContext = lastAnalyzedPlantProfile;

            if (!chatHistoryContext.isEmpty()) {
                fullContext = (fullContext.isEmpty() ? "" : fullContext + "\n\n") + "Previous Conversation Context:\n" + chatHistoryContext;
            }

            if (fullContext.trim().isEmpty()) {
                analyzer.askQuestion(messageText, aiCallback);
            } else {
                analyzer.askFollowUpQuestion(messageText, fullContext, aiCallback);
            }
        }
    }

    private boolean isAppRelatedQuestion(String msg) {
        if (msg == null) return false;
        String m = msg.toLowerCase().trim();
        return m.contains("how the app works") ||
                m.contains("walk through") ||
                m.contains("app details") || 
                m.contains("what can this app do") ||
                m.contains("about smartgrow") ||
                m.contains("features") ||
                m.startsWith("how to scan") ||
                m.startsWith("how do i scan") ||
                m.equals("scan") ||
                m.equals("scanning") ||
                m.contains("my garden") ||
                m.contains("plant diary") ||
                m.equals("garden") ||
                m.contains("reminder") ||
                m.contains("notification") ||
                m.contains("forum") ||
                m.contains("community") ||
                m.contains("history") ||
                m.contains("past chat") ||
                m.contains("session") ||
                m.contains("chatbot") ||
                m.contains("assistant") ||
                m.contains("who are you") ||
                m.contains("weather") ||
                m.contains("dashboard") ||
                m.contains("profile") ||
                m.contains("account") ||
                m.contains("settings");
    }

    private void respondToAppQuestion(String msg) {
        String response;
        ArrayList<String> suggestions = new ArrayList<>();
        String m = msg.toLowerCase().trim();

        if (m.contains("scan") || m.contains("identify") || m.contains("health")) {
            response = "Plant Identification & Health Scanner\n\n" +
                    "To identify a plant, tap the '+' button or the Scan icon.\n\n" +
                    "Capture a new photo or select one from your gallery.\n\n" +
                    "SmartGrow uses AI to:\n\n" +
                    "  Identify 10,000+ species.\n\n" +
                    "  Detect diseases, pests, and nutrient issues.\n\n" +
                    "  Provide immediate care recommendations.";
            suggestions.add("How to set reminders?");
            suggestions.add("Tell me about My Garden.");
        } else if (m.contains("garden") || m.contains("diary")) {
            response = "My Garden (Plant Diary)\n\n" +
                    "The 'My Garden' section is your digital greenhouse.\n\n" +
                    "When you scan a plant, save it here to:\n\n" +
                    "  Track growth history.\n\n" +
                    "  Store health reports.\n\n" +
                    "  Access specific care guides for your collection.";
            suggestions.add("How to scan a plant?");
            suggestions.add("What are Reminders?");
        } else if (m.contains("reminder") || m.contains("notification")) {
            response = "Care Reminders\n\n" +
                    "Stay on top of your plant care with smart alerts!\n\n" +
                    "You can set schedules for Watering, Fertilizing, and Repotting.\n\n" +
                    "Access this via the 'Set Reminder' tab or a plant's profile in your Garden.";
            suggestions.add("How do I scan a plant?");
            suggestions.add("Tell me about the Community.");
        } else if (m.contains("forum") || m.contains("community") || m.contains("global")) {
            response = "Community Forum\n\n" +
                    "Connect with a global network of plant lovers!\n\n" +
                    "Share photos, ask for advice, and learn new gardening tips.\n\n" +
                    "Tap the 'Global' card in the header to join.";
            suggestions.add("How do I scan a plant?");
            suggestions.add("What is the AI Chatbot?");
        } else if (m.contains("history") || m.contains("past chat") || m.contains("session")) {
            response = "Conversation & Scan History\n\n" +
                    "SmartGrow saves your data.\n\n" +
                    "You can revisit every AI conversation and plant analysis result.\n\n" +
                    "Tap the 'Menu' icon (top right) in this chat and select 'History'.";
            suggestions.add("Walk through into SmartGrow");
            suggestions.add("How do I scan a plant?");
        } else if (m.contains("chatbot") || m.contains("assistant") || m.contains("who are you")) {
            response = "SmartGrow AI Assistant\n\n" +
                    "I'm your 24/7 botanical expert!\n\n" +
                    "I can help you identify plants, treat diseases, and navigate the app.\n\n" +
                    "Ask me anything about gardening or app features.";
            suggestions.add("Give me care tips for a Monstera.");
            suggestions.add("Walk through into SmartGrow");
        } else {
            response = "Welcome to SmartGrow!\n\n" +
                    "Here is a full walk-through of what I can do:\n\n" +
                    "  Scan & Diagnose: Identify plants and health issues via camera.\n\n" +
                    "  My Garden: Save and organize your personal plant collection.\n\n" +
                    "  Care Reminders: Set custom alerts for watering and more.\n\n" +
                    "  Community Forum: Chat with other growers globally.\n\n" +
                    "  AI Assistant: Get expert care advice 24/7.\n\n" +
                    "  History: Revisit all your past scans and chats anytime.\n\n" +
                    "Which feature would you like to know more about?";
            suggestions.add("Tell me more about Scanning.");
            suggestions.add("How do Reminders work?");
            suggestions.add("What is My Garden?");
        }

        ChatMessageModel aiMsg = new ChatMessageModel(response, getCurrentPhTime(), ChatMessageModel.TYPE_AI);
        aiMsg.setFollowUpSuggestions(suggestions);
        activeChatList.add(aiMsg);
        
        if (activeChatAdapter != null) {
            activeChatAdapter.notifyItemInserted(activeChatList.size() - 1);
        }
        if (activeRvChatMessages != null) {
            activeRvChatMessages.scrollToPosition(activeChatList.size() - 1);
        }
        appendMessageToFirestore(aiMsg);
    }

    private void launchCameraScanner() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(this, CameraScannerActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCameraScannerForChat() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(this, CameraScannerActivity.class);
            intent.putExtra(CameraScannerActivity.EXTRA_MODE, CameraScannerActivity.MODE_CHAT_ATTACHMENT);
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

    private String cleanAiResponseText(String input) {
        if (input == null || input.isEmpty()) return "";

        if (input.trim().startsWith("{") && input.trim().endsWith("}")) {
            input = input.replaceAll("[\\{\\}\"\\[\\]]", "")
                    .replaceAll(",", "\n\n")
                    .replaceAll(":", ": ");
        }

        String cleaned = input.replace("*", "");
        cleaned = cleaned.replaceAll("[#()\\[\\]_~`]", "");
        cleaned = cleaned.replaceAll("(?m)^[ \t]+|[ \t]+$", "");
        
        cleaned = cleaned.replaceAll("\n{2,}", "\n\n");
        
        if (!cleaned.contains("\n\n") && cleaned.contains("\n")) {
             cleaned = cleaned.replace("\n", "\n\n");
        }

        return cleaned.trim();
    }

    private String getCurrentUserId() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        return currentUser != null ? currentUser.getUid() : null;
    }

    private void saveSessionToHistory(String firstQuestion) {
        String userId = getCurrentUserId();
        if (userId == null || firstQuestion == null) return;

        String title = firstQuestion.trim();
        if (title.length() > 35) {
            title = title.substring(0, 32) + "...";
        }
        if (title.length() > 0) {
            title = title.substring(0, 1).toUpperCase() + title.substring(1);
        }

        if (currentSessionId == null) {
            currentSessionId = String.valueOf(System.currentTimeMillis());
        }

        Map<String, Object> sessionMap = new HashMap<>();
        sessionMap.put("uid", userId);
        sessionMap.put("sessionId", currentSessionId);
        sessionMap.put("title", title);
        sessionMap.put("timestamp", System.currentTimeMillis());

        db.collection("ai_chat_sessions")
                .document(currentSessionId)
                .set(sessionMap, SetOptions.merge());
    }

    private String encodeBitmapToBase64(Bitmap originalBitmap) {
        if (originalBitmap == null) return null;

        long maxByteSize = (long) (1.5 * 1024 * 1024);
        Bitmap workingBitmap = originalBitmap;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int quality = 85;

        workingBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);

        while (outputStream.toByteArray().length > maxByteSize && quality > 20) {
            outputStream.reset();
            quality -= 15;
            workingBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
        }

        while (outputStream.toByteArray().length > maxByteSize && workingBitmap.getWidth() > 300) {
            outputStream.reset();
            int width = (int) (workingBitmap.getWidth() * 0.7);
            int height = (int) (workingBitmap.getHeight() * 0.7);
            workingBitmap = Bitmap.createScaledBitmap(workingBitmap, width, height, true);
            workingBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
        }

        byte[] byteArray = outputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private Bitmap decodeBase64ToBitmap(String base64Str) {
        if (base64Str == null || base64Str.isEmpty()) return null;
        try {
            byte[] decodedBytes = Base64.decode(base64Str, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            return null;
        }
    }

    private void appendMessageToFirestore(ChatMessageModel message) {
        String userId = getCurrentUserId();
        if (userId == null || currentSessionId == null) return;

        Map<String, Object> newMsgMap = new HashMap<>();
        newMsgMap.put("messageText", message.getMessageText());
        newMsgMap.put("messageTime", message.getMessageTime());
        newMsgMap.put("messageType", message.getMessageType());
        newMsgMap.put("timestamp", System.currentTimeMillis());

        String currentImageBase64 = message.getImageBitmap() != null ?
                encodeBitmapToBase64(message.getImageBitmap()) : message.getImageBase64();

        if (currentImageBase64 != null) {
            newMsgMap.put("imageBase64", currentImageBase64);
        }

        if (message.getFollowUpSuggestions() != null) {
            newMsgMap.put("followUpSuggestions", message.getFollowUpSuggestions());
        }

        db.collection("ai_chat_messages")
                .document(currentSessionId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<Map<String, Object>> existingMessages = (List<Map<String, Object>>) documentSnapshot.get("messages");

                        if (existingMessages != null && !existingMessages.isEmpty()) {
                            Map<String, Object> lastMsg = existingMessages.get(existingMessages.size() - 1);
                            String lastText = (String) lastMsg.get("messageText");
                            String lastImage = (String) lastMsg.get("imageBase64");

                            boolean isDuplicate = lastText != null
                                    && lastText.equalsIgnoreCase(message.getMessageText().trim())
                                    && ((lastImage == null && currentImageBase64 == null)
                                    || (lastImage != null && lastImage.equals(currentImageBase64)));

                            if (isDuplicate) {
                                Long currentCount = lastMsg.containsKey("repeatCount") ? (Long) lastMsg.get("repeatCount") : 1L;
                                lastMsg.put("repeatCount", currentCount + 1);
                                lastMsg.put("lastUpdatedTimestamp", System.currentTimeMillis());

                                db.collection("ai_chat_messages")
                                        .document(currentSessionId)
                                        .update("messages", existingMessages, "lastUpdated", System.currentTimeMillis());
                                return;
                            }
                        }

                        newMsgMap.put("repeatCount", 1);
                        db.collection("ai_chat_messages")
                                .document(currentSessionId)
                                .update(
                                        "messages", FieldValue.arrayUnion(newMsgMap),
                                        "lastUpdated", System.currentTimeMillis()
                                );
                    } else {
                        newMsgMap.put("repeatCount", 1);
                        List<Map<String, Object>> initialMessages = new ArrayList<>();
                        initialMessages.add(newMsgMap);

                        Map<String, Object> sessionDoc = new HashMap<>();
                        sessionDoc.put("uid", userId);
                        sessionDoc.put("sessionId", currentSessionId);
                        sessionDoc.put("createdTimestamp", System.currentTimeMillis());
                        sessionDoc.put("lastUpdated", System.currentTimeMillis());
                        sessionDoc.put("messages", initialMessages);

                        db.collection("ai_chat_messages")
                                .document(currentSessionId)
                                .set(sessionDoc);
                    }
                });
    }

    private void loadUserChatHistoryFromFirestore() {
        String userId = getCurrentUserId();
        if (userId == null || currentSessionId == null) {
            addDefaultWelcomeMessage();
            showAiChatAssistantBottomSheet();
            return;
        }

        db.collection("ai_chat_messages")
                .document(currentSessionId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (activeChatList == null) {
                        activeChatList = new ArrayList<>();
                    } else {
                        activeChatList.clear();
                    }

                    if (documentSnapshot.exists()) {
                        List<Map<String, Object>> msgArray = (List<Map<String, Object>>) documentSnapshot.get("messages");
                        if (msgArray != null) {
                            for (Map<String, Object> doc : msgArray) {
                                ChatMessageModel msg = new ChatMessageModel();
                                msg.setMessageText((String) doc.get("messageText"));
                                msg.setMessageTime((String) doc.get("messageTime"));
                                Long type = (Long) doc.get("messageType");
                                if (type != null) msg.setMessageType(type.intValue());

                                String base64 = (String) doc.get("imageBase64");
                                if (base64 != null && !base64.isEmpty()) {
                                    msg.setImageBase64(base64);
                                    msg.setImageBitmap(decodeBase64ToBitmap(base64));
                                }

                                List<String> suggestions = (List<String>) doc.get("followUpSuggestions");
                                if (suggestions != null) {
                                    msg.setFollowUpSuggestions(new ArrayList<>(suggestions));
                                }

                                activeChatList.add(msg);
                            }
                        }
                    }

                    if (activeChatList.isEmpty()) {
                        addDefaultWelcomeMessage();
                    }

                    showAiChatAssistantBottomSheet();
                })
                .addOnFailureListener(e -> {
                    if (activeChatList == null || activeChatList.isEmpty()) {
                        addDefaultWelcomeMessage();
                    }
                    showAiChatAssistantBottomSheet();
                });
    }

    private void addDefaultWelcomeMessage() {
        if (activeChatList == null) activeChatList = new ArrayList<>();
        ArrayList<String> welcomeSuggestions = new ArrayList<>();
        welcomeSuggestions.add("Walk through into SmartGrow");
        welcomeSuggestions.add("How do I scan a plant?");
        welcomeSuggestions.add("Give me care tips for a Monstera.");

        ChatMessageModel welcomeMessage = new ChatMessageModel(
                "Hi! I'm SmartGrow AI, your Plant Smart Care Assistant. How can I help you today?",
                getCurrentPhTime(),
                ChatMessageModel.TYPE_AI);
        welcomeMessage.setFollowUpSuggestions(welcomeSuggestions);
        activeChatList.add(welcomeMessage);
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
                startNewChatSession();
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

    private String getCurrentPhTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Manila"));
        return sdf.format(new Date());
    }

    private void showHistoryBottomSheetDialog() {
        HistoryBottomSheet historySheet = HistoryBottomSheet.newInstance();
        historySheet.setOnSessionSelectedListener(session -> {
            if (session != null) {
                currentSessionId = session.getSessionId();
                if (activeChatDialog != null && activeChatDialog.isShowing()) {
                    activeChatDialog.dismiss();
                }
                loadUserChatHistoryFromFirestore();
            }
        });
        historySheet.show(getSupportFragmentManager(), "HistoryBottomSheet");
    }

    public void showAiChatAssistantBottomSheet() {
        if (activeChatDialog != null && activeChatDialog.isShowing()) {
            activeChatDialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
            if (activeChatAdapter != null) activeChatAdapter.notifyDataSetChanged();
            return;
        }

        if (activeChatList == null) {
            activeChatList = new ArrayList<>();
            addDefaultWelcomeMessage();
        }

        activeChatAdapter = new ChatAdapter(activeChatList);

        activeChatDialog = new BottomSheetDialog(this);
        View chatView = LayoutInflater.from(this).inflate(R.layout.dialog_chat_assistant, null);
        activeChatDialog.setContentView(chatView);
        activeChatDialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);

        activeRvChatMessages = chatView.findViewById(R.id.rv_chat_messages_list);
        EditText etChatInput = chatView.findViewById(R.id.et_chat_input);
        ImageButton ibSend = chatView.findViewById(R.id.ib_send_message);
        ImageView ibMenu = chatView.findViewById(R.id.ib_chat_menu);
        ImageView ibAttach = chatView.findViewById(R.id.ib_chat_attach);

        layoutImagePreviewContainer = chatView.findViewById(R.id.layout_image_preview_container);
        ivPreviewSelectedImage = chatView.findViewById(R.id.iv_preview_selected_image);
        ibRemovePreviewImage = chatView.findViewById(R.id.ib_remove_preview_image);

        if (ibRemovePreviewImage != null) {
            ibRemovePreviewImage.setOnClickListener(v -> clearPendingImage());
        }

        if (pendingImageBitmap != null && ivPreviewSelectedImage != null && layoutImagePreviewContainer != null) {
            ivPreviewSelectedImage.setImageBitmap(pendingImageBitmap);
            layoutImagePreviewContainer.setVisibility(View.VISIBLE);
        }

        if (activeRvChatMessages != null) {
            activeRvChatMessages.setLayoutManager(new LinearLayoutManager(this));
            activeRvChatMessages.setAdapter(activeChatAdapter);
            activeRvChatMessages.scrollToPosition(activeChatList.size() - 1);
        }

        activeChatDialog.setOnDismissListener(dialog -> {
            activeRvChatMessages = null;
            layoutImagePreviewContainer = null;
            ivPreviewSelectedImage = null;
            ibRemovePreviewImage = null;
        });

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
                        showHistoryBottomSheetDialog();
                    });
                }

                menu.showAsDropDown(v, -80, 10);
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
                        launchCameraScannerForChat();
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

        if (ibSend != null && etChatInput != null) {
            ibSend.setOnClickListener(v -> {
                String userText = etChatInput.getText().toString().trim();

                if (userText.isEmpty() && pendingImageBitmap == null) return;

                long currentTime = System.currentTimeMillis();
                if (currentTime - lastRequestTime < 3000) {
                    Toast.makeText(this, "Please wait a moment before sending another message.", Toast.LENGTH_SHORT).show();
                    return;
                }

                lastRequestTime = currentTime;

                Bitmap imageToSend = pendingImageBitmap;
                etChatInput.setText("");
                clearPendingImage();

                sendChatMessageWithMedia(userText, imageToSend);
            });
        }

        activeChatDialog.show();
    }

    private void startNewChatSession() {
        currentSessionId = String.valueOf(System.currentTimeMillis());

        if (activeChatList == null) {
            activeChatList = new ArrayList<>();
        } else {
            activeChatList.clear();
        }

        lastAnalyzedPlantProfile = "";
        clearPendingImage();
        addDefaultWelcomeMessage();

        if (activeChatAdapter != null) {
            activeChatAdapter.notifyDataSetChanged();
        }
    }

    public void submitFollowUpQuestion(String question) {
        sendChatMessageWithMedia(question, null);
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

    @Override
    protected void onResume() {
        super.onResume();
        checkCurrentFragmentForVisibility();


        if (bottomNav != null) {
            Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (current instanceof HomeFragment) {
                bottomNav.setSelectedItemId(R.id.nav_home);
            }
        }
    }
}
