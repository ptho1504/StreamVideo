package com.ptho1504.video_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    // Publish when video is uploaded to S3
    // Encoding Service consume this
    @Bean
    public NewTopic videoUploaderTopic() {
        return TopicBuilder.name("video.uploaded").partitions(3).replicas(1).build();
    }

    // Publish when encoding is completed
    // ?
    @Bean
    public NewTopic videoEncodedTopic() {
        return TopicBuilder.name("video.encoded").partitions(3).replicas(1).build();
    }


}
