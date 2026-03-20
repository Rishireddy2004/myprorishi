# Architecture Diagram

```mermaid
graph TD
    Client["Client\n(Mobile App / Web Browser)"]

    subgraph SpringBoot["Spring Boot Application (JDK 17, Maven)"]
        direction TB
        Controllers["Controllers\nAuthController · UserController · TripController\nBookingController · SearchController · FareController\nReviewController · NotificationController\nTransactionController · AdminController · LocationController"]
        Security["Spring Security + JWT Filter\n(Roles: DRIVER / PASSENGER / ADMIN)"]
        Services["Services\nAuthService · TripService · BookingService\nFareCalculatorService · PaymentService\nRouteMatcherService · TrackingService\nReviewService · NotificationService · AdminService"]
        Repositories["Repositories (Spring Data JPA)\nUserRepository · TripRepository · BookingRepository\nWaypointRepository · ReviewRepository\nTransactionRepository · NotificationRepository\nDisputeRepository · PlatformConfigRepository"]
        WS["WebSocket / STOMP\nLocationWebSocketHandler\n(/topic/trip/{id}/location)"]
    end

    DB["MySQL / PostgreSQL\n(H2 for tests)"]
    Redis["Redis\n(Cache · Pub/Sub · JWT Blocklist)"]
    GoogleMaps["Google Maps APIs\n(Geocoding + Distance Matrix)"]
    Payment["Payment Gateway\n(Stripe / Razorpay)"]
    Email["Email\n(JavaMail / SMTP)"]
    SMS["Twilio SMS"]

    Client -->|"HTTP/HTTPS + JWT Bearer"| Controllers
    Client -->|"WSS / STOMP"| WS
    Controllers --> Security
    Security --> Services
    Services --> Repositories
    Repositories --> DB
    Services -->|"Cache reads/writes\nProximity dedup\nRate limiting"| Redis
    Services -->|"Pub/Sub events\n(booking, cancellation,\nproximity alerts)"| Redis
    Services -->|"Geocode addresses\nCompute road distances"| GoogleMaps
    Services -->|"Hold · Capture\nRefund · Release"| Payment
    Services -->|"Booking confirmations\nTrip updates"| Email
    Services -->|"SMS alerts"| SMS
    WS -->|"Subscribe to\ntrip:{id}:location"| Redis
    WS -->|"Push location\nupdates"| Client
```

## Component Responsibilities

| Component | Technology | Responsibility |
|---|---|---|
| Controllers | `@RestController` | Handle HTTP requests; validate input; delegate to services |
| Security Filter | Spring Security + JWT | Authenticate every request; enforce role-based access |
| Services | `@Service` | Business logic; orchestrate repositories, external APIs, events |
| Repositories | Spring Data JPA | All database interactions via Hibernate; no raw SQL |
| WebSocket Handler | Spring WebSocket + STOMP | Push real-time driver location to connected passengers |
| MySQL / PostgreSQL | JPA `@Entity` | Primary persistent store for all domain data |
| Redis Cache | `RedisTemplate` | Proximity alert deduplication, rate limiting, metrics cache |
| Redis Pub/Sub | `RedisMessageListenerContainer` | Decouple location updates from WebSocket fan-out; notification dispatch |
| Redis JWT Blocklist | `RedisTemplate` | Store invalidated JWT tokens on logout |
| Google Maps APIs | `RestTemplate` | Geocode addresses; compute road distances between waypoints |
| Payment Gateway | Stripe / Razorpay SDK | Tokenize payments; process holds, captures, and refunds |
| JavaMail / SMTP | `JavaMailSender` | Deliver email notifications |
| Twilio SMS | Twilio SDK | Deliver SMS notifications |

## API Documentation

Swagger UI is available at `/swagger-ui.html` when the application is running.  
OpenAPI spec (JSON) is available at `/api-docs`.

All endpoints require a JWT Bearer token in the `Authorization` header except:
- `POST /auth/register`
- `POST /auth/login`
- `POST /auth/password-reset/request`
- `POST /auth/password-reset/confirm`
