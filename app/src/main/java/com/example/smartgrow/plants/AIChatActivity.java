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
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
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

public class AIChatActivity extends AppCompatActivity {

    private static final String TAG = "AIChatActivity";
    public static final String EXTRA_IMAGE_PATH = "extra_image_path";
    public static final String EXTRA_IMAGE_BASE64 = "extra_image_base64";
    public static final String EXTRA_PLANT_NAME = "EXTRA_PLANT_NAME";

    private RecyclerView rvChatMessagesList;
    private EditText etChatInput;
    private ImageButton ibSendMessage;

    // Preview image views & pending bitmap state
    private View layoutImagePreviewContainer;
    private ImageView ivPreviewSelectedImage;
    private Bitmap pendingImageBitmap = null;

    private ArrayList<ChatMessageModel> chatList;
    private ChatAdapter chatAdapter;

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
        setContentView(R.layout.activity_ai_chat);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupLaunchers();
        setupChatFunctionality();

        // Start a new session or load last one if desired
        startNewChatSession();

        // Check for incoming data
        if (getIntent() != null) {
            // Check for incoming image path
            if (getIntent().hasExtra(EXTRA_IMAGE_PATH)) {
                String imagePath = getIntent().getStringExtra(EXTRA_IMAGE_PATH);
                if (imagePath != null && new File(imagePath).exists()) {
                    Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
                    if (bitmap != null) {
                        setPendingImageForChat(getResizedBitmap(bitmap, 1024));
                    }
                }
            }

            // Check for incoming image base64
            if (getIntent().hasExtra(EXTRA_IMAGE_BASE64)) {
                String base64 = getIntent().getStringExtra(EXTRA_IMAGE_BASE64);
                if (base64 != null && !base64.isEmpty()) {
                    Bitmap bitmap = decodeBase64ToBitmap(base64);
                    if (bitmap != null) {
                        setPendingImageForChat(getResizedBitmap(bitmap, 1024));
                    }
                }
            }
        }
    }

    private void initViews() {
        rvChatMessagesList = findViewById(R.id.rv_chat_messages_list);
        etChatInput = findViewById(R.id.et_chat_input);
        ibSendMessage = findViewById(R.id.ib_send_message);
        ImageView ibChatAttach = findViewById(R.id.ib_chat_attach);
        ImageView ibBackArrow = findViewById(R.id.ib_back_arrow);
        ImageView ibChatMenu = findViewById(R.id.ib_chat_menu);

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
                if (currentTime - lastRequestTime < 2000) {
                    Toast.makeText(this, "Please wait a moment.", Toast.LENGTH_SHORT).show();
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

    private void sendChatMessageWithMedia(String messageText, Bitmap imageBitmap) {
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

        String lowerMsg = (messageText != null) ? messageText.toLowerCase().trim() : "";
        if (imageBitmap == null && isAppRelatedQuestion(lowerMsg)) {
            respondToAppQuestion(lowerMsg);
            return;
        }

        chatList.add(new ChatMessageModel("", "", ChatMessageModel.TYPE_LOADING));
        chatAdapter.notifyItemInserted(chatList.size() - 1);
        rvChatMessagesList.scrollToPosition(chatList.size() - 1);

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
                                    "We couldn't detect a plant in the provided image. Please make sure the photo is clear, well-lit, and focused on a plant.",
                                    getCurrentPhTime(),
                                    ChatMessageModel.TYPE_AI);

                            chatList.add(notPlantMsg);
                            chatAdapter.notifyItemInserted(chatList.size() - 1);
                            rvChatMessagesList.scrollToPosition(chatList.size() - 1);
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

                        chatList.add(aiMsg);
                        chatAdapter.notifyItemInserted(chatList.size() - 1);
                        rvChatMessagesList.scrollToPosition(chatList.size() - 1);
                        appendMessageToFirestore(aiMsg);
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        removeLoadingIndicator();
                        ChatMessageModel errorMsg = new ChatMessageModel("I encountered an issue. Please check your connection.", getCurrentPhTime(), ChatMessageModel.TYPE_AI);
                        chatList.add(errorMsg);
                        chatAdapter.notifyItemInserted(chatList.size() - 1);
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
                        String cleanedReply = cleanAiResponseText(rawAiReply);
                        ArrayList<String> suggestions = new ArrayList<>();

                        if (cleanedReply.contains("only answer questions related to plants")) {
                            cleanedReply = "I can only assist with plant-related topics. Ask me about plant care or identification!";
                            suggestions.add("Give me care tips for a Monstera.");
                        } else {
                            suggestions.add("Tell me about ideal humidity.");
                            suggestions.add("Walk through into SmartGrow");
                        }

                        ChatMessageModel msg = new ChatMessageModel(cleanedReply, getCurrentPhTime(), ChatMessageModel.TYPE_AI);
                        msg.setFollowUpSuggestions(suggestions);
                        chatList.add(msg);
                        chatAdapter.notifyItemInserted(chatList.size() - 1);
                        rvChatMessagesList.scrollToPosition(chatList.size() - 1);
                        appendMessageToFirestore(msg);
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        removeLoadingIndicator();
                        ChatMessageModel errorMsg = new ChatMessageModel("I encountered an issue. Please try again.", getCurrentPhTime(), ChatMessageModel.TYPE_AI);
                        chatList.add(errorMsg);
                        chatAdapter.notifyItemInserted(chatList.size() - 1);
                        appendMessageToFirestore(errorMsg);
                    });
                }
            };

            String context = lastAnalyzedPlantProfile;
            String history = buildChatHistoryContext();
            if (!history.isEmpty()) context += "\n\nContext:\n" + history;

            if (context.trim().isEmpty()) analyzer.askQuestion(messageText, aiCallback);
            else analyzer.askFollowUpQuestion(messageText, context, aiCallback);
        }
    }

    private boolean isAppRelatedQuestion(String msg) {
        if (msg == null) return false;
        String m = msg.toLowerCase().trim();
        return m.contains("how the app works") || m.contains("walk through") || m.contains("app details") ||
                m.contains("features") || m.contains("scan") || m.contains("garden") || m.contains("reminder");
    }

    private void respondToAppQuestion(String msg) {
        String response;
        ArrayList<String> suggestions = new ArrayList<>();
        String m = msg.toLowerCase().trim();

        if (m.contains("scan")) {
            response = "Plant Identification & Health Scanner\n\nTo identify a plant, tap the Scan icon. Capture a photo to identify 10,000+ species and detect diseases.";
            suggestions.add("How to set reminders?");
        } else if (m.contains("garden")) {
            response = "My Garden\n\nThe 'My Garden' section stores your scanned plants. Track growth history and access care guides here.";
            suggestions.add("How to scan a plant?");
        } else if (m.contains("reminder")) {
            response = "Care Reminders\n\nSet schedules for Watering, Fertilizing, and Repotting via the 'Set Reminder' tab.";
            suggestions.add("Tell me about the Community.");
        } else {
            response = "Welcome to SmartGrow!\n\nI can help you with:\n• Identifying plants via Scan\n• Organizing your Garden\n• Setting Care Reminders\n• Global Community Forum\n\nWhat would you like to know about?";
            suggestions.add("Tell more about Scanning.");
            suggestions.add("What is My Garden?");
        }

        ChatMessageModel aiMsg = new ChatMessageModel(response, getCurrentPhTime(), ChatMessageModel.TYPE_AI);
        aiMsg.setFollowUpSuggestions(suggestions);
        chatList.add(aiMsg);
        chatAdapter.notifyItemInserted(chatList.size() - 1);
        rvChatMessagesList.scrollToPosition(chatList.size() - 1);
        appendMessageToFirestore(aiMsg);
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
        chatList.clear();
        lastAnalyzedPlantProfile = "";
        clearPendingImage();
        addDefaultWelcomeMessage();
        chatAdapter.notifyDataSetChanged();
    }

    private void addDefaultWelcomeMessage() {
        ArrayList<String> welcomeSuggestions = new ArrayList<>();
        welcomeSuggestions.add("Walk through into SmartGrow");
        welcomeSuggestions.add("How do I scan a plant?");
        ChatMessageModel welcomeMessage = new ChatMessageModel("Hi! I'm SmartGrow AI. How can I help you today?", getCurrentPhTime(), ChatMessageModel.TYPE_AI);
        welcomeMessage.setFollowUpSuggestions(welcomeSuggestions);
        chatList.add(welcomeMessage);
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
            if (chatList.isEmpty()) addDefaultWelcomeMessage();
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
        if (input.trim().startsWith("{") && input.trim().endsWith("}")) {
            input = input.replaceAll("[\\{\\}\"\\[\\]]", "").replaceAll(",", "\n\n").replaceAll(":", ": ");
        }
        String cleaned = input.replace("*", "").replaceAll("[#()\\[\\]_~`]", "").replaceAll("(?m)^[ \t]+|[ \t]+$", "");
        cleaned = cleaned.replaceAll("\n{2,}", "\n\n");
        if (!cleaned.contains("\n\n") && cleaned.contains("\n")) cleaned = cleaned.replace("\n", "\n\n");
        return cleaned.trim();
    }

    private void saveSessionToHistory(String firstQuestion) {
        String userId = getCurrentUserId();
        if (userId == null || firstQuestion == null) return;
        String title = firstQuestion.trim();
        if (title.length() > 35) title = title.substring(0, 32) + "...";
        if (currentSessionId == null) currentSessionId = String.valueOf(System.currentTimeMillis());
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
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream);
        return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT);
    }

    private Bitmap decodeBase64ToBitmap(String base64Str) {
        if (base64Str == null || base64Str.isEmpty()) return null;
        try {
            byte[] decodedBytes = Base64.decode(base64Str, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            Log.e(TAG, "decodeBase64ToBitmap: error", e);
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