# Domain Model

This is the canonical entity-relationship map. It governs the database
schema, the OpenAPI schemas, and the codegen output.

---

## ERD (textual)

```
                ┌────────────┐
                │   Shop     │  (StoreProfile)
                └─────┬──────┘
                      │ 1
        ┌─────────────┼──────────────┬─────────────┬────────────┐
        │             │              │             │            │
        │ N           │ N            │ N           │ N          │ 1
   ┌────▼────┐   ┌────▼────┐    ┌────▼────┐  ┌─────▼─────┐ ┌────▼─────┐
   │  User   │   │  Staff  │    │ Service │  │  Client   │ │ Settings │
   └─────────┘   │ Member  │    └────┬────┘  └─────┬─────┘ └──────────┘
                 └────┬────┘         │             │
                      │              │ M:N         │
                      │              │             │
                      │            ┌─▼─────────┐   │
                      │            │  Package  │   │
                      │            └─────┬─────┘   │
                      │                  │ M:N     │
                      │                  │         │
                      │ 1            ┌───┴────┐    │ 1
                      └──────────────►Booking ◄────┘
                                     └───┬────┘
                                         │ 1
                                ┌────────┴────────┐
                                │                 │
                          ┌─────▼──────┐   ┌──────▼──────┐
                          │Appointment │   │PaymentIntent│
                          │ Record     │   │  (Stripe)   │
                          └────────────┘   └─────────────┘
```

---

## Tables (with key columns)

### Shop
| Column | Notes |
|---|---|
| id | uuid pk |
| slug | unique, lowercase, used for public widget |
| timezone | IANA |
| business_type | enum |
| stripe_connect_account_id | nullable |
| created_at, deleted_at | |

### User
| id | uuid pk |
| email | unique |
| role | barber / salon_owner / admin |
| password_hash | argon2id |
| email_verified_at | nullable |

### UserShopMembership
| user_id | fk |
| shop_id | fk |
| role | owner / manager / staff |
| permissions_override | jsonb, nullable |

### StaffMember
| id | uuid pk |
| shop_id | fk |
| user_id | nullable fk (set when staff can log in) |
| email, phone, first_name, last_name | |
| status | active / on_leave / inactive |
| commission_type, commission_rate | |
| services | array of service_id (denormalised for fast read) |
| start_date | |

### Service
| id, shop_id | |
| name, category, description | |
| duration_minutes, buffer_minutes | |
| price_cents, currency | |
| deposit_required, deposit_amount_cents | |
| status, online_booking_enabled | |
| display_order | |

### ServiceStaff (M:N)
| service_id, staff_id | composite pk |

### Package
| id, shop_id | |
| name, description | |
| custom_duration_minutes | nullable |
| discount_enabled, discount_type, discount_value | |
| status, online_booking_enabled | |

### PackageService (M:N) — ordered
| package_id, service_id, position | composite pk |

### PackageStaff (M:N)
| package_id, staff_id | composite pk |

### Client
| id, shop_id | |
| first_name, last_name, email, phone | |
| total_bookings, lifetime_spend_cents | denormalised counters |
| last_booking_at, next_booking_at | denormalised |
| preferred_staff_id | nullable fk |
| marketing_opt_in, sms_opt_in, email_opt_in | |
| stripe_customer_id | nullable |

### ClientNote
| id, shop_id, client_id, body, pinned, created_by_staff_id, created_at |

### Booking
| id, shop_id | |
| client_id | nullable (walk-ins) |
| staff_id | fk |
| service_id, package_id | nullable, but exactly one set |
| service_name_snapshot | string — frozen at create time |
| duration_minutes, price_cents | snapshotted |
| start_at, end_at | utc |
| status | enum |
| source | enum |
| cancellation_reason | nullable |
| payment_intent_id | nullable fk |
| deposit_paid_cents | nullable |
| version | integer for optimistic locking |
| created_at, updated_at, deleted_at | |

### AppointmentRecord (1:1 with Booking, materialised post-start)
| id, shop_id, booking_id | |
| status | pending / completed / no_show / cancelled |
| note | nullable |
| status_updated_at, status_updated_by_staff_id | |

### WeeklySchedule
| id, shop_id, staff_id (unique) | |
| timezone | IANA |
| days | jsonb — array of 7 `DaySchedule` |

### AvailabilityOverride
| id, shop_id, staff_id | |
| date | local date |
| is_working | bool |
| slots | jsonb |
| note | nullable |

### LoyaltyProgram
| id, shop_id | |
| sequence_order | int, unique-per-shop |
| name, description | |
| reward_type, reward_value, reward_label | |
| redeem_location, partner_name, partner_address | |
| trigger_type, trigger_value | |
| program_expiry_date, reward_expiry_days | |
| status | |
| total_earned, total_redeemed | denormalised counters |

### LoyaltyReward
| id, shop_id, client_id, program_id | |
| status | pending / redeemed / expired / revoked |
| earned_at, redeemed_at, expires_at | |
| redeemed_at_booking_id | nullable |

### ReminderRule
| id, shop_id | |
| event | enum |
| timing | jsonb |
| channels | text[] |
| enabled | |
| template | jsonb |

### ReminderQueueEntry (transient — scheduled jobs)
| id, shop_id, booking_id, rule_id | |
| channel, recipient, fire_at, status, attempt_count | |
| sent_at, error_message | |

### Campaign
| id, shop_id, payload (jsonb) | |
| status, estimated_count, sent_count, failed_count | |
| scheduled_at, sent_at | |
| created_by_staff_id | |

### CampaignDelivery (one per recipient — only persisted on send)
| id, campaign_id, client_id, channel, recipient | |
| status | queued / sent / delivered / failed / bounced |
| provider_message_id | |

### Holiday, SurchargeRule, CancellationFeeRule — all shop-scoped, see schemas.

### PaymentIntent
| id (Stripe pi_…) | text pk |
| shop_id, booking_id, client_id | |
| purpose | enum |
| amount_cents | |
| status | |

### Charge
| id (Stripe ch_…) | text pk |
| intent_id, shop_id, booking_id, client_id | |
| amount_cents, refunded_cents | |
| status | |
| receipt_url, last4, brand | |

### Refund — child of Charge.

### Payout — Stripe-managed, mirrored locally for reporting.

### GiftCard
| id, shop_id, code (unique) | |
| value_cents, balance_cents | |
| status, expires_at | |
| purchased_by_client_id, recipient_name, recipient_email | |

### Subscription (the SHOP's subscription to GetKempt)
| id, shop_id | |
| plan, status, staff_count, rate_per_staff_cents | |
| current_period_start, current_period_end | |
| trial_ends_at | |
| stripe_subscription_id | |

### SmsCreditsLedger
| id, shop_id, delta, reason, balance_after, created_at |

### Invoice — for the shop's GetKempt invoices, not customer-facing.

### Notification
| id, shop_id, user_id (nullable for shop-wide), type, title, body, href | |
| read | |
| created_at | |

### PushDevice
| id, user_id (NOT shop_id — devices belong to users), token, platform | |
| last_seen_at, app_version | |

### Upload
| id, shop_id, purpose, key, content_type, size_bytes | |
| confirmed_at | nullable |

### ApiKey
| id, shop_id, hashed_key, name, role | |
| last_used_at, created_by_user_id | |

### WebhookEvent (incoming, Stripe / Twilio / SendGrid raw payloads)
| id (provider id), provider, event_type, payload (jsonb) | |
| processed_at, error | |

### AuditLog (every state change on critical entities)
| id, shop_id, actor_user_id, entity_type, entity_id, action, before, after, request_id, created_at |

---

## Snapshotting policy

Some fields are deliberately **snapshotted onto child rows** so that
editing the parent does not retroactively rewrite history:

| Snapshot field | Source | Lives on |
|---|---|---|
| `serviceName`, `durationMinutes`, `price` | Service | Booking |
| `staffName` | StaffMember | Booking |
| `clientName` | Client | Booking |
| `rewardLabel`, `rewardValue` | LoyaltyProgram | LoyaltyReward |

This is a hard contract — never replace a snapshot value with the live one
during an aggregate read.

---

## Computed / derived fields

A handful of fields appear in the API responses but are **never stored**:

| Field | Computed how |
|---|---|
| `Package.basePrice` | Σ `services[].price` for current member services |
| `Package.discountedPrice` | basePrice × (1 − discountValue/100) OR basePrice − discountValue |
| `Package.effectiveDurationMinutes` | customDurationMinutes ?? Σ durations |
| `Client.totalBookings` | maintained by trigger but rebuildable from bookings |
| `Booking.surchargesApplied` | computed against current surcharge rules + holidays |
| `Service.depositAmount` (when percentage) | NOT stored as cents — percent recorded; cents computed at booking time |
| `DashboardStats.*` | rolled up from materialised views refreshed every 5 min |

---

## Deletion / archival

| Entity | DELETE semantics |
|---|---|
| Booking | Soft-delete only after final state. Pending → hard-delete allowed for 5 min. |
| Service / Package | Always soft-delete. Future bookings using it remain valid (snapshotted). |
| Staff | Soft-delete. Future bookings auto-cancelled. |
| Client | Soft-delete. GDPR/Privacy Act: hard-delete on request — name redacted to "Anonymous", PII fields nulled, but historical booking COUNT preserved. |
| LoyaltyProgram | Soft-delete only when no pending rewards. |
| Campaign | Never hard-deleted (audit). |
| Holiday / SurchargeRule | Hard-delete OK. |
| Notification | Hard-delete after 90 days. |
| Upload | Hard-delete when unreferenced for 24 h. |
