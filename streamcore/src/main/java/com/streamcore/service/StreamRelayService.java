package com.streamcore.service;

import com.streamcore.model.StreamSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * StreamRelayService
 *
 * Core Phase 1 service responsible for:
 *  - Registering publisher sessions (broadcasters)
 *  - Registering viewer sessions (consumers)
 *  - Relaying binary frames from publisher → all connected viewers
 *  - Tracking stream metadata (viewer count, bytes relayed)
 *
 * Uses thread-safe collections (ConcurrentHashMap, CopyOnWriteArrayList)
 * because WebSocket callbacks can arrive on multiple threads simultaneously.
 * This is a key low-latency design decision: no locking on the hot relay path.
 */
@Service
public class StreamRelayService {

    private static final Logger log = LoggerFactory.getLogger(StreamRelayService.class);

    // streamId → publisher WebSocketSession
    private final Map<String, WebSocketSession> publishers = new ConcurrentHashMap<>();

    // streamId → list of viewer WebSocketSessions
    private final Map<String, List<WebSocketSession>> viewers = new ConcurrentHashMap<>();

    // streamId → StreamSession metadata
    private final Map<String, StreamSession> sessions = new ConcurrentHashMap<>();

    // Default stream key for Phase 1 (single-stream mode)
    public static final String DEFAULT_STREAM_ID = "live";

    /**
     * Register a broadcaster. Creates a new StreamSession if one doesn't exist.
     */
    public void registerPublisher(String streamId, WebSocketSession session) {
        publishers.put(streamId, session);
        sessions.put(streamId, new StreamSession(streamId));
        viewers.putIfAbsent(streamId, new CopyOnWriteArrayList<>());
        log.info("Publisher connected for stream [{}] — sessionId={}", streamId, session.getId());
    }

    /**
     * Register a viewer. Adds them to the relay list for the given stream.
     */
    public void registerViewer(String streamId, WebSocketSession session) {
        viewers.computeIfAbsent(streamId, k -> new CopyOnWriteArrayList<>()).add(session);
        StreamSession s = sessions.get(streamId);
        if (s != null) s.incrementViewers();
        log.info("Viewer connected to stream [{}] — sessionId={}, total viewers={}",
                streamId, session.getId(), getViewerCount(streamId));
    }

    /**
     * Relay a binary frame from the publisher to all active viewers.
     *
     * This is the HOT PATH — called for every single media frame.
     * Kept intentionally lean: no synchronization, no blocking I/O on the relay thread.
     */
    public void relayFrame(String streamId, BinaryMessage frame) {
        List<WebSocketSession> viewerList = viewers.getOrDefault(streamId, Collections.emptyList());
        StreamSession session = sessions.get(streamId);

        if (viewerList.isEmpty()) return;

        byte[] payload = frame.getPayload().array();

        for (WebSocketSession viewer : viewerList) {
            if (viewer.isOpen()) {
                try {
                    viewer.sendMessage(new BinaryMessage(payload));
                } catch (IOException e) {
                    log.warn("Failed to relay frame to viewer {} — removing", viewer.getId());
                    viewerList.remove(viewer);
                }
            }
        }

        if (session != null) {
            session.addBytesRelayed(payload.length);
        }
    }

    /**
     * Remove a publisher when they disconnect.
     */
    public void removePublisher(String streamId, WebSocketSession session) {
        publishers.remove(streamId);
        log.info("Publisher disconnected from stream [{}]", streamId);
    }

    /**
     * Remove a viewer when they disconnect.
     */
    public void removeViewer(String streamId, WebSocketSession session) {
        List<WebSocketSession> viewerList = viewers.get(streamId);
        if (viewerList != null) viewerList.remove(session);
        StreamSession s = sessions.get(streamId);
        if (s != null) s.decrementViewers();
        log.info("Viewer disconnected from stream [{}] — remaining viewers={}", streamId, getViewerCount(streamId));
    }

    public int getViewerCount(String streamId) {
        List<WebSocketSession> viewerList = viewers.get(streamId);
        return viewerList == null ? 0 : viewerList.size();
    }

    public StreamSession getSession(String streamId) {
        return sessions.get(streamId);
    }

    public Map<String, StreamSession> getAllSessions() {
        return Collections.unmodifiableMap(sessions);
    }

    public boolean isLive(String streamId) {
        WebSocketSession pub = publishers.get(streamId);
        return pub != null && pub.isOpen();
    }
}
