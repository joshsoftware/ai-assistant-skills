# D. Request Handling & Validation

Expands rules D1\u2013D5.

## D1 \u2014 Typed decoding with limits [MUST]

```go
func (h *Handler) CreatePayment(w http.ResponseWriter, r *http.Request) {
    r.Body = http.MaxBytesReader(w, r.Body, 1<<20) // 1 MiB cap

    var req CreatePaymentRequest
    dec := json.NewDecoder(r.Body)
    dec.DisallowUnknownFields()
    if err := dec.Decode(&req); err != nil {
        writeError(w, r, ErrBadRequest("malformed request body"))
        return
    }
    // ... validate, then act ...
}
```

- `http.MaxBytesReader` caps body size \u2014 without it, a client can stream an unbounded body.
- `DisallowUnknownFields` rejects drift (rule C5).
- Decode into a typed struct, never a `map[string]any`.

## D2 \u2014 Server-side allow-list validation [MUST]

```go
func (r CreatePaymentRequest) Validate() error {
    var errs []string
    if r.Amount == "" {
        errs = append(errs, "amount is required")
    }
    if !validCurrency(r.Currency) { // allow-list check
        errs = append(errs, "currency must be one of INR, USD, EUR")
    }
    if len(r.Reference) > 64 {
        errs = append(errs, "reference exceeds 64 characters")
    }
    if len(errs) > 0 {
        return ValidationError(errs)
    }
    return nil
}
```

Validate type, range, length, and format against an allow-list (what is permitted), not a denylist (what is known-bad). Validation is server-side; client-side validation is purely for user experience. For BFSI input rules see `golang-bfsi-bindings` category C.

## D3 \u2014 Context propagation [MUST]

```go
func (h *Handler) CreatePayment(w http.ResponseWriter, r *http.Request) {
    ctx := r.Context()
    // every downstream call takes ctx
    result, err := h.svc.CreatePayment(ctx, req)
    // ...
}
```

`r.Context()` is cancelled when the client disconnects. Threading it through service \u2192 repository \u2192 DB call means a disconnected client's work is abandoned promptly instead of running to completion uselessly.

## D4 \u2014 Thin handlers [SHOULD]

A handler does four things: decode, validate, call the service, encode the response.

```go
func (h *Handler) CreatePayment(w http.ResponseWriter, r *http.Request) {
    var req CreatePaymentRequest
    if err := decode(w, r, &req); err != nil {
        writeError(w, r, err); return
    }
    if err := req.Validate(); err != nil {
        writeError(w, r, err); return
    }
    out, err := h.svc.CreatePayment(r.Context(), req.toDomain())
    if err != nil {
        writeError(w, r, err); return
    }
    writeJSON(w, http.StatusCreated, toPaymentResponse(out))
}
```

Business rules \u2014 limits, risk checks, ledger postings \u2014 live in `h.svc`, not in the handler. A handler stuffed with business logic cannot be tested without spinning up HTTP, and the logic cannot be reused by a non-HTTP caller (a worker, a CLI).

## D5 \u2014 Per-request timeout [SHOULD]

```go
func TimeoutMiddleware(d time.Duration) func(http.Handler) http.Handler {
    return func(next http.Handler) http.Handler {
        return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
            ctx, cancel := context.WithTimeout(r.Context(), d)
            defer cancel()
            next.ServeHTTP(w, r.WithContext(ctx))
        })
    }
}
```

A bounded request lifetime stops one slow request (slow DB query, slow upstream) from occupying a worker indefinitely. Tune the duration per route class.

## Common findings

1. No `MaxBytesReader` \u2014 unbounded request body.
2. Decoding into `map[string]any` instead of a typed struct.
3. Validation done only client-side, or only partially server-side.
4. `context.Background()` used in a handler instead of `r.Context()`, so client disconnect does not cancel work.
5. Handlers hundreds of lines long with business logic inline.
6. No per-request timeout \u2014 a slow upstream ties up workers.
