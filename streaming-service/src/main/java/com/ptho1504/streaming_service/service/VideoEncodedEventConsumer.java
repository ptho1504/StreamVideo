package com.ptho1504.streaming_service.service;


import com.ptho1504.streaming_service.event.VideoEncodedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoEncodedEventConsumer {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String MASTER_PLAYLIST_KEY_PREFIX = "streaming:playlist:";

    /*
     * Listens to video.encoded Kafka topic
     * Store master playlist key in Redis when encoding is complete
     * This allows Streaming Service to quickly find the playlist key by movieId
     *  */

    @KafkaListener(
            topics = "video.encoded",
            groupId = "streaming-service-group"
    )
    public void consumeVideoEncodedEvent(VideoEncodedEvent videoEncodedEvent) {
        log.info("Consumed Video EncodedEvent for movie: {} success: {}",
                videoEncodedEvent.getMovieId(), videoEncodedEvent.isSuccess());

        if (videoEncodedEvent.isSuccess()) {
            String cacheKey = MASTER_PLAYLIST_KEY_PREFIX + videoEncodedEvent.getMovieId();
            redisTemplate.opsForValue().set(cacheKey, videoEncodedEvent.getMasterPlaylistKey());

            log.info("Master playlist key stored in Redis for movie: {}", videoEncodedEvent.getMovieId());
        } else {
            log.error("Encoding failed for movie: {} - {}", videoEncodedEvent.getMovieId(), videoEncodedEvent.getErrorMsg());
        }

    }
}
