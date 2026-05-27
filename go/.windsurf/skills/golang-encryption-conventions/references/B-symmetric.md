# B. Symmetric Encryption

Expands rules B1\u2013B6.

## B1 + B3 + B4 + B5 \u2014 AES-256-GCM, done correctly [MUST]

```go
import (
    "crypto/aes"
    "crypto/cipher"
    "crypto/rand"
    "fmt"
)

// encrypt returns nonce and ciphertext. aad is non-secret context
// bound to the ciphertext (e.g. record ID + key version).
func encrypt(key, plaintext, aad []byte) (nonce, ciphertext []byte, err error) {
    if len(key) != 32 { // B4: enforce AES-256 key length
        return nil, nil, fmt.Errorf("key must be 32 bytes for AES-256, got %d", len(key))
    }
    block, err := aes.NewCipher(key)
    if err != nil {
        return nil, nil, fmt.Errorf("aes.NewCipher: %w", err)
    }
    gcm, err := cipher.NewGCM(block)
    if err != nil {
        return nil, nil, fmt.Errorf("cipher.NewGCM: %w", err)
    }
    nonce = make([]byte, gcm.NonceSize())
    if _, err := rand.Read(nonce); err != nil { // B3: fresh nonce, every time
        return nil, nil, fmt.Errorf("nonce: %w", err)
    }
    ciphertext = gcm.Seal(nil, nonce, plaintext, aad)
    return nonce, ciphertext, nil
}

func decrypt(key, nonce, ciphertext, aad []byte) ([]byte, error) {
    if len(key) != 32 {
        return nil, fmt.Errorf("key must be 32 bytes")
    }
    block, err := aes.NewCipher(key)
    if err != nil {
        return nil, fmt.Errorf("aes.NewCipher: %w", err)
    }
    gcm, err := cipher.NewGCM(block)
    if err != nil {
        return nil, fmt.Errorf("cipher.NewGCM: %w", err)
    }
    plaintext, err := gcm.Open(nil, nonce, ciphertext, aad)
    if err != nil {
        // authentication failed: wrong key, tampered ciphertext, or wrong AAD
        return nil, fmt.Errorf("decrypt/authenticate: %w", err)
    }
    return plaintext, nil
}
```

Key points:
- **AEAD (B1):** GCM both encrypts and authenticates. `gcm.Open` returns an error if the ciphertext (or the AAD) was tampered with. There is no separate "verify" step to forget.
- **Fresh nonce (B3):** a new `crypto/rand` nonce per call. A nonce reused with the same key in GCM can leak the authentication subkey \u2014 the single worst mistake in GCM usage.
- **Key length (B4):** checked explicitly. A 16-byte key would silently give AES-128 instead of AES-256.
- **AAD (B5):** binds the ciphertext to its context. If `aad` is the record ID plus key version, an attacker cannot take the ciphertext from record A and paste it into record B \u2014 `gcm.Open` will fail because the AAD will not match.

## B2 \u2014 No ECB, no unauthenticated CBC [MUST]

- **ECB** encrypts identical plaintext blocks to identical ciphertext blocks \u2014 it leaks structure. Never use it.
- **CBC** without a MAC is vulnerable to padding-oracle attacks and provides no tamper detection. If you find CBC, replace it with GCM. CBC-plus-HMAC (encrypt-then-MAC) is technically sound but error-prone to assemble by hand \u2014 prefer AEAD.

## B6 \u2014 Store nonce and key version with the ciphertext [SHOULD]

The nonce is not secret \u2014 it must be stored to decrypt. A practical stored record:

```go
type EncryptedField struct {
    KeyVersion int    // which key encrypted this (B6, supports rotation)
    Nonce      []byte // the GCM nonce
    Ciphertext []byte // includes the GCM authentication tag
}
```

The key version lets you rotate keys: new data uses the current key; old data is still decryptable with the (retained) older key identified by its version.

## chacha20poly1305 as an alternative [MUST-equivalent]

```go
import "golang.org/x/crypto/chacha20poly1305"

aead, err := chacha20poly1305.NewX(key) // XChaCha20-Poly1305, 24-byte nonce
```

A sound AEAD alternative to AES-GCM, especially where AES hardware acceleration is absent. XChaCha20's larger nonce makes random-nonce generation safer against collision over very high message volumes.

## Common findings

1. GCM nonce generated once and reused \u2014 catastrophic.
2. ECB mode used.
3. CBC mode with no authentication \u2014 padding-oracle and tamper exposure.
4. 16-byte key passed where AES-256 was intended \u2014 silent downgrade to AES-128.
5. No AAD, so ciphertexts can be swapped between records.
6. `gcm.Open` error ignored \u2014 tampered ciphertext treated as valid.
