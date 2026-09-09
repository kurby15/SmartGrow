package com.example.smartgrow.plants;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartgrow.R;
import com.example.smartgrow.core.SharedPrefManager;
import com.example.smartgrow.profile.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;

public class HomeFragment extends Fragment {

    private RecyclerView rvTodaysCare;
    private TextView tvDashboardLiveDateTime;
    private TextView tvDashboardWeatherMock;
    private TextView tvUserGreeting;

    private TodaysCareAdapter todaysCareAdapter;
    private List<PlantModel> plantList = new ArrayList<>();
    private List<CareTaskModel> careTaskList = new ArrayList<>();

    private DatabaseReference databaseReference, userRef;
    private String currentUsername;
    private String userFullName = "SmartGrower";
    private SharedPrefManager prefManager;

    private final Handler clockHandler = new Handler();
    private Runnable clockRunnable;

    public HomeFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        prefManager = SharedPrefManager.getInstance(requireContext());
        currentUsername = prefManager.getUsername();
        userFullName = prefManager.getFullName();

        tvUserGreeting = view.findViewById(R.id.tv_user_greeting);
        rvTodaysCare = view.findViewById(R.id.rv_todays_care);

        if (rvTodaysCare != null) {
            rvTodaysCare.setLayoutManager(new LinearLayoutManager(getContext()));
            rvTodaysCare.setHasFixedSize(true);
        }

        todaysCareAdapter = new TodaysCareAdapter(careTaskList, task -> {
            Toast.makeText(getContext(), "Clicked: " + task.getTitle(), Toast.LENGTH_SHORT).show();
        });
        rvTodaysCare.setAdapter(todaysCareAdapter);

        careTaskList.add(new CareTaskModel("Monstera Deliciosa", "💧 Due is 2 hours", "Water Now", R.drawable.img_9));
        careTaskList.add(new CareTaskModel("Snake Plant", "💧 Due today", "Mark Done", R.drawable.img_9));
        careTaskList.add(new CareTaskModel("Snake Plant", "💧 Due today", "Water now", R.drawable.img_9));
        careTaskList.add(new CareTaskModel("Snake Plant", "💧 Due today", "Mark Done", R.drawable.img_9));
        todaysCareAdapter.notifyDataSetChanged();

        if (currentUsername != null && !currentUsername.isEmpty() && !currentUsername.equals("unknown")) {
            databaseReference = FirebaseDatabase.getInstance().getReference("users").child(currentUsername).child("plants");
            userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUsername);

            fetchPlantsFromFirebase();
            fetchUserProfile();
        } else {
            if (tvUserGreeting != null) tvUserGreeting.setText("Welcome back!");
        }

        startRealTimeClock();
        updateMockWeatherEngine();



        ImageView btnOpenReminders = view.findViewById(R.id.btn_open_reminders);
        if (btnOpenReminders != null) {
            btnOpenReminders.setOnClickListener(v -> {
                com.example.smartgrow.plants.SetReminderFragment remindersFragment = new com.example.smartgrow.plants.SetReminderFragment();

                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, remindersFragment)
                        .addToBackStack(null)
                        .commit();
            });
        }
        return view;
    }

    private void fetchUserProfile() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isAdded() && snapshot.exists()) {
                    User user = snapshot.getValue(User.class);
                    if (user != null && user.getFullName() != null) {
                        userFullName = user.getFullName();
                        prefManager.saveUser(user);
                        updateLiveDateTimeAndGreeting();
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void fetchPlantsFromFirebase() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                plantList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    PlantModel plant = dataSnapshot.getValue(PlantModel.class);
                    if (plant != null) plantList.add(plant);
                }
                updateDashboardStats();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded()) Toast.makeText(getContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateDashboardStats() {
        View view = getView();
        if (view == null || !isAdded()) return;

        TextView tvTotalPlants = view.findViewById(R.id.tv_count_total_plants);
        TextView tvAvgHealth = view.findViewById(R.id.tv_value_avg_health);
        TextView tvNeedWater = view.findViewById(R.id.tv_count_need_water);

        if (tvTotalPlants != null) tvTotalPlants.setText(String.valueOf(plantList.size()));

        int needWaterCount = 0;
        int totalHealth = 0;
        careTaskList.clear();

        for (PlantModel p : plantList) {
            totalHealth += p.getHealthPercentage();
            if (p.getHealthPercentage() < 60) {
                needWaterCount++;
                careTaskList.add(new CareTaskModel("Water " + p.getName(), "💧 Due is 2 hours", "Water Now", R.drawable.ic_reminder));
            } else if (p.getHealthPercentage() < 85) {
                careTaskList.add(new CareTaskModel("Check " + p.getName(), "💧 Due today", "Inspect", R.drawable.ic_reminder));
            } else {
                careTaskList.add(new CareTaskModel("Fertilize " + p.getName(), "💧 Due tomorrow", "Mark Done", R.drawable.ic_reminder));
            }
        }

        todaysCareAdapter.notifyDataSetChanged();

        if (!plantList.isEmpty()) {
            int avgHealth = totalHealth / plantList.size();
            if (tvAvgHealth != null) tvAvgHealth.setText(avgHealth + "%");
        } else {
            if (tvAvgHealth != null) tvAvgHealth.setText("0%");
        }

        if (tvNeedWater != null) tvNeedWater.setText(String.valueOf(needWaterCount));
    }

    private void startRealTimeClock() {
        clockRunnable = new Runnable() {
            @Override
            public void run() {
                updateLiveDateTimeAndGreeting();
                clockHandler.postDelayed(this, 1000);
            }
        };
        clockHandler.post(clockRunnable);
    }

    private void updateLiveDateTimeAndGreeting() {
        if (!isAdded()) return;
        TimeZone phTimeZone = TimeZone.getTimeZone("Asia/Manila");
        Calendar calendar = Calendar.getInstance(phTimeZone);
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("EEEE, hh:mm a", new Locale("en", "PH"));
        dateTimeFormat.setTimeZone(phTimeZone);
        if (tvDashboardLiveDateTime != null) tvDashboardLiveDateTime.setText(dateTimeFormat.format(calendar.getTime()));

        int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
        String greeting = (hourOfDay < 12) ? "Good Morning" : (hourOfDay < 17) ? "Good Afternoon" : "Good Evening";
        if (tvUserGreeting != null) {
            String displayName = (userFullName != null && !userFullName.isEmpty()) ? userFullName : "SmartGrower";
            tvUserGreeting.setText(greeting + ", " + displayName + "!");
        }
    }

    private void updateMockWeatherEngine() {
        if (tvDashboardWeatherMock == null || !isAdded()) return;
        String[] weathers = {"Sunny", "Cloudy", "Partly Cloudy", "Light Rain"};
        String[] icons = {"☀️", "☁️", "⛅", "🌦️"};
        Random r = new Random();
        int idx = r.nextInt(weathers.length);
        tvDashboardWeatherMock.setText(icons[idx] + " " + weathers[idx] + ", " + (28 + r.nextInt(5)) + "°C");
    }

    @Override public void onPause() { super.onPause(); clockHandler.removeCallbacks(clockRunnable); }
    @Override public void onResume() { super.onResume(); clockHandler.post(clockRunnable); }
}