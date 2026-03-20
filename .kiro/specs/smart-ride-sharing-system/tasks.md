# Implementation Plan: Smart Ride Sharing System

## Overview

Incremental implementation across four milestones: User Management & Ride Posting → Fare, Payment & Route Matching → Notifications, Reviews & Admin → Testing & Documentation. Each task builds on the previous, ending with all components wired together. Tech stack: Spring Boot 3.x (JDK 17, Maven), Spring Security + JWT, Spring Data JPA + Hibernate, MySQL/PostgreSQL, Spring WebSocket + STOMP, Redis, Google Maps APIs, Razorpay/Stripe/PayPal, Spring Mail, Twilio, JUnit 5 + Mockito + jqwik.

---

## Milestone 1: User Management & Ride Posting Module

- [x] 1. Set up project skeleton and shared infrastructure
  - Generate Spring Boot 3.x Maven project with dependencies: Spring Web, Spring Security, Spring Data JPA, Spring Validation, Spring WebSocket, Spring Mail, Lombok, springdoc-openapi, jqwik, H2 (test scope), MySQL/PostgreSQL driver, Redis, JWT library (e.g. `jjwt`)
  - Create `application.yml` and `application-test.yml` (H2 in-memory) with datasource, JPA, Redis, and JWT config placeholders
  - Create `GlobalExceptionHandler` (`@ControllerAdvice`) returning `{ "error": { "code": "...", "message": "..." } }` envelopes for `MethodArgumentNotValidException`, `409`, `422`, `410`, `502`, and `500` cases
  - Configure Swagger/OpenAPI via `springdoc-openapi`
  - _Requirements: 12.9_

- [x] 2. Implement JPA entities and database schema
  - Create `@Entity` classes: `User`, `Vehicle`, `Trip`, `Waypoint`, `Booking`, `Review`, `Transaction`, `Notification`, `Dispute`, `PlatformConfig`
  - Apply all constraints: unique index on `User.email`, unique constraint on `Review(trip_id, reviewer_id, reviewee_id)`, enums for `Booking.status` and `Trip.status`, `Booking.fare_locked` non-updatable after insert
  - Add JPA indexes on `Trip(origin_lat, origin_lng, destination_lat, destination_lng, departure_time)` for search performance
  - Snapshot `service_fee_rate` from `PlatformConfig` into `Trip.service_fee_rate` at posting time
  - _Requirements: 3.1, 3.6, 5.4, 6.5, 7.9, 11.9, 12.7_

- [x] 3. Implement Spring Security + JWT authentication
  - Implement `JwtTokenProvider`: issue tokens with 24-hour expiry, validate tokens, extract claims
  - Implement `JwtAuthenticationFilter` to authenticate every request via `Authorization: Bearer` header
  - Configure `SecurityFilterChain`: public routes (`/auth/**`), role-based access (`DRIVER`, `PASSENGER`, `ADMIN`)
  - Implement `BCryptPasswordEncoder` with cost factor ≥ 12
  - _Requirements: 1.4, 1.5, 1.9_

- [x] 4. Implement `AuthService` and `AuthController`
  - `POST /auth/register`: validate unique email/phone, hash password with BCrypt, persist `User`, return 201
  - `POST /auth/login`: verify credentials, return JWT; use identical error message for wrong email and wrong password (no field disclosure)
  - `POST /auth/logout`: invalidate token (blocklist in Redis with TTL = remaining token lifetime)
  - `POST /auth/password-reset/request`: generate HMAC-signed reset token (60-min TTL), send via `JavaMailSender`
  - `POST /auth/password-reset/confirm`: validate token TTL, apply new hashed password
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.10_

  - [x] 4.1 Write property test for password storage (Property 1)
    - **Property 1: Password is never stored in plaintext**
    - **Validates: Requirements 1.4**

  - [ ]* 4.2 Write property test for duplicate email rejection (Property 2)
    - **Property 2: Duplicate email registration is rejected**
    - **Validates: Requirements 1.2, 1.3**

  - [ ]* 4.3 Write property test for session token expiry (Property 3)
    - **Property 3: Session token expiry is within 24 hours**
    - **Validates: Requirements 1.5**

  - [ ]* 4.4 Write property test for credential error non-disclosure (Property 4)
    - **Property 4: Invalid credential error does not reveal which field is wrong**
    - **Validates: Requirements 1.6**

  - [ ]* 4.5 Write property test for password reset token TTL (Property 5)
    - **Property 5: Password reset token TTL is at most 60 minutes**
    - **Validates: Requirements 1.7**

- [x] 5. Implement `UserService` and `UserController`
  - `GET /users/:id`: return public profile (name, photo, aggregate rating, review count, vehicle details, verification status)
  - `PATCH /users/me`: update display name, phone, photo URL; persist and return updated profile
  - `PUT /users/me/vehicle`: add/update `Vehicle` entity linked to authenticated user
  - Compute `aggregate_rating` as arithmetic mean of all received `Review.rating` values; store on `User`
  - `PATCH /users/me/notification-preferences`: toggle `email_notifications_enabled`
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.6, 2.7, 10.5_

  - [ ]* 5.1 Write property test for profile update round-trip (Property 6)
    - **Property 6: Profile update is reflected on retrieval**
    - **Validates: Requirements 2.1**

  - [ ]* 5.2 Write property test for aggregate rating calculation (Property 7)
    - **Property 7: Aggregate rating equals arithmetic mean of all received ratings**
    - **Validates: Requirements 2.2**

- [x] 6. Implement `TripService` and `TripController` (posting and lifecycle)
  - `POST /trips`: validate vehicle details exist (reject with prompt if missing), geocode all addresses via Google Maps Geocoding API, persist `Trip` (status=`open`) and `Waypoint` records, snapshot `service_fee_rate` from `PlatformConfig`
  - `GET /trips/:id`: return full trip detail including waypoints
  - `PATCH /trips/:id`: allow driver to update `available_seats` (reject if new value < confirmed booking count) and `base_fare_per_km` on open trips
  - `DELETE /trips/:id`: cancel open trip if departure is > 2 hours away; dispatch cancellation notifications to all confirmed passengers
  - `PATCH /trips/:id/status`: transition `open → in_progress` (≤ 30 min before departure) and `in_progress → completed` (driver within 1 km of destination)
  - _Requirements: 2.5, 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 8.4, 8.5_

  - [ ]* 6.1 Write property test for trip data round-trip (Property 9)
    - **Property 9: Trip data round-trip**
    - **Validates: Requirements 3.1, 3.3**

  - [ ]* 6.2 Write property test for waypoint count boundary (Property 10)
    - **Property 10: Waypoint count boundary**
    - **Validates: Requirements 3.2**

  - [ ]* 6.3 Write property test for geocoder failure preventing trip creation (Property 11)
    - **Property 11: Geocoder failure prevents trip creation**
    - **Validates: Requirements 3.4**

  - [ ]* 6.4 Write property test for newly posted trip status and ID (Property 12)
    - **Property 12: Newly posted trip has status "open" and a unique ID**
    - **Validates: Requirements 3.5**

  - [ ]* 6.5 Write property test for trip cancellation 2-hour window (Property 13)
    - **Property 13: Trip cancellation respects the 2-hour window**
    - **Validates: Requirements 3.6**

  - [ ]* 6.6 Write property test for trip cancellation notifications (Property 14)
    - **Property 14: Trip cancellation triggers notifications to all booked passengers**
    - **Validates: Requirements 3.7**

  - [ ]* 6.7 Write property test for available seat floor constraint (Property 15)
    - **Property 15: Available seat count cannot be reduced below confirmed booking count**
    - **Validates: Requirements 3.8**

  - [ ]* 6.8 Write property test for trip start time constraint (Property 36)
    - **Property 36: Trip cannot be started more than 30 minutes before departure**
    - **Validates: Requirements 8.4**

  - [ ]* 6.9 Write property test for trip completion proximity constraint (Property 37)
    - **Property 37: Trip completion requires driver to be within 1 km of destination**
    - **Validates: Requirements 8.5**

  - [ ]* 6.10 Write property test for driver vehicle requirement (Property 8)
    - **Property 8: Trip posting is blocked without vehicle details**
    - **Validates: Requirements 2.5**

- [x] 7. Implement `BookingService` and `BookingController`
  - `POST /trips/:id/bookings`: acquire JPA pessimistic write lock on `Trip`, verify `available_seats > 0`, decrement seats, persist `Booking` (status=`confirmed`), dispatch booking notifications
  - `DELETE /bookings/:id`: cancel booking if departure > 2 hours away, restore seat count, dispatch cancellation notifications
  - Enforce suspension check: reject trip posting and booking creation for suspended users
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 11.4_

  - [ ]* 7.1 Write property test for booking on full trip rejection (Property 19)
    - **Property 19: Booking on a full trip is rejected**
    - **Validates: Requirements 5.1, 5.2**

  - [ ]* 7.2 Write property test for seat count round-trip (Property 20)
    - **Property 20: Seat count round-trip (book then cancel)**
    - **Validates: Requirements 5.3, 5.6**

  - [ ]* 7.3 Write property test for confirmed booking ID and status (Property 21)
    - **Property 21: Confirmed booking has a unique ID and status "confirmed"**
    - **Validates: Requirements 5.4**

  - [ ]* 7.4 Write property test for booking cancellation 2-hour window (Property 22)
    - **Property 22: Booking cancellation respects the 2-hour window**
    - **Validates: Requirements 5.5**

  - [ ]* 7.5 Write property test for booking state change notifications (Property 23)
    - **Property 23: Booking state change triggers notifications to passenger and driver**
    - **Validates: Requirements 5.7**

  - [ ]* 7.6 Write property test for suspended user blocking (Property 48)
    - **Property 48: Suspended users cannot post trips or make bookings**
    - **Validates: Requirements 11.4**

- [x] 8. Checkpoint — Milestone 1
  - Ensure all Milestone 1 tests pass, ask the user if questions arise.

---

## Milestone 2: Fare Calculation, Payment & Route Matching

- [x] 9. Implement `FareCalculatorService`
  - Inject `GoogleMapsClient` (wraps `RestTemplate`/`WebClient` calling Distance Matrix API) to get road distance in km between boarding and alighting waypoints along the trip route
  - Compute `fare = base_fare_per_km × road_distance_km`, rounded to 2 decimal places
  - `GET /trips/:id/fare?boarding=...&alighting=...`: return fare estimate using current `base_fare_per_km`; for confirmed bookings return `fare_locked`
  - On booking confirmation: lock fare into `Booking.fare_locked` and `Booking.distance_km`; never update after that
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

  - [ ]* 9.1 Write property test for fare calculation formula (Property 24)
    - **Property 24: Fare calculation is base_fare_per_km × road_distance_km**
    - **Validates: Requirements 6.1**

  - [ ]* 9.2 Write property test for booking confirmation fare breakdown fields (Property 25)
    - **Property 25: Booking confirmation response includes fare breakdown fields**
    - **Validates: Requirements 6.3**

  - [ ]* 9.3 Write property test for fare estimate using updated base fare (Property 26)
    - **Property 26: Fare estimate reflects updated base fare for unconfirmed bookings**
    - **Validates: Requirements 6.4**

  - [ ]* 9.4 Write property test for confirmed booking fare immutability (Property 27)
    - **Property 27: Confirmed booking fare is immutable**
    - **Validates: Requirements 6.5**

- [x] 10. Implement `PaymentService` and payment endpoints
  - On booking confirmation: call payment gateway (Stripe/Razorpay/PayPal) to create a hold (Stripe: `PaymentIntent` with `capture_method: manual`); persist `Transaction` (status=`hold`)
  - On trip completion: capture the hold, transfer `fare_locked × (1 − service_fee_rate)` to driver wallet; persist payout `Transaction`
  - On booking cancellation > 24h before departure: full refund; between 2–24h: 50% refund; persist refund `Transaction`
  - On payment failure: set `Booking.status = payment_failed`, do NOT decrement `available_seats`; return `502`
  - `GET /users/me/transactions`: return all transaction records for authenticated user
  - `POST /admin/disputes/:id/refund`: admin-initiated refund for exact specified amount
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7, 7.8, 7.9, 7.10, 11.7, 11.8_

  - [ ]* 10.1 Write property test for payment transaction on booking confirmation (Property 28)
    - **Property 28: Payment transaction is created on booking confirmation**
    - **Validates: Requirements 7.1**

  - [ ]* 10.2 Write property test for payment failure not consuming seats (Property 29)
    - **Property 29: Payment failure sets booking to "payment_failed" without consuming seats**
    - **Validates: Requirements 7.3**

  - [ ]* 10.3 Write property test for driver payout calculation (Property 30)
    - **Property 30: Driver payout equals fare minus service fee**
    - **Validates: Requirements 7.5**

  - [ ]* 10.4 Write property test for full refund on early cancellation (Property 31)
    - **Property 31: Full refund for cancellations more than 24 hours before departure**
    - **Validates: Requirements 7.7**

  - [ ]* 10.5 Write property test for 50% refund on late cancellation (Property 32)
    - **Property 32: 50% refund for cancellations between 2 and 24 hours before departure**
    - **Validates: Requirements 7.8**

  - [ ]* 10.6 Write property test for transaction history completeness (Property 33)
    - **Property 33: Transaction history contains all user transactions**
    - **Validates: Requirements 7.9**

  - [ ]* 10.7 Write property test for admin dispute refund amount (Property 51)
    - **Property 51: Admin dispute refund amount matches the specified value**
    - **Validates: Requirements 11.7**

- [x] 11. Implement `RouteMatcherService` and trip search
  - `GET /trips/search?origin=...&destination=...&date=...`: geocode passenger origin/destination, query trips whose bounding box overlaps search area, compute detour distance for each candidate, filter to trips where both boarding and alighting are within 10 km of the route, rank by ascending detour distance
  - Apply optional filters: price range, vehicle type, minimum driver rating
  - Return each result with: driver name, driver rating, departure time, available seats, estimated fare, vehicle details
  - Return "no results" message when list is empty
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7_

  - [ ]* 11.1 Write property test for 10 km proximity constraint (Property 16)
    - **Property 16: Search results satisfy the 10 km proximity constraint**
    - **Validates: Requirements 4.1**

  - [ ]* 11.2 Write property test for ascending detour sort order (Property 17)
    - **Property 17: Search results are sorted by ascending detour distance**
    - **Validates: Requirements 4.2**

  - [ ]* 11.3 Write property test for required display fields in search results (Property 18)
    - **Property 18: Search result records contain all required display fields**
    - **Validates: Requirements 4.4**

- [x] 12. Implement real-time GPS tracking with WebSocket + Redis
  - `POST /trips/:id/location`: accept `{ lat, lng }` from driver; publish to Redis channel `trip:{id}:location`
  - Configure `RedisMessageListenerContainer` to subscribe to `trip:{id}:location` and fan out via STOMP to all connected passengers (WebSocket event `trip.location.updated`)
  - Proximity check on each location update: if driver distance to any confirmed passenger's boarding waypoint ≤ 2 km, dispatch proximity alert (deduplicated per passenger per trip via Redis key with TTL)
  - Enforce location update interval ≤ 30 seconds (rate-limit via Redis)
  - _Requirements: 8.1, 8.2, 8.3_

  - [ ]* 12.1 Write property test for location broadcast to confirmed passengers (Property 34)
    - **Property 34: Location updates are broadcast to all confirmed passengers**
    - **Validates: Requirements 8.2**

  - [ ]* 12.2 Write property test for proximity alert at 2 km threshold (Property 35)
    - **Property 35: Proximity alert fires when driver is within 2 km of boarding point**
    - **Validates: Requirements 8.3**

- [x] 13. Checkpoint — Milestone 2
  - Ensure all Milestone 2 tests pass, ask the user if questions arise.

---

## Milestone 3: Notification System, Review & Admin Dashboard

- [x] 14. Implement `NotificationService` with retry logic
  - Consume booking, cancellation, proximity, and suspension events (via Redis Pub/Sub or Spring application events)
  - Dispatch in-platform alerts via WebSocket STOMP push to the target user's personal channel
  - Dispatch email via `JavaMailSender` if `User.email_notifications_enabled = true`
  - Dispatch SMS via Twilio SDK if user has SMS enabled
  - Retry failed deliveries up to 3 times at 5-minute intervals; log permanent failures to `Notification` table (`retry_count`, `is_read`)
  - Send ride reminder 2 hours before departure (configurable) to all confirmed passengers
  - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5, 10.6_

  - [ ]* 14.1 Write property test for email notification preference enforcement (Property 44)
    - **Property 44: Email notifications respect user preferences**
    - **Validates: Requirements 10.5**

  - [ ]* 14.2 Write property test for notification retry up to 3 times (Property 45)
    - **Property 45: Failed notifications are retried up to 3 times**
    - **Validates: Requirements 10.6**

- [x] 15. Implement `ReviewService` and `ReviewController`
  - `POST /trips/:id/reviews`: validate trip is completed, reviewer has a confirmed booking, review window ≤ 7 days, no duplicate (trip_id, reviewer_id, reviewee_id); persist `Review`; recompute and update `User.aggregate_rating`
  - `GET /users/:id/reviews`: return all reviews for user ordered by `created_at DESC`
  - On trip completion: make review forms available to driver (for each passenger) and each passenger (for driver) — 2N opportunities
  - Reject rating outside [1, 5] or non-integer with `422`; reject duplicate with `409`; reject after 7-day window with `410`
  - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6, 9.7, 9.8_

  - [ ]* 15.1 Write property test for review opportunities on trip completion (Property 38)
    - **Property 38: Review opportunities are created for all participants on trip completion**
    - **Validates: Requirements 9.1**

  - [ ]* 15.2 Write property test for rating value range validation (Property 39)
    - **Property 39: Rating value must be an integer in [1, 5]**
    - **Validates: Requirements 9.2**

  - [ ]* 15.3 Write property test for duplicate review rejection (Property 40)
    - **Property 40: Duplicate review is rejected**
    - **Validates: Requirements 9.3, 9.4**

  - [ ]* 15.4 Write property test for immediate aggregate rating update (Property 41)
    - **Property 41: Aggregate rating is updated immediately after review submission**
    - **Validates: Requirements 9.5**

  - [ ]* 15.5 Write property test for review window enforcement (Property 42)
    - **Property 42: Review submission is rejected after the 7-day window**
    - **Validates: Requirements 9.6, 9.7**

  - [ ]* 15.6 Write property test for review ordering (Property 43)
    - **Property 43: User reviews are ordered by most recent first**
    - **Validates: Requirements 9.8**

- [x] 16. Implement `AdminService` and `AdminController`
  - `GET /admin/metrics`: return aggregate metrics (total users, trips, completed trips, payment volume, active users, cancellations, disputes); cache result in Redis with 5-minute TTL
  - `GET /admin/users?q=...`: search users by email or name; return profile, trip history, reviews
  - `GET /admin/trips?q=...`: search trips by ID, origin, destination, or driver name
  - `POST /admin/users/:id/suspend` and `POST /admin/users/:id/unsuspend`: toggle `User.is_suspended`; dispatch suspension notification
  - `GET /admin/disputes` and `POST /admin/disputes/:id/resolve`: list disputes; resolve with refund via `PaymentService`
  - `GET /admin/config` and `PUT /admin/config`: read/write `PlatformConfig`; fee change applies only to future trips
  - All admin endpoints protected by `ADMIN` role
  - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5, 11.6, 11.7, 11.8, 11.9, 11.10, 11.11_

  - [ ]* 16.1 Write property test for admin user search (Property 46)
    - **Property 46: Admin user search returns matching users**
    - **Validates: Requirements 11.2**

  - [ ]* 16.2 Write property test for admin trip search (Property 47)
    - **Property 47: Admin trip search returns matching trips**
    - **Validates: Requirements 11.3**

  - [ ]* 16.3 Write property test for suspension notification (Property 49)
    - **Property 49: User suspension triggers a notification to the suspended user**
    - **Validates: Requirements 11.5**

  - [ ]* 16.4 Write property test for dispute record required fields (Property 50)
    - **Property 50: Dispute record contains all required fields**
    - **Validates: Requirements 11.6**

  - [ ]* 16.5 Write property test for service fee applying only to future trips (Property 52)
    - **Property 52: Service fee change applies only to future trips**
    - **Validates: Requirements 11.9**

- [x] 17. Checkpoint — Milestone 3
  - Ensure all Milestone 3 tests pass, ask the user if questions arise.

---

## Milestone 4: Testing, Documentation & Deployment

- [x] 18. Write integration tests with Spring Boot Test
  - Use `@SpringBootTest` + H2 (`application-test.yml`) for all flows
  - Full booking flow: search → fare estimate → book → payment hold → trip complete → payout
  - Full cancellation flow: cancel > 24h (full refund), cancel 2–24h (50% refund)
  - Notification delivery pipeline: booking confirmed → in-platform + email dispatched
  - Admin suspension blocking: suspended user cannot post trip or create booking
  - Concurrent booking: simulate two simultaneous booking requests on a trip with 1 seat; verify exactly one succeeds and the other receives `409` (overbooking prevention via pessimistic lock)
  - _Requirements: 12.2, 12.4_

- [x] 19. Write unit tests with JUnit 5 + Mockito
  - `FareCalculatorService`: known fare inputs/outputs, zero distance, rounding to 2 decimal places
  - `AuthService`: BCrypt hash verification, JWT expiry boundary, reset token TTL boundary
  - `BookingService`: seat decrement/restore, cancellation window boundary (exactly 2h before departure)
  - `ReviewService`: 7-day window boundary (exactly 7 days), rating boundary (1 and 5), aggregate mean with 0 reviews
  - `PaymentService`: refund policy boundaries (exactly 24h, exactly 2h), payout calculation with 0% and 100% fee
  - `NotificationService`: retry count boundary (3 retries), email preference gate
  - _Requirements: 12.1_

- [x] 20. Add database indexes and Redis caching for search performance
  - Verify JPA indexes on `Trip(origin_lat, origin_lng, destination_lat, destination_lng, departure_time)` are present in schema
  - Add `@Cacheable` (Redis) on `RouteMatcherService.search(...)` with TTL ≤ 5 minutes; evict on trip create/update/cancel
  - Add Redis caching on `AdminService.getMetrics()` with TTL ≤ 5 minutes
  - _Requirements: 12.7, 12.8, 11.1_

- [x] 21. Security hardening
  - Confirm all database interactions use Spring Data JPA (no raw SQL string concatenation) to prevent SQL injection
  - Add output encoding / content-security-policy headers for XSS prevention
  - Verify `@Valid` is applied on all `@RequestBody` parameters across all controllers
  - Confirm JWT blocklist (Redis) is checked on every authenticated request (logout invalidation)
  - _Requirements: 12.5, 12.6_

- [x] 22. Generate API documentation and developer diagrams
  - Verify Swagger/OpenAPI UI is accessible at `/swagger-ui.html` and lists all endpoints with request/response schemas and auth requirements
  - Add ER diagram (can be embedded as Mermaid in a `docs/er-diagram.md` file referencing the design document)
  - Add architecture diagram (Mermaid, referencing the design document)
  - _Requirements: 12.9, 12.10_

- [x] 23. Write deployment and user guides
  - `docs/local-setup.md`: steps to run locally (Maven, MySQL/PostgreSQL, Redis, env vars for Maps API key, payment gateway keys, Twilio, mail)
  - `docs/cloud-deployment.md`: deployment steps for at least one of AWS / GCP / Azure
  - `docs/user-guide.md`: step-by-step flows for Passenger (search → book → track → review) and Driver (post trip → manage bookings → complete trip → receive payout)
  - _Requirements: 12.11, 12.12_

- [x] 24. Final checkpoint — Ensure all tests pass
  - Ensure all unit, integration, and property-based tests pass, ask the user if questions arise.

---

## Notes

- Tasks marked with `*` are optional and can be skipped for a faster MVP
- Each task references specific requirements for traceability
- Property tests use `jqwik` with `@Property(tries = 100)` and must include the tag comment `// Feature: smart-ride-sharing-system, Property {N}: {property_title}`
- Unit tests use `@ExtendWith(MockitoExtension.class)`; integration tests use `@SpringBootTest` with H2
- Pessimistic write lock (`@Lock(LockModeType.PESSIMISTIC_WRITE)`) on `Trip.availableSeats` is the concurrency control mechanism for overbooking prevention
- `Booking.fare_locked` must be set as `@Column(updatable = false)` to enforce fare immutability at the ORM level
