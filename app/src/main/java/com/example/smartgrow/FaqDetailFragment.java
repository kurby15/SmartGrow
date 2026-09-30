package com.example.smartgrow;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class FaqDetailFragment extends Fragment {

    private static final String ARG_TITLE = "faq_title";
    private static final String ARG_DESCRIPTION = "faq_description";

    private String title;
    private String description;

    public FaqDetailFragment() {
        // Required empty public constructor
    }

    public static FaqDetailFragment newInstance(String title, String description) {
        FaqDetailFragment fragment = new FaqDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_DESCRIPTION, description);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            title = getArguments().getString(ARG_TITLE);
            description = getArguments().getString(ARG_DESCRIPTION);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_faq_detail, container, false);

        ImageView btnBack = view.findViewById(R.id.btn_back_faq);
        TextView tvTitle = view.findViewById(R.id.tv_faq_title_detail);
        TextView tvDescription = view.findViewById(R.id.tv_faq_desc_detail);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (getParentFragmentManager() != null) {
                    getParentFragmentManager().popBackStack();
                }
            });
        }

        if (tvTitle != null) {
            tvTitle.setText(title);
        }

        if (tvDescription != null) {
            tvDescription.setText(description);
        }

        return view;
    }
}
