# Sources & Rationale

This skill is **engineering conventions, not a standard.**

## What this skill draws on

- **12-Factor App methodology** — config (including secrets) from the environment; strictly separated per deployment.
- **OWASP Secrets Management Cheat Sheet** — no hardcoded secrets, secure storage, rotation, least-privilege access.
- **HashiCorp Vault, AWS Secrets Manager, GCP Secret Manager** documentation — patterns for workload identity authentication, lease TTLs, dynamic secrets.
- **Go standard library** — `os`, `sync/atomic`, `crypto/subtle`.

## What is NOT a standard here

There is no official Go standard for credential management. The patterns (typed secret wrappers, atomic pointer for live rotation, per-environment secret paths) are widely-used engineering practices without a single authoritative reference.

The security *principles* (no hardcoding, rotation, least privilege, fail fast) are widely accepted and appear in OWASP, NIST SP 800-57, and implicitly in the RBI/CERT-In requirements covered by `golang-bfsi-bindings`.

## Relationship to BFSI skills

`golang-bfsi-bindings` go-I-secrets.md covers the same ground with regulatory obligations (CERT-In Phase II Section 4.7.3, RBI ITG-RC&AP key management requirements). This skill is the fuller Go implementation reference. When both are active, the BFSI skill is authoritative on the regulatory requirements; this skill is the implementation guide.

## Note on bcrypt for API key verification

Rule E2 uses bcrypt to store API key hashes. This is correct for initial key issuance (the key is stored hashed and can never be retrieved). However, bcrypt is intentionally slow (250ms+ at cost 12) — it cannot be used for every API request. For high-frequency API key verification, store a fast HMAC-SHA256 hash in a lookup table and use that for the per-request check; verify the bcrypt hash only at key-management time (creation, admin verification).
