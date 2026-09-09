package com.example.smartgrow.community;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.text.format.DateUtils;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.smartgrow.R;
import com.google.android.material.card.MaterialCardView;
import java.util.List;

public class CommunityPostAdapter extends RecyclerView.Adapter<CommunityPostAdapter.PostViewHolder> {

    private final List<CommunityPostModel> postList;
    private final String currentUserId;
    private OnPostInteractionListener listener;

    public interface OnPostInteractionListener {
        void onCommentClick(CommunityPostModel post);
        void onLikeClick(CommunityPostModel post);
        void onMoreClick(View view, CommunityPostModel post);
        void onUserClick(String username);
    }

    public CommunityPostAdapter(List<CommunityPostModel> postList, String currentUserId, OnPostInteractionListener listener) {
        this.postList = postList;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    private boolean showArchivedOnly = false;
    public void setShowArchivedOnly(boolean show) { this.showArchivedOnly = show; }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_forum_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        CommunityPostModel post = postList.get(position);

        holder.tvUsername.setText(post.getUsername());
        holder.tvContent.setText(post.getContent());
        holder.tvLikeCount.setText(String.valueOf(post.getLikesCount()));
        holder.tvCommentCount.setText(String.valueOf(post.getCommentsCount()));

        if (post.getTimestamp() != null) {
            CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                    post.getTimestamp(),
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS);
            holder.tvTime.setText(timeAgo);
        }


        loadProfileImage(post.getProfileImageUri(), holder.ivUserAvatar);

        View.OnClickListener userClickListener = v -> {
            if (listener != null) listener.onUserClick(post.getUserId());
        };
        holder.tvUsername.setOnClickListener(userClickListener);
        holder.ivUserAvatar.setOnClickListener(userClickListener);

        if (post.getPostImageUri() != null && !post.getPostImageUri().isEmpty()) {
            holder.cardPostImage.setVisibility(View.VISIBLE);
            displayPostImage(post.getPostImageUri(), holder.ivPostImage);
            
            holder.ivPostImage.setOnClickListener(v -> {
                showFullImageDialog(v.getContext(), post.getPostImageUri());
            });
        } else {
            holder.cardPostImage.setVisibility(View.GONE);
        }



        // Direktang i-check sa Firestore subcollection kung naka-like ang current user para sigurado
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("posts").document(post.getPostId())
                .collection("likes").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (holder.getAdapterPosition() != RecyclerView.NO_POSITION) {
                        if (documentSnapshot.exists()) {
                            holder.ivLikeIcon.setImageResource(R.drawable.ic_heart_filled);
                            holder.ivLikeIcon.setColorFilter(Color.RED);
                            holder.tvLikeCount.setTextColor(Color.RED);
                        } else {
                            // Babalik sa tamang theme color kapag na-unlike na
                            holder.ivLikeIcon.setImageResource(R.drawable.ic_heart);
                            holder.ivLikeIcon.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_secondary));
                            holder.tvLikeCount.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_secondary));

                        }
                    }
                });

        View.OnClickListener likeClickListener = v -> { if (listener != null) listener.onLikeClick(post); };
        holder.ivLikeIcon.setOnClickListener(likeClickListener);
        holder.tvLikeCount.setOnClickListener(likeClickListener);

        View.OnClickListener commentClickListener = v -> { if (listener != null) listener.onCommentClick(post); };
        holder.ivCommentIcon.setOnClickListener(commentClickListener);
        holder.tvCommentCount.setOnClickListener(commentClickListener);

        holder.btnMore.setOnClickListener(v -> { if (listener != null) listener.onMoreClick(v, post); });
        holder.btnMore.setVisibility(View.VISIBLE);
    }

    private void displayPostImage(String imageData, ImageView imageView) {
        try {
            if (imageData.length() > 1000) {
                byte[] decodedString = Base64.decode(imageData, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                Glide.with(imageView.getContext()).load(decodedByte).into(imageView);
            } else {
                Glide.with(imageView.getContext()).load(imageData).into(imageView);
            }
        } catch (Exception e) {
            imageView.setVisibility(View.GONE);
        }
    }

    private void loadProfileImage(String profileData, ImageView imageView) {
        if (profileData == null || profileData.isEmpty()) {
            imageView.setImageResource(R.drawable.ic_user);
            return;
        }
        try {
            if (profileData.length() > 500) {
                byte[] decodedString = Base64.decode(profileData, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                Glide.with(imageView.getContext()).load(decodedByte).circleCrop().into(imageView);
            } else {
                Glide.with(imageView.getContext()).load(profileData).placeholder(R.drawable.ic_user).circleCrop().into(imageView);
            }
        } catch (Exception e) {
            imageView.setImageResource(R.drawable.ic_user);
        }
    }

    private void showFullImageDialog(android.content.Context context, String imageData) {
        Dialog dialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_full_image);
        
        ImageView imageView = dialog.findViewById(R.id.iv_full_image_viewer);
        ImageButton btnClose = dialog.findViewById(R.id.btn_close_image);
        
        if (imageData.length() > 1000) {
            try {
                byte[] decodedString = Base64.decode(imageData, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                imageView.setImageBitmap(decodedByte);
            } catch (Exception e) {
                Toast.makeText(context, "Error loading image", Toast.LENGTH_SHORT).show();
            }
        } else {
            Glide.with(context).load(imageData).into(imageView);
        }
        
        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Override
    public int getItemCount() {
        return postList != null ? postList.size() : 0;
    }

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView tvUsername, tvTime, tvContent, tvLikeCount, tvCommentCount;
        ImageView ivUserAvatar, ivPostImage, ivLikeIcon, ivCommentIcon;
        MaterialCardView cardPostImage;
        ImageView btnMore;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            ivUserAvatar = itemView.findViewById(R.id.iv_post_user_avatar);
            tvUsername = itemView.findViewById(R.id.tv_post_username);
            tvTime = itemView.findViewById(R.id.tv_post_time);
            tvContent = itemView.findViewById(R.id.tv_post_content);
            cardPostImage = itemView.findViewById(R.id.card_post_image);
            ivPostImage = itemView.findViewById(R.id.iv_post_image);
            ivLikeIcon = itemView.findViewById(R.id.iv_like_icon);
            ivCommentIcon = itemView.findViewById(R.id.iv_comment_icon);
            tvLikeCount = itemView.findViewById(R.id.tv_like_count);
            tvCommentCount = itemView.findViewById(R.id.tv_comment_count);
            btnMore = itemView.findViewById(R.id.btn_post_more);


        }
    }
}
