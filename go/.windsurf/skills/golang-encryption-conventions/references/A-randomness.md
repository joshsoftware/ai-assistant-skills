# A. Secure Randomness

Expands rules A1\u2013A3.

## A1 \u2014 `crypto/rand`, never `math/rand` [MUST]

```go
// WRONG \u2014 math/rand is a deterministic PRNG; output is predictable
import "math/rand"
token := fmt.Sprintf("%d", rand.Int63()) // \u274c

// RIGHT \u2014 crypto/rand reads the OS CSPRNG
import "crypto/rand"
import "encoding/base64"

func newToken(nBytes int) (string, error) {
    b := make([]byte, nBytes)
    if _, err := rand.Read(b); err != nil {
        return "", fmt.Errorf("crypto/rand read: %w", err)
    }
    return base64.RawURLEncoding.EncodeToString(b), nil
}
```

`math/rand` produces a reproducible sequence \u2014 given the seed (or enough output), the next value is predictable. For a token, session ID, OTP, key, nonce, or salt that is fatal. `crypto/rand` reads the operating system's cryptographically secure RNG.

The two packages have nearly identical APIs, which makes the wrong import easy to make and easy to miss. Treat any `math/rand` import in a crypto, auth, token, or key path as a critical defect.

## A2 \u2014 Always check the entropy error [MUST]

```go
if _, err := rand.Read(b); err != nil {
    return fmt.Errorf("crypto/rand read: %w", err)
}
```

`crypto/rand.Read` can fail (the OS entropy source is unavailable). If the error is ignored, `b` may be all zeros or partially filled \u2014 a "random" key or nonce that is entirely predictable. A failed entropy read aborts the operation; it never proceeds.

## A3 \u2014 Adequate length [SHOULD]

- Tokens, session IDs, salts: at least 16 bytes (128 bits).
- Keys: exactly the length the algorithm requires (32 bytes for AES-256).
- Nonces: exactly the algorithm's nonce size (`gcm.NonceSize()` for GCM, 12 bytes).

Do not truncate a generated value below these sizes to make it "shorter for the URL" \u2014 length is security margin.

## Common findings

1. `math/rand` used for tokens, OTPs, session IDs, or keys \u2014 critical.
2. `rand.Read` error ignored \u2014 a possible all-zero key/nonce.
3. `math/rand.Seed(time.Now()...)` then used for anything security-relevant \u2014 the seed space is tiny.
4. Token length truncated below 128 bits.
