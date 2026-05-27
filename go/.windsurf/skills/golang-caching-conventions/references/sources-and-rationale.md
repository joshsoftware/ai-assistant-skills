# Sources & Rationale

This skill is **engineering conventions, not a standard.**

## What this skill draws on

- **Go standard library** — `sync.Map`, `sync.RWMutex` for in-process concurrency; `golang.org/x/sync/singleflight` for stampede protection.
- **go-redis / redis/go-redis** — the widely-used Go Redis client. Skill is client-agnostic; patterns apply to any implementation of the Cache interface.
- **Cache strategy literature** — cache-aside, write-through, write-behind are widely documented patterns with no single authoritative source; they originate in database and distributed systems literature.
- **BFSI constraints** — PII masking requirements from `bfsi-india-core` categories E and F; session security from category A. These are not caching conventions; they are regulatory requirements that the cache must respect.

## Where the community varies

- **Local vs distributed cache** — the skill recommends distributed (Redis) for per-user data. Some teams use per-instance caches with short TTLs and accept inconsistency. The skill makes the trade-off explicit rather than prescribing one answer.
- **write-through vs cache-aside** — genuinely context-dependent. The skill defaults to cache-aside (simpler, more resilient) and describes write-through for the minority of cases where stale reads are unacceptable.
- **singleflight** — almost universally recommended for stampede protection on popular keys; not every key needs it.

## What is NOT covered here

Caching strategy for HTTP responses (ETag, Cache-Control headers) is a separate concern governed by HTTP standards (RFC 7234). This skill covers only application-level data caching.
