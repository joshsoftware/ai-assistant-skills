# D. Hashing & Password Storage

Expands rules D1–D4.

## D1 — Approved hashes for security purposes [MUST]

```go
import "crypto/sha256"

hash := sha256.Sum256(data)          // [32]byte
// or for streaming:
h := sha256.New()
h.Write(data)
digest := h.Sum(nil)
```

SHA-256 is the baseline. Use SHA-384 or SHA-512 where higher assurance is needed.

Never use MD5 or SHA-1 for any security purpose:
- **MD5** — collision attacks are trivially practical; broken for integrity.
- **SHA-1** — collision attacks demonstrated (SHAttered, 2017); broken for signing and integrity.

Acceptable non-security uses (e.g. cache key, ETag, deduplication fingerprint) exist, but they must be annotated with a comment stating the non-security role so a reviewer does not flag them and so they never drift into a security role.

```go
// checksum for deduplication only — not a security control
hash := md5.Sum(fileContent) // non-security: safe here
```

## D2 + D3 — Password hashing [MUST]

Never hash passwords with a general-purpose hash, even with a salt. SHA-256 is fast by design; an attacker can evaluate billions per second on modern hardware. Password hashing algorithms are deliberately slow and memory-hard.

### bcrypt (most common)

```go
import "golang.org/x/crypto/bcrypt"

func hashPassword(password []byte) ([]byte, error) {
    return bcrypt.GenerateFromPassword(password, 12) // cost >= 12
}

func checkPassword(hashed, password []byte) error {
    return bcrypt.CompareHashAndPassword(hashed, password)
}
```

Tuning: bcrypt cost 12 should take ~250ms on production hardware. If it takes far less, increase the cost. Run a benchmark on your actual hardware and set the highest cost that stays within your latency budget (typically 200–300ms for a login endpoint).

`bcrypt.DefaultCost` is 10 — too low for 2024+ hardware. Set explicitly to 12 or higher.

### Argon2id (recommended for new code)

```go
import "golang.org/x/crypto/argon2"

func hashPasswordArgon2(password []byte) ([]byte, error) {
    salt := make([]byte, 16)
    if _, err := rand.Read(salt); err != nil {
        return nil, err
    }
    hash := argon2.IDKey(password, salt, 1, 64*1024, 4, 32)
    // store salt + hash together (e.g. encoded together)
    return append(salt, hash...), nil
}
```

OWASP recommended parameters for Argon2id (minimum; tune upward to your latency budget):
- `time` (iterations): 1
- `memory`: 64 MB (64 * 1024 KiB)
- `threads`: 4 (or the number of available CPUs)
- `keyLen`: 32

Argon2id is memory-hard, making GPU/ASIC attacks more expensive than bcrypt. Prefer it for new code.

### scrypt

Also acceptable. Use `golang.org/x/crypto/scrypt` with parameters meeting current OWASP guidance. Less commonly used than bcrypt/Argon2 in Go.

## D4 — Annotate non-security hash uses [SHOULD]

```go
// etag is a cache-coherence token, not a security control.
// MD5 is intentionally used here for speed; it carries no security obligation.
etag := fmt.Sprintf("%x", md5.Sum(body))
```

The comment prevents a future reviewer from flagging it as "using MD5" without context, and prevents someone from copy-pasting the pattern into a security context.

## Common findings

1. bcrypt with `bcrypt.DefaultCost` (10) — too low.
2. SHA-256 used to "hash passwords" with a salt — fast hash, brute-forceable.
3. Passwords stored unhashed or base64-encoded.
4. MD5 used for an ETag with no comment — reviewer cannot distinguish security from non-security use.
5. Argon2 parameters copy-pasted from an old blog post below current OWASP guidance.
