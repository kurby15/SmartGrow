package com.example.smartgrow.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import com.example.smartgrow.R;

public class AppPreferencesFragment extends Fragment {

    private SwitchCompat switchDarkMode;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_app_preferences, container, false);

        // Correct IDs mapped from fragment_app_preferences.xml
        switchDarkMode = view.findViewById(R.id.switch_theme);
        
        View btnBack = view.findViewById(R.id.btn_back_preferences);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (isAdded()) {
                    getParentFragmentManager().popBackStack();
                }
            });
        }

        SharedPreferences prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        
        if (switchDarkMode != null) {
            switchDarkMode.setChecked(prefs.getBoolean("dark_mode", false));
            switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean("dark_mode", isChecked).apply();
                if (isChecked) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                }
            });
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        View bottomNav = requireActivity().findViewById(R.id.bottom_navigation_bar);
        View mainHeader = requireActivity().findViewById(R.id.layout_top_header);


        if (bottomNav != null) bottomNav.setVisibility(View.GONE);
        if (mainHeader != null) mainHeader.setVisibility(View.GONE);

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        View bottomNav = requireActivity().findViewById(R.id.bottom_navigation_bar);
        View mainHeader = requireActivity().findViewById(R.id.layout_top_header);


        if (bottomNav != null) bottomNav.setVisibility(View.VISIBLE);
        if (mainHeader != null) mainHeader.setVisibility(View.VISIBLE);

    }
}
