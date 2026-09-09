package com.example.smartgrow.plants;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartgrow.R;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class TodaysCareAdapter extends RecyclerView.Adapter<TodaysCareAdapter.TaskViewHolder> {

    private List<CareTaskModel> taskList;
    private OnTaskActionListener actionListener;

    public interface OnTaskActionListener {
        void onActionClick(CareTaskModel task);
    }

    public TodaysCareAdapter(List<CareTaskModel> taskList, OnTaskActionListener actionListener) {
        this.taskList = taskList;
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_todays_care, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        CareTaskModel task = taskList.get(position);
        holder.tvTaskTitle.setText(task.getTitle());
        holder.tvTaskDue.setText(task.getDueText());
        holder.btnTaskAction.setText(task.getActionText());
        holder.ivTaskImage.setImageResource(task.getImageResId());

        holder.btnTaskAction.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onActionClick(task);
            }
        });
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTaskTitle, tvTaskDue;
        Button btnTaskAction;
        ImageView ivTaskImage;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTaskTitle = itemView.findViewById(R.id.tv_plant_name);
            tvTaskDue = itemView.findViewById(R.id.tv_plant_due);
            btnTaskAction = itemView.findViewById(R.id.btn_action);
            ivTaskImage = itemView.findViewById(R.id.iv_plant_image);
        }
    }
}