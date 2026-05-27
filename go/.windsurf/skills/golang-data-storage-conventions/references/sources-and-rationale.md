# Sources & Rationale

This skill is **engineering conventions, not a standard.**

## What this skill draws on

- **Go `database/sql` documentation** — the package's design intent (pool, not connection; context-first; close rows) is documented by the standard library.
- **Go Database Tutorial** (`go.dev/doc/database/`) — the official Go guide on database access.
- **pgx and sqlx documentation** — widely-used Go database libraries.
- **Repository pattern** — a widely-known application architecture pattern (Evans DDD, Fowler PoEAA). There is no Go-specific standard for it; the pattern is adapted to Go idioms (interfaces at the consumer, DBTX interface).
- **12-Factor App** — for configuration of DSN / connection parameters from the environment.
- **OWASP ASVS V5** — for parameterised queries (the security basis for B1, also covered by `golang-bfsi-bindings` C2).

## Where the community varies

- **Repository pattern necessity** — some Go developers argue the repository pattern is over-engineering for simple services; others consider it essential for testability. The skill makes it a SHOULD, not a MUST, acknowledging this tension.
- **ORM vs raw SQL** — contested. The skill is neutral: ORMs are acceptable (MAY), with conditions. Neither "always use an ORM" nor "never use an ORM" is correct.
- **Connection pool sizing** — the specific numbers (25 open, 10 idle, etc.) are starting-point heuristics. The only firm rule is: configure them explicitly. The right values depend on the workload and the database server's limits.

## Relationship to BFSI skills

BFSI-specific data concerns — money types, idempotency, double-entry, PII masking, audit trails, data residency — are governed by `golang-bfsi-bindings` categories L, F, E, and M respectively. This skill restates E1 (money as integer/decimal) and E2 (UTC timestamps) because they are schema decisions as well as application decisions; the BFSI skill is authoritative on the regulatory intent.
