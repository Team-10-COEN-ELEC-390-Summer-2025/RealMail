package com.example.realmail;

import com.google.type.DateTime;

import java.time.LocalDateTime;

public class historyListItem {

    private Boolean status;
    private String timeOfOccurence;

    public historyListItem(Boolean status, String timeOfOccurence) {
        this.status = status;
        this.timeOfOccurence = timeOfOccurence;
    }

    public Boolean getStatus() {
        return status;
    }

    public String getTimeOfOccurence() {
        return timeOfOccurence;
    }
}
