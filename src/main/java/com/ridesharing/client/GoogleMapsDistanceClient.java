package com.ridesharing.client;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.ridesharing.common.exception.ExternalServiceException;

/**
 * Wraps the Google Maps Distance Matrix API to compute road distance in km
 * between two lat/lng points (Requirement 6.1).
 */
@Component
public class GoogleMapsDistanceClient {

    private static final String DISTANCE_MATRIX_URL =
            "https://maps.googleapis.com/maps/api/distancematrix/json";

    private final RestTemplate restTemplate;
    private final String apiKey;

    public GoogleMapsDistanceClient(RestTemplate restTemplate,
                                    @Value("${app.google-maps.api-key:}") String apiKey) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
    }

    /**
     * Returns the road distance in kilometres between two lat/lng points.
     *
     * @param originLat      origin latitude
     * @param originLng      origin longitude
     * @param destLat        destination latitude
     * @param destLng        destination longitude
     * @return road distance in km
     * @throws ExternalServiceException if the API key is blank or the call fails
     */
    public double getRoadDistanceKm(double originLat, double originLng,
                                    double destLat, double destLng) {
        if (!StringUtils.hasText(apiKey)) {
            // Stub: use Haversine straight-line distance for local dev
            return haversineKm(originLat, originLng, destLat, destLng);
        }

        String origins = originLat + "," + originLng;
        String destinations = destLat + "," + destLng;

        String url = UriComponentsBuilder.fromHttpUrl(DISTANCE_MATRIX_URL)
                .queryParam("origins", origins)
                .queryParam("destinations", destinations)
                .queryParam("key", apiKey)
                .toUriString();

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null) {
                throw new ExternalServiceException("DISTANCE_FAILED",
                        "Empty response from Google Maps Distance Matrix API.");
            }

            String status = (String) response.get("status");
            if (!"OK".equals(status)) {
                throw new ExternalServiceException("DISTANCE_FAILED",
                        "Distance Matrix API returned status: " + status);
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rows =
                    (List<Map<String, Object>>) response.get("rows");

            if (rows == null || rows.isEmpty()) {
                throw new ExternalServiceException("DISTANCE_FAILED",
                        "No rows in Distance Matrix API response.");
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> elements =
                    (List<Map<String, Object>>) rows.get(0).get("elements");

            if (elements == null || elements.isEmpty()) {
                throw new ExternalServiceException("DISTANCE_FAILED",
                        "No elements in Distance Matrix API response.");
            }

            Map<String, Object> element = elements.get(0);
            String elementStatus = (String) element.get("status");
            if (!"OK".equals(elementStatus)) {
                throw new ExternalServiceException("DISTANCE_FAILED",
                        "Distance Matrix element status: " + elementStatus);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> distance = (Map<String, Object>) element.get("distance");
            int distanceMeters = ((Number) distance.get("value")).intValue();

            return distanceMeters / 1000.0;

        } catch (ExternalServiceException e) {
            throw e;
        } catch (RestClientException e) {
            throw new ExternalServiceException("DISTANCE_FAILED",
                    "Failed to call Google Maps Distance Matrix API: " + e.getMessage(), e);
        }
    }

    private double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
