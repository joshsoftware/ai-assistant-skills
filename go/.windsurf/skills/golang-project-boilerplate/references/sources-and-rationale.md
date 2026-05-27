# Sources & Rationale

This skill is **engineering conventions, not a standard.** There is no official Go project-layout standard. This file records what the conventions are based on and where genuine disagreement exists.

## What this skill draws on

- **Go community `project-layout` patterns** \u2014 the widely-referenced `cmd/`, `internal/`, `pkg/` structure. Explicitly NOT an official standard; its own maintainers and the broader community note it is not endorsed by the Go team, and a counter-project exists arguing against treating it as a standard.
- **The Twelve-Factor App methodology** \u2014 for configuration from the environment, explicit dependency declaration, and processes as the unit of execution.
- **Go standard library conventions** \u2014 `context` propagation, `signal.NotifyContext`, `http.Server.Shutdown`, `log/slog`.
- **Effective Go and Go Code Review Comments** \u2014 for idioms such as "accept interfaces, return structs" and small consumer-defined interfaces.

## Where the community genuinely disagrees

- **`pkg/` vs no `pkg/`** \u2014 some experienced Go developers consider `pkg/` unnecessary nesting. This skill makes `pkg/` a **MAY**, used only when reuse is real.
- **Flat vs structured** \u2014 for small projects, a flat layout (everything in the root package) is idiomatic and preferred by many. This skill says: start flat, add structure as the project grows. Over-structuring a small service is a real anti-pattern.
- **`internal/` by domain vs by layer** \u2014 both are used in practice. This skill recommends domain grouping as a **SHOULD**, not a **MUST**, because layer grouping is defensible for some teams.
- **DI containers** \u2014 contested. This skill treats manual wiring as the default and containers as a **MAY** for large graphs.

## Why "conventions, not standards" matters here

The user's stated principle was to use official standards where they exist and to clearly state when something is not a set standard. For Go project layout, there is no official standard \u2014 so every rule in this skill is a team convention. Adopt, adapt, or override per project. The value is consistency within an organisation, not conformance to an external authority.

## Relationship to BFSI skills

This skill is structural and security-neutral. Where a BFSI service is involved, `bfsi-india-core` and `golang-bfsi-bindings` govern regulatory and security requirements. Nothing here overrides them.
