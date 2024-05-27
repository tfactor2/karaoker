package com.eva_karaoke.karaoke.repository;

import com.eva_karaoke.karaoke.model.Video;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoRepository extends JpaRepository<Video, Long> {
}
