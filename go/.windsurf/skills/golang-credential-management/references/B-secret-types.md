# B. Secret Types & In-Memory Protection

Expands rules B1–B4.

## B1 — Redacting secret type [MUST]

```go
// A secret type that cannot be accidentally logged, printed, or serialised in plaintext.
type Secret []byte

func (Secret) String() string               { return "[REDACTED]" }
func (Secret) GoString() string             { return "[REDACTED]" }
func (Secret) LogValue() slog.Value         { return slog.StringValue("[REDACTED]") }
func (Secret) MarshalJSON() ([]byte, error) { return []byte(`"[REDACTED]"`), nil }
func (Secret) MarshalText() ([]byte, error) { return []byte("[REDACTED]"), nil }

// The raw bytes are accessible only via an explicit method — greppable in code review
func (s Secret) Bytes() []byte { return []byte(s) }
```

For string-typed secrets (API keys, tokens):

```go
type APIKey string

func (APIKey) String() string               { return "[REDACTED]" }
func (APIKey) LogValue() slog.Value         { return slog.StringValue("[REDACTED]") }
func (APIKey) MarshalJSON() ([]byte, error) { return []byte(`"[REDACTED]"`), nil }

func (k APIKey) Value() string { return string(k) } // explicit reveal
```

Code review convention: any call to `.Bytes()` or `.Value()` requires a comment explaining why the reveal is needed.

## B2 — Zeroise after use [SHOULD]

```go
key := fetchSigningKey()     // returns Secret ([]byte wrapper)
defer func() {
    raw := key.Bytes()
    for i := range raw { raw[i] = 0 }
}()
// use key...
```

Limitations:
- Go's GC may have already copied the backing array.
- The compiler may eliminate the loop as a no-op (rare, but possible with optimisation).
- Best-effort: still worthwhile for long-lived keys or keys held in a struct that is kept in memory across requests.

## B4 — Clear environment variable after reading [SHOULD]

```go
func loadSecrets() (*Secrets, error) {
    dsn := os.Getenv("DB_DSN")
    os.Unsetenv("DB_DSN") // clear after reading to reduce exposure window
    if dsn == "" {
        return nil, errors.New("DB_DSN is required")
    }
    return &Secrets{DBDSN: Secret(dsn)}, nil
}
```

Other processes running as the same OS user can read `/proc/<pid>/environ` before the variable is cleared. Clearing reduces the window. Not a guarantee; part of a defence-in-depth approach.

---

# C. Secret Store Integration

Expands rules C1–C5.

## C2 + C3 — Context timeout + in-memory caching [MUST/SHOULD]

```go
type SecretProvider struct {
    client  VaultClient
    cache   atomic.Pointer[cachedSecrets]
    mu      sync.Mutex
}

type cachedSecrets struct {
    values    map[string]Secret
    fetchedAt time.Time
    leaseTTL  time.Duration
}

func (p *SecretProvider) Get(ctx context.Context, key string) (Secret, error) {
    // Check in-memory cache first
    if c := p.cache.Load(); c != nil && time.Since(c.fetchedAt) < c.leaseTTL {
        if v, ok := c.values[key]; ok {
            return v, nil
        }
    }
    return p.refresh(ctx, key)
}

func (p *SecretProvider) refresh(ctx context.Context, key string) (Secret, error) {
    p.mu.Lock()
    defer p.mu.Unlock()

    // Double-checked locking
    if c := p.cache.Load(); c != nil && time.Since(c.fetchedAt) < c.leaseTTL {
        if v, ok := c.values[key]; ok { return v, nil }
    }

    // Fetch with explicit timeout (C2)
    fetchCtx, cancel := context.WithTimeout(ctx, 5*time.Second)
    defer cancel()

    values, leaseTTL, err := p.client.FetchSecrets(fetchCtx)
    if err != nil {
        return nil, fmt.Errorf("secret store fetch: %w", err)
    }
    p.cache.Store(&cachedSecrets{values: values, fetchedAt: time.Now(), leaseTTL: leaseTTL})
    return values[key], nil
}
```

## C4 — Live rotation via atomic pointer [SHOULD]

The `atomic.Pointer[cachedSecrets]` above already supports live rotation: when the background refresher stores a new version, any reader that acquires the pointer after the store sees the new secrets. No restart required.

---

# D. Rotation & Lifecycle

Expands rules D1–D5.

## D2 — Zero-downtime rotation [MUST]

A rotation strategy that requires a service restart is not acceptable for production BFSI services. The rotation window pattern:

1. The secret store issues a new version of the secret.
2. The service detects the new version (via lease TTL expiry, or a notification) and fetches it.
3. For a period (the rotation window — e.g. 5 minutes), the service accepts HMAC verification with both the old and the new key, but signs only with the new key.
4. After the rotation window, the old key is dropped.

```go
type SigningKeys struct {
    Current  Secret
    Previous Secret // retained for rotation window
    RotatedAt time.Time
}

func (k *SigningKeys) Verify(mac, message []byte) bool {
    if hmac.Equal(mac, sign(k.Current.Bytes(), message)) { return true }
    // Accept the previous key during the rotation window (e.g. 5 minutes)
    if time.Since(k.RotatedAt) < 5*time.Minute {
        return hmac.Equal(mac, sign(k.Previous.Bytes(), message))
    }
    return false
}
```

---

# E. API Key Management

Expands rules E1–E5.

## E1 + E2 — Generation and storage [MUST]

```go
// Generate a new API key — show to the user once, never store plaintext
func generateAPIKey() (plaintext string, hash []byte, err error) {
    raw := make([]byte, 32)
    if _, err := rand.Read(raw); err != nil {
        return "", nil, fmt.Errorf("crypto/rand: %w", err)
    }
    plaintext = "ak_" + base64.RawURLEncoding.EncodeToString(raw) // prefix for grep-ability
    hash, err = bcrypt.GenerateFromPassword([]byte(plaintext), 12)
    return plaintext, hash, err
}

// Verify an incoming API key
func verifyAPIKey(incoming string, storedHash []byte) bool {
    return bcrypt.CompareHashAndPassword(storedHash, []byte(incoming)) == nil
}
```

Store only `hash` in the database. The `plaintext` is shown once to the issuing user and then discarded. If a user loses their key, they generate a new one.

## E3 — Scoped keys [MUST]

```go
type APIKeyScope string
const (
    ScopePaymentRead    APIKeyScope = "payment:read"
    ScopePaymentCreate  APIKeyScope = "payment:create"
    ScopeWebhookReceive APIKeyScope = "webhook:receive"
)

type APIKeyRecord struct {
    Hash      []byte
    OwnerID   string
    Scopes    []APIKeyScope
    ExpiresAt *time.Time
    RevokedAt *time.Time
}
```

## E4 — Revocation [MUST]

```go
func (r *APIKeyRepo) Revoke(ctx context.Context, keyID string) error {
    now := time.Now().UTC()
    return r.db.ExecContext(ctx,
        "UPDATE api_keys SET revoked_at = $1 WHERE id = $2",
        now, keyID,
    )
}

// Validation: reject revoked keys before checking the hash (fast path)
func (s *Service) ValidateAPIKey(ctx context.Context, incoming string) (APIKeyRecord, error) {
    // prefix lookup to find candidate keys without a full-table bcrypt scan
    // ...
    if key.RevokedAt != nil {
        return APIKeyRecord{}, ErrRevoked
    }
    if key.ExpiresAt != nil && time.Now().After(*key.ExpiresAt) {
        return APIKeyRecord{}, ErrExpired
    }
    if !verifyAPIKey(incoming, key.Hash) {
        return APIKeyRecord{}, ErrInvalidKey
    }
    return key, nil
}
```

## Common findings

1. API key stored in plaintext in the database — a DB read returns live credentials.
2. No scoping — one key grants access to everything.
3. No expiry or revocation — a leaked key is valid forever.
4. API key stored in `localStorage` on the client — XSS-accessible.
5. bcrypt used for API key verification on every request — bcrypt is intentionally slow; use HMAC-SHA256 for high-frequency verification, bcrypt only for initial validation.
