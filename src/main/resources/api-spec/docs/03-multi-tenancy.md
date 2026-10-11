# Multi-tenancy

GetKempt is multi-tenant from day one. Every shop is a separate logical
tenant. Below is the contract every implementer must honour.

---

## The golden rule

> **Every read or write MUST be filtered by `shop_id` at the lowest level
> of the data layer. The shop id ALWAYS comes from the JWT — NEVER from
> a request body, header, or URL.**

The only exception is the unauthenticated `/public/*` family, which is
addressed by shop **slug** and resolves the shop id server-side before
querying.

## Recommended enforcement layers

A correctly built backend will enforce tenancy at THREE layers, defence-in-depth:

1. **Middleware**: extracts `shop_id` from the validated JWT and stuffs it
   into the request context. The handler is never given a way to alter it.
2. **Repository**: every query is built with `WHERE shop_id = $ctx.shop_id`
   appended by a query builder helper. Hand-written `WHERE` clauses are
   forbidden by lint.
3. **Database**: PostgreSQL Row-Level Security (RLS) on every multi-tenant
   table. Even if the app forgets, the DB will reject the row.

Example RLS policy (PostgreSQL):

```sql
ALTER TABLE bookings ENABLE ROW LEVEL SECURITY;

CREATE POLICY bookings_tenant_isolation ON bookings
  USING (shop_id = current_setting('app.shop_id')::uuid)
  WITH CHECK (shop_id = current_setting('app.shop_id')::uuid);
```

The session-level GUC `app.shop_id` is set at connection check-out from
the JWT.

## Resource ownership

Every primary entity has a `shop_id` column:

```
User, Session
Shop (StoreProfile)
StoreSettings
StaffMember, StaffInvitation
Service, Package
Client, ClientNote
Booking, AppointmentRecord, BookingNote
LoyaltyProgram, LoyaltyReward
ReminderRule, ReminderQueueEntry
Campaign, CampaignDelivery
Holiday, SurchargeRule, CancellationFeeRule
PaymentIntent, Charge, Refund, Payout, GiftCard
Notification, PushDevice, ApiKey
Upload
```

Cross-tenant joins are forbidden at the schema level (`ON delete cascade`
boundaries stop at the shop).

## Cross-tenant primitives

A few primitives ARE shared across tenants:

- `User` — a person can be staff at multiple shops. The link table
  `UserShopMembership(user_id, shop_id, role)` is what's actually scoped.
- `Holiday` templates (public-holiday catalogue per region) — read-only
  reference data.
- Stripe Customer rows — but partitioned by shop via Connect.

## Returning 404, not 403

When a user requests a resource that exists but lives in a different shop,
the API returns **404 Not Found**, not 403. This prevents tenant enumeration.

The only time 403 is used is when the user is in the right shop but their
**role** doesn't allow the operation.

## The "current shop" header

Users with multi-shop access (rare today, planned for franchises) MUST pass
an `X-Shop-Id` header on every request — when omitted, the backend uses the
shop_id baked into the issued token.

To switch shops, the client calls `POST /auth/switch-shop { shopId }` and
gets a freshly minted token pair for that shop.

> Note: this endpoint is reserved for v2. v1 always issues per-shop tokens.

## Tenant-level resource limits

Per shop, enforced at the API layer:

| Resource | Max |
|---|---|
| Staff members | 100 (soft) — beyond requires Enterprise plan |
| Services | 200 |
| Packages | 50 |
| Active loyalty programs | 10 |
| Holidays / year | 365 |
| Surcharge rules | 20 |
| Cancellation fee tiers | 5 |
| Reminder rules | 20 |
| API keys | 5 |
| Campaigns / month | 50 (P1 — open to all) |

429 responses on hitting these — the message includes the limit and the
relevant upgrade path.

## Data export & deletion

On account closure (`/billing/subscription` cancel-immediately), the
following lifecycle applies:

1. **Day 0**: subscription cancelled, shop marked `archived`. Read-only
   access remains via the dashboard for 30 days.
2. **Day 30**: a full data export (JSON + CSV) is queued and the owner is
   emailed a download link valid for 7 days.
3. **Day 60**: all tenant data is hard-deleted from primary DB.
4. **Day 90**: data evicted from warehouse + log archives. Backups
   continue to age out per the 90-day retention policy.

Tenants can self-serve deletion at any point in steps 1-2.
