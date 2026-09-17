package com.example.smartgrow.plants;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartgrow.R;

import java.util.List;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder> {

    private List<ScheduleModel> scheduleList;

    public ScheduleAdapter(List<ScheduleModel> scheduleList) {
        this.scheduleList = scheduleList;
    }

    @NonNull
    @Override
    public ScheduleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_plant_schedule, parent, false);
        return new ScheduleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ScheduleViewHolder holder, int position) {
        ScheduleModel schedule = scheduleList.get(position);

        holder.tvTitle.setText(schedule.getTaskTitle());
        holder.tvTime.setText(schedule.getTaskTime());
        holder.tvNote.setText(schedule.getTaskNote());

        // Dynamic icon and color based on task type
        String title = schedule.getTaskTitle().toLowerCase();
        if (title.contains("water")) {
            holder.ivIcon.setImageResource(R.drawable.ic_water);
            holder.ivIcon.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.primary_green));
        } else if (title.contains("fertilize")) {
            holder.ivIcon.setImageResource(R.drawable.ic_soil);
            holder.ivIcon.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.primary_green));
        } else if (title.contains("sunlight")) {
            holder.ivIcon.setImageResource(R.drawable.ic_sunlight);
            holder.ivIcon.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.primary_green));
        } else {
            holder.ivIcon.setImageResource(R.drawable.ic_reminder);
            holder.ivIcon.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_secondary));
        }
    }

    @Override
    public int getItemCount() {
        return scheduleList.size();
    }

    public static class ScheduleViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvTime, tvNote;
        ImageView ivIcon;

        public ScheduleViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_schedule_title);
            tvTime = itemView.findViewById(R.id.tv_schedule_time);
            tvNote = itemView.findViewById(R.id.tv_schedule_note);
            ivIcon = itemView.findViewById(R.id.iv_schedule_icon);
        }
    }
}