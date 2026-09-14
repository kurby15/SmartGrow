package com.example.smartgrow.plants;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    private RecyclerView rvTodaysCare;
    private TextView tvDashboardLiveDateTime;
    private TextView tvDashboardWeatherMock;
    private TextView tvUserGreeting;
    
    // Stats TextViews
    private TextView tvTotalPlants, tvAvgHealth, tvNeedWater, tvPestAlerts;

    private TodaysCareAdapter todaysCareAdapter;
    private List<MyGardenPlantModel> plantList = new ArrayList<>();
    private List<CareTaskModel> careTaskList = new ArrayList<>();

    private DatabaseReference userRef;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration diaryListener;

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

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        prefManager = SharedPrefManager.getInstance(requireContext());
        currentUsername = prefManager.getUsername();
        userFullName = prefManager.getFullName();

        initViews(view);

        if (currentUsername != null && !currentUsername.isEmpty() && !currentUsername.equals("unknown")) {
            userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUsername);
            fetchUserProfile();
        }

        listenToDiaryData();
        startRealTimeClock();
        updateMockWeatherEngine();

        return view;
    }

    private void initViews(View view) {
        tvUserGreeting = view.findViewById(R.id.tv_user_greeting);
        rvTodaysCare = view.findViewById(R.id.rv_todays_care);
        
        tvTotalPlants = view.findViewById(R.id.tv_count_total_plants);
        tvAvgHealth = view.findViewById(R.id.tv_value_avg_health);
        tvNeedWater = view.findViewById(R.id.tv_count_need_water);
        tvPestAlerts = view.findViewById(R.id.tv_count_pest_alerts);

        // Set Default Values
        if (tvTotalPlants != null) tvTotalPlants.setText("0");
        if (tvAvgHealth != null) tvAvgHealth.setText("0%");
        if (tvNeedWater != null) tvNeedWater.setText("0");
        if (tvPestAlerts != null) tvPestAlerts.setText("0");

        if (rvTodaysCare != null) {
            rvTodaysCare.setLayoutManager(new LinearLayoutManager(getContext()));
            rvTodaysCare.setHasFixedSize(true);
        }

        todaysCareAdapter = new TodaysCareAdapter(careTaskList, task -> {
            Toast.makeText(getContext(), "Task: " + task.getTitle(), Toast.LENGTH_SHORT).show();
        });
        rvTodaysCare.setAdapter(todaysCareAdapter);

        ImageView btnOpenReminders = view.findViewById(R.id.btn_open_reminders);
        if (btnOpenReminders != null) {
            btnOpenReminders.setOnClickListener(v -> {
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new com.example.smartgrow.plants.SetReminderFragment())
                        .addToBackStack(null)
                        .commit();
            });
        }
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

    private void listenToDiaryData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        diaryListener = db.collection("diary")
                .whereEqualTo("userId", currentUser.getUid())
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening to diary data", error);
                        return;
                    }

                    if (value != null) {
                        plantList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            MyGardenPlantModel plant = doc.toObject(MyGardenPlantModel.class);
                            if (plant != null) {
                                if (plant.getId() == null) plant.setId(doc.getId());
                                plantList.add(plant);
                            }
                        }
                        updateDashboardStats();
                    }
                });
    }

    private void updateDashboardStats() {
        if (!isAdded()) return;

        if (tvTotalPlants != null) tvTotalPlants.setText(String.valueOf(plantList.size()));
        if (tvPestAlerts != null) tvPestAlerts.setText("0");

        int needWaterCount = 0;
        int totalHealth = 0;
        careTaskList.clear();

        for (MyGardenPlantModel p : plantList) {
            totalHealth += p.getHealthPercentage();
            
            // Logic: Health < 60 means needs water
            if (p.getHealthPercentage() < 60) {
                needWaterCount++;
                careTaskList.add(new CareTaskModel("Water " + p.getPlantName(), "💧 Due in 2 hours", "Water Now", R.drawable.ic_reminder));
            } else if (p.getHealthPercentage() < 85) {
                careTaskList.add(new CareTaskModel("Check " + p.getPlantName(), "🔍 Due today", "Inspect", R.drawable.ic_reminder));
            } else {
                careTaskList.add(new CareTaskModel("Care for " + p.getPlantName(), "🌱 Healthy condition", "Mark Done", R.drawable.ic_reminder));
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
        
        int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
        String greeting = (hourOfDay < 12) ? "Good Morning" : (hourOfDay < 17) ? "Good Afternoon" : "Good Evening";
        if (tvUserGreeting != null) {
            String displayName = (userFullName != null && !userFullName.isEmpty()) ? userFullName : "SmartGrower";
            tvUserGreeting.setText(greeting + ", " + displayName + "!");
        }
    }

    private void updateMockWeatherEngine() {
        if (!isAdded()) return;
        String[] weathers = {"Sunny", "Cloudy", "Partly Cloudy", "Light Rain"};
        String[] icons = {"☀️", "☁️", "⛅", "🌦️"};
        Random r = new Random();
        int idx = r.nextInt(weathers.length);
        if (tvDashboardWeatherMock != null) {
            tvDashboardWeatherMock.setText(icons[idx] + " " + weathers[idx] + ", " + (28 + r.nextInt(5)) + "°C");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        clockHandler.removeCallbacks(clockRunnable);
    }

    @Override
    public void onResume() {
        super.onResume();
        clockHandler.post(clockRunnable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (diaryListener != null) diaryListener.remove();
    }
}