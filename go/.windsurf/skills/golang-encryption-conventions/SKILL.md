---
name: golang-encryption-conventions
description: Go encryption and decryption conventions for backend services and BFSI applications \u2014 symmetric and asymmetric encryption, hashing, password storage, MACs, key derivation, secure randomness, and envelope encryption. Use whenever Go code performs cryptographic operations, or when such code is being reviewed or designed. Activate on mentions of encrypt, decrypt, AES, GCM, RSA, ECDSA, hashing, SHA-256, bcrypt, Argon2, HMAC, crypto/rand, key derivation, envelope encryption, field-level encryption, or "how do I encrypt this in Go". These are engineering conventions built on established cryptographic standards (NIST, OWASP) applied to Go's standard library. This skill is self-contained; for BFSI regulatory key-management obligations also consult bfsi-india-core and golang-bfsi-bindings.
---

# Go Encryption & Decryption Conventions

Conventions for cryptographic operations in Go. The cryptographic *primitives* here rest on genuine standards (NIST, OWASP, IETF); the *application conventions* (when to use which, how to structure key handling in Go) are engineering practice.

This skill is intentionally self-contained. It overlaps with `golang-bfsi-bindings` category D by design \u2014 if both are active, treat the BFSI skill as authoritative on regulatory key-management obligations (HSM/KMS, key lifecycle, audit) and this skill as the Go implementation detail.

## How to use this skill

1. When writing or reviewing Go crypto code, walk the rule categories.
2. Each rule has a severity (RFC 2119) and links to a `references/` file.
3. **MUST** violations are blockers \u2014 cryptography fails closed and fails silently; a wrong choice is not visible at runtime.
4. Never invent cryptographic constructions. Use the standard library and `golang.org/x/crypto` as directed.

## Sources

- **NIST** \u2014 SP 800-90A (random number generation), SP 800-38D (GCM), SP 800-57 (key management), FIPS 197 (AES).
- **OWASP** \u2014 Cryptographic Storage Cheat Sheet, Password Storage Cheat Sheet.
- **IETF RFCs** for the algorithms (AES-GCM, HKDF, etc.).
- **Go standard library** `crypto/*` and `golang.org/x/crypto/*` package documentation.

Full notes: `references/sources-and-rationale.md`.

## Rule categories

| # | Category | Reference file |
|---|---|---|
| A | Secure randomness | `references/A-randomness.md` |
| B | Symmetric encryption | `references/B-symmetric.md` |
| C | Asymmetric encryption & signatures | `references/C-asymmetric.md` |
| D | Hashing & password storage | `references/D-hashing-passwords.md` |
| E | MACs & key derivation | `references/E-mac-kdf.md` |
| F | Key handling in Go | `references/F-key-handling.md` |

---

## Rule index

### A. Secure randomness

- **A1 [MUST]** Use `crypto/rand` for all security-sensitive randomness \u2014 keys, nonces, IVs, salts, tokens, session IDs. Never `math/rand` (it is a predictable PRNG, even when seeded). *(NIST SP 800-90A.)*
- **A2 [MUST]** Check the error returned by `crypto/rand.Read`. A failed entropy read must abort the operation, never proceed with partial or zero bytes.
- **A3 [SHOULD]** Generate randomness of adequate length: \u2265 16 bytes (128 bits) for tokens and salts; the exact size required by the algorithm for keys and nonces.

### B. Symmetric encryption

- **B1 [MUST]** Use authenticated encryption (AEAD). The default is AES-256-GCM via `crypto/aes` + `crypto/cipher.NewGCM`. `chacha20poly1305` is an acceptable alternative. *(NIST SP 800-38D.)*
- **B2 [MUST]** Never use ECB mode. Never use CBC (or any unauthenticated mode) without a separate MAC \u2014 and prefer AEAD over CBC-plus-MAC entirely.
- **B3 [MUST]** Generate a fresh nonce/IV from `crypto/rand` for every encryption operation. Never reuse a nonce with the same key \u2014 GCM nonce reuse is catastrophic (it can reveal the authentication key).
- **B4 [MUST]** Use a full-length key: 32 bytes for AES-256. Check the key length explicitly; a short key silently downgrades the cipher strength.
- **B5 [SHOULD]** Bind ciphertext to its context with AEAD associated data (AAD) \u2014 e.g. record ID, tenant, key version \u2014 so ciphertexts cannot be swapped between records.
- **B6 [SHOULD]** Store the nonce and a key-version identifier alongside the ciphertext; they are not secret and are needed to decrypt.

### C. Asymmetric encryption & signatures

- **C1 [MUST]** Use RSA-2048 or larger, or ECDSA P-256 or larger, or Ed25519. Below RSA-2048 is not acceptable.
- **C2 [SHOULD]** For RSA encryption use OAEP (`rsa.EncryptOAEP`), not PKCS#1 v1.5. For RSA signatures prefer PSS (`rsa.SignPSS`) over PKCS#1 v1.5; use v1.5 only for legacy interoperability.
- **C3 [MUST]** Verify signatures correctly: check the returned error, and verify against the expected public key. A signature check that ignores its error verifies nothing.
- **C4 [SHOULD]** Use Ed25519 (`crypto/ed25519`) for new signing use cases where both ends are under your control \u2014 it is fast, has small keys, and is misuse-resistant.

### D. Hashing & password storage

- **D1 [MUST]** Use SHA-256 or stronger for security-relevant hashing. Never MD5 or SHA-1 for any security purpose (integrity, signing, token derivation, fingerprinting that matters).
- **D2 [MUST]** Never hash passwords with a plain or salted general-purpose hash (SHA-256, etc.). Use a password hash: `bcrypt`, `scrypt`, or Argon2id from `golang.org/x/crypto`. *(OWASP Password Storage.)*
- **D3 [MUST]** Use adequate password-hash parameters: bcrypt cost \u2265 12; Argon2id with sufficient memory/time per current OWASP guidance. Tune to hardware.
- **D4 [SHOULD]** A non-security use of a fast hash (e.g. a cache key, an ETag) is acceptable but must be commented as non-security so a reviewer does not flag it \u2014 and must never drift into a security role.

### E. MACs & key derivation

- **E1 [MUST]** Use HMAC (`crypto/hmac`) with SHA-256 or stronger for message authentication when not using AEAD.
- **E2 [MUST]** Compare MACs, tokens, and other secrets in constant time \u2014 `hmac.Equal` or `crypto/subtle.ConstantTimeCompare`. Never `==` or `bytes.Equal` (they short-circuit and leak timing).
- **E3 [MUST]** Derive keys with a proper KDF: HKDF (`golang.org/x/crypto/hkdf`) from a high-entropy secret; PBKDF2/scrypt/Argon2 from a low-entropy secret like a password. Never derive a key by plain-hashing a static string.
- **E4 [SHOULD]** Derive distinct keys for distinct purposes (encryption vs MAC vs token signing) rather than reusing one key everywhere.

### F. Key handling in Go

- **F1 [MUST]** Never hard-code keys, IVs, salts, or pepper in source, in committed config, or in container images.
- **F2 [MUST]** Obtain production keys from a managed key store (KMS/HSM/Vault). Use envelope encryption: a data key encrypts the data; a master key (in the KMS/HSM) encrypts the data key.
- **F3 [SHOULD]** Zeroise key material after use \u2014 overwrite the backing array (`for i := range k { k[i] = 0 }`) and drop the reference. This is best-effort under Go's garbage collector but still worthwhile for long-lived keys.
- **F4 [SHOULD]** Pass keys as `[]byte` slices and avoid copying them into many values; each copy is another byte array that must be zeroised.
- **F5 [MUST]** Make key-bearing and secret-bearing types render as redacted in logs/JSON (`String()`, `LogValue()`, `MarshalJSON()` returning a redaction placeholder) so they cannot be logged by accident.
- **F6 [SHOULD]** Support key rotation: tag ciphertext with a key version, keep old keys available long enough to decrypt historic data, then retire them.

## Out of scope

- TLS configuration \u2014 see `golang-api-conventions` and `golang-bfsi-bindings` category H.
- Regulatory key-management obligations (HSM mandates, audit, lifecycle SOPs) \u2014 see `bfsi-india-core` / `golang-bfsi-bindings` category D.
- Certificate and PKI management.

## A note on judgement

Cryptography punishes small mistakes severely and silently \u2014 a reused nonce or an ignored error produces output that looks correct and is not. When unsure, prefer the highest-level, most misuse-resistant construction (AEAD over raw block ciphers, Ed25519 over hand-rolled signing) and never deviate from the standard library without expert review.
