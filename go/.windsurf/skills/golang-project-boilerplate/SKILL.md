---
name: golang-project-boilerplate
description: Go project layout and boilerplate conventions for backend services and BFSI applications. Use whenever a new Go service, module, or repository is being scaffolded, or when an existing Go project's structure is being reviewed or reorganised. Activate on mentions of project layout, folder structure, scaffolding, boilerplate, cmd/internal/pkg directories, main.go organisation, dependency wiring, application bootstrap, graceful shutdown, or "how should I structure this Go service". These are team engineering conventions synthesised from widely-accepted Go community practice \u2014 they are NOT an official standard. Pair with golang-bfsi-bindings when the project is a BFSI application.
---

# Go Project Boilerplate & Layout Conventions

Team conventions for structuring Go services. **These are conventions, not a standard.** Even the well-known `golang-standards/project-layout` repository is explicitly not an official Go standard and is not universally endorsed by the Go community. Apply judgement; adopt only what the project's size justifies.

## How to use this skill

1. When scaffolding or reviewing a Go service, walk the rule categories below.
2. Each rule has a severity (RFC 2119: **MUST**, **SHOULD**, **MAY**) and links to a `references/` file for rationale and examples.
3. Treat **MUST** violations as blockers. **SHOULD** violations need a documented reason.
4. Scale the structure to the project. A small service does not need every directory.

## Sources (community practice, not standards)

- The Go community's `project-layout` patterns (widely referenced; explicitly not an official standard).
- The Twelve-Factor App methodology (config, dependencies, processes).
- Standard library conventions (`net/http`, `context`, `log/slog`).
- Effective Go and the Go Code Review Comments.

Full notes: `references/sources-and-rationale.md`.

## Rule categories

| # | Category | Reference file |
|---|---|---|
| A | Directory layout | `references/A-directory-layout.md` |
| B | Entry points & bootstrap | `references/B-entrypoint-bootstrap.md` |
| C | Dependency wiring & configuration | `references/C-wiring-config.md` |
| D | Module hygiene | `references/D-module-hygiene.md` |

---

## Rule index

### A. Directory layout

- **A1 [SHOULD]** Place each executable's `main` package under `cmd/<binary-name>/`. The directory name matches the binary. *(Community `project-layout` convention.)*
- **A2 [SHOULD]** Place code that must not be imported by other repositories under `internal/`. The Go toolchain enforces the import boundary. *(Standard library mechanism.)*
- **A3 [MAY]** Place code intentionally meant for reuse by other repositories under `pkg/`. If nothing is meant to be reused, omit `pkg/` entirely.
- **A4 [SHOULD]** Organise `internal/` by domain/feature (e.g. `internal/payment/`, `internal/ledger/`), not by technical layer (`internal/handlers/`, `internal/services/`). Domain grouping keeps related code together and limits cross-package coupling.
- **A5 [SHOULD]** Within a domain package, the common file split is `handler.go` (transport), `service.go` (business logic), `repository.go` or `store.go` (data access), `model.go` (types). Keep transport, logic, and data access separated.
- **A6 [MUST]** Do not put substantial logic in `cmd/.../main.go`. `main` wires dependencies and starts the process; nothing else.
- **A7 [MAY]** Use `api/` for API contract artefacts (OpenAPI specs, protobuf files), `configs/` for config templates, `migrations/` for DB migration files, `scripts/` for build/ops scripts, `deployments/` for deployment manifests.
- **A8 [SHOULD]** Keep the repository root uncluttered: `go.mod`, `go.sum`, `README.md`, `Makefile`/build config, and top-level directories. Avoid scattering many `.go` files in root for a multi-package service.

### B. Entry points & bootstrap

- **B1 [MUST]** `main()` delegates immediately to a `run(ctx context.Context) error` function. `main` only calls `run`, handles the returned error, and sets the exit code. This makes the bootstrap testable. *(Widely-adopted Go pattern.)*
- **B2 [MUST]** The bootstrap creates a root `context.Context` that is cancelled on `SIGINT`/`SIGTERM` (`signal.NotifyContext`), and passes it down. *(Standard library; required for graceful shutdown.)*
- **B3 [MUST]** Implement graceful shutdown: on signal, stop accepting new work, drain in-flight work within a bounded timeout, then exit. For HTTP, use `http.Server.Shutdown`. *(Critical for BFSI \u2014 in-flight transactions must not be stranded.)*
- **B4 [SHOULD]** Construct dependencies (config, logger, DB pool, clients) explicitly in the bootstrap and inject them downward. Avoid hidden global state.
- **B5 [SHOULD]** Fail fast on startup: if a required dependency (DB, secret store, critical config) is unavailable, exit non-zero rather than starting in a degraded state.
- **B6 [MUST]** Set a non-zero exit code on fatal startup or runtime failure so orchestrators detect it.

### C. Dependency wiring & configuration

- **C1 [MUST]** Read configuration from the environment (Twelve-Factor). Do not hard-code environment-specific values (hosts, ports, DSNs, endpoints) in source. *(Twelve-Factor; see also the credential-management skill.)*
- **C2 [MUST]** Validate all configuration at startup. Missing or malformed required config causes a fail-fast exit with a clear message naming the offending key.
- **C3 [SHOULD]** Load configuration into a typed struct once, at startup; pass the struct (or narrowed sub-structs) to the components that need it. Do not call `os.Getenv` scattered through the codebase.
- **C4 [SHOULD]** Prefer explicit constructor functions (`NewService(deps...) *Service`) over package-level singletons. Explicit wiring is testable and makes the dependency graph visible.
- **C5 [SHOULD]** Define interfaces at the point of use (the consumer package), not the implementation package, so dependencies can be substituted in tests.
- **C6 [MAY]** A dependency-injection container or wiring tool is acceptable for large services, but manual wiring in the bootstrap is clearer for most.

### D. Module hygiene

- **D1 [MUST]** Commit `go.mod` and `go.sum`. `go.sum` is the integrity record for every dependency.
- **D2 [MUST]** The module path in `go.mod` matches the repository's canonical location.
- **D3 [SHOULD]** Keep the `go` directive in `go.mod` at a supported Go version; upgrade deliberately.
- **D4 [SHOULD]** A `replace` directive pointing outside the repository requires review before merge \u2014 it silently changes what code is built.
- **D5 [SHOULD]** Run `go mod tidy` before merge so the module files reflect actual imports.
- **D6 [MAY]** For multi-service repositories, a single module is usually simpler than multiple modules; choose multiple modules only with a clear reason.

## Out of scope

- CI/CD pipeline definitions \u2014 separate concern.
- Tooling configuration (linters, formatters) \u2014 conventions here are tool-agnostic.
- BFSI regulatory requirements \u2014 covered by `bfsi-india-core` / `golang-bfsi-bindings`.

## Interaction with other skills

When `golang-bfsi-bindings` is also active, that skill governs security and regulatory concerns; this skill governs structure. If they appear to conflict, the BFSI skill wins on security intent.
