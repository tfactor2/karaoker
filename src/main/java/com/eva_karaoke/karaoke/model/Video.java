package com.eva_karaoke.karaoke.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class Video {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private String url;
	private String errorMessage;
	private int retries;

	@Enumerated(EnumType.STRING)
	private VideoProgressStatus progressStatus;

	@Enumerated(EnumType.STRING)
	private VideoStatus status;

	@Transient
	private int ProgressStepValue = 0;

	public static final String SPLEETER_VOCALS_FILENAME = "vocals.wav";
	public static final String SPLEETER_ACCOMPANIMENT_FILENAME = "accompaniment.wav";

	public void setPause() {
		switch (getStatus()) {
      case null:
      case STATUS_VIDEO_DOWNLOADING:
        setStatus(VideoStatus.STATUS_VIDEO_DOWNLOAD_FAILED);
        break;
      case STATUS_AUDIO_EXTRACTING:
        setStatus(VideoStatus.STATUS_AUDIO_EXTRACTION_FAILED);
        break;
      case STATUS_AUDIO_SEPARATING:
        setStatus(VideoStatus.STATUS_AUDIO_SEPARATION_FAILED);
        break;
      default: 
        break;
    }
		setProgressStatus(VideoProgressStatus.STATUS_PAUSED);
	}

	public String getFolder() {
		return "./ProcessFiles/Video_" + id.toString(); 
	}

	public String getPathVideoFile() {
		return getFolder() + "/video." + getVideoFileExt() ;
	}

	public String getVideoFileExt() {
		return "mp4";
	}

	public String getPathAudioFile() {
		return getFolder() + "/audio." + getAudioFileExt();
	}

	public String getAudioFileExt() {
		return "mp3";
	}

	public String getPathSpleeterFolder() {
		return getFolder() + "/spleeter_output";
	}

	public String getPathVocalFile() {
		return getPathSpleeterFolder() + "/" + SPLEETER_VOCALS_FILENAME;
	}

	public String getPathAccompanimentFile() {
		return getPathSpleeterFolder() + "/" + SPLEETER_ACCOMPANIMENT_FILENAME;
	}

	public int getProgressStepValue() {
		if (this.progressStatus == VideoProgressStatus.STATUS_COMPLETED) {
			return 100;
		} 

		if (this.progressStatus.equals(VideoProgressStatus.STATUS_QUEUED) || this.progressStatus.equals(VideoProgressStatus.STATUS_PAUSED)) {
			return 0;
		} 

		return this.ProgressStepValue; 
	}
}
