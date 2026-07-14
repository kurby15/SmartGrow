package com.example.smartgrow;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class AboutSmartGrowFragment extends Fragment {

    public AboutSmartGrowFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_about_smart_grow, container, false);

        // Back Button
        view.findViewById(R.id.btn_back_about).setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        // Official Website Button
        view.findViewById(R.id.btn_official_website).setOnClickListener(v -> {
            // TODO: Add intent to open browser
            Toast.makeText(getContext(), "Opening website...", Toast.LENGTH_SHORT).show();
        });

        // Check for Updates Button
        view.findViewById(R.id.btn_rate_app).setOnClickListener(v -> {
            // TODO: Add logic to check updates or open playstore
            Toast.makeText(getContext(), "You are on the latest version!", Toast.LENGTH_SHORT).show();
        });

        return view;
    }
}