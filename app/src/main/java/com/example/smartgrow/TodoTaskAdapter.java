package com.example.smartgrow;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.checkbox.MaterialCheckBox;
import java.util.List;

public class TodoTaskAdapter extends RecyclerView.Adapter<TodoTaskAdapter.TaskViewHolder> {

    private List<TaskModel> taskList;
    private OnTaskStatusChangedListener listener;

    public interface OnTaskStatusChangedListener {
        void onTaskCompleted(TaskModel task);
    }

    public TodoTaskAdapter(List<TaskModel> taskList) {
        this.taskList = taskList;
    }

    public void setOnTaskStatusChangedListener(OnTaskStatusChangedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_todo_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        TaskModel task = taskList.get(position);

        holder.tvTaskTitle.setText(task.getTaskTitle());
        holder.tvTaskTime.setText(task.getTaskTime());

        holder.cbTaskStatus.setOnCheckedChangeListener(null);
        holder.cbTaskStatus.setChecked(task.isCompleted());

        holder.cbTaskStatus.setOnCheckedChangeListener((buttonView, isChecked) -> {
            task.setCompleted(isChecked);
            if (isChecked && listener != null) {
                listener.onTaskCompleted(task);
            }
        });
    }

    @Override
    public int getItemCount() {
        return taskList != null ? taskList.size() : 0;
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        MaterialCheckBox cbTaskStatus;
        TextView tvTaskTitle, tvTaskTime;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            cbTaskStatus = itemView.findViewById(R.id.cb_task_status);
            tvTaskTitle = itemView.findViewById(R.id.tv_task_title);
            tvTaskTime = itemView.findViewById(R.id.tv_task_time);
        }
    }
}