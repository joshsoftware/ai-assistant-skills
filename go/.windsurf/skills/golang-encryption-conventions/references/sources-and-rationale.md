# Sources & Rationale

## What is genuinely standardised here

The cryptographic *algorithms* are standardised:
- **NIST FIPS 197** — AES specification.
- **NIST SP 800-38D** — GCM mode of operation.
- **NIST SP 800-90A** — random number generation.
- **NIST SP 800-57** — key management recommendations.
- **IETF RFC 5869** — HKDF.
- **IETF RFC 8032** — Ed25519.
- **OWASP Password Storage Cheat Sheet** — bcrypt/Argon2 parameters.
- **OWASP Cryptographic Storage Cheat Sheet** — selection and usage guidance.

## What is convention

The *Go implementation* (how to call `crypto/aes`, how to structure envelope encryption, the zeroisation pattern, the redacting type wrapper) is engineering convention drawing on these standards. The standard says "use AES-256-GCM with a random nonce"; this skill shows how to do it correctly in Go.

## Where reasonable people differ

- **bcrypt vs Argon2id** — both are sound. Argon2id is the OWASP current recommendation for new code; bcrypt is widely deployed and still acceptable. The skill presents both.
- **Zeroisation** — some argue Go's GC semantics make zeroisation unreliable enough to be not worth doing; others argue best-effort is still worth doing. The skill says SHOULD (not MUST) and explains the limitation.
- **Ed25519 vs ECDSA** — both are sound. The skill recommends Ed25519 for new use where both ends are under your control, ECDSA/RSA for interoperability.

## Relationship to BFSI skills

`golang-bfsi-bindings` category D covers the same cryptographic primitives with an emphasis on regulatory key-management obligations (HSM, KMS, key lifecycle SOP, PCI-DSS for card data). This skill covers the Go implementation details. When both are active, the BFSI skill is authoritative on the regulatory intent; this skill is the implementation guide.
