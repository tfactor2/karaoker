package com.eva_karaoke.karaoke;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class KaraokeApplication {

	public static void main(String[] args) {
		SpringApplication.run(KaraokeApplication.class, args);
	}

}
