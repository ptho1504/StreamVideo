package com.ptho1504.encoding_service.service;

import com.ptho1504.encoding_service.event.VideoEncodedEvent;
import com.ptho1504.encoding_service.event.VideoUploadedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class EncodingService {
    private final S3Client s3Client;
    private final KafkaTemplate<String, VideoEncodedEvent> kafkaTemplate;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${ffmpeg.path}")
    private String ffmpegPath;

    @Value("${encoding.base-path}")
    private String basePath;

    private static final String VIDEO_ENCODED_TOPIC = "video.encoded";

    // Video qualities encode
    // Format: resolution, bitrate, height

    private static final List<int[]> VIDEO_QUALITIES = Arrays.asList(
            new int[]{1920, 5000, 1080}, // 1080p
            new int[]{1280, 2500, 720},  // 720p
            new int[]{854, 1000, 480},   // 480p
            new int[]{640, 800, 360}     // 360p
    );


    /*
     * Main encoding pipeline
     * Steps:
     * 1. Download raw video from S3
     * 2. Encode to multiple qualities using FFmpeg
     * 3. Generate HLS playlist (.m3u8) for each quality
     * 4. Create master playlist.
     * 5. Upload all encoded files back to S3
     * 6. Publish VideoEncodedEvent to Kafka
     * */
    public void encodeVideo(VideoUploadedEvent event) throws IOException {
        log.info("Starting encoding platform for movie: {}", event.getMovieId());

        // Create a unique path for a movie
        String jobPath = basePath + "/" + event.getMovieId();

        try {
            // Create tmp directories
            Files.createDirectories(Paths.get(jobPath));
            Files.createDirectories(Paths.get(jobPath + "/encoded"));

            // Step 1: Download raw video from S3
            String localVideoPath = jobPath + "/raw_video.mp4";
            downloadFromS3(event.getVideoKey(), localVideoPath);
            log.info("Raw video downloaded to {}", localVideoPath);


            // Step 2: Encode multiple quality + generate HLS

            for (int []quality : VIDEO_QUALITIES) {
                int width = quality[0];
                int bitrate = quality[1];
                int height = quality[2];

                String qualityDir = jobPath + "/encoded" + height + "p";
                Files.createDirectories(Paths.get(qualityDir));

                encodeToHLS(localVideoPath, qualityDir, width, height, bitrate);
                log.info("Encoded video {}p downloaded", height);
            }

            // Step 4: Generate master playlist
            String masterPlayListPath = jobPath + "/encoded/master.m3u8";
            generateMasterPlaylist(masterPlayListPath);
            log.info("Encoded master playlist downloaded to {}", masterPlayListPath);

            // Step 5: Upload all resource into S3
            String encodePrefix = "encoded/" + event.getMovieId() + "/";
            uploadEncodeFilesToS3(jobPath + "/encoded", encodePrefix);
            log.info("All encoded files uploaded to S3");

            // Step 6: Publish EncodedVideoEvent
            String masterPlaylistKey = encodePrefix + "master.m3u8";
            String hlsUrl = "https://" + bucketName + ".s3.amazonaws.com/" + masterPlaylistKey;
            VideoEncodedEvent encodedEvent = new VideoEncodedEvent(
                    event.getMovieId(),
                    hlsUrl,
                    masterPlaylistKey,
                    true,
                    null
            );


            kafkaTemplate.send(VIDEO_ENCODED_TOPIC, event.getMovieId(), encodedEvent);
            log.info("Video encoded event published for movie: {}", event.getMovieId());

        } catch (Exception e) {
            log.error("Failed to encode video {} : {}", event.getMovieId(), e.getMessage());

            // Publish failure event
            VideoEncodedEvent failureEvent = new VideoEncodedEvent(
                    event.getMovieId(),
                    null,
                    null,
                    true,
                    e.getMessage()
            );

            kafkaTemplate.send(VIDEO_ENCODED_TOPIC, event.getMovieId(), failureEvent);
        } finally {
          // clean up temp file
          cleanupTempFiles(jobPath);
        }
    }

    private void cleanupTempFiles(String jobPath) {
        try {
            Path dir = Paths.get(jobPath);
            if (Files.exists(dir)) {
                Files.walk(dir)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);

                log.info("Temp files clean up for job: {}", jobPath);
            }
        } catch (IOException e) {
            log.warn("Failed to clean up temp files: {}", e.getMessage());
        }
    }

    private void uploadEncodeFilesToS3(String localDir, String s3Prefix) {
        File directory = new File(localDir);
        uploadDirectoryToS3(directory, localDir, s3Prefix);
    }

    private void uploadDirectoryToS3(File directory, String baseDir, String s3Prefix) {
        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                uploadDirectoryToS3(file, baseDir, s3Prefix);
            } else {
                String relatedPath = file.getAbsolutePath().substring(baseDir.length() + 1)
                        .replace("\\", "/");
                String s3Key = s3Prefix + relatedPath;

                String contentType = file.getName().endsWith(".m3u8") ? "application/x-mpegUrl" : "video/MP2T";

                PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(s3Key)
                        .contentType(contentType)
                        .build();

                s3Client.putObject(putObjectRequest, RequestBody.fromFile(file));
                log.debug("Uploaded {}", s3Key);
            }
        }

    }

    private void generateMasterPlaylist(String masterPlayListPath) throws IOException {
        StringBuilder masterPlaylist = new StringBuilder();
        masterPlaylist.append("#EXTM3U\n");
        masterPlaylist.append("EXT-X-VERSION:3\n\n");

        // Add each quality to master playlist

        int[][] qualities = {{1920, 5000, 1090}, {1280, 2800, 720}, {854, 1200, 480}, {800, 640, 360}};


        for(int []q: qualities) {
            int width = q[0];
            int bitrate = q[1];
            int height = q[2];

            masterPlaylist.append("#EXT-X-STREAM-INF:BANDWIDTH=\n")
                    .append(bitrate * 1000)
                    .append(", RESOLUTION=").append(width).append("x").append(height)
                    .append(",CODECS=\"avc1.42e01ea,m4a.40.2\"\n");
            masterPlaylist.append(height).append("p/playlist.m3u8\n\n");
        }

        Files.writeString(Paths.get(masterPlayListPath), masterPlaylist.toString());
    }

    private void encodeToHLS(String inputPath, String outputDir, int width, int height, int bitrate)
            throws IOException, InterruptedException {
        String playlistPath = outputDir + "/playlist.m3u8";
        String segmentPattern = outputDir + "/segment_%03d.ts";

        // FFmpeg Command for HLS encoding
        List<String> command = Arrays.asList(
                ffmpegPath,
                "-i", inputPath,
                "-vf", "scale=" + width + ":" + height,
                "-c:v", "libx264",
                "-b:v", bitrate + "k",
                "-c:a", "aac",
                "-b:a", "128k",
                "-hls_time", "10",
                "-hls_list_size", "0",
                "-hls_segment_filename", segmentPattern,
                "-f", "hls",
                playlistPath
        );

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();

        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg encoding failed with exit code" + exitCode);
        }
    }



    private void downloadFromS3(String videoKey, String localVideoPath) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(bucketName).key(videoKey).build();

        s3Client.getObject(getObjectRequest, Paths.get(localVideoPath));

    }
}
