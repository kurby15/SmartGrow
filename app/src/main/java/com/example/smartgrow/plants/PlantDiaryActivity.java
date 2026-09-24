package com.example.smartgrow.plants;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.smartgrow.R;
import com.example.smartgrow.camera.CameraScannerActivity;
import com.example.smartgrow.profile.ProfileFragment;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PlantDiaryActivity extends AppCompatActivity {

    private static final String TAG = "PlantDiaryActivity";

    private TextView btnPlantInfo, btnSchedule, btnHistory;
    private ViewPager2 viewPager;
    private String plantId;

    public static Bitmap tempScannedBitmap = null;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration plantListener;

    // TTS Engine
    private TextToSpeech textToSpeech;
    private boolean isTtsReady = false;

    // Parent Scroll View
    private NestedScrollView mainScrollView;

    // UI Navigation Tabs
    private View tabOverview, tabCare, tabExplore;

    // UI Elements - Images & Map
    private ImageView ivPlantMain, ivPlantMatch1, ivPlantMatch2, ivHealthPreview, ibSpeaker, btnPlantAiChat;
    private MapView mapView;
    private GoogleMap googleMap;

    // UI Elements - Top Info & Scores
    private TextView tvPlantTitle, tvScientificName, tvHealthState, tvAliases;
    private TextView tvHealthScore, tvMatchConfidence;

    // UI Elements - Basic Info & Care
    private TextView tvPetToxicity, tvWeedPotential, tvDistribution, tvHabitat, tvPlantType, tvLifespan, tvCareDifficulty;
    private LinearLayout layoutDistributionClick;

    // UI Elements - Common Problems Container
    private LinearLayout layoutCommonProblemsContainer;

    // UI Elements - Characteristics
    private TextView tvUltimateHeight, tvUltimateSpread, tvLeafType, tvPlantingTime, tvLeafColor;
    private LinearLayout layoutLeafColorsContainer;

    // UI Elements - Care Conditions & How-tos
    private TextView tvTemp, tvHardiness, tvSunlight, tvSoil;
    private TextView tvPruningContent, tvPropagationContent, tvRepottingContent, tvDiagnose;

    // UI Elements - Additional Dynamic Text Sections
    private TextView tvUsesContent, tvAdaptationContent, tvEcologicalContent, tvHistoryContent, tvNameStoryContent, tvSymbolismContent;

    // UI Elements - Buttons
    private MaterialButton btnSaveToGardenBottom, btnInlineSave;
    private ImageView ibBack;

    // Extracted Data Fields (Text + Percentages)
    private String plantName = "Unknown Plant";
    private String scientificName = "N/A";
    private String healthStatus = "Healthy";
    int healthPercentage = 100;
    int matchConfidencePercentage = 95;
    private String careDifficultyText = "Easy";
    private int careDifficultyPercentage = 50;

    private String aliases = "N/A";
    private String petToxicity = "Non-toxic";
    private String weedPotential = "Low";
    private String distribution = "N/A";
    private String habitat = "N/A";
    private String plantType = "Unknown";
    private String lifespan = "N/A";
    private boolean isArtificial = false;

    private List<String> leafColorsList = new ArrayList<>();

    // Coordinates (Default Region)
    private double mapLat = 14.5995;
    private double mapLng = 120.9842;

    private static class MapLocation {
        double lat;
        double lng;
        String title;
        String snippet;
        String distributionType;

        MapLocation(double lat, double lng, String title, String snippet, String distributionType) {
            this.lat = lat;
            this.lng = lng;
            this.title = title;
            this.snippet = snippet;
            this.distributionType = distributionType;
        }
    }
    private List<PlantDiaryActivity.MapLocation> mapLocations = new ArrayList<>();

    // Dynamic Details
    private String ultimateHeight = "N/A";
    private String ultimateSpread = "N/A";
    private String leafType = "N/A";
    private String plantingTime = "N/A";
    private String temperatureRange = "N/A";
    private String hardinessZones = "N/A";
    private String sunlightText = "Partial sun";
    private String soilText = "Loam, Sandy loam";
    private String pruningText = "N/A";
    private String propagationText = "N/A";
    private String repottingText = "N/A";

    private String usesText = "N/A";
    private String adaptationText = "N/A";
    private String ecologicalText = "N/A";
    private String historyText = "N/A";
    private String nameStoryText = "N/A";
    private String symbolismText = "N/A";

    private JSONArray commonProblemsArray = null;
    private Bitmap scannedBitmap;
    private String mRawAnalysisJson = null;
    private String plantBase64Image = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plant_diary);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        plantId = getIntent().getStringExtra("plant_id");

        initViews();
        setupTabs();
        setupTextToSpeech();

        // Initialize MapView
        if (mapView != null) {
            mapView.onCreate(savedInstanceState);
            mapView.getMapAsync(map -> {
                googleMap = map;
                updateMapMarkers();
            });
        }

        if (plantId != null && !plantId.isEmpty()) {
            startPlantListener();
        }

        if (ibBack != null) {
            ibBack.setOnClickListener(v -> finish());
        }

        if (ibSpeaker != null) {
            ibSpeaker.setOnClickListener(v -> speakPlantDetails());
        }

        if (tvDiagnose != null) {
            tvDiagnose.setOnClickListener(v -> {
                Intent intent = new Intent(PlantDiaryActivity.this, CameraScannerActivity.class);
                intent.putExtra("plant_id", plantId);
                intent.putExtra("plant_name", plantName);
                startActivity(intent);
            });
        }

        btnPlantAiChat.setOnClickListener(v -> {
            String plantNameStr = tvPlantTitle.getText().toString();

            Intent intent = new Intent(PlantDiaryActivity.this, AIChatActivity.class);
            intent.putExtra(AIChatActivity.EXTRA_PLANT_NAME, plantNameStr);
            if (plantBase64Image != null) {
                intent.putExtra(AIChatActivity.EXTRA_IMAGE_BASE64, plantBase64Image);
            }
            startActivity(intent);
        });
    }

    private void initViews() {
        // Tabs & ViewPager
        btnPlantInfo = findViewById(R.id.btn_tab_plant_info);
        btnSchedule = findViewById(R.id.btn_tab_schedule);
        btnHistory = findViewById(R.id.btn_tab_history);
        viewPager = findViewById(R.id.view_pager);

        // Header Views
        ivPlantMain = findViewById(R.id.iv_plant_main);
        ivPlantMatch1 = findViewById(R.id.iv_plant_match_1);
        ivPlantMatch2 = findViewById(R.id.iv_plant_match_2);
        ibBack = findViewById(R.id.ib_back);
        ibSpeaker = findViewById(R.id.ib_speaker);
        btnPlantAiChat = findViewById(R.id.btn_plant_ai_chat);

        // Map View
        mapView = findViewById(R.id.map_view);

        tvPlantTitle = findViewById(R.id.tv_plant_title);
        tvScientificName = findViewById(R.id.tv_scientific_name);
        tvHealthScore = findViewById(R.id.tv_health_score);
        tvHealthState = findViewById(R.id.tv_health_state);
        tvAliases = findViewById(R.id.tv_also_known);
        tvMatchConfidence = findViewById(R.id.tv_match_confidence);
        tvDiagnose = findViewById(R.id.btn_diagnose);

        tvLeafColor = findViewById(R.id.layout_leaf_colors_container);
    }

    private void setupTextToSpeech() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.ENGLISH);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "TTS Language not supported");
                } else {
                    isTtsReady = true;
                }
            } else {
                Log.e(TAG, "TTS Initialization failed");
            }
        });
    }

    private void speakPlantDetails() {
        if (!isTtsReady) {
            Toast.makeText(this, "Text-to-Speech is not ready yet.", Toast.LENGTH_SHORT).show();
            return;
        }

        String textToSpeak = "Plant name: " + plantName + ". Scientific name: " + scientificName + ". Status: " + healthStatus;
        if (textToSpeak != null && !textToSpeak.isEmpty()) {
            textToSpeech.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    private void setupTabs() {
        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(adapter);

        if (btnPlantInfo != null) btnPlantInfo.setOnClickListener(v -> viewPager.setCurrentItem(0));
        if (btnSchedule != null) btnSchedule.setOnClickListener(v -> viewPager.setCurrentItem(1));
        if (btnHistory != null) btnHistory.setOnClickListener(v -> viewPager.setCurrentItem(2));

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateTabUI(position);
            }
        });
    }

    private void startPlantListener() {
        if (plantId == null || plantId.isEmpty()) return;

        plantListener = db.collection("diary").document(plantId)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e);
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        // Basic Info & Text fields
                        plantName = documentSnapshot.getString("plantName");
                        scientificName = documentSnapshot.getString("scientificName");
                        healthStatus = documentSnapshot.getString("healthStatus");
                        aliases = documentSnapshot.getString("aliases");
                        distribution = documentSnapshot.getString("distribution");
                        habitat = documentSnapshot.getString("habitat");
                        petToxicity = documentSnapshot.getString("petToxicity");
                        weedPotential = documentSnapshot.getString("weedPotential");
                        plantType = documentSnapshot.getString("plantType");
                        lifespan = documentSnapshot.getString("lifespan");
                        careDifficultyText = documentSnapshot.getString("careDifficultyText");

                        // Characteristics
                        ultimateHeight = documentSnapshot.getString("ultimateHeight");
                        ultimateSpread = documentSnapshot.getString("ultimateSpread");
                        leafType = documentSnapshot.getString("leafType");
                        plantingTime = documentSnapshot.getString("plantingTime");

                        // Parse Leaf Colors List
                        leafColorsList.clear();
                        List<String> colors = (List<String>) documentSnapshot.get("leaf_colors");
                        if (colors != null) {
                            leafColorsList.addAll(colors);
                        }

                        // Care Conditions
                        temperatureRange = documentSnapshot.getString("temperatureRange");
                        hardinessZones = documentSnapshot.getString("hardinessZones");
                        sunlightText = documentSnapshot.getString("sunlight");
                        soilText = documentSnapshot.getString("soil");

                        // How-tos
                        pruningText = documentSnapshot.getString("pruning");
                        propagationText = documentSnapshot.getString("propagation");
                        repottingText = documentSnapshot.getString("repotting");

                        // Additional Dynamic Sections
                        usesText = documentSnapshot.getString("usesText");
                        adaptationText = documentSnapshot.getString("adaptationText");
                        ecologicalText = documentSnapshot.getString("ecologicalText");
                        historyText = documentSnapshot.getString("historyText");
                        nameStoryText = documentSnapshot.getString("nameStoryText");
                        symbolismText = documentSnapshot.getString("symbolismText");

                        // Health Percentage Fallbacks
                        Long hPercent = documentSnapshot.getLong("healthPercentage");
                        if (hPercent == null) {
                            Double dVal = documentSnapshot.getDouble("healthPercentage");
                            if (dVal != null) hPercent = dVal.longValue();
                        }
                        if (hPercent != null) healthPercentage = hPercent.intValue();

                        // Match Confidence Fallbacks
                        Long mPercent = documentSnapshot.getLong("matchConfidence");
                        if (mPercent == null) {
                            mPercent = documentSnapshot.getLong("matchConfidencePercentage");
                        }
                        if (mPercent == null) {
                            Double dVal = documentSnapshot.getDouble("matchConfidence");
                            if (dVal != null) mPercent = dVal.longValue();
                        }
                        if (mPercent != null) matchConfidencePercentage = mPercent.intValue();

                        Boolean artificial = documentSnapshot.getBoolean("isArtificial");
                        if (artificial != null) isArtificial = artificial;

                        // Map Coordinates Parsing
                        mapLocations.clear();
                        List<Map<String, Object>> coordsList = (List<Map<String, Object>>) documentSnapshot.get("distribution_coordinates");
                        if (coordsList != null) {
                            for (Map<String, Object> cMap : coordsList) {
                                try {
                                    double lat = 0;
                                    if (cMap.get("latitude") instanceof Number) {
                                        lat = ((Number) cMap.get("latitude")).doubleValue();
                                    }
                                    double lng = 0;
                                    if (cMap.get("longitude") instanceof Number) {
                                        lng = ((Number) cMap.get("longitude")).doubleValue();
                                    }
                                    String title = (String) cMap.get("title");
                                    String snippet = (String) cMap.get("snippet");
                                    String dType = (String) cMap.get("distribution_type");
                                    mapLocations.add(new MapLocation(lat, lng, title, snippet, dType));
                                } catch (Exception ex) {
                                    Log.w(TAG, "Error parsing coordinate from list", ex);
                                }
                            }
                        }

                        // Base64 Image Decoding
                        String base64Image = documentSnapshot.getString("imageBase64");
                        plantBase64Image = base64Image;
                        if (base64Image != null && !base64Image.isEmpty()) {
                            try {
                                byte[] decodedBytes = Base64.decode(base64Image, Base64.DEFAULT);
                                scannedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                            } catch (Exception ex) {
                                Log.e(TAG, "Error decoding base64 image", ex);
                            }
                        }

                        updateUI();
                        updateMapMarkers();
                    }
                });
    }

    private void updateUI() {
        if (tvPlantTitle != null) {
            tvPlantTitle.setText(plantName != null ? plantName : "");
        }
        if (tvScientificName != null) {
            tvScientificName.setText(scientificName != null ? scientificName : "");
        }
        if (tvHealthState != null) {
            tvHealthState.setText(healthStatus != null ? "Status: " + healthStatus : "");
        }
        if (tvAliases != null) {
            tvAliases.setText(aliases != null ? "Also known as: " + aliases : "");
        }

        // Update Leaf Colors UI
        if (tvLeafColor != null) {
            if (!leafColorsList.isEmpty()) {
                tvLeafColor.setText(TextUtils.join(", ", leafColorsList));
            } else {
                tvLeafColor.setText("N/A");
            }
        }

        // Health Score Badge
        if (tvHealthScore != null) {
            tvHealthScore.setText("Health: " + healthPercentage + "%");
            tvHealthScore.setVisibility(View.VISIBLE);
        }

        // Match Confidence Badge
        if (tvMatchConfidence != null) {
            tvMatchConfidence.setText(matchConfidencePercentage + "% Match Confidence");
            tvMatchConfidence.setVisibility(View.VISIBLE);
        }

        // Images setup
        if (scannedBitmap != null) {
            if (ivPlantMain != null) ivPlantMain.setImageBitmap(scannedBitmap);
            if (ivPlantMatch1 != null) ivPlantMatch1.setImageBitmap(scannedBitmap);
            if (ivPlantMatch2 != null) ivPlantMatch2.setImageBitmap(scannedBitmap);
        }
    }

    private void updateMapMarkers() {
        if (googleMap == null) return;
        googleMap.clear();

        for (MapLocation loc : mapLocations) {
            LatLng position = new LatLng(loc.lat, loc.lng);
            googleMap.addMarker(new MarkerOptions()
                    .position(position)
                    .title(loc.title != null ? loc.title : "Plant Distribution")
                    .snippet(loc.snippet));
        }

        if (!mapLocations.isEmpty()) {
            LatLng firstLoc = new LatLng(mapLocations.get(0).lat, mapLocations.get(0).lng);
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(firstLoc, 4f));
        }
    }

    public String getPlantId() {
        return plantId;
    }

    private void updateTabUI(int position) {
        if (btnPlantInfo != null) {
            btnPlantInfo.setBackgroundResource(R.drawable.bg_inactive_pill);
            btnPlantInfo.setTextColor(Color.parseColor("#1E2420"));
        }
        if (btnSchedule != null) {
            btnSchedule.setBackgroundResource(R.drawable.bg_inactive_pill);
            btnSchedule.setTextColor(Color.parseColor("#1E2420"));
        }
        if (btnHistory != null) {
            btnHistory.setBackgroundResource(R.drawable.bg_inactive_pill);
            btnHistory.setTextColor(Color.parseColor("#1E2420"));;
        }

        switch (position) {
            case 0:
                if (btnPlantInfo != null) {
                    btnPlantInfo.setBackgroundResource(R.drawable.bg_pill_active);
                    btnPlantInfo.setTextColor(getResources().getColor(R.color.white, null));
                }
                break;
            case 1:
                if (btnSchedule != null) {
                    btnSchedule.setBackgroundResource(R.drawable.bg_pill_active);
                    btnSchedule.setTextColor(getResources().getColor(R.color.white, null));
                }
                break;
            case 2:
                if (btnHistory != null) {
                    btnHistory.setBackgroundResource(R.drawable.bg_pill_active);
                    btnHistory.setTextColor(getResources().getColor(R.color.white, null));
                }
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    protected void onPause() {
        if (mapView != null) mapView.onPause();
        super.onPause();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) mapView.onLowMemory();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        if (mapView != null) mapView.onDestroy();
        if (plantListener != null) {
            plantListener.remove();
        }
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}