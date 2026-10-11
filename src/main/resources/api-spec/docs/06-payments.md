# Payments

GetKempt is a Stripe Connect platform. Funds flow **directly to the
shop's connected Stripe account**; GetKempt takes an application fee
on each charge (currently 0% — fees come from the SaaS subscription).

---

## High-level architecture

```
   Client browser ──Stripe.js──► Stripe (PaymentIntent confirmation)
        │                              │
        ▼                              ▼
   GetKempt API ──API──► Stripe ──webhook──► GetKempt API
        ▲                                       │
        │                                       ▼
        └────────────── persist Charge / update Booking
```

- The API NEVER touches raw card numbers — PCI scope is minimal.
- All PaymentIntents are created with `application_fee_amount` baked in.
- All requests against Stripe carry the connected account header
  `Stripe-Account: acct_…`.

## Onboarding

1. Owner clicks **Settings → Payments → Connect Stripe**.
2. Frontend calls `POST /payments/connect/onboard` with redirect URLs.
3. Backend creates (or refreshes) a Stripe Express account + Account Link.
4. User is sent to Stripe's hosted onboarding (KYC, bank details).
5. On return, Stripe sends `account.updated` to `/webhooks/stripe`.
6. Backend updates `Shop.stripe_connect_account_id` and
   `StoreSettings.stripeConnected = true` once `charges_enabled = true`.

## Charging at booking

The shop owner picks a `paymentMode` in settings:

| Mode | What happens at `POST /bookings` |
|---|---|
| `in_store` | No PaymentIntent. Booking is `confirmed`. Owner takes payment at the appointment via card terminal / cash / EFT. |
| `deposit` | PaymentIntent created for `depositValue` of the price. Booking is `pending` until intent succeeds — then auto-promoted to `confirmed`. |
| `full_upfront` | PaymentIntent created for the full amount. Same auto-promotion. |

The client confirms with Stripe.js using the returned `client_secret`.
The Stripe `payment_intent.succeeded` webhook is the source of truth for
status changes; the booking is NOT confirmed until the webhook arrives.

## Capture timing

- Deposits → automatic capture (the deposit is non-refundable per policy).
- Full upfront → automatic capture.
- No-show / cancellation fees → `capture_method=manual`. Funds are
  authorised when the booking is created and only captured if the no-show
  / cancellation actually happens. This avoids holding too long — Stripe's
  auth window is 7 days for cards.

## Refunds

`POST /payments/charges/{id}/refund` accepts:

```json
{
  "amount": { "amountCents": 5000, "currency": "AUD" },
  "reason": "requested_by_customer",
  "note":   "Client was unhappy with cut"
}
```

- Omit `amount` for a full refund.
- Reasons map to Stripe's allowed reasons.
- Audit log records the refund + who initiated it.

## Tipping

Tips are added during `/bookings/{id}/complete`. They appear as a separate
line item on the same PaymentIntent (or a follow-up PaymentIntent if the
original was already captured).

## Cancellation / no-show fees

Configured in `StoreSettings.cancellationFeeRules` (tiered) and
`StoreSettings.noShowFeeType/Value` (single).

- If `autoCharge=true`, the backend tries to capture without further
  client interaction (using the held authorisation OR the saved card via
  off-session PaymentIntent).
- If `autoCharge=false`, the backend emits a "Fee owed" alert in the
  Dashboard and a notification to the client with a payment link.

## Gift cards

- Each gift card has a unique 12-char alphanumeric code.
- Selling: `POST /payments/gift-cards` → creates the row + a PaymentIntent
  for the buyer + emails the recipient.
- Redeeming: `POST /payments/gift-cards/{code}/redeem` reduces the balance.
  Redemption emits a `Charge` row with `purpose=gift_card_redemption` so
  it shows up in reporting.
- Expiry follows AU consumer-law minimums (3 years) — enforced at create
  time (`expiresAt` cannot be ≤ 3 years from now).

## Payouts

Stripe owns the payout schedule (default: daily, T+2). The API surfaces
recent payouts read-only via `GET /payments/payouts`.

## Application fees (platform commission)

Per environment-config:

| Env | Fee |
|---|---|
| local | 0 |
| staging | 0 |
| production | 0 (v1) — model is SaaS subscription, no per-transaction fee |

The schema allows future per-shop / per-plan fee overrides.

## Webhook handling

`/webhooks/stripe` handles these events; everything else is ignored
(returned 200 to keep Stripe's delivery healthy):

| Event | Action |
|---|---|
| `payment_intent.succeeded` | Mark booking `confirmed` if pending; persist `Charge` row |
| `payment_intent.payment_failed` | Bell-notify owner; booking stays pending |
| `payment_intent.canceled` | Mark intent canceled; if linked booking is pending, cancel it |
| `charge.refunded` | Update `Charge.refunded_cents`, set status |
| `charge.dispute.created` | Bell-notify owner; status=disputed |
| `payout.paid` | Persist `Payout` row; bell-notify |
| `payout.failed` | Persist; bell-notify owner with red severity |
| `account.updated` | Refresh `Shop.stripe_connect_status` |
| `invoice.paid` (platform) | Mark subscription invoice paid |
| `customer.subscription.updated` (platform) | Sync subscription state |

All events stored raw in `WebhookEvent` table for replay/audit.

## SMS billing (different system)

SMS credits use **GetKempt's own Stripe customer (the platform)** —
NOT the shop's Connect account. The shop pays GetKempt for credits, and
GetKempt absorbs the Twilio cost.

`POST /billing/sms-credits/purchase`:

1. Validate bundle id.
2. Create a one-off PaymentIntent on the platform account, default card.
3. On succeeded webhook, increment `SmsCreditsLedger` with positive delta.

Every outbound SMS decrements the ledger. Reaching 0 disables SMS reminders
(an alert is raised when balance < 100).

## Edge cases & invariants

- **Currency**: AUD only in v1. The schema is structured to accept others
  but the validators reject non-AUD.
- **Negative amounts**: never accepted. Refunds use a positive amount; the
  direction is implied by the endpoint.
- **Double-capture**: prevented by Stripe's idempotency + the Idempotency-Key
  forwarded on the API.
- **Webhook replay**: idempotent on `event.id`. Re-deliveries are no-ops.
- **Out-of-order webhooks**: status transitions are guarded — `succeeded`
  cannot regress to `processing`. Older event timestamps are ignored.
