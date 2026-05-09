package com.ptho1504.streaming_service.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StreamingResponse {
    private String movieId;
    private String streamingURL;
    private String quality;
    private long expiredInMinutes;
}
