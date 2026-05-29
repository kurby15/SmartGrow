package com.example.smartgrow;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

public class EditPlantBottomSheet extends BottomSheetDialogFragment {

    private EditText etEditName, etEditSpecies, etEditDate;
    private Spinner spinnerEditStatus;
    private MaterialButton btnSave;

    // Kunin ang mga default values na pinasa mula sa card list
    private String currentName, currentSpecies, currentDate, currentStatus;

    public static EditPlantBottomSheet newInstance(String name, String species, String date, String status) {
        EditPlantBottomSheet fragment = new EditPlantBottomSheet();
        Bundle args = new Bundle();
        args.putString("key_name", name);
        args.putString("key_species", species);
        args.putString("key_date", date);
        args.putString("key_status", status);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentName = getArguments().getString("key_name");
            currentSpecies = getArguments().getString("key_species");
            currentDate = getArguments().getString("key_date");
            currentStatus = getArguments().getString("key_status");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_edit_plant_sheet, container, false);

        // Bind layouts
        etEditName = view.findViewById(R.id.et_edit_plant_name);
        etEditSpecies = view.findViewById(R.id.et_edit_plant_species);
        etEditDate = view.findViewById(R.id.et_edit_plant_date);
        spinnerEditStatus = view.findViewById(R.id.spinner_edit_plant_status);
        btnSave = view.findViewById(R.id.btn_save_plant_changes);

        // I-set up ang Spinner drop-down options
        String[] statusOptions = {"Healthy", "Diseased", "Monitoring"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, statusOptions);
        spinnerEditStatus.setAdapter(adapter);

        // 🌟 AUTO-FILL: Ilagay agad ang lumang details sa mga kahon para pwedeng baguhin
        etEditName.setText(currentName);
        etEditSpecies.setText(currentSpecies);
        etEditDate.setText(currentDate);

        // Auto-select status sa spinner
        if (currentStatus != null) {
            for (int i = 0; i < statusOptions.length; i++) {
                if (statusOptions[i].equalsIgnoreCase(currentStatus)) {
                    spinnerEditStatus.setSelection(i);
                    break;
                }
            }
        }

        // Click actions
        btnSave.setOnClickListener(v -> {
            // Dito mangyayari ang Firebase update logic mamaya ng mga backend coders mo
            dismiss(); // Isasara ang sheet pagka-save
        });

        return view;
    }
}