# C. Asymmetric Encryption & Signatures

Expands rules C1\u2013C4.

## C1 \u2014 Adequate key sizes [MUST]

- RSA: 2048 bits minimum (3072+ for long-lived keys).
- ECDSA: P-256 minimum (P-384 for higher assurance).
- Ed25519: fixed strength, no size parameter \u2014 always adequate.

RSA-1024 and below are not acceptable.

## C2 \u2014 OAEP for RSA encryption, PSS for RSA signatures [SHOULD]

```go
import (
    "crypto/rand"
    "crypto/rsa"
    "crypto/sha256"
)

// RSA encryption \u2014 OAEP, not PKCS#1 v1.5
ciphertext, err := rsa.EncryptOAEP(sha256.New(), rand.Reader, pub, plaintext, label)
if err != nil {
    return fmt.Errorf("rsa OAEP encrypt: %w", err)
}

plaintext, err := rsa.DecryptOAEP(sha256.New(), rand.Reader, priv, ciphertext, label)
if err != nil {
    return fmt.Errorf("rsa OAEP decrypt: %w", err)
}
```

```go
// RSA signing \u2014 PSS preferred
hashed := sha256.Sum256(message)
sig, err := rsa.SignPSS(rand.Reader, priv, crypto.SHA256, hashed[:], nil)
if err != nil {
    return fmt.Errorf("rsa PSS sign: %w", err)
}
```

PKCS#1 v1.5 encryption is vulnerable to known padding attacks; OAEP is the modern choice. PKCS#1 v1.5 signatures are still in wide legacy use \u2014 use them only when a counterparty requires it, and prefer PSS otherwise.

Note: RSA encryption is for small payloads (a key, a token). To protect bulk data, use envelope encryption \u2014 RSA-encrypt a symmetric data key, then AES-GCM-encrypt the data with that key (see `references/F-key-handling.md`).

## C3 \u2014 Verify signatures correctly [MUST]

```go
hashed := sha256.Sum256(message)
if err := rsa.VerifyPSS(pub, crypto.SHA256, hashed[:], sig, nil); err != nil {
    return fmt.Errorf("signature verification failed: %w", err)
}
// only past this point is the signature trusted
```

The verification call returns an error. If that error is ignored, nothing has been verified \u2014 the code merely *ran* a verification and discarded its result. Also confirm the `pub` key is the one actually expected for the signer (e.g. pinned, or fetched from a trusted source), not whatever key arrived alongside the message.

## C4 \u2014 Prefer Ed25519 for new signing [SHOULD]

```go
import "crypto/ed25519"

pub, priv, err := ed25519.GenerateKey(rand.Reader)
if err != nil {
    return fmt.Errorf("ed25519 keygen: %w", err)
}

sig := ed25519.Sign(priv, message)

if !ed25519.Verify(pub, message, sig) {
    return errors.New("signature verification failed")
}
```

For new signing use cases where both ends are under your control, Ed25519 is fast, has small keys and signatures, needs no separate hashing step, and has very few ways to misuse. Choose RSA/ECDSA when an external standard or counterparty requires them.

## Common findings

1. RSA key smaller than 2048 bits.
2. PKCS#1 v1.5 encryption used for new code instead of OAEP.
3. Signature `Verify`/`VerifyPSS` error ignored \u2014 verification is a no-op.
4. Signature verified against a key supplied with the message rather than a trusted/pinned key.
5. RSA used to encrypt bulk data directly instead of envelope encryption.
