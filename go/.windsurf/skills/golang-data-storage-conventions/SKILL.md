---
name: golang-data-storage-conventions
description: Go data storage conventions for relational databases, connection pool configuration, the repository pattern, query building, transaction management, and data integrity. Use whenever Go code interacts with a relational database (PostgreSQL, MySQL/MariaDB, SQLite), or when data-access code is being written, reviewed, or structured. Activate on mentions of database/sql, sqlx, pgx, GORM, connection pool, sql.DB, repository, query, transaction, migration, ORM, or "how do I store/retrieve data in Go". These are engineering conventions built on Go standard library practice and widely-accepted patterns. For BFSI-specific data rules (money types, double-entry, idempotency, PII storage, audit trails) also consult bfsi-india-core and golang-bfsi-bindings.
---

# Go Data Storage Conventions

Conventions for relational database access in Go. **These are conventions, not a standard.**

The rules here cover how to configure and use `database/sql` and the `pgx`/`sqlx` ecosystem correctly. BFSI-specific obligations (money handling, ledger integrity, idempotency, PII masking, audit trails) are summarised in the interaction section and governed in depth by `golang-bfsi-bindings`.

## How to use this skill

1. When writing or reviewing data-access code, walk the rule categories.
2. Each rule has a severity (RFC 2119) and links to a `references/` file.
3. **MUST** violations are blockers.

## Sources

- Go standard library `database/sql` documentation and design intent.
- `pgx` (PostgreSQL) and `sqlx` library documentation and community practice.
- Widely-accepted repository-pattern guidance for Go.
- 12-Factor App (configuration from environment).

Full notes: `references/sources-and-rationale.md`.

## Rule categories

| # | Category | Reference file |
|---|---|---|
| A | Connection pool setup | `references/A-connection-pool.md` |
| B | Query execution & parameterisation | `references/B-queries.md` |
| C | Transaction management | `references/C-transactions.md` |
| D | Repository pattern | `references/D-repository-pattern.md` |
| E | Data integrity & correctness | `references/E-data-integrity.md` |

---

## Rule index

### A. Connection pool setup

- **A1 [MUST]** Create `sql.DB` once at application startup and share it; it is a pool, not a single connection. Never create a new `sql.DB` per request.
- **A2 [MUST]** Configure pool limits explicitly: `SetMaxOpenConns`, `SetMaxIdleConns`, `SetConnMaxLifetime`, `SetConnMaxIdleTime`. The zero-value defaults are unsafe for production (unlimited open connections).
- **A3 [MUST]** Verify connectivity at startup with `db.PingContext(ctx)` before accepting traffic. Fail fast if the database is unreachable.
- **A4 [MUST]** Pass `context.Context` to every `database/sql` call (`QueryContext`, `ExecContext`, `BeginTx`). Never use the context-free variants (`Query`, `Exec`, `Begin`) in a long-running service.
- **A5 [SHOULD]** Set `ConnMaxLifetime` to prevent stale connections after a database failover or firewall timeout. Typical value: 5–15 minutes.

### B. Query execution & parameterisation

- **B1 [MUST]** All queries use parameterised statements. String-formatted or concatenated SQL is prohibited — this is SQL injection, and column/table names needed dynamically must come from a server-side allow-list, never from raw input.
- **B2 [MUST]** Close `*sql.Rows` with `defer rows.Close()` immediately after a successful `QueryContext`. Check `rows.Err()` after iterating.
- **B3 [MUST]** Scan into typed fields, not `interface{}`. Typed scans catch schema/code drift at compile time or early runtime.
- **B4 [SHOULD]** Use named parameters or a query builder for complex queries to improve readability and reduce positional-parameter mistakes. Whatever the approach, it must still produce parameterised SQL.
- **B5 [SHOULD]** Log slow queries (above a configurable threshold) at a warning level with the query fingerprint (not parameter values) and execution duration.

### C. Transaction management

- **C1 [MUST]** Use `db.BeginTx(ctx, opts)` — always with context, never bare `db.Begin()`.
- **C2 [MUST]** Always `defer tx.Rollback()` immediately after `BeginTx`; the deferred rollback is a no-op after a successful commit. This prevents forgetting to roll back on an early return.
- **C3 [MUST]** Choose the isolation level explicitly for the workload. Do not rely on the database driver's default without understanding it. Ledger/financial postings use serializable or repeatable-read. Read-heavy reporting uses read-committed.
- **C4 [MUST]** Never pass a `*sql.Tx` to a function alongside the original `*sql.DB`. The repository receives one or the other. A common pattern is to make the repository's data-access interface accept a `DBTX` interface (`QueryContext`, `ExecContext`) satisfied by both `*sql.DB` and `*sql.Tx`.
- **C5 [SHOULD]** For long-running background operations that update many rows, prefer batching with small transactions over one giant transaction to avoid long lock-hold times.

### D. Repository pattern

- **D1 [SHOULD]** Encapsulate all data-access logic in a repository layer. Handlers and services have no SQL; they call repository methods.
- **D2 [SHOULD]** Define the repository as an interface in the consuming service's package (not the repository package), listing only the methods the service needs. This enables fake/mock implementations for testing.
- **D3 [SHOULD]** Name repository methods by their intent: `Save`, `FindByID`, `ListByAccount`, `Delete`. Avoid exposing the underlying SQL semantics in the name.
- **D4 [MUST]** Repository methods always take `context.Context` as their first parameter.
- **D5 [MAY]** ORMs are acceptable. When using one, understand how it generates SQL and ensure all paths use parameterised queries. Use the raw-query escape hatch sparingly and with the same parameterisation discipline as `database/sql`.

### E. Data integrity & correctness

- **E1 [MUST]** Monetary amounts are stored as integers (minor units — paise, cents) or a fixed-precision decimal type. Never as `FLOAT`, `REAL`, or `DOUBLE PRECISION`. *(Restated from `golang-bfsi-bindings` L1 — it is also a storage-schema decision.)*
- **E2 [MUST]** Timestamps are stored in UTC. The application converts to IST or any other timezone only for display.
- **E3 [SHOULD]** Enforce uniqueness and referential integrity with database constraints, not only in application code. Constraints survive direct database access and data migrations.
- **E4 [SHOULD]** Use `NOT NULL` on columns that must always have a value. A nullable column where null is semantically meaningless signals schema drift.
- **E5 [SHOULD]** Add database-level check constraints for enumerations and ranges that the application enforces. A constraint that only lives in the application is bypassed by a direct write or a bug.

## Interaction with BFSI skills

The following rules appear here as MUST statements but are governed in full depth (rationale, regulator basis, patterns) by `golang-bfsi-bindings`:
- Money types → `go-L-money-idempotency.md`
- Idempotency at the data layer → `go-L-money-idempotency.md`
- Audit-trail persistence → `go-F-logging.md`
- PII storage and masking → `go-E-data-pii.md`
- Parameterised queries (BFSI security angle) → `go-C-input-output.md`

## Out of scope

- Database migration tooling — covered by `golang-db-migration-conventions` skill.
- NoSQL / document stores / time-series / vector databases — different patterns.
- ORM-specific configuration — the skill is ORM-agnostic.
