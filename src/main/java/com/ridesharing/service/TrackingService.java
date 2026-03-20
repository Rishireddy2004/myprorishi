package com.ridesharing.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridesharing.common.exception.ResourceNotFoundException;
import com.ridesharing.domain.Booking;
import com.ridesharing.domain.BookingStatus;
import com.ridesharing.domain.Trip;
import com.ridesharing.domain.Waypoint;
import com.ridesharing.dto.LocationUpdateDTO;
import com.ridesharing.repository.BookingRepository;
import com.ridesharing.repository.TripRepository;
import com.ridesharing.websocket.LocationWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TrackingService {

    private static final double PROXIMITY_THRESHOLD_KM = 2.0;
    private static final long PROXIMITY_DEDUP_SECONDS = 300;

    // In-memory last-known location per trip (used when Redis is unavailable)
    private final ConcurrentHashMap<String, LocationUpdateDTO> lastLocations = new ConcurrentHashMap<>();

    private final TripRepository tripRepository;
    private final BookingRepository bookingRepository;
    @Nullable private final RedisTemplate<String, String> redisTemplate;
    @Nullable private final RedisMessageListenerContainer listenerContainer;
    private final LocationWebSocketHandler locationWebSocketHandler;
    private final ObjectMapper objectMapper;

    @Autowired
    public TrackingService(TripRepository tripRepository,
                           BookingRepository bookingRepository,
                           @Autowired(required = false) RedisTemplate<String, String> redisTemplate,
                           @Autowired(required = false) RedisMessageListenerContainer listenerContainer,
                           LocationWebSocketHandler locationWebSocketHandler,
                           ObjectMapper objectMapper) {
        this.tripRepository = tripRepository;
        this.bookingRepository = bookingRepository;
        this.redisTemplate = redisTemplate;
        this.listenerContainer = listenerContainer;
        this.locationWebSocketHandler = locationWebSocketHandler;
        this.objectMapper = objectMapper;
    }

    /**
     * Accepts a location update from the driver, enforces rate limiting,
     * publishes to Redis Pub/Sub (if available), and broadcasts directly
     * via WebSocket for no-Redis local dev.
     */
    public void updateLocation(UUID tripId, LocationUpdateDTO dto) {
        // Verify trip exists
        tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("TRIP_NOT_FOUND", "Trip not found: " + tripId));

        String channel = "trip:" + tripId + ":location";

        if (redisTemplate != null && listenerContainer != null) {
            // Rate-limit: reject if last update was within 5 seconds (was 30s — too slow for live tracking)
            String rateKey = "rate:trip:" + tripId + ":location";
            Boolean isNew = redisTemplate.opsForValue().setIfAbsent(rateKey, "1", Duration.ofSeconds(5));
            if (Boolean.FALSE.equals(isNew)) {
                return; // silently skip, don't throw — driver keeps sending
            }
            ChannelTopic topic = new ChannelTopic(channel);
            listenerContainer.addMessageListener(locationWebSocketHandler, topic);
            String payload = buildPayload(tripId, dto);
            redisTemplate.convertAndSend(channel, payload);
        } else {
            // No Redis — broadcast directly via WebSocket (local dev)
            String payload = buildPayload(tripId, dto);
            locationWebSocketHandler.broadcastDirect(tripId.toString(), payload);
        }

        // Store last known location (in-memory fallback for REST polling)
        lastLocations.put(tripId.toString(), dto);

        // Proximity check for confirmed passengers
        checkProximity(tripId, dto);
    }

    /** Returns the last known location for a trip, or null if not yet started. */
    public LocationUpdateDTO getLastLocation(UUID tripId) {
        return lastLocations.get(tripId.toString());
    }

    private void checkProximity(UUID tripId, LocationUpdateDTO driverLocation) {
        List<Booking> confirmedBookings = bookingRepository.findByTripIdAndStatus(tripId, BookingStatus.CONFIRMED);

        for (Booking booking : confirmedBookings) {
            Waypoint boardingWaypoint = booking.getBoardingWaypoint();
            if (boardingWaypoint == null) {
                continue;
            }

            double distance = haversineKm(
                    driverLocation.getLat(), driverLocation.getLng(),
                    boardingWaypoint.getLat(), boardingWaypoint.getLng()
            );

            if (distance <= PROXIMITY_THRESHOLD_KM) {
                UUID passengerId = booking.getPassenger().getId();
                if (redisTemplate != null) {
                    String dedupKey = "proximity:trip:" + tripId + ":passenger:" + passengerId;
                    Boolean alertNew = redisTemplate.opsForValue().setIfAbsent(dedupKey, "1", Duration.ofSeconds(PROXIMITY_DEDUP_SECONDS));
                    if (Boolean.TRUE.equals(alertNew)) {
                        dispatchProximityAlert(tripId, passengerId, distance);
                    }
                } else {
                    dispatchProximityAlert(tripId, passengerId, distance);
                }
            }
        }
    }

    private void dispatchProximityAlert(UUID tripId, UUID passengerId, double distanceKm) {
        if (redisTemplate == null) return;
        // Publish proximity alert to a dedicated channel for the passenger
        String alertChannel = "trip:" + tripId + ":proximity:" + passengerId;
        try {
            String alertPayload = objectMapper.writeValueAsString(Map.of(
                    "event", "proximity.alert",
                    "tripId", tripId.toString(),
                    "passengerId", passengerId.toString(),
                    "distanceKm", distanceKm
            ));
            redisTemplate.convertAndSend(alertChannel, alertPayload);
        } catch (JsonProcessingException e) {
            // Log and continue — alert failure should not break location update
        }
    }

    private String buildPayload(UUID tripId, LocationUpdateDTO dto) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "tripId", tripId.toString(),
                    "lat", dto.getLat(),
                    "lng", dto.getLng()
            ));
        } catch (JsonProcessingException e) {
            return "{\"tripId\":\"" + tripId + "\",\"lat\":" + dto.getLat() + ",\"lng\":" + dto.getLng() + "}";
        }
    }

    /**
     * Haversine formula — returns distance in kilometres between two lat/lng points.
     */
    static double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6371.0; // Earth radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
