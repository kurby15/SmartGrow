package com.example.smartgrow.plants;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartgrow.R;

import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private List<PlantHistoryModel> historyList;

    public HistoryAdapter(List<PlantHistoryModel> historyList) {
        this.historyList = historyList;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_plant_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        PlantHistoryModel history = historyList.get(position);

        holder.tvDate.setText(history.getDate());
        holder.tvHistoryTitle.setText(history.getAction());
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }



        public static class HistoryViewHolder extends RecyclerView.ViewHolder {
            TextView tvDate, tvHistoryTitle;

            public HistoryViewHolder(@NonNull View itemView) {
                super(itemView);
                tvDate = itemView.findViewById(R.id.tv_history_date);
                tvHistoryTitle = itemView.findViewById(R.id.tv_history_title);
            }
        }
    }
