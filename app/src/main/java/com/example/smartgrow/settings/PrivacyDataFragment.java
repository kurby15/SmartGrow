package com.example.smartgrow.settings;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.example.smartgrow.R;

public class PrivacyDataFragment extends Fragment {

    public PrivacyDataFragment() {
        // Required empty public constructor
    }

    public static PrivacyDataFragment newInstance(String param1, String param2) {
        PrivacyDataFragment fragment = new PrivacyDataFragment();
        Bundle args = new Bundle();
        args.putString("param1", param1);
        args.putString("param2", param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_privacy_data, container, false);

        view.findViewById(R.id.btn_back_privacy_data).setOnClickListener(v -> {
            getParentFragmentManager().popBackStack();
        });
        return view;
    }
}
