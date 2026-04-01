package com.streamcore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * StreamCore — Live Audio/Video Streaming Server
 *
 * Phase 1: WebSocket-based stream ingest and relay
 * Phase 2: FFmpeg transcoding + HLS output
 * Phase 3: Real-time viewer management
 * Phase 4: Docker + web UI
 */
@SpringBootApplication
public class StreamCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(StreamCoreApplication.class, args);
    }
}
