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
        // Inflates your XML item layout
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_history_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatSessionModel session = sessionList.get(position);

        if (holder.tvHistoryTitle != null) {
            String title = session.getTitle();
            if (title == null || title.isEmpty()) {
                title = "New Conversation";
            }
            holder.tvHistoryTitle.setText(title);
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
        TextView tvHistoryTitle;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Matches @id/tv_history_title from your XML layout
            tvHistoryTitle = itemView.findViewById(R.id.tv_history_title);
        }
    }
}