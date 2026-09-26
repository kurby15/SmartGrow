package com.example.smartgrow.plants;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.smartgrow.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class TodoListBottomSheet extends BottomSheetDialogFragment {

    private RecyclerView rvTasks;

    public static TodoListBottomSheet newInstance() {
        return new TodoListBottomSheet();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);

        if (dialog.getWindow() != null) {
            dialog.getWindow().getAttributes().windowAnimations = com.google.android.material.R.style.Animation_Material3_BottomSheetDialog;
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog bsd = (BottomSheetDialog) dialogInterface;
            View bottomSheet = bsd.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setHideable(true);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        });

        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_todo_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvTasks = view.findViewById(R.id.rv_todo_tasks_list);
        if (rvTasks != null) {
            rvTasks.setLayoutManager(new LinearLayoutManager(getContext()));
            rvTasks.setHasFixedSize(false);
            rvTasks.setNestedScrollingEnabled(false);
        }
    }
}
