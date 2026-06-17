# Facility Repair Management & Dispatch System

A **Spring Boot modular monolith** for managing and dispatching facility repair tickets in a university setting. Technicians are automatically assigned using an **Adaptive Weighted Composite Scoring (AWCS)** algorithm. A 3D room viewer built with Three.js lets users locate devices spatially.

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 4.0.6, Java 21, Spring Modulith 2.0.6 |
| Database | PostgreSQL 16 + Liquibase (13 migrations) |
| Cache / Token Blacklist | Redis 7 |
| Frontend | React 19, TypeScript, Vite, Three.js, TanStack Query, Zustand, Tailwind CSS |
| Deployment | Docker Compose |

## Architecture — 9 Modules

```
shared          ← events, exceptions, security config (no dependencies)
identity        ← auth, JWT, users, roles
facility        ← buildings, rooms, devices, categories, 3D scenes
ticket          ← ticket lifecycle, report dedup, severity escalation
sla             ← SLA policies (DB-driven), scheduled breach detection
dispatch        ← auto/manual assignment, AWCS scoring, technician skills
notification    ← in-app notifications (event-driven, no API deps)
audit           ← immutable audit log (event-driven, no API deps)
analytics       ← KPI dashboard, MTTR, SLA compliance, ticket volume
```

Inter-module communication uses typed **Module API interfaces** (sync) and **ApplicationEvents** (async, at-least-once delivery via `spring-modulith-events`).

---

## Prerequisites

- **Docker & Docker Compose** v2+ — everything else runs inside containers
- (Optional, for local dev without Docker) Java 21 + Maven 3.9, PostgreSQL 16, Redis 7

---

## Quick Start — Docker Compose

### 1. Clone the repository

```bash
git clone <repo-url>
cd facility
```

### 2. Configure environment variables

Copy the provided example and edit if needed:

```bash
cp .env .env.local   # optional – compose.yaml already reads .env by default
```

The defaults in `.env` work out-of-the-box for local development. For production change `JWT_SECRET` and database passwords before deploying.

```
POSTGRES_DB=facility_db
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
JWT_SECRET=a-256-bit-secret
JWT_EXPIRATION=86400000        # 24 h in ms
JWT_REFRESH_EXPIRATION=604800000  # 7 d in ms
```

### 3. Start all services

```bash
docker compose up --build
```

This starts four containers:

| Container | Port | Purpose |
|---|---|---|
| `facility-postgres` | 5432 | PostgreSQL database |
| `facility-redis` | 6379 | Redis (JWT blacklist) |
| `facility-app` | 8080 | Spring Boot REST API |
| `facility-frontend` | 80 | React SPA (Nginx) |

> The Spring Boot app waits for Postgres and Redis to pass health checks before starting. Liquibase runs all 13 migrations automatically on first boot.

### 4. Access the app

| URL | Description |
|---|---|
| http://localhost | React frontend |
| http://localhost:8080/api/v1 | REST API base |

---

## Local Development (without Docker)

### Backend

Requires Java 21, Maven 3.9, a running PostgreSQL 16 instance, and Redis 7.

```bash
# Start infrastructure only
docker compose up postgres redis -d

# Run Spring Boot with dev defaults
./mvnw spring-boot:run
```

The app reads environment variables from the shell; the `application.yaml` defaults fall back to `localhost` so no additional config is required.

### Frontend

```bash
cd ../front-end   # adjust path to your frontend directory
npm install
npm run dev       # Vite dev server at http://localhost:5173
```

---

## Default Roles & Seed Data

Liquibase seeds the following accounts on first boot:

| Role | Capabilities |
|---|---|
| `ADMIN` | Full access: dispatch, user management, analytics, audit log, 3D editor |
| `MANAGER` | Submit repair reports, view own tickets, 3D viewer (read-only) |
| `TECHNICIAN` | View assigned tickets, update ticket status, 3D viewer |

Check the seed migration (`V7__seed.yaml`) for default credentials.

---

## API Overview

All endpoints are prefixed with `/api/v1`. Authentication uses **JWT Bearer tokens**.

| Group | Endpoints |
|---|---|
| Auth | `POST /auth/login`, `/auth/refresh`, `/auth/logout`, `/auth/register` |
| Tickets | `GET/POST /tickets`, `GET /tickets/:id`, `PATCH /tickets/:id/status` |
| Dispatch | `POST /dispatch/assign`, `GET /dispatch/history/ticket/:id` |
| Technicians | `GET /technician-skills/eligible?ticketId=X`, `GET /technician-skills/performance/:id` |
| Facility | `GET /buildings`, `GET /rooms`, `GET /devices`, `GET /categories` |
| 3D Scene | `GET/PUT /rooms/:id/scene`, `GET/POST /furniture-types` |
| Analytics | `GET /analytics/dashboard`, `/analytics/ticket-volume`, `/analytics/mttr`, `/analytics/sla-breaches` |
| Notifications | `GET /notifications`, `PATCH /notifications/:id/read` |
| Audit | `GET /audit-logs` |

---

## Key Features

- **Report deduplication** — duplicate reports for the same device within a 24-hour window are merged into an existing open ticket rather than creating a new one.
- **AWCS auto-assignment** — scores eligible technicians on skill match, current workload, historical performance, and urgency-adjusted SLA proximity; weights shift dynamically based on ticket priority.
- **SLA tracking** — policies are DB-driven per category; a `@Scheduled` job detects breaches and publishes `SLABreachEvent`.
- **3D room viewer** — drag-and-drop furniture placement (Admin) with per-device ticket status overlay visible to all roles.
- **Event-driven side effects** — `notification` and `audit` modules consume Spring Modulith application events with at-least-once delivery; no direct coupling to business modules.

---

## Stopping the App

```bash
docker compose down          # stop containers, keep data volumes
docker compose down -v       # stop and delete all data volumes (clean slate)
```
