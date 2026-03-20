package com.ridesharing.controller;

import com.ridesharing.common.exception.ResourceNotFoundException;
import com.ridesharing.domain.Transaction;
import com.ridesharing.domain.User;
import com.ridesharing.dto.TransactionDTO;
import com.ridesharing.repository.TransactionRepository;
import com.ridesharing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Exposes transaction history for the authenticated user.
 * GET /users/me/transactions — Requirement 7.5 (11.7, 11.8)
 */
@RestController
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @GetMapping("/users/me/transactions")
    public ResponseEntity<List<TransactionDTO>> getMyTransactions(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND",
                        "User not found: " + userDetails.getUsername()));

        List<TransactionDTO> transactions = transactionRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toDTO)
                .toList();

        return ResponseEntity.ok(transactions);
    }

    private TransactionDTO toDTO(Transaction tx) {
        return TransactionDTO.builder()
                .id(tx.getId())
                .userId(tx.getUser().getId())
                .bookingId(tx.getBooking() != null ? tx.getBooking().getId() : null)
                .type(tx.getType())
                .amount(tx.getAmount())
                .currency(tx.getCurrency())
                .status(tx.getStatus())
                .stripeReference(tx.getStripeReference())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}
