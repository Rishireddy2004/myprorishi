package com.ridesharing.websocket;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Subscribes to Redis Pub/Sub channels matching "trip:*:location" and fans out
 * location updates to WebSocket subscribers via STOMP.
 * Also provides a direct broadcast method for no-Redis local dev.
 */
@Component
public class LocationWebSocketHandler implements MessageListener {

    private static final Pattern CHANNEL_PATTERN = Pattern.compile("trip:([^:]+):location");

    private final SimpMessagingTemplate messagingTemplate;

    public LocationWebSocketHandler(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
        String body = new String(message.getBody(), StandardCharsets.UTF_8);

        Matcher matcher = CHANNEL_PATTERN.matcher(channel);
        if (matcher.matches()) {
            String tripId = matcher.group(1);
            broadcastDirect(tripId, body);
        }
    }

    /** Broadcast a location payload directly to WebSocket subscribers (no Redis needed). */
    public void broadcastDirect(String tripId, String payload) {
        messagingTemplate.convertAndSend("/topic/trip/" + tripId + "/location", payload);
    }
}
