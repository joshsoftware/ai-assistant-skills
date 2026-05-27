# G. Middleware & Cross-Cutting Concerns

Expands rules G1\u2013G5.

## G1 \u2014 Cross-cutting concerns as middleware [SHOULD]

Anything that applies to many handlers \u2014 logging, correlation IDs, panic recovery, authentication, timeouts, rate limiting \u2014 is middleware, not code repeated in each handler.

```go
type Middleware func(http.Handler) http.Handler

func Chain(h http.Handler, mw ...Middleware) http.Handler {
    for i := len(mw) - 1; i >= 0; i-- {
        h = mw[i](h)
    }
    return h
}

handler := Chain(businessHandler,
    RecoverMiddleware,      // outermost: catches panics from everything inside
    CorrelationMiddleware,  // assigns an ID before logging needs it
    LoggingMiddleware,
    TimeoutMiddleware(8*time.Second),
    RateLimitMiddleware,
    AuthMiddleware,         // innermost of the cross-cutting set, before business logic
)
```

Order matters. Recovery is outermost so it catches panics from every other layer. Correlation runs early so the ID is available to logging and to error responses.

## G2 \u2014 Panic-recovery middleware [MUST]

```go
func RecoverMiddleware(next http.Handler) http.Handler {
    return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
        defer func() {
            if rec := recover(); rec != nil {
                slog.ErrorContext(r.Context(), "panic recovered",
                    slog.Any("panic", rec),
                    slog.String("correlation_id", correlationID(r.Context())),
                )
                writeJSON(w, http.StatusInternalServerError, genericError(r))
            }
        }()
        next.ServeHTTP(w, r)
    })
}
```

Without this, a nil-pointer dereference in one handler crashes the whole process \u2014 taking down every other in-flight request. For a BFSI service that can mean abandoned transactions. (See `golang-bfsi-bindings` rule G \u2014 panic discipline. Note: goroutines launched *inside* a handler need their own `recover`; middleware only covers the handler goroutine.)

## G3 \u2014 Correlation-ID middleware [MUST]

```go
func CorrelationMiddleware(next http.Handler) http.Handler {
    return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
        id := r.Header.Get("X-Correlation-ID")
        if id == "" {
            id = newCorrelationID()
        }
        ctx := withCorrelationID(r.Context(), id)
        w.Header().Set("X-Correlation-ID", id)
        next.ServeHTTP(w, r.WithContext(ctx))
    })
}
```

The ID is propagated from the inbound header if present (so a trace spans services), or generated. It goes into the context (for logging and error responses) and the response header (so the caller can record it).

## G4 \u2014 Rate limiting at the edge [SHOULD]

Customer-facing APIs apply rate limiting so a single client cannot overwhelm the service. Limit per client identity (API key, authenticated user) where possible, falling back to source IP. A rate-limited request gets `429 Too Many Requests`, ideally with a `Retry-After` header.

## G5 \u2014 Security middleware before business handlers [MUST]

Authentication and authorisation middleware runs before the business handler. The handler can then assume the caller is authenticated and carries a known identity in the context. Authorisation decisions are always made server-side. The mechanics \u2014 MFA, session handling, object-level authorisation \u2014 are governed by `golang-bfsi-bindings` categories A and B; this skill only fixes *where* the check sits in the request pipeline.

## Common findings

1. Logging / correlation / recovery code copy-pasted into each handler instead of middleware.
2. No panic-recovery middleware \u2014 one bad handler crashes the process.
3. Recovery middleware placed inside other middleware, so it misses their panics.
4. Correlation ID generated but not propagated from the inbound header, breaking cross-service traces.
5. No rate limiting on customer-facing endpoints.
6. Authorisation checks done inside business logic inconsistently rather than enforced uniformly before handlers.
