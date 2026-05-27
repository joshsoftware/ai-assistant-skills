# A. JWT Issuance & Validation

Expands rules A1–A7.

## A1 + A2 — Vetted library, pinned algorithm [MUST]

```go
import "github.com/golang-jwt/jwt/v5"

// WRONG — trusts whatever algorithm the token header claims
token, err := jwt.Parse(raw, func(t *jwt.Token) (any, error) {
    return secretKey, nil // alg: none attack possible
})

// RIGHT — pin the expected algorithm; reject anything else
token, err := jwt.Parse(raw,
    func(t *jwt.Token) (any, error) {
        if _, ok := t.Method.(*jwt.SigningMethodHMAC); !ok {
            return nil, fmt.Errorf("unexpected signing method: %v", t.Header["alg"])
        }
        return secretKey, nil
    },
    jwt.WithValidMethods([]string{"HS256"}), // explicit allow-list
)
```

For asymmetric (RS256/ES256):
```go
token, err := jwt.Parse(raw,
    func(t *jwt.Token) (any, error) {
        if _, ok := t.Method.(*jwt.SigningMethodECDSA); !ok {
            return nil, fmt.Errorf("unexpected signing method: %v", t.Header["alg"])
        }
        return publicKey, nil
    },
    jwt.WithValidMethods([]string{"ES256"}),
)
```

## A3 — Validate all standard claims [MUST]

```go
token, err := jwt.ParseWithClaims(raw, &jwt.RegisteredClaims{},
    keyFunc,
    jwt.WithValidMethods([]string{"ES256"}),
    jwt.WithIssuer("https://auth.acme.com"),       // iss check
    jwt.WithAudience("payments-service"),           // aud check
    jwt.WithExpirationRequired(),                   // exp must be present
    jwt.WithIssuedAt(),                             // iat must not be in the future
)
if err != nil {
    return nil, fmt.Errorf("invalid token: %w", err)
}
```

A token with a valid signature but an expired `exp`, wrong `iss`, or wrong `aud` must be rejected. The library must enforce these — do not only check the signature.

## A4 — Short access token lifetimes [MUST]

```go
claims := jwt.RegisteredClaims{
    Issuer:    "https://auth.acme.com",
    Subject:   userID,
    Audience:  jwt.ClaimStrings{"payments-service"},
    ExpiresAt: jwt.NewNumericDate(time.Now().Add(10 * time.Minute)), // short
    IssuedAt:  jwt.NewNumericDate(time.Now()),
    ID:        newTokenID(), // jti — unique per token, used for revocation
}
```

10–15 minutes for user-facing APIs. The short lifetime limits the damage if a token is leaked — it expires before most attack chains complete. Pair with a refresh token for UX continuity.

## A5 — Revocation list [MUST]

```go
// On logout: add the token's jti to a Redis blocklist with a TTL = token's remaining lifetime
func (s *AuthService) Logout(ctx context.Context, tokenID string, expiry time.Time) error {
    ttl := time.Until(expiry)
    if ttl <= 0 { return nil } // already expired
    return s.redis.Set(ctx, "revoked:"+tokenID, "1", ttl).Err()
}

// In JWT validation middleware: check the blocklist
func (m *AuthMiddleware) isRevoked(ctx context.Context, tokenID string) bool {
    val, err := m.redis.Get(ctx, "revoked:"+tokenID).Result()
    return err == nil && val == "1"
}
```

## A6 — No sensitive data in claims [MUST]

```go
// WRONG — password, PII in claims
claims := CustomClaims{
    Password: user.Password, // catastrophic
    Aadhaar:  user.Aadhaar,  // compliance violation
}

// RIGHT — only the identifier and authorisation-relevant claims
claims := CustomClaims{
    RegisteredClaims: jwt.RegisteredClaims{Subject: userID, ...},
    Roles:            []string{"payment-initiator"},
    TenantID:         tenantID,
}
```

## A7 — Asymmetric signing for cross-service tokens [SHOULD]

If a token issued by service A is consumed by service B, use RS256/ES256/EdDSA. Service B needs only the public key to verify — it does not need the private signing key. Distributing a shared HS256 secret to multiple services is harder to manage and compromises all services if one leaks it.

## Common findings

1. JWT library used without pinning the algorithm — `alg: none` attack possible.
2. `exp`, `iss`, and `aud` not validated — token accepted after expiry or from a different issuer.
3. No revocation mechanism — logout does not invalidate the token.
4. Refresh token stored in `localStorage` — readable by JavaScript, vulnerable to XSS.
5. PII in JWT claims — leaked via MITM or access to log files that record tokens.
