package com.streamcore.controller;

import com.streamcore.service.StreamRelayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.net.URI;

/**
 * StreamWebSocketHandler
 *
 * Handles both publisher (/stream/publish) and viewer (/stream/watch) WebSocket connections.
 *
 * Connection flow:
 *   Publisher connects → /stream/publish?streamId=live
 *     → sends binary frames (raw video/audio chunks)
 *     → server relays to all viewers
 *
 *   Viewer connects  → /stream/watch?streamId=live
 *     → receives binary frames relayed from the publisher
 *
 * Phase 1 keeps this simple: raw binary passthrough, no transcoding yet.
 * Phase 2 will add FFmpeg transcoding between ingest and relay.
 */
@Component
public class StreamWebSocketHandler extends AbstractWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(StreamWebSocketHandler.class);

    private final StreamRelayService relayService;

    public StreamWebSocketHandler(StreamRelayService relayService) {
        this.relayService = relayService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String path = getPath(session);
        String streamId = getStreamId(session);

        if (path.contains("/publish")) {
            relayService.registerPublisher(streamId, session);
            session.sendMessage(new TextMessage("{\"status\":\"publishing\",\"streamId\":\"" + streamId + "\"}"));
        } else if (path.contains("/watch")) {
            relayService.registerViewer(streamId, session);
            boolean live = relayService.isLive(streamId);
            session.sendMessage(new TextMessage(
                "{\"status\":\"watching\",\"streamId\":\"" + streamId + "\",\"live\":" + live + "}"
            ));
        }
    }

    /**
     * Handle incoming binary frames from publishers.
     * This is called on every media frame — kept as lean as possible.
     */
    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        String streamId = getStreamId(session);
        relayService.relayFrame(streamId, message);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // Phase 1: text messages are control signals (future use)
        log.debug("Text message from {}: {}", session.getId(), message.getPayload());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String path = getPath(session);
        String streamId = getStreamId(session);

        if (path.contains("/publish")) {
            relayService.removePublisher(streamId, session);
        } else {
            relayService.removeViewer(streamId, session);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("Transport error on session {}: {}", session.getId(), exception.getMessage());
    }

    // --- Helpers ---

    private String getPath(WebSocketSession session) {
        URI uri = session.getUri();
        return uri != null ? uri.getPath() : "";
    }

    private String getStreamId(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri != null && uri.getQuery() != null) {
            for (String param : uri.getQuery().split("&")) {
                String[] kv = param.split("=");
                if (kv.length == 2 && kv[0].equals("streamId")) {
                    return kv[1];
                }
            }
        }
        return StreamRelayService.DEFAULT_STREAM_ID;
    }
}
