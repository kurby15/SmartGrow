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
import android.util.Base64;
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
import com.example.smartgrow.camera.HistoryBottomSheet;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

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

    // Firebase References
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentSessionId = null;

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

    // ==========================================
    // TEXT SANITIZER & FORMAT CLEANER
    // ==========================================

    /**
     * Removes markdown formatting symbols (*, #, (), [], _, ~, `) and normalizes spaces.
     * Also strips raw JSON brackets if unparsed string reaches the UI layer.
     */
    private String cleanAiResponseText(String input) {
        if (input == null || input.isEmpty()) return "";

        // Emergency fallback: If raw JSON string reaches UI, convert keys to human-readable lines
        if (input.trim().startsWith("{") && input.trim().endsWith("}")) {
            input = input.replaceAll("[\\{\\}\"\\[\\]]", "")
                    .replaceAll(",", "\n")
                    .replaceAll(":", ": ");
        }

        // Remove markdown control characters (*, #, (), [], _, ~, `)
        String cleaned = input.replaceAll("[*#()\\[\\]_~`]", "");

        // Trim excess space on individual lines
        cleaned = cleaned.replaceAll("(?m)^[ \t]+|[ \t]+$", "");

        // Normalize multiple blank lines into at most two newlines
        cleaned = cleaned.replaceAll("\n{3,}", "\n\n");

        return cleaned.trim();
    }

    // ==========================================
    // FIRESTORE HISTORY HELPERS & BASE64 PARSER
    // ==========================================

    private String encodeBitmapToBase64(Bitmap originalBitmap) {
        if (originalBitmap == null) return null;

        long maxByteSize = (long) (1.5 * 1024 * 1024); // 1.5 MB Safety threshold
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

    private String getCurrentUserId() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        return currentUser != null ? currentUser.getUid() : null;
    }

    private void ensureActiveSession(String initialText) {
        if (currentSessionId != null) return;

        currentSessionId = UUID.randomUUID().toString();
        String userId = getCurrentUserId();
        if (userId == null) return;

        String title = initialText.length() > 30 ? initialText.substring(0, 30) + "..." : initialText;

        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("sessionId", currentSessionId);
        sessionData.put("title", title);
        sessionData.put("timestamp", System.currentTimeMillis());

        db.collection("users")
                .document(userId)
                .collection("ai_history")
                .document(currentSessionId)
                .set(sessionData);
    }

    private void saveMessageToFirestore(ChatMessageModel message) {
        String userId = getCurrentUserId();
        if (userId == null) return;

        String initialSessionName = "Plant Analysis";
        if (message.getText() != null && !message.getText().isEmpty()) {
            initialSessionName = message.getText();
        }

        if (currentSessionId == null) {
            ensureActiveSession(initialSessionName);
        }

        Map<String, Object> msgMap = new HashMap<>();
        msgMap.put("messageText", message.getMessageText());
        msgMap.put("messageTime", message.getMessageTime());
        msgMap.put("messageType", message.getMessageType());
        msgMap.put("timestamp", System.currentTimeMillis());

        if (message.getImageBitmap() != null) {
            String base64Image = encodeBitmapToBase64(message.getImageBitmap());
            msgMap.put("imageBase64", base64Image);
        } else if (message.getImageBase64() != null) {
            msgMap.put("imageBase64", message.getImageBase64());
        }

        if (message.getFollowUpSuggestions() != null) {
            msgMap.put("followUpSuggestions", message.getFollowUpSuggestions());
        }

        db.collection("users")
                .document(userId)
                .collection("ai_history")
                .document(currentSessionId)
                .collection("messages")
                .add(msgMap);
    }

    private void loadSessionMessagesFromFirestore(String sessionId) {
        String userId = getCurrentUserId();
        if (userId == null || sessionId == null) return;

        currentSessionId = sessionId;

        db.collection("users")
                .document(userId)
                .collection("ai_history")
                .document(sessionId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (activeChatList == null) {
                        activeChatList = new ArrayList<>();
                    } else {
                        activeChatList.clear();
                    }

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        ChatMessageModel msg = doc.toObject(ChatMessageModel.class);

                        if (msg.getImageBase64() != null && !msg.getImageBase64().isEmpty()) {
                            Bitmap bitmap = decodeBase64ToBitmap(msg.getImageBase64());
                            msg.setImageBitmap(bitmap);
                        }

                        activeChatList.add(msg);
                    }

                    showAiChatAssistantBottomSheet();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load chat session.", Toast.LENGTH_SHORT).show()
                );
    }

    // ==========================================
    // UI POPUPS & ASSISTANT LOGIC
    // ==========================================

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
            if (activeChatAdapter != null) activeChatAdapter.notifyDataSetChanged();
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

        activeChatAdapter = new ChatAdapter(activeChatList);

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
        currentSessionId = UUID.randomUUID().toString();
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

            saveMessageToFirestore(newWelcomeMsg);
        }
        Toast.makeText(this, "New Chat Started", Toast.LENGTH_SHORT).show();
    }

    private void handleImageAnalysis(Bitmap bitmap) {
        if (bitmap == null) return;

        if (activeChatList == null || activeChatAdapter == null) {
            showAiChatAssistantBottomSheet();
        }

        ChatMessageModel userImageMsg = new ChatMessageModel(bitmap, getCurrentPhTime(), ChatMessageModel.TYPE_USER);
        activeChatList.add(userImageMsg);
        activeChatAdapter.notifyItemInserted(activeChatList.size() - 1);

        saveMessageToFirestore(userImageMsg);

        activeChatList.add(new ChatMessageModel("", "", ChatMessageModel.TYPE_LOADING));
        activeChatAdapter.notifyItemInserted(activeChatList.size() - 1);
        if (activeRvChatMessages != null) {
            activeRvChatMessages.scrollToPosition(activeChatList.size() - 1);
        }

        PlantAnalyzer analyzer = new PlantAnalyzer();
        analyzer.analyzePlant(bitmap, new PlantAnalyzer.PlantCallback() {
            @Override
            public void onSuccess(String rawStructuredResult) {
                runOnUiThread(() -> {
                    removeLoadingIndicator();
                    if (activeChatList == null || activeChatAdapter == null) return;

                    String cleanedResult = cleanAiResponseText(rawStructuredResult);
                    lastAnalyzedPlantProfile = cleanedResult;

                    ArrayList<String> suggestions = new ArrayList<>();

                    if (rawStructuredResult != null && rawStructuredResult.contains("does not appear to contain a plant")) {
                        suggestions.add("How to scan correctly?");
                        suggestions.add("See sample plant image");
                    } else if (rawStructuredResult != null && rawStructuredResult.contains("Artificial / Fake Plant Detected")) {
                        suggestions.add("How to clean artificial plants?");
                        suggestions.add("Care tips for the real version");
                        suggestions.add("Where to buy real plants?");
                    } else {
                        String plantName = "this plant";
                        String majorSymptom = "";

                        try {
                            if (rawStructuredResult != null && rawStructuredResult.contains("Local Name:")) {
                                int localStart = rawStructuredResult.indexOf("Local Name:") + "Local Name:".length();
                                int localEnd = rawStructuredResult.indexOf("\n", localStart);
                                if (localEnd == -1) localEnd = rawStructuredResult.length();
                                plantName = cleanAiResponseText(rawStructuredResult.substring(localStart, localEnd));
                            } else if (rawStructuredResult != null && rawStructuredResult.contains("Name:")) {
                                int nameStart = rawStructuredResult.indexOf("Name:") + "Name:".length();
                                int nameEnd = rawStructuredResult.indexOf("\n", nameStart);
                                if (nameEnd == -1) nameEnd = rawStructuredResult.length();
                                plantName = cleanAiResponseText(rawStructuredResult.substring(nameStart, nameEnd));
                            }

                            if (rawStructuredResult != null && rawStructuredResult.contains("Problems Detected")) {
                                int problemSectionStart = rawStructuredResult.indexOf("Problems Detected");
                                int lineStart = rawStructuredResult.indexOf("\n", problemSectionStart);
                                if (lineStart != -1) {
                                    int lineEnd = rawStructuredResult.indexOf("\n", lineStart + 1);
                                    if (lineEnd == -1) lineEnd = rawStructuredResult.length();
                                    majorSymptom = cleanAiResponseText(rawStructuredResult.substring(lineStart, lineEnd));
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

                    ChatMessageModel aiMessage = new ChatMessageModel(cleanedResult, getCurrentPhTime(), ChatMessageModel.TYPE_AI);
                    aiMessage.setFollowUpSuggestions(suggestions);

                    activeChatList.add(aiMessage);
                    activeChatAdapter.notifyItemInserted(activeChatList.size() - 1);

                    saveMessageToFirestore(aiMessage);

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

                    ChatMessageModel errorMsg = new ChatMessageModel(
                            "Sorry, I encountered an internal communication issue: " + error,
                            getCurrentPhTime(),
                            ChatMessageModel.TYPE_AI);

                    activeChatList.add(errorMsg);
                    activeChatAdapter.notifyItemInserted(activeChatList.size() - 1);

                    saveMessageToFirestore(errorMsg);

                    if (activeRvChatMessages != null) {
                        activeRvChatMessages.scrollToPosition(activeChatList.size() - 1);
                    }
                });
            }
        });
    }

    public void submitFollowUpQuestion(String question) {
        if (activeChatList == null || activeChatAdapter == null) return;

        ChatMessageModel userMsg = new ChatMessageModel(question, getCurrentPhTime(), ChatMessageModel.TYPE_USER);
        activeChatList.add(userMsg);
        activeChatAdapter.notifyItemInserted(activeChatList.size() - 1);

        saveMessageToFirestore(userMsg);

        activeChatList.add(new ChatMessageModel("", "", ChatMessageModel.TYPE_LOADING));
        activeChatAdapter.notifyItemInserted(activeChatList.size() - 1);
        if (activeRvChatMessages != null) {
            activeRvChatMessages.scrollToPosition(activeChatList.size() - 1);
        }

        PlantAnalyzer analyzer = new PlantAnalyzer();

        PlantAnalyzer.PlantCallback aiCallback = new PlantAnalyzer.PlantCallback() {
            @Override
            public void onSuccess(String rawAiReply) {
                runOnUiThread(() -> {
                    removeLoadingIndicator();
                    if (activeChatList == null || activeChatAdapter == null) return;

                    String cleanedReply = cleanAiResponseText(rawAiReply);

                    ArrayList<String> suggestions = new ArrayList<>();

                    if (cleanedReply.contains("only answer questions related to plants")) {
                        suggestions.add("Give me care tips for a Monstera.");
                        suggestions.add("How often should I water succulents?");
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

                    ChatMessageModel msg = new ChatMessageModel(cleanedReply, getCurrentPhTime(), ChatMessageModel.TYPE_AI);
                    msg.setFollowUpSuggestions(suggestions);

                    activeChatList.add(msg);
                    activeChatAdapter.notifyItemInserted(activeChatList.size() - 1);

                    saveMessageToFirestore(msg);

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

                    ChatMessageModel errorMsg = new ChatMessageModel(
                            "Sorry, I couldn't process your request: " + error,
                            getCurrentPhTime(),
                            ChatMessageModel.TYPE_AI);

                    activeChatList.add(errorMsg);
                    activeChatAdapter.notifyItemInserted(activeChatList.size() - 1);

                    saveMessageToFirestore(errorMsg);

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
        HistoryBottomSheet historyBottomSheet = HistoryBottomSheet.newInstance();
        historyBottomSheet.setOnSessionSelectedListener(session -> {
            if (activeChatDialog != null && activeChatDialog.isShowing()) {
                activeChatDialog.dismiss();
            }
            loadSessionMessagesFromFirestore(session.getSessionId());
        });
        historyBottomSheet.show(getSupportFragmentManager(), "HistoryBottomSheet");
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