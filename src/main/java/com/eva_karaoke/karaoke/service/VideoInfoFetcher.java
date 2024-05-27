package com.eva_karaoke.karaoke.service;

import java.util.concurrent.CompletableFuture;

import com.eva_karaoke.karaoke.dto.VideoInfo;

public interface VideoInfoFetcher {
    CompletableFuture<VideoInfo> getVideoInfo(VideoInfo videoInfo);
}

