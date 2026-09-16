package com.example.smartgrow.plants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartgrow.R;

import java.util.List;

public class PestAdapter extends RecyclerView.Adapter<PestAdapter.PestViewHolder> {

    private List<PestModel> pestList;

    public PestAdapter(List<PestModel> pestList) {
        this.pestList = pestList;
    }

    @NonNull
    @Override
    public PestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pest, parent, false);
        return new PestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PestViewHolder holder, int position) {
        PestModel pest = pestList.get(position);

        holder.tvPestName.setText(pest.getName());
        holder.tvPestDesc.setText(pest.getDescription());

        if (position == pestList.size() - 1) {
            holder.viewDivider.setVisibility(View.GONE);
        } else {
            holder.viewDivider.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return pestList.size();
    }

    public static class PestViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPestImage;
        TextView tvPestName;
        TextView tvPestDesc;
        View viewDivider;

        public PestViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPestImage = itemView.findViewById(R.id.iv_pest_image);
            tvPestName = itemView.findViewById(R.id.tv_pest_name);
            tvPestDesc = itemView.findViewById(R.id.tv_pest_desc);
            viewDivider = itemView.findViewById(R.id.view_pest_divider);
        }
    }
}