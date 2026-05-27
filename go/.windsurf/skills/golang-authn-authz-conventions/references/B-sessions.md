# B. Session Management

Expands rules B1–B5. See also `golang-bfsi-bindings` go-A-authn-session.md for the BFSI-specific patterns (CSPRNG for session IDs, cookie flags, JWT verification).

## B1 + B3 — CSPRNG session ID, rotate on login [MUST]

```go
func newSessionID() (string, error) {
    b := make([]byte, 32) // 256 bits
    if _, err := rand.Read(b); err != nil {
        return "", fmt.Errorf("crypto/rand: %w", err)
    }
    return base64.RawURLEncoding.EncodeToString(b), nil
}

// On login: discard the pre-auth session, issue a new one
func (s *Service) Login(ctx context.Context, w http.ResponseWriter, creds Credentials) error {
    user, err := s.authenticate(ctx, creds)
    if err != nil { return err }

    // Invalidate any existing session for this browser
    oldSID := sessionIDFromCookie(w, r)
    if oldSID != "" { s.sessionStore.Delete(ctx, oldSID) }

    // Issue a new session ID post-authentication
    newSID, _ := newSessionID()
    s.sessionStore.Set(ctx, newSID, Session{UserID: user.ID}, 15*time.Minute)
    setSessionCookie(w, newSID)
    return nil
}
```

## B2 — Secure cookie flags [MUST]

```go
func setSessionCookie(w http.ResponseWriter, sid string) {
    http.SetCookie(w, &http.Cookie{
        Name:     "sid",
        Value:    sid,
        Path:     "/",
        Secure:   true,           // HTTPS only
        HttpOnly: true,           // not accessible by JavaScript
        SameSite: http.SameSiteStrictMode,
        MaxAge:   900,            // 15 min — hint to browser; server enforces separately
    })
}
```

## B4 + B5 — Server-side invalidation and timeouts [MUST]

The session store (Redis, database) is the source of truth. Cookie deletion is a hint to the browser:

```go
func (s *Service) Logout(ctx context.Context, sid string, w http.ResponseWriter) {
    s.sessionStore.Delete(ctx, sid) // server-side invalidation — this is what counts
    http.SetCookie(w, &http.Cookie{Name: "sid", MaxAge: -1}) // clear browser cookie
}
```

Inactivity timeout enforced server-side:
```go
type Session struct {
    UserID    string
    CreatedAt time.Time
    LastSeen  time.Time
}

func (s *Session) IsExpired(inactivityLimit, absoluteLimit time.Duration) bool {
    return time.Since(s.LastSeen) > inactivityLimit ||
           time.Since(s.CreatedAt) > absoluteLimit
}
```

---

# C. OAuth2 & OIDC Integration

Expands rules C1–C5.

## C1 — PKCE for all authorisation-code flows [MUST]

```go
import "golang.org/x/oauth2"

// Generate a code verifier and challenge (PKCE)
verifier := oauth2.GenerateVerifier()

// Store verifier in the session before redirecting
session.Set("pkce_verifier", verifier)

// Redirect with code_challenge
url := oauthConfig.AuthCodeURL(state,
    oauth2.AccessTypeOffline,
    oauth2.S256ChallengeOption(verifier),
)
http.Redirect(w, r, url, http.StatusFound)

// On callback: exchange with the verifier
token, err := oauthConfig.Exchange(ctx, code, oauth2.VerifierOption(verifier))
```

`golang.org/x/oauth2` handles PKCE; use it rather than building the code-verifier/challenge manually.

## C2 — Validate `state` on callback [MUST]

```go
func handleCallback(w http.ResponseWriter, r *http.Request) {
    returnedState := r.URL.Query().Get("state")
    expectedState := getStateFromSession(r)
    if !hmac.Equal([]byte(returnedState), []byte(expectedState)) {
        http.Error(w, "invalid state", http.StatusBadRequest)
        return
    }
    // ... proceed with token exchange
}
```

The `state` value must be a random, per-request nonce tied to the session. Without it, an attacker can initiate an OAuth2 flow on the victim's behalf (CSRF).

---

# D. Authorisation & RBAC

Expands rules D1–D5.

## D1 + D2 — Server-side, least-privilege [MUST]

```go
// Context key for caller identity
type callerKey struct{}

func callerFromContext(ctx context.Context) Caller {
    c, _ := ctx.Value(callerKey{}).(Caller)
    return c
}

// Handler checks server-side
func (h *Handler) CreatePayment(w http.ResponseWriter, r *http.Request) {
    caller := callerFromContext(r.Context())
    if !caller.HasPermission("payment:create") {
        writeError(w, r, ErrForbidden)
        return
    }
    // ...
}
```

## D3 — Object-level entitlement [MUST]

```go
acc, err := h.repo.FindByID(ctx, id)
if err != nil { /* handle */ }
if !caller.CanAccessAccount(acc) {
    // Return 404, not 403 — don't reveal the object exists
    writeError(w, r, ErrNotFound)
    return
}
```

## D4 — Data-driven roles and permissions [SHOULD]

```go
// Roles stored in DB, not hardcoded
type Permission struct {
    Role     string
    Resource string
    Action   string
}

func (c Caller) HasPermission(resource, action string) bool {
    for _, p := range c.Permissions {
        if p.Resource == resource && p.Action == action { return true }
    }
    return false
}
```

Hardcoded `if role == "admin"` branches cannot be updated without a deploy and accumulate quietly over years into a security tangle.

---

# E. Auth Middleware Patterns

Expands rules E1–E5.

## E1 + E2 — Identity in context, not re-parsed in handlers [MUST]

```go
// Middleware — runs once, places identity in context
func AuthMiddleware(next http.Handler) http.Handler {
    return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
        raw := extractBearerToken(r)
        caller, err := validateAndParseCaller(raw)
        if err != nil {
            writeError(w, r, ErrUnauthorized)
            return
        }
        ctx := context.WithValue(r.Context(), callerKey{}, caller)
        next.ServeHTTP(w, r.WithContext(ctx))
    })
}
```

Handlers read `callerFromContext(r.Context())` — they never touch the `Authorization` header directly.

## E3 — 401 vs 403 [MUST]

- `401 Unauthorized` — no valid identity (missing token, expired, invalid signature).
- `403 Forbidden` — valid identity, insufficient permission.

Returning `401` for a permission failure confuses the client into retrying with different credentials when the problem is the role, not the credentials.

## E4 + E5 — Separate authn from authz [SHOULD]

```go
// Authentication: who are you?
mux = Chain(mux, AuthMiddleware)

// Authorisation: per-route role enforcement
mux.Handle("POST /payments", RequireRole("payment:create")(createPaymentHandler))
mux.Handle("GET /admin/users", RequireRole("admin")(adminUsersHandler))
```

`RequireRole` reads the caller from context (already authenticated) and checks the role. It does not re-validate the token — that already happened in `AuthMiddleware`.

## Common findings

1. Role check inside a handler (`if r.Header.Get("X-Role") == "admin"`) — client-controlled.
2. JWT `alg` header trusted for algorithm selection.
3. `401` returned for a permission failure (should be `403`).
4. Refresh token stored in `localStorage` — XSS-accessible.
5. Logout clears the cookie but leaves the server-side session valid.
6. All route-level permission checks hardcoded in handlers rather than in middleware.
