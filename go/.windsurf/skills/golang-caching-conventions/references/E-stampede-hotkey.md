# E. Stampede & Hot-Key Protection

Expands rules E1–E3.

## E1 — singleflight for stampede protection [MUST]

A cache stampede (thundering herd) happens when a popular key expires and many goroutines simultaneously detect the miss and all race to repopulate it by hitting the database. Under high traffic, this can bring down the DB.

`golang.org/x/sync/singleflight` ensures only one goroutine performs the expensive operation; all concurrent callers share the result:

```go
import "golang.org/x/sync/singleflight"

type Service struct {
    cache  Cache
    repo   Repository
    group  singleflight.Group
}

func (s *Service) GetAccount(ctx context.Context, id string) (Account, error) {
    key := cacheKey("account", id)

    // Check cache first (fast path, no singleflight needed for hits)
    if cached, err := s.cache.Get(ctx, key); err == nil {
        var acc Account
        if json.Unmarshal(cached, &acc) == nil {
            return acc, nil
        }
    }

    // Cache miss — use singleflight to deduplicate concurrent DB fetches
    type result struct {
        acc Account
        err error
    }
    v, err, _ := s.group.Do(key, func() (any, error) {
        acc, err := s.repo.FindByID(ctx, id)
        if err != nil {
            return nil, err
        }
        if data, merr := json.Marshal(acc); merr == nil {
            _ = s.cache.Set(ctx, key, data, 30*time.Second)
        }
        return acc, nil
    })
    if err != nil {
        return Account{}, err
    }
    return v.(Account), nil
}
```

The third return value from `singleflight.Group.Do` is a boolean indicating whether this caller got a shared (deduplicated) result — useful for metrics.

## E2 — Local L1 cache for hot keys [SHOULD]

For a Redis key queried tens of thousands of times per second, even Redis becomes a bottleneck and a single-point network hop. A very-short-TTL in-process cache in front of Redis eliminates most calls:

```go
type TwoLevelCache struct {
    l1  *ristretto.Cache  // in-process, short TTL (1-5s)
    l2  RedisCache        // distributed, longer TTL
}

func (c *TwoLevelCache) Get(ctx context.Context, key string) ([]byte, error) {
    if v, ok := c.l1.Get(key); ok {
        return v.([]byte), nil
    }
    v, err := c.l2.Get(ctx, key)
    if err == nil {
        c.l1.SetWithTTL(key, v, 1, 2*time.Second)
    }
    return v, err
}
```

Limit the L1 TTL to 1–5 seconds for account/financial data (see rule C4). Longer L1 TTLs on financial data are a BFSI compliance concern.

## E3 — Monitor hit rate and eviction rate [SHOULD]

```go
// Emit as metrics on every cache operation
cacheHits.Inc()   // on a successful Get
cacheMisses.Inc() // on a cache miss
```

A hit rate below ~80% often signals a cache that is too small, has TTLs that are too short, or is being used for data that is never re-read. An eviction rate that grows with traffic signals that the cache is too small for the working set.

## Common findings

1. No stampede protection on a popular key — a TTL expiry under load cascades to DB overload.
2. `singleflight.Group` shared across too many unrelated keys — a slow DB fetch for key A blocks all goroutines waiting on key A; that is correct. But if the group is shared and key A is very slow, it should not delay key B.
3. L1 cache with a long TTL for account balance data — serves stale financial data.
4. No cache metrics — hit/miss rate unknown, cache effectiveness unobservable.
