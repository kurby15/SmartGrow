package com.example.smartgrow.plants;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.smartgrow.R;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PlantInfoFragment extends Fragment {

    private static final String TAG = "PlantInfoFragment";

    private TextView tvPetToxicity, tvWeedPotential, tvDistribution, tvHabitat, tvPlantType, tvLifespan;
    private TextView tvUltimateHeight, tvUltimateSpread, tvLeafType, tvPlantingTime;
    private TextView tvTemp, tvHardiness, tvSunlight, tvSoil;
    private TextView tvPruningContent, tvPropagationContent, tvRepottingContent;
    private TextView tvUsesContent, tvAdaptationContent, tvEcologicalContent, tvHistoryContent, tvNamestoryContent, tvSymbolismContent;

    private LinearLayout layoutDistributionClick, layoutLeafColorsContainer, layoutCommonProblemsContainer;
    private MapView mapView;
    private GoogleMap googleMap;
    private RecyclerView rvCommonPests;

    private FirebaseFirestore db;

    // Data members
    private JSONArray commonProblemsArray = null;
    private List<PestModel> pestList = new ArrayList<>();
    private String scientificName = "";

    // Store coordinates for the map
    private List<MapLocation> mapLocations = new ArrayList<>();

    private static class MapLocation {
        double lat;
        double lng;
        String title;
        String snippet;

        MapLocation(double lat, double lng, String title, String snippet) {
            this.lat = lat;
            this.lng = lng;
            this.title = title;
            this.snippet = snippet;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_plant_info, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        // 1. Initialize all TextViews
        tvPetToxicity = view.findViewById(R.id.tv_pet_toxicity);
        tvWeedPotential = view.findViewById(R.id.tv_weed_potential);
        tvDistribution = view.findViewById(R.id.tv_distribution);
        tvHabitat = view.findViewById(R.id.tv_habitat);
        tvPlantType = view.findViewById(R.id.tv_plant_type);
        tvLifespan = view.findViewById(R.id.tv_lifespan);

        tvUltimateHeight = view.findViewById(R.id.tv_ultimate_height);
        tvUltimateSpread = view.findViewById(R.id.tv_ultimate_spread);
        tvLeafType = view.findViewById(R.id.tv_leaf_type);
        tvPlantingTime = view.findViewById(R.id.tv_planting_time);

        tvTemp = view.findViewById(R.id.tv_temp);
        tvHardiness = view.findViewById(R.id.tv_hardiness);
        tvSunlight = view.findViewById(R.id.tv_sunlight);
        tvSoil = view.findViewById(R.id.tv_soil);

        tvPruningContent = view.findViewById(R.id.tv_pruning_content);
        tvPropagationContent = view.findViewById(R.id.tv_propagation_content);
        tvRepottingContent = view.findViewById(R.id.tv_repotting_content);

        tvUsesContent = view.findViewById(R.id.tv_uses_content);
        tvAdaptationContent = view.findViewById(R.id.tv_adaptation_content);
        tvEcologicalContent = view.findViewById(R.id.tv_ecological_content);
        tvHistoryContent = view.findViewById(R.id.tv_history_content);
        tvNamestoryContent = view.findViewById(R.id.tv_namestory_content);
        tvSymbolismContent = view.findViewById(R.id.tv_symbolism_content);

        layoutDistributionClick = view.findViewById(R.id.layout_distribution_click);
        layoutLeafColorsContainer = view.findViewById(R.id.layout_leaf_colors_container);
        layoutCommonProblemsContainer = view.findViewById(R.id.layout_common_problems_container);

        // 2. MapView Initialization
        mapView = view.findViewById(R.id.map_view);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(map -> {
            googleMap = map;
            updateMapMarkers();
        });

        rvCommonPests = view.findViewById(R.id.rv_common_pests);
        if (rvCommonPests != null) {
            rvCommonPests.setLayoutManager(new LinearLayoutManager(requireContext()));
        }

        if (getActivity() instanceof PlantDiaryActivity) {
            String plantId = ((PlantDiaryActivity) getActivity()).getPlantId();
            if (plantId != null && !plantId.isEmpty()) {
                loadPlantDataFromFirestore(plantId);
            }
        }
    }

    private void loadPlantDataFromFirestore(String plantId) {
        db.collection("diary").document(plantId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && getView() != null) {
                        scientificName = documentSnapshot.getString("scientificName");

                        // Basic Info & Care
                        if (tvPetToxicity != null) tvPetToxicity.setText(documentSnapshot.getString("petToxicity"));
                        if (tvWeedPotential != null) tvWeedPotential.setText(documentSnapshot.getString("weedPotential"));
                        if (tvDistribution != null) tvDistribution.setText(documentSnapshot.getString("distribution"));
                        if (tvHabitat != null) tvHabitat.setText(documentSnapshot.getString("habitat"));
                        if (tvPlantType != null) tvPlantType.setText(documentSnapshot.getString("plantType"));
                        if (tvLifespan != null) tvLifespan.setText(documentSnapshot.getString("lifespan"));

                        // Characteristics
                        if (tvUltimateHeight != null) tvUltimateHeight.setText(documentSnapshot.getString("ultimateHeight"));
                        if (tvUltimateSpread != null) tvUltimateSpread.setText(documentSnapshot.getString("ultimateSpread"));
                        if (tvLeafType != null) tvLeafType.setText(documentSnapshot.getString("leafType"));
                        if (tvPlantingTime != null) tvPlantingTime.setText(documentSnapshot.getString("plantingTime"));

                        // 3. Leaf Colors
                        List<String> colorsList = (List<String>) documentSnapshot.get("leaf_colors");
                        if (colorsList == null) colorsList = (List<String>) documentSnapshot.get("leafColors");
                        renderLeafColorSwatches(colorsList);

                        // 4. Map Coordinates
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
                                    mapLocations.add(new MapLocation(lat, lng, title, snippet));
                                } catch (Exception e) {
                                    Log.w(TAG, "Error parsing coordinate", e);
                                }
                            }
                        }
                        updateMapMarkers();

                        // Common Problems
                        String cpJson = documentSnapshot.getString("commonProblemsJson");
                        if (cpJson != null && !cpJson.isEmpty()) {
                            try {
                                commonProblemsArray = new JSONArray(cpJson);
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing commonProblemsJson", e);
                            }
                        } else {
                            List<Map<String, Object>> cpList = (List<Map<String, Object>>) documentSnapshot.get("common_problems_list");
                            if (cpList != null) {
                                commonProblemsArray = new JSONArray();
                                for (Map<String, Object> pMap : cpList) {
                                    commonProblemsArray.put(new JSONObject(pMap));
                                }
                            }
                        }
                        renderCommonProblems();

                        // Common Pests
                        pestList.clear();
                        List<Map<String, String>> pestData = (List<Map<String, String>>) documentSnapshot.get("common_pests");
                        if (pestData != null) {
                            for (Map<String, String> pMap : pestData) {
                                pestList.add(new PestModel(pMap.get("name"), pMap.get("description"), ""));
                            }
                        }
                        renderCommonPests();

                        // Care Conditions
                        if (tvTemp != null) {
                            String tempRange = documentSnapshot.getString("temperatureRange");
                            if (tempRange != null && !tempRange.isEmpty()) {
                                tvTemp.setText("Temperature: " + tempRange);
                            } else {
                                tvTemp.setText("Temperature: N/A");
                            }
                        }
                        if (tvHardiness != null) {
                            String hardiness = documentSnapshot.getString("hardinessZones");
                            if (hardiness != null && !hardiness.isEmpty()) {
                                tvHardiness.setText("Hardiness: " + hardiness);
                            }
                        }
                        if (tvSunlight != null) tvSunlight.setText(documentSnapshot.getString("sunlight"));
                        if (tvSoil != null) tvSoil.setText(documentSnapshot.getString("soil"));

                        // How-tos
                        if (tvPruningContent != null) tvPruningContent.setText(documentSnapshot.getString("pruning"));
                        if (tvPropagationContent != null) tvPropagationContent.setText(documentSnapshot.getString("propagation"));
                        if (tvRepottingContent != null) tvRepottingContent.setText(documentSnapshot.getString("repotting"));

                        // Additional Dynamic Sections
                        if (tvUsesContent != null) tvUsesContent.setText(documentSnapshot.getString("usesText"));
                        if (tvAdaptationContent != null) tvAdaptationContent.setText(documentSnapshot.getString("adaptationText"));
                        if (tvEcologicalContent != null) tvEcologicalContent.setText(documentSnapshot.getString("ecologicalText"));
                        if (tvHistoryContent != null) tvHistoryContent.setText(documentSnapshot.getString("historyText"));
                        if (tvNamestoryContent != null) tvNamestoryContent.setText(documentSnapshot.getString("nameStoryText"));
                        if (tvSymbolismContent != null) tvSymbolismContent.setText(documentSnapshot.getString("symbolismText"));
                    }
                });
    }

    private void renderLeafColorSwatches(List<String> leafColorsList) {
        if (layoutLeafColorsContainer == null) return;
        layoutLeafColorsContainer.removeAllViews();

        if (leafColorsList == null || leafColorsList.isEmpty()) {
            return;
        }

        for (String hexColor : leafColorsList) {
            if (hexColor == null || hexColor.trim().isEmpty()) continue;

            View colorSwatch = new View(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dpToPx(16), dpToPx(16));
            params.setMargins(dpToPx(4), 0, 0, 0);
            colorSwatch.setLayoutParams(params);

            try {
                String colorStr = hexColor.trim();
                if (!colorStr.startsWith("#")) colorStr = "#" + colorStr;

                GradientDrawable circleDrawable = new GradientDrawable();
                circleDrawable.setShape(GradientDrawable.OVAL);
                circleDrawable.setColor(Color.parseColor(colorStr));

                colorSwatch.setBackground(circleDrawable);
            } catch (Exception e) {
                Log.w(TAG, "Invalid color code fallback: " + hexColor);
            }

            layoutLeafColorsContainer.addView(colorSwatch);
        }
    }

    private void renderCommonProblems() {
        if (layoutCommonProblemsContainer == null) return;
        layoutCommonProblemsContainer.removeAllViews();

        if (commonProblemsArray != null && commonProblemsArray.length() > 0) {
            int defaultDrawableRes = R.drawable.disease;

            for (int i = 0; i < commonProblemsArray.length(); i++) {
                try {
                    JSONObject problem = commonProblemsArray.getJSONObject(i);
                    String problemTitle = problem.optString("title", "Common Issue");
                    int problemLikelihood = problem.optInt("likelihood_percentage", 0);
                    String problemImageUrl = problem.optString("image_url", "");

                    MaterialCardView card = new MaterialCardView(requireContext());
                    LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(dpToPx(160), dpToPx(160));
                    cardParams.setMargins(0, 0, dpToPx(12), 0);
                    card.setLayoutParams(cardParams);
                    card.setRadius(dpToPx(16));
                    card.setCardElevation(dpToPx(2));
                    card.setCardBackgroundColor(Color.parseColor("#1E1E1E"));
                    card.setStrokeWidth(0);

                    LinearLayout innerLayout = new LinearLayout(requireContext());
                    innerLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
                    innerLayout.setOrientation(LinearLayout.VERTICAL);

                    ImageView problemImageView = new ImageView(requireContext());
                    problemImageView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(100)));
                    problemImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

                    final String targetProblemImg = (problemImageUrl != null && problemImageUrl.startsWith("http"))
                            ? problemImageUrl
                            : "https://loremflickr.com/320/240/" + Uri.encode(scientificName + " " + problemTitle);

                    Glide.with(this)
                            .load(targetProblemImg)
                            .placeholder(defaultDrawableRes)
                            .error(defaultDrawableRes)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(problemImageView);

                    card.setOnClickListener(v -> showProblemDetailBottomSheet(problem));

                    TextView titleTextView = new TextView(requireContext());
                    titleTextView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
                    String displayProblemText = (problemLikelihood > 0) ? problemTitle + " (" + problemLikelihood + "%)" : problemTitle;
                    titleTextView.setText(displayProblemText);
                    titleTextView.setTextColor(Color.WHITE);
                    titleTextView.setTextSize(13);
                    titleTextView.setPadding(dpToPx(10), dpToPx(6), dpToPx(10), dpToPx(6));

                    innerLayout.addView(problemImageView);
                    innerLayout.addView(titleTextView);
                    card.addView(innerLayout);
                    layoutCommonProblemsContainer.addView(card);
                } catch (Exception e) {
                    Log.e(TAG, "Error rendering problem card", e);
                }
            }
        }
    }

    private void showProblemDetailBottomSheet(JSONObject problem) {
        String problemTitle = problem.optString("title", "Common Issue");
        String problemDescription = problem.optString("description", "No description available.");
        String symptomAnalysis = problem.optString("symptom_analysis", "No symptom analysis provided.");
        String diseaseCause = problem.optString("disease_cause", "No cause information provided.");
        String solutions = problem.optString("solutions", "No solution steps provided.");
        String prevention = problem.optString("prevention", "No prevention guide provided.");
        String problemImageUrl = problem.optString("image_url", "");

        final String targetProblemImg = (problemImageUrl != null && problemImageUrl.startsWith("http"))
                ? problemImageUrl
                : "https://loremflickr.com/320/240/" + Uri.encode(scientificName + " " + problemTitle);

        Intent intent = new Intent(requireContext(), ProblemDetailsActivity.class);
        intent.putExtra("problem_title", problemTitle);
        intent.putExtra("problem_description", problemDescription);
        intent.putExtra("symptom_analysis", symptomAnalysis);
        intent.putExtra("disease_cause", diseaseCause);
        intent.putExtra("solutions", solutions);
        intent.putExtra("prevention", prevention);
        intent.putExtra("image_url", targetProblemImg);
        intent.putExtra("scientific_name", scientificName);

        startActivity(intent);
    }

    private void renderCommonPests() {
        if (rvCommonPests == null || pestList.isEmpty()) return;
        PestAdapter adapter = new PestAdapter(pestList);
        rvCommonPests.setAdapter(adapter);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
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

    @Override
    public void onStart() {
        super.onStart();
        if (mapView != null) mapView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    public void onPause() {
        if (mapView != null) mapView.onPause();
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mapView != null) mapView.onStop();
    }

    @Override
    public void onDestroy() {
        if (mapView != null) mapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) mapView.onLowMemory();
    }
}
