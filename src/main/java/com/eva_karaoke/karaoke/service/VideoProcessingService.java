package com.eva_karaoke.karaoke.service;

import com.eva_karaoke.karaoke.model.Video;
import com.eva_karaoke.karaoke.model.VideoProgressStatus;
import com.eva_karaoke.karaoke.model.VideoStatus;
import com.eva_karaoke.karaoke.repository.VideoRepository;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteResultHandler;
import org.apache.commons.exec.LogOutputStream;
import org.apache.commons.exec.PumpStreamHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class VideoProcessingService {
  public static final int MAX_RETRY_ON_ERROR = 3;
  public static final String DOWNLOAD_VIDEO = "download_video";
  public static final String EXTRACT_AUDIO = "extract_audio";
  public static final String SEPARATE_AUDIO = "separate_audio";

  // private final ExecutorService executor = Executors.newSingleThreadExecutor();
  private final ConcurrentHashMap<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
  private final ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(1);

  @Autowired
  private VideoRepository videoRepository;

  @Async
  public void processVideo(Long videoId) {
    Video video = findVideoById(videoId);
    
    // change the state to progress and cleanup errors
    video.setProgressStatus(VideoProgressStatus.STATUS_PROGRESS);
    video.setRetries(0);
    video.setErrorMessage(null);
    
    // if there was an error on some step - we retry this step again
    switch (video.getStatus()) {
      case null:
      case STATUS_VIDEO_DOWNLOAD_FAILED:
        video.setStatus(VideoStatus.STATUS_VIDEO_DOWNLOADING);
        break;
      case STATUS_AUDIO_EXTRACTION_FAILED:
        video.setStatus(VideoStatus.STATUS_AUDIO_EXTRACTING);
        break;
      case STATUS_AUDIO_SEPARATION_FAILED:
        video.setStatus(VideoStatus.STATUS_AUDIO_SEPARATING);
        break;
      default: 
        break;
    }
    videoRepository.save(video);

    processVideoStep(video);
  }

  private void processVideoStep(Video video) {
    while (video.getProgressStatus().equals(VideoProgressStatus.STATUS_PROGRESS)) {
      switch (video.getStatus()) {
        case STATUS_VIDEO_DOWNLOADING:
          processCommand(Command.DOWNLOAD_VIDEO, video, VideoStatus.STATUS_AUDIO_EXTRACTING, VideoStatus.STATUS_VIDEO_DOWNLOAD_FAILED);
          break;
        case STATUS_AUDIO_EXTRACTING:
          processCommand(Command.EXTRACT_AUDIO, video, VideoStatus.STATUS_AUDIO_SEPARATING, VideoStatus.STATUS_AUDIO_EXTRACTION_FAILED);
          break;
        case STATUS_AUDIO_SEPARATING:
          processCommand(Command.SEPARATE_AUDIO, video, VideoStatus.STATUS_COMPLETE, VideoStatus.STATUS_AUDIO_SEPARATION_FAILED);
          break;
        default:
          break;
      }

      if(video.getStatus().equals(VideoStatus.STATUS_COMPLETE)) {
        video.setProgressStatus(VideoProgressStatus.STATUS_COMPLETED);
        videoRepository.save(video);
        sendProgress(video, 100);
        completeEmitter(video); 
        break;
      }
    }
  }

  private void processCommand(Command command, Video video, VideoStatus nextStatusOnComplete, VideoStatus statusOnFail) {
    try {
      switch (command) {
        case DOWNLOAD_VIDEO:
          runCommand("youtube-dl -f bestvideo+bestaudio[ext=m4a] --merge-output-format " + video.getVideoFileExt() + " -o " + video.getPathVideoFile() + " -k " + video.getUrl(), video);
          break;
        case EXTRACT_AUDIO:
          runCommand("ffmpeg -i " + video.getPathVideoFile() + " -q:a 0 -map a " + video.getPathAudioFile(), video);
          break;
        case SEPARATE_AUDIO:
          // Execute spleeter via a separate bash script to setup the envs
          // runCommand("scripts/spleeter_command.sh separate -o " + video.getPathSpleeterFolder() + " " + video.getPathAudioFile());
          // Using the model with a better quality: -n mdx_extra_q 
          runCommand("scripts/demucs_command.sh --out " + video.getPathSpleeterFolder() + " --two-stems=vocals --" + video.getAudioFileExt() + " -n mdx_extra_q --mp3-bitrate 320 --mp3-preset 2 -d cpu " + video.getPathAudioFile(), video);
          break;
      }

      video.setStatus(nextStatusOnComplete);
      video.setRetries(0);

    } catch (Exception e) {
      handleCommandError(video, statusOnFail, e);
    }

    videoRepository.save(video);
  }

  private void handleCommandError(Video video, VideoStatus statusOnFail, Exception e) {
    video.setRetries(video.getRetries() + 1);
    
    if (video.getRetries() > MAX_RETRY_ON_ERROR) {
      // if the number of attempts exceeded, we pause the progress and store the error
      video.setProgressStatus(VideoProgressStatus.STATUS_PAUSED);
      video.setStatus(statusOnFail);
      video.setErrorMessage(e.getMessage());
    }
  }

  private Video findVideoById(Long videoId) {
    return videoRepository.findById(videoId).orElseThrow(() -> new IllegalArgumentException("Video not found with id: " + videoId));
  }

  private void runCommand(String command, Video video) throws ExecuteException, IOException {
    System.out.println(command);
    CommandLine cmdLine = CommandLine.parse(command);
    DefaultExecutor executor = new DefaultExecutor();

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
    executor.setStreamHandler(streamHandler);

    AtomicBoolean processComplete = new AtomicBoolean(false);

    // Custom ExecuteResultHandler
    ExecuteResultHandler resultHandler = new ExecuteResultHandler() {
        @Override
        public void onProcessComplete(int exitValue) {
          processComplete.set(true);
          System.out.println("Process completed with exit code " + exitValue);
        }

        @Override
        public void onProcessFailed(ExecuteException e) {
          processComplete.set(true);
          System.out.println("Process failed");
          e.printStackTrace();
        }
    };

    try {
      // Start the process
      executor.execute(cmdLine, resultHandler);

      // Periodically read and print the output
      Runnable outputRunnable = () -> {
          while (!processComplete.get()) {
              try {
                  Thread.sleep(5000);
                  String output = outputStream.toString();
                  if (!output.isEmpty()) {
                    parseProgress(video, output);

                    System.out.println("Output:\n" + output);
                    outputStream.reset(); // Clear the buffer after reading
                  }
              } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
                  break;
              }
          }
          // Print any remaining output after process completion
          String remainingOutput = outputStream.toString();
          if (!remainingOutput.isEmpty()) {
            parseProgress(video, remainingOutput);
            System.out.println("Output:\n" + remainingOutput);
          }
      };

      // Start the output reading thread
      Thread outputThread = new Thread(outputRunnable);
      outputThread.start();

      // Wait for the process to complete
      while (!processComplete.get()) {
          Thread.sleep(1000); // Check every second if the process is complete
      }

      // Stop the output reading thread
      outputThread.interrupt();

    } catch (IOException | InterruptedException e) {
        e.printStackTrace();
    }
  }

  private void parseProgress(Video video, String output) {
    Pattern pattern = Pattern.compile("\\b(\\d{1,3})%\\b");
    Matcher matcher = pattern.matcher(output);
    if (matcher.find()) {
      int progress = Integer.parseInt(matcher.group(1));
      sendProgress(video, progress);
    }
  }

  private void sendProgress(Video video, int progress) {
    video.setProgressStepValue(progress);

    SseEmitter emitter = emitters.get(video.getId());
    if (emitter != null) {
        try {
            emitter.send(video);
        } catch (IOException e) {
          completeEmitter(video);
          emitters.remove(video.getId());
        }
    }
  }

  private void sendInitialProgress(Video video) {
    sendProgress(video, video.getProgressStepValue());
  }

  private void completeEmitter(Video video) {
    SseEmitter emitter = emitters.get(video.getId());
    if (emitter != null) {
      sendProgress(video, 100);
      emitter.complete();
      emitters.remove(video.getId());
    }
  }

  public void addEmitter(Video video, SseEmitter emitter) {
    emitters.put(video.getId(), emitter);
    emitter.onCompletion(() -> emitters.remove(video.getId()));
    emitter.onTimeout(() -> emitters.remove(video.getId()));
    
    scheduledExecutor.schedule(() -> {
      if (video.getProgressStatus() == VideoProgressStatus.STATUS_COMPLETED) {
        completeEmitter(video);
      }
    }, 1, TimeUnit.SECONDS);

    sendInitialProgress(video);
  }

  private enum Command {
    DOWNLOAD_VIDEO,
    EXTRACT_AUDIO,
    SEPARATE_AUDIO
  }
}

