# Notifications, Reminders & Campaigns

There are THREE distinct messaging systems. Don't mix them up.

| System | Trigger | Recipients | Endpoints |
|---|---|---|---|
| **Reminders** | Booking lifecycle events | One client per booking | `/reminders/*` |
| **Campaigns** | Owner-initiated broadcast | Many clients (segment) | `/campaigns/*` |
| **Notifications** | System events | Shop staff / owner (in-app + push) | `/notifications/*` |

---

## 1. Reminders

### Rule engine

Each `ReminderRule` is `(event, timing, channels[], template)`. The events
are:

| Event | When fires | Multiple allowed? |
|---|---|---|
| `booking_confirmed` | Booking → confirmed | No |
| `appointment_reminder` | N hours before start_at | Yes (e.g. 24h + 2h) |
| `appointment_followup` | N hours after end_at | No |
| `booking_cancelled` | Booking → cancelled | No |
| `booking_rescheduled` | Booking time changed | No |

### Dispatch flow

```
   Booking event ─► ReminderRule matcher ─► ReminderQueueEntry rows
                                                  │
                              every 10 sec        ▼
                           ┌────────────────────────────┐
                           │ reminder-dispatch worker   │
                           │ ─ load entries fire_at ≤ now│
                           │ ─ render template          │
                           │ ─ route to provider        │
                           │   ─ email → SendGrid       │
                           │   ─ sms   → Twilio (charges SMS credits) │
                           │   ─ push  → Expo / APNs / FCM │
                           │ ─ update status            │
                           └────────────────────────────┘
                                            │
                            webhook callback (delivery status)
                                            │
                                            ▼
                            update Notification + bell user
```

### Template variables

The renderer resolves `{{tokens}}` server-side. Variables:

```
{{client_name}}        {{service_name}}     {{staff_name}}
{{appointment_date}}   {{appointment_time}}
{{shop_name}}          {{shop_address}}
{{manage_link}}        {{cancel_link}}
```

### SMS credit accounting

- Each SMS sent decrements `SmsCreditsLedger` by 1.
- Multi-segment SMS (>160 chars) decrements by `segmentCount`.
- Reaching 0 disables SMS reminders globally (queue entries with channel=sms
  fail with code `out_of_credits`). Email / push continue.

### Master switch

`GlobalReminderSettings.masterEnabled` short-circuits the worker — useful
for emergency shutoff (Twilio outage, etc.).

---

## 2. Campaigns

### Why async

Audiences can be in the thousands. The frontend NEVER enumerates recipients.
It POSTs a filter descriptor; the backend resolves it server-side and queues.

### Endpoints

- `POST /campaigns/estimate` — preflight count + cost.
- `POST /campaigns/preview` — render template with sample data.
- `POST /campaigns` — actually dispatch (or schedule).

### Filter resolution

```
audience = "all"        → SELECT id FROM clients WHERE shop_id=… AND deleted_at IS NULL
audience = "upcoming"   → … AND next_booking_at IS NOT NULL
audience = "staff"      → joins bookings where staff_id = …
audience = "selected"   → WHERE id IN (…)
```

Each `CampaignDelivery` row is created lazily as the worker dispatches —
the table stays small.

### Stop conditions

- `status=cancelled` is checked at every batch — in-flight messages stop.
- Per-client `marketing_opt_in=false` blocks marketing campaigns.
- Per-channel opt-outs (`sms_opt_in`, `email_opt_in`) block that channel.
- The Australian Spam Act 2003 requires an unsubscribe footer for all
  marketing — automatically appended by the renderer.

---

## 3. In-app notifications

### Sources

Notifications are emitted by the API into the `Notification` table when:

- A booking is created from the public widget → notify owner + staff.
- A booking is cancelled / rescheduled by the client → notify staff.
- A Stripe payment fails / refund processed / dispute opened → notify owner.
- A payout arrives → notify owner.
- SMS credits drop below 100 → notify owner.
- A staff invitation is accepted → notify owner.
- A loyalty reward is earned by a client (high-value milestone) → notify staff.

### Delivery

For each notification:

1. INSERT into `Notification`.
2. If the user has push devices registered AND
   `NotificationPreferences.push.<type>=true`, push to each device.
3. The bell UI polls `/notifications?unreadOnly=true` every 60s (or
   subscribes to a WebSocket channel — v2).

### Preferences

Per-user (NOT per-shop) — stored in `NotificationPreferences`. Three
channels (`inApp`, `email`, `push`) × N types (booking_cancelled, etc.).

Default preferences are populated on user creation from a static config.

---

## Outbound webhooks (future — v0.4)

Shops will be able to register their own webhook URLs. Events emitted:

```
booking.created
booking.cancelled
booking.completed
booking.rescheduled
client.created
loyalty.earned
loyalty.redeemed
payment.succeeded
payment.refunded
review.submitted
```

Each event uses the `WebhookEnvelope` schema:

```json
{
  "id":        "evt_…",
  "event":     "booking.created",
  "createdAt": "2026-03-12T…Z",
  "livemode":  true,
  "data":      { … event payload … }
}
```

Signature: `X-GetKempt-Signature: t=<ts>,v1=<hmac>`. Receivers must verify
within a 5-minute clock skew.

Delivery retries: exponential backoff for 48 hours, then mark dead.
