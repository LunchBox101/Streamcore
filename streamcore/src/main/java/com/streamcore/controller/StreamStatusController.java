package com.streamcore.controller;

import com.streamcore.model.StreamSession;
import com.streamcore.service.StreamRelayService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * StreamStatusController
 *
 * REST API for stream metadata and health checks.
 * Useful for dashboards, monitoring, and the Phase 4 web UI.
 *
 * Endpoints:
 *   GET /api/streams           — list all active streams
 *   GET /api/streams/{id}      — get stats for a specific stream
 *   GET /api/streams/{id}/live — quick liveness check
 */
@RestController
@RequestMapping("/api/streams")
public class StreamStatusController {

    private final StreamRelayService relayService;

    public StreamStatusController(StreamRelayService relayService) {
        this.relayService = relayService;
    }

    /**
     * List all known stream sessions with metadata.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listStreams() {
        Map<String, StreamSession> sessions = relayService.getAllSessions();

        Map<String, Object> response = new HashMap<>();
        response.put("totalStreams", sessions.size());

        Map<String, Object> streamData = new HashMap<>();
        sessions.forEach((id, session) -> {
            Map<String, Object> info = new HashMap<>();
            info.put("streamId", session.getStreamId());
            info.put("startedAt", session.getStartedAt().toString());
            info.put("viewerCount", session.getViewerCount());
            info.put("bytesRelayed", session.getBytesRelayed());
            info.put("live", relayService.isLive(id));
            streamData.put(id, info);
        });

        response.put("streams", streamData);
        return ResponseEntity.ok(response);
    }

    /**
     * Get stats for a single stream.
     */
    @GetMapping("/{streamId}")
    public ResponseEntity<?> getStream(@PathVariable String streamId) {
        StreamSession session = relayService.getSession(streamId);

        if (session == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> info = new HashMap<>();
        info.put("streamId", session.getStreamId());
        info.put("startedAt", session.getStartedAt().toString());
        info.put("viewerCount", session.getViewerCount());
        info.put("bytesRelayed", session.getBytesRelayed());
        info.put("live", relayService.isLive(streamId));

        return ResponseEntity.ok(info);
    }

    /**
     * Quick liveness check — useful for polling from a viewer UI.
     */
    @GetMapping("/{streamId}/live")
    public ResponseEntity<Map<String, Object>> isLive(@PathVariable String streamId) {
        Map<String, Object> result = new HashMap<>();
        result.put("streamId", streamId);
        result.put("live", relayService.isLive(streamId));
        result.put("viewers", relayService.getViewerCount(streamId));
        return ResponseEntity.ok(result);
    }
}
