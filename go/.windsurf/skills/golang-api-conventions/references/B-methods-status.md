# B. Methods, Status Codes & Responses

Expands rules B1\u2013B7.

## B1 + B2 \u2014 HTTP method semantics [MUST]

| Method | Use | Safe | Idempotent |
|---|---|---|---|
| GET | Read a resource or collection | Yes | Yes |
| POST | Create a resource, or a non-idempotent action | No | No |
| PUT | Replace a resource entirely | No | Yes |
| PATCH | Partially update a resource | No | No* |
| DELETE | Remove a resource | No | Yes |

*PATCH can be made idempotent depending on the patch semantics; do not assume it.

`GET` is **safe** \u2014 it must never change state. A `GET` that creates, updates, or triggers a side effect is a defect (it will be retried by caches, prefetchers, and crawlers).

`PUT` and `DELETE` are **idempotent** \u2014 calling them twice has the same effect as once. Clients and proxies may retry them.

## B3 \u2014 Accurate status codes [MUST]

Success:
- `200 OK` \u2014 successful GET/PATCH/PUT with a body
- `201 Created` \u2014 resource created; include a `Location` header pointing to it
- `202 Accepted` \u2014 request accepted for async processing; not yet complete
- `204 No Content` \u2014 success with no body (e.g. DELETE)

Client errors:
- `400 Bad Request` \u2014 malformed request (bad JSON, missing required field)
- `401 Unauthorized` \u2014 missing or invalid authentication
- `403 Forbidden` \u2014 authenticated but not permitted
- `404 Not Found` \u2014 resource does not exist (or caller not entitled \u2014 see note)
- `409 Conflict` \u2014 conflicts with current state (e.g. duplicate, version mismatch)
- `422 Unprocessable Entity` \u2014 syntactically valid but semantically invalid
- `429 Too Many Requests` \u2014 rate limit exceeded

Server errors:
- `500 Internal Server Error` \u2014 unexpected server fault
- `503 Service Unavailable` \u2014 dependency down, shedding load, shutting down

Note on 403 vs 404: for resources exposing customer data, returning `404` for "exists but you are not entitled" avoids leaking the existence of the resource. Be consistent (see `golang-bfsi-bindings` rule B5).

## B4 \u2014 JSON responses [MUST]

```go
func writeJSON(w http.ResponseWriter, status int, v any) {
    w.Header().Set("Content-Type", "application/json")
    w.WriteHeader(status)
    if err := json.NewEncoder(w).Encode(v); err != nil {
        // log; response is already partially written
    }
}
```

Never build JSON with `fmt.Sprintf`. `encoding/json` handles escaping correctly.

## B5 \u2014 Dedicated response types [SHOULD]

```go
// internal domain type \u2014 has fields the client must not see
type Payment struct {
    ID           string
    Amount       Money
    InternalRisk float64   // internal only
    LedgerRef    string    // internal only
}

// response type \u2014 only what the client gets
type PaymentResponse struct {
    ID     string `json:"id"`
    Amount string `json:"amount"`
    Status string `json:"status"`
}

func toPaymentResponse(p Payment) PaymentResponse { /* map fields */ }
```

Serialising the domain struct directly risks leaking `InternalRisk`, `LedgerRef`, and anything added later. A dedicated response type makes the public shape explicit and stable. (This also intersects with PII handling \u2014 see `golang-bfsi-bindings` data rules.)

## B6 \u2014 Consistent envelopes [SHOULD]

Collection:
```json
{
  "data": [ {...}, {...} ],
  "pagination": { "next_cursor": "...", "has_more": true }
}
```

Single resource: return the object directly, or wrap it as `{"data": {...}}` \u2014 but be consistent across the whole API. Mixing bare objects and wrapped objects forces clients to special-case endpoints.

## B7 \u2014 Idempotency keys on financial endpoints [MUST]

A `POST /payments` (non-idempotent by HTTP semantics) that moves money must accept an `Idempotency-Key` header so a client retry after a network failure does not create a second payment. This is an API-design decision; the mechanism is detailed in `golang-bfsi-bindings` rules H6 and L2.

## Common findings

1. State-changing `GET` endpoints.
2. `200 OK` returned for everything, including errors (error signalled only in the body).
3. `500` returned for client errors that should be `400`/`409`/`422`.
4. `201 Created` without a `Location` header.
5. Internal domain structs serialised directly, leaking internal fields.
6. Inconsistent response envelopes across endpoints.
7. `POST /payments` with no idempotency key.
