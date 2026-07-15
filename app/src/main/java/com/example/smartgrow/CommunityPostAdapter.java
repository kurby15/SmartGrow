package com.example.smartgrow;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import java.util.List;

public class CommunityPostAdapter extends RecyclerView.Adapter<CommunityPostAdapter.PostViewHolder> {

    private final List<CommunityPostModel> postList;

    public CommunityPostAdapter(List<CommunityPostModel> postList) {
        this.postList = postList;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_forum_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        CommunityPostModel post = postList.get(position);

        // 1. I-bind ang Basic Texts gamit ang EKSAKTONG getters mula sa iyong Model
        holder.tvUsername.setText(post.getUsername()); // Gumagamit ng getUsername()
        holder.tvTime.setText(post.getTimeAgo());      // Gumagamit ng getTimeAgo()
        holder.tvContent.setText(post.getContent());   // Gumagamit ng getContent()
        holder.tvLikeCount.setText(String.valueOf(post.getLikesCount()));       // Gumagamit ng getLikesCount()
        holder.tvCommentCount.setText(String.valueOf(post.getCommentsCount())); // Gumagamit ng getCommentsCount()

        // 2. DYNAMIC IMAGE VISIBILITY LOGIC (Tugma sa getPostImageUri())
        if (post.getPostImageUri() != null && !post.getPostImageUri().isEmpty()) {
            // Ipakita ang card_post_image kapag may image URI string
            holder.cardPostImage.setVisibility(View.VISIBLE);

            try {
                Uri imageUri = Uri.parse(post.getPostImageUri());
                holder.ivPostImage.setImageURI(imageUri);
            } catch (Exception e) {
                e.printStackTrace();
                // Fallback kung sakaling hindi mabasa ang URI string
                holder.cardPostImage.setVisibility(View.GONE);
            }
        } else {
            // Itago ang image card frame kung walang larawan
            holder.cardPostImage.setVisibility(View.GONE);
            holder.ivPostImage.setImageURI(null);
        }
    }

    @Override
    public int getItemCount() {
        return postList != null ? postList.size() : 0;
    }

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView tvUsername, tvTime, tvContent, tvLikeCount, tvCommentCount;
        ImageView ivUserAvatar, ivPostImage;
        MaterialCardView cardPostImage;
        LinearLayout btnLike, btnComment;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);

            ivUserAvatar = itemView.findViewById(R.id.iv_post_user_avatar);
            tvUsername = itemView.findViewById(R.id.tv_post_username);
            tvTime = itemView.findViewById(R.id.tv_post_time);
            tvContent = itemView.findViewById(R.id.tv_post_content);

            cardPostImage = itemView.findViewById(R.id.card_post_image);
            ivPostImage = itemView.findViewById(R.id.iv_post_image);

            btnLike = itemView.findViewById(R.id.btn_post_like);
            btnComment = itemView.findViewById(R.id.btn_post_comment);
            tvLikeCount = itemView.findViewById(R.id.tv_like_count);
            tvCommentCount = itemView.findViewById(R.id.tv_comment_count);
        }
    }
}