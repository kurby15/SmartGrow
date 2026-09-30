package com.example.smartgrow.plants;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
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
import com.example.smartgrow.core.NotificationHelper;
import com.example.smartgrow.core.SharedPrefManager;
import com.example.smartgrow.profile.User;
import com.facebook.shimmer.ShimmerFrameLayout;
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
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    private ShimmerFrameLayout shimmerFrameLayout;
    private View mainContentContainer;
    private boolean isDataLoaded = false;

    private RecyclerView rvTodaysCare;
    private TextView tvDashboardLiveDateTime;
    private TextView tvDashboardWeatherMock;
    private TextView tvUserGreeting;

    // Stats TextViews
    private TextView tvTotalPlants, tvAvgHealth, tvNeedWater, tvPestAlerts;

    private TodaysCareAdapter todaysCareAdapter;
    private final List<MyGardenPlantModel> plantList = new ArrayList<>();
    private final List<CareTaskModel> careTaskList = new ArrayList<>();

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
        startShimmer();

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
        // Corrected ID mappings from fragment_home.xml
        shimmerFrameLayout = view.findViewById(R.id.home_skeleton);
        mainContentContainer = view.findViewById(R.id.home_content);

        tvUserGreeting = view.findViewById(R.id.tv_user_greeting);
        rvTodaysCare = view.findViewById(R.id.rv_todays_care);

        tvDashboardWeatherMock = view.findViewById(R.id.tv_weather_desc);
        tvDashboardLiveDateTime = null;

        tvTotalPlants = view.findViewById(R.id.tv_count_total_plants);
        tvAvgHealth = view.findViewById(R.id.tv_value_avg_health);
        tvNeedWater = view.findViewById(R.id.tv_count_need_water);
        tvPestAlerts = view.findViewById(R.id.tv_count_pest_alerts);

        if (tvTotalPlants != null) tvTotalPlants.setText("0");
        if (tvAvgHealth != null) tvAvgHealth.setText("0%");
        if (tvNeedWater != null) tvNeedWater.setText("0");
        if (tvPestAlerts != null) tvPestAlerts.setText("0");

        if (rvTodaysCare != null) {
            rvTodaysCare.setLayoutManager(new LinearLayoutManager(getContext()));
            rvTodaysCare.setHasFixedSize(true);
        }

        todaysCareAdapter = new TodaysCareAdapter(careTaskList, this::markTaskAsDone);
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
    private void startShimmer() {
        if (shimmerFrameLayout != null) {
            shimmerFrameLayout.setVisibility(View.VISIBLE);
            shimmerFrameLayout.startShimmer();
        }
        if (mainContentContainer != null) {
            mainContentContainer.setVisibility(View.GONE);
        }
    }

    private void stopShimmer() {
        // Nagdagdag tayo ng 1-second delay (1000 milliseconds) bago itago ang skeleton
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (shimmerFrameLayout != null) {
                shimmerFrameLayout.stopShimmer();
                shimmerFrameLayout.setVisibility(View.GONE);
            }
            if (mainContentContainer != null) {
                mainContentContainer.setVisibility(View.VISIBLE);
            }
            isDataLoaded = true;
        }, 1000); // Pwede mong palitan ang 1000 kung gusto mo mas mabilis o mas matagal
    }

    private void listenToDiaryData() {
        // Siguraduhing naka-start ang shimmer bago mag-fetch
        startShimmer();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            stopShimmer();
            return;
        }

        diaryListener = db.collection("diary")
                .whereEqualTo("userId", currentUser.getUid())
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening to diary data", error);
                        stopShimmer();
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

                    // Hide skeleton shimmer and display content on first fetch completion
                    stopShimmer();
                });
    }

    private void markTaskAsDone(CareTaskModel task) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Manila"));
        String todayDate = sdf.format(new Date());
        String fieldToUpdate = "";

        if ("Water".equals(task.getTaskType())) {
            fieldToUpdate = "lastWateredDate";
        } else if ("Check".equals(task.getTaskType())) {
            fieldToUpdate = "lastCheckedDate";
        } else if ("Fertilize".equals(task.getTaskType())) {
            fieldToUpdate = "lastFertilizedDate";
        }

        if (!fieldToUpdate.isEmpty() && task.getId() != null) {
            db.collection("diary").document(task.getId())
                    .update(fieldToUpdate, todayDate)
                    .addOnSuccessListener(aVoid -> {
                        NotificationHelper.cancelOverdueReminder(requireContext(), task.getId(), task.getTaskType());
                        Toast.makeText(getContext(), task.getTaskType() + " marked as done!", Toast.LENGTH_SHORT).show();
                        updateDashboardStats();
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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

    private void updateDashboardStats() {
        if (!isAdded()) return;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Manila"));
        String todayDate = sdf.format(new Date());

        if (tvTotalPlants != null) tvTotalPlants.setText(String.valueOf(plantList.size()));

        int needWaterCount = 0;
        int totalHealth = 0;
        java.util.HashSet<String> plantsWithPests = new java.util.HashSet<>();
        careTaskList.clear();

        for (MyGardenPlantModel p : plantList) {
            totalHealth += p.getHealthPercentage();

            boolean hasPest = p.getHealthPercentage() < 20;

            if (hasPest) {
                String plantKey = (p.getPlantId() != null && !p.getPlantId().isEmpty()) ? p.getPlantId() : p.getPlantName();
                if (plantKey == null || plantKey.isEmpty()) plantKey = p.getId();

                if (plantKey != null && !plantKey.isEmpty()) {
                    plantsWithPests.add(plantKey);
                }
            }

            boolean isWateredToday = todayDate.equals(p.getLastWateredDate());
            Map<String, Object> reminders = p.getReminders();

            Bitmap plantBitmap = null;
            try {
                String base64Str = p.getImageBase64();
                if (base64Str != null && !base64Str.isEmpty()) {
                    byte[] decodedBytes = Base64.decode(base64Str, Base64.DEFAULT);
                    plantBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            boolean waterTaskAdded = false;
            boolean fertTaskAdded = false;
            boolean checkTaskAdded = false;
            boolean countedForWater = false;

            if (reminders != null) {
                String waterFreq = (String) reminders.get("wateringSchedule");
                String fertFreq = (String) reminders.get("fertilizerSchedule");
                String sunFreq = (String) reminders.get("sunlightSchedule");
                String prefTime = (String) reminders.get("preferredTime");
                if (prefTime == null) prefTime = "12:00 PM";

                if (waterFreq != null && !"None".equalsIgnoreCase(waterFreq)) {
                    if (isTaskDue(waterFreq, p.getLastWateredDate())) {
                        CareTaskModel task = new CareTaskModel(p.getId(), "Water " + p.getPlantName(), "💧 Scheduled at " + prefTime, "Water Now", plantBitmap, "Water");
                        task.setDone(isWateredToday);
                        careTaskList.add(task);
                        waterTaskAdded = true;

                        if (!isWateredToday && isTimeReached(prefTime)) {
                            needWaterCount++;
                            countedForWater = true;
                        }
                    }
                } else if (!isWateredToday && p.getHealthPercentage() < 60) {
                    needWaterCount++;
                    countedForWater = true;
                }

                if (fertFreq != null && !"None".equalsIgnoreCase(fertFreq)) {
                    if (isTaskDue(fertFreq, p.getLastFertilizedDate())) {
                        boolean isDone = todayDate.equals(p.getLastFertilizedDate());
                        CareTaskModel task = new CareTaskModel(p.getId(), "Fertilize " + p.getPlantName(), "🌿 Feeding due today", "Feed Now", plantBitmap, "Fertilize");
                        task.setDone(isDone);
                        careTaskList.add(task);
                        fertTaskAdded = true;
                    }
                }

                if (sunFreq != null && !"None".equalsIgnoreCase(sunFreq)) {
                    if (isTaskDue(sunFreq, p.getLastCheckedDate())) {
                        boolean isDone = todayDate.equals(p.getLastCheckedDate());
                        CareTaskModel task = new CareTaskModel(p.getId(), "Check " + p.getPlantName(), "☀️ Sunlight check today", "Inspect", plantBitmap, "Check");
                        task.setDone(isDone);
                        careTaskList.add(task);
                        checkTaskAdded = true;
                    }
                }
            } else {
                if (!isWateredToday && p.getHealthPercentage() < 60) {
                    needWaterCount++;
                    countedForWater = true;
                }
            }

            if (p.getHealthPercentage() < 50) {
                if (!isWateredToday && !waterTaskAdded) {
                    CareTaskModel waterTask = new CareTaskModel(p.getId(), "Water " + p.getPlantName(), "⚠️ Low Health: needs water", "Water Now", plantBitmap, "Water");
                    waterTask.setDone(false);
                    careTaskList.add(waterTask);
                    waterTaskAdded = true;
                    if (!countedForWater) {
                        needWaterCount++;
                    }
                }

                if (!todayDate.equals(p.getLastFertilizedDate()) && !fertTaskAdded) {
                    CareTaskModel fertTask = new CareTaskModel(p.getId(), "Fertilize " + p.getPlantName(), "⚠️ Low Health: needs nutrients", "Feed Now", plantBitmap, "Fertilize");
                    fertTask.setDone(false);
                    careTaskList.add(fertTask);
                    fertTaskAdded = true;
                }

                if (!todayDate.equals(p.getLastCheckedDate()) && !checkTaskAdded) {
                    CareTaskModel sunTask = new CareTaskModel(p.getId(), "Sunlight for " + p.getPlantName(), "⚠️ Low Health: needs sunlight", "Inspect", plantBitmap, "Check");
                    sunTask.setDone(false);
                    careTaskList.add(sunTask);
                    checkTaskAdded = true;
                }
            }
        }

        if (todaysCareAdapter != null) {
            todaysCareAdapter.notifyDataSetChanged();
        }

        if (!plantList.isEmpty()) {
            int avgHealth = totalHealth / plantList.size();
            if (tvAvgHealth != null) tvAvgHealth.setText(avgHealth + "%");
        } else {
            if (tvAvgHealth != null) tvAvgHealth.setText("0%");
        }

        if (tvNeedWater != null) tvNeedWater.setText(String.valueOf(needWaterCount));
        if (tvPestAlerts != null) tvPestAlerts.setText(String.valueOf(plantsWithPests.size()));
    }

    private boolean isTimeReached(String prefTime) {
        if (prefTime == null) return true;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("Asia/Manila"));
            Date timeDate = sdf.parse(prefTime);
            if (timeDate == null) return true;

            TimeZone phTimeZone = TimeZone.getTimeZone("Asia/Manila");
            Calendar schedCal = Calendar.getInstance(phTimeZone);
            Calendar timeCal = Calendar.getInstance(phTimeZone);
            timeCal.setTime(timeDate);

            schedCal.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY));
            schedCal.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE));
            schedCal.set(Calendar.SECOND, 0);
            schedCal.set(Calendar.MILLISECOND, 0);

            Calendar currentCal = Calendar.getInstance(phTimeZone);
            return !currentCal.before(schedCal);
        } catch (Exception e) {
            return true;
        }
    }

    private boolean isTaskDue(String frequency, String lastDate) {
        if (frequency == null || "None".equalsIgnoreCase(frequency)) return false;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Manila"));
        String todayDate = sdf.format(new Date());

        if (todayDate.equals(lastDate)) return true;
        if (lastDate == null || lastDate.isEmpty()) return true;

        try {
            Date last = sdf.parse(lastDate);
            Date today = sdf.parse(todayDate);
            if (last == null || today == null) return true;

            long diffInMillis = Math.abs(today.getTime() - last.getTime());
            long diffInDays = diffInMillis / (24 * 60 * 60 * 1000);

            if ("Every Day".equalsIgnoreCase(frequency)) return diffInDays >= 1;
            if ("Every 2 Days".equalsIgnoreCase(frequency)) return diffInDays >= 2;
            if ("Every 3 Days".equalsIgnoreCase(frequency)) return diffInDays >= 3;
            if ("Weekly".equalsIgnoreCase(frequency) || "Every Week".equalsIgnoreCase(frequency)) return diffInDays >= 7;
            if ("Every 2 Weeks".equalsIgnoreCase(frequency)) return diffInDays >= 14;
            if ("Monthly".equalsIgnoreCase(frequency)) return diffInDays >= 30;

            return false;
        } catch (Exception e) {
            return true;
        }
    }

    private void startRealTimeClock() {
        clockRunnable = new Runnable() {
            @Override
            public void run() {
                updateLiveDateTimeAndGreeting();
                TimeZone phTimeZone = TimeZone.getTimeZone("Asia/Manila");
                Calendar cal = Calendar.getInstance(phTimeZone);
                if (cal.get(Calendar.SECOND) == 0) {
                    updateDashboardStats();
                }
                clockHandler.postDelayed(this, 1000);
            }
        };
        clockHandler.post(clockRunnable);
    }

    private void updateLiveDateTimeAndGreeting() {
        if (!isAdded()) return;
        TimeZone phTimeZone = TimeZone.getTimeZone("Asia/Manila");
        Calendar calendar = Calendar.getInstance(phTimeZone);
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("EEEE, MMMM d • h:mm a", new Locale("en", "PH"));
        dateTimeFormat.setTimeZone(phTimeZone);

        TextView tvDayTime = getView() != null ? getView().findViewById(R.id.tv_day_time) : null;
        if (tvDayTime != null) {
            tvDayTime.setText(dateTimeFormat.format(calendar.getTime()));
        }

        int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
        String greeting = (hourOfDay < 12) ? "Good Morning" : (hourOfDay < 17) ? "Good Afternoon" : "Good Evening";
        if (tvUserGreeting != null) {
            String displayName = (userFullName != null && !userFullName.isEmpty()) ? userFullName : "SmartGrower";
            tvUserGreeting.setText(greeting + ", " + displayName + "!");
        }
    }

    private void updateMockWeatherEngine() {
        if (!isAdded()) return;

        Random r = new Random();
        int temp = 16 + r.nextInt(21);
        int humidity;
        String selectedCondition;
        int iconResId;

        if (temp >= 32) {
            selectedCondition = "Sunny";
            iconResId = R.drawable.ic_sunny;
            humidity = 45 + r.nextInt(10);
        } else if (temp >= 27) {
            selectedCondition = "Partly Cloudy";
            iconResId = R.drawable.ic_partly_cloudy;
            humidity = 55 + r.nextInt(10);
        } else if (temp >= 22) {
            selectedCondition = "Cloudy";
            iconResId = R.drawable.ic_cloudy;
            humidity = 65 + r.nextInt(10);
        } else if (temp >= 18) {
            selectedCondition = "Light Rain";
            iconResId = R.drawable.ic_light_rain;
            humidity = 75 + r.nextInt(10);
        } else {
            selectedCondition = "Thunderstorm";
            iconResId = R.drawable.ic_thunderstorm;
            humidity = 85 + r.nextInt(11);
        }

        View view = getView();
        if (view != null) {
            TextView tvTemp = view.findViewById(R.id.tv_weather_temp);
            TextView tvDesc = view.findViewById(R.id.tv_weather_desc);
            TextView tvAdvice = view.findViewById(R.id.tv_watering_advice);
            ImageView ivIcon = view.findViewById(R.id.iv_weather_icon);

            if (tvTemp != null) tvTemp.setText(temp + "°C");
            if (tvDesc != null) tvDesc.setText(selectedCondition + " • Humidity " + humidity + "%");
            if (ivIcon != null) ivIcon.setImageResource(iconResId);

            if (tvAdvice != null) {
                if (selectedCondition.equals("Light Rain") || selectedCondition.equals("Thunderstorm")) {
                    tvAdvice.setText("Not ideal for watering (Rain expected)");
                } else if (selectedCondition.equals("Cloudy")) {
                    tvAdvice.setText("Good weather for watering (Low evaporation)");
                } else if (selectedCondition.equals("Partly Cloudy")) {
                    tvAdvice.setText("Great conditions for general plant care");
                } else if (selectedCondition.equals("Sunny") && temp > 30) {
                    tvAdvice.setText("Water early morning or evening");
                } else {
                    tvAdvice.setText("Better weather for watering");
                }
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        clockHandler.removeCallbacks(clockRunnable);
        if (shimmerFrameLayout != null) {
            shimmerFrameLayout.stopShimmer();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        clockHandler.post(clockRunnable);
        updateDashboardStats();
        if (shimmerFrameLayout != null && !isDataLoaded) {
            shimmerFrameLayout.startShimmer();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (diaryListener != null) diaryListener.remove();
    }
}