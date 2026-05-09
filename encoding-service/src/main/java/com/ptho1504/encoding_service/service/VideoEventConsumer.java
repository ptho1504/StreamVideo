package com.ptho1504.encoding_service.service;


import com.ptho1504.encoding_service.event.VideoUploadedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class VideoEventConsumer {
    private final EncodingService encodingService;


    /*
     * Listen to video.uploaded Kafka topic
     * Triggered when video service upload a raw video
     *
     * FLOW:
     *
     * Video Service -> S3 Upload -> Kafka (video.uploaded)
     *                            -> This consumer
     *                            -> EncodingService -> FFmpeg -> S3
     *                            -> Kafka (video.encoded)
     * */
    @KafkaListener(
            topics = "video.uploaded",
            groupId = "encoding-service-group"
    )
    public void consumeVideoUploadedEvent(VideoUploadedEvent videoUploadedEvent) {
        log.info("Consumed VideoUploadedEvent for movie: {} file : {} ",
                videoUploadedEvent.getMovieId(), videoUploadedEvent.getOriginalFileName());
        try {
            encodingService.encodeVideo(videoUploadedEvent);
        } catch (Exception e) {
            log.error("Failed to processing encoding for movie: {} - {}",
                    videoUploadedEvent.getMovieId(), videoUploadedEvent.getOriginalFileName());
        }
    }
}
