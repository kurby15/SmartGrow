package com.example.smartgrow;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<ChatMessageModel> messageList;

    public ChatAdapter(List<ChatMessageModel> messageList) {
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
        ChatMessageModel message = messageList.get(position);

        if (holder instanceof UserMessageViewHolder) {
            ((UserMessageViewHolder) holder).tvMessageText.setText(message.getMessageText());
            ((UserMessageViewHolder) holder).tvMessageTime.setText(message.getMessageTime());
        } else if (holder instanceof AiMessageViewHolder) {
            ((AiMessageViewHolder) holder).tvMessageText.setText(message.getMessageText());
            ((AiMessageViewHolder) holder).tvMessageTime.setText(message.getMessageTime());
        }
        // LoadingViewHolder doesn't need binding for now as it's static
    }

    @Override
    public int getItemCount() {
        return messageList != null ? messageList.size() : 0;
    }

    public static class UserMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessageText, tvMessageTime;
        public UserMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessageText = itemView.findViewById(R.id.tv_chat_message_text);
            tvMessageTime = itemView.findViewById(R.id.tv_chat_message_time);
        }
    }

    public static class AiMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessageText, tvMessageTime;
        public AiMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessageText = itemView.findViewById(R.id.tv_chat_message_text);
            tvMessageTime = itemView.findViewById(R.id.tv_chat_message_time);
        }
    }

    public static class LoadingViewHolder extends RecyclerView.ViewHolder {
        public LoadingViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}