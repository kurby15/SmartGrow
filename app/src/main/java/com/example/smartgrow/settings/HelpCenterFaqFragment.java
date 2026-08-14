package com.example.smartgrow.settings;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.example.smartgrow.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link HelpCenterFaqFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HelpCenterFaqFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public HelpCenterFaqFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment HelpCenterFaqFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static HelpCenterFaqFragment newInstance(String param1, String param2) {
        HelpCenterFaqFragment fragment = new HelpCenterFaqFragment();
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
        View view = inflater.inflate(R.layout.fragment_help_center_faq, container, false);

        view.findViewById(R.id.btn_back_help).setOnClickListener(v -> {
            getParentFragmentManager().popBackStack();
        });
        return view;
    }
}
