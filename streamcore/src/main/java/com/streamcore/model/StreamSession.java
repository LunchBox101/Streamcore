package com.streamcore.model;

import java.time.Instant;

/**
 * Represents an active live stream session.
 */
public class StreamSession {

    private final String streamId;
    private final Instant startedAt;
    private int viewerCount;
    private long bytesRelayed;

    public StreamSession(String streamId) {
        this.streamId = streamId;
        this.startedAt = Instant.now();
        this.viewerCount = 0;
        this.bytesRelayed = 0;
    }

    public String getStreamId() { return streamId; }
    public Instant getStartedAt() { return startedAt; }
    public int getViewerCount() { return viewerCount; }
    public long getBytesRelayed() { return bytesRelayed; }

    public void incrementViewers() { this.viewerCount++; }
    public void decrementViewers() { if (this.viewerCount > 0) this.viewerCount--; }
    public void addBytesRelayed(long bytes) { this.bytesRelayed += bytes; }

    @Override
    public String toString() {
        return "StreamSession{id=" + streamId +
               ", viewers=" + viewerCount +
               ", bytesRelayed=" + bytesRelayed + "}";
    }
}
