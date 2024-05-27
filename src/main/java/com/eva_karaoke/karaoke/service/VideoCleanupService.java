package com.eva_karaoke.karaoke.service;

import com.eva_karaoke.karaoke.repository.VideoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class VideoCleanupService {
    @Autowired
    private VideoRepository videoRepository;

    @Async
    public void cleanupVideo(Long videoId) {
        String videoDir = "./ProcessFiles/Video_" + videoId;
        try {
            Files.walk(Paths.get(videoDir))
                .map(Path::toFile)
                .forEach(File::delete);
            Files.deleteIfExists(Paths.get(videoDir));
        } catch (IOException e) {
            e.printStackTrace();
        }
        videoRepository.deleteById(videoId);
    }
}
