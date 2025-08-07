package com.team10.realmail;


//created class for listing


public class historyListItem {

    private Boolean status;
    private String timeOfOccurence;

    //constrcutor
    public historyListItem(Boolean status, String timeOfOccurence) {
        this.status = status;
        this.timeOfOccurence = timeOfOccurence;
    }

    //getters
    public Boolean getStatus() {
        return status;
    }

    public String getTimeOfOccurence() {
        return timeOfOccurence;
    }
}
