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

    private final List<String> detectedPlantsList;

    public AiHistoryAdapter(List<String> detectedPlantsList) {
        this.detectedPlantsList = detectedPlantsList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ai_history_circle, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String plantName = detectedPlantsList.get(position);

        if (holder.tvHistoryPlantName != null) {
            holder.tvHistoryPlantName.setText(plantName);
        }
    }

    @Override
    public int getItemCount() {
        return detectedPlantsList != null ? detectedPlantsList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvHistoryPlantName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHistoryPlantName = itemView.findViewById(R.id.tv_history_plant_name);
        }
    }
}
