package com.example.smartgrow.plants;

 import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartgrow.BuildConfig;
import com.example.smartgrow.R;
import com.example.smartgrow.core.NotificationHelper;
import com.example.smartgrow.core.SharedPrefManager;
import com.example.smartgrow.profile.User;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
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

import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

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

    private FusedLocationProviderClient fusedLocationClient;
    private OkHttpClient httpClient;

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

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        httpClient = new OkHttpClient();

        initViews(view);
        startShimmer();

        if (currentUsername != null && !currentUsername.isEmpty() && !currentUsername.equals("unknown")) {
            userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUsername);
            fetchUserProfile();
        }

        listenToDiaryData();
        startRealTimeClock();
        checkLocationPermissionAndFetchWeather();

        return view;
    }

    private void initViews(View view) {
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

    private void checkLocationPermissionAndFetchWeather() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            fetchLocationAndWeather();
        }
    }

    private void fetchLocationAndWeather() {
        try {
            fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        fetchRealWeather(location.getLatitude(), location.getLongitude());
                    } else {
                        Log.e(TAG, "Location is null, using default weather display");
                        updateWeatherUI("N/A", "Location unavailable", 0, "Cloudy");
                    }
                }
            });
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission not granted", e);
        }
    }

    private void fetchRealWeather(double lat, double lon) {
        String apiKey = BuildConfig.WEATHER_API_KEY;
        String url = "https://api.openweathermap.org/data/2.5/weather?lat=" + lat + "&lon=" + lon + "&appid=" + apiKey + "&units=metric";

        Request request = new Request.Builder().url(url).build();
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Weather API call failed", e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String jsonData = response.body().string();
                        JSONObject jsonObject = new JSONObject(jsonData);
                        JSONObject main = jsonObject.getJSONObject("main");
                        double temp = main.getDouble("temp");
                        int humidity = main.getInt("humidity");
                        String condition = jsonObject.getJSONArray("weather").getJSONObject(0).getString("main");

                        new Handler(Looper.getMainLooper()).post(() -> {
                            updateWeatherUI(String.format(Locale.getDefault(), "%.0f°C", temp), condition, humidity, condition);
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing weather data", e);
                    }
                }
            }
        });
    }

    private void updateWeatherUI(String temp, String condition, int humidity, String conditionKey) {
        if (!isAdded()) return;
        View view = getView();
        if (view == null) return;

        TextView tvTemp = view.findViewById(R.id.tv_weather_temp);
        TextView tvDesc = view.findViewById(R.id.tv_weather_desc);
        TextView tvAdvice = view.findViewById(R.id.tv_watering_advice);
        ImageView ivIcon = view.findViewById(R.id.iv_weather_icon);

        if (tvTemp != null) tvTemp.setText(temp);
        if (tvDesc != null) tvDesc.setText(condition + " • Humidity " + humidity + "%");

        int iconResId = R.drawable.ic_cloudy;
        String advice = getString(R.string.weather_advice_default);

        if (conditionKey.equalsIgnoreCase("Clear")) {
            iconResId = R.drawable.ic_sunny;
            advice = getString(R.string.weather_advice_sunny_hot);
        } else if (conditionKey.equalsIgnoreCase("Clouds")) {
            iconResId = R.drawable.ic_partly_cloudy;
            advice = getString(R.string.weather_advice_partly_cloudy);
        } else if (conditionKey.contains("Rain") || conditionKey.contains("Drizzle")) {
            iconResId = R.drawable.ic_light_rain;
            advice = getString(R.string.weather_advice_rain);
        } else if (conditionKey.contains("Thunderstorm")) {
            iconResId = R.drawable.ic_thunderstorm;
            advice = getString(R.string.weather_advice_rain);
        }

        if (ivIcon != null) ivIcon.setImageResource(iconResId);
        if (tvAdvice != null) {
            String prefix = getString(R.string.sprout_ai_prefix);
            tvAdvice.setText(prefix + advice);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchLocationAndWeather();
            } else {
                Toast.makeText(getContext(), "Location permission denied. Weather will not be updated.", Toast.LENGTH_SHORT).show();
            }
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
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (shimmerFrameLayout != null) {
                shimmerFrameLayout.stopShimmer();
                shimmerFrameLayout.setVisibility(View.GONE);
            }
            if (mainContentContainer != null) {
                mainContentContainer.setVisibility(View.VISIBLE);
            }
            isDataLoaded = true;
        }, 1000);
    }

    private void listenToDiaryData() {
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
                    // Fetch real weather every 30 minutes instead of every minute to save API calls
                    if (cal.get(Calendar.MINUTE) % 30 == 0) {
                        checkLocationPermissionAndFetchWeather();
                    }
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
        checkLocationPermissionAndFetchWeather();
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