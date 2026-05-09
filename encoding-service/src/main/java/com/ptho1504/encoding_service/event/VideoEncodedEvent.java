package com.ptho1504.encoding_service.event;


import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
public class VideoEncodedEvent {
    private String movieId;
    private String hlsUrl;
    private String masterPlaylistKey;
    private boolean success;
    private String errorMsg;
}
