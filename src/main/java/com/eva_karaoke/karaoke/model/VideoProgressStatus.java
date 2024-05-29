package com.eva_karaoke.karaoke.model;

public enum VideoProgressStatus {
    STATUS_QUEUED("queued"),
    STATUS_PROGRESS("progress"),
    STATUS_PAUSED("paused"),
    STATUS_COMPLETED("completed");

    private final String progressStatus;

    VideoProgressStatus(String progressStatus) {
        this.progressStatus = progressStatus;
    }

    public String getProgressStatus() {
        return progressStatus;
    }

    @Override
    public String toString() {
        return progressStatus;
    }
}
