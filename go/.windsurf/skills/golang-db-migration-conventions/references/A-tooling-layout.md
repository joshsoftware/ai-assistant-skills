# A. Tooling & Directory Layout

Expands rules A1–A5.

## A1 + A2 — golang-migrate setup [MUST]

```
myservice/
├── migrations/
│   ├── 20250315130000_create_payments.up.sql
│   ├── 20250315130000_create_payments.down.sql
│   ├── 20250316090000_add_payment_idempotency_key.up.sql
│   ├── 20250316090000_add_payment_idempotency_key.down.sql
│   └── 20250317110000_create_audit_log.up.sql
│   └── 20250317110000_create_audit_log.down.sql
├── cmd/migrate/
│   └── main.go       ← standalone migration runner
├── cmd/api/
│   └── main.go
└── go.mod
```

A standalone migration runner (`cmd/migrate/`) is preferred over running migrations inside the API server, so:
- Migrations are a separate step in the deployment pipeline.
- The API server binary does not need migration credentials.
- Migrations can be rolled back independently of the API server.

## A3 — Migrations in the same PR [MUST]

A PR adding a `payment_status` column to Go code must include the migration that creates it. A PR that only modifies Go code expecting a column that does not exist yet will fail in production.

Use a CI check that flags if Go code references a column not present in the migration files.

## A4 — SQL over ORM migrations [SHOULD]

```sql
-- 20250315130000_create_payments.up.sql
-- Add: payments table for recording payment intents
-- Ticket: PAY-1234
BEGIN;

CREATE TABLE IF NOT EXISTS payments (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id      UUID        NOT NULL,
    amount_minor   BIGINT      NOT NULL CHECK (amount_minor > 0),    -- no float
    currency       CHAR(3)     NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'pending'
                               CHECK (status IN ('pending','processing','completed','failed','reversed')),
    idempotency_key VARCHAR(128) NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),               -- UTC
    created_by     UUID        NOT NULL,
    CONSTRAINT payments_idempotency_key_uq UNIQUE (tenant_id, idempotency_key)
);

CREATE INDEX payments_tenant_status_idx ON payments (tenant_id, status);
CREATE INDEX payments_created_at_idx ON payments (created_at);

COMMIT;
```

Note the BFSI conventions applied in the schema itself:
- `amount_minor BIGINT` — never FLOAT. Mirrors `golang-bfsi-bindings` rule L1.
- `TIMESTAMPTZ` — UTC-stored timestamps. Mirrors `golang-data-storage-conventions` rule E2.
- `CHECK` constraint on status — enumeration enforced at DB level. Mirrors rule E5.
- `UNIQUE (tenant_id, idempotency_key)` — deduplication enforced at DB level.
- `NOT NULL` on required audit columns.

---

# B. Migration File Conventions

Expands rules B1–B6.

## B2 — Every up has a down [MUST]

```sql
-- 20250316090000_add_payment_idempotency_key.down.sql
-- Reversal: drop the idempotency key column added in the up migration
-- Note: this drops data — only use in local dev / disaster recovery
BEGIN;
ALTER TABLE payments DROP COLUMN IF EXISTS idempotency_key;
COMMIT;
```

The down migration is a safety net, not a routine operation. In production, forward migrations are the only path. The down exists so that:
1. A developer can roll back locally after testing.
2. In a true disaster, a DBA can run it manually after taking a backup.

## B4 — One logical change per file [MUST]

```
-- WRONG: one file doing three things
20250320_add_column_backfill_and_index.up.sql

-- RIGHT: three focused files
20250320100000_add_payment_ref_column.up.sql      ← ADD COLUMN (nullable)
20250320100001_backfill_payment_ref.up.sql        ← UPDATE in batches
20250320100002_payment_ref_not_null_and_index.up.sql ← SET NOT NULL + CREATE INDEX
```

Focused migrations are easier to understand, easier to roll back, and clearer in their blast radius if something goes wrong.

---

# C. Zero-Downtime Patterns

Expands rules C1–C5.

## C1 — Adding a non-nullable column [MUST]

```sql
-- Step 1 (in one deployment): add nullable with a default
ALTER TABLE payments ADD COLUMN fee_minor BIGINT DEFAULT 0;

-- Step 2 (in a later deployment, after application populates it): set not-null
ALTER TABLE payments ALTER COLUMN fee_minor SET NOT NULL;
```

A naked `ALTER TABLE payments ADD COLUMN fee_minor BIGINT NOT NULL` on a live table:
- PostgreSQL < 11: rewrites the table, acquiring an exclusive lock for the duration. Outage on large tables.
- PostgreSQL >= 11: instant for columns with a default (stored as a default, not a rewrite).

When in doubt, use the two-step approach.

## C2 — Removing a column safely [MUST]

```
Deployment N:   code still references old_col; schema has both old_col and new_col
Deployment N+1: code references only new_col
Deployment N+2: migration drops old_col
```

Never drop a column in the same deployment that removes the code reference. A rolling deploy will have old instances writing to a column that no longer exists.

## C3 — Concurrent index creation [SHOULD]

```sql
-- WRONG on a live, high-traffic table: locks writers for the duration
CREATE INDEX payments_account_idx ON payments (account_id);

-- RIGHT: creates the index without blocking writers
CREATE INDEX CONCURRENTLY payments_account_idx ON payments (account_id);
```

`CREATE INDEX CONCURRENTLY` cannot run inside a transaction block. Run it outside a `BEGIN/COMMIT` wrapper.

---

# D. Data Migrations

Expands rules D1–D3.

## D2 + D3 — Batched, idempotent backfill [MUST]

```sql
-- 20250320100001_backfill_payment_ref.up.sql
-- Backfill: populate payment_ref from legacy_ref for existing rows
-- Idempotent: WHERE payment_ref IS NULL skips already-migrated rows
-- Batched: 1000 rows per commit, LOOP until done
DO $$
DECLARE
    updated INT;
BEGIN
    LOOP
        UPDATE payments
        SET payment_ref = legacy_ref
        WHERE payment_ref IS NULL    -- idempotent: skip already-migrated rows
          AND legacy_ref IS NOT NULL
        LIMIT 1000;                  -- batch: 1000 rows per commit

        GET DIAGNOSTICS updated = ROW_COUNT;
        EXIT WHEN updated = 0;      -- done when no more rows to update

        PERFORM pg_sleep(0.01);     -- brief pause to reduce lock contention
    END LOOP;
END $$;
```

For very large tables (> 10M rows), run this outside the migration tool as a background job with monitoring and progress logging. Record the estimate in the PR.

---

# E. CI & Deployment

Expands rules E1–E5.

## E2 — Advisory lock for concurrent runners [MUST]

With golang-migrate:
```go
m, err := migrate.New("file://migrations", dsn)
if err != nil { return err }
// golang-migrate uses a schema_migrations table with a lock — concurrent runs are safe by default
if err := m.Up(); err != nil && err != migrate.ErrNoChange {
    return fmt.Errorf("migrate up: %w", err)
}
```

With goose:
```go
// goose uses a goose_db_version table with a lock
if err := goose.Up(db, "migrations"); err != nil {
    return fmt.Errorf("goose up: %w", err)
}
```

Both tools manage their own advisory lock via a version table. The advisory lock prevents two concurrent migration runners from applying the same migration simultaneously.

## E1 — CI validation [MUST]

```yaml
# In CI pipeline
- name: Validate migrations
  run: |
    migrate validate -path ./migrations -database "$DB_DSN"
    # Or for goose: goose -dir ./migrations validate
```

A malformed SQL file must not reach production.

## Common findings

1. No migration tool — schema changes applied by hand with no version tracking.
2. Large migration adding a column, backfilling, and adding an index in one file — hard to roll back if the backfill fails.
3. `ADD COLUMN col NOT NULL` on a large table without a default — table lock in production.
4. `DROP COLUMN` in the same deployment that removes the code reference — old instances crash.
5. `CREATE INDEX` (not CONCURRENTLY) on a large table — writers blocked for minutes.
6. Data migration as a single UPDATE touching millions of rows — lock held for minutes.
7. Migration file exists but the CI does not validate it — syntax error discovered in production.
