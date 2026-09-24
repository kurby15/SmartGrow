package com.example.smartgrow.camera;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartgrow.MainActivity;
import com.example.smartgrow.R;
import com.example.smartgrow.plants.AIChatActivity;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<ChatMessageModel> messageList;

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
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == ChatMessageModel.TYPE_USER) {
            View view = inflater.inflate(R.layout.item_chat_right_user, parent, false);
            return new UserMessageViewHolder(view);
        } else if (viewType == ChatMessageModel.TYPE_LOADING) {
            View view = inflater.inflate(R.layout.item_chat_loading, parent, false);
            return new LoadingViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_chat_left_ai, parent, false);
            return new AiMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessageModel message = messageList.get(position);

        if (holder instanceof UserMessageViewHolder) {
            UserMessageViewHolder userHolder = (UserMessageViewHolder) holder;

            if (message.getMessageText() == null || message.getMessageText().trim().isEmpty()) {
                userHolder.cardText.setVisibility(View.GONE);
            } else {
                userHolder.cardText.setVisibility(View.VISIBLE);
                userHolder.tvMessageText.setText(message.getMessageText());
            }

            Bitmap displayBitmap = getOrDecodeBitmap(message);

            if (message.hasImage() && displayBitmap != null) {
                userHolder.cardImage.setVisibility(View.VISIBLE);
                userHolder.ivChatImage.setImageBitmap(displayBitmap);

                View.OnClickListener imageClickListener = v -> {
                    Dialog previewDialog = new Dialog(v.getContext());
                    previewDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

                    ImageView previewImageView = new ImageView(v.getContext());
                    previewImageView.setLayoutParams(new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT));
                    previewImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    previewImageView.setImageBitmap(displayBitmap);

                    previewDialog.setContentView(previewImageView);
                    if (previewDialog.getWindow() != null) {
                        previewDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
                        previewDialog.getWindow().setLayout(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT);
                    }

                    previewImageView.setOnClickListener(imgView -> previewDialog.dismiss());
                    previewDialog.show();
                };

                userHolder.ivChatImage.setOnClickListener(imageClickListener);
                userHolder.cardImage.setOnClickListener(imageClickListener);
            } else {
                userHolder.cardImage.setVisibility(View.GONE);
                userHolder.ivChatImage.setImageBitmap(null);
            }

            userHolder.tvMessageTime.setText(message.getMessageTime());

        } else if (holder instanceof AiMessageViewHolder) {
            AiMessageViewHolder aiHolder = (AiMessageViewHolder) holder;
            aiHolder.tvMessageText.setText(message.getMessageText());
            aiHolder.tvMessageTime.setText(message.getMessageTime());

            if (aiHolder.chipGroupSuggestions != null) {
                aiHolder.chipGroupSuggestions.removeAllViews();

                ArrayList<String> suggestions = message.getFollowUpSuggestions();

                if (suggestions != null && !suggestions.isEmpty()) {
                    aiHolder.chipGroupSuggestions.setVisibility(View.VISIBLE);

                    for (String textQuestion : suggestions) {
                        Chip customChip = new Chip(holder.itemView.getContext());
                        customChip.setText(textQuestion);
                        customChip.setCheckable(false);
                        customChip.setClickable(true);

                        customChip.setOnClickListener(v -> {
                            Activity activity = getActivityFromContext(v.getContext());
                            if (activity instanceof MainActivity) {
                                ((MainActivity) activity).submitFollowUpQuestion(textQuestion);
                            } else if (activity instanceof AIChatActivity) {
                                ((AIChatActivity) activity).submitFollowUpQuestion(textQuestion);
                            }
                        });

                        aiHolder.chipGroupSuggestions.addView(customChip);
                    }
                } else {
                    aiHolder.chipGroupSuggestions.setVisibility(View.GONE);
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        return messageList != null ? messageList.size() : 0;
    }

    private Bitmap getOrDecodeBitmap(ChatMessageModel message) {
        if (message.getImageBitmap() != null) {
            return message.getImageBitmap();
        }
        if (message.getImageBase64() != null && !message.getImageBase64().isEmpty()) {
            try {
                byte[] decodedBytes = Base64.decode(message.getImageBase64(), Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                message.setImageBitmap(bitmap); 
                return bitmap;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private Activity getActivityFromContext(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    public static class UserMessageViewHolder extends RecyclerView.ViewHolder {
        final TextView tvMessageText, tvMessageTime;
        final ImageView ivChatImage;
        final MaterialCardView cardImage, cardText;

        public UserMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessageText = itemView.findViewById(R.id.tv_chat_message_text);
            tvMessageTime = itemView.findViewById(R.id.tv_chat_message_time);
            ivChatImage = itemView.findViewById(R.id.iv_chat_image);
            cardImage = itemView.findViewById(R.id.card_chat_image);
            cardText = itemView.findViewById(R.id.card_chat_text);
        }
    }

    public static class AiMessageViewHolder extends RecyclerView.ViewHolder {
        final TextView tvMessageText, tvMessageTime;
        final ChipGroup chipGroupSuggestions;

        public AiMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessageText = itemView.findViewById(R.id.tv_chat_message_text);
            tvMessageTime = itemView.findViewById(R.id.tv_chat_message_time);
            chipGroupSuggestions = itemView.findViewById(R.id.cg_follow_up_suggestions);
        }
    }

    public static class LoadingViewHolder extends RecyclerView.ViewHolder {
        public LoadingViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}