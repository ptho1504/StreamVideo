package com.ptho1504.content_service.serivce;


import com.ptho1504.content_service.model.VideoStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class VideoUploadedEncodedEventConsumer {

    private final ContentService contentService;


    @KafkaListener(
            topics = "video.uploaded"
    )
    public void consumeVideoUploadedEvent(
            @Payload Map<String, Object> payload) {
        String movieId = payload.get("movieId").toString();
        String videoKey = payload.get("videoKey").toString();
        contentService.updateVideoKey(movieId, videoKey);
    }

    @KafkaListener(
            topics = "video.encoded"
    )
    public void consumeVideoEncodedEvent(
            @Payload Map<String, Object> payload) {
        String movieId = payload.get("movieId").toString();
        String hlsUrl = payload.get("hlsUrl").toString();
        boolean success = payload.get("success").toString().equals("true");

        if (success) {
            contentService.updateHslUrl(movieId, hlsUrl);
        } else {
            String errorMessage = payload.get("errorMessage").toString();
            contentService.updateMovieStatus(movieId, VideoStatus.FAILED);
        }
    }
}
