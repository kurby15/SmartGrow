package com.example.smartgrow;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import java.util.Calendar;

public class AddLogBottomSheet extends BottomSheetDialogFragment {

    private EditText etLogDate;
    private ImageView imgCalendarIcon;
    private MaterialButton btnSubmitLog;

    public static AddLogBottomSheet newInstance() {
        return new AddLogBottomSheet();
    }

    // ⭐ HETO ANG LOGIC: Pinapayagan si user mag slide up at slide down via Touchscreen gesture!
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);

        if (dialog.getWindow() != null) {
            dialog.getWindow().getAttributes().windowAnimations = com.google.android.material.R.style.Animation_Material3_BottomSheetDialog;
        }

        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog bsd = (BottomSheetDialog) dialogInterface;
            View bottomSheet = bsd.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setHideable(true); // Pwedeng hilahin pababa gamit ang daliri para i-dismiss!
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED); // Automatic naka-open agad full views
            }
        });

        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_add_log_sheet, container, false);

        // Bind interactive views
        etLogDate = view.findViewById(R.id.et_log_date);
        imgCalendarIcon = view.findViewById(R.id.img_log_calendar_icon);
        btnSubmitLog = view.findViewById(R.id.btn_submit_log);

        // 📅 Calendar Selector Popup Logic
        View.OnClickListener dateListener = v -> showDatePickerDialog();
        etLogDate.setOnClickListener(dateListener);
        imgCalendarIcon.setOnClickListener(dateListener);

        // 💾 Submit Log Trigger Button
        btnSubmitLog.setOnClickListener(v -> {
            Toast.makeText(getContext(), "New Plant Growth Log Saved!", Toast.LENGTH_SHORT).show();
            dismiss(); // Isasara ang sheet pagka-save
        });

        return view;
    }

    private void showDatePickerDialog() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String formattedDate = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear);
                    etLogDate.setText(formattedDate);
                }, year, month, day);
        datePickerDialog.show();
    }
}