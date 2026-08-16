package com.example.smartgrow.camera;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.smartgrow.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_history_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatSessionModel session = sessionList.get(position);

        // Set Title
        if (holder.tvHistoryTitle != null) {
            String title = session.getTitle();
            if (title == null || title.isEmpty()) {
                title = "New Conversation";
            }
            holder.tvHistoryTitle.setText(title);
        }

        // Set Formatted Date/Time (Optional)
        if (holder.tvHistoryDate != null && session.getTimestamp() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault());
            String formattedDate = sdf.format(new Date(session.getTimestamp()));
            holder.tvHistoryDate.setText(formattedDate);
        }

        // Click Listener
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
        TextView tvHistoryDate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHistoryTitle = itemView.findViewById(R.id.tv_history_title);
            tvHistoryDate = itemView.findViewById(R.id.tv_history_date); // Optional ID in XML
        }
    }
}