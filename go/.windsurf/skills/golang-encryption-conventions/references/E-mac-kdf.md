# E. MACs & Key Derivation

Expands rules E1–E4.

## E1 — HMAC-SHA256 for message authentication [MUST]

```go
import (
    "crypto/hmac"
    "crypto/sha256"
)

func sign(key, message []byte) []byte {
    mac := hmac.New(sha256.New, key)
    mac.Write(message)
    return mac.Sum(nil)
}
```

Use HMAC when you need to verify that a message came from someone who holds the key and was not tampered with — e.g. an audit-chain link, a signed webhook payload, a stateless token (though AEAD is often cleaner for tokens).

HMAC-SHA256 provides 128 bits of security against forgery. HMAC-SHA512 for higher assurance.

## E2 — Constant-time comparison [MUST]

```go
import "crypto/hmac"

func verify(key, message, receivedMAC []byte) bool {
    expected := sign(key, message)
    return hmac.Equal(receivedMAC, expected) // constant-time
}
```

`hmac.Equal` is constant-time — it takes the same time regardless of where the mismatch is. Never use `bytes.Equal` or `==` on a MAC, token, or secret comparison:

```go
// WRONG — short-circuits at first mismatch; leaks timing
if bytes.Equal(receivedMAC, expectedMAC) { ... }

// WRONG
if receivedToken == expectedToken { ... }
```

A timing attack against a MAC comparison can, given enough oracle queries, recover the expected value byte by byte. For MACs and secrets this is a real-world attack, not theoretical.

For arbitrary byte slices: `crypto/subtle.ConstantTimeCompare(a, b) == 1`.

## E3 — Proper key derivation [MUST]

Never derive a key by hashing a string:

```go
// WRONG — produces a deterministic key with poor key-separation properties
key := sha256.Sum256([]byte("my-app-encryption-key")) // not a KDF
```

### HKDF — from a high-entropy secret (e.g. a master key)

```go
import (
    "crypto/sha256"
    "golang.org/x/crypto/hkdf"
    "io"
)

func deriveKey(masterKey, salt, info []byte, length int) ([]byte, error) {
    r := hkdf.New(sha256.New, masterKey, salt, info)
    key := make([]byte, length)
    if _, err := io.ReadFull(r, key); err != nil {
        return nil, fmt.Errorf("hkdf: %w", err)
    }
    return key, nil
}
```

`info` is a context string that separates purposes: `"encryption"`, `"signing"`, `"token-mac"`. Deriving from the same master with different `info` values produces independent keys — even if one is compromised, the others are not.

### PBKDF2 / scrypt / Argon2id — from a low-entropy secret (e.g. a user password)

A user password goes through a password-hash KDF before being used as key material. See `D-hashing-passwords.md`.

## E4 — One key per purpose [SHOULD]

Reusing a single key for encryption, signing, and token MAC is poor practice — a vulnerability in one use can affect the others. Derive separate keys from a master via HKDF with distinct `info` labels:

```go
encKey, _ := deriveKey(master, salt, []byte("aes-gcm-encryption"), 32)
macKey, _ := deriveKey(master, salt, []byte("hmac-sha256-audit"), 32)
tokenKey, _ := deriveKey(master, salt, []byte("token-signing"), 32)
```

## Common findings

1. `bytes.Equal` used to compare a MAC — timing leak.
2. `==` used to compare tokens — timing leak.
3. Key "derived" by `sha256.Sum256("some static string")` — not a KDF, no salt, weak derivation.
4. One key used for both encryption and MAC.
5. HKDF `info` field left empty or the same across all derived keys — no purpose separation.
