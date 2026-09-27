# Movie Ticket Booking System

Spring Boot backend for seat-level movie ticket booking. Features include catalog management, time-limited seat holds, race-safe confirmation with pessimistic locking, dynamic weekend pricing, discount codes, customer booking history, and configurable refund policies on booking cancellation.

## Run

Requires Java 21 and Maven 3.9+.

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@21 mvn test
JAVA_HOME=/opt/homebrew/opt/openjdk@21 mvn spring-boot:run
```

Default database is in-memory H2 (schema created by Hibernate, reset on restart). H2 console: [http://localhost:8080/h2-console](http://localhost:8080/h2-console) (JDBC URL `jdbc:h2:mem:tickets`).

To use PostgreSQL instead:

```bash
DB_URL=jdbc:postgresql://localhost:5432/tickets DB_USERNAME=tickets DB_PASSWORD=tickets mvn spring-boot:run
```

## API Outline

### Admin Endpoints (`X-Role: ADMIN`)
- `POST /api/admin/cities` — Create city (returns 409 CONFLICT if name exists)
- `POST /api/admin/theaters` — Create theater
- `POST /api/admin/auditoriums` — Create auditorium
- `POST /api/admin/seats` — Create seat (`REGULAR` or `PREMIUM`)
- `POST /api/admin/movies` — Create movie
- `POST /api/admin/shows` — Create show (snapshots auditorium seats onto show)
- `POST /api/admin/discounts` — Create discount code (percentage or flat discount)
- `GET /api/admin/discounts` — List discount codes
- `POST /api/admin/refund-policies` — Create/update refund policy tier (`hoursBeforeShow`, `refundPercentage`)
- `GET /api/admin/refund-policies` — List refund policies

### Public Endpoints
- `GET /api/cities`
- `GET /api/cities/{cityId}/shows`
- `GET /api/shows/{showId}/seats`

### Customer Endpoints (`X-Role: CUSTOMER`)
- `POST /api/shows/{showId}/holds` — Create seat hold
- `POST /api/holds/{token}/confirm` — Confirm hold with optional `discountCode`
- `GET /api/bookings?email={email}` — Get customer booking history
- `POST /api/bookings/{reference}/cancel` — Cancel booking and release seats with tiered refund

## Pricing & Rules

- **Dynamic Weekend Pricing**: Shows on Saturday or Sunday automatically apply a 1.20x (20%) weekend boost on seat prices (configurable via `pricing.weekend-multiplier`).
- **Discount Codes**: Applied at confirmation time. Supports percentage-off or flat cents off, with validity checks (`validUntil`, `maxUses`).
- **Cancellation & Refunds**: Configurable refund percentage based on hours remaining until show start (e.g. >=24h -> 100%, >=2h -> 50%, <2h -> 0%). Released seats return to `AVAILABLE` status.

## Example Flow

```bash
# 1. Create City
curl -X POST http://localhost:8080/api/admin/cities \
  -H 'X-Role: ADMIN' -H 'Content-Type: application/json' \
  -d '{"name":"Bengaluru"}'

# 2. Hold Seats
curl -X POST http://localhost:8080/api/shows/1/holds \
  -H 'X-Role: CUSTOMER' -H 'Content-Type: application/json' \
  -d '{"customerEmail":"sam@example.com","showSeatIds":[1,2]}'

# 3. Confirm Hold with Discount Code
curl -X POST http://localhost:8080/api/holds/HOLD_TOKEN/confirm \
  -H 'X-Role: CUSTOMER' -H 'Content-Type: application/json' \
  -d '{"customerEmail":"sam@example.com", "discountCode":"SAVE20"}'

# 4. View Booking History
curl -s "http://localhost:8080/api/bookings?email=sam@example.com" \
  -H 'X-Role: CUSTOMER'

# 5. Cancel Booking
curl -X POST http://localhost:8080/api/bookings/BKG-XXXXXXXX/cancel \
  -H 'X-Role: CUSTOMER' -H 'Content-Type: application/json' \
  -d '{"customerEmail":"sam@example.com"}'
```

## Concurrency & Design Notes

Selected `show_seats` rows are locked with a pessimistic write lock ordered by id (`SELECT ... FOR UPDATE`). Availability is rechecked before hold or confirmation. Expired holds are released on access and by a background scheduler (~30s).

## Tests

`mvn test` covers:
- Pricing and hold expiry rules
- HTTP hold -> confirm happy path
- Conflicting hold & expired hold (409/410)
- Concurrent seat contention between parallel threads
- Dynamic weekend pricing & discount code application
- Customer booking history retrieval
- Cancellation, seat releasing, and tiered refund calculation
