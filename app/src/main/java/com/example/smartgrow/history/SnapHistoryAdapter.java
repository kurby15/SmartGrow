package com.example.smartgrow.history;

import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartgrow.R;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class SnapHistoryAdapter extends RecyclerView.Adapter<SnapHistoryAdapter.SnapViewHolder> {

    private final List<SnapHistoryModel> snapList;
    private final OnSnapClickListener listener;
    private List<String> gardenPlantNames = new ArrayList<>(); // Track items in garden

    public interface OnSnapClickListener {
        void onSnapClick(SnapHistoryModel snap);
        void onAddToGardenClick(SnapHistoryModel snap);
        void onEditNameClick(SnapHistoryModel snap);
        void onDeleteSnapClick(SnapHistoryModel snap);
    }

    public SnapHistoryAdapter(List<SnapHistoryModel> snapList, OnSnapClickListener listener) {
        this.snapList = snapList;
        this.listener = listener;
    }

    // Method to update the list of plants already in the user's garden
    public void setGardenPlantNames(List<String> gardenPlantNames) {
        this.gardenPlantNames = (gardenPlantNames != null) ? gardenPlantNames : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SnapViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_snap_history, parent, false);
        return new SnapViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SnapViewHolder holder, int position) {
        SnapHistoryModel snap = snapList.get(position);

        String plantName = snap.getPlantName() != null ? snap.getPlantName() : "Unknown Plant";
        holder.tvCommonName.setText(plantName);

        if (snap.getScientificName() != null && !snap.getScientificName().isEmpty()) {
            holder.tvScientificName.setText(snap.getScientificName());
            holder.tvScientificName.setVisibility(View.VISIBLE);
        } else {
            holder.tvScientificName.setVisibility(View.GONE);
        }

        if (snap.getImageBase64() != null && !snap.getImageBase64().isEmpty()) {
            Bitmap decodedBitmap = decodeBase64ToBitmap(snap.getImageBase64());
            if (decodedBitmap != null) {
                holder.ivPlantImage.setImageBitmap(decodedBitmap);
            } else {
                holder.ivPlantImage.setImageResource(R.drawable.ic_launcher_background);
            }
        } else {
            holder.ivPlantImage.setImageResource(R.drawable.ic_launcher_background);
        }

        // --- CHECK IF PLANT IS ALREADY IN MY GARDEN ---
        boolean isInGarden = isPlantInGarden(plantName);


        if (holder.fabAction != null) {
            if (isInGarden) {
                // Style as Disabled / Added state (Kulay gray tapos check icon)
                holder.fabAction.setCardBackgroundColor(Color.parseColor("#8E8E93"));
                if (holder.ivFabIcon != null) {
                    holder.ivFabIcon.setImageResource(R.drawable.ic_check); // Siguraduhing mayroon kang ganitong icon sa drawable
                }
                holder.fabAction.setEnabled(false);
                holder.fabAction.setOnClickListener(null);
            } else {
                // Style as Active Add state (Kulay green tapos plus icon)
                holder.fabAction.setCardBackgroundColor(Color.parseColor("#2E7D32"));
                if (holder.ivFabIcon != null) {
                    holder.ivFabIcon.setImageResource(R.drawable.ic_add_plus_green); // O kung ano mang plus icon ang gamit mo
                }
                holder.fabAction.setEnabled(true);
                holder.fabAction.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onAddToGardenClick(snap);
                    }
                });
            }
        }


        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSnapClick(snap);
            }
        });

        if (holder.ibMoreOptions != null) {
            holder.ibMoreOptions.setOnClickListener(v -> showPopupMenu(v, snap));
        }
    }

    private boolean isPlantInGarden(String plantName) {
        if (gardenPlantNames == null || plantName == null) return false;
        for (String name : gardenPlantNames) {
            if (name != null && name.equalsIgnoreCase(plantName.trim())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getItemCount() {
        return snapList.size();
    }

    private Bitmap decodeBase64ToBitmap(String base64Str) {
        try {
            byte[] decodedBytes = Base64.decode(base64Str, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void showPopupMenu(View view, SnapHistoryModel snap) {
        PopupMenu popup = new PopupMenu(view.getContext(), view);
        popup.inflate(R.menu.snap_history_item_menu);

        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_rename_snap) {
                if (listener != null) listener.onEditNameClick(snap);
                return true;
            } else if (itemId == R.id.action_delete) {
                if (listener != null) listener.onDeleteSnapClick(snap);
                return true;
            }
            return false;
            });

        popup.show();
    }

    static class SnapViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPlantImage, ivFabIcon;
        TextView tvCommonName, tvScientificName;
        ImageView ibMoreOptions;
        MaterialCardView fabAction;

        public SnapViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPlantImage = itemView.findViewById(R.id.iv_plant_image);
            tvCommonName = itemView.findViewById(R.id.tv_common_name);
            tvScientificName = itemView.findViewById(R.id.tv_scientific_name);
            ibMoreOptions = itemView.findViewById(R.id.ib_more_options);
            fabAction = itemView.findViewById(R.id.fab_action);


            if (fabAction != null) {
                ivFabIcon = fabAction.findViewById(R.id.iv_fab_icon);
            }
        }
    }
}