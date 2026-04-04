package com.streamcore.service;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class HlsTranscodingService {
    private static final Logger log = LoggerFactory.getLogger(HlsTranscodingService.class);

    private Process ffmpegProcess;
    private OutputStream ffmpegInput;

    public void startTranscoding(String streamId) throws IOException {
        new File("hls-output/" + streamId).mkdirs();

        List<String> command = List.of(
            "ffmpeg",
            "-analyzeduration", "0",
            "-probesize", "32",
            "-f", "mpegts",  
            "-i", "pipe:0",
            "-c:v", "libx264",
            "-c:a", "aac",
            "-f", "hls",
            "-hls_time", "2",
            "-hls_list_size", "5",
            "-hls_flags", "delete_segments",
            "hls-output/" + streamId + "/index.m3u8"
        );

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(false);

        ffmpegProcess = pb.start();
        ffmpegInput = ffmpegProcess.getOutputStream();

        log.info("FFmpeg started for streamId [{}]", streamId);

        Thread stderrReader = new Thread(() -> {
            try (var reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(ffmpegProcess.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("[FFmpeg] {}", line);
                }
            } catch (IOException e) {
                log.error("Error reading FFmpeg stderr: {}", e.getMessage());
            }
        });
        stderrReader.setDaemon(true);
        stderrReader.start();

    }

    public void writeChunk(byte[] data) throws IOException {
        if (ffmpegInput != null && ffmpegProcess != null && ffmpegProcess.isAlive()) {
            ffmpegInput.write(data);
            ffmpegInput.flush();
        }
    }

    public void stopTranscoding() {
        try {
            if (ffmpegInput != null) {
                ffmpegInput.close();
            }
            if (ffmpegProcess != null) {
                ffmpegProcess.destroy();
            }
            log.info("FFmpeg stopped");
        } catch (IOException e) {
            log.error("Error stopping FFmpeg: {}", e.getMessage());
        }
    }

}
