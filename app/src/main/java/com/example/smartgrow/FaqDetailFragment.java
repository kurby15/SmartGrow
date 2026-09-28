package com.example.smartgrow;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

public class HelpCenterFragment extends Fragment {

    public HelpCenterFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_faq_detail, container, false);

        // Back button sa header
        ImageView btnBackHelp = view.findViewById(R.id.btn_back_help);
        if (btnBackHelp != null) {
            btnBackHelp.setOnClickListener(v -> {
                if (getParentFragmentManager() != null) {
                    getParentFragmentManager().popBackStack();
                }
            });
        }

        // FAQ Item 1
        setupFaqClick(view, R.id.card_faq_1,
                "What is SmartGrow and how does it help?",
                "SmartGrow is an AI-powered mobile application designed to simplify plant management, assess plant health, identify plant species, and detect visual symptoms of pests and diseases using advanced image recognition. The platform analyzes uploaded leaf photos to generate instant diagnostic breakdowns, severity ratings, and actionable treatment plans. SmartGrow helps plant owners, urban farmers, and hobbyists maintain thriving plants by providing a complete care ecosystem—combining an interactive AI Chat Assistant for personalized advice, a Digital Plant Diary (\"My Garden\") for tracking growth, automated care schedule reminders, and an interactive Community Forum for sharing advice with other growers."
        );

        // FAQ Item 2
        setupFaqClick(view, R.id.card_faq_2,
                "Are my plant scan records saved automatically?",
                "Yes, every time you perform a visual diagnosis, the details of your scan are automatically saved into your personal scan history log. For long-term tracking, you can tap the \"Save to My Garden\" button directly on the diagnostic results screen. Doing so stores the plant profile, diagnosis summary, and recommended treatment plan straight into your Digital Plant Diary on our secure servers. This allows you to revisit historical diagnoses, track recovery over time, and maintain photo records of your plant's growth"
        );

        // FAQ Item 3
        setupFaqClick(view, R.id.card_faq_3,
                "Can I delete my posts in the Community Forum?",
                "Yes, you maintain full authority and control over the content you share in the Community Forum. If you want to modify or remove a post you created, navigate to your post card in the community feed and tap the \"More\" overflow menu icon (represented by three dots). From there, you can edit your post text or select \"Delete\". Upon selecting delete, a confirmation prompt will appear to prevent accidental removal before permanently deleting the post entry and any attached photos from the platform."
        );

        // FAQ Item 4
        setupFaqClick(view, R.id.card_faq_4,
                "Can I use the application while offline?",
                "SmartGrow allows limited functionality without an active internet connection. When offline, you can open the app to browse locally saved data on your device, including your previously saved plant profiles in the Digital Plant Diary, historical scan logs, and static guides in the Help Center. However, online access is required for live features like real-time AI visual scanning, interactive AI Chat, photo uploads, cloud synchronization, and community forum interactions."
        );

        // FAQ Item 5
        setupFaqClick(view, R.id.card_faq_5,
                "How do I set up automated care reminders for my plants?",
                "To configure care schedules, open your Digital Plant Diary (\"My Garden\") and select the specific plant profile you wish to manage. Tap the Reminder Icon on the plant's detail page to set custom routine schedules tailored to that plant's needs, such as watering frequency, fertilization intervals, or sunlight exposure adjustments. Once set, SmartGrow will automatically deliver local push notifications directly to your device so you never miss a care routine."
        );

        // FAQ Item 6
        setupFaqClick(view, R.id.card_faq_6,
                "How do I add and manage plants in \"My Garden\"?",
                "After completing an image scan, tap the \"Save to My Garden\" button on the results screen. This action automatically creates a new plant profile inside your personal Digital Plant Diary, storing the initial scan data, diagnosis summary, and recommended care instructions into Firebase Firestore. Within \"My Garden,\" you can monitor individual plant progress over time, review historical scan logs, upload updated photos, and update plant status as recovery progresses"
        );

        // FAQ Item 7
        setupFaqClick(view, R.id.card_faq_7,
                "What should I do if my plant scan image is blurry or yields uncertain results?",
                "For optimal diagnostic accuracy, ensure the plant leaf or affected area is well-lit, sharply focused, and centered in the camera frame. Avoid strong shadows, glare, or extremely zoomed-out shots that capture too much background clutter. If an initial scan is inconclusive, retake the photo under clear natural lighting focusing specifically on the border between healthy and affected tissue, or pass the image directly into the AI Chat Assistant for further contextual analysis."
        );

        // FAQ Item 8
        setupFaqClick(view, R.id.card_faq_8,
                "Can I use SmartGrow offline, and what happens to my saved data?",
                "SmartGrow operates on a hybrid architecture. When offline, you can still open the app to browse locally cached data, including previously saved plant profiles in your Digital Plant Diary, past scan history logs, and static guides in the Help Center. However, active internet access is required to perform live AI image diagnoses, upload media to Firebase Storage, sync new updates to Firebase Firestore, and send or view Community Forum posts."
        );

        // FAQ Item 9
        setupFaqClick(view, R.id.card_faq_9,
                "How do I talk to the AI Plant Assistant?",
                "You can access the interactive AI Chat Assistant directly from the main navigation menu or directly from a completed scan result screen. Powered by the Gemini AI API, the chat assistant understands natural language queries, allowing you to ask tailored follow-up questions. You can request advice on fertilizer application, recommended watering schedules for your climate, specific pesticide dosages, or general troubleshooting for complex plant symptoms."
        );

        // FAQ Item 10
        setupFaqClick(view, R.id.card_faq_10,
                "How does the AI Plant Health Diagnosis work?",
                "To perform a diagnosis, capture a photo using your device’s camera or upload an existing image from your gallery. The image is securely processed and analyzed using the Google Gemini AI API, which evaluates visual symptoms such as leaf discoloration, spot patterns, surface lesions, and structural damage. SmartGrow identifies the plant species, detects underlying diseases or pest infestations, assesses the severity level, and generates a structured treatment plan complete with preventative care guidelines and organic or chemical recovery recommendations."
        );

        return view;
    }

    private void setupFaqClick(View rootView, int cardId, String title, String description) {
        RelativeLayout card = rootView.findViewById(cardId);
        if (card != null) {
            card.setOnClickListener(v -> {
                FaqDetailFragment detailFragment = FaqDetailFragment.newInstance(title, description);
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, detailFragment)
                        .addToBackStack(null)
                        .commit();
            });
        }
    }
}