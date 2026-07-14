package com.example.smartgrow;

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
            // Smooth Material 3 entrance animations para sa magandang transition UX
            dialog.getWindow().getAttributes().windowAnimations = com.google.android.material.R.style.Animation_Material3_BottomSheetDialog;

            // 🚀 KEYBOARD PROTECTION ENGINE: Automatic compression para hindi matakpan ng keyboard kung may internal edit fields ang bawat task item
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        // 🚀 SCREEN INTERACTION ENGINE FOR RECYCLERVIEW
        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog bsd = (BottomSheetDialog) dialogInterface;
            View bottomSheet = bsd.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setHideable(true);

                // Pinipilit ang sheet na bumukas sa full-screen capacity nito agad para hindi maging stiff ang scroll gestures
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true); // Nilalagpasan ang partial state para iwas-bitay na display status
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

        // 🔗 Map component instance properties
        rvTasks = view.findViewById(R.id.rv_todo_tasks_list);

        // 🛠️ Pro-Grade RecyclerView Optimizations para sa flexible windows
        rvTasks.setLayoutManager(new LinearLayoutManager(getContext()));

        // Pinapabilis nito ang layout measurement cycles habang nag-i-scroll
        rvTasks.setHasFixedSize(true);

        // Pinapagana nito ang seamless nested scrolling behavior kasama ang NestedScrollView ng XML container
        rvTasks.setNestedScrollingEnabled(false);

        // Ikonekta ang iyong adapter dito:
        // rvTasks.setAdapter(yourTodoAdapter);
    }
}