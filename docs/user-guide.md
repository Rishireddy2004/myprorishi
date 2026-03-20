# User Guide

This guide walks through the main flows for both Passengers and Drivers. All requests require a `Bearer <token>` header (obtained after login) unless marked as public.

Base URL: `http://localhost:8080`

---

## Passenger Flow

### Step 1 — Register

**POST** `/api/auth/register`

```json
{
  "name": "Alice Smith",
  "email": "alice@example.com",
  "password": "SecurePass123!",
  "phone": "+15551234567",
  "role": "PASSENGER"
}
```

Response: `201 Created` with user details.

---

### Step 2 — Login

**POST** `/api/auth/login`

```json
{
  "email": "alice@example.com",
  "password": "SecurePass123!"
}
```

Response includes a JWT token:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer"
}
```

Use this token in the `Authorization: Bearer <token>` header for all subsequent requests.

---

### Step 3 — Search for Trips

**GET** `/api/search/trips?origin=New+York,NY&destination=Newark,NJ&date=2024-12-01&seats=1`

| Parameter | Description |
|-----------|-------------|
| `origin` | Pickup location (address or lat,lng) |
| `destination` | Drop-off location |
| `date` | Travel date (YYYY-MM-DD) |
| `seats` | Number of seats needed |

Response: list of available trips with driver info, departure time, available seats, and fare.

---

### Step 4 — View Fare Estimate

**GET** `/api/fare/estimate?origin=New+York,NY&destination=Newark,NJ`

Response:

```json
{
  "estimatedFare": 18.50,
  "distanceKm": 22.3,
  "currency": "USD"
}
```

---

### Step 5 — Book a Trip

**POST** `/api/bookings`

```json
{
  "tripId": 42,
  "seats": 1,
  "paymentMethodId": "pm_card_visa"
}
```

Response: `201 Created` with booking details including `bookingId` and status `PENDING`.

The driver will confirm or reject the booking. You will receive an email/SMS notification when the status changes.

---

### Step 6 — Check Booking Status

**GET** `/api/bookings/{bookingId}`

Possible statuses: `PENDING` → `CONFIRMED` → `IN_PROGRESS` → `COMPLETED` / `CANCELLED`.

---

### Step 7 — Track Driver in Real Time

Once the booking is `CONFIRMED`, connect to the WebSocket endpoint to receive live location updates:

```
ws://localhost:8080/ws/location
```

Subscribe to topic `/topic/trip/{tripId}/location` to receive driver coordinates as the trip progresses.

Alternatively, poll the REST endpoint:

**GET** `/api/location/trip/{tripId}`

```json
{
  "latitude": 40.7128,
  "longitude": -74.0060,
  "updatedAt": "2024-12-01T14:32:00Z"
}
```

---

### Step 8 — Cancel a Booking (if needed)

**DELETE** `/api/bookings/{bookingId}`

Cancellation is allowed before the trip starts. A refund is issued automatically if the payment was already captured.

---

### Step 9 — Leave a Review

After the trip is `COMPLETED`:

**POST** `/api/reviews`

```json
{
  "bookingId": 101,
  "rating": 5,
  "comment": "Great driver, very punctual!"
}
```

Response: `201 Created` with the review details.

---

### Step 10 — View Transaction History

**GET** `/api/transactions`

Returns a paginated list of all payments and refunds for the authenticated passenger.

---

## Driver Flow

### Step 1 — Register

**POST** `/api/auth/register`

```json
{
  "name": "Bob Jones",
  "email": "bob@example.com",
  "password": "SecurePass123!",
  "phone": "+15559876543",
  "role": "DRIVER"
}
```

---

### Step 2 — Login

**POST** `/api/auth/login`

Same as the passenger login. Save the returned JWT token.

---

### Step 3 — Add a Vehicle

**POST** `/api/users/me/vehicle`

```json
{
  "make": "Toyota",
  "model": "Camry",
  "year": 2021,
  "licensePlate": "ABC-1234",
  "color": "Silver",
  "seats": 4
}
```

A vehicle must be registered before posting a trip.

---

### Step 4 — Post a Trip

**POST** `/api/trips`

```json
{
  "origin": "New York, NY",
  "destination": "Newark, NJ",
  "departureTime": "2024-12-01T15:00:00",
  "availableSeats": 3,
  "pricePerSeat": 18.00,
  "waypoints": [
    { "location": "Jersey City, NJ", "order": 1 }
  ]
}
```

Response: `201 Created` with `tripId` and status `SCHEDULED`.

---

### Step 5 — View Incoming Bookings

**GET** `/api/trips/{tripId}/bookings`

Lists all bookings for the trip with passenger details and seat counts.

---

### Step 6 — Confirm or Reject a Booking

**PATCH** `/api/bookings/{bookingId}/status`

```json
{ "status": "CONFIRMED" }
```

or

```json
{ "status": "CANCELLED" }
```

Passengers are notified automatically via email and SMS.

---

### Step 7 — Start the Trip

When you are ready to depart:

**PATCH** `/api/trips/{tripId}/status`

```json
{ "status": "IN_PROGRESS" }
```

---

### Step 8 — Send Live Location Updates

While the trip is in progress, push your GPS coordinates so passengers can track you:

**POST** `/api/location/update`

```json
{
  "tripId": 42,
  "latitude": 40.7128,
  "longitude": -74.0060
}
```

Call this endpoint periodically (e.g., every 5 seconds) from your driver app.

---

### Step 9 — Complete the Trip

**PATCH** `/api/trips/{tripId}/status`

```json
{ "status": "COMPLETED" }
```

This triggers automatic payment capture for all confirmed bookings.

---

### Step 10 — Receive Payout

**GET** `/api/transactions`

Lists all earnings. Payouts are processed via Stripe and transferred to the driver's connected bank account. You can also view payout history in the Stripe Dashboard.

---

### Step 11 — View Reviews Received

**GET** `/api/reviews/driver/{driverId}`

Returns all reviews left by passengers, including ratings and comments.

---

## Admin Flow (brief reference)

Admins authenticate with `role: ADMIN` and have access to additional endpoints:

| Action | Endpoint |
|--------|----------|
| List all users | `GET /api/admin/users` |
| Suspend a user | `PATCH /api/admin/users/{userId}/suspend` |
| View all trips | `GET /api/admin/trips` |
| Cancel any trip | `PATCH /api/admin/trips/{tripId}/cancel` |
| View disputes | `GET /api/admin/disputes` |
| Resolve a dispute | `PATCH /api/admin/disputes/{disputeId}/resolve` |
| View platform metrics | `GET /api/admin/metrics` |
| Update platform config | `PUT /api/admin/config` |

---

## API Reference

Full interactive API documentation is available at:

- Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- OpenAPI JSON: [http://localhost:8080/api-docs](http://localhost:8080/api-docs)
