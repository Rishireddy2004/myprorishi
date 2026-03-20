# Requirements Document

## Introduction

The Smart Ride Sharing System is a website-based platform that connects vehicle owners (drivers) traveling long distances with passengers heading in the same direction. Drivers post available trips with seat capacity; passengers search and book seats along matching routes. The platform handles dynamic fare calculation based on per-passenger distance traveled, secure payment processing, real-time route tracking, user reviews, and administrative oversight. The goals are transparency, cost-effectiveness for both parties, and reduced environmental impact through shared travel.

### Tech Stack

- **Backend**: Spring Boot (Java), Spring Security with JWT for authentication and role-based access control
- **Password Hashing**: BCrypt or Argon2 (cost factor configured for security)
- **Distance & Geocoding**: Google Maps Distance Matrix API for road distance calculation and address geocoding
- **Payments**: Razorpay, Stripe, or PayPal — passengers pay at booking; drivers receive payout after trip completion via wallet or bank transfer
- **Real-Time Updates**: WebSockets (Spring WebSocket / STOMP) or Firebase Realtime Database for live driver location and in-app notifications
- **SMS Notifications**: Twilio for SMS delivery; Spring Mail for email delivery
- **Database**: Relational database (PostgreSQL) with indexing on frequently searched columns (source, destination, date) and Redis caching for popular ride searches
- **Testing**: JUnit + Mockito (unit), Spring Boot Test (integration), Postman (API), JMeter (load)
- **Documentation**: Swagger/OpenAPI for API docs, ER diagram, architecture diagram; deployment guides for local and cloud (AWS/GCP/Azure)

### Implementation Milestones

- **Milestone 1**: User Management & Ride Posting — registration/login, role-based profiles, trip posting, search, and booking
- **Milestone 2**: Fare Calculation, Payment & Route Matching — Google Maps distance-based fares, payment gateway integration, WebSocket real-time updates, partial route matching
- **Milestone 3**: Notifications, Reviews & Admin Dashboard — SMS/email alerts, ride reminders, post-ride reviews, admin monitoring and driver verification
- **Milestone 4**: Testing, Documentation & Deployment — unit/integration/load testing, security hardening, scalability optimizations, full documentation

---

## Glossary

- **System**: The Smart Ride Sharing System platform as a whole
- **Driver**: A registered user who owns a vehicle and posts a trip offering available seats
- **Passenger**: A registered user who searches for and books a seat on a posted trip
- **User**: Any registered individual on the platform (Driver or Passenger role, or both)
- **Trip**: A journey posted by a Driver with a defined origin, destination, route, date/time, available seats, and base fare
- **Booking**: A confirmed reservation made by a Passenger for one or more seats on a Trip
- **Fare**: The monetary amount a Passenger pays for a Booking, calculated based on the distance the Passenger travels along the Trip route
- **Route**: The geographic path of a Trip, defined by waypoints between origin and destination
- **Waypoint**: An intermediate stop along a Route where Passengers may board or alight
- **Matching**: The process of identifying Trips whose Routes overlap with a Passenger's requested origin and destination
- **Payment**: A financial transaction between a Passenger and a Driver, processed through the platform
- **Review**: A rating and optional text comment left by a User about another User after a completed Trip
- **Rating**: A numeric score (1–5) that is part of a Review
- **Admin**: A platform operator with elevated privileges to monitor and manage the System
- **Notification**: An in-platform or email alert sent to a User about a relevant event
- **Geocoder**: The component responsible for converting addresses to geographic coordinates
- **Route_Matcher**: The component responsible for finding Trips that overlap a Passenger's requested route
- **Fare_Calculator**: The component responsible for computing the Fare for a Booking
- **Payment_Processor**: The component responsible for handling payment transactions
- **Review_Service**: The component responsible for collecting and aggregating Reviews and Ratings
- **Notification_Service**: The component responsible for dispatching Notifications to Users
- **Admin_Dashboard**: The interface and backend component used by Admins to monitor and manage the System
- **SMS_Provider**: The third-party service (e.g., Twilio) responsible for delivering SMS messages to Users
- **Distance_API**: The Google Maps Distance Matrix API component used to compute road distances between geographic coordinates
- **Auth_Service**: The Spring Security + JWT component responsible for authentication, token issuance, and role-based access control

---

## Requirements

### Requirement 1: User Registration and Authentication

**User Story:** As a visitor, I want to register and log in securely, so that I can access the platform as a Driver or Passenger.

#### Acceptance Criteria

1. THE System SHALL allow a visitor to register by providing a unique email address, password, full name, and phone number.
2. THE System SHALL allow a visitor to register using a phone number as the primary identifier in place of an email address.
3. WHEN a visitor submits a registration form, THE System SHALL validate that the email address or phone number is not already associated with an existing account.
4. IF a visitor submits a registration form with an email address or phone number already in use, THEN THE System SHALL return a descriptive error message without creating a new account.
5. WHEN a visitor submits valid registration data, THE Auth_Service SHALL hash the password using BCrypt or Argon2 before storing it, with a cost factor sufficient to resist brute-force attacks.
6. WHEN a registered User submits valid credentials, THE Auth_Service SHALL authenticate the User and issue a JWT session token with an expiry of no more than 24 hours.
7. IF a User submits invalid credentials, THEN THE Auth_Service SHALL return an authentication failure message without revealing which field is incorrect.
8. WHEN a User requests a password reset, THE System SHALL send a time-limited reset link to the User's registered email address, valid for no more than 60 minutes.
9. THE System SHALL allow a User to hold both Driver and Passenger roles simultaneously under a single account.
10. WHEN a User logs out, THE Auth_Service SHALL invalidate the User's current session token so that subsequent requests using that token are rejected.

---

### Requirement 2: User Profile Management

**User Story:** As a registered User, I want to manage my profile, so that other Users can trust and identify me.

#### Acceptance Criteria

1. THE System SHALL allow a User to upload a profile photo, update their display name, and update their phone number.
2. THE System SHALL display a User's aggregate Rating on their public profile, calculated as the arithmetic mean of all Ratings received.
3. WHEN a User has received no Reviews, THE System SHALL display the profile without a Rating score.
4. THE System SHALL allow a Driver to add vehicle details including make, model, year, color, license plate number, and passenger capacity to their profile.
5. IF a Driver attempts to post a Trip without vehicle details on their profile, THEN THE System SHALL prompt the Driver to complete vehicle details before proceeding.
6. THE System SHALL display a Driver's verification status on their public profile, indicating whether the Driver's identity and vehicle have been verified by an Admin.
7. WHEN a Driver's profile is viewed, THE System SHALL display the Driver's vehicle details including car model, license plate, and seating capacity.

---

### Requirement 3: Trip Posting

**User Story:** As a Driver, I want to post a trip with route and seat details, so that Passengers can find and book available seats.

#### Acceptance Criteria

1. WHEN a Driver submits a trip posting, THE System SHALL record the origin address, destination address, departure date and time, number of available seats, and base fare per kilometer.
2. THE System SHALL allow a Driver to add up to 5 intermediate Waypoints to a Trip.
3. WHEN a Driver opens the trip posting form, THE System SHALL auto-fill vehicle details (car model, license plate, and capacity) from the Driver's profile.
4. WHEN a Driver submits a trip posting, THE Geocoder SHALL convert all provided addresses and Waypoints to geographic coordinates.
5. IF the Geocoder cannot resolve a provided address, THEN THE System SHALL return a descriptive error and prevent the Trip from being posted.
6. WHEN a Trip is successfully posted, THE System SHALL assign the Trip a unique identifier and set its status to "open", and store the Trip in the rides table.
7. THE System SHALL allow a Driver to cancel a Trip with status "open" at least 2 hours before the scheduled departure time.
8. WHEN a Driver cancels a Trip, THE Notification_Service SHALL notify all Passengers with confirmed Bookings on that Trip.
9. WHILE a Trip has status "open", THE System SHALL allow the Driver to update the number of available seats, provided the updated count is not less than the number of confirmed Bookings.

---

### Requirement 4: Trip Search and Route Matching

**User Story:** As a Passenger, I want to search for trips that match my route, so that I can find a ride going in my direction.

#### Acceptance Criteria

1. WHEN a Passenger submits a search with an origin, destination, and preferred date, THE Route_Matcher SHALL return all Trips whose Routes pass within 10 kilometers of both the Passenger's origin and destination.
2. THE Route_Matcher SHALL identify partial matches where a Passenger's origin and destination fall along the Driver's route between the Driver's origin and destination, even if they do not coincide with the Driver's exact endpoints.
3. THE Route_Matcher SHALL rank search results by ascending detour distance added to the Driver's route to accommodate the Passenger's boarding and alighting points.
4. THE System SHALL allow a Passenger to filter search results by price range (minimum and maximum Fare), vehicle type, and minimum Driver Rating.
5. WHEN no matching Trips are found, THE System SHALL display a message indicating no results and suggest the Passenger adjust the search date or radius.
6. THE System SHALL display for each search result: Driver name, Driver Rating, departure time, available seats, estimated Fare, and vehicle details.
7. WHEN a Passenger selects a Trip from search results, THE System SHALL display the full Route on a map with all Waypoints marked.

---

### Requirement 5: Seat Booking

**User Story:** As a Passenger, I want to book a seat on a matching trip, so that I have a confirmed place on the ride.

#### Acceptance Criteria

1. WHEN a Passenger requests to book a seat on a Trip, THE System SHALL require the Passenger to provide their name and contact number as part of the Booking.
2. WHEN a Passenger requests to book a seat on a Trip, THE System SHALL verify that at least one seat is available before confirming the Booking.
3. IF no seats are available at the time of booking, THEN THE System SHALL inform the Passenger and prevent the Booking from being created.
4. WHEN a Booking is confirmed, THE System SHALL decrement the available seat count on the Trip by the number of seats booked.
5. WHEN a Booking is confirmed, THE System SHALL assign the Booking a unique identifier and set its status to "confirmed".
6. WHEN a Booking is confirmed, THE System SHALL update the available seat count visible on both the Driver's dashboard and the Passenger's dashboard within 60 seconds.
7. THE System SHALL allow a Passenger to cancel a Booking at least 2 hours before the Trip's scheduled departure time.
8. WHEN a Passenger cancels a Booking, THE System SHALL increment the available seat count on the Trip by the number of seats released.
9. WHEN a Booking is confirmed or cancelled, THE Notification_Service SHALL send a confirmation Notification to both the Passenger and the Driver.

---

### Requirement 6: Fare Calculation

**User Story:** As a Passenger, I want the fare to reflect the distance I actually travel, so that I pay a fair and transparent amount.

#### Acceptance Criteria

1. WHEN a Passenger selects a boarding point and alighting point on a Trip, THE Fare_Calculator SHALL query the Distance_API to obtain the road distance in kilometers between those points along the Trip's Route.
2. THE Fare_Calculator SHALL compute the Fare as: `Fare = Base_Fare + (Rate_per_Km × Distance_km)`, where Base_Fare and Rate_per_Km are set by the Driver at trip posting time.
3. WHEN a Trip has multiple Passengers, THE Fare_Calculator SHALL compute each Passenger's Fare proportional to the distance that Passenger travels along the shared route segment.
4. THE Fare_Calculator SHALL display the computed Fare to the Passenger before the Booking is confirmed.
5. THE System SHALL display a fare breakdown showing base fare, distance traveled, and rate per kilometer on the Booking confirmation page.
6. WHEN the Driver updates the base fare per kilometer on an open Trip, THE Fare_Calculator SHALL recompute and display updated estimated Fares for all unconfirmed Bookings on that Trip.
7. WHEN a Booking is confirmed, THE System SHALL lock the Fare at the value computed at confirmation time and SHALL NOT alter it due to subsequent changes to the Trip's base fare.

---

### Requirement 7: Payment Processing

**User Story:** As a User, I want payments to be handled securely through the platform, so that financial transactions are safe and traceable.

#### Acceptance Criteria

1. WHEN a Passenger confirms a Booking, THE Payment_Processor SHALL initiate a payment transaction for the computed Fare using the Passenger's selected payment method via Razorpay, Stripe, or PayPal.
2. THE System SHALL support payment via credit card, debit card, and platform wallet as payment methods.
3. IF a payment transaction fails, THEN THE Payment_Processor SHALL return a descriptive error and THE System SHALL set the Booking status to "payment_failed" without decrementing available seats.
4. WHEN a payment transaction succeeds, THE Payment_Processor SHALL hold the Fare in escrow until the Trip is marked as completed.
5. WHEN a Trip is marked as completed, THE Payment_Processor SHALL release the escrowed Fare to the Driver's platform wallet or bank account, minus the platform service fee.
6. THE System SHALL display the platform service fee percentage to the Driver before the Driver posts a Trip.
7. WHEN a Passenger cancels a Booking more than 24 hours before departure, THE Payment_Processor SHALL initiate a full refund to the Passenger's original payment method within 5 business days.
8. WHEN a Passenger cancels a Booking between 2 and 24 hours before departure, THE Payment_Processor SHALL initiate a 50% refund to the Passenger's original payment method within 5 business days.
9. THE System SHALL record every payment transaction, refund, and wallet balance change in the payments table.
10. THE System SHALL provide each User with a transaction history listing all Payments, refunds, and wallet balance changes associated with their account.

---

### Requirement 8: Route Tracking

**User Story:** As a Passenger, I want to track the trip in real time, so that I know when the Driver is approaching my pickup point.

#### Acceptance Criteria

1. WHILE a Trip has status "in_progress", THE System SHALL update the Driver's geographic position at intervals of no more than 30 seconds.
2. WHILE a Trip has status "in_progress", THE System SHALL display the Driver's current position on a map to all Passengers with confirmed Bookings on that Trip.
3. WHEN the Driver's position is within 2 kilometers of a Passenger's boarding Waypoint, THE Notification_Service SHALL send a proximity alert Notification to that Passenger.
4. THE System SHALL allow a Driver to mark a Trip as "in_progress" no earlier than 30 minutes before the scheduled departure time.
5. THE System SHALL allow a Driver to mark a Trip as "completed" only after the Driver's recorded position has reached within 1 kilometer of the Trip's destination.

---

### Requirement 9: Reviews and Ratings

**User Story:** As a User, I want to leave and receive reviews after a trip, so that the community can make informed decisions about who to travel with.

#### Acceptance Criteria

1. WHEN a Trip is marked as completed, THE Review_Service SHALL make a review form available to both the Driver and each Passenger with a confirmed Booking on that Trip.
2. THE Review_Service SHALL accept a Rating between 1 and 5 (inclusive, integer values) and an optional text comment of up to 500 characters.
3. THE System SHALL allow each User to submit exactly one Review per completed Trip per counterpart User.
4. IF a User attempts to submit a second Review for the same counterpart on the same Trip, THEN THE Review_Service SHALL reject the submission and return a descriptive error.
5. WHEN a Review is submitted, THE Review_Service SHALL update the recipient User's aggregate Rating immediately.
6. THE System SHALL make the review form available for no more than 7 days after the Trip is marked as completed.
7. IF a User attempts to submit a Review after the 7-day window has closed, THEN THE Review_Service SHALL reject the submission and return a descriptive error.
8. THE System SHALL display all Reviews received by a User on that User's public profile, ordered by most recent first.

---

### Requirement 10: Notifications

**User Story:** As a User, I want to receive timely notifications about my trips and bookings, so that I stay informed without having to check the platform manually.

#### Acceptance Criteria

1. THE Notification_Service SHALL deliver Notifications via in-platform alerts, email (Spring Mail), and SMS (Twilio).
2. WHEN a Booking is confirmed, THE Notification_Service SHALL send a Notification to the Passenger and the Driver within 60 seconds of confirmation.
3. WHEN a Trip or Booking is cancelled or rescheduled, THE Notification_Service SHALL send a Notification to all affected Users within 60 seconds of the change.
4. WHEN a Driver's position triggers a proximity alert, THE Notification_Service SHALL deliver the alert to the relevant Passenger within 30 seconds.
5. THE Notification_Service SHALL send a ride reminder Notification to all Passengers with confirmed Bookings on a Trip at a configurable interval before the scheduled departure time, defaulting to 2 hours before departure.
6. THE System SHALL allow a User to configure notification preferences to enable or disable email and SMS Notifications independently of in-platform alerts.
7. IF a Notification cannot be delivered due to a system error, THEN THE Notification_Service SHALL retry delivery up to 3 times at 5-minute intervals before logging the failure.

---

### Requirement 11: Admin Dashboard

**User Story:** As an Admin, I want a dashboard to monitor platform activity and manage users and trips, so that I can ensure platform integrity and resolve disputes.

#### Acceptance Criteria

1. THE Admin_Dashboard SHALL display aggregate platform metrics including total registered Users, total Trips posted, total completed Trips, total payment volume, active Users, and cancellation and dispute counts, updated at intervals of no more than 5 minutes.
2. THE Admin_Dashboard SHALL allow an Admin to search for a User by email address or name and view that User's profile, trip history, and Reviews.
3. THE Admin_Dashboard SHALL allow an Admin to search for a Trip by unique identifier, origin, destination, or Driver name.
4. THE System SHALL allow an Admin to suspend a User account, preventing the suspended User from posting Trips or making Bookings.
5. WHEN an Admin suspends a User account, THE Notification_Service SHALL send a Notification to the suspended User explaining the suspension.
6. THE Admin_Dashboard SHALL allow an Admin to verify a Driver's identity and vehicle details, updating the Driver's verification status on their profile.
7. THE Admin_Dashboard SHALL display all reported disputes, including the reporting User, the reported User, the associated Trip, and the dispute description.
8. THE System SHALL allow an Admin to resolve a dispute by issuing a full or partial refund, overriding the standard cancellation policy.
9. WHEN an Admin issues a refund through dispute resolution, THE Payment_Processor SHALL process the refund within 5 business days.
10. THE Admin_Dashboard SHALL allow an Admin to generate reports covering total rides, total earnings, active Users, and cancellations and disputes for a specified date range.
11. THE Admin_Dashboard SHALL allow an Admin to configure the platform service fee percentage, effective for all Trips posted after the change is saved.

---

### Requirement 12: Testing and Quality Assurance

**User Story:** As a developer, I want a comprehensive test suite and security hardening, so that the platform is reliable, secure, and scalable under real-world load.

#### Acceptance Criteria

1. THE System SHALL include unit tests written with JUnit and Mockito covering all service-layer components, with each test targeting a single unit of behavior.
2. THE System SHALL include integration tests written with Spring Boot Test that verify end-to-end flows including user registration, trip posting, booking, payment, and notification dispatch.
3. THE System SHALL include API tests using Postman collections that validate all documented REST endpoints for correct status codes, response schemas, and error responses.
4. WHEN load tests are executed with JMeter simulating concurrent booking requests, THE System SHALL maintain correct seat availability with no overbooking across all concurrent sessions.
5. THE System SHALL prevent SQL injection attacks by using parameterized queries or an ORM for all database interactions.
6. THE System SHALL prevent cross-site scripting (XSS) attacks by sanitizing and encoding all user-supplied content before rendering it in the web interface.
7. THE System SHALL apply database indexes on the rides table columns used in search queries (source, destination, and departure date) to support efficient search performance.
8. THE System SHALL cache frequently searched ride results to reduce redundant database queries, with a cache TTL of no more than 5 minutes.
9. THE System SHALL provide API documentation generated by Swagger/OpenAPI listing all endpoints, request parameters, response schemas, and authentication requirements.
10. THE System SHALL provide an ER diagram and architecture diagram as part of the developer documentation.
11. THE System SHALL provide a user guide describing the step-by-step flow for Passengers and Drivers.
12. THE System SHALL provide a deployment guide covering local setup and cloud deployment on at least one of AWS, GCP, or Azure.
