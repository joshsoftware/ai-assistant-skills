# A. godoc Comments

Expands rules A1–A6. The formatting rules here are as close to "official" as Go documentation gets — they are enforced by `go doc`, `pkg.go.dev`, and the `go/doc` package.

## A1 + A2 — Every exported symbol, starting with the name [MUST]

```go
// Package payments provides services for initiating, tracking, and reconciling
// financial payments in compliance with RBI Digital Payment Security Controls.
package payments

// Payment represents a single financial payment intent.
// It is immutable after creation; amendments create a new Payment.
type Payment struct {
    ID       string
    Amount   money.Amount
    Status   PaymentStatus
}

// NewPayment creates a new Payment with the given amount and beneficiary.
// The returned Payment has status Pending.
// Returns ErrInvalidAmount if amount.Minor <= 0.
// Returns ErrInvalidCurrency if amount.Currency is not supported.
func NewPayment(amount money.Amount, beneficiary AccountID) (Payment, error) { ... }

// Validate checks all fields of the Payment for consistency.
// It is safe for concurrent use.
func (p *Payment) Validate() error { ... }

// ErrInvalidAmount is returned by NewPayment when the amount minor units are <= 0.
var ErrInvalidAmount = errors.New("amount must be positive")
```

Rules embodied:
- **Package comment** starts with "Package payments".
- **Type comment** starts with the type name.
- **Function comment** starts with the function name.
- **Error variable** has a comment explaining when it is returned.

## A3 — Package comments for large packages [MUST]

For a package with many files, put the package comment in `doc.go`:

```go
// Package payments provides services for initiating, tracking, and reconciling
// financial payments in compliance with RBI Digital Payment Security Controls,
// 2021.
//
// The core types are Payment, Ledger, and Reconciler. Payments flow through
// the following states:
//
//	Pending → Processing → Completed
//	Pending → Processing → Failed
//	Completed → Reversed
//
// # Security considerations
//
// All payment creation endpoints require an Idempotency-Key header.
// PII fields on Payment are masked by default; see money.Account.
package payments
```

Go 1.19+ supports formatted doc comments with headers (`# Title`), code blocks (indented), and lists. Use them for complex packages.

## A4 — Examples [SHOULD]

```go
// In payment_example_test.go
func ExampleNewPayment() {
    amount := money.MustParse("100.00", "INR")
    p, err := NewPayment(amount, AccountID("acc_123"))
    if err != nil {
        log.Fatal(err)
    }
    fmt.Println(p.Status)
    // Output: Pending
}
```

Examples are compiled by `go test` and verified against their `// Output:` comment — they cannot silently go stale the way prose documentation can.

## A5 — Document error returns [SHOULD]

```go
// FindByID retrieves the Payment with the given ID.
//
// Returns ErrNotFound if no payment with that ID exists or if the caller
// is not entitled to access it (to prevent information disclosure).
// Returns ErrDatabase on a transient database failure — the caller should retry.
func (r *Repository) FindByID(ctx context.Context, id string) (Payment, error) { ... }
```

Callers need to know the difference between `ErrNotFound` (don't retry) and `ErrDatabase` (might retry). Without documentation, they resort to string-matching on `err.Error()`.

## A6 — Document concurrency [SHOULD]

```go
// LedgerStore is safe for concurrent use by multiple goroutines.
// All operations acquire an internal mutex.
type LedgerStore struct { ... }

// RateLimiter is NOT safe for concurrent use without external synchronisation.
type RateLimiter struct { ... }
```

Go users expect types in the standard library to say "safe for concurrent use" when they are. Apply the same convention to your own types.

## Common findings

1. Exported function with no doc comment — `go vet` warns; `pkg.go.dev` shows a blank.
2. Comment does not start with the symbol name: `// Creates a payment` instead of `// NewPayment creates...`.
3. Error variables undocumented — callers use `errors.Is` without knowing what they're checking for.
4. No package comment at all on a public package.
5. Concurrency safety not documented — callers do not know whether to add a mutex.
