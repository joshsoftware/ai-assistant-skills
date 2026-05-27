# F. Cache Abstraction & Interface

Expands rules F1–F4.

## F1 + F2 — Cache interface with context [SHOULD]

```go
type Cache interface {
    Get(ctx context.Context, key string) ([]byte, error)
    Set(ctx context.Context, key string, value []byte, ttl time.Duration) error
    Delete(ctx context.Context, key string) error
    DeletePrefix(ctx context.Context, prefix string) error
}
```

Benefits:
- Swap Redis for an in-process fake in tests — no Redis instance needed in CI.
- Swap Redis for Memcached or another store without changing calling code.
- Add instrumentation (metrics, tracing) in a single wrapper.

The interface operates on `[]byte`, not on `interface{}` or generics, to keep it simple and explicit about serialisation. The caller is responsible for marshalling/unmarshalling.

## F3 — Cache failures are non-fatal on reads [MUST]

```go
acc, err := s.getFromCache(ctx, id)
if err != nil {
    // Cache miss or cache down — fall through to DB
    // Log at debug/warn, not error — this is expected under load or cache restarts
    slog.DebugContext(ctx, "cache miss", slog.String("key", key), slog.Any("err", err))
    acc, err = s.repo.FindByID(ctx, id)
    if err != nil {
        return Account{}, err
    }
}
```

If the cache is unavailable, the request must still succeed (slower, from the DB). A cache outage that propagates to service outage is a design defect. The cache is an optimisation, not a primary source of truth for reads.

## F4 — Context timeout on every cache call [MUST]

```go
cacheCtx, cancel := context.WithTimeout(ctx, 50*time.Millisecond)
defer cancel()
cached, err := s.cache.Get(cacheCtx, key)
```

A hung Redis call (network partition, Redis blocked on a heavy operation) must not block a request indefinitely. A 50ms timeout is a starting point; tune per service's latency budget.

## Common findings

1. Direct `go-redis` client calls scattered throughout service and handler code — no abstraction layer.
2. A cache error on a read returns an error to the caller instead of falling through to the DB.
3. No timeout on Redis calls — a network partition hangs worker goroutines.
4. In tests, a real Redis is required — tests are slow and brittle in CI.

---

# G. BFSI-Specific Caching Rules

Expands rules G1–G4.

## G1 — PII masking in cache [MUST]

A cache is not exempt from the PII-handling rules that apply to the database. Any value containing account numbers, Aadhaar, PAN, mobile, email, or other PII must be stored in the same masked/tokenised form as in the database.

```go
// WRONG — caches the full account number
cacheValue := AccountView{Number: acc.Number.Reveal(), ...}

// RIGHT — caches the masked form (the same type whose String() is masked)
cacheValue := AccountView{Number: acc.Number, ...} // Number.MarshalJSON() emits masked form
```

If a PII field is unavoidable in a cache value (e.g. it is needed for display on a high-traffic endpoint), the Redis connection must be TLS-secured, authenticated, and the key must be namespaced to the tenant. See `golang-bfsi-bindings` go-E-data-pii.md for the typed masking pattern.

## G2 — Cached balance carries a timestamp [MUST]

```go
type CachedBalance struct {
    AmountMinor int64     `json:"amount_minor"`
    Currency    string    `json:"currency"`
    AsOf        time.Time `json:"as_of"` // UTC timestamp of when this was fetched
}
```

A consumer must check `AsOf` before acting. For payment authorisation, the live balance is always fetched from the ledger — the cached value is for display only.

## G3 — Session tokens in Redis require TLS + auth [MUST]

If Redis is used as a session store:
- The Redis connection uses TLS (`RedisOptions.TLSConfig` with `MinVersion: tls.VersionTLS12`).
- Redis requires a password or ACL user (no unauthenticated Redis in production).
- Session keys are namespaced and have a TTL matching the session timeout (see `golang-bfsi-bindings` rule A4).

## G4 — Audit records bypass the cache [MUST]

Audit-trail records (financial transactions, auth events, privileged actions) are written directly to the append-only audit store. They are never cached — caching an audit record could cause it to be written once to the cache and then lost if the async write to the audit store fails.

## Common findings

1. Full account number stored in a Redis cache value that Redis returns as plaintext.
2. Cached balance used for payment authorisation without checking `AsOf`.
3. Redis used as session store with no TLS and no authentication.
4. Audit event "optimised" through a write-behind cache — event lost on cache failure.
