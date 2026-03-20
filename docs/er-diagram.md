# Entity-Relationship Diagram

```mermaid
erDiagram
    USER {
        uuid id PK
        string email UK
        string passwordHash
        string fullName
        string phone
        string photoUrl
        double aggregateRating
        int reviewCount
        boolean isVerified
        boolean isSuspended
        boolean emailNotificationsEnabled
        string role
        timestamp createdAt
    }

    VEHICLE {
        uuid id PK
        uuid user_id FK
        string make
        string model
        int year
        string color
        string licensePlate
        int passengerCapacity
    }

    TRIP {
        uuid id PK
        uuid driver_id FK
        string originAddress
        double originLat
        double originLng
        string destinationAddress
        double destinationLat
        double destinationLng
        timestamp departureTime
        int totalSeats
        int availableSeats
        double baseFarePerKm
        string status
        double serviceFeeRate
        timestamp createdAt
    }

    WAYPOINT {
        uuid id PK
        uuid trip_id FK
        int sequenceOrder
        string address
        double lat
        double lng
    }

    BOOKING {
        uuid id PK
        uuid trip_id FK
        uuid passenger_id FK
        uuid boarding_waypoint_id FK
        uuid alighting_waypoint_id FK
        float fareLocked
        float distanceKm
        int seatsBooked
        string status
        string paymentIntentId
        timestamp createdAt
    }

    REVIEW {
        uuid id PK
        uuid trip_id FK
        uuid reviewer_id FK
        uuid reviewee_id FK
        int rating
        string comment
        timestamp createdAt
    }

    TRANSACTION {
        uuid id PK
        uuid user_id FK
        uuid booking_id FK
        string type
        float amount
        string currency
        string status
        string stripeReference
        timestamp createdAt
    }

    NOTIFICATION {
        uuid id PK
        uuid user_id FK
        string type
        string payloadJson
        boolean isRead
        int retryCount
        timestamp createdAt
    }

    DISPUTE {
        uuid id PK
        uuid reporter_id FK
        uuid reported_id FK
        uuid trip_id FK
        string description
        string status
        float refundAmount
        timestamp createdAt
        timestamp resolvedAt
    }

    PLATFORM_CONFIG {
        string key PK
        string value
        timestamp updatedAt
    }

    USER ||--o{ VEHICLE : "owns"
    USER ||--o{ TRIP : "drives"
    USER ||--o{ BOOKING : "makes"
    USER ||--o{ REVIEW : "writes"
    USER ||--o{ REVIEW : "receives"
    USER ||--o{ TRANSACTION : "has"
    USER ||--o{ NOTIFICATION : "receives"
    USER ||--o{ DISPUTE : "reports"
    USER ||--o{ DISPUTE : "is reported in"
    TRIP ||--o{ WAYPOINT : "has"
    TRIP ||--o{ BOOKING : "contains"
    TRIP ||--o{ REVIEW : "associated with"
    TRIP ||--o{ DISPUTE : "subject of"
    BOOKING ||--o{ TRANSACTION : "generates"
    WAYPOINT ||--o{ BOOKING : "boarding point for"
    WAYPOINT ||--o{ BOOKING : "alighting point for"
```

## Key Constraints

| Entity | Constraint |
|---|---|
| `USER.email` | Unique index |
| `REVIEW(trip_id, reviewer_id, reviewee_id)` | Unique constraint — prevents duplicate reviews |
| `BOOKING.fareLocked` | Set at confirmation time; `updatable = false` |
| `TRIP.serviceFeeRate` | Snapshotted at trip posting time; immune to admin config changes |
| `BOOKING.status` | Enum: `CONFIRMED`, `CANCELLED`, `PAYMENT_FAILED`, `COMPLETED` |
| `TRIP.status` | Enum: `OPEN`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED` |
| `WAYPOINT.sequenceOrder` | 0 = origin, max = destination; intermediates 1–5 |
| `DISPUTE.status` | String: `OPEN`, `RESOLVED` |
