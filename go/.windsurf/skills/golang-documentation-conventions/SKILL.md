---
name: golang-documentation-conventions
description: Go documentation conventions — godoc comments, README structure, OpenAPI/Swagger contract documentation, Architecture Decision Records (ADRs), runbooks, and changelog conventions. Use whenever Go code is being documented, a README is being written or reviewed, an API contract needs to be documented, or an ADR needs to be created. Activate on mentions of godoc, doc comment, README, OpenAPI, Swagger, API documentation, ADR, architecture decision, runbook, changelog, or "how do I document this in Go". These are engineering conventions synthesised from Go community practice (godoc standards), OpenAPI specification, and widely-accepted documentation practices. No official standard exists beyond godoc formatting rules.
---

# Go Documentation Conventions

Conventions for documentation in Go projects. **These are conventions, not a standard.**

Exception: godoc comment formatting is effectively standardised by the Go toolchain (`go doc`, `pkg.go.dev`). The rules around godoc are as close to "official" as anything in Go documentation gets. Everything else is community convention.

## How to use this skill

1. Walk the rule categories when writing or reviewing documentation.
2. **MUST** rules carry real consequences (missing godoc breaks `go doc`; missing OpenAPI contract breaks client code generation; missing runbook means an incident takes longer to resolve).
3. **SHOULD** rules are strong conventions.

## Sources

- **godoc** — Go standard library documentation conventions (documented by the Go team at `go.dev/doc/comment`). This is as close to an official standard as Go has for documentation format.
- **OpenAPI 3.x** — the industry-standard API contract format.
- **ADR format** — Michael Nygard's original ADR format, widely adopted.
- **Keep a Changelog** — a widely-followed changelog format convention.

Full notes: `references/sources-and-rationale.md`.

## Rule categories

| # | Category | Reference file |
|---|---|---|
| A | godoc comments | `references/A-godoc.md` |
| B | README | `references/B-readme.md` |
| C | OpenAPI / API contract | `references/C-openapi.md` |
| D | Architecture Decision Records | `references/D-adr.md` |
| E | Runbooks | `references/E-runbooks.md` |

---

## Rule index

### A. godoc comments

- **A1 [MUST]** Every exported symbol (function, type, method, constant, variable, package) has a doc comment. `go doc` and `pkg.go.dev` use these; missing comments are gaps in the public API surface.
- **A2 [MUST]** A doc comment starts with the name of the symbol being documented, in the form of a sentence. `// NewPayment creates a new payment with...` not `// Creates a new payment...`
- **A3 [MUST]** Package-level comments (`// Package payments provides...`) describe the package's purpose in one or two sentences. For a large package, put the package comment in a dedicated `doc.go` file.
- **A4 [SHOULD]** Include an example (`func Example_...` or `func ExampleFunctionName`) for complex or non-obvious functions. Examples appear in `go doc` output and are compiled and run by `go test`.
- **A5 [SHOULD]** Document error returns — what errors can be returned and under what conditions. Especially important for errors that callers must handle differently (`ErrNotFound`, `ErrDuplicate`).
- **A6 [SHOULD]** Document concurrency characteristics — whether a type is safe for concurrent use and any locking requirements the caller must satisfy.

### B. README

- **B1 [MUST]** Every repository and every significant sub-service has a `README.md` at its root.
- **B2 [MUST]** The README covers: what the service does (one paragraph), how to run it locally, how to run tests, required environment variables, and links to further documentation (API contract, runbooks, ADRs).
- **B3 [SHOULD]** The README is the entry point; it links out rather than duplicating content. Long runbooks, architecture decisions, and API contracts live in their own files.
- **B4 [SHOULD]** The README includes a badge or note indicating the current build status and (for BFSI) the applicable regulatory scope (`bfsi-india-core` category tag).

### C. OpenAPI / API contract

- **C1 [MUST]** Every HTTP API has an OpenAPI 3.x specification (`api/openapi.yaml` or `api/openapi.json`). The spec is the contract between the service and its consumers.
- **C2 [MUST]** The spec is versioned alongside the code. A PR that changes API behaviour must also update the spec.
- **C3 [MUST]** The spec documents all error responses with their status codes and the stable error `code` values (see `golang-api-conventions` rule E4). Consumers need to know what `PAYMENT_DECLINED` means.
- **C4 [SHOULD]** Generate server stubs or request/response validators from the spec (code-first or spec-first — choose one and be consistent). A spec that diverges from the implementation is worse than no spec.
- **C5 [SHOULD]** Mark deprecated endpoints with `deprecated: true` in the spec and add a `Deprecation` / `Sunset` header in responses so clients can detect and act on the deprecation.

### D. Architecture Decision Records (ADRs)

- **D1 [SHOULD]** Use ADRs to record significant architectural decisions: why a technology was chosen, why an alternative was rejected, what trade-offs were made.
- **D2 [SHOULD]** Store ADRs in the repository under `docs/decisions/` as numbered Markdown files: `0001-use-postgresql.md`, `0002-use-redis-for-sessions.md`.
- **D3 [SHOULD]** An ADR has five sections: Title, Status, Context, Decision, Consequences.
- **D4 [SHOULD]** Once an ADR is accepted, it is immutable (except for a status update to Superseded). Decisions are documented, not revised.
- **D5 [MAY]** For BFSI services, record in an ADR any decision to deviate from a `bfsi-india-core` SHOULD rule, including the compensating control.

### E. Runbooks

- **E1 [MUST]** Every production service has at least one runbook covering: how to deploy, how to roll back, common failure modes and their resolution, on-call escalation path, and links to dashboards and logs.
- **E2 [MUST]** For BFSI services: the runbook includes the CERT-In 6-hour incident-reporting procedure, the RBI incident-notification process, and the named on-call owner for each.
- **E3 [SHOULD]** Runbooks are tested — someone not involved in writing them follows them cold. An untested runbook is a guess.
- **E4 [SHOULD]** Runbooks are linked from the README and from the on-call alert (e.g. in the alert annotation). A runbook no-one can find in an incident is no runbook.

## Out of scope

- API versioning strategy — see `golang-api-conventions` rule C.
- Code style comments — enforced by `gofmt` / `golint`, not a documentation concern.
- Internal wiki pages — team-specific; this skill covers what lives in the repository.
