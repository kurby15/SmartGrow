package com.example.smartgrow.plants;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.smartgrow.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ScheduleFragment extends Fragment {

    private static final String TAG = "ScheduleFragment";
    private RecyclerView rvSchedule;
    private ScheduleAdapter adapter;
    private List<ScheduleModel> scheduleList = new ArrayList<>();
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_plant_schedule, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        rvSchedule = view.findViewById(R.id.rvSchedule);
        rvSchedule.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        adapter = new ScheduleAdapter(scheduleList);
        rvSchedule.setAdapter(adapter);

        if (getActivity() instanceof PlantDiaryActivity) {
            String plantId = ((PlantDiaryActivity) getActivity()).getPlantId();
            if (plantId != null && !plantId.isEmpty()) {
                loadScheduleData(plantId);
            }
        }
    }

    private void loadScheduleData(String plantId) {
        db.collection("diary").document(plantId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        scheduleList.clear();
                        Map<String, Object> reminders = (Map<String, Object>) documentSnapshot.get("reminders");
                        
                        int health = 100;
                        Long hPercent = documentSnapshot.getLong("healthPercentage");
                        if (hPercent != null) health = hPercent.intValue();
                        
                        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                        String lastWatered = documentSnapshot.getString("lastWateredDate");
                        String lastFertilized = documentSnapshot.getString("lastFertilizedDate");
                        String lastChecked = documentSnapshot.getString("lastCheckedDate");

                        boolean waterAdded = false;
                        boolean fertAdded = false;
                        boolean sunAdded = false;

                        String prefTime = "12:00 PM";
                        if (reminders != null) {
                            prefTime = (String) reminders.get("preferredTime");
                            if (prefTime == null) prefTime = "12:00 PM";

                            String waterFreq = (String) reminders.get("wateringSchedule");
                            if (waterFreq != null && !waterFreq.equalsIgnoreCase("None")) {
                                scheduleList.add(new ScheduleModel("Water", prefTime, "Frequency: " + waterFreq));
                                waterAdded = true;
                            }

                            String fertFreq = (String) reminders.get("fertilizerSchedule");
                            if (fertFreq != null && !fertFreq.equalsIgnoreCase("None")) {
                                scheduleList.add(new ScheduleModel("Fertilize", prefTime, "Frequency: " + fertFreq));
                                fertAdded = true;
                            }

                            String sunFreq = (String) reminders.get("sunlightSchedule");
                            if (sunFreq != null && !sunFreq.equalsIgnoreCase("None")) {
                                scheduleList.add(new ScheduleModel("Sunlight", prefTime, "Frequency: " + sunFreq));
                                sunAdded = true;
                            }
                        }

                        // Health-based automatic tasks (Health below 50)
                        if (health < 50) {
                            if (!waterAdded && !todayDate.equals(lastWatered)) {
                                scheduleList.add(new ScheduleModel("Water (Low Health)", "Immediate", "Automatic care needed"));
                            }
                            if (!fertAdded && !todayDate.equals(lastFertilized)) {
                                scheduleList.add(new ScheduleModel("Fertilize (Low Health)", "Immediate", "Nutrients required"));
                            }
                            if (!sunAdded && !todayDate.equals(lastChecked)) {
                                scheduleList.add(new ScheduleModel("Sunlight (Low Health)", "Immediate", "Sunlight check required"));
                            }
                        }

                        if (scheduleList.isEmpty()) {
                            scheduleList.add(new ScheduleModel("No Schedule", "-", "Set reminders to see them here."));
                        }
                        
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading schedule", e);
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to load schedule", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
