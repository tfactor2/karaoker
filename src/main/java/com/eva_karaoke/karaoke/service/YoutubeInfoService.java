package com.eva_karaoke.karaoke.service;

import org.springframework.stereotype.Service;

import com.eva_karaoke.karaoke.dto.VideoInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class YoutubeInfoService implements VideoInfoFetcher {

  @Override
  public CompletableFuture<VideoInfo> getVideoInfo(VideoInfo videoInfo) {
    return CompletableFuture.supplyAsync(() -> getVideoInfoSync(videoInfo) );
  }

  private VideoInfo getVideoInfoSync(VideoInfo videoInfo) {

    try {
      ProcessBuilder processBuilder = new ProcessBuilder("youtube-dl", "--get-title", "--get-duration", "--get-thumbnail", videoInfo.getUrl());
      processBuilder.redirectErrorStream(true);

      Process process = processBuilder.start();

      // Capture the output
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
        // Filter out warning messages
        String output = reader.lines()
                              .filter(line -> !line.startsWith("WARNING:"))
                              .collect(Collectors.joining("\n"));
        process.waitFor();

        String[] info = output.split("\n");
        
        if (info.length >= 3) {
            videoInfo.setTitle(info[0]);
            videoInfo.setThumbnailUrl(info[1]);
            videoInfo.setLength(info[2]);
            videoInfo.setStatus(VideoInfo.STATUS_COMPLETED);
        } else {
          System.out.println("getVideoInfoSync Info: " + info);
          videoInfo.setError("Failed to fetch video information");
        }

      } catch (IOException | InterruptedException e) {
        System.out.println(e.getMessage());
        videoInfo.setError("Failed to fetch video information");
      }
    } catch (IOException e) {
      System.out.println(e.getMessage());
      videoInfo.setError("Failed to fetch video information");
    }

    return videoInfo;
  }
}