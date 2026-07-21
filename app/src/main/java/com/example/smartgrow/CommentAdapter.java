package com.example.smartgrow;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.format.DateUtils;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private final List<CommentModel> commentList;

    public CommentAdapter(List<CommentModel> commentList) {
        this.commentList = commentList;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        CommentModel comment = commentList.get(position);

        holder.tvUsername.setText(comment.getUsername());
        holder.tvContent.setText(comment.getContent());

        if (comment.getTimestamp() != null) {
            CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                    comment.getTimestamp(),
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS);
            holder.tvTime.setText(timeAgo);
        }

        // 👤 PROFILE PIC SYNC (Base64 or URL)
        loadProfileImage(comment.getProfileImageUri(), holder.ivAvatar);
    }

    private void loadProfileImage(String profileData, ImageView imageView) {
        if (profileData == null || profileData.isEmpty()) {
            imageView.setImageResource(R.drawable.ic_user);
            return;
        }

        try {
            if (profileData.length() > 500) { // Base64 detected
                byte[] decodedString = Base64.decode(profileData, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                Glide.with(imageView.getContext()).load(decodedByte).circleCrop().into(imageView);
            } else { // URL
                Glide.with(imageView.getContext()).load(profileData).placeholder(R.drawable.ic_user).circleCrop().into(imageView);
            }
        } catch (Exception e) {
            imageView.setImageResource(R.drawable.ic_user);
        }
    }

    @Override
    public int getItemCount() {
        return commentList != null ? commentList.size() : 0;
    }

    public static class CommentViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView ivAvatar;
        TextView tvUsername, tvContent, tvTime;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_comment_user_avatar);
            tvUsername = itemView.findViewById(R.id.tv_comment_username);
            tvContent = itemView.findViewById(R.id.tv_comment_content);
            tvTime = itemView.findViewById(R.id.tv_comment_time);
        }
    }
}
