package com.ptho1504.video_service.controller;

import com.ptho1504.video_service.service.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/videos")
@Slf4j
@RequiredArgsConstructor
public class VideoController {
    private final VideoService videoService;

    /*
     *  Upload video file for a movie
     * Accept multipart file upload
     * */
    @PostMapping("/upload/{movieId}")
    public ResponseEntity<String> uploadVideo(
            @PathVariable String movieId,
            @RequestParam("file") MultipartFile file) throws IOException {
        log.info("Uploading video with ID: {} and size {} MB", movieId, file.getSize() / (1024 * 1024));

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Empty file");
        }

        String videoKey = videoService.uploadVideo(movieId, file);

        return ResponseEntity.ok(
                String.format("Video uploaded successfully! Key: " + videoKey + " - Encoding started automatically via Kafka")
        );
    }


}
