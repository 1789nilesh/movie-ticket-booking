# Agent notes

This is a Spring Boot 3.4 / Java 21 Maven service for seat-level movie ticket booking.

## Conventions

- Package root: `com.movietickets` (`domain`, `repository`, `service`, `api`).
- Money is integer cents.
- Auth is the `X-Role` header (`ADMIN` or `CUSTOMER`), not Spring Security.
- Seat allocation must stay race-safe: lock `ShowSeat` rows `order by id` with `PESSIMISTIC_WRITE`, then re-check status.
- `DomainException` is the HTTP error type. Hold/confirm use `noRollbackFor = DomainException` so expired seats can be released even when the request fails.
- `spring.jpa.open-in-view` is false; initialize or fetch-join associations before leaving a `@Transactional` service method.
- Dynamic pricing (weekend multiplier) and discount codes are computed in `PricingService`.
- Cancellation calculates tiered refunds based on `RefundPolicy` and releases `ShowSeat` rows back to `AVAILABLE`.

## Commands

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@21 mvn test
JAVA_HOME=/opt/homebrew/opt/openjdk@21 mvn spring-boot:run
```

Default persistence is H2.

## Docs to keep in sync

When you change APIs or assumptions, update `README.md`. Assignment PDF lives in `docs/`.
