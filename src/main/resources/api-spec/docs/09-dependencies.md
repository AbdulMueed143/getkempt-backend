# External Dependencies & Infrastructure

This is the running list of third-party services and self-hosted
infrastructure the backend depends on. Anyone implementing the API needs
accounts, env vars, and IAM access to all of these.

---

## Required SaaS

| Service | Purpose | Where used | Required env vars |
|---|---|---|---|
| **PostgreSQL 15+** | Primary DB | All persistence | `DATABASE_URL` |
| **Redis 7+** | Rate limit + cache + job queue (BullMQ) | Rate limits, idempotency, reminder dispatch | `REDIS_URL` |
| **Stripe** | Payments + Connect + Subscriptions | `/payments/*`, `/billing/*`, `/webhooks/stripe` | `STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET`, `STRIPE_PLATFORM_PUBLISHABLE_KEY` |
| **SendGrid** (or Postmark) | Transactional email | Auth emails, reminders, campaigns | `SENDGRID_API_KEY`, `SENDGRID_WEBHOOK_PUBLIC_KEY` |
| **Twilio** | SMS | Reminders, campaigns | `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_MESSAGING_SERVICE_SID` |
| **Expo Push** | Push notifications (mobile clients later) | `/notifications/*` | `EXPO_ACCESS_TOKEN` |
| **AWS S3** (or Cloudflare R2) | Image uploads | `/uploads/*` | `S3_BUCKET`, `S3_REGION`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `S3_PUBLIC_CDN_URL` |
| **Cloudflare** | Edge / CDN / WAF / DDoS / rate-limit edge | Routing | n/a — DNS only |
| **Google Maps Geocoding API** | Address → lat/lng + Place ID | `StoreAddress` resolution | `GOOGLE_MAPS_SERVER_KEY` |
| **hCaptcha** (or Cloudflare Turnstile) | Bot protection on public booking + login | `/public/*`, `/auth/signup` | `HCAPTCHA_SECRET` |
| **Sentry** | Error monitoring | Server runtime | `SENTRY_DSN` |
| **Datadog** (or Grafana Cloud) | APM + logs + metrics | Server runtime | `DD_API_KEY` |

## Optional / future

| Service | Purpose | Needed for |
|---|---|---|
| **Algolia / Meilisearch** | Full-text search for clients | v0.3 client search at scale |
| **ClickHouse / Snowflake** | Analytics warehouse | Dashboard charts beyond 90 days |
| **Resend** | Alternative transactional email | If we ditch SendGrid |
| **Cloudflare R2** | Cheaper S3-compatible object storage | Image archive |
| **OpenAI / Anthropic** | AI smart reply / summary | v0.5 — "draft a campaign for me" |

## Repository expectations

The backend repo (`getkempt-backend`) consumes this spec via:

```bash
# In the backend repo
cd backend
git submodule add ../api-spec ./api-spec
# OR vendor a snapshot
cp -r ../frontend/api-spec ./api-spec
```

…then runs codegen against `api-spec/openapi.yaml`. The generator output
lives in `backend/src/generated/` and is committed (for code review).

## Local dev setup

```bash
# Start dependencies
docker compose up postgres redis stripe-mock minio mailcatcher

# Bootstrap the schema
pnpm db:migrate
pnpm db:seed:dev   # 3 shops, 50 clients, services, etc.

# Run the API
pnpm dev
```

## CI / CD prerequisites

- GitHub Actions OIDC into AWS (no long-lived keys).
- Stripe webhook secret per environment, stored in AWS Secrets Manager.
- Production DB has PgBouncer in transaction-pool mode in front.
- Migrations run via `ProductionLock` pattern (advisory lock to prevent
  concurrent runs across replicas).

## Domains

| Env | API | Public widget | Admin app |
|---|---|---|---|
| prod | `api.getkempt.co` | `book.getkempt.co` | `app.getkempt.co` |
| staging | `staging-api.getkempt.co` | `staging-book.getkempt.co` | `staging.getkempt.co` |

## Cross-repo contract

Three repositories share this spec:

```
                    api-spec/openapi.yaml
                  /         |          \
                 /          |           \
        frontend            backend       mobile (future)
   (Next.js client)     (Node API)       (Expo React Native)
```

- Frontend regenerates its API client on every spec change (CI step).
- Backend regenerates handler stubs (CI step) — but business logic is
  hand-written, not generated.
- Breaking changes to the spec require a PR labelled `breaking-api`
  which triggers all three repos' compatibility checks.

## Versioning the spec

- Each release of `api-spec` is git-tagged `vX.Y.Z`.
- Frontend / backend / mobile pin to a specific tag.
- The `openapi.yaml` `info.version` is the spec version, NOT the API URL
  version. Multiple spec versions can target the same `/api/v1` URL.

## When this list changes

This document is updated when a new external service is introduced.
Adding a dependency requires:

1. A justification in the PR description.
2. Update of the production secrets store.
3. A line in the runbook for incident response.
4. Update of `SECURITY.md` if the service touches PII.
