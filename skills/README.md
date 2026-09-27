# Skills used during development

## Cursor agent

The first version of this service was implemented in Cursor (Grok 4.6 agent) from the take-home PDF:

- Scaffolded a Maven Spring Boot 3.4 app (Java 21, JPA, Validation, H2, PostgreSQL driver).
- Modeled cities → theaters → auditoriums → seats, shows with seat snapshots, holds, and bookings.
- Implemented pessimistic locking for concurrent seat holds and a scheduled hold expiry job.
- Added header-based `X-Role` RBAC, Bean Validation, and a global exception handler.
- Wrote unit tests plus integration tests for the booking flow, expiry, and a two-thread hold race.

## Not used in v1

Docker, frontend frameworks, OAuth, observability stacks, and payment SDKs were left out of scope by the assignment.
