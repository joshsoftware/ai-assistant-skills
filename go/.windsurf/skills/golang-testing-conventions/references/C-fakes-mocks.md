# C. Fakes, Mocks & Test Doubles

Expands rules C1–C5.

## C1 — Hand-written fakes [SHOULD]

```go
// The interface (defined in the consumer — payment/service.go)
type PaymentRepository interface {
    Save(ctx context.Context, p Payment) error
    FindByID(ctx context.Context, id string) (Payment, error)
}

// The fake (defined in payment/service_test.go or a testutil package)
type fakeRepo struct {
    payments map[string]Payment
    saveErr  error // inject errors for specific test cases
}

func newFakeRepo() *fakeRepo {
    return &fakeRepo{payments: make(map[string]Payment)}
}

func (f *fakeRepo) Save(_ context.Context, p Payment) error {
    if f.saveErr != nil { return f.saveErr }
    f.payments[p.ID] = p
    return nil
}

func (f *fakeRepo) FindByID(_ context.Context, id string) (Payment, error) {
    p, ok := f.payments[id]
    if !ok { return Payment{}, ErrNotFound }
    return p, nil
}
```

The fake is a real, if simplified, implementation. It does not assert "was Save called once with this exact argument?" — it just stores what is given. That makes tests less brittle: a refactor that calls Save twice does not break the test unless the test cares about the outcome.

## C2 — Generated mocks for call-sequence assertions [SHOULD]

Use gomock or mockery when you genuinely need to assert that a function was called exactly once, with specific arguments, in a specific order. This is appropriate for side-effect-only interfaces (an email sender, an audit emitter) where the call itself is the observable behaviour.

```go
ctrl := gomock.NewController(t)
mockAudit := NewMockAuditEmitter(ctrl)
mockAudit.EXPECT().
    Emit(gomock.Any(), gomock.Eq(AuditEvent{Action: "PAYMENT_CREATED"})).
    Times(1)
```

Use mocks sparingly. Over-mocking produces tests that specify implementation details instead of behaviour — they break on every refactor.

## C5 — httptest.NewServer for HTTP dependencies [SHOULD]

```go
func TestPaymentClient_Create(t *testing.T) {
    srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
        // verify the request shape
        assert.Equal(t, "POST", r.Method)
        assert.Equal(t, "/v1/payments", r.URL.Path)
        assert.NotEmpty(t, r.Header.Get("Idempotency-Key"))

        w.Header().Set("Content-Type", "application/json")
        w.WriteHeader(http.StatusCreated)
        json.NewEncoder(w).Encode(Payment{ID: "pay_123", Status: "pending"})
    }))
    defer srv.Close()

    client := NewPaymentClient(srv.URL, http.DefaultClient)
    p, err := client.Create(context.Background(), CreateRequest{...})
    // ...
}
```

`httptest.NewServer` binds a real TCP port and runs a real handler. The client code is tested exactly as it runs in production — including header construction, JSON encoding, and response parsing. A mock `http.Client` would skip all that.

---

# D. Assertions & Error Handling in Tests

Expands rules D1–D4.

## D1 + D2 — t.Errorf vs t.Fatalf [SHOULD/MUST]

```go
// t.Errorf — non-fatal: records failure, test continues
t.Errorf("got status %d, want %d", got, want)

// t.Fatalf — fatal: records failure, stops this test immediately
t.Fatalf("failed to parse config: %v", err) // continuing would panic on nil cfg
```

Use `t.Fatalf` when continuing would cause a panic or produce misleading output. Use `t.Errorf` otherwise — collecting multiple failures per test is more informative.

Error message convention: `"got %v, want %v"` or `"got %v, want %v (case: %s)"`.

## D3 — testify [MAY]

```go
import (
    "github.com/stretchr/testify/assert"  // non-fatal
    "github.com/stretchr/testify/require" // fatal (like t.Fatalf)
)

func TestService_CreatePayment(t *testing.T) {
    p, err := svc.Create(ctx, req)
    require.NoError(t, err)          // stop immediately if err != nil
    assert.Equal(t, "pending", p.Status)
    assert.NotEmpty(t, p.ID)
}
```

`require` stops the test on failure; `assert` records and continues. This mirrors `t.Fatalf` / `t.Errorf`. Testify is optional but reduces boilerplate; the standard library is always sufficient.

## D4 — Never ignore errors [MUST]

```go
// WRONG — silent swallow
p, _ := svc.Create(ctx, req)
assert.Equal(t, "pending", p.Status) // tests the zero-value Payment if Create failed

// RIGHT
p, err := svc.Create(ctx, req)
require.NoError(t, err)
assert.Equal(t, "pending", p.Status)
```

A test with a swallowed error is a false positive — it passes when the function under test returned an error and a zero-value result.

---

# E. Race Detector, Benchmarks & Fuzz

Expands rules E1–E4.

## E1 — Race detector in CI [MUST]

```bash
# In CI
go test -race ./...

# For BFSI services: run with race detector as a non-optional gate
# A data race on a balance counter or an idempotency store is a correctness defect
```

The race detector adds ~2× overhead; that is acceptable for a CI gate. It does not add overhead to production binaries. There is no reason not to run it.

## E2 + E3 — Benchmarks [SHOULD]

```go
func BenchmarkPaymentSerialise(b *testing.B) {
    b.ReportAllocs() // surface allocations per op
    p := Payment{ID: "pay_123", Amount: money.MustParse("100.00", "INR")}
    b.ResetTimer()
    for i := 0; i < b.N; i++ {
        _, err := json.Marshal(p)
        if err != nil {
            b.Fatal(err)
        }
    }
}
```

Run with `go test -bench=. -benchmem ./...`. Commit the output as a baseline; compare on PRs that touch hot paths.

## E4 — Fuzz testing [MAY]

```go
func FuzzParseAmount(f *testing.F) {
    // Seed corpus: known valid and boundary inputs
    f.Add("100.00", "INR")
    f.Add("0.01",   "INR")
    f.Add("0",      "INR")

    f.Fuzz(func(t *testing.T, amount, currency string) {
        // Must not panic regardless of input
        result, err := money.Parse(amount, currency)
        if err == nil {
            // If it parsed successfully, it must round-trip
            if result.Minor < 0 {
                t.Errorf("parsed a negative minor: %d", result.Minor)
            }
        }
    })
}
```

Run continuously: `go test -fuzz=FuzzParseAmount -fuzztime=60s`. Findings are saved as corpus files and re-run on every `go test` run.

---

# F. Integration & Database Tests

Expands rules F1–F4.

## F1 + F2 — Isolated database tests [SHOULD]

Using a transaction that rolls back:

```go
func TestPaymentRepository_Save(t *testing.T) {
    if testing.Short() { t.Skip("integration test") }

    db := testDB(t) // creates or borrows a test DB, registers t.Cleanup(db.Close)

    tx, err := db.BeginTx(context.Background(), nil)
    require.NoError(t, err)
    t.Cleanup(func() { tx.Rollback(context.Background()) }) // always rolls back

    repo := NewRepository(tx)
    p := Payment{ID: "pay_test_" + randomSuffix(), ...}

    err = repo.Save(context.Background(), p)
    require.NoError(t, err)

    got, err := repo.FindByID(context.Background(), p.ID)
    require.NoError(t, err)
    assert.Equal(t, p.ID, got.ID)
    // Rollback fires via t.Cleanup — no data left in DB
}
```

Using testcontainers (clean container per test run):

```go
func testDB(t *testing.T) *sql.DB {
    t.Helper()
    ctx := context.Background()
    pg, err := testcontainers.GenericContainer(ctx, testcontainers.GenericContainerRequest{
        ContainerRequest: testcontainers.ContainerRequest{
            Image:        "postgres:16-alpine",
            ExposedPorts: []string{"5432/tcp"},
            Env:          map[string]string{"POSTGRES_PASSWORD": "test"},
            WaitingFor:   wait.ForListeningPort("5432/tcp"),
        },
        Started: true,
    })
    require.NoError(t, err)
    t.Cleanup(func() { pg.Terminate(ctx) })
    // ... build DSN, migrate, return *sql.DB
}
```

## F4 — HTTP handler tests without a server [SHOULD]

```go
func TestPaymentHandler_Create(t *testing.T) {
    svc := &fakeService{}
    h := NewHandler(svc, slog.Default())

    body := `{"amount":"100.00","currency":"INR"}`
    req := httptest.NewRequest(http.MethodPost, "/v1/payments", strings.NewReader(body))
    req.Header.Set("Content-Type", "application/json")
    req.Header.Set("Idempotency-Key", "test-key-001")
    w := httptest.NewRecorder()

    h.CreatePayment(w, req)

    res := w.Result()
    assert.Equal(t, http.StatusCreated, res.StatusCode)
    // decode and assert response body...
}
```

No real server started. The handler function is called directly, which is faster, more precise, and avoids binding a port.

## Common findings

1. Integration and unit tests mixed — `go test ./...` takes minutes, developers skip it.
2. Integration tests sharing a global database table — tests pollute each other's data.
3. Benchmarks without `b.ReportAllocs()` — allocations invisible.
4. Race detector not run in CI — data races in concurrent payment code reach production.
5. HTTP handler tests using a full HTTP server when `httptest.NewRecorder` would suffice.
