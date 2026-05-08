package com.ptho1504.content_service.model;

/**
 * Tracks the video processing life cycle
 *
 * FLOW:
 * PENDING -> UPLOADED -> ENCODING -> ENCODED -> READY
 *                                 -> FAILED
 *
 * */
public enum VideoStatus {
    PENDING, // movie added but not uploaded yet
    UPLOADED, // raw video updated to S3
    ENCODING, // FFmpeg is encoding the video
    ENCODED, // Encoding complete
    READY, // HLS playlist ready - can be streamed
    FAILED // Encoding  failed
}
