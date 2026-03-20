package com.ridesharing.controller;

import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ridesharing.dto.TripSearchResponseDTO;
import com.ridesharing.service.RouteMatcherService;

/**
 * Exposes the trip search endpoint (Requirements 4.1–4.7).
 *
 * GET /trips/search?origin=...&destination=...&date=...
 *   &minPrice=...&maxPrice=...&vehicleMake=...&minDriverRating=...
 */
@RestController
@RequestMapping("/trips")
public class SearchController {

    private final RouteMatcherService routeMatcherService;

    public SearchController(RouteMatcherService routeMatcherService) {
        this.routeMatcherService = routeMatcherService;
    }

    /**
     * Search for available trips matching the passenger's origin, destination, and date.
     *
     * @param origin          passenger origin address (required)
     * @param destination     passenger destination address (required)
     * @param date            travel date in ISO format yyyy-MM-dd (required)
     * @param minPrice        optional minimum base fare per km
     * @param maxPrice        optional maximum base fare per km
     * @param vehicleMake     optional vehicle make filter (e.g. "Toyota")
     * @param minDriverRating optional minimum driver aggregate rating (1.0–5.0)
     * @return 200 with list of matching trips (may be empty with a message)
     */
    @GetMapping("/search")
    public ResponseEntity<TripSearchResponseDTO> searchTrips(
            @RequestParam(required = false, defaultValue = "") String origin,
            @RequestParam(required = false, defaultValue = "") String destination,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime timeFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime timeTo,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) String vehicleMake,
            @RequestParam(required = false) Double minDriverRating) {

        TripSearchResponseDTO response = routeMatcherService.search(
                origin, destination, date != null ? date : LocalDate.now(),
                timeFrom, timeTo, minPrice, maxPrice, vehicleMake, minDriverRating);

        return ResponseEntity.ok(response);
    }
}
