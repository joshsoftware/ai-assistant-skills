# B. Query Execution & Parameterisation

Expands rules B1–B5.

## B1 — Parameterised queries only [MUST]

```go
// WRONG — SQL injection
query := fmt.Sprintf("SELECT * FROM accounts WHERE id = '%s'", userInput)
rows, err := db.QueryContext(ctx, query)

// RIGHT — parameterised
rows, err := db.QueryContext(ctx,
    "SELECT id, balance, owner_id FROM accounts WHERE id = $1",
    accountID,
)
```

The parameter (`$1` for PostgreSQL, `?` for MySQL/SQLite) is sent separately from the query text. The database driver handles quoting and escaping; the query structure is fixed regardless of the parameter value.

Dynamic structure (column to ORDER BY, table name) never comes from raw input. Use a server-side allow-list:

```go
var sortable = map[string]string{
    "created_at": "txn.created_at",
    "amount":     "txn.amount_minor",
}
col, ok := sortable[req.SortBy]
if !ok {
    return nil, ErrBadRequest("invalid sort field")
}
// col is a known, safe string — safe to interpolate into ORDER BY
query := "SELECT ... ORDER BY " + col + " DESC LIMIT $1"
rows, err := db.QueryContext(ctx, query, pageSize)
```

## B2 — Always close rows [MUST]

```go
rows, err := db.QueryContext(ctx, query, args...)
if err != nil {
    return nil, fmt.Errorf("query: %w", err)
}
defer rows.Close() // immediately after a successful QueryContext

var results []Payment
for rows.Next() {
    var p Payment
    if err := rows.Scan(&p.ID, &p.AmountMinor, &p.Status); err != nil {
        return nil, fmt.Errorf("scan: %w", err)
    }
    results = append(results, p)
}
// Always check rows.Err() after the loop
if err := rows.Err(); err != nil {
    return nil, fmt.Errorf("rows: %w", err)
}
return results, nil
```

`rows.Close()` returns the underlying connection to the pool. A forgotten `Close()` holds a connection open until GC finalises the `*sql.Rows`, which may never happen promptly under load — starving the pool.

`rows.Err()` catches errors that occurred mid-iteration (e.g. a network failure partway through a result set). Ignoring it means silently returning a partial, corrupt result set.

## B3 — Typed scans [MUST]

```go
// WRONG — scans into interface{}; type asserted later; schema drift invisible
var id interface{}
rows.Scan(&id)

// RIGHT — scans into a typed variable
var id string
rows.Scan(&id)
```

Typed scans catch mismatches between the query result and the Go type at scan time, not at assertion time or not at all. For more complex structs, `sqlx` provides `StructScan` / `Select` that scan directly into a tagged struct, reducing boilerplate.

## B4 — Named parameters / query builders for complex queries [SHOULD]

For a query with many parameters, positional markers become hard to count and easy to transpose. Named parameters via `sqlx`:

```go
_, err := sqlx.NamedExecContext(ctx, db,
    `INSERT INTO payments (id, amount_minor, currency, account_id, created_at)
     VALUES (:id, :amount_minor, :currency, :account_id, :created_at)`,
    payment,
)
```

Or a typed query builder that emits parameterised SQL. Whatever approach is used, the output must still be parameterised — a query builder that interpolates values into the string is not an improvement.

## B5 — Log slow queries [SHOULD]

```go
start := time.Now()
rows, err := db.QueryContext(ctx, query, args...)
elapsed := time.Since(start)

if elapsed > cfg.SlowQueryThreshold {
    slog.WarnContext(ctx, "slow query",
        slog.Duration("elapsed", elapsed),
        slog.String("fingerprint", fingerprint(query)), // not params; no PII
    )
}
```

Log the query fingerprint (the template with parameters removed), not the parameter values — parameters may contain PII or sensitive data. The fingerprint is sufficient to identify the query for optimisation.

## Common findings

1. `fmt.Sprintf` building SQL from input — injection.
2. `rows.Close()` called only on the non-error path; forgotten on an error path — pool starvation.
3. `rows.Err()` not checked — partial result sets returned as complete.
4. Schema change adds a column; scan silently drops it because `Scan(&a, &b)` has the wrong arity.
5. Slow queries logged with parameter values containing PII.
