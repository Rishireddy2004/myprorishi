# Local Setup Guide

## Prerequisites

| Tool | Version |
|------|---------|
| JDK | 17+ |
| Maven | 3.9+ |
| MySQL | 8.0+ (or PostgreSQL 15+) |
| Redis | 7.0+ |

## 1. Clone and Build

```bash
git clone <repository-url>
cd smart-ride-sharing-system
mvn clean package -DskipTests
```

## 2. Database Setup

### MySQL

```sql
CREATE DATABASE ridesharing CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'ridesharing'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON ridesharing.* TO 'ridesharing'@'localhost';
FLUSH PRIVILEGES;
```

### PostgreSQL (alternative)

```sql
CREATE DATABASE ridesharing;
CREATE USER ridesharing WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE ridesharing TO ridesharing;
```

If using PostgreSQL, update `DB_URL` to `jdbc:postgresql://localhost:5432/ridesharing` and ensure the PostgreSQL dialect is set in `application.yml`.

> The application uses `ddl-auto: update`, so Hibernate will create/update tables automatically on first run.

## 3. Redis Setup

Start Redis with default settings (no password for local dev):

```bash
redis-server
```

Or with a password:

```bash
redis-server --requirepass your_redis_password
```

## 4. Environment Variables

Set the following environment variables before running the application. You can export them in your shell or create a `.env` file and source it.

| Variable | Description | Example |
|----------|-------------|---------|
| `DB_URL` | JDBC connection URL | `jdbc:mysql://localhost:3306/ridesharing` |
| `DB_USERNAME` | Database username | `ridesharing` |
| `DB_PASSWORD` | Database password | `your_password` |
| `REDIS_HOST` | Redis hostname | `localhost` |
| `REDIS_PORT` | Redis port | `6379` |
| `REDIS_PASSWORD` | Redis password (leave empty if none) | `` |
| `JWT_SECRET` | Secret key for signing JWTs (min 256-bit) | `a-very-long-random-secret-key-here` |
| `GOOGLE_MAPS_API_KEY` | Google Maps Platform API key (Distance Matrix + Geocoding) | `AIza...` |
| `STRIPE_SECRET_KEY` | Stripe secret key | `sk_test_...` |
| `STRIPE_PUBLISHABLE_KEY` | Stripe publishable key | `pk_test_...` |
| `TWILIO_ACCOUNT_SID` | Twilio Account SID | `AC...` |
| `TWILIO_AUTH_TOKEN` | Twilio Auth Token | `...` |
| `TWILIO_FROM_NUMBER` | Twilio SMS sender number (E.164 format) | `+15551234567` |
| `MAIL_HOST` | SMTP host | `smtp.gmail.com` |
| `MAIL_PORT` | SMTP port | `587` |
| `MAIL_USERNAME` | SMTP username / email address | `you@gmail.com` |
| `MAIL_PASSWORD` | SMTP password or app password | `...` |

### Quick export (bash)

```bash
export DB_URL=jdbc:mysql://localhost:3306/ridesharing
export DB_USERNAME=ridesharing
export DB_PASSWORD=your_password
export REDIS_HOST=localhost
export REDIS_PORT=6379
export REDIS_PASSWORD=
export JWT_SECRET=change-me-to-a-long-random-secret-key-at-least-256-bits
export GOOGLE_MAPS_API_KEY=your_google_maps_api_key
export STRIPE_SECRET_KEY=sk_test_your_stripe_secret_key
export STRIPE_PUBLISHABLE_KEY=pk_test_your_stripe_publishable_key
export TWILIO_ACCOUNT_SID=your_twilio_account_sid
export TWILIO_AUTH_TOKEN=your_twilio_auth_token
export TWILIO_FROM_NUMBER=+15551234567
export MAIL_HOST=smtp.gmail.com
export MAIL_PORT=587
export MAIL_USERNAME=you@gmail.com
export MAIL_PASSWORD=your_app_password
```

## 5. Run the Application

```bash
java -jar target/smart-ride-sharing-system-0.0.1-SNAPSHOT.jar
```

The application starts on port **8080** by default.

## 6. Verify

- Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- OpenAPI JSON: [http://localhost:8080/api-docs](http://localhost:8080/api-docs)
- Health check: `curl http://localhost:8080/api/auth/login` (should return 400, not 404)

## External API Keys

### Google Maps
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Enable **Distance Matrix API** and **Geocoding API**
3. Create an API key and restrict it to those two APIs

### Stripe
1. Sign up at [stripe.com](https://stripe.com)
2. Use test keys from the Dashboard → Developers → API keys

### Twilio
1. Sign up at [twilio.com](https://twilio.com)
2. Get Account SID and Auth Token from the Console Dashboard
3. Buy or use a trial phone number as `TWILIO_FROM_NUMBER`

### Gmail SMTP
Use an [App Password](https://support.google.com/accounts/answer/185833) if 2FA is enabled on your Google account.
