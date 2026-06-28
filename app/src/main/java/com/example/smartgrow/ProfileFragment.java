package com.example.smartgrow;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        tvFullName = view.findViewById(R.id.tv_user_display_name);
        tvRank = view.findViewById(R.id.tv_user_rank);
        tvChoice2 = view.findViewById(R.id.tv_choice2);
        tvChoice3 = view.findViewById(R.id.tv_choice3);
        tvChoice4 = view.findViewById(R.id.tv_choice4);

        SharedPreferences preferences = getActivity().getSharedPreferences("SmartGrowPrefs", Context.MODE_PRIVATE);
        currentUsername = preferences.getString("current_username", "");

        if (!currentUsername.isEmpty()) {
            databaseReference = FirebaseDatabase.getInstance().getReference("users").child(currentUsername);
            fetchUserData();
        }

        view.findViewById(R.id.btn_logout).setOnClickListener(v -> {
            SharedPreferences.Editor editor = preferences.edit();
            editor.clear();
            editor.apply();
            
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        return view;
    }

    private void fetchUserData() {
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        tvFullName.setText(user.getFullName());
                        
                        // Choice 1 goes to Rank
                        tvRank.setText(user.getChoice1());
                        
                        // Choices 2, 3, 4 go to Interests section
                        if (tvChoice2 != null) tvChoice2.setText(user.getChoice2());
                        if (tvChoice3 != null) tvChoice3.setText(user.getChoice3());
                        if (tvChoice4 != null) tvChoice4.setText(user.getChoice4());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error fetching data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}