package com.example.smartgrow.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.smartgrow.R;

public class SupportInfoFragment extends Fragment {

    public SupportInfoFragment() {
        // Required empty public constructor
    }

    public static SupportInfoFragment newInstance(String param1, String param2) {
        SupportInfoFragment fragment = new SupportInfoFragment();
        Bundle args = new Bundle();
        args.putString("param1", param1);
        args.putString("param2", param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_support_info, container, false);

        view.findViewById(R.id.btn_back_support).setOnClickListener(v ->{
            if(getActivity() != null){
                getActivity().getOnBackPressedDispatcher().onBackPressed();
            }
        });
        view.findViewById(R.id.item_help_center).setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HelpCenterFaqFragment())
                    .addToBackStack(null)
                    .commit();
        });

        view.findViewById(R.id.item_contact_us).setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new ContactUsFragment())
                    .addToBackStack(null)
                    .commit();
        });

        view.findViewById(R.id.item_privacy_policy).setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new PrivacyPolicyFragment())
                    .addToBackStack(null)
                    .commit();
        });

        view.findViewById(R.id.item_terms_service).setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new TermOfServiceFragment())
                    .addToBackStack(null)
                    .commit();
        });

        view.findViewById(R.id.item_about_smartgrow).setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new AboutSmartGrowFragment())
                    .addToBackStack(null)
                    .commit();
        });

        return view;
    }
}
