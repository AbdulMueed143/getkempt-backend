# GetKempt vs. Fresha: the case for a different model

*A positioning brief for Australian salons and barbershops, grounded in Fresha's publicly documented pricing/fee structure and in GetKempt's actual system architecture.*

## The short version

Fresha won a lot of the Australian salon/barber market by launching as a "free" booking platform. It isn't free anymore, and the way it makes money creates a structural conflict: **the better a shop does at building its own client base, the more Fresha's fee model tends to cost it.** GetKempt's architecture is built to avoid that conflict — flat, predictable costs; funds that land directly in the shop's own Stripe account; and no platform fee that taxes a shop's own marketing success.

## What Fresha actually charges (as of 2026)

| Cost | Fresha | Source-backed detail |
|---|---|---|
| Subscription | Paid plans since 2025 | ~US$19.95/mo solo, or ~US$14.95/mo per bookable team member on the Team plan |
| New-client marketplace commission | 20% per client, minimum US$6 | Charged on appointments from clients Fresha's marketplace deems "new" — even when a shop disputes the client is actually a repeat customer |
| Card processing | ~2.29–2.79% + $0.20 per transaction | Varies by in-person vs. online vs. manually-entered card, on top of the subscription |
| No-show / cancellation fees, surcharges | Available, but pass-through of card fees to clients isn't legal in every region | Adds setup complexity for the owner to get right |

Independent reviews and cost breakdowns consistently flag the same problem: **the sticker price is not the real price.** A shop doing meaningful online-booking volume ends up stacking a subscription, a per-transaction processing fee, and a 20% new-client tax on top of each other — and that total is hard to predict from the pricing page alone.

## The documented pain points

- **The "new client" fee is inconsistently applied.** Multiple independently reported cases describe shops being charged the 20% marketplace fee for clients who had *already booked before* — including clients the business owner had contacted directly to get them to rebook — with support reportedly declining refunds on technicalities.
- **Fee transparency is a recurring complaint.** Cancellation and processing charges have been reported as unclear at the point of booking, which lands the owner — not Fresha — in the client-service dispute when a customer sees an unexpected charge.
- **The incentive structure works against a shop's own growth.** Once a salon is established and most of its bookings come from its own marketing, referrals, or repeat clients rather than Fresha's marketplace, it is still exposed to marketplace-style fee logic for a discovery benefit it increasingly isn't using.

None of this makes Fresha a bad product — it has real scale, a broad feature set, and a large marketplace audience that's genuinely useful to a brand-new shop with no client base yet. The critique here is specifically about the *economics* as a business matures.

## Where GetKempt's architecture is built to be different

These aren't marketing claims about a live pricing page — they're properties of how the system is actually built:

- **Stripe Connect, not a marketplace escrow.** Client payments settle directly into the shop's own connected Stripe account. GetKempt's application fee is configured at 0% for v1 by design — the business model is a flat SaaS subscription, not a cut of every transaction a shop's own marketing generates.
- **No "new client" tax.** There's no concept of a marketplace-sourced-client fee anywhere in the system. A client who books because of a shop's Instagram, word of mouth, or a walk-in past the window costs the shop exactly the same as one who found it through in-app discovery — because GetKempt doesn't have a discovery marketplace to fund.
- **The shop chooses its own payment methods — including "don't use the payment system at all."** `StoreSettings` lets an owner pick `in_store`, `deposit`, or `full_upfront` as the booking payment mode, and separately define exactly which in-person methods (card, cash, bank transfer) are accepted. A shop that wants to keep taking cash at the chair, with zero processing fee and zero platform involvement, can — and the system now validates that a staff member can only record a payment method the *shop itself* opted into, so "what payment methods are allowed" is the owner's decision, enforced consistently across both booking checkout and retail sale checkout.
- **Retail, not just services.** A basic point-of-sale mechanism (product catalogue with stock tracking, and a sale/checkout flow supporting the same online-vs-in-store payment choice as bookings) means a shop selling shampoo, wax, or gift sets isn't pushed into a separate system or a higher-tier plan to do it.
- **Reliable notifications by construction, not by luck.** Email/SMS/push sends (signup verification, password reset, staff invitations, booking confirmed/cancelled/rescheduled) go through a transactional outbox: the "we're going to notify someone" fact is committed to the database in the same step as the business action, and a background dispatcher — with retry and backoff — is what actually calls the email/SMS/push provider. A slow or down notification provider can never fail or slow down the booking/signup/cancellation request itself, and a failed send is retried automatically rather than silently lost.

## The pitch, in one paragraph

Fresha is a reasonable choice for a shop with zero existing clients that wants marketplace discovery and is willing to pay a percentage of its own growth for it indefinitely. GetKempt is built for the more common Australian reality: an established or growing shop that already has its own clients, already takes cash and EFTPOS at the chair, and wants software that charges what it says it charges — without a "new client" fee that goes up as the business's own marketing works.

## Honesty check

This is a positioning case, not an independent audit — it leans on Fresha's own published pricing pages and third-party reviews/cost breakdowns (linked below) rather than a first-party financial comparison of two live companies, since GetKempt is a system being built rather than a company with its own public pricing yet. Anyone evaluating this for real money should read Fresha's current terms and pricing directly, and should treat "flat SaaS, 0% app fee" as GetKempt's *current configured design decision*, not a permanent promise independent of whatever business model is ultimately chosen for it.

---

### Sources

- [Fresha Pricing](https://www.fresha.com/pricing)
- [Fresha Pricing (2026): Plans, Costs & Fees — SchedulingKit](https://schedulingkit.com/pricing-guides/fresha-pricing)
- [Fresha Pricing vs Pabau: Plans, Fees & Hidden Costs (2026) — Pabau](https://pabau.com/blog/fresha-pricing/)
- [Does Fresha Charge Commission? Yes — Here's How Much (2026) — Valet Vault](https://www.valetvault.com.au/blog/does-fresha-charge-commission-australia/)
- [The True Cost of Fresha — Are Fresha fees worth it? (Medium)](https://medium.com/@asbaines/the-true-cost-of-fresha-276d6856bcc0)
- [Is Fresha Worth It for Salons? Commission Math and When to Switch — DaySpark](https://dayspark.com/blog/is-fresha-worth-it-for-salons)
- [Fresha: 4 Hidden Costs Beyond the Price Tag (2026) — Costbench](https://costbench.com/software/salon-spa/fresha/hidden-costs/)
- [Fresha's Marketplace New Client Fee — Fresha Support](https://support.fresha.com/hc/en-us/articles/4402096033938-Fresha-s-Marketplace-New-Client-Fee)
- [About surcharges — Fresha Help Center](https://www.fresha.com/help-center/knowledge-base/payments/562-about-surcharges)
- [Fresha Reviews — Trustpilot](https://www.trustpilot.com/review/fresha.com)
