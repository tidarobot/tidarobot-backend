# TidaroBot — CLAUDE.md

## Project Overview

TidaroBot is a Spring Boot REST API backend for a web application that automates parking spot reservations in an office via the Tidaro external platform. The system handles:

- User registration and JWT-based authentication
- Admin approval workflow for new users (PENDING → APPROVED/REJECTED)
- Planning automated parking reservations (floor selection: MINUS_1, MINUS_2, OUTDOOR; city: CRACOW, WARSAW)
- Background scheduler that triggers the parking bot at the right time
- Statistics module: ranking of days, users, and floors by reservation count

The bot component will authenticate to the external Tidaro system using each user's Tidaro credentials stored at registration.

---

## Tech Stack

| Layer | Technology | Version |
|---|---|---|
| Language | Java | 21 |
| Framework | Spring Boot | 4.0.3 |
| Build tool | Maven (mvnw wrapper) | — |
| Security | Spring Security + JWT (JJWT 0.11.5) | — |
| ORM | Hibernate / Spring Data JPA | — |
| Database (dev) | H2 in-memory | runtime scope |
| Database (prod) | PostgreSQL | localhost:5432/tidarobot |
| Password hashing | BCrypt | — |
| Boilerplate reduction | Lombok | — |
| Testing | JUnit Jupiter | — |

---

## Project Structure

```
src/main/java/org/uj/project/tidarobot/
├── TidaroBotApplication.java
├── auth/
│   ├── controller/AuthController.java     # POST /auth/register, /auth/login
│   ├── dto/                               # AuthResponse, LoginRequest, RegisterRequest
│   └── service/
│       ├── AuthService.java               # Registration & login logic
│       └── JwtService.java                # JWT generation & validation (HS256, 24h TTL)
├── config/
│   ├── ApplicationConfig.java             # UserDetailsService, AuthManager, BCrypt, @EnableScheduling
│   └── SecurityConfig.java                # CORS, CSRF off, public /auth/**, ADMIN on /admin/**
├── exception/
│   ├── InvalidReservationException.java
│   ├── UserExistsException.java
│   ├── UserNotFoundException.java
│   └── handler/
│       ├── ApiError.java                  # Standardised error response body
│       └── GlobalExceptionHandler.java
├── parking/
│   ├── controller/ParkingController.java  # POST/GET /parking/reservations, DELETE /parking/reservations/{id}
│   ├── dto/                               # ParkingReservationRequest, ParkingReservationResponse
│   ├── entity/
│   │   ├── City.java                      # Enum: CRACOW | WARSAW
│   │   ├── Floor.java                     # Enum: MINUS_1 | MINUS_2 | OUTDOOR
│   │   ├── ParkingReservation.java        # JPA entity; table: parking_reservation
│   │   └── ReservationStatus.java         # Enum: SCHEDULED | IN_PROGRESS | COMPLETED | FAILED | CANCELLED
│   ├── repository/ParkingReservationRepository.java
│   ├── scheduler/ParkingScheduler.java    # Polls every 60s; triggers bot for due reservations
│   └── service/
│       ├── ParkingBotService.java         # Executes bot logic (Python integration TODO)
│       └── ParkingReservationService.java # Create / cancel / validate reservations
├── security/
│   └── JwtAuthenticationFilter.java       # Extracts Bearer token, sets SecurityContext
├── stats/
│   ├── controller/StatsController.java    # GET /stats/days, /stats/users, /stats/floors
│   ├── dto/                               # DayRankingEntry, UserRankingEntry, FloorRankingEntry
│   ├── repository/StatsRepository.java    # JPQL group-by queries with optional filters
│   └── service/StatsService.java          # Paginates & sorts ranking results
└── user/
    ├── controller/
    │   ├── AdminController.java           # GET /admin/users, PATCH /admin/users/{id}
    │   └── UserController.java            # PATCH /users/update, DELETE /users
    ├── dto/                               # UpdateUserRequest, UserResponse
    ├── entity/
    │   ├── User.java                      # JPA entity, implements UserDetails; table: _user
    │   ├── Role.java                      # Enum: USER | ADMIN
    │   └── Status.java                    # Enum: PENDING | APPROVED | REJECTED
    ├── repository/
    │   ├── UserRepository.java
    │   └── UserSpecification.java         # JPA Specifications for role/status/username filtering
    └── service/UserService.java
```

---

## API Endpoints

### Auth (public)

| Method | Path | Purpose |
|---|---|---|
| POST | `/auth/register` | Register; new users start as PENDING, returns JWT |
| POST | `/auth/login` | Authenticate; returns JWT |

### User (JWT required)

| Method | Path | Purpose |
|---|---|---|
| PATCH | `/users/update` | Update own email and/or password |
| DELETE | `/users` | Delete own account |

### Admin (JWT + ADMIN role required)

| Method | Path | Query params | Purpose |
|---|---|---|---|
| GET | `/admin/users` | `pageable`, `role`, `status`, `username` | List all users with pagination & filters |
| PATCH | `/admin/users/{id}` | `status` | Approve or reject a user |

### Parking (JWT required)

| Method | Path | Purpose |
|---|---|---|
| POST | `/parking/reservations` | Create a reservation (weekdays only, CRACOW only for now) |
| GET | `/parking/reservations` | List own reservations |
| DELETE | `/parking/reservations/{id}` | Cancel a SCHEDULED reservation |

### Stats (JWT required)

| Method | Path | Query params | Purpose |
|---|---|---|---|
| GET | `/stats/days` | `pageable`, `city`, `status`, `dateFrom`, `dateTo` | Days ranked by reservation count |
| GET | `/stats/users` | `pageable`, `city`, `status`, `dateFrom`, `dateTo` | Users ranked by reservation count |
| GET | `/stats/floors` | `pageable`, `city`, `status`, `dateFrom`, `dateTo` | Floors ranked by reservation count |

> Tokens are passed as `Authorization: Bearer <token>`.  
> A user account is only enabled (login allowed) when `status == APPROVED`.

---

## Configuration & Profiles

| Profile | Database | Activated by |
|---|---|---|
| `dev` | H2 in-memory (`jdbc:h2:mem:testdb`), H2 console at `/h2-console` | `spring.profiles.active=dev` |
| `prod` | PostgreSQL `localhost:5432/tidarobot` | `spring.profiles.active=prod` (default) |

`JWT_SECRET` must be provided as an environment variable (loaded in `application.yml`).  
The file `src/main/resources/jwt.env` is used locally but must **not** be committed with real secrets.

### Parking scheduler properties (`application.yml`)

| Property | Default | Meaning |
|---|---|---|
| `parking.days-before` | `2` | How many working days before the target date to trigger the bot |
| `parking.trigger-hour` | `16` | Hour of day (24h) at which the bot fires |

---

## Domain Model

### User (`_user`)
Holds `username`, `email`, `passwordHash`, `role`, `status`, `createdAt`, plus `loginTidaro` / `passwordTidaro` for Tidaro integration. Implements `UserDetails`; `isEnabled()` returns true only when `status == APPROVED`.

### ParkingReservation (`parking_reservation`)
Links a `User` to a `city`, `floor`, `targetDate` (the date to park), `scheduledFor` (when the bot fires), `status`, and `createdAt`.

### Enums

| Enum | Values |
|---|---|
| `Role` | `USER`, `ADMIN` |
| `Status` | `PENDING`, `APPROVED`, `REJECTED` |
| `City` | `CRACOW`, `WARSAW` |
| `Floor` | `MINUS_1`, `MINUS_2`, `OUTDOOR` |
| `ReservationStatus` | `SCHEDULED`, `IN_PROGRESS`, `COMPLETED`, `FAILED`, `CANCELLED` |

---

## Background Scheduler

`ParkingScheduler` runs every 60 seconds. It queries all `SCHEDULED` reservations whose `scheduledFor <= now` and calls `ParkingBotService.execute()` for each. The bot service sets the status to `IN_PROGRESS`, runs the automation (Python integration is a **TODO**), then marks it `COMPLETED` or `FAILED`.

---

## Exception Handling

All errors go through `GlobalExceptionHandler` and return an `ApiError` JSON body (`status`, `error`, `message`, `path`, `timestamp`).

| Exception | HTTP status |
|---|---|
| `UserExistsException` | 409 Conflict |
| `UserNotFoundException` | 404 Not Found |
| `InvalidReservationException` | 400 Bad Request |

---

## Development Notes

- Keep controllers thin; put all business logic in `*Service` classes.
- All REST errors must go through `GlobalExceptionHandler` and return `ApiError`.
- New domain modules follow the same package layout: `controller/`, `dto/`, `entity/`, `repository/`, `service/`.
- Run locally with the `dev` profile to use H2 and skip PostgreSQL setup.
- Warsaw city support is not yet implemented in `ParkingReservationService` (throws `InvalidReservationException`).
- The Python bot integration in `ParkingBotService.execute()` is a TODO — currently mocked with logging.
- The `/users` endpoints still lack explicit security rules — mark any new unprotected endpoints with a TODO until they are secured.
