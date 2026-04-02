package com.streamcore;

import org.springframework.web.socket.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.File;
import java.nio.file.Files;

public class StreamPublisherTest {

    public static void main(String[] args) throws Exception {
        byte[] videoData = Files.readAllBytes(
            new File("test-video.ts").toPath()
        );

        StandardWebSocketClient client = new StandardWebSocketClient();
        
        WebSocketSession session = client.execute(
            new AbstractWebSocketHandler() {},
            "ws://localhost:8080/stream/publish?streamId=live"
        ).get();

        System.out.println("Connected! Sending " + videoData.length + " bytes...");
        
        int chunkSize = 65536; // 64KB chunks
        for (int i = 0; i < videoData.length; i += chunkSize) {
            int end = Math.min(i + chunkSize, videoData.length);
            byte[] chunk = new byte[end - i];
            System.arraycopy(videoData, i, chunk, 0, chunk.length);
            session.sendMessage(new BinaryMessage(chunk));
            Thread.sleep(100);
        }

        session.close();
        System.out.println("Done!");
    }
}