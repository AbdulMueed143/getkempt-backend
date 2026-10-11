# GetKempt — Backend API Specification

> OpenAPI 3.0.3 contract for the **GetKempt** salon & barbershop management platform.
> This package is the single source of truth that the backend repository
> (`getkempt-backend`) uses to generate route handlers, request/response models,
> and the TypeScript client that the frontend consumes.

---

## What's in here

```
api-spec/
├── README.md                  # This file
├── openapi.yaml               # Root OpenAPI 3.0.3 document (entry point)
│
├── paths/                     # One file per domain — request/response only
│   ├── auth.yaml              # signup, login, refresh, password reset
│   ├── users.yaml             # /me, profile, change password
│   ├── staff.yaml             # staff CRUD, invitations
│   ├── clients.yaml           # client CRUD, search, notes
│   ├── services.yaml          # service catalogue CRUD
│   ├── packages.yaml          # bundled services CRUD
│   ├── bookings.yaml          # bookings CRUD, calendar feeds, slot search
│   ├── availability.yaml      # weekly schedule + date overrides
│   ├── visits.yaml            # past appointments — attended / no-show
│   ├── loyalty.yaml           # programs + client progress + redemption
│   ├── store.yaml             # store profile + settings + holidays
│   ├── campaigns.yaml         # broadcast email/SMS/push campaigns
│   ├── reminders.yaml         # automated reminder rule engine + stats
│   ├── dashboard.yaml         # aggregated stats, alerts, charts
│   ├── payments.yaml          # Stripe Connect, deposits, charges, gift cards
│   ├── billing.yaml           # subscription, SMS credits, invoices
│   ├── notifications.yaml     # in-app alert bell + push device registration
│   ├── uploads.yaml           # presigned image upload URLs
│   ├── public.yaml            # unauthenticated client-facing booking widget
│   └── webhooks.yaml          # incoming (Stripe) + outgoing (delivery callbacks)
│
├── components/
│   ├── schemas.yaml           # All data models (~120 schemas) grouped by domain
│   ├── parameters.yaml        # Reusable path / query / header parameters
│   ├── responses.yaml         # Reusable error responses (400/401/403/404/409/422/429/5xx)
│   └── securitySchemes.yaml   # BearerJWT + ApiKey + WebhookSignature
│
└── docs/
    ├── 01-conventions.md      # URL shape, casing, dates, money, IDs, idempotency
    ├── 02-authentication.md   # JWT pair, refresh flow, RBAC matrix, token claims
    ├── 03-multi-tenancy.md    # Shop scoping, X-Shop-Id, data isolation rules
    ├── 04-domain-model.md     # Entity-relationship diagram + table dependencies
    ├── 05-booking-lifecycle.md # State machine, slot search, race conditions
    ├── 06-payments.md         # Stripe Connect flow, deposits, refunds, fees
    ├── 07-notifications.md    # Reminder engine, campaigns, channel routing
    ├── 08-rate-limiting.md    # Quotas per endpoint family, 429 contract
    └── 09-dependencies.md     # Third-party services + infra the API needs
```

---

## How to consume this spec

### Generate a TypeScript client

```bash
npx @hey-api/openapi-ts \
  -i ./api-spec/openapi.yaml \
  -o ./src/lib/api/generated \
  -c @hey-api/client-fetch
```

### Generate a NestJS / Express backend (Node)

```bash
npx openapi-generator-cli generate \
  -i ./api-spec/openapi.yaml \
  -g typescript-node-server \
  -o ./generated
```

### Generate a Go backend (Echo / Gin)

```bash
oapi-codegen --config oapi-codegen.yaml ./api-spec/openapi.yaml
```

### Generate Rust (Axum)

```bash
cargo install openapi-generator-cli
openapi-generator generate -i api-spec/openapi.yaml -g rust-server -o ./backend
```

### Validate the spec

```bash
npx @redocly/cli lint ./api-spec/openapi.yaml
npx @redocly/cli bundle ./api-spec/openapi.yaml -o ./dist/openapi.bundle.yaml
```

---

## Design principles

1. **REST + JSON.** Resource-oriented URLs, standard HTTP verbs. No GraphQL.
2. **Versioned at the path prefix.** All endpoints sit under `/api/v1/`.
3. **Multi-tenant from day one.** Every authenticated request is scoped to a
   `shopId` derived from the JWT — clients never pass it in URLs.
4. **Idempotent writes.** All mutating endpoints accept an `Idempotency-Key`
   header. Retries are safe.
5. **Cursor pagination.** List endpoints use `?cursor=…&limit=…` rather than
   page numbers. Cursors are opaque base64 tokens.
6. **All times are ISO-8601 UTC** in the wire format. Localisation happens
   client-side using the shop's `timezone` field on `StoreProfile`.
7. **Money is integer cents.** `priceCents: 6500` ≡ $65.00 AUD. Never floats.
8. **No PII in URLs.** Email, phone numbers, etc. are never path or query
   params — always in the request body or derived from the JWT.
9. **Soft-delete by default.** DELETE endpoints set `deletedAt` rather than
   removing rows, so historical bookings retain references.
10. **Webhooks are signed.** All outgoing webhooks include an
    `X-GetKempt-Signature` HMAC-SHA256 header keyed on the per-tenant secret.

See [`docs/01-conventions.md`](./docs/01-conventions.md) for the full rulebook.

---

## Domain map at a glance

| Domain | Endpoints | Owns | Depends on |
|---|---|---|---|
| `auth` | 8 | sessions, tokens | users |
| `users` | 4 | account, profile | auth |
| `staff` | 10 | staff members, invites | auth, services |
| `clients` | 9 | client records, notes | bookings (read-only aggregates) |
| `services` | 8 | service catalogue | staff |
| `packages` | 7 | bundled services | services, staff |
| `bookings` | 14 | appointments, calendar | staff, services, packages, clients, availability |
| `availability` | 9 | weekly schedule, overrides | staff |
| `visits` | 6 | post-appointment review | bookings |
| `loyalty` | 11 | programs, client progress | bookings, clients, payments |
| `store` | 12 | profile, settings, holidays | — (root tenant data) |
| `campaigns` | 6 | broadcast messaging jobs | clients, staff |
| `reminders` | 9 | automated rule engine | bookings, store settings |
| `dashboard` | 5 | aggregated read-only views | all |
| `payments` | 14 | Stripe Connect, charges, refunds | bookings, store settings |
| `billing` | 9 | subscription, SMS credits | payments |
| `notifications` | 6 | in-app alerts, push devices | reminders, campaigns |
| `uploads` | 2 | presigned S3/R2 URLs | — |
| `public` | 8 | unauthenticated booking widget | bookings, services, staff, availability |
| `webhooks` | 4 | Stripe in / delivery callbacks | payments, reminders, campaigns |

Total: **~160 endpoints across 20 path files**.

See [`docs/04-domain-model.md`](./docs/04-domain-model.md) for the full ERD.

---

## Status

- ☑ Schemas — complete for v0.2 (booking + payments + reminders)
- ☑ Auth — JWT pair + email verification + password reset
- ☑ Multi-tenant scoping rules
- ☐ GraphQL subscription channel for real-time calendar updates _(post-v0.2)_
- ☐ Multi-location enterprise endpoints _(post-v0.5)_

---

## License & ownership

Internal — GetKempt. Do not share outside the engineering org.
