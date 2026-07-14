package com.example.smartgrow;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link AppPreferencesFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AppPreferencesFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    public AppPreferencesFragment() {
        // Required empty public constructor
    }

    public static AppPreferencesFragment newInstance(String param1, String param2) {
        AppPreferencesFragment fragment = new AppPreferencesFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // I-inflate ang binagong responsive xml layout
        View view = inflater.inflate(R.layout.fragment_app_preferences, container, false);

        // Click listener para sa back button gamit ang dispatcher
        view.findViewById(R.id.btn_back_preferences).setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getOnBackPressedDispatcher().onBackPressed();
            }
        });

        return view;
    }
}