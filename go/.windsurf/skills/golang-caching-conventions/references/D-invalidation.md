# D. Cache Invalidation

Expands rules D1–D4. Phil Karlton's "two hard things" quote exists for a reason.

## D2 + D3 — Delete-on-write, correct order [SHOULD / MUST]

The safest invalidation pattern for cache-aside is: **write to DB first, then delete the cache key**.

```go
func (s *Service) UpdateAccount(ctx context.Context, acc Account) error {
    // 1. Write to DB first
    if err := s.repo.Save(ctx, acc); err != nil {
        return fmt.Errorf("save account: %w", err)
    }
    // 2. Delete cache after successful DB write
    // Non-fatal: if cache delete fails, TTL will expire the entry anyway
    if err := s.cache.Delete(ctx, cacheKey("account", acc.ID)); err != nil {
        slog.WarnContext(ctx, "cache delete failed",
            slog.String("key", cacheKey("account", acc.ID)),
            slog.Any("error", err),
        )
    }
    return nil
}
```

Why delete rather than update? Updating the cache and the DB atomically is not possible without a distributed transaction. A partial failure (DB succeeds, cache update fails) leaves a stale entry. Deletion is safer: the next read does a cache miss and repopulates the fresh value from the DB.

Why DB first? If you delete the cache first and the DB write fails, a concurrent read repopulates the now-stale (pre-update) value before the write has been retried. Then the DB write succeeds, but the cache now holds the old value again — you've made things worse.

## D4 — Grouped invalidation via version tokens [SHOULD]

When you need to invalidate all cached entries for a given account or tenant, maintaining a list of all keys is error-prone. Two better approaches:

**Version token in the key:**
```go
// Stored in DB or Redis:  account_cache_version:{accountID} -> int
func accountCacheKey(accountID string, version int) string {
    return cacheKey("account", accountID, strconv.Itoa(version))
}
// To invalidate: increment the version; old keys become unreachable and expire naturally
```

**Key prefix scan (Redis SCAN):**
```go
// Use SCAN with a prefix to find and delete all keys for an account
// Avoid KEYS in production — it blocks Redis
iter := rdb.Scan(ctx, 0, "payments:prod:account:"+accountID+":*", 0).Iterator()
for iter.Next(ctx) {
    rdb.Del(ctx, iter.Val())
}
```

The version token is preferred for large key spaces — it does not require a scan.

## Common findings

1. Cache deleted before the DB write — concurrent reads repopulate stale data.
2. Cache update and DB write in sequence with no error handling on the cache step — partial failure leaves a stale entry.
3. Manual list of keys to invalidate — always incomplete.
4. `KEYS *pattern*` used in production Redis for grouped invalidation — blocks the Redis event loop.
