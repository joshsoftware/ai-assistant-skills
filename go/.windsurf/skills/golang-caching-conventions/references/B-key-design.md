# B. Key Design & Namespacing

Expands rules B1–B5.

## B1 — Namespaced key schema [MUST]

```go
func cacheKey(parts ...string) string {
    return strings.Join(parts, ":")
}

// Usage:
key := cacheKey("payments", "prod", "account", accountID)
// → "payments:prod:account:abc123"
```

A flat key namespace causes collisions as services and features grow. The schema `<service>:<env>:<entity>:<id>` is a widely-used convention. Document the schema for your team and enforce it through a constructor function rather than raw string formatting scattered across the codebase.

## B2 — Never use raw user input as a key [MUST]

```go
// WRONG
key := "user:" + r.FormValue("account_id") // user can inject arbitrary keys

// RIGHT — validate and type the ID first
accID, err := parseAccountID(r.FormValue("account_id"))
if err != nil { return err }
key := cacheKey("payments", "prod", "account", accID.String())
```

## B3 — Tenant isolation in the key [MUST for multi-tenant]

```go
key := cacheKey("payments", tenantID, "account", accountID)
```

Every key in a multi-tenant system includes the tenant ID. Omitting it means tenant A's cache hit serves tenant B's account data.

## B4 — Schema version in key [SHOULD]

```go
const accountCacheVersion = "v2"
key := cacheKey("payments", tenantID, "account", accountCacheVersion, accountID)
```

When the shape of a cached value changes between deployments, a version tag in the key ensures old serialised bytes are not unmarshalled by new code. Without it, a rolling deploy can cause deserialisation errors or silent field-dropping.

## Common findings

1. Keys built with `fmt.Sprintf("account_%s", id)` — no namespace, collides with other features.
2. Multi-tenant system with no tenant ID in keys — data leaks between tenants.
3. Schema change deployed without bumping the key version — old bytes silently decoded by new structs.
