package com.team10.realmail;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class historyListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final List<HistoryListDisplayItem> displayList;
    private final Context context;

    public historyListAdapter(List<HistoryListDisplayItem> displayList, Context context) {
        this.displayList = displayList;
        this.context = context;
    }

    @Override
    public int getItemViewType(int position) {
        return displayList.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == HistoryListDisplayItem.TYPE_HEADER) {
            View view = LayoutInflater.from(context).inflate(R.layout.history_list_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.history_list_item, parent, false);
            return new ItemViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        HistoryListDisplayItem displayItem = displayList.get(position);
        if (displayItem.type == HistoryListDisplayItem.TYPE_HEADER) {
            HeaderViewHolder headerHolder = (HeaderViewHolder) holder;
            headerHolder.deviceIdHeader.setText("Device ID: " + displayItem.deviceId);
        } else {
            ItemViewHolder itemHolder = (ItemViewHolder) holder;
            historyListItem item = displayItem.item;
            itemHolder.status.setText("New Mail");
            String time = item.getTimeOfOccurence();
            String formattedTime = "-";
            if (time != null && !time.isEmpty()) {
                try {
                    java.time.Instant instant = java.time.Instant.parse(time);
                    java.time.ZonedDateTime zdt = instant.atZone(java.time.ZoneId.systemDefault());
                    java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy, hh:mm a");
                    formattedTime = formatter.format(zdt);
                } catch (Exception e) {
                    formattedTime = time;
                }
            }
            itemHolder.time_of_occurence.setText(formattedTime);
        }
    }

    @Override
    public int getItemCount() {
        return displayList.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView deviceIdHeader;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            deviceIdHeader = itemView.findViewById(R.id.device_id_header);
        }
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView status, time_of_occurence;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            status = itemView.findViewById(R.id.mailbox_status);
            time_of_occurence = itemView.findViewById(R.id.time_of_occurence);
        }
    }
}
