# Sources & Rationale

This skill is **engineering conventions, not a standard.**

## What this skill draws on

- **golang-migrate** (`github.com/golang-migrate/migrate/v4`) — one of the two most widely used Go migration tools. SQL-first, supports PostgreSQL, MySQL, SQLite, and more.
- **goose** (`github.com/pressly/goose/v3`) — the other widely used tool. Supports both SQL and Go-based migrations.
- **PostgreSQL DDL documentation** — `CREATE INDEX CONCURRENTLY`, transaction support for DDL, `ALTER TABLE` locking semantics.
- **Zero-downtime migration literature** — widely published in SRE and database engineering communities (Stripe engineering blog, GitLab migration docs, etc.). No single authoritative source; the expand–contract pattern is the canonical approach.
- **BFSI schema conventions** — the migration examples in this skill apply the BFSI data-type rules from `golang-data-storage-conventions` (integer minor units, TIMESTAMPTZ for UTC, NOT NULL audit columns) directly in the SQL. This is where those rules materialise at the schema level.

## Tooling choice

The skill is tool-agnostic in its rules, with golang-migrate as the primary example. The choice between golang-migrate and goose is a team convention:

- **golang-migrate**: SQL-only by default, pure CLI / Go library, wide database support, no embedded Go code in migrations.
- **goose**: supports SQL and Go-based migrations (useful for data migrations needing Go logic), has a higher-level API, slightly simpler CLI.

**ORM auto-migration (GORM AutoMigrate, ent schema):** not recommended for production BFSI services. These tools are convenient for development but are hard to audit, cannot express downgrade steps, and may produce unexpected DDL. If used, the same principles (version-controlled, reviewed, tested) must apply to whatever they generate.

## Zero-downtime migration note

The `CREATE INDEX CONCURRENTLY` and expand–contract patterns are PostgreSQL-primary. MySQL has different locking semantics for DDL (many operations use online DDL by default in MySQL 8+). The principle — avoid exclusive locks on live tables — applies everywhere; the specific SQL syntax differs.
