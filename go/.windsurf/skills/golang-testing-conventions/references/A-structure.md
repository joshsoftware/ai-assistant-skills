# A. Test Structure & Organisation

Expands rules A1–A6.

## A1 — File naming and package choice [MUST]

```
payment/
├── service.go
├── service_test.go         ← white-box: package payment (can access unexported fields)
├── handler.go
└── handler_test.go         ← black-box: package payment_test (tests public API only)
```

Use black-box (`package payment_test`) for testing the public API — it ensures your tests only use what consumers can see, catching awkward exports. Use white-box (`package payment`) when a test genuinely needs to reach unexported helpers, but prefer black-box by default.

## A2 — Test naming [MUST]

```go
// Single case
func TestNewPayment(t *testing.T) { ... }

// Multi-case function: FunctionName_Scenario
func TestCreatePayment_DuplicateKey(t *testing.T) { ... }
func TestCreatePayment_InsufficientFunds(t *testing.T) { ... }

// Method: TypeName_MethodName
func TestService_CreatePayment(t *testing.T) { ... }
```

The test name appears in failure output. `TestCreatePayment_DuplicateKey failed` is actionable; `TestCreatePayment3 failed` is not.

## A3 — Test independence [MUST]

```go
// WRONG — second test depends on state from first
var sharedDB *sql.DB

func TestA(t *testing.T) { sharedDB.Exec("INSERT ...") }
func TestB(t *testing.T) { sharedDB.Query("SELECT ...") } // depends on TestA having run

// RIGHT — each test arranges its own state
func TestB(t *testing.T) {
    db := setupTestDB(t) // creates a fresh DB or uses a transaction
    db.Exec("INSERT ...") // sets up what THIS test needs
    // ...
}
```

Tests run in declaration order by default but `-shuffle=on` randomises them. A test that only passes in a specific order is a hidden bug.

## A4 — t.Helper() in helpers [SHOULD]

```go
func requireNoError(t *testing.T, err error) {
    t.Helper() // without this, failure lines point here, not to the caller
    if err != nil {
        t.Fatalf("unexpected error: %v", err)
    }
}
```

Without `t.Helper()`, a failure in `requireNoError` reports the line inside the helper. With it, the failure reports the line that called `requireNoError` — which is where the problem actually is.

## A5 — t.Cleanup vs defer in loops [SHOULD]

```go
// WRONG — defer in a loop fires at end of function, not end of iteration
for _, tc := range cases {
    tmp, _ := os.CreateTemp("", "test*")
    defer os.Remove(tmp.Name()) // all cleanup deferred until after the loop
    // ...
}

// RIGHT — t.Cleanup fires at end of the subtest
for _, tc := range cases {
    tc := tc
    t.Run(tc.name, func(t *testing.T) {
        tmp, _ := os.CreateTemp("", "test*")
        t.Cleanup(func() { os.Remove(tmp.Name()) })
        // ...
    })
}
```

## A6 — Separate integration tests [SHOULD]

Two approaches:

```go
// Build tag approach — run with: go test -tags=integration ./...
//go:build integration

package payment_test

func TestCreatePayment_Postgres(t *testing.T) { ... }
```

```go
// testing.Short() approach — skip with: go test -short ./...
func TestCreatePayment_Postgres(t *testing.T) {
    if testing.Short() {
        t.Skip("skipping integration test in short mode")
    }
    // ...
}
```

Default CI gate: `go test ./...` (unit only, fast). Full CI gate: `go test -tags=integration ./...` (with Docker Compose / testcontainers).

## Common findings

1. Test names like `Test1`, `TestCase2` — failure output is unreadable.
2. Tests sharing a global `*sql.DB` that accumulates state across tests.
3. A test helper that does `t.Fatal` but forgot `t.Helper()` — failure line points to wrong place.
4. Integration tests mixed in with unit tests, making `go test ./...` slow or broken in CI.
