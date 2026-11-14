# Nordia — Meeting Rooms API

A REST API that replaces Nordia's Excel sheet for booking meeting rooms: list rooms, book them, cancel bookings, and see a room's schedule for a day, with the company's booking rules enforced server-side.

**Stack:** Java 21 · Spring Boot 4.0 · Spring Data JPA (Hibernate) · SQLite · Spring Security (HTTP Basic) · springdoc-openapi · JUnit 5 / MockMvc · Docker

---

## Requirements

- **JDK 21 or newer** (developed and tested on JDK 21+; the code targets Java 21)
- **or Docker**, if you prefer not to install a JDK

No Maven installation needed: the project ships with the Maven Wrapper (`mvnw` / `mvnw.cmd`), which downloads the right Maven version on first run.

---

## Run

All commands are run from the repository root.

### With the JDK

```bash
# macOS / Linux / Git Bash
./mvnw spring-boot:run

# Windows (cmd or PowerShell)
mvnw.cmd spring-boot:run
```

The API starts on **http://localhost:8090**.
On first start it creates the SQLite file `nordia.db` and seeds the 6 rooms from `rooms.json`. Restarting reuses the same file; delete it to start from a clean state.

### With Docker

```bash
docker build -t nordia-rooms .
docker run --rm -p 8090:8090 -v nordia-data:/data nordia-rooms
```

The database lives in the `nordia-data` volume, so bookings survive container restarts. The image is multi-stage (JDK to build, JRE to run) and runs as a non-root user. Tests are not run during `docker build`; run them with the command below.

### Configuration

Everything has a working default; override with environment variables if needed.

| Variable | Default | Purpose |
|---|---|---|
| `PORT` | `8090` | HTTP port |
| `DB_PATH` | `nordia.db` (Docker: `/data/nordia.db`) | SQLite file location |
| `APP_TIMEZONE` | `Europe/Oslo` | Company timezone used for all booking rules |
| `ALICE_PASSWORD` / `BOB_PASSWORD` / `ADMIN_PASSWORD` | `alice-pass` / `bob-pass` / `admin-pass` | Demo user passwords |

---

## Test

```bash
./mvnw test        # Windows: mvnw.cmd test
```

Tests run against an in-memory SQLite database and a controllable clock, so they are fast, isolated from your local `nordia.db`, and independent of the current date.

| Test class | Kind | What it covers |
|---|---|---|
| `BookingPolicyTest` | Unit (no Spring) | Opening hours, weekends, min/max duration, end before start, past start, whole-minute precision — including exact boundaries (08:00, 19:00, 15 min, 4 h) |
| `BookingApiTest` | Integration (MockMvc + real security + DB) | Creation, overlap (partial / enclosing / enclosed), touching bookings allowed, same slot in another room, cancelled booking frees the slot, 3-per-day limit (cancelled don't count, past ones of the same day do), cancellation rules (future only, only once, organiser or admin), room schedule for a day, malformed input |
| `RoomApiTest` | Integration | Listing, `minCapacity` filter, pagination, invalid query parameters, authentication required |
| `BookingConcurrencyTest` | Integration | 10 simultaneous requests for the same slot → exactly one succeeds |

---

## Try it

### Swagger UI

Open **http://localhost:8090/swagger-ui.html**, click **Authorize**, and log in with one of the demo users:

| User | Password | Role |
|---|---|---|
| `alice` | `alice-pass` | USER |
| `bob` | `bob-pass` | USER |
| `admin` | `admin-pass` | ADMIN |

The raw OpenAPI document is at `/v3/api-docs`.

### curl

```bash
# Rooms with at least 10 seats
curl -u alice:alice-pass "http://localhost:8090/api/rooms?minCapacity=10"

# Create a booking (use a future weekday)
curl -i -u alice:alice-pass -H "Content-Type: application/json" \
  -d '{"roomId":1,"start":"2026-11-16T10:00","end":"2026-11-16T11:00","subject":"Team sync"}' \
  http://localhost:8090/api/bookings

# A room's bookings for a day
curl -u alice:alice-pass "http://localhost:8090/api/rooms/1/bookings?date=2026-11-16"

# Cancel booking 1
curl -i -u alice:alice-pass -X DELETE http://localhost:8090/api/bookings/1
```

On Windows PowerShell, use `curl.exe` instead of `curl`, and escape the inner double quotes of the JSON body (`\"`).

---

## Endpoints

All endpoints require authentication (HTTP Basic). Times are **local to the company timezone**, ISO-8601 **without offset**, e.g. `2026-11-16T10:00`.

| Method | Path | Description | Success |
|---|---|---|---|
| `GET` | `/api/rooms?minCapacity=&page=&size=` | List rooms, optionally with capacity ≥ `minCapacity`. Paginated (`page` from 0, `size` 1–100, default 20) | 200 |
| `POST` | `/api/bookings` | Create a booking. Body: `roomId`, `start`, `end`, `subject` | 201 + `Location` |
| `GET` | `/api/bookings/{id}` | Get one booking (including cancelled ones) | 200 |
| `DELETE` | `/api/bookings/{id}` | Cancel a booking | 204 |
| `GET` | `/api/rooms/{roomId}/bookings?date=YYYY-MM-DD` | Active bookings of a room for a day, sorted by start | 200 |

Example booking response:

```json
{
  "id": 1,
  "roomId": 1,
  "start": "2026-11-16T10:00:00",
  "end": "2026-11-16T11:00:00",
  "organiser": "alice",
  "subject": "Team sync",
  "status": "ACTIVE"
}
```

---

## Business rules

| Rule | Implementation |
|---|---|
| No overlap in the same room | Rejected if an active booking satisfies `existing.start < new.end AND existing.end > new.start`. Touching bookings (one ends 10:00, next starts 10:00) are allowed. |
| Monday–Friday, 08:00–19:00 | Start and end on the same weekday, start ≥ 08:00, end ≤ 19:00 |
| 15 minutes to 4 hours | Both bounds inclusive |
| Max 3 active bookings per organiser per day | "Active" = not cancelled; see assumptions |
| Only a booking that hasn't started can be cancelled | Cancellation allowed while `now < start` |

---

## Errors

Every error uses the [RFC 9457 Problem Details](https://www.rfc-editor.org/rfc/rfc9457) format, plus a stable `code` that clients can rely on instead of parsing messages:

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "Room Aurora is already booked during this time.",
  "instance": "/api/bookings",
  "code": "ROOM_ALREADY_BOOKED"
}
```

Validation errors also include an `errors` object mapping each invalid field to its message.

| Status | Meaning | Codes |
|---|---|---|
| 400 | Malformed request: missing or invalid field, bad date format, unknown JSON field, invalid query parameter | `VALIDATION_FAILED` (plus Spring's standard 400s) |
| 401 | Missing or wrong credentials | `UNAUTHENTICATED` |
| 403 | Authenticated but not allowed | `NOT_BOOKING_ORGANISER` |
| 404 | Unknown room or booking | `ROOM_NOT_FOUND`, `BOOKING_NOT_FOUND` |
| 409 | Conflicts with the current state | `ROOM_ALREADY_BOOKED`, `DAILY_LIMIT_REACHED`, `BOOKING_ALREADY_CANCELLED`, `BOOKING_ALREADY_STARTED` |
| 422 | Well-formed but breaks a booking rule | `OUTSIDE_OPENING_HOURS`, `INVALID_DURATION`, `INVALID_TIME_RANGE`, `START_IN_PAST`, `TIME_NOT_ON_MINUTE` |
| 500 | Unexpected error (details are logged server-side, never returned) | `INTERNAL_ERROR` |

---

## Choices and assumptions

- **"Active" for the 3-per-day limit = not cancelled.** Bookings that already took place earlier the same day still count: the rule reads as a daily quota, and dropping past bookings would let someone book again as each meeting ends.
- **The organiser is the authenticated user**, not a field in the request. Otherwise anyone could book under someone else's name, use up their daily quota, or cancel their meetings. Sending `organiser` in the body is rejected with a 400. Only the organiser or an `ADMIN` can cancel a booking.
- **Local time in one company timezone (`Europe/Oslo`), not UTC.** All rules are wall-clock rules for one office. Daylight-saving transitions happen at night, outside 08:00–19:00, so local durations are always exact.
- **Times must be on whole minutes**, and **bookings must start in the future**.
- **Cancellation is a soft delete**: the booking stays, with status `CANCELLED`, and no longer blocks its slot or counts toward the daily limit.
- **SQLite with a single connection.** The overlap check and the insert run in one transaction, and the single connection serialises writes, so two simultaneous requests can never double-book a slot (covered by `BookingConcurrencyTest`). This trades write concurrency for simplicity — fine at Nordia's scale.
- **Dates are stored as fixed-width ISO text** (`2026-11-16T10:00:00`), which SQLite compares correctly as strings and which stays timezone-independent.
- **Pagination on rooms only.** A room's bookings for one day are bounded by the rules (at most 44 slots of 15 minutes), so paging them would add complexity for nothing.
- **Business rules live in `BookingPolicy`**, a plain class with no database access, so they are unit-tested at their exact boundaries.

## Not included

- Creating or editing rooms (they are seeded from `rooms.json`), and editing bookings (cancel and rebook instead)
- Database migrations (Flyway): the schema is generated by Hibernate
- Production-grade authentication: users are in configuration and HTTP Basic must sit behind HTTPS; a real deployment would use an identity provider and tokens

---

## Project structure

```
├── src/main/java/com/nordia/rooms/
│   ├── RoomsApiApplication.java
│   ├── config/    security, OpenAPI, clock
│   ├── room/      Room entity, repository, seeder, service, controller
│   ├── booking/   Booking entity, rules (BookingPolicy), service, controller
│   └── common/    error codes, global error handler, pagination, date converter
├── src/main/resources/
│   ├── application.yml
│   └── rooms.json
├── src/test/      unit, integration and concurrency tests
├── Dockerfile
├── pom.xml
└── README.md
```
