package com.streamcore.controller;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.nio.file.*;

@RestController
@RequestMapping("/hls")
public class HlsController {

    @GetMapping("/{streamId}/index.m3u8")
    public ResponseEntity<Resource> getPlaylist(@PathVariable String streamId) {
        Path file = Paths.get("hls-output", streamId, "index.m3u8");
        if (!Files.exists(file)) return ResponseEntity.notFound().build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.apple.mpegurl"));
        headers.setCacheControl(CacheControl.noCache());
        return ResponseEntity.ok().headers(headers).body(new FileSystemResource(file));
    }

    @GetMapping("/{streamId}/{segment:.+\\.ts}")
    public ResponseEntity<Resource> getSegment(@PathVariable String streamId, 
                                                @PathVariable String segment) {
        Path file = Paths.get("hls-output", streamId, segment);
        if (!Files.exists(file)) return ResponseEntity.notFound().build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("video/MP2T"));
        return ResponseEntity.ok().headers(headers).body(new FileSystemResource(file));
    }
}