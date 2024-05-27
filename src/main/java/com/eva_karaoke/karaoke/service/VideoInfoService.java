package com.eva_karaoke.karaoke.service;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.eva_karaoke.karaoke.dto.VideoInfo;

@Service
public class VideoInfoService {
  private final VideoInfoFetcher videoInfoFetcher;

  @Autowired
  public VideoInfoService(VideoInfoFetcher titleFetcher) {
    this.videoInfoFetcher = titleFetcher;
  }

  @Async
  public CompletableFuture<VideoInfo> getVideoInfo(VideoInfo videoInfo) {
    return videoInfoFetcher.getVideoInfo(videoInfo);
  }
}