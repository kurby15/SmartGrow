package com.example.smartgrow.plants;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.smartgrow.R;

import java.util.ArrayList;
import java.util.List;

public class ScheduleFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_plant_schedule, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView rvSchedule = view.findViewById(R.id.rvSchedule);

        List<ScheduleModel> scheduleList = new ArrayList<>();
        scheduleList.add(new ScheduleModel("Water", "Today, 6 PM", "Not necessary in Sept"));
        scheduleList.add(new ScheduleModel("Fertilize", "Next week", "Use balanced liquid fertilizer"));

        // I-set ang Adapter at LayoutManager sa RecyclerView
        ScheduleAdapter adapter = new ScheduleAdapter(scheduleList);
        rvSchedule.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvSchedule.setAdapter(adapter);
    }
}