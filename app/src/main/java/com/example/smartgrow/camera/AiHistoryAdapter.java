package com.example.smartgrow.camera;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.smartgrow.R;
import java.util.List;

public class AiHistoryAdapter extends RecyclerView.Adapter<AiHistoryAdapter.ViewHolder> {

    private final List<ChatSessionModel> sessionList;
    private final OnSessionClickListener listener;

    public interface OnSessionClickListener {
        void onSessionClick(ChatSessionModel session);
    }

    public AiHistoryAdapter(List<ChatSessionModel> sessionList, OnSessionClickListener listener) {
        this.sessionList = sessionList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ai_history_circle, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatSessionModel session = sessionList.get(position);

        if (holder.tvHistoryPlantName != null) {
            holder.tvHistoryPlantName.setText(session.getTitle());
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSessionClick(session);
            }
        });
    }

    @Override
    public int getItemCount() {
        return sessionList != null ? sessionList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvHistoryPlantName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHistoryPlantName = itemView.findViewById(R.id.tv_history_plant_name);
        }
    }
}