package com.example.smartgrow;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler; // For background timers
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
import com.google.android.material.card.MaterialCardView;
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
    private TextView tvActionViewAllHistory; // View Chat Logs component

    // Real-time Displays
    private TextView tvDashboardLiveDateTime;
    private TextView tvDashboardWeatherMock;
    private TextView tvUserGreeting;

    // Recycler adapters
    private RecyclerView rvAiHistory;
    private AiHistoryAdapter aiHistoryAdapter;
    private HomePlantAdapter homePlantAdapter;
    private List<PlantModel> plantDataList;

    // Ticking engine for clock
    private final Handler clockHandler = new Handler();
    private Runnable clockRunnable;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // 1. Bind structural dashboard controls (Wala na rito ang header buttons!)
        tvTaskReminder = view.findViewById(R.id.tv_task_reminder);
        tvActionViewAllHistory = view.findViewById(R.id.tv_action_view_all_history);

        // 2. Bind ang Real-time Displays mula sa XML
        tvDashboardLiveDateTime = view.findViewById(R.id.tv_dashboard_live_datetime);
        tvDashboardWeatherMock = view.findViewById(R.id.tv_dashboard_weather_mock);
        tvUserGreeting = view.findViewById(R.id.tv_user_greeting);

        // 3. Bind ang Horizontal AI History View
        rvAiHistory = view.findViewById(R.id.rv_ai_detected_history);
        if (rvAiHistory != null) {
            rvAiHistory.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        }

        // 4. Bind the Target List View
        rvMyPlantsList = view.findViewById(R.id.rv_my_plants_list);
        if (rvMyPlantsList != null) {
            rvMyPlantsList.setLayoutManager(new LinearLayoutManager(getContext()));
            rvMyPlantsList.setHasFixedSize(true);
        }

        // Execute setups
        setupClickListeners();
        setupTaskReminderLink();
        setupAiHistoryList();
        setupMockPlantAdapter();

        // Run system background clock & weather
        startRealTimeClock();
        updateMockWeatherEngine();

        return view;
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
        String greeting;
        if (hourOfDay >= 0 && hourOfDay < 12) {
            greeting = "Good Morning, Isha!";
        } else if (hourOfDay >= 12 && hourOfDay < 17) {
            greeting = "Good Afternoon, Isha!";
        } else {
            greeting = "Good Evening, Isha!";
        }
        if (tvUserGreeting != null) tvUserGreeting.setText(greeting);
    }

    private void updateMockWeatherEngine() {
        if (tvDashboardWeatherMock == null) return;
        String[] weatherConditions = {"☀️ Sunny, 32°C", "⛅ Partly Cloudy, 29°C", "🌧️ Rainy, 26°C", "☁️ Overcast, 28°C"};
        int randomIndex = new Random().nextInt(weatherConditions.length);
        tvDashboardWeatherMock.setText(weatherConditions[randomIndex]);
    }

    private void setupClickListeners() {
        if (tvTaskReminder != null) {
            tvTaskReminder.setOnClickListener(v -> showTodoBottomSheetDialog());
        }

        // ⚡ INAYOS NA KONEKSYON: Kapag pinindot ang View Chat Logs sa dashboard,
        // tatawagin nito ang safe at iisang showAiChatAssistantBottomSheet() na nasa MainActivity
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

            List<TaskModel> todoList = new ArrayList<>();
            todoList.add(new TaskModel("Water Lagundi (Cough & Asthma Relief)", "10:00 AM", false));
            todoList.add(new TaskModel("Check Yerba Buena leaves for fungal disease", "02:30 PM", false));
            todoList.add(new TaskModel("Apply fertilizer to Sambong plant", "04:00 PM", true));

            TodoTaskAdapter todoAdapter = new TodoTaskAdapter(todoList);
            rvTodoList.setAdapter(todoAdapter);
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

    private void setupTaskReminderLink() {
        String fullText = "You have 2 tasks pending today. See Task";
        android.text.SpannableString spannableString = new android.text.SpannableString(fullText);
        int startIndex = fullText.indexOf("See Task");
        int endIndex = startIndex + "See Task".length();
        if (startIndex != -1) {
            spannableString.setSpan(new android.text.style.ForegroundColorSpan(Color.parseColor("#0C6211")), startIndex, endIndex, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannableString.setSpan(new android.text.style.UnderlineSpan(), startIndex, endIndex, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannableString.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), startIndex, endIndex, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (tvTaskReminder != null) tvTaskReminder.setText(spannableString);
    }

    private void setupMockPlantAdapter() {
        plantDataList = new ArrayList<>();
        plantDataList.add(new PlantModel("Lagundi", "Vitex negundo", "May 25, 2026", "Healthy", "Cough & Asthma Relief", 92));
        plantDataList.add(new PlantModel("Sambong", "Blumea balsamifera", "May 26, 2026", "Healthy", "Kidney Stones Relief", 85));
        plantDataList.add(new PlantModel("Yerba Buena", "Clinopodium douglasii", "May 28, 2026", "Diseased", "Minty Fresh / Cough", 33));

        homePlantAdapter = new HomePlantAdapter(plantDataList);
        if (rvMyPlantsList != null) rvMyPlantsList.setAdapter(homePlantAdapter);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (clockHandler != null && clockRunnable != null) {
            clockHandler.removeCallbacks(clockRunnable);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (clockHandler != null && clockRunnable != null) {
            clockHandler.removeCallbacks(clockRunnable);
            clockHandler.post(clockRunnable);
        }
    }
}