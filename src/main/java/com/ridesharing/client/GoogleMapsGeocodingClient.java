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
 * Thin wrapper around the Google Maps Geocoding REST API.
 * Uses a configurable API key from application.yml (app.google-maps.api-key).
 */
@Component
public class GoogleMapsGeocodingClient {

    private static final String GEOCODING_URL = "https://maps.googleapis.com/maps/api/geocode/json";

    private final RestTemplate restTemplate;
    private final String apiKey;

    public GoogleMapsGeocodingClient(RestTemplate restTemplate,
                                     @Value("${app.google-maps.api-key:}") String apiKey) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
    }

    /**
     * Geocode a human-readable address to lat/lng coordinates.
     * Returns stub coordinates (0.0, 0.0) when no API key is configured (local dev mode).
     *
     * @param address the address to geocode
     * @return a double array [lat, lng]
     * @throws ExternalServiceException if the geocoding call fails
     */
    public double[] geocode(String address) {
        if (!StringUtils.hasText(apiKey)) {
            // Stub: return deterministic fake coords based on address hash for local dev
            int hash = address.hashCode();
            double lat = 17.0 + (hash % 1000) / 10000.0;
            double lng = 78.0 + (Math.abs(hash / 1000) % 1000) / 10000.0;
            return new double[]{lat, lng};
        }

        String url = UriComponentsBuilder.fromHttpUrl(GEOCODING_URL)
                .queryParam("address", address)
                .queryParam("key", apiKey)
                .toUriString();

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null) {
                throw new ExternalServiceException("GEOCODING_FAILED",
                        "Empty response from Google Maps Geocoding API for address: " + address);
            }

            String status = (String) response.get("status");
            if (!"OK".equals(status)) {
                throw new ExternalServiceException("GEOCODING_FAILED",
                        "Google Maps Geocoding API returned status '" + status + "' for address: " + address);
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
            if (results == null || results.isEmpty()) {
                throw new ExternalServiceException("GEOCODING_NO_RESULTS",
                        "No geocoding results found for address: " + address);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> geometry = (Map<String, Object>) results.get(0).get("geometry");
            @SuppressWarnings("unchecked")
            Map<String, Object> location = (Map<String, Object>) geometry.get("location");

            double lat = ((Number) location.get("lat")).doubleValue();
            double lng = ((Number) location.get("lng")).doubleValue();

            return new double[]{lat, lng};

        } catch (ExternalServiceException e) {
            throw e;
        } catch (RestClientException e) {
            throw new ExternalServiceException("GEOCODING_FAILED",
                    "Failed to call Google Maps Geocoding API: " + e.getMessage(), e);
        }
    }
}
