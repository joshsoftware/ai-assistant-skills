---
name: golang-testing-conventions
description: Go unit testing conventions — table-driven tests, subtests, test helpers, fakes vs mocks, test organisation, the race detector, benchmarks, fuzz tests, integration test separation, and coverage. Use whenever Go test files are being written or reviewed, or when a testing strategy is being designed. Activate on mentions of testing.T, t.Run, table-driven, testify, gomock, mockery, go test, -race, benchmark, fuzz, integration test, test coverage, or "how do I test this in Go". These are engineering conventions built on Go standard library idioms — the testing package and table-driven approach are idiomatic Go, effectively community-standard. The race detector is part of the official Go toolchain.
---

# Go Testing Conventions

Conventions for testing Go services. **These are conventions.** The Go standard library's `testing` package and the table-driven test pattern are idiomatic enough to be effectively community-standard. The race detector (`-race`) is part of the official Go toolchain.

## How to use this skill

1. Walk the rule categories below when writing or reviewing test code.
2. **MUST** violations are blockers. **SHOULD** violations need a documented reason.
3. For BFSI services: the race detector is not optional — financial data races are correctness defects, not performance hints.

## Sources

- **Go standard library `testing` package** — the canonical Go testing mechanism. Not a formal standard but authoritative by virtue of being the toolchain.
- **Go Code Review Comments** — includes table-driven test guidance.
- **`golang.org/x/sync/errgroup`**, `go test -fuzz`, `go test -bench` — official Go toolchain features.
- **testify** and **gomock/mockery** — widely-used third-party libraries.

Full notes: `references/sources-and-rationale.md`.

## Rule categories

| # | Category | Reference file |
|---|---|---|
| A | Test structure & organisation | `references/A-structure.md` |
| B | Table-driven tests | `references/B-table-driven.md` |
| C | Fakes, mocks & test doubles | `references/C-fakes-mocks.md` |
| D | Assertions & error handling in tests | `references/D-assertions.md` |
| E | Race detector, benchmarks & fuzz | `references/E-race-bench-fuzz.md` |
| F | Integration & database tests | `references/F-integration.md` |

---

## Rule index

### A. Test structure & organisation

- **A1 [MUST]** Test files are named `<source>_test.go` and live in the same package (white-box) or a `_test` suffix package (black-box). Use the black-box form (`package payment_test`) for public API tests; white-box for internals that need access.
- **A2 [MUST]** Test function names follow `TestFunctionName_Scenario` or `TestFunctionName` for single-case tests. The name must explain what is being tested, not just which function.
- **A3 [MUST]** Tests are independent — each test arranges its own state and does not rely on execution order or shared mutable state from previous tests.
- **A4 [SHOULD]** Use `t.Helper()` in test helper functions so failure messages report the caller's line, not the helper's line.
- **A5 [SHOULD]** Use `t.Cleanup(func())` to register teardown rather than `defer` in loops — `defer` in a loop fires at end of function, not end of iteration.
- **A6 [SHOULD]** Separate integration tests from unit tests using a build tag or a flag check (`testing.Short()`), so `go test ./...` runs only fast, dependency-free tests by default.

### B. Table-driven tests

- **B1 [SHOULD]** Table-driven tests are the default pattern for functions with multiple input/output cases. They eliminate repetition and make it easy to add cases.
- **B2 [MUST]** Each test case in a table has a descriptive name (`name` field). The name must make a failing test's output self-explanatory without reading the table.
- **B3 [MUST]** Use `t.Run(tc.name, func(t *testing.T) {...})` for each table row. This produces named subtests that are individually runnable and filterable.
- **B4 [MUST]** When running table-driven subtests in parallel (`t.Parallel()`), capture the loop variable inside the subtest to prevent the well-known closure capture bug.
- **B5 [SHOULD]** Prefer a `map[string]struct{...}` over a `[]struct{...}` for test case tables — maps randomise iteration order, which prevents tests from silently depending on order.

### C. Fakes, mocks & test doubles

- **C1 [SHOULD]** Prefer hand-written fakes over generated mocks for simple interfaces. A fake is a simplified but real implementation; it is readable and does not assert call counts by default.
- **C2 [SHOULD]** Use generated mocks (gomock, mockery) when you need to assert specific call sequences, call counts, or argument matching.
- **C3 [MUST]** Test doubles are only for external dependencies (database, HTTP, message queue, file system). Do not mock your own domain types — test them directly.
- **C4 [MUST]** Interfaces used as seams for testing are defined in the consumer package and are as small as possible (one or two methods). A large interface is hard to fake and indicates the consumer needs too much.
- **C5 [SHOULD]** For HTTP dependencies, use `httptest.NewServer` rather than mocking the HTTP client. This tests the actual request-building and response-parsing code.

### D. Assertions & error handling in tests

- **D1 [SHOULD]** Use `t.Errorf` for non-fatal failures (record the failure, continue the test). Use `t.Fatalf` for fatal failures (stop the test immediately — used when continuing would panic or produce misleading output).
- **D2 [SHOULD]** Error messages follow `got X, want Y` convention: `t.Errorf("got %v, want %v", got, want)`. The message must be actionable without reading the test.
- **D3 [MAY]** Use `testify/assert` and `testify/require` for richer assertions and cleaner output. `require.NoError` is equivalent to `t.Fatalf` on error; `assert.Equal` is equivalent to `t.Errorf`.
- **D4 [MUST]** Never ignore errors in tests. Use `require.NoError(t, err)` or `if err != nil { t.Fatalf(...) }`. A test that silently swallows an error is testing nothing.

### E. Race detector, benchmarks & fuzz

- **E1 [MUST]** Run the test suite with `-race` in CI. For BFSI services, a data race in payment or ledger code is a correctness defect. The race detector is a first-class Go tool, not optional.
- **E2 [SHOULD]** Write benchmarks (`func BenchmarkXxx(b *testing.B)`) for performance-sensitive paths: hot cache lookups, payment processing loops, serialisation. Benchmark output is a regression baseline.
- **E3 [SHOULD]** Use `b.ReportAllocs()` in benchmarks to surface unexpected allocations. Unexpected allocations in a hot path are a signal of inefficiency.
- **E4 [MAY]** Use fuzz testing (`func FuzzXxx(f *testing.F)`) for parser inputs, deserialisation, and any function that processes untrusted external data. Fuzz testing finds edge cases table-driven tests miss.

### F. Integration & database tests

- **F1 [SHOULD]** Use `testcontainers-go` or Docker Compose for integration tests that need a real database or message broker. Do not use the shared development database for automated tests.
- **F2 [SHOULD]** Each integration test creates its own schema or uses a transaction that is rolled back at the end of the test (`t.Cleanup(func() { tx.Rollback(ctx) })`). Tests must not pollute each other's data.
- **F3 [SHOULD]** Use `testing.Short()` to skip integration tests in unit-test runs: `if testing.Short() { t.Skip("integration test") }`.
- **F4 [SHOULD]** For HTTP handler tests, use `httptest.NewRecorder()` and call the handler directly, without starting a real server. This is faster and more precise than an end-to-end HTTP call.

## Out of scope

- Load and performance testing — separate tooling (k6, locust).
- End-to-end browser/UI testing.
- Contract testing (Pact) — a separate practice layer.
