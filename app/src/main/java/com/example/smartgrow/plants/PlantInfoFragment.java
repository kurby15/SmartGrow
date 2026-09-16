package com.example.smartgrow.plants;

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
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.smartgrow.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PlantInfoFragment extends Fragment {

    private static final String TAG = "PlantInfoFragment";

    private TextView tvPetToxicity, tvWeedPotential, tvDistribution, tvHabitat, tvPlantType, tvLifespan;
    private TextView tvUltimateHeight, tvUltimateSpread, tvLeafType, tvPlantingTime, tvLeafColor;
    private TextView tvTemp, tvHardiness, tvSunlight, tvSoil;
    private TextView tvPruningContent, tvPropagationContent, tvRepottingContent;
    private TextView tvUsesContent, tvAdaptationContent, tvEcologicalContent, tvHistoryContent, tvNamestoryContent, tvSymbolismContent;

    private LinearLayout layoutDistributionClick, layoutLeafColorsContainer, layoutCommonProblemsContainer;
    private MapView mapView;
    private GoogleMap googleMap;
    private RecyclerView rvCommonPests;

    private FirebaseFirestore db;

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

        // 1. Initialize ang lahat ng TextViews (Kasama ang tv_leaf_color)
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

        // 2. MapView Initialization at Async Setup
        mapView = view.findViewById(R.id.map_view);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(map -> {
            googleMap = map;
            updateMapMarkers();
        });

        rvCommonPests = view.findViewById(R.id.rv_common_pests);
        rvCommonPests.setLayoutManager(new LinearLayoutManager(requireContext()));

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

                        // 3. Leaf Colors Parsing & Display
                        List<String> colorsList = (List<String>) documentSnapshot.get("leaf_colors");
                        if (tvLeafColor != null) {
                            if (colorsList != null && !colorsList.isEmpty()) {
                                tvLeafColor.setText(TextUtils.join(", ", colorsList));
                            } else {
                                tvLeafColor.setText("N/A");
                            }
                        }

                        // 4. Distribution Coordinates Parsing para sa Maps
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