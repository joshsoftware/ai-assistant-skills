# Sources & Rationale

## What is genuinely standardised here

- **godoc comment format** — the Go team documents the formatting rules at `go.dev/doc/comment`. The "symbol name first" rule and package comment conventions are enforced by the toolchain. This is the closest thing to an official Go documentation standard.
- **OpenAPI 3.x** — the OpenAPI Initiative's specification. Industry standard for REST API contracts.

## What is convention

- **README structure** — no official standard. The sections (Quick start, Environment variables, Runbook link) are widely-used community convention.
- **ADR format** — Michael Nygard's original format (Context, Decision, Consequences) is the most widely adopted. There are variations (Y-Statements, MADR, RFC format). The skill picks the original Nygard format as a **SHOULD** because consistency within a team matters more than which format.
- **Runbook structure** — no standard. The required sections (deployment, rollback, failure modes, incident procedures) are derived from SRE practice and BFSI incident-reporting obligations.

## BFSI documentation obligations

The CERT-In 6-hour incident reporting requirement (rule N1 in `bfsi-india-core`) has a direct runbook implication: the runbook must contain the reporting procedure, the template, and the on-call contact, because 6 hours is not enough time to discover these under pressure. This is why runbooks are a **MUST** for BFSI services in this skill, not merely a SHOULD.

## Keep a Changelog

Not covered explicitly in this skill, but teams shipping a versioned API should consider `keepachangelog.com` format for `CHANGELOG.md`. It pairs naturally with the ADR practice.
