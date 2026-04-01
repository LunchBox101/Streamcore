# StreamCore 🎬

A **live audio/video streaming server** built with Java 17 and Spring Boot.  
Demonstrates real-time systems, low-latency WebSocket pipelines, and media relay architecture.

---

## Architecture

```
Publisher (Browser/OBS)
        │
        ▼  WebSocket /stream/publish
┌─────────────────────┐
│   StreamCore Server  │  ← Spring Boot + Java 17
│                     │
│  StreamRelayService  │  ← Binary frame relay (hot path)
│  (ConcurrentHashMap) │     No locking, thread-safe collections
└─────────────────────┘
        │
        ▼  WebSocket /stream/watch
  Viewer 1, Viewer 2, Viewer N...
```

**Phase 1 (current):** Raw WebSocket binary relay — publisher sends frames, server fans out to all viewers in real time.  
**Phase 2:** FFmpeg transcoding + HLS segment output for browser-native playback.  
**Phase 3:** Multi-stream support, viewer concurrency hardening, backpressure handling.  
**Phase 4:** Docker production deployment, metrics dashboard, load testing.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2 |
| Real-time | Spring WebSocket (JSR-356) |
| REST API | Spring MVC |
| Build | Maven |
| Container | Docker + docker-compose |
| Monitoring | Spring Actuator |

---

## Getting Started

### Run locally with Maven
```bash
./mvnw spring-boot:run
```

### Run with Docker
```bash
docker-compose up --build
```

Open `http://localhost:8080` in your browser for the live dashboard.

---

## API Reference

### WebSocket Endpoints
| Endpoint | Role | Description |
|---|---|---|
| `ws://localhost:8080/stream/publish?streamId=live` | Publisher | Send binary video/audio frames |
| `ws://localhost:8080/stream/watch?streamId=live` | Viewer | Receive relayed frames |

### REST Endpoints
| Method | Path | Description |
|---|---|---|
| GET | `/api/streams` | List all active streams |
| GET | `/api/streams/{id}` | Stats for a specific stream |
| GET | `/api/streams/{id}/live` | Liveness + viewer count |
| GET | `/actuator/health` | Server health check |

---

## Key Design Decisions

### Thread Safety on the Hot Path
The frame relay loop (`StreamRelayService.relayFrame`) uses `CopyOnWriteArrayList` for the viewer list — safe for concurrent reads with no locking overhead, ideal for the high-frequency relay path.

### Binary WebSocket Transport
Raw binary frames (not base64) are used for efficiency. Spring's `BinaryMessage` avoids unnecessary serialization overhead on each frame.

### Configurable Buffer Sizes
`application.properties` exposes `max-binary-message-buffer-size` (default 10MB) to handle large video frames without drops.

---

## What's Next (Phase 2)

- Integrate **FFmpeg via Java ProcessBuilder** for transcoding
- Output **HLS segments** (`.m3u8` + `.ts` files) for browser-native playback
- Add **backpressure** handling when viewers fall behind

---

## Author

**Lance Leonard** — Senior Backend Java Engineer  
[github.com/LunchBox101](https://github.com/LunchBox101) · [linkedin.com/in/lance-leonard](https://linkedin.com/in/lance-leonard)
