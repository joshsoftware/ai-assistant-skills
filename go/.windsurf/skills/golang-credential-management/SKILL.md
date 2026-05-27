---
name: golang-credential-management
description: Go credential management conventions — how Go services load, use, rotate, and protect secrets at runtime. Covers environment variable delivery, secret store integration (HashiCorp Vault, AWS Secrets Manager, GCP Secret Manager), secret lifecycle, API key management, rotation patterns, and the Go types and patterns that prevent accidental credential leakage. Use whenever Go code handles secrets, API keys, database passwords, TLS certificates, HMAC keys, or service account credentials. Activate on mentions of secret, credential, API key, vault, secrets manager, rotation, env var, os.Getenv, .env file, or "how do I manage secrets in Go". These are engineering conventions — no official Go or industry standard defines credential management mechanics. Standalone skill — intentionally overlaps with golang-bfsi-bindings go-I-secrets.md; if both are active the BFSI skill is authoritative on regulatory obligations.
---

# Go Credential Management Conventions

Conventions for managing credentials and secrets in Go services. **These are conventions, not a standard.**

The security *principles* (no hardcoded secrets, rotation, least-privilege access) are widely accepted. The *Go implementation* is engineering practice.

## How to use this skill

1. Walk the rule categories below when writing or reviewing secret-handling code.
2. **MUST** violations are blockers.
3. For BFSI regulatory obligations (HSM mandates, key lifecycle SOPs, CERT-In requirements), also consult `golang-bfsi-bindings` go-I-secrets.md.

## Sources

- 12-Factor App methodology (config from environment).
- HashiCorp Vault, AWS Secrets Manager, GCP Secret Manager documentation.
- OWASP Secrets Management Cheat Sheet.
- Go standard library: `os`, `crypto/subtle`, `sync/atomic`.

Full notes: `references/sources-and-rationale.md`.

## Rule categories

| # | Category | Reference file |
|---|---|---|
| A | Secret loading & delivery | `references/A-loading-delivery.md` |
| B | Secret types & in-memory protection | `references/B-secret-types.md` |
| C | Secret store integration | `references/C-secret-stores.md` |
| D | Rotation & lifecycle | `references/D-rotation-lifecycle.md` |
| E | API key management | `references/E-api-keys.md` |

---

## Rule index

### A. Secret loading & delivery

- **A1 [MUST]** No secret literals in source code, committed config files, container images, or CI artefacts.
- **A2 [MUST]** Production secrets are delivered at runtime from a managed secret store (Vault, AWS Secrets Manager, GCP Secret Manager, Azure Key Vault, or equivalent KMS). Environment variables are an acceptable *delivery mechanism* when the orchestrator injects them from the secret store — not when they are set to literal values in deployment manifests.
- **A3 [MUST]** Validate at startup that all required secrets are present and non-empty. Fail fast with a clear message naming the missing secret. Do not start a service with a missing credential.
- **A4 [MUST]** Different secrets for each environment (dev, SIT, UAT, pre-prod, prod). A secret valid in a lower environment must not work in production.
- **A5 [MUST]** Secrets are never logged. Implement secret types that render as `[REDACTED]` in all serialisation and logging paths.

### B. Secret types & in-memory protection

- **B1 [MUST]** Secret-bearing values use a typed wrapper (`type SecretKey []byte`, `type APIKey string`) whose `String()`, `LogValue()`, and `MarshalJSON()` return `"[REDACTED]"`.
- **B2 [MUST]** After a secret is used, overwrite the backing byte slice (`for i := range key { key[i] = 0 }`). This is best-effort under Go's GC but still worthwhile for long-lived keys.
- **B3 [SHOULD]** Pass secrets as `[]byte` parameters rather than copying into struct fields, to minimise the number of copies in memory.
- **B4 [SHOULD]** After reading secrets from environment variables, clear the environment variable to reduce the window of exposure to other processes running as the same user.

### C. Secret store integration

- **C1 [MUST]** Secret store access is authenticated using a workload identity (IAM role, Kubernetes service account, Vault AppRole/AWS IAM) — never with a static credential stored in code.
- **C2 [MUST]** Secret store calls always use `context.Context` with an explicit timeout. A hung secret-store call must not block service startup indefinitely.
- **C3 [SHOULD]** Cache fetched secrets in memory with the secret store's recommended TTL or lease duration. Do not fetch on every request — the secret store is not a low-latency data store.
- **C4 [SHOULD]** Support live rotation: hold the current secret in an `atomic.Pointer` or behind a read-write mutex so the service can swap in a new value without restart.
- **C5 [SHOULD]** On secret fetch failure at startup, retry with exponential backoff for a bounded period before failing. Transient unavailability of the secret store should not prevent a service from starting.

### D. Rotation & lifecycle

- **D1 [MUST]** Every secret has a defined rotation cadence. Secrets that are never rotated are a standing risk.
- **D2 [MUST]** Rotation must be operationally seamless — the service handles both the old and new secret during the rotation window, then drops the old one.
- **D3 [MUST]** Rotate immediately on: suspected or confirmed compromise, departure of a person with access, or a production incident involving the secret.
- **D4 [SHOULD]** Automate rotation where the secret store supports it (AWS Secrets Manager automatic rotation, Vault dynamic secrets). Manual rotation is error-prone and delayed.
- **D5 [SHOULD]** Log (to the audit trail, not to application logs) that a rotation occurred, when, and by whom. Do not log the secret value.

### E. API key management

- **E1 [MUST]** API keys issued to clients are generated with `crypto/rand` (minimum 32 bytes), base64url-encoded. Never sequential, never guessable.
- **E2 [MUST]** Store only a hash of the API key (HMAC-SHA256 or bcrypt), never the plaintext. The key is shown to the issuing user once at creation and never again.
- **E3 [MUST]** API keys are scoped — each key has a defined set of permissions. A single "master key" for all operations is prohibited.
- **E4 [MUST]** API keys have an expiry date and a revocation mechanism. A revoked key must be rejected immediately.
- **E5 [SHOULD]** Associate each API key with a human owner or system identity for audit and attribution. An orphaned key with no known owner is a decommissioning risk.

## Out of scope

- TLS certificate issuance and rotation — PKI / infrastructure concern.
- Encryption key management (HSM, KMS, DEK/KEK) — see `golang-encryption-conventions` rule F2.
- BFSI-specific HSM mandates and key lifecycle SOPs — see `golang-bfsi-bindings` go-I-secrets.md and go-D-crypto.md.
