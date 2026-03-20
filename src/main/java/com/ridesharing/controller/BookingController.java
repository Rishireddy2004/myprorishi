package com.ridesharing.controller;

import com.ridesharing.dto.BookingRequestDTO;
import com.ridesharing.dto.BookingResponseDTO;
import com.ridesharing.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
@RestController
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /** POST /trips/{id}/bookings — book seats on a trip */
    @PostMapping("/trips/{id}/bookings")
    public ResponseEntity<BookingResponseDTO> createBooking(
            @PathVariable UUID id,
            @RequestBody BookingRequestDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        BookingResponseDTO response = bookingService.createBooking(id, dto, userDetails.getUsername(), dto.getRedeemPoints());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** GET /bookings/my — get all bookings for the authenticated passenger */
    @GetMapping("/bookings/my")
    public ResponseEntity<Map<String, List<BookingResponseDTO>>> getMyBookings(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<BookingResponseDTO> bookings = bookingService.getMyBookings(userDetails.getUsername());
        return ResponseEntity.ok(Map.of("bookings", bookings));
    }

    /** GET /bookings/trip/{tripId} — get all bookings for a trip (driver) */
    @GetMapping("/bookings/trip/{tripId}")
    public ResponseEntity<Map<String, List<BookingResponseDTO>>> getTripBookings(
            @PathVariable UUID tripId,
            @AuthenticationPrincipal UserDetails userDetails) {
        List<BookingResponseDTO> bookings = bookingService.getTripBookings(tripId, userDetails.getUsername());
        return ResponseEntity.ok(Map.of("bookings", bookings));
    }

    /** PATCH /bookings/{id}/confirm — driver confirms a pending booking */
    @PatchMapping("/bookings/{id}/confirm")
    public ResponseEntity<BookingResponseDTO> confirmBooking(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(bookingService.confirmBooking(id, userDetails.getUsername()));
    }

    /** PATCH /bookings/{id}/reject — driver rejects a booking */
    @PatchMapping("/bookings/{id}/reject")
    public ResponseEntity<BookingResponseDTO> rejectBooking(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(bookingService.rejectBooking(id, userDetails.getUsername()));
    }

    /** GET /trips/{tripId}/co-passengers — get fellow confirmed/pending passengers for a trip */
    @GetMapping("/trips/{tripId}/co-passengers")
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> getCoPassengers(
            @PathVariable UUID tripId,
            @AuthenticationPrincipal UserDetails userDetails) {
        List<Map<String, String>> passengers = bookingService.getCoPassengers(tripId, userDetails.getUsername());
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("passengers", passengers);
        return ResponseEntity.ok(result);
    }

    /** DELETE /bookings/{id} — passenger cancels a booking */
    @DeleteMapping("/bookings/{id}")
    public ResponseEntity<Void> cancelBooking(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        bookingService.cancelBooking(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
