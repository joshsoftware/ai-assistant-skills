# Sources & Rationale

This skill is **engineering conventions, not a standard.**

## What this skill draws on

- **REST as an architectural style** \u2014 REST is a style described by Roy Fielding, not a wire specification. "RESTful API best practices" are widely-shared community conventions, not a ratified standard. Resource-oriented URLs, HTTP verb semantics, and status-code usage are the stable, broadly-agreed parts.
- **HTTP semantics** \u2014 method safety and idempotency, and status-code meanings, are genuinely standardised (the HTTP RFCs). Rules B1\u2013B3 rest on that standardised layer.
- **Go standard library** \u2014 `net/http`, `ServeMux` with method-based routing (Go 1.22+), `encoding/json`, `context`. The handler/middleware patterns here are idiomatic Go.
- **OpenAPI** \u2014 the de facto contract format for documenting REST APIs.

## What is genuinely standardised vs convention

- **Standardised:** HTTP methods, their safety/idempotency properties, and status-code semantics (HTTP RFCs). Rules that rest on these are firm.
- **Convention:** URL design (plural nouns, nesting depth), versioning style, response envelopes, pagination style, error-body shape. These are widely-agreed community practice with real variation between organisations. The skill picks a coherent default and says so.

## Where the community varies

- **Versioning** \u2014 URL-path vs header vs media-type versioning all have advocates. The skill defaults to URL-path for operational transparency but accepts header versioning if applied consistently.
- **Response envelopes** \u2014 some teams wrap every response in `{"data": ...}`, others return bare objects. The skill requires *consistency*, not a specific choice.
- **Pagination** \u2014 cursor vs offset is a real trade-off. The skill recommends cursor for large/changing data, accepts offset for small/stable data.
- **Error shape** \u2014 there is an RFC for a problem-details JSON format, but it is not universally adopted. The skill specifies a simple consistent shape rather than mandating that RFC.

## Relationship to the BFSI skills

Security-sensitive API concerns \u2014 authentication, authorisation, TLS, input-validation depth, idempotency for money movement, error-disclosure limits \u2014 are governed by `bfsi-india-core` and `golang-bfsi-bindings`. This skill restates them only where they are also API-shape decisions, and defers to the BFSI skills on the security intent. Where this skill and the BFSI skills both speak, they are designed to agree.
