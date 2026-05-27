# E. Error Responses

Expands rules E1\u2013E5.

## E1 \u2014 One consistent error shape [MUST]

Every error response across the whole API uses the same JSON structure:

```json
{
  "error": {
    "code": "PAYMENT_DECLINED",
    "message": "The payment was declined by the issuing bank.",
    "correlation_id": "req_8f3a1c2e"
  }
}
```

Optionally, a `details` array for field-level validation errors:

```json
{
  "error": {
    "code": "VALIDATION_FAILED",
    "message": "One or more fields are invalid.",
    "correlation_id": "req_8f3a1c2e",
    "details": [
      { "field": "amount",   "issue": "must be greater than zero" },
      { "field": "currency", "issue": "must be one of INR, USD, EUR" }
    ]
  }
}
```

A consistent shape means every client writes one error-handling path, not one per endpoint.

## E2 \u2014 No internal detail in errors [MUST]

The error body never contains: stack traces, SQL fragments, ORM error text, driver errors, internal hostnames or IPs, file paths, framework or library versions. These leak architecture to an attacker and are useless to a legitimate client. Full internal detail is logged server-side only. (See `golang-bfsi-bindings` rule G1.)

## E3 \u2014 Correlation ID on every error [MUST]

The `correlation_id` in the response is the same ID written to the server logs for that request. A user reporting a problem quotes the ID; support finds the full server-side trace instantly. The ID is opaque and carries no sensitive data.

## E4 \u2014 Stable, documented error codes [SHOULD]

```go
const (
    CodeValidationFailed = "VALIDATION_FAILED"
    CodePaymentDeclined  = "PAYMENT_DECLINED"
    CodeLimitExceeded    = "LIMIT_EXCEEDED"
    CodeDuplicate        = "DUPLICATE_REQUEST"
    CodeNotFound         = "NOT_FOUND"
    CodeInternal         = "INTERNAL_ERROR"
)
```

`code` is a stable machine-readable string a client can branch on; it is part of the API contract and does not change within a major version. `message` is human-readable and may be reworded freely.

## E5 \u2014 Deliberate error-to-status mapping [MUST]

```go
func classify(err error) (status int, code string) {
    switch {
    case errors.Is(err, ErrValidation):
        return http.StatusBadRequest, CodeValidationFailed
    case errors.Is(err, ErrNotFound):
        return http.StatusNotFound, CodeNotFound
    case errors.Is(err, ErrDuplicate):
        return http.StatusConflict, CodeDuplicate
    case errors.Is(err, ErrLimitExceeded):
        return http.StatusUnprocessableEntity, CodeLimitExceeded
    default:
        return http.StatusInternalServerError, CodeInternal
    }
}
```

A central classifier maps domain errors (sentinels or typed errors, inspected with `errors.Is`/`errors.As`) to status codes. The default is `500` \u2014 but most errors are *not* `500`. Returning `500` for a validation failure or a duplicate is a defect: it tells the client (and monitoring) that the server is broken when it is not.

## Putting it together

```go
func writeError(w http.ResponseWriter, r *http.Request, err error) {
    corrID := correlationID(r.Context())
    status, code := classify(err)

    // full detail server-side only
    slog.ErrorContext(r.Context(), "request failed",
        slog.String("correlation_id", corrID),
        slog.String("code", code),
        slog.Any("error", err),
    )

    writeJSON(w, status, ErrorEnvelope{
        Error: ErrorBody{
            Code:          code,
            Message:       publicMessage(code),
            CorrelationID: corrID,
        },
    })
}
```

## Common findings

1. Different error shapes on different endpoints.
2. `err.Error()` written straight into the response body, leaking SQL/paths.
3. No correlation ID \u2014 a reported error cannot be traced.
4. Error `code` absent or unstable, so clients string-match on `message`.
5. `500` returned for validation, conflict, or not-found errors, distorting error metrics.
