package com.example.smartgrow;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import android.content.Context;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;

public class HomeFragment extends Fragment {

    private RecyclerView rvMyPlantsList;
    private TextView tvTaskReminder;
    private TextView tvActionViewAllHistory;

    private TextView tvDashboardLiveDateTime;
    private TextView tvDashboardWeatherMock;
    private TextView tvUserGreeting;

    private RecyclerView rvAiHistory;
    private AiHistoryAdapter aiHistoryAdapter;
    private HomePlantAdapter homePlantAdapter;
    private List<PlantModel> plantList = new ArrayList<>();
    private List<TaskModel> currentTasks = new ArrayList<>();

    private DatabaseReference databaseReference, userRef;
    private String currentUsername;
    private String userFullName = "SmartGrower";

    private final Handler clockHandler = new Handler();
    private Runnable clockRunnable;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        SharedPreferences preferences = getActivity().getSharedPreferences("SmartGrowPrefs", Context.MODE_PRIVATE);
        currentUsername = preferences.getString("current_username", "");

        tvTaskReminder = view.findViewById(R.id.tv_task_reminder);
        tvActionViewAllHistory = view.findViewById(R.id.tv_action_view_all_history);
        tvDashboardLiveDateTime = view.findViewById(R.id.tv_dashboard_live_datetime);
        tvDashboardWeatherMock = view.findViewById(R.id.tv_dashboard_weather_mock);
        tvUserGreeting = view.findViewById(R.id.tv_user_greeting);

        rvAiHistory = view.findViewById(R.id.rv_ai_detected_history);
        if (rvAiHistory != null) {
            rvAiHistory.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        }

        rvMyPlantsList = view.findViewById(R.id.rv_my_plants_list);
        if (rvMyPlantsList != null) {
            rvMyPlantsList.setLayoutManager(new LinearLayoutManager(getContext()));
            rvMyPlantsList.setHasFixedSize(true);
        }

        homePlantAdapter = new HomePlantAdapter(plantList);
        rvMyPlantsList.setAdapter(homePlantAdapter);

        if (!currentUsername.isEmpty()) {
            databaseReference = FirebaseDatabase.getInstance().getReference("users").child(currentUsername).child("plants");
            userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUsername);
            
            fetchPlantsFromFirebase();
            fetchUserProfile();
        }

        setupClickListeners();
        startRealTimeClock();
        updateMockWeatherEngine();
        setupAiHistoryList();

        return view;
    }

    private void fetchUserProfile() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    User user = snapshot.getValue(User.class);
                    if (user != null && user.getFullName() != null) {
                        userFullName = user.getFullName();
                        updateLiveDateTimeAndGreeting(); // Update greeting immediately once name is loaded
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
                plantList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    PlantModel plant = dataSnapshot.getValue(PlantModel.class);
                    if (plant != null) {
                        plantList.add(plant);
                    }
                }
                homePlantAdapter.notifyDataSetChanged();
                updateDashboardStats();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateDashboardStats() {
        View view = getView();
        if (view == null) return;

        TextView tvTotalPlants = view.findViewById(R.id.tv_count_total_plants);
        if (tvTotalPlants != null) tvTotalPlants.setText(String.valueOf(plantList.size()));

        int needWaterCount = 0;
        int totalHealth = 0;
        List<TaskModel> todayTasks = new ArrayList<>();

        for (PlantModel p : plantList) {
            totalHealth += p.getHealthPercentage();
            if (p.getHealthPercentage() < 60) {
                needWaterCount++;
                todayTasks.add(new TaskModel("Water " + p.getName() + " (Low Health Alert)", "Urgent", false));
            } else if (p.getHealthPercentage() < 85) {
                todayTasks.add(new TaskModel("Check " + p.getName() + " for issues", "Today", false));
            }
        }

        if (!plantList.isEmpty()) {
            int avgHealth = totalHealth / plantList.size();
            TextView tvAvgHealth = view.findViewById(R.id.tv_value_avg_health);
            if (tvAvgHealth != null) tvAvgHealth.setText(avgHealth + "%");
        } else {
            // 🔄 RESET LOGIC: Kapag walang halaman, dapat 0% ang health dashboard
            TextView tvAvgHealth = view.findViewById(R.id.tv_value_avg_health);
            if (tvAvgHealth != null) tvAvgHealth.setText("0%");
        }

        TextView tvNeedWater = view.findViewById(R.id.tv_count_need_water);
        if (tvNeedWater != null) tvNeedWater.setText(String.valueOf(needWaterCount));

        this.currentTasks = todayTasks;
        updateTaskReminderLink(todayTasks.size());
    }

    private void updateTaskReminderLink(int taskCount) {
        String fullText = "You have " + taskCount + " tasks pending today. See Task";
        android.text.SpannableString spannableString = new android.text.SpannableString(fullText);
        int startIndex = fullText.indexOf("See Task");
        if (startIndex != -1) {
            int endIndex = startIndex + "See Task".length();
            spannableString.setSpan(new android.text.style.ForegroundColorSpan(Color.parseColor("#0C6211")), startIndex, endIndex, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannableString.setSpan(new android.text.style.UnderlineSpan(), startIndex, endIndex, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannableString.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), startIndex, endIndex, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (tvTaskReminder != null) tvTaskReminder.setText(spannableString);
    }

    private void setupClickListeners() {
        if (tvTaskReminder != null) {
            tvTaskReminder.setOnClickListener(v -> showTodoBottomSheetDialog());
        }
        if (tvActionViewAllHistory != null) {
            tvActionViewAllHistory.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).showAiChatAssistantBottomSheet();
                }
            });
        }
    }

    private void showTodoBottomSheetDialog() {
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext());
        View sheetView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_todo_sheet, null);
        bottomSheetDialog.setContentView(sheetView);

        RecyclerView rvTodoList = sheetView.findViewById(R.id.rv_todo_tasks_list);
        if (rvTodoList != null) {
            rvTodoList.setLayoutManager(new LinearLayoutManager(getContext()));
            rvTodoList.setHasFixedSize(true);

            if (currentTasks.isEmpty()) {
                List<TaskModel> emptyTasks = new ArrayList<>();
                emptyTasks.add(new TaskModel("No pending tasks! All plants are healthy.", "Done", true));
                rvTodoList.setAdapter(new TodoTaskAdapter(emptyTasks));
            } else {
                rvTodoList.setAdapter(new TodoTaskAdapter(currentTasks));
            }
        }
        bottomSheetDialog.show();
    }

    private void setupAiHistoryList() {
        if (rvAiHistory != null) {
            List<String> mockDetectedPlantsList = new ArrayList<>();
            mockDetectedPlantsList.add("Oregano");
            mockDetectedPlantsList.add("Sambong");
            mockDetectedPlantsList.add("Lagundi");
            aiHistoryAdapter = new AiHistoryAdapter(mockDetectedPlantsList);
            rvAiHistory.setAdapter(aiHistoryAdapter);
        }
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
        TimeZone phTimeZone = TimeZone.getTimeZone("Asia/Manila");
        Calendar calendar = Calendar.getInstance(phTimeZone);
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("EEEE, hh:mm a", new Locale("en", "PH"));
        dateTimeFormat.setTimeZone(phTimeZone);
        String currentDateTime = dateTimeFormat.format(calendar.getTime());
        if (tvDashboardLiveDateTime != null) tvDashboardLiveDateTime.setText(currentDateTime);
        int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
        String greeting = (hourOfDay < 12) ? "Good Morning!" : (hourOfDay < 17) ? "Good Afternoon!" : "Good Evening!";
        if (tvUserGreeting != null) tvUserGreeting.setText(greeting + " " + userFullName);
    }

    private void updateMockWeatherEngine() {
        if (tvDashboardWeatherMock == null) return;
        String[] weatherConditions = {"☀️ Sunny, 32°C", "⛅ Partly Cloudy, 29°C", "🌧️ Rainy, 26°C", "☁️ Overcast, 28°C"};
        tvDashboardWeatherMock.setText(weatherConditions[new Random().nextInt(weatherConditions.length)]);
    }

    @Override
    public void onPause() { super.onPause(); if (clockHandler != null) clockHandler.removeCallbacks(clockRunnable); }
    @Override
    public void onResume() { super.onResume(); if (clockHandler != null) clockHandler.post(clockRunnable); }
}