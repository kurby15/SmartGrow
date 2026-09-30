package com.example.smartgrow.settings;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.smartgrow.R;
import com.example.smartgrow.FaqDetailFragment;
import java.util.ArrayList;
import java.util.List;

public class HelpCenterFaqFragment extends Fragment {

    private List<FaqItem> faqList = new ArrayList<>();
    private TextView tvEmptyState;
    private LinearLayout faqContainer;

    public HelpCenterFaqFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_help_center_faq, container, false);

        tvEmptyState = view.findViewById(R.id.tv_empty_search_faq);
        EditText etSearch = view.findViewById(R.id.et_search_faq);
        // Kunin ang container para madaling ma-detect ang dividers
        faqContainer = view.findViewById(R.id.layout_faq_items_container);

        initFaqData();

        // Back button in header
        ImageView btnBackHelp = view.findViewById(R.id.btn_back_help);
        if (btnBackHelp != null) {
            btnBackHelp.setOnClickListener(v -> {
                if (getParentFragmentManager() != null) {
                    getParentFragmentManager().popBackStack();
                }
            });
        }

        // Setup individual FAQ clicks
        for (FaqItem item : faqList) {
            setupFaqClick(view, item);
        }

        // Search/Filter logic
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterFaqs(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        return view;
    }

    private void initFaqData() {
        faqList.clear();

        // Tinanggal na ang dividerId requirement para mas malinis ang XML structure
        faqList.add(new FaqItem(R.id.card_faq_1, "What is SmartGrow and how does it help?",
                "SmartGrow is an AI-powered mobile application designed to simplify plant management, assess plant health, identify plant species, and detect visual symptoms of pests and diseases using advanced image recognition."));

        faqList.add(new FaqItem(R.id.card_faq_2, "Are my plant scan records saved automatically?",
                "Yes, every time you perform a visual diagnosis, the details of your scan are automatically saved into your personal scan history log."));

        faqList.add(new FaqItem(R.id.card_faq_3, "Can I delete my posts in the Community Forum?",
                "Yes, you maintain full authority and control over the content you share in the Community Forum. Tap the 'More' menu on your post to edit or delete it."));

        faqList.add(new FaqItem(R.id.card_faq_4, "Can I use the application while offline?",
                "SmartGrow allows limited functionality without an active internet connection. You can browse locally saved data like your Plant Diary."));

        faqList.add(new FaqItem(R.id.card_faq_5, "How do I set up automated care reminders for my plants?",
                "Open your Digital Plant Diary, select a plant, and tap the Reminder Icon to set schedules for watering, fertilization, or sunlight."));

        faqList.add(new FaqItem(R.id.card_faq_6, "How do I add and manage plants in \"My Garden\"?",
                "After scanning, tap 'Save to My Garden' to create a profile. You can then monitor growth and update plant status anytime."));

        faqList.add(new FaqItem(R.id.card_faq_7, "What should I do if my plant scan image is blurry?",
                "Ensure the leaf is well-lit and centered. Avoid shadows. If blurry, retake the photo for better diagnostic accuracy."));

        faqList.add(new FaqItem(R.id.card_faq_8, "What happens to my data if SmartGrow is offline?",
                "SmartGrow operates on a hybrid architecture. View cached data offline, but use online access for live AI scanning and chat."));

        faqList.add(new FaqItem(R.id.card_faq_9, "How do I talk to the AI Plant Assistant?",
                "Access the AI Chat Assistant from the main menu or a completed scan screen to ask natural language questions about plant care."));

        faqList.add(new FaqItem(R.id.card_faq_10, "How does the AI Plant Health Diagnosis work?",
                "It uses Google Gemini AI to analyze leaf photos for discoloration, patterns, and lesions to identify species and diseases."));
    }

    private void filterFaqs(String query) {
        String lowerCaseQuery = query.toLowerCase().trim();
        boolean anyVisible = false;

        if (faqContainer == null) return;

        for (FaqItem item : faqList) {
            View cardView = getView().findViewById(item.cardId);

            if (cardView != null) {
                // Dynamically hanapin ang divider line na kasunod ng card
                int cardIndex = faqContainer.indexOfChild(cardView);
                View dividerView = null;
                if (cardIndex != -1 && cardIndex < faqContainer.getChildCount() - 1) {
                    View nextView = faqContainer.getChildAt(cardIndex + 1);
                    // Siguraduhing divider nga ito (walang ID) at hindi panibagong card
                    if (nextView.getId() == View.NO_ID) {
                        dividerView = nextView;
                    }
                }

                if (item.title.toLowerCase().contains(lowerCaseQuery) ||
                        item.content.toLowerCase().contains(lowerCaseQuery)) {

                    cardView.setVisibility(View.VISIBLE);
                    if (dividerView != null) dividerView.setVisibility(View.VISIBLE);
                    anyVisible = true;
                } else {
                    cardView.setVisibility(View.GONE);
                    if (dividerView != null) dividerView.setVisibility(View.GONE);
                }
            }
        }

        if (tvEmptyState != null) {
            tvEmptyState.setVisibility(anyVisible ? View.GONE : View.VISIBLE);
        }
    }

    private void setupFaqClick(View rootView, FaqItem item) {
        View card = rootView.findViewById(item.cardId);
        if (card != null) {
            card.setOnClickListener(v -> {
                FaqDetailFragment detailFragment = FaqDetailFragment.newInstance(item.title, item.content);
                if (getParentFragmentManager() != null) {
                    getParentFragmentManager().beginTransaction()
                            .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                            .replace(R.id.fragment_container, detailFragment)
                            .addToBackStack(null)
                            .commit();
                }
            });
        }
    }

    private static class FaqItem {
        int cardId;
        String title;
        String content;

        FaqItem(int cardId, String title, String content) {
            this.cardId = cardId;
            this.title = title;
            this.content = content;
        }
    }
}