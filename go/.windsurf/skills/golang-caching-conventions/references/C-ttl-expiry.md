# C. TTL & Expiry

Expands rules C1–C4.

## C1 + C2 — Every entry has an explicit TTL [MUST]

```go
// WRONG — no TTL; entry lives forever
_ = cache.Set(ctx, key, value, 0)

// RIGHT — explicit TTL
_ = cache.Set(ctx, key, value, 30*time.Second)
```

An entry with TTL 0 (or no TTL) in Redis persists until explicitly deleted. In a long-running service this means:
- The cache grows unboundedly.
- Entries become increasingly stale without signalling.
- A schema change cannot be rolled out cleanly — old entries are still served.

Set TTL at write time. A background cleanup job is not a substitute — it is delayed, uncertain, and can race with active reads.

## C3 — Jitter on batch TTLs [SHOULD]

```go
import "math/rand"

func ttlWithJitter(base time.Duration) time.Duration {
    jitter := time.Duration(rand.Int63n(int64(base / 10)))
    return base + jitter
}
```

If 10,000 entries are written in a batch with the same TTL (e.g. during a daily data refresh), they all expire simultaneously. The resulting simultaneous cache misses hammer the database. Adding ±10% jitter spreads expiry over time. Note: this uses `math/rand` for jitter — acceptable here because jitter is not security-sensitive (see `golang-encryption-conventions` rule A1 for the security case).

## C4 — TTL for BFSI financial data [SHOULD]

For account balances, transaction statuses, and limits in a BFSI context:
- **Real-time payment authorisation**: fetch live, never from cache.
- **Account balance display** (UI): TTL ≤ 30 seconds, with a staleness timestamp returned to the caller.
- **Reference data** (IFSC codes, fee schedules): TTL of minutes to hours is acceptable.

Document the business-acceptable staleness for each cached entity and derive the TTL from it. "What's the worst case if a user sees this value X seconds out of date?" is the right question.

## Common findings

1. `cache.Set(ctx, key, value, 0)` — infinite TTL, growing cache.
2. All batch-populated entries with identical TTL, causing a stampede on expiry.
3. Financial data with a 5-minute TTL when the business can only tolerate 30 seconds of staleness.
