# GetKempt — Backend API

Spring Boot 4.1 / Java 25 / MongoDB backend for the GetKempt salon & barbershop platform.
The HTTP contract lives in `src/main/resources/api-spec/` (OpenAPI 3.0.3) and is served
as-is by Swagger UI.

## Stack

| Concern         | Choice                                                     |
|-----------------|------------------------------------------------------------|
| Runtime         | Java 25 LTS (Gradle toolchain, auto-provisioned), Spring Boot 4.1.x |
| Persistence     | MongoDB via Spring Data; tenant-scoped `TenantRepository`  |
| Auth            | Stateless JWT (JJWT), Argon2id passwords, server-side sessions for revocation |
| Docs            | springdoc 3.x serving `static/openapi.yaml` at `/swagger-ui.html` |
| Background work | Transactional outbox + `@Scheduled` dispatcher (email/SMS/push stubs) |

## Prerequisites

- Any JDK 17+ to launch Gradle; the build itself targets **Java 25** and Gradle
  downloads a matching JDK automatically (foojay toolchain resolver) if none is installed.
- Docker (for the local MongoDB) — or any MongoDB 6+/7 **replica set**.

MongoDB **must** run as a replica set, even a single-node one: booking and sale creation
use multi-document transactions, which a standalone `mongod` rejects at runtime.

## Run locally

```bash
# 1. Database (single-node replica set, auto-initiated on first boot)
docker compose up -d mongo

# 2. Configuration (all values have dev defaults — see .env.example)
cp .env.example .env            # optional; or export the variables directly

# 3. API
./gradlew bootRun
```

The API listens on `http://localhost:4000` (override with `SERVER_PORT`).

- Swagger UI: http://localhost:4000/swagger-ui.html
- Raw contract: http://localhost:4000/openapi.yaml

### Smoke test

```bash
curl -s -X POST http://localhost:4000/api/v1/auth/signup \
  -H 'Content-Type: application/json' \
  -d '{"firstName":"Demo","lastName":"Owner","email":"owner@example.com","businessName":"Demo Salon","role":"salon_owner","password":"Password123","placeId":"ChIJ-demo","agreeToTerms":true}'
```

(`placeId` is resolved through the stub `PlacesLookupPort` locally, so any value works.)

Use the returned access token as `Authorization: Bearer <token>` for every other
`/api/v1/**` route. Everything except `/api/v1/auth/**` requires a token.

### Postman

`postman/` holds a ready-made collection and a *GetKempt Local* environment:

1. Import both files into Postman and select the **GetKempt Local** environment.
2. Run **Auth → Signup** (or **Login**). Its test script stores `accessToken` /
   `refreshToken`, and every other request uses the bearer token automatically.
3. "Create" requests store the new id (`clientId`, `serviceId`, …) for the follow-ups.

The collection is generated from the contract and limited to implemented routes;
regenerate it after changing the spec or adding endpoints:

```bash
npx @redocly/cli bundle src/main/resources/api-spec/openapi.yaml \
    -o src/main/resources/static/openapi.yaml      # rebundle the served contract
python3 tools/generate-postman.py                   # needs: pip install pyyaml
```

### Configuration

| Variable                | Default                                   | Notes                                        |
|-------------------------|-------------------------------------------|----------------------------------------------|
| `SERVER_PORT`           | `4000`                                    |                                              |
| `MONGODB_URI`           | `mongodb://localhost:27017/getkempt`      | Append `?directConnection=true` for a single-node replica set |
| `MONGODB_DATABASE`      | `getkempt`                                |                                              |
| `JWT_SECRET`            | insecure dev value                        | **Set in every deployed environment**; ≥ 32 chars |
| `GOOGLE_PLACES_API_KEY` | empty                                     | Optional; store profile enrichment at signup |

Everything else (token TTLs, OTP, pagination limits, outbox cadence) is in
`src/main/resources/application.yml`.

## Build & test

```bash
./gradlew build            # compiles, runs unit tests, produces build/libs/getkempt-*.jar
./gradlew bootJar -x test  # jar only
```

`GetkemptApplicationTests` (full context load) runs only when `MONGODB_URI` is set,
because index creation at startup needs a live database:

```bash
MONGODB_URI='mongodb://localhost:27017/getkempt?directConnection=true' ./gradlew test
```

## Deploy (container)

```bash
docker build -t getkempt-api .
docker run -p 4000:4000 \
  -e MONGODB_URI='mongodb+srv://…/getkempt' \
  -e JWT_SECRET='…' \
  getkempt-api
```

The image is a two-stage build (JDK 25 build → JRE 25 runtime, non-root user) and reads
`JAVA_OPTS` for JVM tuning (`-XX:MaxRAMPercentage=75.0` by default).
`docker compose up --build` runs the same image against the local MongoDB.

## Project layout

```
src/main/java/au/com/getkempt
├── advice/           global exception handler → RFC-style error envelope
├── common/           cross-cutting: errors, ids, idempotency, money, pagination,
│                     tenancy (TenantContext*), TenantRepository base class
├── configurations/   security chains, JWT filters, Mongo, scheduling, MVC, OpenAPI
├── controllers/      one package per domain (auth, bookings, clients, staff, store, …)
├── entities/         MongoDB documents (extend BaseDocument for tenant scoping)
├── enums/            wire enums (lower-case values via WireEnumConverterFactory)
├── integrations/     ports (email, SMS, push, payments, storage, places) + stub impls
├── mappers/          entity ↔ DTO mapping (hand-written)
├── repositories/     TenantRepository subclasses + Spring Data interfaces
├── request/ responses/  API DTOs (records)
└── services/         domain logic
```

## Known gaps (intentionally not yet built)

- All external integrations (`integrations/**`) are stubs that log instead of sending.
- Customer-facing OTP auth (`/api/v1/client/**`), campaigns, reminders, billing,
  payments/Stripe and webhooks exist in the spec but have no implementation.
- `JWTController` (`/api/v1/jwt/**`) and `ShopController` (`/api/v1/shop`) are legacy
  placeholders superseded by `AuthController` and `StoreController`.
