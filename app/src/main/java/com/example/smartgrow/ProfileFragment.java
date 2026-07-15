package com.example.smartgrow;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ProfileFragment extends Fragment {

    private TextView tvFullName, tvRank;
    private TextView tvChoice2, tvChoice3, tvChoice4;
    private DatabaseReference databaseReference;
    private String currentUsername;
    private SharedPreferences preferences; // Global variable para ma-access sa logout function

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Bind views
        tvFullName = view.findViewById(R.id.tv_user_display_name);
        tvRank = view.findViewById(R.id.tv_user_rank);
        tvChoice2 = view.findViewById(R.id.tv_choice2);
        tvChoice3 = view.findViewById(R.id.tv_choice3);
        tvChoice4 = view.findViewById(R.id.tv_choice4);

        // Click listeners for navigation
        view.findViewById(R.id.btn_preferences).setOnClickListener(v -> {
            if (isAdded()) {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new AppPreferencesFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });

        view.findViewById(R.id.btn_security).setOnClickListener(v -> {
            if (isAdded()) {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new AccountSecurityFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });

        view.findViewById(R.id.btn_support).setOnClickListener(v -> {
            if (isAdded()) {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new SupportInfoFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });

        // Initialize Shared Preferences safely
        if (getActivity() != null) {
            preferences = getActivity().getSharedPreferences("SmartGrowPrefs", Context.MODE_PRIVATE);
            currentUsername = preferences.getString("current_username", "");

            if (!currentUsername.isEmpty()) {
                databaseReference = FirebaseDatabase.getInstance().getReference("users").child(currentUsername);
                fetchUserData();
            }

            // Logout Listener na magpapakita ng iyong custom layout dialog
            view.findViewById(R.id.btn_logout).setOnClickListener(v -> {
                showCustomLogoutDialog();
            });
        }

        return view;
    }

    // Function para i-load at ipakita ang iyong custom dialog_logout XML layout
    private void showCustomLogoutDialog() {
        if (getContext() == null || getActivity() == null) return;

        // 1. I-inflate ang custom XML layout mo
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_logout, null);

        // 2. I-build ang AlertDialog gamit ang inflated view
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(dialogView);
        builder.setCancelable(true); // Pwedeng isara kapag pinindot sa labas ng dialog

        AlertDialog dialog = builder.create();

        // Pinapakinis nito ang sulok ng dialog kung may rounded corners ka sa XML (tinatanggal ang default black background shape)
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // 3. Hanapin ang mga buttons sa loob ng iyong custom XML layout
        // NOTE: Palitan mo ang R.id.btn_dialog_yes at R.id.btn_dialog_no kung iba ang ID na nilagay mo sa XML mo
        Button btnYes = dialogView.findViewById(R.id.btn_logout_yes);
        Button btnNo = dialogView.findViewById(R.id.btn_logout_no);

        // Kapag pinindot ang Yes / Logout
        if (btnYes != null) {
            btnYes.setOnClickListener(v -> {
                dialog.dismiss(); // Isara ang dialog window
                performLogout();  // Patakbuhin ang pag-clear ng session at redirect
            });
        }

        // Kapag pinindot ang No / Cancel
        if (btnNo != null) {
            btnNo.setOnClickListener(v -> {
                dialog.dismiss(); // Isara lang ang dialog
            });
        }

        dialog.show();
    }

    // Function na nag-aasikaso ng pagbura ng session at pag-redirect sa LoginActivity
    private void performLogout() {
        if (preferences != null && getActivity() != null) {
            // 1. I-clear ang SharedPreferences session ng SmartGrow
            SharedPreferences.Editor editor = preferences.edit();
            editor.clear();
            editor.apply();

            // 2. I-setup ang Intent para lumipat sa LoginActivity
            Intent intent = new Intent(getActivity(), LoginActivity.class);

            // Tinatanggal nito ang backstack para hindi na makabalik sa dashboard pag pinindot ang back button ng phone
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);

            // 3. Isara ang Main/Dashboard Activity
            getActivity().finish();
        }
    }

    private void fetchUserData() {
        if (databaseReference == null) return;

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Ensure fragment is active before accessing UI components
                if (!isAdded()) return;

                if (snapshot.exists()) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        String fullName = user.getFullName();

                        if (tvFullName != null) tvFullName.setText(fullName);

                        if (getActivity() != null && fullName != null) {
                            SharedPreferences preferences = getActivity().getSharedPreferences("SmartGrowPrefs", Context.MODE_PRIVATE);
                            preferences.edit().putString("user_fullname", fullName).apply();
                        }

                        // Choice 1 targets Rank
                        if (tvRank != null && user.getChoice1() != null) {
                            tvRank.setText(user.getChoice1());
                        }

                        // Choices 2, 3, 4 populate dynamic Interests
                        if (tvChoice2 != null && user.getChoice2() != null) {
                            tvChoice2.setText(user.getChoice2());
                        }
                        if (tvChoice3 != null && user.getChoice3() != null) {
                            tvChoice3.setText(user.getChoice3());
                        }
                        if (tvChoice4 != null && user.getChoice4() != null) {
                            tvChoice4.setText(user.getChoice4());
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(), "Error fetching data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}