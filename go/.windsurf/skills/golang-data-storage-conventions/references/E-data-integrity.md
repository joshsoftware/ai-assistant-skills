# E. Data Integrity & Correctness

Expands rules E1–E5.

## E1 — Money as integer minor units or fixed-precision decimal [MUST]

```sql
-- Schema: use INTEGER or BIGINT for minor units
amount_minor  BIGINT      NOT NULL CHECK (amount_minor >= 0),
currency      CHAR(3)     NOT NULL,

-- Or: NUMERIC with explicit precision and scale
amount        NUMERIC(19,4) NOT NULL CHECK (amount >= 0),
```

Never `FLOAT`, `REAL`, or `DOUBLE PRECISION` for monetary values. Floating-point types cannot represent decimal fractions exactly (0.1 + 0.2 ≠ 0.3 in binary floating-point). For a payment system, this produces real rounding errors at scale.

In Go, a `BIGINT` column maps to `int64` (minor units, e.g. paise for INR). A `NUMERIC` column maps to a `decimal.Decimal` from `github.com/shopspring/decimal` or `string` — never to `float64`. See `golang-bfsi-bindings` go-L-money-idempotency.md.

## E2 — UTC timestamps [MUST]

```sql
-- Schema
created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),  -- PostgreSQL (zone-aware)
updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
```

```go
// Go side — always UTC
p.CreatedAt = time.Now().UTC()
_, err := db.ExecContext(ctx,
    "INSERT INTO payments (id, created_at) VALUES ($1, $2)",
    p.ID, p.CreatedAt,
)
```

Store timestamps in UTC (or as `TIMESTAMPTZ` which carries the zone). Convert to IST or any other timezone only for display. Mixing stored timezones produces incorrect comparisons, wrong settlement-window calculations, and broken reconciliation.

## E3 — Enforce constraints in the database [SHOULD]

Application-level validation is a first line of defence. Database constraints are a backstop that holds even when:
- A developer bypasses the application (running a direct SQL update for a hotfix).
- A bug allows invalid data through the validation layer.
- A data migration script writes data directly.

Examples:

```sql
-- Referential integrity
account_id  UUID NOT NULL REFERENCES accounts(id),

-- Uniqueness
UNIQUE (idempotency_key),
UNIQUE (account_id, txn_date, ref),

-- Range
CHECK (amount_minor > 0),
CHECK (status IN ('pending', 'completed', 'failed', 'reversed')),
```

A `UNIQUE (idempotency_key)` constraint at the database level provides a final guard against duplicate inserts even under concurrent requests that both pass the application-level duplicate check.

## E4 — NOT NULL on required columns [SHOULD]

If a column must always have a value, declare it `NOT NULL`. A nullable column that is never legitimately null will, given enough time, get a null from a bug or a migration — and the application will need to handle it. Express the intent in the schema.

## E5 — Enum and range check constraints [SHOULD]

```sql
-- Explicit enum constraint
status VARCHAR(20) NOT NULL CHECK (status IN ('pending', 'processing', 'completed', 'failed')),
```

When a new status is added to the Go enum, the database constraint must be updated at the same time. The constraint makes that easy to see in the migration. Without it, an invalid status can be stored silently.

## E6 — Audit columns [SHOULD for BFSI workloads]

```sql
created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
created_by   UUID        NOT NULL,
updated_at   TIMESTAMPTZ,
updated_by   UUID,
```

Add audit columns to every entity that matters for compliance. These are not the full audit-trail (which is append-only and lives in an audit table — see `golang-bfsi-bindings` go-F-logging.md) but the basic "who created / last updated" on the record. Populate them from the caller identity in the context.

## Common findings

1. `FLOAT` or `DOUBLE` column for a monetary amount — rounding errors at scale.
2. Timestamps stored as local time or as a bare integer — DST bugs, reconciliation drift.
3. Unique idempotency-key constraint missing — duplicate inserts possible under concurrency.
4. Foreign-key constraints absent — orphaned records accumulate silently.
5. Status/type column with no CHECK constraint — invalid values storable.
