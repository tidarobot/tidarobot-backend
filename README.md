# TidaroBot

Spring Boot REST API backend for automating parking spot reservations via the Tidaro platform.

## Running with Docker

### Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/)

### 1. Clone both repositories into the same folder

```
projects/
├── tidaroBot/          # this repository
└── tidaroBotFrontend/  # frontend repository
```

```bash
git clone <backend-repo-url> tidaroBot
git clone <frontend-repo-url> tidaroBotFrontend
```

### 2. Configure environment variables

Inside the `tidaroBot/` directory, copy `.env.example` to `.env` and fill in the values:

```bash
cp .env.example .env
```

```env
JWT_SECRET=your_secret_key_here
DB_USERNAME=your_db_username
DB_PASSWORD=your_db_password
```

> `JWT_SECRET` should be a long random string (at least 32 characters).

### 3. Start the application

```bash
cd tidaroBot
docker-compose up --build
```

The app will be available at:
- Frontend: http://localhost
- Backend API: http://localhost:8080