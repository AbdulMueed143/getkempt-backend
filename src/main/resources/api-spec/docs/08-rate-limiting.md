# Rate Limiting

Rate limits protect both GetKempt and the shops from accidental or
malicious abuse. They're implemented with **token-bucket** at the edge
(Cloudflare) and **leaky-bucket** at the application (Redis).

---

## Buckets

Limits are applied across multiple dimensions:

- **Per IP** — for unauthenticated endpoints.
- **Per session token (jti)** — for authenticated user actions.
- **Per shop** — for multi-tenant resources (campaigns, exports).
- **Per integration API key** — for service-to-service calls.

The strictest bucket wins. The bucket that triggered is named in the 429
response body for debugging.

## Limits table

### Authentication

| Endpoint | Limit | Bucket | Notes |
|---|---|---|---|
| `POST /auth/signup` | 3 / hour | IP | + global captcha after 1 attempt |
| `POST /auth/login` | 10 / min | IP | + account lock after 50 fails / 15 min |
| `POST /auth/refresh` | 60 / min | session | |
| `POST /auth/forgot-password` | 3 / hour | email | Always 204 — silent |
| `POST /auth/reset-password` | 10 / hour | IP | |
| `POST /auth/verify-email` | 10 / hour | IP | |
| `POST /auth/resend-verification` | 1 / 5min | user | |

### Bookings

| Endpoint | Limit | Bucket |
|---|---|---|
| `GET /bookings/calendar` | 60 / min | session |
| `POST /bookings` | 60 / min | session |
| `POST /bookings/slots/search` | 120 / min | session |
| `POST /bookings/conflicts/check` | 240 / min | session |

### Public widget

| Endpoint | Limit | Bucket | Notes |
|---|---|---|---|
| `GET /public/shops/{slug}/*` | 60 / min | IP | |
| `POST /public/shops/{slug}/bookings` | 3 / min | IP | + captcha |
| `POST /public/bookings/{token}/cancel` | 5 / min | IP | |

### Messaging

| Endpoint | Limit | Bucket | Notes |
|---|---|---|---|
| `POST /campaigns` | 10 / hour | shop | Delivery-rate limited separately |
| `POST /reminders/rules/{id}/test` | 5 / hour | shop | SMS credits charged |

### Heavy reads

| Endpoint | Limit | Bucket |
|---|---|---|
| `POST /clients/export` | 5 / day | shop |
| `POST /bookings/export` | 10 / day | shop |
| `GET /dashboard/chart` | 60 / min | session |

### General default

All other authenticated endpoints: **600 requests / minute / session**.

## 429 response

```http
HTTP/1.1 429 Too Many Requests
Retry-After: 30
Content-Type: application/json

{
  "code":      "rate_limited",
  "message":   "Too many requests — try again in 30 seconds.",
  "requestId": "req_…",
  "bucket":    "session"
}
```

- `Retry-After` is ALWAYS present, in seconds.
- Clients MUST honour `Retry-After`.

## Burst budget

The token-bucket allows a short burst (typically 2× the steady-state) to
absorb legitimate spikes (e.g. the calendar view loads ~5 endpoints in
parallel). The leaky bucket is sized to refill in 60s.

## Exemptions

- Owner-role tokens have higher limits (1200/min default) — they often
  run reports and bulk ops.
- Admin tokens have NO application-layer limit, only the edge limit.

## Per-event quotas (non-RPM)

Some operations are gated by quotas rather than rate-limits:

| Quota | Counter | Reset |
|---|---|---|
| Active staff members | `shop.staffCount` | n/a — billed |
| Campaigns | 50 / month | calendar month |
| Holidays | 365 / year | calendar year |
| Reminder rules | 20 total | none — owner can delete |
| API keys | 5 total | none |

These return **402 Payment Required** (quota exceeded — needs higher plan)
or **422 Unprocessable Entity** (immutable limit), NOT 429.

## Implementation notes for backend authors

- Use Redis `INCR + EXPIRE` per bucket key. Key format:
  `rl:{bucket}:{endpoint_class}:{identifier}:{minute}`.
- Always check FIRST and increment after — to prevent under-counting
  bursts.
- Always set `X-RateLimit-Limit`, `X-RateLimit-Remaining`,
  `X-RateLimit-Reset` headers on every response.
