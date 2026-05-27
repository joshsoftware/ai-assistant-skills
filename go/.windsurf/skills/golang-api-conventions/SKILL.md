---
name: golang-api-conventions
description: Go HTTP/REST API design and implementation conventions for backend services and BFSI applications. Use whenever a Go HTTP API is being designed, built, or reviewed \u2014 endpoint design, routing, request/response shapes, status codes, versioning, pagination, filtering, error responses, middleware, content negotiation, or rate limiting. Activate on mentions of REST API, HTTP handler, net/http, ServeMux, router, endpoint, JSON request/response, API versioning, API errors, or "design this API" for a Go service. These are team engineering conventions synthesised from widely-accepted REST and Go community practice \u2014 they are NOT an official standard. Pair with golang-bfsi-bindings for BFSI security and regulatory requirements.
---

# Go API Conventions

Team conventions for designing and implementing HTTP/REST APIs in Go. **These are conventions, not a standard.** REST itself is an architectural style, not a spec; the rules here are widely-accepted community practice adapted to Go.

## How to use this skill

1. When designing or reviewing a Go HTTP API, walk the rule categories below.
2. Each rule has a severity (RFC 2119: **MUST**, **SHOULD**, **MAY**) and links to a `references/` file.
3. **MUST** violations are blockers. **SHOULD** violations need a documented reason.
4. Security-sensitive concerns (authn, authz, TLS, input validation) are summarised here but governed in depth by `golang-bfsi-bindings`.

## Sources (community practice, not standards)

- Widely-accepted REST design practice: resource-oriented URLs, correct HTTP verbs and status codes, versioning, pagination.
- Go standard library: `net/http`, `ServeMux` method-based routing (Go 1.22+), `encoding/json`, `context`.
- OpenAPI as the contract format.

Full notes: `references/sources-and-rationale.md`.

## Rule categories

| # | Category | Reference file |
|---|---|---|
| A | Resource design & URLs | `references/A-resource-design.md` |
| B | Methods, status codes & responses | `references/B-methods-status.md` |
| C | Versioning & compatibility | `references/C-versioning.md` |
| D | Request handling & validation | `references/D-request-handling.md` |
| E | Error responses | `references/E-error-responses.md` |
| F | Pagination, filtering & sorting | `references/F-pagination-filtering.md` |
| G | Middleware & cross-cutting concerns | `references/G-middleware.md` |

---

## Rule index

### A. Resource design & URLs

- **A1 [SHOULD]** URLs name resources with plural nouns: `/payments`, `/accounts/{id}`. Not verbs: avoid `/getPayment`, `/createAccount`.
- **A2 [SHOULD]** Express hierarchy through nesting only one level deep: `/accounts/{id}/transactions`. Deeper nesting becomes unwieldy; prefer query parameters or top-level resources.
- **A3 [SHOULD]** Use lowercase, hyphen-free or hyphen-separated path segments consistently. Pick one style for multi-word resources and keep it.
- **A4 [MUST]** Identifiers in paths are opaque to the client. Do not encode meaning a client could exploit; prefer non-sequential identifiers for resources exposing customer data (defence against enumeration \u2014 see `golang-bfsi-bindings` rule B5).
- **A5 [SHOULD]** Keep the API resource-oriented. Where an action genuinely does not map to a resource verb (e.g. "reverse a payment"), a sub-resource (`POST /payments/{id}/reversals`) is cleaner than an RPC-style verb in the path.

### B. Methods, status codes & responses

- **B1 [MUST]** Use HTTP methods per their semantics: `GET` (read, safe, idempotent), `POST` (create or non-idempotent action), `PUT` (full replace, idempotent), `PATCH` (partial update), `DELETE` (remove, idempotent).
- **B2 [MUST]** `GET` never changes state. A state-changing `GET` is a defect.
- **B3 [MUST]** Return accurate status codes: `200` success, `201` created (with `Location`), `202` accepted (async), `204` no content, `400` bad request, `401` unauthenticated, `403` unauthorised, `404` not found, `409` conflict, `422` unprocessable, `429` rate-limited, `500` server error, `503` unavailable.
- **B4 [MUST]** Responses are JSON with `Content-Type: application/json`. Construct JSON with `encoding/json`; never hand-concatenate.
- **B5 [SHOULD]** Response bodies are typed structs with explicit JSON tags. Do not serialise internal domain structs directly \u2014 use a dedicated response type so internal fields are never leaked (see also `golang-bfsi-bindings` data/PII rules).
- **B6 [SHOULD]** A successful collection response has a consistent envelope (e.g. `{"data": [...], "pagination": {...}}`); a single-resource response returns the resource object. Be consistent across the whole API.
- **B7 [MUST]** For state-changing endpoints crossing a payment/financial boundary, require an idempotency key (see `golang-bfsi-bindings` rule H6 / L2). This is a BFSI requirement, restated here because it is an API-design decision.

### C. Versioning & compatibility

- **C1 [MUST]** The API is versioned. URL-path versioning (`/v1/payments`) is the default convention; header-based versioning is acceptable if the team commits to it consistently.
- **C2 [SHOULD]** Use whole-number major versions (`v1`, `v2`). Do not put minor/patch versions in the URL.
- **C3 [MUST]** Within a major version, only make backward-compatible changes: add optional fields, add endpoints. Never remove or rename a field, change a type, or change semantics within a version.
- **C4 [SHOULD]** A breaking change means a new major version; the old version runs in parallel through a documented deprecation window.
- **C5 [SHOULD]** Unknown fields in a request are rejected (`DisallowUnknownFields`) so client/server drift surfaces early rather than silently.

### D. Request handling & validation

- **D1 [MUST]** Decode request bodies into typed structs. Limit body size (`http.MaxBytesReader`). Use `json.Decoder` with `DisallowUnknownFields()`.
- **D2 [MUST]** Validate every input server-side against an allow-list (type, range, length, format) before acting. Client validation is UX only.
- **D3 [MUST]** Pass `context.Context` from the request (`r.Context()`) into every downstream call (DB, RPC, outbound HTTP) so cancellation and deadlines propagate.
- **D4 [SHOULD]** Handlers are thin: decode, validate, call the service, encode the result. Business logic lives in the service layer, not the handler.
- **D5 [SHOULD]** Set a per-request timeout (via `context.WithTimeout` or a middleware) so a slow request cannot occupy a worker indefinitely.

### E. Error responses

- **E1 [MUST]** Error responses use a single consistent JSON shape across the whole API \u2014 e.g. `{"error": {"code": "...", "message": "...", "correlation_id": "..."}}`.
- **E2 [MUST]** Error responses never leak internal detail: no stack traces, no SQL, no framework versions, no internal hostnames or paths (see `golang-bfsi-bindings` rule G1).
- **E3 [MUST]** Every error response carries a correlation ID that also appears in server logs, so support can trace it.
- **E4 [SHOULD]** Error `code` values are stable, machine-readable strings (e.g. `PAYMENT_DECLINED`), documented in the API contract. The `message` is human-readable and may change.
- **E5 [MUST]** Map internal errors to status codes deliberately (see `references/E-error-responses.md`); do not return `500` for what is really a `400` or `409`.

### F. Pagination, filtering & sorting

- **F1 [MUST]** Collection endpoints are paginated. An unbounded list endpoint is a defect \u2014 it will eventually return an unsafe payload size.
- **F2 [SHOULD]** Use cursor-based pagination for large or frequently-changing datasets; offset/limit is acceptable for small, stable datasets.
- **F3 [MUST]** Enforce a maximum page size server-side; a client request for a larger page is capped, not honoured.
- **F4 [SHOULD]** Filtering and sorting parameters are validated against a server-side allow-list of permitted fields \u2014 never interpolated into a query (see `golang-bfsi-bindings` rule C2).

### G. Middleware & cross-cutting concerns

- **G1 [SHOULD]** Implement cross-cutting concerns as middleware: request logging, correlation-ID injection, panic recovery, authentication, timeout, rate limiting. Keep handlers focused on their resource.
- **G2 [MUST]** A panic-recovery middleware wraps all handlers so one panicking request cannot crash the process (see `golang-bfsi-bindings` rule G \u2014 panic discipline).
- **G3 [MUST]** A correlation-ID middleware generates or propagates a request ID and places it in the context and the response.
- **G4 [SHOULD]** Apply rate limiting at the edge for customer-facing APIs.
- **G5 [MUST]** Security middleware (authn, authz) runs before business handlers; authorisation is always server-side (see `golang-bfsi-bindings` rules A, B).

## Out of scope

- Detailed authn/authz mechanics, TLS configuration, crypto \u2014 governed by `golang-bfsi-bindings`.
- gRPC and GraphQL \u2014 this skill is REST/HTTP. The principles transfer but the specifics differ.
- API gateway / infrastructure configuration.

## Interaction with other skills

`golang-bfsi-bindings` governs security and regulatory concerns; this skill governs API shape and ergonomics. Where both speak (idempotency keys, error disclosure, input validation), they agree by design; the BFSI skill is authoritative on the security intent.
