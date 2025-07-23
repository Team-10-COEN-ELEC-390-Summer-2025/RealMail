package com.example.realmail;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.time.format.DateTimeFormatter;

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

        // Always show "New Mail" for status
        holder.status.setText("New Mail");

        // Format the timestamp string to a user-friendly date
        String time = item.getTimeOfOccurence();
        String formattedTime = "-";
        if (time != null && !time.isEmpty()) {
            try {
                java.time.Instant instant = java.time.Instant.parse(time);
                java.time.ZonedDateTime zdt = instant.atZone(java.time.ZoneId.systemDefault());
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy, hh:mm a");
                formattedTime = formatter.format(zdt);
            } catch (Exception e) {
                formattedTime = time; // fallback to raw string if parsing fails
            }
        }
        holder.time_of_occurence.setText(formattedTime);
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    static class historyViewHolder extends RecyclerView.ViewHolder {
        TextView status, time_of_occurence;


        public historyViewHolder(@NonNull View itemView) {
            super(itemView);

            status = itemView.findViewById(R.id.mailbox_status);
            time_of_occurence = itemView.findViewById(R.id.time_of_occurence);
        }
    }
}
