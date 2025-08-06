package com.team10.realmail;

public class HistoryListDisplayItem {
    public static final int TYPE_HEADER = 0;
    public static final int TYPE_ITEM = 1;

    public int type;
    public String deviceId; // for header
    public historyListItem item; // for item

    public HistoryListDisplayItem(int type, String deviceId, historyListItem item) {
        this.type = type;
        this.deviceId = deviceId;
        this.item = item;
    }
}
