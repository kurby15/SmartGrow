package com.example.smartgrow;

import android.view.LayoutInflater;
import android.view. View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ChatAssistantAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    final private List<ChatMessageModel> messageList;

    public ChatAssistantAdapter(List<ChatMessageModel> messageList) {
        this.messageList = messageList;
    }

    @Override
    public int getItemViewType(int position) {
        return messageList.get(position).getMessageType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ChatMessageModel.TYPE_USER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_right_user, parent, false);
            return new UserMessageViewHolder(view);
        } else if (viewType == ChatMessageModel.TYPE_LOADING) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_loading, parent, false);
            return new LoadingViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_left_ai, parent, false);
            return new AiMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessageModel model = messageList.get(position);
        if (holder instanceof UserMessageViewHolder) {
            ((UserMessageViewHolder) holder).tvMessage.setText(model.getMessageText());
            ((UserMessageViewHolder) holder).tvTime.setText(model.getMessageTime());
        } else if (holder instanceof AiMessageViewHolder) {
            ((AiMessageViewHolder) holder).tvMessage.setText(model.getMessageText());
            ((AiMessageViewHolder) holder).tvTime.setText(model.getMessageTime());
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    // View Holders Core Blocks
    static class AiMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;
        public AiMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tv_chat_message_text);
            tvTime = itemView.findViewById(R.id.tv_chat_message_time);
        }
    }

    static class UserMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;
        public UserMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tv_chat_message_text);
            tvTime = itemView.findViewById(R.id.tv_chat_message_time);
        }
    }

    static class LoadingViewHolder extends RecyclerView.ViewHolder {
        public LoadingViewHolder(@NonNull View itemView) { super(itemView); }
    }
}