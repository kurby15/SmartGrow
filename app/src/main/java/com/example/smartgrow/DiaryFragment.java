package com.example.smartgrow;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import java.util.ArrayList;
import java.util.List;

public class DiaryFragment extends Fragment {

    // 🟢 NILINIS NA: Tinanggal na ang header cards (cardDiaryNotification, cardDiaryGlobal, cardDiaryProfile)
    private MaterialCardView cardBtnAddNewDiary;
    private EditText etSearchPlants;
    private RecyclerView rvPlantDiaryList;

    // Lalagyan ng listahan ng pekeng halaman natin
    private List<PlantModel> mockPlantList;

    public DiaryFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_diary, container, false);

        // 🔗 Bind Search Context at Control Buttons
        etSearchPlants = view.findViewById(R.id.et_search_plants);
        cardBtnAddNewDiary = view.findViewById(R.id.card_btn_add_new_diary);

        // 🔗 Bind ang Target List View Container (RecyclerView)
        rvPlantDiaryList = view.findViewById(R.id.rv_plant_diary_list);
        if (rvPlantDiaryList != null) {
            rvPlantDiaryList.setLayoutManager(new LinearLayoutManager(getContext()));
            rvPlantDiaryList.setHasFixedSize(true);
        }

        // 🛠️ Setup Click Actions
        setupClickListeners();

        // 🌿 Dito na natin pagaganahin ang pag-load ng pekeng data!
        setupMockDiaryAdapter();

        return view;
    }

    private void setupClickListeners() {
        // 🟢 NILINIS NA LISTENERS: Ang natira na lang ay ang Add New Diary Button
        if (cardBtnAddNewDiary != null) {
            cardBtnAddNewDiary.setOnClickListener(v -> {
                AddPlantBottomSheetActivity addPlantSheet = new AddPlantBottomSheetActivity();
                addPlantSheet.show(getParentFragmentManager(), "AddPlantBottomSheetTag");
            });
        }
    }

    // 🚀 Dito mangyayari ang milagro ng pekeng data!
    private void setupMockDiaryAdapter() {
        mockPlantList = new ArrayList<>();

        // 📝 Mag-imbento tayo ng 3 halaman para kunwaring galing sa database
        mockPlantList.add(new PlantModel("My Healing Plant", "Lagundi", "15/01/2026", "Healthy"));
        mockPlantList.add(new PlantModel("Office Table Buddy", "Snake Plant", "02/03/2026", "Healthy"));
        mockPlantList.add(new PlantModel("Backyard Shrub", "Oregano", "20/04/2026", "Healthy"));

        // Isalpak na ang Adapter sa RecyclerView natin para lumitaw sa phone screen!
        MockDiaryAdapter adapter = new MockDiaryAdapter(mockPlantList);
        if (rvPlantDiaryList != null) {
            rvPlantDiaryList.setAdapter(adapter);
        }
    }
}