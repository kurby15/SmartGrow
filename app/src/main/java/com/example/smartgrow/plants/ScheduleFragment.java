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

import java.util.ArrayList;
import java.util.List;
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
                        
                        if (reminders != null) {
                            String time = (String) reminders.get("preferredTime");
                            if (time == null) time = "Not set";

                            String water = (String) reminders.get("wateringSchedule");
                            if (water != null && !water.equalsIgnoreCase("None")) {
                                scheduleList.add(new ScheduleModel("Water", time, "Frequency: " + water));
                            }

                            String fert = (String) reminders.get("fertilizerSchedule");
                            if (fert != null && !fert.equalsIgnoreCase("None")) {
                                scheduleList.add(new ScheduleModel("Fertilize", time, "Frequency: " + fert));
                            }

                            String sun = (String) reminders.get("sunlightSchedule");
                            if (sun != null && !sun.equalsIgnoreCase("None")) {
                                scheduleList.add(new ScheduleModel("Sunlight", time, "Frequency: " + sun));
                            }
                        }

                        if (scheduleList.isEmpty()) {
                            // Optional: add a placeholder if no schedules are set
                            scheduleList.add(new ScheduleModel("No Schedule", "-", "Set reminders to see them here."));
                        }
                        
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading schedule", e);
                    Toast.makeText(getContext(), "Failed to load schedule", Toast.LENGTH_SHORT).show();
                });
    }
}
