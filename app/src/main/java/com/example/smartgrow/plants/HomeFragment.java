package com.example.smartgrow.plants;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
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

import com.example.smartgrow.MainActivity;
import com.example.smartgrow.R;
import com.example.smartgrow.camera.AiHistoryAdapter;
import com.example.smartgrow.core.SharedPrefManager;
import com.example.smartgrow.profile.User;
import com.google.android.material.bottomsheet.BottomSheetDialog;
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

        tvTaskReminder = view.findViewById(R.id.tv_task_reminder);
        tvActionViewAllHistory = view.findViewById(R.id.tv_action_view_all_history);
        tvDashboardLiveDateTime = view.findViewById(R.id.tv_dashboard_live_datetime);
        tvDashboardWeatherMock = view.findViewById(R.id.tv_dashboard_weather_mock);
        tvUserGreeting = view.findViewById(R.id.tv_user_greeting);
        rvAiHistory = view.findViewById(R.id.rv_ai_detected_history);
        rvMyPlantsList = view.findViewById(R.id.rv_my_plants_list);

        if (rvAiHistory != null) {
            rvAiHistory.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        }

        if (rvMyPlantsList != null) {
            rvMyPlantsList.setLayoutManager(new LinearLayoutManager(getContext()));
            rvMyPlantsList.setHasFixedSize(true);
        }

        homePlantAdapter = new HomePlantAdapter(plantList);
        rvMyPlantsList.setAdapter(homePlantAdapter);

        if (currentUsername != null && !currentUsername.isEmpty() && !currentUsername.equals("unknown")) {
            databaseReference = FirebaseDatabase.getInstance().getReference("users").child(currentUsername).child("plants");
            userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUsername);

            fetchPlantsFromFirebase();
            fetchUserProfile();
        } else {
            if (tvUserGreeting != null) tvUserGreeting.setText("Welcome back!");
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
                homePlantAdapter.notifyDataSetChanged();
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
        List<TaskModel> todayTasks = new ArrayList<>();

        for (PlantModel p : plantList) {
            totalHealth += p.getHealthPercentage();
            if (p.getHealthPercentage() < 60) {
                needWaterCount++;
                todayTasks.add(new TaskModel("Water " + p.getName() + " (Low Health Alert)", "Urgent", false));
            } else if (p.getHealthPercentage() < 85) {
                todayTasks.add(new TaskModel("Check " + p.getName() + " for issues", "Today", false));
            }

            if (p.getReminders() != null) {
                ReminderModel rem = p.getReminders();
                String taskTime = (rem.getPreferredTime() != null && !rem.getPreferredTime().isEmpty()) ? rem.getPreferredTime() : "Today";
                if (rem.getWateringSchedule() != null && !rem.getWateringSchedule().equals("None")) todayTasks.add(new TaskModel("Watering: " + p.getName(), taskTime, false));
                if (rem.getSunlightSchedule() != null && !rem.getSunlightSchedule().equals("None")) todayTasks.add(new TaskModel("Sunlight: " + p.getName(), taskTime, false));
                if (rem.getFertilizerSchedule() != null && !rem.getFertilizerSchedule().equals("None")) todayTasks.add(new TaskModel("Fertilizer: " + p.getName(), taskTime, false));
            }
        }

        if (!plantList.isEmpty()) {
            int avgHealth = totalHealth / plantList.size();
            if (tvAvgHealth != null) tvAvgHealth.setText(avgHealth + "%");
        } else {
            if (tvAvgHealth != null) tvAvgHealth.setText("0%");
        }

        if (tvNeedWater != null) tvNeedWater.setText(String.valueOf(needWaterCount));
        this.currentTasks = todayTasks;
        updateTaskReminderLink(todayTasks.size());
    }

    private void updateTaskReminderLink(int taskCount) {
        if (tvTaskReminder == null || !isAdded()) return;
        String fullText = "You have " + taskCount + " tasks pending today. See Task";
        SpannableString spannableString = new SpannableString(fullText);
        int startIndex = fullText.indexOf("See Task");
        if (startIndex != -1) {
            int endIndex = startIndex + "See Task".length();
            spannableString.setSpan(new ForegroundColorSpan(Color.parseColor("#0C6211")), startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannableString.setSpan(new UnderlineSpan(), startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannableString.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        tvTaskReminder.setText(spannableString);
    }

    private void setupClickListeners() {
        if (tvTaskReminder != null) tvTaskReminder.setOnClickListener(v -> showTodoBottomSheetDialog());
        if (tvActionViewAllHistory != null) {
            tvActionViewAllHistory.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).showAiChatAssistantBottomSheet();
            });
        }
    }

    private void showTodoBottomSheetDialog() {
        if (getContext() == null) return;
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext());
        View sheetView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_todo_sheet, null);
        bottomSheetDialog.setContentView(sheetView);

        RecyclerView rvTodoList = sheetView.findViewById(R.id.rv_todo_tasks_list);
        if (rvTodoList != null) {
            rvTodoList.setLayoutManager(new LinearLayoutManager(getContext()));
            TodoTaskAdapter taskAdapter;
            if (currentTasks.isEmpty()) {
                List<TaskModel> emptyTasks = new ArrayList<>();
                emptyTasks.add(new TaskModel("No pending tasks! All plants are healthy.", "Done", true));
                taskAdapter = new TodoTaskAdapter(emptyTasks);
            } else {
                taskAdapter = new TodoTaskAdapter(currentTasks);
                taskAdapter.setOnTaskStatusChangedListener(task -> {
                    if (task.isCompleted()) Toast.makeText(getContext(), "🌿 Task Done: " + task.getTaskTitle() + "! ✨", Toast.LENGTH_LONG).show();
                });
            }
            rvTodoList.setAdapter(taskAdapter);
        }
        bottomSheetDialog.show();
    }

    private void setupAiHistoryList() {
        if (rvAiHistory == null || !isAdded()) return;

        com.google.firebase.auth.FirebaseUser currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUser.getUid())
                .collection("ai_history")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded()) return;

                    List<com.example.smartgrow.camera.ChatSessionModel> sessionList = new ArrayList<>();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        com.example.smartgrow.camera.ChatSessionModel session = doc.toObject(com.example.smartgrow.camera.ChatSessionModel.class);
                        sessionList.add(session);
                    }

                    aiHistoryAdapter = new AiHistoryAdapter(sessionList, session -> {
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).showAiChatAssistantBottomSheet();
                        }
                    });

                    rvAiHistory.setAdapter(aiHistoryAdapter);
                });
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
