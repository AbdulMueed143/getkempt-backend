# Authentication

## Token model

GetKempt uses **rotating JWT bearer tokens** with a short-lived access token
and a long-lived refresh token. Refresh tokens rotate on every use to limit
the blast radius of a leak.

| Token | TTL | Format | Stored where |
|---|---|---|---|
| Access | 15 min | JWT (RS256) | Memory only (Zustand) |
| Refresh | 30 days | Opaque (64 random bytes, base64url) | httpOnly secure cookie + body |

## Access-token claims

```json
{
  "sub":  "usr_…",
  "shop_id": "shp_…",
  "role": "owner",
  "staff_id": "stf_…",
  "permissions": ["bookings.write", "reports.read", …],
  "iat":  1748000000,
  "exp":  1748000900,
  "jti":  "ses_…"
}
```

- `sub` — the `User.id`.
- `shop_id` — the tenant. EVERY query filters on this server-side.
- `role` — coarse role (`owner` / `manager` / `staff` / `admin`).
- `staff_id` — present iff the user is linked to a staff row.
- `permissions` — fine-grained string array. Roles map to permission bundles.
- `jti` — session id used for revocation.

## RBAC matrix

| Capability | Owner | Manager | Staff |
|---|:--:|:--:|:--:|
| View own bookings | ✓ | ✓ | ✓ |
| View all bookings | ✓ | ✓ |   |
| Create / edit bookings | ✓ | ✓ | own |
| Cancel any booking | ✓ | ✓ |   |
| Manage services | ✓ | ✓ |   |
| Manage packages | ✓ | ✓ |   |
| Manage staff (CRUD) | ✓ | ✓ |   |
| Change staff pay rate | ✓ |   |   |
| Manage store profile / settings | ✓ |   |   |
| Manage holidays / surcharges | ✓ | ✓ |   |
| Manage loyalty programs | ✓ | ✓ |   |
| Dispatch campaigns | ✓ | ✓ |   |
| Configure reminders | ✓ | ✓ |   |
| Manage payments (Stripe Connect) | ✓ |   |   |
| Manage subscription / billing | ✓ |   |   |
| View clients | ✓ | ✓ | own |
| Edit clients | ✓ | ✓ |   |

## Flows

### Signup

```
client → POST /auth/signup
              ┌── User row
              ├── StoreProfile row (slug auto-generated)
              ├── default Service ("Haircut") and StaffMember (the owner)
              ├── default ReminderRules (booking_confirmed, 24h reminder)
              └── default WeeklySchedule (Mon-Fri 9-17)
client ← 201 { user, tokens }
        (verification email sent in background)
```

### Login

```
client → POST /auth/login
client ← 200 { user, tokens }
client → GET  /users/me         (sanity check + hydrate)
```

### Refresh

```
client → access call → 401 (token expired)
client → POST /auth/refresh { refreshToken }
client ← 200 { newAccessToken, NEW refreshToken }   ← old refresh now invalid
```

If a refresh token is presented after it's been rotated, the server treats
this as a compromised session and revokes the entire chain — the user is
logged out everywhere.

### Password reset

```
client → POST /auth/forgot-password { email }       → 204 (always)
                ↓
            email sent with reset link
                ↓
client → POST /auth/reset-password { token, password }
client ← 200 { user, tokens }                       ← logged in immediately
```

### Email verification

```
client → signup → unverified account
              ↓
        verification email sent
              ↓
client → POST /auth/verify-email { token }
```

Unverified accounts CAN log in, but bookings created from a public widget
require a verified shop owner. The dashboard shows a banner until verified.

## Sign-out

`POST /auth/logout` adds the access token's `jti` to a Redis revocation set
(TTL = remaining lifetime of the token). The refresh token is hard-deleted.

`GET /users/me/sessions` + `DELETE /users/me/sessions` lets the user manage
all active sessions (useful for "log out everywhere").

## Public widget tokens

The public booking endpoints (`/public/*`) are **unauthenticated**, but
each created booking is associated with an opaque `token` that allows
the client to manage that booking (view, cancel). The token:

- Is single-shop scoped.
- Is invalidated after the appointment date + 30 days.
- Carries no PII — it's a server-issued opaque blob.
- Rate-limited per IP for cancellations / reschedules.

## Service-to-service auth

Trusted integrations (Zapier, Make.com, custom client portals) use an
`X-Api-Key` header instead of a bearer token. API keys:

- Are issued from `Settings → Integrations → API Keys` (UI not yet built).
- Inherit a fixed role (defaults to `manager`).
- Never expire — must be manually rotated.
- Are scoped to a single shop.
