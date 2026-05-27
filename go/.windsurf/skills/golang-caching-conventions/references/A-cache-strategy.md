# A. Cache Strategy Selection

Expands rules A1–A5.

## The three strategies at a glance

| Strategy | Read path | Write path | Best for |
|---|---|---|---|
| Cache-aside | Check cache → miss → fetch DB → populate cache → return | Write DB directly; delete/expire cache entry | Read-heavy, infrequent writes |
| Write-through | Check cache → hit or miss → return | Write cache AND DB synchronously | Read/write balance, stale reads unacceptable |
| Write-behind | Check cache → return from cache | Write cache; async worker writes DB | Write-intensive, eventual consistency OK |

## A2 — Cache-aside (default) [SHOULD]

```go
func (s *Service) GetAccount(ctx context.Context, id string) (Account, error) {
    // 1. Check cache
    if cached, err := s.cache.Get(ctx, cacheKey("account", id)); err == nil {
        var acc Account
        if err := json.Unmarshal(cached, &acc); err == nil {
            return acc, nil
        }
    }
    // 2. Miss — fetch from DB
    acc, err := s.repo.FindByID(ctx, id)
    if err != nil {
        return Account{}, err
    }
    // 3. Populate cache (non-fatal if it fails)
    if data, err := json.Marshal(acc); err == nil {
        _ = s.cache.Set(ctx, cacheKey("account", id), data, 30*time.Second)
    }
    return acc, nil
}
```

Characteristics:
- Cache is only populated on reads — no wasted writes for data never read.
- Resilient: if the cache is down, reads still work (slower, from DB).
- Tolerates a brief stale window between a DB write and cache expiry.

## A3 — Write-through [SHOULD when needed]

```go
func (s *Service) UpdateAccountName(ctx context.Context, id, name string) error {
    // 1. Write to DB
    if err := s.repo.UpdateName(ctx, id, name); err != nil {
        return err
    }
    // 2. Update cache immediately (write-through)
    acc, _ := s.repo.FindByID(ctx, id)
    if data, err := json.Marshal(acc); err == nil {
        _ = s.cache.Set(ctx, cacheKey("account", id), data, 30*time.Second)
    }
    return nil
}
```

Use when a stale read after a write is unacceptable. Downside: higher write latency (two writes per update). A failure after the DB write but before the cache write leaves them inconsistent — handle by deleting the cache key on error rather than leaving a stale entry.

## A4 — Write-behind [MAY]

Write-behind writes to the cache immediately and to the database asynchronously via a durable queue and a worker. Appropriate for write-intensive, eventually-consistent workloads (e.g. a real-time counter, a user's last-seen timestamp). Do not use for financial postings or any data where eventual consistency is not acceptable — the async write can fail and be lost.

## A5 — In-process vs distributed [MUST choose deliberately]

| | In-process (sync.Map, ristretto) | Distributed (Redis) |
|---|---|---|
| Latency | Nanoseconds | ~1ms over the network |
| Consistency | Per-instance only | Shared across all instances |
| Scale-out | Each new instance has cold cache | All instances share warm cache |
| Use for | Global config, reference tables, truly read-only data | Per-user, per-account, per-session data |

A balance or account state cached in-process is visible only to the instance that populated it. If a customer's request hits a different instance (common with load balancers), that instance has a cache miss and fetches from DB — defeating the cache. Worse: one instance may serve stale data while another has the fresh version.

Multi-level (L1 + L2): For extremely hot reads, combine an in-process short-TTL cache (L1) in front of Redis (L2). The L1 window must be very short (1–5 seconds) for account/financial data.

## Common findings

1. No explicit strategy documented — developers inconsistently mix cache-aside reads with write-through updates on the same data.
2. In-process cache used for per-user data in a multi-instance deployment.
3. Write-behind used for financial data where eventual consistency is not acceptable.
4. Cache-aside with no fallback — a cache failure returns an error instead of going to the DB.
