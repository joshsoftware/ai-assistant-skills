---
name: golang-caching-conventions
description: Go caching conventions for in-process and distributed caches — cache strategies (cache-aside, write-through, write-behind), TTL management, invalidation, stampede protection, local vs distributed cache selection, Redis integration, and cache interface abstraction. Use whenever Go code adds, reads, or invalidates cache entries, or when a caching layer is being designed or reviewed. Activate on mentions of cache, Redis, in-memory store, TTL, cache-aside, cache invalidation, singleflight, hot key, stampede, go-redis, ristretto, or "should I cache this". These are engineering conventions — there is no official standard for caching in Go. Pair with golang-bfsi-bindings for BFSI-specific concerns (cached PII must follow the same masking rules as persisted PII; cached session tokens are security-sensitive).
---

# Go Caching Conventions

Conventions for caching in Go backend services. **These are conventions, not a standard.**

Caching is a trade-off between performance and correctness. Every rule here is about managing that trade-off explicitly rather than accidentally. The wrong cache strategy silently serves stale data; the wrong invalidation strategy causes cache stampedes; the wrong key design causes hot-key exhaustion or data leaks between tenants.

## How to use this skill

1. When designing or reviewing a cache layer, walk the rule categories below.
2. Each rule has a severity (RFC 2119) and links to a `references/` file for rationale and patterns.
3. **MUST** violations are blockers. **SHOULD** violations need a documented reason.

## Sources (community practice, not standards)

- Go standard library: `sync.Map`, `sync.RWMutex`, `golang.org/x/sync/singleflight`.
- `go-redis` / `redis/go-redis` — the widely-used Redis client for Go.
- Cache strategy literature: cache-aside, write-through, write-behind patterns.
- BFSI-specific: stale data in a payment or account cache is a compliance risk, not just a UX issue.

Full notes: `references/sources-and-rationale.md`.

## Rule categories

| # | Category | Reference file |
|---|---|---|
| A | Cache strategy selection | `references/A-cache-strategy.md` |
| B | Key design & namespacing | `references/B-key-design.md` |
| C | TTL & expiry | `references/C-ttl-expiry.md` |
| D | Invalidation | `references/D-invalidation.md` |
| E | Stampede & hot-key protection | `references/E-stampede-hotkey.md` |
| F | Cache abstraction & interface | `references/F-cache-interface.md` |
| G | BFSI-specific caching rules | `references/G-bfsi-caching.md` |

---

## Rule index

### A. Cache strategy selection

- **A1 [MUST]** Choose the cache strategy explicitly and document it on the cache site. The three strategies are cache-aside (default), write-through, and write-behind. Mixing them on the same data without intent produces inconsistency.
- **A2 [SHOULD]** Use cache-aside (lazy loading) as the default for read-heavy, infrequently-changing data. Load from the source on miss, populate the cache, return the result.
- **A3 [SHOULD]** Use write-through for data that is written as often as it is read, where stale reads are unacceptable and write latency is tolerable.
- **A4 [MAY]** Use write-behind only for write-intensive, eventually-consistent workloads where write latency is more critical than immediate persistence. Requires a durable queue and a worker to avoid data loss.
- **A5 [MUST]** In-process (local) cache is appropriate only for truly global, rarely-changing data (e.g. reference tables, config). For any per-user or per-account data in a multi-instance deployment, use a distributed cache (Redis) — an in-process cache is invisible to other instances.

### B. Key design & namespacing

- **B1 [MUST]** Cache keys are namespaced to prevent collision across features, environments, and services: `<service>:<env>:<entity>:<id>`. Example: `payments:prod:account:abc123`.
- **B2 [MUST]** Never use user-supplied input as a raw cache key. Sanitise and hash where necessary.
- **B3 [MUST]** In multi-tenant systems, the tenant ID is part of every cache key. Missing it lets tenant A read tenant B's cached data.
- **B4 [SHOULD]** Include a version or schema tag in cache keys for data whose shape changes with deployments. A schema change without a key change causes deserialisaion panics or silent data corruption.
- **B5 [SHOULD]** Keep key strings short and consistent. Document the key schema in `references/B-key-design.md` or a team wiki so all callers follow the same format.

### C. TTL & expiry

- **C1 [MUST]** Every cache entry has an explicit TTL. An entry with no TTL grows the cache unboundedly and serves increasingly stale data.
- **C2 [MUST]** TTL is set at write time, not by a background janitor. Relying on periodic cleanup jobs as the sole expiry mechanism is not a substitute for TTL.
- **C3 [SHOULD]** Add jitter to TTLs for entries that are written in batches, to prevent a thundering-herd on expiry. `TTL = base + rand(0, base*0.1)`.
- **C4 [SHOULD]** For BFSI account/balance data, TTL must reflect the maximum staleness the business can tolerate — typically seconds, not minutes. Overly long TTLs on financial data are a compliance risk.

### D. Invalidation

- **D1 [SHOULD]** Prefer TTL-based expiry over event-driven invalidation for most use cases. Event-driven invalidation is complex and easy to miss.
- **D2 [SHOULD]** When event-driven invalidation is needed, delete the cache entry on write (delete-on-write), do not update it. The next read will repopulate from the source. Updating cache and database in the same operation risks partial failure.
- **D3 [MUST]** Do not delete the cache entry before the database write. The correct order is: write to DB first, then delete cache. Deleting first creates a window where a concurrent read repopulates stale data before the DB write completes.
- **D4 [SHOULD]** For grouped invalidation (e.g. "invalidate all entries for account X"), use a key prefix scan or a version token embedded in all keys for that group — don't maintain a manual list.

### E. Stampede & hot-key protection

- **E1 [MUST]** Protect high-traffic cache entries from cache stampede (thundering herd) using `golang.org/x/sync/singleflight`. On a cache miss, singleflight ensures only one goroutine performs the expensive fetch; all concurrent waiters share its result.
- **E2 [SHOULD]** For read-heavy hot keys in Redis, consider local in-process caching of the value for a very short window (1–5 seconds) so the Redis call is not made per request.
- **E3 [SHOULD]** Monitor cache hit rate and eviction rate. A low hit rate or a high eviction rate signals the cache is not effective or is too small.

### F. Cache abstraction & interface

- **F1 [SHOULD]** Define a cache interface rather than coupling to a specific client (Redis, Memcached, in-process). This allows the implementation to be swapped and simplifies testing.
- **F2 [SHOULD]** The cache interface takes `context.Context` on every method. Cache calls can fail or time out and must be cancellable.
- **F3 [MUST]** Cache failures are non-fatal for read paths. If the cache is unavailable, fall through to the source. Failing a request because the cache is down is worse than a slow request served from the DB.
- **F4 [MUST]** Set an explicit timeout on every Redis call via the context. A hung Redis call must not block a request indefinitely.

### G. BFSI-specific caching rules

- **G1 [MUST]** PII (account numbers, Aadhaar, mobile, email, PAN) in cached values must be stored in masked/tokenised form, identical to how it is handled in persistence. The cache is not exempt from PII masking requirements.
- **G2 [MUST]** Cached account balances and transaction data carry a timestamp. A consumer must check the timestamp before acting on a cached balance. For real-time payment authorisation, the live balance is fetched — not a cached one.
- **G3 [MUST]** Session tokens and authentication material must not be cached in a shared, non-encrypted cache. Redis cache for session tokens requires authentication on the Redis connection and TLS in transit.
- **G4 [SHOULD]** Audit-trail events are never cached and never served from cache. Audit records go directly to the append-only store.

## Out of scope

- Cache-level replication, cluster config, Redis Sentinel / Redis Cluster setup — infrastructure concerns.
- CDN caching / HTTP cache headers — separate from application-level caching.
- Full-page caching / fragment caching for rendered HTML.
