package com.eva_karaoke.karaoke.dto;

import lombok.Data;

@Data
public class VideoInfo {
    private String url;
    private String title;
    private String thumbnailUrl;
    private String length;
    private String error;
    private String status;

    public static final String STATUS_PROGRESS = "progress";
    public static final String STATUS_COMPLETED = "completed";
    public static final String STATUS_ERROR = "error";

    public VideoInfo(String url) {
        this.url = url;
        this.status = STATUS_PROGRESS;
    }

    public void setError(String msg) {
        this.status = STATUS_ERROR;
        this.error = msg;
    }
}
