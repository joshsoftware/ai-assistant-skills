# Sources & Rationale

## What is genuinely standard here

- **`testing` package** — part of the Go standard library. The `TestXxx`, `BenchmarkXxx`, `FuzzXxx` naming conventions and `testing.T`, `testing.B`, `testing.F` types are authoritative by virtue of being the toolchain.
- **`go test -race`** — the race detector is a first-class Go toolchain feature. Not using it in CI is a deliberate choice that must be justified.
- **Table-driven test pattern** — documented in the Go Code Review Comments and widely endorsed by the Go team. Not a formal standard, but effectively community-standard.

## What is convention

- **testify vs standard library** — contested. The Go standard library is sufficient for everything; testify reduces boilerplate. The skill makes testify a MAY, not a MUST.
- **gomock vs mockery vs hand-written fakes** — no consensus. The skill recommends hand-written fakes for simple cases and generated mocks for call-sequence assertions, with a note to use mocks sparingly.
- **testcontainers vs Docker Compose for integration tests** — both are used in practice. The skill is tooling-agnostic; the principle (isolated, real dependency) is the rule.
- **Black-box vs white-box test packages** — both are idiomatic Go. The skill recommends black-box as a default with white-box where necessary.

## BFSI note

The race detector rule (E1) is elevated to **MUST** for BFSI services specifically. A data race on a balance counter, an idempotency store, or a session map is not a performance hint — it is a correctness defect that can produce duplicate debits, double credits, or authentication bypasses. This elevation is not from a regulator document; it is an engineering judgment about the consequences of data races in financial code.
