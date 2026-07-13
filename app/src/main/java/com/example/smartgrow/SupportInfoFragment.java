package com.example.smartgrow;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SupportInfoFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SupportInfoFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public SupportInfoFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment SupportInfoFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static SupportInfoFragment newInstance(String param1, String param2) {
        SupportInfoFragment fragment = new SupportInfoFragment();
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