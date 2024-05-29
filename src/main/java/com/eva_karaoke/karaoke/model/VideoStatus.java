package com.eva_karaoke.karaoke.model;

public enum VideoStatus {
    STATUS_VIDEO_DOWNLOADING("video:downloading"),
    STATUS_VIDEO_DOWNLOAD_FAILED("video:download_failed"),
    STATUS_AUDIO_EXTRACTING("audio:extracting"),
    STATUS_AUDIO_EXTRACTION_FAILED("audio:extraction_failed"),
    STATUS_AUDIO_SEPARATING("audio:separating"),
    STATUS_AUDIO_SEPARATION_FAILED("audio:separation_failed"),
    STATUS_COMPLETE("complete");

    private final String status;

    VideoStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return status;
    }
}
