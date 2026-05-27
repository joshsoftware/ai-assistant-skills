# B. Table-Driven Tests

Expands rules B1–B5. Table-driven tests are the idiomatic Go approach for testing functions with multiple input cases.

## B1–B3 — The canonical pattern [SHOULD/MUST]

```go
func TestNewPayment(t *testing.T) {
    tests := map[string]struct {   // B5: map randomises iteration order
        amount   string
        currency string
        wantErr  error
    }{
        "valid INR payment":        {amount: "100.00", currency: "INR", wantErr: nil},
        "zero amount":              {amount: "0",      currency: "INR", wantErr: ErrInvalidAmount},
        "negative amount":          {amount: "-5.00",  currency: "INR", wantErr: ErrInvalidAmount},
        "unsupported currency":     {amount: "100.00", currency: "XYZ", wantErr: ErrInvalidCurrency},
        "empty currency":           {amount: "100.00", currency: "",    wantErr: ErrInvalidCurrency},
    }

    for name, tc := range tests {
        tc := tc // B4: capture loop variable before t.Run
        t.Run(name, func(t *testing.T) {
            t.Parallel() // run subtests concurrently where safe
            amt, err := money.Parse(tc.amount, tc.currency)
            if err != nil {
                t.Fatalf("money.Parse: %v", err)
            }
            _, gotErr := NewPayment(amt)
            if !errors.Is(gotErr, tc.wantErr) {
                t.Errorf("got error %v, want %v", gotErr, tc.wantErr)
            }
        })
    }
}
```

Key points:
- **B2:** Names like `"zero amount"` appear in failure output: `--- FAIL: TestNewPayment/zero_amount`. Immediately actionable.
- **B3:** `t.Run` creates a named subtest. Run a single case: `go test -run TestNewPayment/zero_amount`.
- **B4:** The loop-variable capture (`tc := tc`) is critical when subtests run in parallel. Without it, all goroutines share the same `tc` and read the last value. (Go 1.22+ fixes this at the language level, but capturing is still the portable pattern.)
- **B5:** `map[string]struct{...}` randomises order, exposing any hidden ordering dependency.

## When NOT to use a table

- When setup and teardown differ significantly per case.
- When there is only one case — a regular test function is cleaner.
- When each case has a completely different assertion shape.

## Common findings

1. Slice instead of map for test cases — tests always run in the same order, hiding ordering dependencies.
2. No descriptive name on each case — `{input: 0, want: err}` is meaningless in failure output.
3. `t.Run` missing — all cases share one test name; one failure obscures the rest.
4. Loop variable not captured before `t.Parallel()` — all subtests read the last loop value (Go < 1.22).
5. Table grown to 30+ cases that all test the same thing — split into focused tables.
