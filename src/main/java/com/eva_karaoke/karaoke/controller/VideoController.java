package com.eva_karaoke.karaoke.controller;

import com.eva_karaoke.karaoke.dto.VideoInfo;
import com.eva_karaoke.karaoke.model.Video;
import com.eva_karaoke.karaoke.model.VideoProgressStatus;
import com.eva_karaoke.karaoke.repository.VideoRepository;
import com.eva_karaoke.karaoke.service.VideoCleanupService;
import com.eva_karaoke.karaoke.service.VideoProcessingService;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
// Pagination
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/videos")
public class VideoController {

  @Autowired
  private VideoRepository videoRepository;

  @Autowired
  private VideoProcessingService videoProcessingService;

  @Autowired
  private VideoCleanupService videoCleanupService;

  @PostMapping
  public ResponseEntity<Video> createVideo(@RequestBody Video video) {
    video.setProgressStatus(VideoProgressStatus.STATUS_QUEUED);
    Video savedVideo = videoRepository.save(video);
    videoProcessingService.processVideo(savedVideo.getId());
    return new ResponseEntity<>(savedVideo, HttpStatus.CREATED);
  }

  // Get a list of all videos with pagination
  @GetMapping
  public Page<Video> getAllVideos(@RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "50") int size) {
    // Enforce maximum page size of 100
    if (size > 100) {
        size = 100;
    }
    
    Pageable pageable = PageRequest.of(page, size);
    return videoRepository.findAll(pageable);
  }

  // Get a specific video by ID
  @GetMapping("/{id}")
  public ResponseEntity<Video> getVideoById(@PathVariable Long id) {
    return videoRepository.findById(id)
            .map(video -> new ResponseEntity<>(video, HttpStatus.OK))
            .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
  }

  @PostMapping("/{id}/restart")
  public ResponseEntity<Video> restartVideoProcessing(@PathVariable Long id, @RequestParam(defaultValue = "false") Boolean force) {
    Video video = videoRepository.findById(id).orElse(null);
    if (video == null) {
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    if(force) {
      video.setPause();
      videoRepository.save(video);
    }

    if (!video.getProgressStatus().equals(VideoProgressStatus.STATUS_PAUSED)) {
        return new ResponseEntity<>(HttpStatus.LOCKED);
    }
   
    video.setProgressStatus(VideoProgressStatus.STATUS_QUEUED);
    videoRepository.save(video);
    videoProcessingService.processVideo(video.getId());

    return new ResponseEntity<>(video, HttpStatus.OK);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteVideo(@PathVariable Long id) {
    if (!videoRepository.existsById(id)) {
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    videoCleanupService.cleanupVideo(id);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @GetMapping("/progress/{id}")
  public ResponseEntity<SseEmitter> streamVideoProgress(@PathVariable Long id) {
    Video video = videoRepository.findById(id).orElse(null);
    if (video == null) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    if(video.getProgressStatus() == VideoProgressStatus.STATUS_COMPLETED) {
      return ResponseEntity.status(HttpStatus.SEE_OTHER)
        .header("Location", "/videos/" + id)
        .build();
    }
    // 15 sec timeout 
    SseEmitter emitter = new SseEmitter(15000L);
    videoProcessingService.addEmitter(video, emitter);
    return new ResponseEntity<>(emitter, HttpStatus.OK);
  }
}
