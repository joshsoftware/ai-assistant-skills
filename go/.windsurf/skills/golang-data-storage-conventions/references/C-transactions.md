# C. Transaction Management

Expands rules C1–C5.

## C1 + C2 — BeginTx + deferred Rollback [MUST]

```go
func (r *Repository) Transfer(ctx context.Context, from, to AccountID, amount Money) error {
    tx, err := r.db.BeginTx(ctx, &sql.TxOptions{Isolation: sql.LevelSerializable})
    if err != nil {
        return fmt.Errorf("begin tx: %w", err)
    }
    // Deferred Rollback is a safety net:
    // - If Commit is called successfully, Rollback is a no-op.
    // - If we return early (any error path), Rollback fires automatically.
    defer tx.Rollback() //nolint:errcheck // no-op after Commit; error logged separately

    if err := debitAccount(ctx, tx, from, amount); err != nil {
        return fmt.Errorf("debit: %w", err) // Rollback fires via defer
    }
    if err := creditAccount(ctx, tx, to, amount); err != nil {
        return fmt.Errorf("credit: %w", err) // Rollback fires via defer
    }
    if err := tx.Commit(); err != nil {
        return fmt.Errorf("commit: %w", err)
    }
    return nil
}
```

Why `defer tx.Rollback()` and not explicit rollback? Because explicit rollback is often forgotten on early-return paths. The pattern above is safe: after a successful `Commit()`, the deferred `Rollback()` returns `sql.ErrTxDone` which we intentionally ignore. After any early return, the deferred `Rollback()` cleans up properly.

## C3 — Explicit isolation level [MUST]

```go
// Ledger postings — prevent phantom reads and write skew
tx, err := db.BeginTx(ctx, &sql.TxOptions{Isolation: sql.LevelSerializable})

// Reporting queries — read-committed avoids blocking writes, adequate for reports
tx, err := db.BeginTx(ctx, &sql.TxOptions{
    Isolation: sql.LevelReadCommitted,
    ReadOnly:  true,
})
```

Isolation levels and their guarantees vary by database engine:
- PostgreSQL `SERIALIZABLE` — SSI (Serializable Snapshot Isolation); full serializable semantics.
- MySQL/MariaDB `REPEATABLE READ` — default; prevents dirty/non-repeatable reads but not phantom reads (MySQL has gap locks that partially address this).
- PostgreSQL `REPEATABLE READ` — snapshot isolation; prevents dirty, non-repeatable, and most phantom reads.

Know which level your engine provides and whether it matches the semantic guarantee you need.

## C4 — DBTX interface [MUST]

A function that runs inside a transaction should not accept `*sql.DB` — it cannot be called transactionally. But it also should not unconditionally accept `*sql.Tx`, or it cannot be called outside a transaction.

The `DBTX` interface (common pattern):

```go
// In the repository or a shared db package:
type DBTX interface {
    QueryContext(ctx context.Context, query string, args ...any) (*sql.Rows, error)
    ExecContext(ctx context.Context, query string, args ...any) (sql.Result, error)
    QueryRowContext(ctx context.Context, query string, args ...any) *sql.Row
}

type Repository struct {
    db DBTX // satisfied by both *sql.DB and *sql.Tx
}
```

The service layer that needs a transaction creates the `*sql.Tx` and constructs a `Repository` with it for the duration of the transaction. Outside a transaction, it constructs the repository with `*sql.DB`.

## C5 — Small transactions for bulk operations [SHOULD]

A single transaction updating 1 million rows holds locks for the duration, blocks all concurrent writers, and fills the WAL/binlog. Batch it:

```go
const batchSize = 500
for i := 0; i < len(rows); i += batchSize {
    batch := rows[i:min(i+batchSize, len(rows))]
    if err := processInTx(ctx, db, batch); err != nil {
        return err
    }
}
```

Design idempotent batches so a retry after a partial failure does not double-process.

## Common findings

1. `db.Begin()` without context — no cancellation or deadline propagation.
2. No `defer tx.Rollback()` — early returns leave the transaction open, holding locks.
3. Isolation level left at the driver's implicit default — developer does not know what isolation the transaction actually has.
4. Mixing `*sql.DB` and `*sql.Tx` in the same function — a write on the DB bypasses the transaction.
5. One giant transaction for a batch operation, blocking all other writers for minutes.
