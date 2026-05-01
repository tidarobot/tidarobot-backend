# TidaroBot — CLAUDE.md

## Project Overview

TidaroBot is a Spring Boot REST API backend for a web application that automates parking spot reservations in an office via the Tidaro external platform. The system handles:

- User registration and JWT-based authentication
- Admin approval workflow for new users (PENDING → APPROVED/REJECTED)
- Planning automated parking reservations (floor selection: -1, -2, outdoor)
- Statistics module: ranking of days with most reservations and most active users

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
├── TidaroBotApplication.java          # Spring Boot entry point
├── auth/
│   ├── controller/AuthController.java # POST /auth/register, /auth/login
│   ├── dto/                           # AuthResponse, LoginRequest, RegisterRequest
│   └── service/
│       ├── AuthService.java           # Registration & login logic
│       └── JwtService.java            # JWT generation & validation (HS256, 24h TTL)
├── config/
│   ├── ApplicationConfig.java         # UserDetailsService, AuthManager, BCrypt beans
│   └── SecurityConfig.java            # CORS, CSRF off, public /auth/**, ADMIN guard on /admin/**
├── exception/
│   ├── UserExistsException.java
│   ├── UserNotFoundException.java
│   └── handler/
│       ├── ApiError.java              # Standardised error response body
│       └── GlobalExceptionHandler.java
├── security/
│   └── JwtAuthenticationFilter.java   # Extracts Bearer token, sets SecurityContext
└── user/
    ├── controller/
    │   ├── UserController.java         # PATCH /users/update, DELETE /users
    │   └── AdminController.java        # PATCH /admin/users/{id}
    ├── dto/UpdateUserRequest.java
    ├── entity/
    │   ├── User.java                   # JPA entity, implements UserDetails; table: _user
    │   ├── Role.java                   # Enum: USER | ADMIN
    │   └── Status.java                 # Enum: PENDING | APPROVED | REJECTED
    ├── repository/UserRepository.java
    └── service/UserService.java
```

---

## API Endpoints

| Method | Path | Auth | Role | Purpose |
|---|---|---|---|---|
| POST | `/auth/register` | No | — | Register; new users start as PENDING |
| POST | `/auth/login` | No | — | Authenticate; returns JWT |
| PATCH | `/users/update` | Yes | USER | Update own email/password |
| DELETE | `/users` | Yes | USER | Delete own account |
| PATCH | `/admin/users/{id}` | Yes | ADMIN | Approve or reject a user |

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

---

## Domain Model

- **User** — core entity; holds `username`, `email`, `passwordHash`, `role`, `status`, `createdAt`, plus `loginTidaro` / `passwordTidaro` for Tidaro integration.
- **Role** — `USER` or `ADMIN`.
- **Status** — `PENDING` (after registration) → `APPROVED` or `REJECTED` (by admin).

---

## Planned Modules (not yet implemented)

- **Parking reservation scheduler** — lets users plan reservations for specific dates and parking floors (-1, -2, outdoor); the bot triggers automated booking on the Tidaro platform.
- **Statistics module**:
  - Ranking of days with the highest number of reservations.
  - Ranking of users executing the most reservations.

---

## Development Notes

- Keep controllers thin; put all business logic in `*Service` classes.
- All REST errors must go through `GlobalExceptionHandler` and return `ApiError`.
- New domain modules follow the same package layout: `controller/`, `dto/`, `entity/`, `repository/`, `service/`.
- Run locally with the `dev` profile to use H2 and skip PostgreSQL setup.
- The project is at an early stage (v0.0.1-SNAPSHOT). The `/users` endpoints still lack explicit security rules — mark any new unprotected endpoints with a TODO until they are secured.
