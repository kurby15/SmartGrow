package com.example.smartgrow.plants;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.smartgrow.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
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

public class PlantDetailsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "PlantDetailsActivity";
    public static Bitmap tempScannedBitmap = null;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // TTS Pronunciation Engine
    private TextToSpeech textToSpeech;
    private boolean isTtsReady = false;

    // UI Elements - Images & Map
    private ImageView ivPlantMain, ivPlantMatch1, ivPlantMatch2, ivHealthPreview, ibSpeaker;
    private MapView mapView;
    private GoogleMap googleMap;

    // UI Elements - Top Info
    private TextView tvPlantTitle, tvScientificName, tvHealthState, tvAliases;

    // UI Elements - Basic Info
    private TextView tvPetToxicity, tvWeedPotential, tvDistribution, tvPlantType, tvLifespan;

    // UI Elements - Common Problems Container
    private LinearLayout layoutCommonProblemsContainer;

    // UI Elements - Characteristics
    private TextView tvUltimateHeight, tvUltimateSpread, tvLeafType, tvPlantingTime;
    private View viewLeafColor;

    // UI Elements - Care Conditions
    private TextView tvTemp, tvHardiness;

    // UI Elements - Additional Dynamic Text Sections
    private TextView tvUsesContent, tvAdaptationContent, tvEcologicalContent, tvHistoryContent, tvNameStoryContent, tvSymbolismContent;

    // UI Elements - Buttons
    private MaterialButton btnSaveToGardenBottom, btnInlineSave, btnViewFrequency;
    private ImageButton ibBack, ibCameraTop;

    // Extracted Data Fields
    private String plantName = "Unknown Plant";
    private String scientificName = "N/A";
    private String healthStatus = "Healthy";
    private String aliases = "N/A";
    private String petToxicity = "Non-toxic";
    private String weedPotential = "Low";
    private String distribution = "N/A";
    private String plantType = "Unknown";
    private String lifespan = "N/A";

    // Coordinates (Default: Philippines region)
    private double mapLat = 14.5995;
    private double mapLng = 120.9842;

    // Structure for Distribution/Origin Pinpoints
    private static class MapLocation {
        double lat;
        double lng;
        String title;
        String snippet;
        boolean isNativeOrigin;

        MapLocation(double lat, double lng, String title, String snippet, boolean isNativeOrigin) {
            this.lat = lat;
            this.lng = lng;
            this.title = title;
            this.snippet = snippet;
            this.isNativeOrigin = isNativeOrigin;
        }
    }
    private List<MapLocation> mapLocations = new ArrayList<>();

    // Dynamic Content Fields
    private String ultimateHeight = "N/A";
    private String ultimateSpread = "N/A";
    private String leafType = "N/A";
    private String plantingTime = "N/A";
    private String temperatureRange = "N/A";
    private String hardinessZones = "N/A";
    private String usesText = "N/A";
    private String adaptationText = "N/A";
    private String ecologicalText = "N/A";
    private String historyText = "N/A";
    private String nameStoryText = "N/A";
    private String symbolismText = "N/A";
    private String leafColorHex = "#FFFFFF";

    private JSONArray commonProblemsArray = null;
    private Bitmap scannedBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plant_details);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        initTextToSpeech();

        if (mapView != null) {
            mapView.onCreate(savedInstanceState);
        }

        if (tempScannedBitmap != null) {
            this.scannedBitmap = getResizedBitmap(tempScannedBitmap, 800);
            tempScannedBitmap = null;

            if (ivPlantMain != null) ivPlantMain.setImageBitmap(scannedBitmap);
            if (ivHealthPreview != null) ivHealthPreview.setImageBitmap(scannedBitmap);
            if (ivPlantMatch1 != null) ivPlantMatch1.setImageBitmap(scannedBitmap);
            if (ivPlantMatch2 != null) ivPlantMatch2.setImageBitmap(scannedBitmap);
        }

        setupClickableImages();
        parseIntentData();

        if (ibBack != null) ibBack.setOnClickListener(v -> finish());
        if (ibCameraTop != null) ibCameraTop.setOnClickListener(v -> finish());
        if (ibSpeaker != null) ibSpeaker.setOnClickListener(v -> speakPlantPronunciation());
        if (btnSaveToGardenBottom != null) btnSaveToGardenBottom.setOnClickListener(v -> savePlantToDiary());
        if (btnInlineSave != null) btnInlineSave.setOnClickListener(v -> savePlantToDiary());
    }

    private void initViews() {
        ivPlantMain = findViewById(R.id.iv_plant_main);
        ivPlantMatch1 = findViewById(R.id.iv_plant_match_1);
        ivPlantMatch2 = findViewById(R.id.iv_plant_match_2);
        ivHealthPreview = findViewById(R.id.iv_health_preview);

        mapView = findViewById(R.id.map_view);

        tvPlantTitle = findViewById(R.id.tv_plant_title);
        tvScientificName = findViewById(R.id.tv_scientific_name);
        tvHealthState = findViewById(R.id.tv_health_state);
        tvAliases = findViewById(R.id.tv_aliases);
        tvPetToxicity = findViewById(R.id.tv_pet_toxicity);
        tvWeedPotential = findViewById(R.id.tv_weed_potential);
        tvDistribution = findViewById(R.id.tv_distribution);
        tvPlantType = findViewById(R.id.tv_plant_type);
        tvLifespan = findViewById(R.id.tv_lifespan);

        layoutCommonProblemsContainer = findViewById(R.id.layout_common_problems_container);

        tvUltimateHeight = findViewById(R.id.tv_ultimate_height);
        tvUltimateSpread = findViewById(R.id.tv_ultimate_spread);
        tvLeafType = findViewById(R.id.tv_leaf_type);
        tvPlantingTime = findViewById(R.id.tv_planting_time);
        viewLeafColor = findViewById(R.id.view_leaf_color);

        tvTemp = findViewById(R.id.tv_temp);
        tvHardiness = findViewById(R.id.tv_hardiness);

        tvUsesContent = findViewById(R.id.tv_uses_content);
        tvAdaptationContent = findViewById(R.id.tv_adaptation_content);
        tvEcologicalContent = findViewById(R.id.tv_ecological_content);
        tvHistoryContent = findViewById(R.id.tv_history_content);
        tvNameStoryContent = findViewById(R.id.tv_namestory_content);
        tvSymbolismContent = findViewById(R.id.tv_symbolism_content);

        btnSaveToGardenBottom = findViewById(R.id.btn_save_to_garden_bottom);
        btnInlineSave = findViewById(R.id.btn_inline_save);
        btnViewFrequency = findViewById(R.id.btn_view_frequency);
        ibBack = findViewById(R.id.ib_back);
        ibCameraTop = findViewById(R.id.ib_camera_top);
        ibSpeaker = findViewById(R.id.ib_speaker);
    }

    private void initTextToSpeech() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.US);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "TTS Language not supported.");
                } else {
                    isTtsReady = true;
                }
            } else {
                Log.e(TAG, "TTS Initialization failed.");
            }
        });
    }

    private void speakPlantPronunciation() {
        if (isTtsReady && textToSpeech != null) {
            String speechText = plantName;
            if (scientificName != null && !scientificName.equals("N/A") && !scientificName.isEmpty()) {
                speechText += ". " + scientificName;
            }
            textToSpeech.speak(speechText, TextToSpeech.QUEUE_FLUSH, null, "PlantPronunciation");
        } else {
            Toast.makeText(this, "Voice engine is initializing...", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupClickableImages() {
        View.OnClickListener bitmapClickListener = v -> {
            if (scannedBitmap != null) {
                showBitmapPreviewDialog(scannedBitmap);
            }
        };

        if (ivPlantMain != null) ivPlantMain.setOnClickListener(bitmapClickListener);
        if (ivHealthPreview != null) ivHealthPreview.setOnClickListener(bitmapClickListener);
        if (ivPlantMatch1 != null) ivPlantMatch1.setOnClickListener(bitmapClickListener);
        if (ivPlantMatch2 != null) ivPlantMatch2.setOnClickListener(bitmapClickListener);
    }

    private void showBitmapPreviewDialog(Bitmap bitmap) {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        ImageView fullImageView = new ImageView(this);
        fullImageView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        fullImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        fullImageView.setImageBitmap(bitmap);
        fullImageView.setBackgroundColor(Color.BLACK);

        fullImageView.setOnClickListener(v -> dialog.dismiss());
        dialog.setContentView(fullImageView);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        }
        dialog.show();
    }

    private void showUrlPreviewDialog(String imageUrl) {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        ImageView fullImageView = new ImageView(this);
        fullImageView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        fullImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        fullImageView.setBackgroundColor(Color.BLACK);

        Glide.with(this)
                .load(imageUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(fullImageView);

        fullImageView.setOnClickListener(v -> dialog.dismiss());
        dialog.setContentView(fullImageView);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        }
        dialog.show();
    }

    private void parseIntentData() {
        if (getIntent() == null) return;

        String rawJson = getIntent().getStringExtra("raw_ai_json");
        if (rawJson != null && !rawJson.trim().isEmpty()) {
            String cleanJson = sanitizeJsonString(rawJson);
            if (cleanJson.startsWith("{")) {
                populateDataFromJson(cleanJson);
            } else {
                populateDataFromText(rawJson);
            }
            return;
        }

        if (getIntent().hasExtra("plant_name")) plantName = getIntent().getStringExtra("plant_name");
        if (getIntent().hasExtra("scientific_name")) scientificName = getIntent().getStringExtra("scientific_name");
        if (getIntent().hasExtra("health_status")) healthStatus = getIntent().getStringExtra("health_status");

        updateUI();
    }

    private String sanitizeJsonString(String raw) {
        if (raw == null) return "";
        String cleaned = raw.trim();

        if (cleaned.startsWith("```")) {
            int firstLineBreak = cleaned.indexOf("\n");
            if (firstLineBreak != -1) {
                cleaned = cleaned.substring(firstLineBreak + 1);
            } else {
                cleaned = cleaned.replaceFirst("^```[a-zA-Z]*", "");
            }
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.lastIndexOf("```"));
            }
        }
        return cleaned.trim();
    }

    private void populateDataFromJson(String jsonString) {
        try {
            JSONObject root = new JSONObject(jsonString);

            if (root.has("is_plant") && !root.getBoolean("is_plant")) {
                Toast.makeText(this, "The uploaded image is not recognized as a plant.", Toast.LENGTH_LONG).show();
                plantName = "Not a Plant";
                updateUI();
                return;
            }

            JSONObject profile = root.optJSONObject("plant_profile");
            if (profile != null) {
                String fullTitle = profile.optString("name", plantName);

                if (fullTitle.contains("(") && fullTitle.contains(")")) {
                    int open = fullTitle.indexOf("(");
                    int close = fullTitle.indexOf(")");
                    if (open < close) {
                        plantName = fullTitle.substring(0, open).trim();
                        scientificName = fullTitle.substring(open + 1, close).trim();
                    } else {
                        plantName = fullTitle;
                    }
                } else {
                    plantName = fullTitle;
                }

                if (profile.has("scientific_name") && !profile.optString("scientific_name").isEmpty()) {
                    scientificName = profile.optString("scientific_name");
                }

                aliases = profile.optString("philippine_name", profile.optString("aliases", aliases));
                distribution = profile.optString("distribution_text", profile.optString("origin", distribution));

                mapLocations.clear();
                JSONArray locArray = profile.optJSONArray("distribution_coordinates");
                if (locArray == null) locArray = profile.optJSONArray("locations");
                if (locArray == null) locArray = profile.optJSONArray("coordinates");

                if (locArray != null && locArray.length() > 0) {
                    for (int i = 0; i < locArray.length(); i++) {
                        JSONObject locObj = locArray.optJSONObject(i);
                        if (locObj != null) {
                            double lat = locObj.optDouble("latitude", locObj.optDouble("lat", mapLat));
                            double lng = locObj.optDouble("longitude", locObj.optDouble("lng", mapLng));
                            String title = locObj.optString("title", locObj.optString("region", "Distribution Point " + (i + 1)));
                            String snippet = locObj.optString("snippet", locObj.optString("description", "Plant Distribution Region"));
                            boolean isNative = locObj.optBoolean("is_native", i == 0);
                            mapLocations.add(new MapLocation(lat, lng, title, snippet, isNative));
                        }
                    }
                }

                if (mapLocations.isEmpty()) {
                    if (profile.has("latitude") && profile.has("longitude")) {
                        mapLat = profile.optDouble("latitude", mapLat);
                        mapLng = profile.optDouble("longitude", mapLng);
                    }
                    mapLocations.add(new MapLocation(mapLat, mapLng, plantName + " Origin", distribution, true));
                }

                plantType = profile.optString("type", profile.optString("plant_type", plantType));
                petToxicity = profile.optString("pet_toxicity", petToxicity);
                weedPotential = profile.optString("weed_potential", weedPotential);
                lifespan = profile.optString("lifespan", lifespan);
            }

            commonProblemsArray = root.optJSONArray("common_problems");

            JSONObject characteristics = root.optJSONObject("characteristics");
            if (characteristics != null) {
                ultimateHeight = characteristics.optString("ultimate_height", ultimateHeight);
                ultimateSpread = characteristics.optString("ultimate_spread", ultimateSpread);
                leafType = characteristics.optString("leaf_type", leafType);
                plantingTime = characteristics.optString("planting_time", plantingTime);
                leafColorHex = characteristics.optString("leaf_color_hex", leafColorHex);
            }

            JSONObject ecosystem = root.optJSONObject("ecosystem");
            if (ecosystem != null) {
                temperatureRange = ecosystem.optString("temp_range", temperatureRange);
                hardinessZones = ecosystem.optString("hardiness_zones", hardinessZones);
            }

            JSONObject extraDetails = root.optJSONObject("extra_details");
            if (extraDetails != null) {
                usesText = extraDetails.optString("uses", usesText);
                adaptationText = extraDetails.optString("adaptation_strategies", adaptationText);
                ecologicalText = extraDetails.optString("ecological_application", ecologicalText);
                historyText = extraDetails.optString("history_and_legends", historyText);
                nameStoryText = extraDetails.optString("name_story", nameStoryText);
                symbolismText = extraDetails.optString("symbolism", symbolismText);
            }

            JSONObject health = root.optJSONObject("health_scanner");
            if (health != null) {
                healthStatus = health.optString("status", healthStatus);
            }

            if (root.optBoolean("is_artificial", false)) {
                healthStatus = "Artificial / Plastic Plant";
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to parse JSON in PlantDetailsActivity", e);
        }
        updateUI();
    }

    private void populateDataFromText(String text) {
        if (text == null) return;

        if (mapLocations.isEmpty()) {
            mapLocations.add(new MapLocation(mapLat, mapLng, plantName + " Distribution", distribution, true));
        }

        String[] lines = text.split("\n");
        for (String line : lines) {
            String lower = line.toLowerCase();
            if (lower.contains("name:")) {
                plantName = extractValue(line);
            } else if (lower.contains("scientific name:")) {
                scientificName = extractValue(line);
            } else if (lower.contains("health:") || lower.contains("condition:") || lower.contains("status:")) {
                healthStatus = extractValue(line);
            } else if (lower.contains("local name:") || lower.contains("aliases:")) {
                aliases = extractValue(line);
            } else if (lower.contains("toxicity:")) {
                petToxicity = extractValue(line);
            } else if (lower.contains("weed potential:")) {
                weedPotential = extractValue(line);
            } else if (lower.contains("native origin:") || lower.contains("distribution:")) {
                distribution = extractValue(line);
            } else if (lower.contains("type:")) {
                plantType = extractValue(line);
            } else if (lower.contains("lifespan:")) {
                lifespan = extractValue(line);
            } else if (lower.contains("uses:")) {
                usesText = extractValue(line);
            } else if (lower.contains("symbolism:")) {
                symbolismText = extractValue(line);
            }
        }
        updateUI();
    }

    private String extractValue(String line) {
        if (line != null && line.contains(":")) {
            return line.substring(line.indexOf(":") + 1).trim();
        }
        return line != null ? line.trim() : "";
    }

    private void updateUI() {
        if (isDestroyed() || isFinishing()) return;

        if (tvPlantTitle != null) tvPlantTitle.setText(plantName);
        if (tvScientificName != null) tvScientificName.setText(scientificName);
        if (tvHealthState != null) tvHealthState.setText(healthStatus);
        if (tvAliases != null) tvAliases.setText("Also known as: " + aliases);
        if (tvPetToxicity != null) tvPetToxicity.setText(petToxicity);
        if (tvWeedPotential != null) tvWeedPotential.setText(weedPotential);
        if (tvDistribution != null) tvDistribution.setText(distribution);
        if (tvPlantType != null) tvPlantType.setText(plantType);
        if (tvLifespan != null) tvLifespan.setText(lifespan);

        setupMapView();
        renderCommonProblems();

        if (tvUltimateHeight != null) tvUltimateHeight.setText(ultimateHeight);
        if (tvUltimateSpread != null) tvUltimateSpread.setText(ultimateSpread);
        if (tvLeafType != null) tvLeafType.setText(leafType);
        if (tvPlantingTime != null) tvPlantingTime.setText(plantingTime);

        if (viewLeafColor != null && leafColorHex != null && leafColorHex.startsWith("#")) {
            try {
                viewLeafColor.setBackgroundColor(Color.parseColor(leafColorHex));
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Invalid color hex code: " + leafColorHex);
            }
        }

        if (tvTemp != null) tvTemp.setText("Temperature: " + temperatureRange);
        if (tvHardiness != null) tvHardiness.setText("Hardiness Zones: " + hardinessZones);

        if (tvUsesContent != null) tvUsesContent.setText(usesText);
        if (tvAdaptationContent != null) tvAdaptationContent.setText(adaptationText);
        if (tvEcologicalContent != null) tvEcologicalContent.setText(ecologicalText);
        if (tvHistoryContent != null) tvHistoryContent.setText(historyText);
        if (tvNameStoryContent != null) tvNameStoryContent.setText(nameStoryText);
        if (tvSymbolismContent != null) tvSymbolismContent.setText(symbolismText);
    }

    private void setupMapView() {
        if (mapView != null) {
            mapView.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;
        googleMap.clear();

        googleMap.setOnMapClickListener(latLng -> showFullScreenMapDialog());

        if (mapLocations != null && !mapLocations.isEmpty()) {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();

            for (MapLocation loc : mapLocations) {
                LatLng latLng = new LatLng(loc.lat, loc.lng);
                MarkerOptions markerOptions = new MarkerOptions()
                        .position(latLng)
                        .title(loc.title);

                if (loc.snippet != null && !loc.snippet.isEmpty()) {
                    markerOptions.snippet(loc.snippet);
                }

                if (loc.isNativeOrigin) {
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                } else {
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
                }

                googleMap.addMarker(markerOptions);
                builder.include(latLng);
            }

            if (mapLocations.size() == 1) {
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mapLocations.get(0).lat, mapLocations.get(0).lng), 5.0f));
            } else {
                try {
                    LatLngBounds bounds = builder.build();
                    int padding = 120;
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
                } catch (Exception e) {
                    Log.e(TAG, "Error centering bounds for map markers", e);
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mapLocations.get(0).lat, mapLocations.get(0).lng), 4.0f));
                }
            }
        }
    }

    private void showFullScreenMapDialog() {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        FrameLayout container = new FrameLayout(this);
        container.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        MapView fullMapView = new MapView(this);
        fullMapView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        container.addView(fullMapView);

        ImageButton closeBtn = new ImageButton(this);
        FrameLayout.LayoutParams btnParams = new FrameLayout.LayoutParams(
                dpToPx(48),
                dpToPx(48)
        );
        btnParams.gravity = Gravity.TOP | Gravity.END;
        btnParams.setMargins(0, dpToPx(24), dpToPx(24), 0);
        closeBtn.setLayoutParams(btnParams);
        closeBtn.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        closeBtn.setBackgroundColor(Color.parseColor("#80000000"));
        closeBtn.setColorFilter(Color.WHITE);
        closeBtn.setOnClickListener(v -> dialog.dismiss());
        container.addView(closeBtn);

        dialog.setContentView(container);

        fullMapView.onCreate(null);
        fullMapView.onStart();
        fullMapView.onResume();

        fullMapView.getMapAsync(fullMap -> {
            fullMap.clear();
            if (mapLocations != null && !mapLocations.isEmpty()) {
                LatLngBounds.Builder builder = new LatLngBounds.Builder();

                for (MapLocation loc : mapLocations) {
                    LatLng latLng = new LatLng(loc.lat, loc.lng);
                    MarkerOptions markerOptions = new MarkerOptions()
                            .position(latLng)
                            .title(loc.title);

                    if (loc.snippet != null && !loc.snippet.isEmpty()) {
                        markerOptions.snippet(loc.snippet);
                    }

                    if (loc.isNativeOrigin) {
                        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                    } else {
                        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
                    }

                    fullMap.addMarker(markerOptions);
                    builder.include(latLng);
                }

                if (mapLocations.size() == 1) {
                    fullMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mapLocations.get(0).lat, mapLocations.get(0).lng), 5.0f));
                } else {
                    try {
                        LatLngBounds bounds = builder.build();
                        fullMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, dpToPx(80)));
                    } catch (Exception e) {
                        fullMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mapLocations.get(0).lat, mapLocations.get(0).lng), 4.0f));
                    }
                }
            }
        });

        dialog.setOnDismissListener(d -> {
            fullMapView.onPause();
            fullMapView.onDestroy();
        });

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        }
        dialog.show();
    }

    private void renderCommonProblems() {
        if (layoutCommonProblemsContainer == null) return;
        layoutCommonProblemsContainer.removeAllViews();

        if (commonProblemsArray != null && commonProblemsArray.length() > 0) {
            for (int i = 0; i < commonProblemsArray.length(); i++) {
                try {
                    JSONObject problem = commonProblemsArray.getJSONObject(i);
                    String problemTitle = problem.optString("title", "Common Issue");
                    String problemImageUrl = problem.optString("image_url", "");

                    MaterialCardView card = new MaterialCardView(this);
                    LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                            dpToPx(160),
                            dpToPx(160)
                    );
                    cardParams.setMargins(0, 0, dpToPx(12), 0);
                    card.setLayoutParams(cardParams);
                    card.setRadius(dpToPx(16));
                    card.setCardElevation(dpToPx(2));
                    card.setCardBackgroundColor(Color.parseColor("#262626"));
                    card.setStrokeWidth(0);

                    LinearLayout innerLayout = new LinearLayout(this);
                    innerLayout.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.MATCH_PARENT
                    ));
                    innerLayout.setOrientation(LinearLayout.VERTICAL);

                    ImageView problemImageView = new ImageView(this);
                    LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            dpToPx(100)
                    );
                    problemImageView.setLayoutParams(imgParams);
                    problemImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

                    final String targetProblemImg = (problemImageUrl != null && problemImageUrl.startsWith("http"))
                            ? problemImageUrl
                            : "https://loremflickr.com/320/240/" + Uri.encode(scientificName + " " + problemTitle);

                    Glide.with(getApplicationContext())
                            .load(targetProblemImg)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(problemImageView);

                    card.setOnClickListener(v -> showUrlPreviewDialog(targetProblemImg));

                    TextView titleTextView = new TextView(this);
                    LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.MATCH_PARENT
                    );
                    titleTextView.setLayoutParams(textParams);
                    titleTextView.setText(problemTitle);
                    titleTextView.setTextColor(Color.WHITE);
                    titleTextView.setTextSize(13);
                    titleTextView.setPadding(dpToPx(10), dpToPx(6), dpToPx(10), dpToPx(6));

                    innerLayout.addView(problemImageView);
                    innerLayout.addView(titleTextView);
                    card.addView(innerLayout);

                    layoutCommonProblemsContainer.addView(card);
                } catch (Exception e) {
                    Log.e(TAG, "Error adding common problem card: ", e);
                }
            }
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void savePlantToDiary() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please login to save to your garden.", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        String docId = String.valueOf(System.currentTimeMillis());

        Map<String, Object> diaryEntry = new HashMap<>();
        diaryEntry.put("id", docId);
        diaryEntry.put("userId", userId);
        diaryEntry.put("plantName", plantName);
        diaryEntry.put("scientificName", scientificName);
        diaryEntry.put("healthStatus", healthStatus);
        diaryEntry.put("aliases", aliases);
        diaryEntry.put("petToxicity", petToxicity);
        diaryEntry.put("weedPotential", weedPotential);
        diaryEntry.put("distribution", distribution);
        diaryEntry.put("plantType", plantType);
        diaryEntry.put("lifespan", lifespan);
        diaryEntry.put("timestamp", System.currentTimeMillis());

        if (scannedBitmap != null) {
            diaryEntry.put("imageBase64", encodeBitmapToBase64(scannedBitmap));
        }

        db.collection("diary")
                .document(docId)
                .set(diaryEntry)
                .addOnSuccessListener(aVoid -> {
                    if (!isFinishing() && !isDestroyed()) {
                        Toast.makeText(this, "Added to My Garden Diary!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isFinishing() && !isDestroyed()) {
                        Toast.makeText(this, "Error saving: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String encodeBitmapToBase64(@NonNull Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, baos);
        return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
    }

    private Bitmap getResizedBitmap(@NonNull Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }

    // Lifecycle Forwarding for Google MapView & TTS

    @Override
    protected void onStart() {
        super.onStart();
        if (mapView != null) mapView.onStart();
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
    protected void onStop() {
        if (mapView != null) mapView.onStop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (mapView != null) mapView.onDestroy();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) mapView.onLowMemory();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) mapView.onSaveInstanceState(outState);
    }
}