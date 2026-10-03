package com.example.smartgrow.plants;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.smartgrow.MainActivity;
import com.example.smartgrow.R;
import com.example.smartgrow.core.NotificationHelper;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlantDetailsActivity extends AppCompatActivity {

    private static final String TAG = "PlantDetailsActivity";
    public static Bitmap tempScannedBitmap = null;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private TextToSpeech textToSpeech;
    private boolean isTtsReady = false;

    private ImageView ivPlantMain, ivPlantMatch1, ivPlantMatch2, ivHealthPreview, ibSpeaker, btnPlantAiChat, ibBack;
    private TextView tvPlantTitle, tvScientificName, tvHealthState, tvAliases, tvHealthScore, tvMatchConfidence;
    private View layoutThoughtBubble;
    private ProgressBar pbHealthScore;
    private View tabOverview, tabCare, tabExplore;
    private MaterialButton btnSaveToGardenBottom;

    private String plantName = "Unknown Plant";
    private String scientificName = "N/A";
    private String healthStatus = "Healthy";
    private int healthPercentage = 100;
    private int matchConfidencePercentage = 95;
    private String aliases = "N/A";
    private boolean isArtificial = false;
    private String mRawAnalysisJson = null;
    private Bitmap scannedBitmap;
    private String plant_uid = null;

    private int currentActiveTabIndex = -1;

    private final Handler bubbleHandler = new Handler(Looper.getMainLooper());
    private Runnable showBubbleRunnable, hideBubbleRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plant_details);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initStaticViews();
        initTextToSpeech();

        if (tempScannedBitmap != null) {
            this.scannedBitmap = getResizedBitmap(tempScannedBitmap, 800);
            tempScannedBitmap = null;
            updateImages();
        }

        parseIntentData();

        if (ibBack != null) ibBack.setOnClickListener(v -> finish());
        if (ibSpeaker != null) ibSpeaker.setOnClickListener(v -> speakPlantPronunciation());
        
        if (btnPlantAiChat != null) {
            btnPlantAiChat.setOnClickListener(v -> {
                Intent intent = new Intent(PlantDetailsActivity.this, AIChatActivity.class);
                if (scannedBitmap != null) {
                    intent.putExtra(AIChatActivity.EXTRA_IMAGE_BASE64, encodeBitmapToBase64(scannedBitmap));
                }
                intent.putExtra(AIChatActivity.EXTRA_PLANT_NAME, plantName);
                startActivity(intent);
            });
        }

        if (layoutThoughtBubble != null) {
            layoutThoughtBubble.setOnClickListener(v -> {
                if (btnPlantAiChat != null) btnPlantAiChat.performClick();
            });
        }

        if (btnSaveToGardenBottom != null) btnSaveToGardenBottom.setOnClickListener(v -> savePlantToDiary());

        setupClickableImages();
        setupTabs();
        startBubbleCycle();
    }

    private void initStaticViews() {
        ivPlantMain = findViewById(R.id.iv_plant_main);
        ivPlantMatch1 = findViewById(R.id.iv_plant_match_1);
        ivPlantMatch2 = findViewById(R.id.iv_plant_match_2);
        ivHealthPreview = findViewById(R.id.iv_health_preview);
        ibBack = findViewById(R.id.ib_back);
        ibSpeaker = findViewById(R.id.ib_speaker);
        btnPlantAiChat = findViewById(R.id.btn_plant_ai_chat);
        layoutThoughtBubble = findViewById(R.id.layout_thought_bubble);

        tvPlantTitle = findViewById(R.id.tv_plant_title);
        tvScientificName = findViewById(R.id.tv_scientific_name);
        tvHealthState = findViewById(R.id.tv_health_state);
        tvHealthScore = findViewById(R.id.tv_health_score);
        tvMatchConfidence = findViewById(R.id.tv_match_confidence);
        tvAliases = findViewById(R.id.tv_aliases);
        pbHealthScore = findViewById(R.id.pb_health_score);

        tabOverview = findViewById(R.id.tab_overview);
        tabCare = findViewById(R.id.tab_care);
        tabExplore = findViewById(R.id.tab_explore);

        btnSaveToGardenBottom = findViewById(R.id.btn_save_to_garden_bottom);
    }

    private void startBubbleCycle() {
        if (layoutThoughtBubble == null) return;

        showBubbleRunnable = new Runnable() {
            @Override
            public void run() {
                layoutThoughtBubble.setVisibility(View.VISIBLE);
                bubbleHandler.postDelayed(hideBubbleRunnable, 5000); // Show for 5 seconds
            }
        };

        hideBubbleRunnable = new Runnable() {
            @Override
            public void run() {
                layoutThoughtBubble.setVisibility(View.GONE);
                bubbleHandler.postDelayed(showBubbleRunnable, 3000); // Hide for 3 seconds
            }
        };

        // Initial delay before first show
        bubbleHandler.postDelayed(showBubbleRunnable, 3000);
    }

    private void setupTabs() {
        if (tabOverview != null) tabOverview.setOnClickListener(v -> scrollToSection(0));
        if (tabCare != null) tabCare.setOnClickListener(v -> scrollToSection(1));
        if (tabExplore != null) tabExplore.setOnClickListener(v -> scrollToSection(2));
        updateTabSelection(0);
    }

    private void scrollToSection(int index) {
        Fragment f = getSupportFragmentManager().findFragmentById(R.id.plant_details_fragment_container);
        if (f instanceof PlantDetailsFragment) {
            ((PlantDetailsFragment) f).scrollToSection(index);
        }
        updateTabSelection(index);
    }

    public void onSectionChanged(int index) {
        runOnUiThread(() -> updateTabSelection(index));
    }

    private void updateTabSelection(int index) {
        if (currentActiveTabIndex == index) return;
        currentActiveTabIndex = index;
        
        int primaryGreen = ContextCompat.getColor(this, R.color.primary_green);
        View[] tabViews = {tabOverview, tabCare, tabExplore};
        
        for (int i = 0; i < tabViews.length; i++) {
            if (tabViews[i] instanceof LinearLayout) {
                LinearLayout tabLayout = (LinearLayout) tabViews[i];
                TextView tabText = (TextView) tabLayout.getChildAt(0);
                
                if (i == index) {
                    tabLayout.setBackgroundResource(R.drawable.bg_pill_active);
                    tabText.setTextColor(Color.WHITE);
                } else {
                    tabLayout.setBackgroundResource(0);
                    tabText.setTextColor(primaryGreen);
                }
            }
        }
    }

    private void updateImages() {
        if (scannedBitmap == null) return;
        if (ivPlantMain != null) ivPlantMain.setImageBitmap(scannedBitmap);
        if (ivHealthPreview != null) ivHealthPreview.setImageBitmap(scannedBitmap);
        if (ivPlantMatch1 != null) ivPlantMatch1.setImageBitmap(scannedBitmap);
        if (ivPlantMatch2 != null) ivPlantMatch2.setImageBitmap(scannedBitmap);
    }

    private void parseIntentData() {
        if (getIntent() == null) return;

        plant_uid = getIntent().getStringExtra("plant_uid");
        String plantId = getIntent().getStringExtra("plant_id");
        if (plantId != null && !plantId.isEmpty()) {
            loadPlantFromFirestore(plantId);
            return;
        }

        healthPercentage = getIntent().getIntExtra("health_percentage", 100);
        matchConfidencePercentage = getIntent().getIntExtra("match_percentage", 95);

        String base64ImageExtra = getIntent().getStringExtra("image_base64");
        if (base64ImageExtra != null && !base64ImageExtra.isEmpty()) {
            scannedBitmap = decodeBase64ToBitmap(base64ImageExtra);
            updateImages();
        }

        String rawJson = getIntent().getStringExtra("raw_ai_json");
        if (rawJson != null && !rawJson.trim().isEmpty()) {
            mRawAnalysisJson = rawJson;
            populateStaticData(sanitizeJsonString(rawJson));
            loadFragment(mRawAnalysisJson);
        }
    }

    private void loadPlantFromFirestore(String plantId) {
        if (btnSaveToGardenBottom != null) btnSaveToGardenBottom.setVisibility(View.GONE);
        db.collection("diary").document(plantId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                plantName = doc.getString("plantName");
                scientificName = doc.getString("scientificName");
                healthStatus = doc.getString("healthStatus");
                Long hp = doc.getLong("healthPercentage");
                if (hp != null) healthPercentage = hp.intValue();
                Long mc = doc.getLong("matchConfidencePercentage");
                if (mc != null) matchConfidencePercentage = mc.intValue();
                aliases = doc.getString("aliases");
                Boolean art = doc.getBoolean("isArtificial");
                if (art != null) isArtificial = art;
                
                String base64 = doc.getString("imageBase64");
                if (base64 != null) {
                    scannedBitmap = decodeBase64ToBitmap(base64);
                    updateImages();
                }

                mRawAnalysisJson = doc.getString("rawAnalysisJson");
                updateStaticUI();
                if (mRawAnalysisJson != null) loadFragment(mRawAnalysisJson);
            }
        });
    }

    private void populateStaticData(String json) {
        try {
            JSONObject root = new JSONObject(json);
            JSONObject profile = root.optJSONObject("plant_profile");
            if (profile != null) {
                plantName = profile.optString("name", "Unknown");
                scientificName = profile.optString("scientific_name", "N/A");
                aliases = profile.optString("philippine_name", profile.optString("aliases", "N/A"));
                matchConfidencePercentage = parsePercentage(profile.opt("confidence"), matchConfidencePercentage);
            }
            JSONObject health = root.optJSONObject("health_scanner");
            if (health != null) {
                healthStatus = health.optString("status", "Healthy");
                healthPercentage = parsePercentage(health.opt("health_score"), healthPercentage);
            }
            isArtificial = root.optBoolean("is_artificial", false);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing static data", e);
        }
        updateStaticUI();
    }

    private void updateStaticUI() {
        if (tvPlantTitle != null) tvPlantTitle.setText(plantName);
        if (tvScientificName != null) tvScientificName.setText(scientificName);
        if (tvAliases != null) tvAliases.setText("Also known as: " + aliases);
        if (tvHealthState != null) {
            tvHealthState.setText(healthStatus);
            tvHealthState.setTextColor(Color.parseColor(getHealthColorHex()));
        }
        if (tvHealthScore != null) {
            tvHealthScore.setText(isArtificial ? "Status: " + healthStatus : "Health: " + healthPercentage + "%");
            tvHealthScore.setTextColor(Color.parseColor(getHealthColorHex()));
        }
        if (pbHealthScore != null) pbHealthScore.setProgress(healthPercentage);
        if (tvMatchConfidence != null) tvMatchConfidence.setText(matchConfidencePercentage + "% Match Confidence");
    }

    private void loadFragment(String rawJson) {
        Fragment fragment = PlantDetailsFragment.newInstance(rawJson);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.plant_details_fragment_container, fragment)
                .commit();
    }

    private String getHealthColorHex() {
        if (healthPercentage < 50) return "#F44336";
        if (healthPercentage < 80) return "#FFC107";
        return "#81C784";
    }

    private int parsePercentage(Object raw, int fallback) {
        if (raw == null) return fallback;
        if (raw instanceof Integer) return (Integer) raw;
        if (raw instanceof Double) return ((Double) raw).intValue();
        Matcher m = Pattern.compile("(\\d+)").matcher(String.valueOf(raw));
        if (m.find()) return Integer.parseInt(m.group(1));
        return fallback;
    }

    private String sanitizeJsonString(String raw) {
        if (raw == null) return "";
        String cleaned = raw.trim();
        if (cleaned.startsWith("```")) {
            int first = cleaned.indexOf("\n");
            cleaned = cleaned.substring(first != -1 ? first + 1 : 3);
            if (cleaned.endsWith("```")) cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        return cleaned.trim();
    }

    private Bitmap decodeBase64ToBitmap(String base64Str) {
        try {
            byte[] bytes = android.util.Base64.decode(base64Str, android.util.Base64.DEFAULT);
            return android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception e) { return null; }
    }

    private String encodeBitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, baos);
        return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
    }

    private Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth(), height = image.getHeight();
        float ratio = (float) width / (float) height;
        if (ratio > 1) { width = maxSize; height = (int) (width / ratio); }
        else { height = maxSize; width = (int) (height * ratio); }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }

    private void initTextToSpeech() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(Locale.US);
                isTtsReady = true;
            }
        });
    }

    private void speakPlantPronunciation() {
        if (isTtsReady && textToSpeech != null) {
            String text = plantName + ". " + (scientificName.equals("N/A") ? "" : scientificName);
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "TTS");
        }
    }

    private void setupClickableImages() {
        View.OnClickListener l = v -> { if (scannedBitmap != null) showBitmapPreviewDialog(scannedBitmap); };
        if (ivPlantMain != null) ivPlantMain.setOnClickListener(l);
        if (ivHealthPreview != null) ivHealthPreview.setOnClickListener(l);
    }

    private void showBitmapPreviewDialog(Bitmap bitmap) {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        ImageView iv = new ImageView(this);
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        iv.setImageBitmap(bitmap);
        iv.setOnClickListener(v -> dialog.dismiss());
        dialog.setContentView(iv);
        dialog.show();
    }

    private void savePlantToDiary() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) { Toast.makeText(this, "Login to save", Toast.LENGTH_SHORT).show(); return; }
        performSave(user.getUid());
    }

    private void performSave(String userId) {
        String docId = String.valueOf(System.currentTimeMillis());
        Map<String, Object> diaryEntry = new HashMap<>();
        diaryEntry.put("id", docId);
        diaryEntry.put("userId", userId);
        diaryEntry.put("plant_uid", plant_uid != null ? plant_uid : docId);
        diaryEntry.put("plantName", plantName);
        diaryEntry.put("scientificName", scientificName);
        diaryEntry.put("healthStatus", healthStatus);
        diaryEntry.put("healthPercentage", healthPercentage);
        diaryEntry.put("healthColor", getHealthColorHex());
        diaryEntry.put("matchConfidencePercentage", matchConfidencePercentage);
        diaryEntry.put("aliases", aliases);
        diaryEntry.put("isArtificial", isArtificial);
        diaryEntry.put("timestamp", System.currentTimeMillis());
        diaryEntry.put("rawAnalysisJson", mRawAnalysisJson);

        if (scannedBitmap != null) {
            diaryEntry.put("imageBase64", encodeBitmapToBase64(scannedBitmap));
        }

        try {
            JSONObject root = new JSONObject(sanitizeJsonString(mRawAnalysisJson));
            JSONObject profile = root.optJSONObject("plant_profile");
            if (profile != null) {
                diaryEntry.put("distribution", profile.optString("distribution_text", profile.optString("origin", "N/A")));
                diaryEntry.put("habitat", profile.optString("habitat", "N/A"));
                diaryEntry.put("petToxicity", profile.optString("pet_toxicity", "N/A"));
                diaryEntry.put("weedPotential", profile.optString("weed_potential", "N/A"));
                diaryEntry.put("plantType", profile.optString("type", profile.optString("plant_type", "N/A")));
                diaryEntry.put("lifespan", profile.optString("lifespan", "N/A"));
                diaryEntry.put("careDifficultyText", profile.optString("care_difficulty", "Easy"));
            }
        } catch (Exception ignored) {}

        if (healthPercentage < 50) {
            Map<String, Object> reminderData = new HashMap<>();
            reminderData.put("wateringSchedule", "Every Day");
            reminderData.put("fertilizerSchedule", "Every Week");
            reminderData.put("sunlightSchedule", "Every Day");
            reminderData.put("preferredTime", "08:00 AM");
            diaryEntry.put("reminders", reminderData);
        }

        db.collection("diary").document(docId).set(diaryEntry).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Saved to My Garden Diary!", Toast.LENGTH_SHORT).show();
            if (healthPercentage < 50) {
                try {
                    NotificationHelper.createNotificationChannel(this);
                    NotificationHelper.scheduleReminder(this, docId, plantName, "Water", "08:00 AM", "Every Day");
                } catch (Exception ignored) {}
            }
            startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
        }).addOnFailureListener(e -> Toast.makeText(this, "Error saving: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) { textToSpeech.stop(); textToSpeech.shutdown(); }
        bubbleHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
