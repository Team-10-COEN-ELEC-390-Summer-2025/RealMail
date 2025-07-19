package com.example.realmail;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class historyListAdapter extends RecyclerView.Adapter<historyListAdapter.historyViewHolder> {

    private final List<historyListItem> historyList;

    private final Context context;

    public historyListAdapter(List<historyListItem> historyList, Context context) {
        this.historyList = historyList;
        this.context = context;
    }

    @NonNull
    @Override
    public historyListAdapter.historyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.history_list_item, parent, false);
        return new historyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull historyListAdapter.historyViewHolder holder, int position) {
            historyListItem item = historyList.get(position);

            if(item.getStatus()){
                holder.status.setText("Opened");
            } else {
                holder.status.setText("Closed");
            }

            holder.time_of_occurence.setText(item.getTimeOfOccurence().toString());
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    static class historyViewHolder extends RecyclerView.ViewHolder{
        TextView status, time_of_occurence;


        public historyViewHolder(@NonNull View itemView) {
            super(itemView);

            status = itemView.findViewById(R.id.mailbox_status);
            time_of_occurence = itemView.findViewById(R.id.time_of_occurence);
        }
    }
}
