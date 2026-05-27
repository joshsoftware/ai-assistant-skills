---
name: golang-db-migration-conventions
description: Go database migration conventions — migration tooling (golang-migrate, goose), file naming and directory layout, up/down pairs, transactional migrations, zero-downtime migration patterns, data migrations, advisory locks for concurrent deployments, CI validation, and BFSI schema conventions. Use whenever database schema changes are being planned or reviewed for a Go service. Activate on mentions of migration, schema change, DDL, golang-migrate, goose, migrate up, migrate down, ALTER TABLE, CREATE TABLE, ADD COLUMN, or "how do I migrate the database in Go". These are engineering conventions — no official standard governs database migration tooling or patterns.
---

# Go Database Migration Conventions

Conventions for database schema migrations in Go services. **These are conventions, not a standard.**

The tooling choices (golang-migrate vs goose) are community conventions with no single authority. The *principles* (every change is versioned, every up has a down, migrations are tested, data migrations are separate from schema migrations, zero-downtime patterns exist) are widely accepted engineering practice.

## How to use this skill

1. Walk the rule categories when planning or reviewing a schema change.
2. **MUST** violations are blockers. **SHOULD** violations need a documented reason.
3. For BFSI: the schema is subject to the same data-retention and data-type rules as the application (UTC timestamps, integer minor units for money, not-null constraints for required audit fields).

## Sources

- `golang-migrate` documentation.
- `goose` documentation.
- PostgreSQL DDL documentation.
- Zero-downtime migration patterns (widely published in SRE and database community).

Full notes: `references/sources-and-rationale.md`.

## Rule categories

| # | Category | Reference file |
|---|---|---|
| A | Tooling & directory layout | `references/A-tooling-layout.md` |
| B | Migration file conventions | `references/B-file-conventions.md` |
| C | Zero-downtime patterns | `references/C-zero-downtime.md` |
| D | Data migrations | `references/D-data-migrations.md` |
| E | CI & deployment | `references/E-ci-deployment.md` |

---

## Rule index

### A. Tooling & directory layout

- **A1 [MUST]** Use a versioned migration tool — golang-migrate or goose. Never apply schema changes via hand-run SQL scripts with no tracking.
- **A2 [MUST]** Migration files live under `migrations/` (or `db/migrations/`) in the repository, alongside the application code. Schema history is part of the codebase, not a separate artefact.
- **A3 [MUST]** Migrations are committed in the same PR as the application code that requires the schema change. A PR that modifies Go code to use a new column must include the migration that adds it.
- **A4 [SHOULD]** Prefer SQL-based migrations over ORM-generated or Go-code-based migrations for clarity and auditability. SQL migrations are readable by any database engineer.
- **A5 [SHOULD]** Run migrations at service startup (or in a separate deployment step before the new service version is deployed). Never run migrations from application-request handlers.

### B. Migration file conventions

- **B1 [MUST]** Migration files are named with a timestamp prefix: `20250315140000_add_payment_status.up.sql`. Timestamp ordering prevents naming conflicts when multiple developers add migrations.
- **B2 [MUST]** Every up migration has a corresponding down migration. The down migration must actually reverse the change — `DROP TABLE` for a `CREATE TABLE`, `DROP COLUMN` for an `ADD COLUMN`.
- **B3 [MUST]** Migrations are forward-only in production. The down migration exists for local development rollback and disaster recovery, not for routine reversal of deployed changes.
- **B4 [MUST]** Each migration file is focused and small. One logical change per file. A migration that adds a column, backfills data, and adds an index is three separate migrations.
- **B5 [SHOULD]** Wrap DDL statements in explicit transactions where the database supports it. A half-applied migration leaves the schema in an inconsistent state.
- **B6 [SHOULD]** Include a brief comment at the top of each migration file explaining the *why*: which feature or ticket drove the change.

### C. Zero-downtime patterns

- **C1 [MUST]** Adding a non-nullable column without a default requires a default value or a backfill before the constraint is applied. A naked `ADD COLUMN col NOT NULL` on a live table locks the table during backfill on PostgreSQL < 11.
- **C2 [MUST]** Never `DROP COLUMN` or rename a column in a single deployment if the running application still references it. Remove the code reference first, deploy, then drop the column in a subsequent migration.
- **C3 [SHOULD]** Adding an index to a large table uses `CREATE INDEX CONCURRENTLY` (PostgreSQL) to avoid locking writers. A regular `CREATE INDEX` acquires an exclusive lock.
- **C4 [SHOULD]** New columns should be nullable or have a database default so old application instances (still deployed during a rolling update) can still write rows without failing on the new column.
- **C5 [SHOULD]** Follow the expand–contract pattern for column renames: (1) add new column, (2) dual-write to both, (3) backfill old→new, (4) read from new, (5) stop writing to old, (6) drop old. This avoids a single-deployment breaking change.

### D. Data migrations

- **D1 [MUST]** Separate schema migrations (DDL) from data migrations (DML backfills). A DDL migration must complete instantly in production; a data migration that scans and updates millions of rows must run in the background, in batches.
- **D2 [MUST]** Data migrations run in small batches with a `LIMIT` and a commit between batches. A single transaction updating millions of rows holds locks, fills WAL, and risks timeout.
- **D3 [MUST]** Data migrations are idempotent: re-running the same data migration produces the same result. Use `WHERE` conditions that skip already-migrated rows.
- **D4 [SHOULD]** Data migrations are tested with a production-volume data set (or a representative sample) before running on production. Row count and estimated duration are stated in the PR.

### E. CI & deployment

- **E1 [MUST]** CI validates migrations: `migrate validate` or equivalent on every PR. A syntactically invalid migration file must never reach production.
- **E2 [MUST]** Use an advisory lock to prevent concurrent migration runs in multi-instance deployments. Both golang-migrate and goose support this; ensure it is configured.
- **E3 [MUST]** Before running migrations on production, verify a recent backup exists and is restorable. For BFSI services this links to the DR rules in `bfsi-india-core` category O.
- **E4 [SHOULD]** Run migrations against a copy of production data in a staging environment before applying to production for significant changes.
- **E5 [SHOULD]** Track the time each migration takes in staging. A migration that takes > 1 second on a small staging dataset will take proportionally longer on production — plan for it.

## Out of scope

- ORM-level migrations (GORM `AutoMigrate`) — the skill recommends explicit SQL migrations for auditability; if you use ORM migrations, the same principles apply.
- NoSQL schema changes — different patterns.
- Kafka topic schema evolution — covered by `golang-messaging-conventions` rule A1.
