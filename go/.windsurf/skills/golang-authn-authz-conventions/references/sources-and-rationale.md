# Sources & Rationale

## What is genuinely standardised here

- **JWT** — RFC 7519 (JWT), RFC 7515 (JWS), RFC 7517 (JWK). The JWT format, signing methods, and registered claims are standards. The Go implementation conventions are not.
- **OAuth2** — RFC 6749 (framework), RFC 7636 (PKCE), RFC 7662 (introspection), RFC 8693 (token exchange).
- **OIDC** — OpenID Connect Core 1.0.
- **OWASP ASVS V3** (Session Management), **V4** (Access Control) — internationally recognised but not government-mandated for Indian BFSI (RBI references them as "industry-accepted practice").

## What is convention

The Go implementation — how to structure auth middleware, where to put identity in context, how to design the RBAC interface — is engineering practice. There is no Go standard for this.

## Relationship to BFSI skills

`golang-bfsi-bindings` categories A (authentication/session) and B (authorisation) cover the same ground with regulatory citations (RBI, CERT-In). This skill is the fuller Go-implementation reference. When both are active:
- This skill governs the Go patterns.
- The BFSI skill governs the regulatory obligations (MFA, cooling periods, step-up auth, session timeouts aligned to RBI requirements, audit logging of auth events).
- They agree on the mechanics (CSPRNG session IDs, cookie flags, JWT validation); the BFSI skill adds the regulatory dimension.
