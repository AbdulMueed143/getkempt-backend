# Booking Lifecycle

The booking is the most state-heavy entity in the system. This document is
the canonical state machine and side-effect catalogue.

---

## State diagram

```
                         ┌─────────┐
              new ──────►│ pending │  (created, deposit not yet captured)
                         └────┬────┘
                              │
                       deposit captured / manual confirm
                              ▼
                         ┌───────────┐
                         │ confirmed │  ─────► reschedule (stays confirmed)
                         └─────┬─────┘
                               │
                  appointment time reached
                               │
                         ┌─────▼───────┐
                         │ in_progress │   (calendar shows current-time bar)
                         └─────┬───────┘
                               │
                  staff marks attended
                               │
                         ┌─────▼─────┐
                         │ completed │
                         └───────────┘

  From pending OR confirmed → cancelled (with optional fee)
  From confirmed (after end_at) → no_show  (with optional fee)
  From completed → CANNOT BE UNDONE — opens dispute UI instead
```

## Status transitions table

| From | To | Endpoint | Notes |
|---|---|---|---|
| (none) | pending | `POST /bookings` | When deposit required, intent created |
| (none) | confirmed | `POST /bookings` | When no payment required OR shop policy = in_store |
| pending | confirmed | `POST /bookings/{id}/confirm` OR auto after deposit success |
| pending | cancelled | `POST /bookings/{id}/cancel` |
| confirmed | in_progress | scheduled job @ start_at | Backend-driven |
| in_progress | completed | `POST /bookings/{id}/complete` |
| confirmed | cancelled | `POST /bookings/{id}/cancel` |
| confirmed | no_show | `POST /bookings/{id}/no-show` (manual) OR auto job 30min past end_at |
| in_progress | no_show | `POST /bookings/{id}/no-show` |
| confirmed / in_progress | confirmed (rescheduled) | `POST /bookings/{id}/reschedule` |

All other transitions return 409.

## Creation flow (detailed)

### Inputs (`POST /bookings`)

```json
{
  "staffId":   "stf_…",
  "serviceId": "svc_…",     // or packageId
  "startAt":   "2026-03-12T04:30:00Z",
  "clientId":  "cli_…",     // OR `client: {…}` inline
  "source":    "manual",
  "notes":     "First-time client"
}
```

### Server steps

1. **Authn / authz**: caller must have `bookings.write` for the target shop;
   `staff` role can only book for themselves.
2. **Resolve service / package**: load Service or Package (must be in the same shop).
3. **Resolve or create client**: if `client` body present, upsert by phone.
4. **Slot validation** — in one transaction:
   - Confirm `staffId` performs the service (`ServiceStaff` row exists).
   - Compute `endAt = startAt + duration + bufferMinutes`.
   - Check the slot lies INSIDE the staff's weekly schedule, less any override.
   - Check no overlap with existing bookings (`status IN [pending, confirmed, in_progress]`).
   - Check no overlap with holidays for which `appliesTo` includes the staff.
   - Apply surcharge rules — for each matching rule, compute the surcharge.
5. **Snapshot fields**: copy `serviceName`, `durationMinutes`, `price`,
   `staffName`, `clientName` onto the booking row.
6. **Compute total price**: `service.price + Σ surcharges`.
7. **Payment policy**:
   - `paymentMode=in_store`: no PaymentIntent.
   - `paymentMode=deposit`: create PaymentIntent for deposit amount, status
     `requires_payment_method`. Booking starts as `pending`.
   - `paymentMode=full_upfront`: create PaymentIntent for full amount.
8. **Persist booking** with `status = pending` (if intent created) or
   `confirmed` (no intent needed).
9. **Schedule reminder jobs**: for each active `ReminderRule` matching the
   event `booking_confirmed`, queue a `ReminderQueueEntry`.
10. **Fire notifications**: bell-notify shop owner + the assigned staff.
11. **Idempotency**: cache the response on `Idempotency-Key`.
12. **Return** the booking + (optionally) the PaymentIntent client_secret.

### Race condition handling

Two clients could attempt the same slot simultaneously. The DB uses a
**unique exclusion constraint** on `(staff_id, time_range)` for active
bookings — the second insert raises a constraint error which the handler
translates to **409 Conflict**.

```sql
ALTER TABLE bookings
  ADD CONSTRAINT no_overlap_per_staff
  EXCLUDE USING gist (
    staff_id WITH =,
    tstzrange(start_at, end_at) WITH &&
  ) WHERE (status IN ('pending', 'confirmed', 'in_progress'));
```

## Cancellation flow

`POST /bookings/{id}/cancel`:

1. State guard: must be `pending` or `confirmed`.
2. Apply policy: lookup `StoreSettings.cancellationFeeRules`. Find the tier
   whose `withinHours` is the tightest that still covers the time-until-appt.
3. If fee applies AND `chargeFee=true` AND a PaymentIntent exists:
   - Capture the portion of the held funds equal to the fee.
   - Refund the remainder.
4. If fee applies AND no held funds: create a new PaymentIntent for the fee
   and return it for the client to settle.
5. Mark `status=cancelled`, `cancellation_reason=…`.
6. Trigger `booking_cancelled` reminder (if rule enabled).
7. Free the calendar slot.
8. Bell-notify owner.

## Reschedule flow

`POST /bookings/{id}/reschedule`:

1. State guard: must be `confirmed`.
2. Re-run the SAME conflict checks as creation, with `ignoreBookingId=self`.
3. Update start/end_at. Recompute surcharges (the new time may attract
   different rules; the price difference is logged on the booking).
4. PaymentIntent retained — no new charge unless surcharge delta > 0.
5. Trigger `booking_rescheduled` reminder.
6. Cancel any already-scheduled queue entries for the old time and reschedule.

## Completion flow

`POST /bookings/{id}/complete`:

1. State guard: `confirmed` or `in_progress`.
2. Capture any uncaptured PaymentIntent (manual capture mode).
3. Update `AppointmentRecord.status=completed`.
4. Increment `Client.total_bookings` + `lifetime_spend_cents`.
5. Run loyalty progression: evaluate the client's current program. If the
   trigger is now met, emit a `LoyaltyReward` row and bell-notify.
6. Trigger `appointment_followup` reminder rule.

## No-show flow

`POST /bookings/{id}/no-show`:

1. State guard: `confirmed` or `in_progress` AND past `end_at`.
2. If `StoreSettings.autoCharge` AND PaymentIntent exists, capture the
   `noShowFeeType/Value` portion.
3. Mark `AppointmentRecord.status=no_show`.
4. Bell-notify owner with the client name + amount charged.

## Background jobs

| Job | Frequency | Purpose |
|---|---|---|
| `transition-in-progress` | every 1 min | bookings where `start_at` ≤ now AND status=confirmed → in_progress |
| `auto-no-show` | every 5 min | bookings 30 min past `end_at` still in_progress with no manual mark → no_show |
| `cancel-pending-payment` | every 1 min | pending bookings older than 10 min with intent in `requires_payment_method` → cancelled |
| `reminder-dispatch` | every 10 sec | poll queue entries with `fire_at <= now` |
| `materialise-appointment-records` | daily 02:00 | for completeness/reporting |
| `loyalty-rebuild` | weekly | reconciles denormalised counters |

## Conflict-check contract

`POST /bookings/conflicts/check` is a **stateless** query. It does NOT
hold any lock — between the check and the actual `POST /bookings`, a
conflict can still arise. The booking POST is the source of truth via
the DB exclusion constraint.
