# Conventions

This document is the single source of truth for "how the API behaves" across
every endpoint. Every implementer — whether they're hand-writing the backend
or running a codegen — must follow these.

---

## 1. URL shape

- All endpoints live under `/api/v1/`.
- Path segments are **lower-case kebab-case** (`/forgot-password`, never `/forgotPassword`).
- Resources are **plural nouns** (`/bookings`, not `/booking`).
- Sub-resources are nested two levels deep maximum (`/clients/{id}/notes`).
- Actions on a resource are POSTs to a verb path: `/bookings/{id}/cancel`.

## 2. Versioning

- Major versions live in the URL prefix (`/api/v1`, `/api/v2`).
- Breaking changes bump major. Additive changes don't.
- Deprecated endpoints return a `Deprecation: true` and `Sunset: <date>` header
  for at least 90 days before removal.

## 3. JSON

- Field names are **camelCase**.
- Enum values are **snake_case** strings (matches the frontend types).
- No trailing nulls — omit optional fields rather than sending `null` _unless_
  the schema declares the field nullable to distinguish "not set" from "absent".
- No `Date` objects on the wire — always ISO-8601 strings.

## 4. Dates & timezones

- Wire format: **ISO-8601 UTC with `Z` suffix** (`2026-03-12T04:30:00Z`).
- Calendar-only fields (no time component) use `YYYY-MM-DD` in the shop's
  local timezone — used for `Holiday.date`, `AvailabilityOverride.date`, etc.
- Times of day inside a schedule use `HH:MM` 24-hour, also shop-local.
- The shop's `timezone` lives on `StoreProfile.timezone` (IANA — e.g.
  `Australia/Melbourne`). All client-side localisation reads from there.

## 5. Money

- Always integer cents (`Money.amountCents`).
- Currency is always `AUD` in v1 — multi-currency is a v2 feature.
- Never use floats for money. The OpenAPI schemas reject `number` in money
  fields.

## 6. IDs

- All IDs are UUIDv4 (server-assigned, opaque to clients).
- ID prefixes per type (informational — clients should NOT parse):
  | Prefix | Type |
  |---|---|
  | `usr_` | User |
  | `shp_` | Shop |
  | `stf_` | Staff |
  | `cli_` | Client |
  | `bk_`  | Booking |
  | `apt_` | AppointmentRecord |
  | `svc_` | Service |
  | `pkg_` | Package |
  | `lp_`  | LoyaltyProgram |
  | `lr_`  | LoyaltyReward |
  | `cmp_` | Campaign |
  | `rmd_` | ReminderRule |
  | `pi_`  | PaymentIntent (Stripe-issued) |
  | `ch_`  | Charge (Stripe-issued) |
- Stripe IDs (`pi_…`, `ch_…`) are stored as-is, NOT remapped to UUID.

## 7. Pagination

- All list endpoints take `?cursor=…&limit=…`.
- The response envelope is:
  ```json
  {
    "data": [ … ],
    "pageInfo": {
      "nextCursor": "…opaque…",
      "hasNextPage": true,
      "totalCount": 240
    }
  }
  ```
- `totalCount` is **advisory** — only returned when cheap to compute. Treat
  its absence as "unknown".
- `limit` default = 25, max = 100. Requests above 100 return 400.
- Cursors are opaque base64. Servers must reject tampered cursors with 400.

## 8. Filtering, sorting

- Filters are query string params with the field name.
- Multiple values use repeated keys: `?status=confirmed&status=pending`.
- Sorting via `?sort=fieldName` (asc) or `?sort=-fieldName` (desc).
- Only documented sort keys are supported — undocumented keys return 400.

## 9. Errors

Every error response uses the same envelope (`Error` schema):

```json
{
  "code":      "not_found",
  "message":   "Booking with id bk_… not found",
  "requestId": "req_01HRMK4N9X3T7Q",
  "docsUrl":   "https://docs.getkempt.co/errors#not_found"
}
```

Validation errors extend it with `fields[]` (`ValidationError`).

The HTTP status codes used:

| Code | When |
|---|---|
| `400 Bad Request` | Malformed JSON / invalid query param shape |
| `401 Unauthorized` | Missing / expired / revoked token |
| `402 Payment Required` | Stripe Connect missing / insufficient SMS credits / fee owed |
| `403 Forbidden` | Authenticated but role / permission denies the op |
| `404 Not Found` | Resource doesn't exist OR tenant has no access (do not leak existence) |
| `409 Conflict` | Duplicate, slot taken, state machine forbids transition |
| `412 Precondition Failed` | Optimistic-lock `If-Match` mismatch |
| `422 Unprocessable Entity` | Body passed JSON shape but failed business rules |
| `429 Too Many Requests` | Rate limit — see `Retry-After` |
| `5xx` | Server error |

## 10. Idempotency

- Every POST / PUT / PATCH that has side effects accepts an `Idempotency-Key` header.
- The server caches `(idempotencyKey, route, body-hash) → response` for 24 h.
- A repeated request with the same key but **different body** returns 409.
- A repeated request with the same key returns the cached response (status & body).

## 11. Optimistic concurrency

- Resources that can be edited concurrently expose a `version` integer field.
- Clients pass it back as `If-Match: <version>` on PATCH / PUT.
- The server returns `412 Precondition Failed` on stale writes.
- This applies to: `Booking`, `Service`, `Package`, `StaffMember`, `StoreProfile`,
  `StoreSettings`, `LoyaltyProgram`, `WeeklySchedule`.

## 12. Soft-deletion

- DELETE on a resource sets `deletedAt` instead of removing the row.
- Soft-deleted rows are excluded from list endpoints by default.
- Pass `?includeDeleted=true` (admin-only) to include them.
- Historical bookings retain references to deleted staff / services /
  packages — never break joins for audit purposes.

## 13. Multi-tenancy

- The shop id (`shop_id`) is sealed inside every JWT.
- It MUST NEVER appear in URLs.
- Every database query is filtered by `shop_id` at the repository layer.
- Cross-tenant queries are rejected at the application layer with 404, not 403,
  to prevent enumeration.

See [`03-multi-tenancy.md`](./03-multi-tenancy.md) for the row-level security
rules.

## 14. Rate limits

| Endpoint family | Limit | Notes |
|---|---|---|
| `POST /auth/login` | 10 / min / IP | Bans IP for 15 min after 50 failures |
| `POST /auth/signup` | 3 / hour / IP |  |
| `POST /auth/forgot-password` | 3 / hour / email |  |
| `GET /bookings/calendar` | 60 / min / token | Heavy; cache the response |
| `POST /campaigns` | 10 / hour / shop | Send-rate is metered separately |
| `POST /public/shops/.../bookings` | 3 / min / IP | Captcha required |
| All others (authenticated) | 600 / min / token |  |

See `08-rate-limiting.md` for the full table and 429 contract.

## 15. Tracing

- Every request gets a server-assigned `X-Request-Id` (echoed in headers + errors).
- Clients may supply their own `X-Request-Id` for end-to-end tracing.

## 16. CORS

- Allowed origins are configured per environment.
- Production: `https://app.getkempt.co`, `https://book.getkempt.co`,
  any tenant's custom domain (looked up from `StoreProfile`).
- The `/public/*` endpoints permit any origin (`*`).
