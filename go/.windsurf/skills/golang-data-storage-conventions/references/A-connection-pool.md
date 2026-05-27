# A. Connection Pool Setup

Expands rules A1–A5.

## A1 + A2 — One pool, configured explicitly [MUST]

`sql.DB` is a connection pool manager, not a single connection. The correct pattern is one pool created at startup, shared everywhere, with explicit limits:

```go
func NewDBPool(ctx context.Context, cfg DBConfig) (*sql.DB, error) {
    db, err := sql.Open(cfg.Driver, cfg.DSN)
    if err != nil {
        return nil, fmt.Errorf("sql.Open: %w", err)
    }

    // Explicit pool configuration — never leave these at zero/default
    db.SetMaxOpenConns(cfg.MaxOpenConns)           // e.g. 25
    db.SetMaxIdleConns(cfg.MaxIdleConns)           // e.g. 10
    db.SetConnMaxLifetime(cfg.ConnMaxLifetime)     // e.g. 10 * time.Minute
    db.SetConnMaxIdleTime(cfg.ConnMaxIdleTime)     // e.g. 5 * time.Minute

    if err := db.PingContext(ctx); err != nil {    // A3: verify connectivity
        db.Close()
        return nil, fmt.Errorf("db ping: %w", err)
    }
    return db, nil
}
```

### Why zero defaults are dangerous

| Setting | Zero-value default | Problem |
|---|---|---|
| `MaxOpenConns` | Unlimited | Under load a single service can open thousands of connections, exhausting the database server's limit |
| `MaxIdleConns` | 2 (prior to Go 1.14, 0 after) | Either wastes connections or causes excessive reconnection overhead |
| `ConnMaxLifetime` | Forever | Connections become stale after a database failover, firewall restart, or server-side idle timeout |
| `ConnMaxIdleTime` | Forever | Same as above for idle connections |

### Recommended starting values (tune per workload)

```go
MaxOpenConns:    25,
MaxIdleConns:    10,
ConnMaxLifetime: 10 * time.Minute,
ConnMaxIdleTime:  5 * time.Minute,
```

## A3 — PingContext at startup [MUST]

```go
if err := db.PingContext(ctx); err != nil {
    return nil, fmt.Errorf("database unreachable at startup: %w", err)
}
```

`sql.Open` does not connect — it only validates the driver name and DSN format. The first real connection is lazy. A startup ping makes the service fail fast and loudly if the database is unreachable, rather than failing silently on the first request.

## A4 — Always use context variants [MUST]

```go
// WRONG — no context; cannot be cancelled, no deadline propagation
rows, err := db.Query("SELECT ...")

// RIGHT — context flows from the request or the calling goroutine
rows, err := db.QueryContext(ctx, "SELECT ...")
```

The context-free variants (`Query`, `Exec`, `Begin`) exist for backwards compatibility. In a long-running service, use only the context-aware variants. A request cancelled by a disconnecting client, or a timeout firing, will cancel the DB call and release the connection back to the pool promptly.

## A5 — ConnMaxLifetime [SHOULD]

Many production environments have firewalls or load balancers that drop TCP connections idle for more than N minutes (commonly 15–30 min). A `sql.DB` connection that survived this silently is not detected until the next query fails with a broken-pipe error. `ConnMaxLifetime` ensures connections are recycled before the external timeout fires, preventing the error entirely.

## pgx note

When using `pgx` directly (rather than through `database/sql`), pool configuration is on `pgxpool.Config`:

```go
config, _ := pgxpool.ParseConfig(dsn)
config.MaxConns = 25
config.MinConns = 5
config.MaxConnLifetime = 10 * time.Minute
config.MaxConnIdleTime =  5 * time.Minute
pool, _ := pgxpool.NewWithConfig(ctx, config)
```

The same principles apply; the field names differ.

## Common findings

1. `sql.Open` called per-request — a new connection (or pool) for every request.
2. `MaxOpenConns` left at 0 — unlimited connections exhaust the database.
3. `db.Ping()` / no startup connectivity check — unreachable database discovered on the first request.
4. Context-free `db.Query`, `db.Exec` used throughout.
5. `ConnMaxLifetime` not set — stale connections after a database failover.
