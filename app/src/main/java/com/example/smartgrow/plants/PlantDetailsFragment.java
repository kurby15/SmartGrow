package com.example.smartgrow.plants;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
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
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.smartgrow.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.card.MaterialCardView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PlantDetailsFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG = "PlantDetailsFragment";
    private static final String ARG_JSON = "raw_json";

    private String mRawJson;
    private NestedScrollView scrollView;
    private MapView mapView;
    private GoogleMap googleMap;

    private TextView tvPetToxicity, tvWeedPotential, tvDistribution, tvHabitat, tvPlantType, tvLifespan;
    private LinearLayout layoutCommonProblemsContainer;
    private RecyclerView rvCommonPests;
    private List<PestModel> pestList = new ArrayList<>();
    private TextView tvUltimateHeight, tvUltimateSpread, tvLeafType, tvPlantingTime;
    private LinearLayout layoutLeafColorsContainer;
    private TextView tvTemp, tvHardiness, tvSunlight, tvSoil;
    private TextView tvPruningContent, tvPropagationContent, tvRepottingContent;
    private TextView tvUsesContent, tvAdaptationContent, tvEcologicalContent, tvHistoryContent, tvNameStoryContent, tvSymbolismContent;
    private TextView tvDetectedPest;

    private View sectionBasicInfo, sectionCare, sectionExplore;

    private List<MapLocation> mapLocations = new ArrayList<>();
    private List<String> leafColorsList = new ArrayList<>();
    private JSONArray commonProblemsArray = null;
    private String scientificName = "N/A";
    private String distribution = "N/A";

    private boolean isAutoScrolling = false;

    public static PlantDetailsFragment newInstance(String rawJson) {
        PlantDetailsFragment fragment = new PlantDetailsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_JSON, rawJson);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mRawJson = getArguments().getString(ARG_JSON);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_plant_details, container, false);
        initViews(view);
        if (mapView != null) mapView.onCreate(savedInstanceState);
        parseAndPopulateData();
        setupScrollListener();
        return view;
    }

    private void initViews(View v) {
        scrollView = v.findViewById(R.id.fragment_scroll_view);
        sectionBasicInfo = v.findViewById(R.id.section_basic_info);
        sectionCare = v.findViewById(R.id.section_care);
        sectionExplore = v.findViewById(R.id.section_explore);

        tvPetToxicity = v.findViewById(R.id.tv_pet_toxicity);
        tvWeedPotential = v.findViewById(R.id.tv_weed_potential);
        tvDistribution = v.findViewById(R.id.tv_distribution);
        tvHabitat = v.findViewById(R.id.tv_habitat);
        tvPlantType = v.findViewById(R.id.tv_plant_type);
        tvLifespan = v.findViewById(R.id.tv_lifespan);
        layoutCommonProblemsContainer = v.findViewById(R.id.layout_common_problems_container);
        rvCommonPests = v.findViewById(R.id.rv_common_pests);
        tvDetectedPest = v.findViewById(R.id.tv_detected_pest);
        
        if (rvCommonPests != null) rvCommonPests.setLayoutManager(new LinearLayoutManager(getContext()));
        
        tvUltimateHeight = v.findViewById(R.id.tv_ultimate_height);
        tvUltimateSpread = v.findViewById(R.id.tv_ultimate_spread);
        tvLeafType = v.findViewById(R.id.tv_leaf_type);
        tvPlantingTime = v.findViewById(R.id.tv_planting_time);
        layoutLeafColorsContainer = v.findViewById(R.id.layout_leaf_colors_container);
        
        tvTemp = v.findViewById(R.id.tv_temp);
        tvHardiness = v.findViewById(R.id.tv_hardiness);
        tvSunlight = v.findViewById(R.id.tv_sunlight);
        tvSoil = v.findViewById(R.id.tv_soil);
        
        tvPruningContent = v.findViewById(R.id.tv_pruning_content);
        tvPropagationContent = v.findViewById(R.id.tv_propagation_content);
        tvRepottingContent = v.findViewById(R.id.tv_repotting_content);
        
        tvUsesContent = v.findViewById(R.id.tv_uses_content);
        tvAdaptationContent = v.findViewById(R.id.tv_adaptation_content);
        tvEcologicalContent = v.findViewById(R.id.tv_ecological_content);
        tvHistoryContent = v.findViewById(R.id.tv_history_content);
        tvNameStoryContent = v.findViewById(R.id.tv_namestory_content);
        tvSymbolismContent = v.findViewById(R.id.tv_symbolism_content);
        
        mapView = v.findViewById(R.id.map_view);

        v.findViewById(R.id.layout_distribution_click).setOnClickListener(view -> 
            Toast.makeText(getContext(), "Distribution: " + distribution, Toast.LENGTH_SHORT).show());
    }

    private void setupScrollListener() {
        if (scrollView == null) return;
        scrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            if (isAutoScrolling) return;

            int careTop = sectionCare.getTop();
            int exploreTop = sectionExplore.getTop();

            if (scrollY >= exploreTop - 100) {
                notifyActivitySectionChanged(2);
            } else if (scrollY >= careTop - 100) {
                notifyActivitySectionChanged(1);
            } else {
                notifyActivitySectionChanged(0);
            }
        });
    }

    private void notifyActivitySectionChanged(int index) {
        if (getActivity() instanceof PlantDetailsActivity) {
            ((PlantDetailsActivity) getActivity()).onSectionChanged(index);
        }
    }

    public void scrollToSection(int index) {
        if (scrollView == null || getView() == null) return;
        View target = null;
        switch (index) {
            case 0: target = sectionBasicInfo; break;
            case 1: target = sectionCare; break;
            case 2: target = sectionExplore; break;
        }
        if (target != null) {
            View finalTarget = target;
            isAutoScrolling = true;
            scrollView.post(() -> {
                scrollView.smoothScrollTo(0, finalTarget.getTop());
                scrollView.postDelayed(() -> isAutoScrolling = false, 600);
            });
        }
    }

    private void parseAndPopulateData() {
        if (mRawJson == null || mRawJson.isEmpty()) return;
        try {
            JSONObject root = new JSONObject(sanitizeJson(mRawJson));
            JSONObject profile = root.optJSONObject("plant_profile");
            if (profile != null) {
                scientificName = profile.optString("scientific_name", "N/A");
                distribution = profile.optString("distribution_text", profile.optString("origin", "N/A"));
                tvPetToxicity.setText(profile.optString("pet_toxicity", "N/A"));
                tvWeedPotential.setText(profile.optString("weed_potential", "N/A"));
                tvDistribution.setText(distribution);
                tvHabitat.setText(profile.optString("habitat", "N/A"));
                tvPlantType.setText(profile.optString("type", profile.optString("plant_type", "N/A")));
                tvLifespan.setText(profile.optString("lifespan", "N/A"));

                JSONArray locArray = profile.optJSONArray("distribution_coordinates");
                if (locArray != null) {
                    mapLocations.clear();
                    for (int i = 0; i < locArray.length(); i++) {
                        JSONObject l = locArray.optJSONObject(i);
                        if (l != null) mapLocations.add(new MapLocation(l.optDouble("latitude"), l.optDouble("longitude"), l.optString("title"), l.optString("snippet"), l.optString("distribution_type")));
                    }
                }
            }

            commonProblemsArray = root.optJSONArray("common_problems");
            renderCommonProblems();

            JSONObject pestInfo = root.optJSONObject("pest_info");
            if (pestInfo != null) {
                if (tvDetectedPest != null) tvDetectedPest.setText("Detected: " + pestInfo.optString("possible_pest_detected", "None"));
                pestList.clear();
                JSONArray pests = pestInfo.optJSONArray("common_pests");
                if (pests != null) {
                    for (int i = 0; i < pests.length(); i++) {
                        JSONObject p = pests.optJSONObject(i);
                        if (p != null) pestList.add(new PestModel(p.optString("name"), p.optString("description"), ""));
                    }
                }
                if (rvCommonPests != null) rvCommonPests.setAdapter(new PestAdapter(pestList));
            }

            JSONObject charac = root.optJSONObject("characteristics");
            if (charac != null) {
                tvUltimateHeight.setText(charac.optString("ultimate_height", "N/A"));
                tvUltimateSpread.setText(charac.optString("ultimate_spread", "N/A"));
                tvLeafType.setText(charac.optString("leaf_type", "N/A"));
                tvPlantingTime.setText(charac.optString("planting_time", "N/A"));
                leafColorsList.clear();
                JSONArray colors = charac.optJSONArray("leaf_colors");
                if (colors != null) for (int i = 0; i < colors.length(); i++) leafColorsList.add(colors.optString(i));
                renderLeafColorSwatches();
            }

            JSONObject eco = root.optJSONObject("ecosystem");
            if (eco != null) {
                tvTemp.setText("Temperature: " + eco.optString("temp_range", "N/A"));
                tvHardiness.setText("Hardiness Zones: " + eco.optString("hardiness_zones", "N/A"));
                tvSunlight.setText(eco.optString("sunlight", "N/A"));
                tvSoil.setText(eco.optString("soil", "N/A"));
            }

            JSONObject how = root.optJSONObject("how_tos");
            if (how != null) {
                tvPruningContent.setText(how.optString("pruning", "N/A"));
                tvPropagationContent.setText(how.optString("propagation", "N/A"));
                tvRepottingContent.setText(how.optString("repotting", "N/A"));
            }

            JSONObject extra = root.optJSONObject("extra_details");
            if (extra != null) {
                tvUsesContent.setText(extra.optString("uses", "N/A"));
                tvAdaptationContent.setText(extra.optString("adaptation_strategies", "N/A"));
                tvEcologicalContent.setText(extra.optString("ecological_application", "N/A"));
                tvHistoryContent.setText(extra.optString("history_and_legends", "N/A"));
                tvNameStoryContent.setText(extra.optString("name_story", "N/A"));
                tvSymbolismContent.setText(extra.optString("symbolism", "N/A"));
            }
            if (mapView != null) mapView.getMapAsync(this);
        } catch (Exception e) { Log.e(TAG, "Parse error", e); }
    }

    private String sanitizeJson(String raw) {
        if (raw == null) return "{}";
        String c = raw.trim();
        if (c.startsWith("```")) {
            int f = c.indexOf("\n");
            c = c.substring(f != -1 ? f + 1 : 3);
            if (c.endsWith("```")) c = c.substring(0, c.length() - 3);
        }
        return c.trim();
    }

    private void renderCommonProblems() {
        if (layoutCommonProblemsContainer == null || commonProblemsArray == null) return;
        layoutCommonProblemsContainer.removeAllViews();
        for (int i = 0; i < commonProblemsArray.length(); i++) {
            try {
                JSONObject p = commonProblemsArray.getJSONObject(i);
                MaterialCardView card = new MaterialCardView(getContext());
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dpToPx(160), dpToPx(160));
                lp.setMargins(0, 0, dpToPx(12), 0);
                card.setLayoutParams(lp);
                card.setRadius(dpToPx(16));
                card.setCardBackgroundColor(Color.parseColor("#1E1E1E"));
                LinearLayout inner = new LinearLayout(getContext());
                inner.setOrientation(LinearLayout.VERTICAL);
                ImageView img = new ImageView(getContext());
                img.setLayoutParams(new LinearLayout.LayoutParams(-1, dpToPx(100)));
                img.setScaleType(ImageView.ScaleType.CENTER_CROP);
                Glide.with(this).load(p.optString("image_url")).placeholder(R.drawable.disease).into(img);
                TextView tv = new TextView(getContext());
                tv.setText(p.optString("title"));
                tv.setTextColor(Color.WHITE);
                tv.setPadding(dpToPx(10), dpToPx(6), dpToPx(10), dpToPx(6));
                inner.addView(img); inner.addView(tv); card.addView(inner);
                card.setOnClickListener(v -> {
                    Intent intent = new Intent(getContext(), ProblemDetailsActivity.class);
                    intent.putExtra("problem_title", p.optString("title"));
                    intent.putExtra("problem_description", p.optString("description"));
                    intent.putExtra("symptom_analysis", p.optString("symptom_analysis"));
                    intent.putExtra("disease_cause", p.optString("disease_cause"));
                    intent.putExtra("solutions", p.optString("solutions"));
                    intent.putExtra("prevention", p.optString("prevention"));
                    intent.putExtra("image_url", p.optString("image_url"));
                    intent.putExtra("scientific_name", scientificName);
                    startActivity(intent);
                });
                layoutCommonProblemsContainer.addView(card);
            } catch (Exception ignored) {}
        }
    }

    private void renderLeafColorSwatches() {
        if (layoutLeafColorsContainer == null) return;
        layoutLeafColorsContainer.removeAllViews();
        for (String c : leafColorsList) {
            View s = new View(getContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dpToPx(16), dpToPx(16));
            lp.setMargins(dpToPx(4), 0, 0, 0);
            s.setLayoutParams(lp);
            GradientDrawable d = new GradientDrawable();
            d.setShape(GradientDrawable.OVAL);
            try { d.setColor(Color.parseColor(c.startsWith("#") ? c : "#" + c)); } catch (Exception e) { d.setColor(Color.GREEN); }
            s.setBackground(d);
            layoutLeafColorsContainer.addView(s);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;
        googleMap.clear();
        if (mapLocations.isEmpty()) return;
        LatLngBounds.Builder b = new LatLngBounds.Builder();
        for (MapLocation l : mapLocations) {
            LatLng p = new LatLng(l.lat, l.lng);
            googleMap.addMarker(new MarkerOptions().position(p).title(l.title).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
            b.include(p);
        }
        try {
            if (mapLocations.size() == 1) googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mapLocations.get(0).lat, mapLocations.get(0).lng), 5f));
            else googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(b.build(), 120));
        } catch (Exception ignored) {}
        googleMap.setOnMapClickListener(latLng -> showFullScreenMap());
    }

    private void showFullScreenMap() {
        Dialog dialog = new Dialog(getContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        FrameLayout container = new FrameLayout(getContext());
        MapView fullMapView = new MapView(getContext());
        fullMapView.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        container.addView(fullMapView);
        ImageButton closeBtn = new ImageButton(getContext());
        FrameLayout.LayoutParams btnParams = new FrameLayout.LayoutParams(dpToPx(48), dpToPx(48));
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
            LatLngBounds.Builder b = new LatLngBounds.Builder();
            for (MapLocation loc : mapLocations) {
                LatLng p = new LatLng(loc.lat, loc.lng);
                fullMap.addMarker(new MarkerOptions().position(p).title(loc.title));
                b.include(p);
            }
            if (mapLocations.size() == 1) fullMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mapLocations.get(0).lat, mapLocations.get(0).lng), 5f));
            else try { fullMap.moveCamera(CameraUpdateFactory.newLatLngBounds(b.build(), dpToPx(80))); } catch (Exception ignored) {}
        });
        dialog.show();
    }

    private int dpToPx(int dp) { return Math.round(dp * getResources().getDisplayMetrics().density); }

    @Override public void onStart() { super.onStart(); if (mapView != null) mapView.onStart(); }
    @Override public void onResume() { super.onResume(); if (mapView != null) mapView.onResume(); }
    @Override public void onPause() { if (mapView != null) mapView.onPause(); super.onPause(); }
    @Override public void onStop() { if (mapView != null) mapView.onStop(); super.onStop(); }
    @Override public void onDestroy() { if (mapView != null) mapView.onDestroy(); super.onDestroy(); }
    @Override public void onLowMemory() { super.onLowMemory(); if (mapView != null) mapView.onLowMemory(); }

    private static class MapLocation {
        double lat, lng; String title, snippet, distributionType;
        MapLocation(double lat, double lng, String title, String snippet, String dType) { this.lat = lat; this.lng = lng; this.title = title; this.snippet = snippet; this.distributionType = dType; }
    }
}
