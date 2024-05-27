package com.eva_karaoke.karaoke.controller;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.eva_karaoke.karaoke.dto.VideoInfo;
import com.eva_karaoke.karaoke.service.VideoInfoService;

@RestController
@RequestMapping("/video-infos")
public class VideoInfoController {

    @Autowired
    private VideoInfoService videoInfoService;

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamSseMvc(@RequestParam String url) {
      VideoInfo videoInfo = new VideoInfo(url);

      SseEmitter emitter = new SseEmitter();

      try {
        emitter.send(videoInfo);
      } catch (IOException e) {
          emitter.completeWithError(e);
          return emitter;
      }

      CompletableFuture<VideoInfo> future = videoInfoService.getVideoInfo(videoInfo);

      future.thenAccept(updatedVideoInfo -> {
          try {
              emitter.send(updatedVideoInfo);

              if(!updatedVideoInfo.getStatus().equals(VideoInfo.STATUS_PROGRESS)) {
                emitter.complete();
              }

          } catch (IOException e) {
              emitter.completeWithError(e);
          }
      }).exceptionally(ex -> {
          try {
              emitter.send("Error: " + ex.getMessage());
              emitter.completeWithError(ex);
          } catch (IOException e) {
              e.printStackTrace();
          }
          return null;
      });

      return emitter;
    }
}
