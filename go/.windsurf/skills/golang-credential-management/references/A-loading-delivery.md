# A. Secret Loading & Delivery

Expands rules A1–A5.

## A1 — No secrets in source [MUST]

```go
// ALL WRONG
const dbPassword = "super-secret-pw"
var apiKey = "sk_live_abc123"
os.Setenv("JWT_SECRET", "hardcoded-value")

// WRONG — committed default that looks like a placeholder but is a real secret
dsn := getenvOr("DB_DSN", "postgres://app:password@db/payments")
```

Secrets committed to source are in the repository history forever, even after deletion. Every developer, contractor, and GitHub Actions runner that ever cloned the repo has seen them.

Scan the repo and its history for secrets before and after every merge: git-secrets, truffleHog, or gitleaks as CI gates.

## A2 — Runtime delivery from a secret store [MUST]

The environment variable is the *interface* between the secret store and the application — not the storage for the secret.

**Correct delivery chain:**
```
Vault / AWS Secrets Manager
    → injected by orchestrator (k8s ExternalSecrets, Vault Agent Injector)
        → environment variable or in-pod file
            → os.Getenv / os.ReadFile at startup
                → typed config struct (loaded once, validated, passed down)
```

The application code only reads `os.Getenv`. The value of that variable is the concern of the deployment pipeline and the secret store, not the application.

**Docker / Kubernetes anti-patterns to avoid:**
```yaml
# WRONG — secret literal in deployment manifest (checked into git)
env:
  - name: DB_PASSWORD
    value: "super-secret-pw"

# RIGHT — reference to a k8s Secret or ExternalSecret
env:
  - name: DB_PASSWORD
    valueFrom:
      secretKeyRef:
        name: payments-db-creds
        key: password
```

## A3 — Fail fast on missing secrets [MUST]

```go
func (c *Config) validate() error {
    var missing []string
    if c.DB.DSN == "" {
        missing = append(missing, "DB_DSN")
    }
    if c.Auth.JWTSecret == "" {
        missing = append(missing, "JWT_SECRET")
    }
    if len(missing) > 0 {
        return fmt.Errorf("required secrets not set: %s", strings.Join(missing, ", "))
    }
    return nil
}
```

A service that starts with a missing secret fails silently on every request that needs it, rather than immediately and loudly at startup. Fail fast with a named list of missing keys so the ops team can fix the deployment, not debug a running service.

## A4 — Per-environment secrets [MUST]

```
vault kv put secret/payments/dev     db_dsn="..." jwt_secret="..."
vault kv put secret/payments/staging db_dsn="..." jwt_secret="..."
vault kv put secret/payments/prod    db_dsn="..." jwt_secret="..."
```

The production Vault path is accessible only from production workload identities. A developer with access to the dev path cannot read production secrets. This is enforced in the secret store's access policy, not in the application.

## A5 — Secrets never logged [MUST]

See `references/B-secret-types.md` for the typed wrapper that enforces this.

## Common findings

1. `.env` file committed to the repository with real secrets.
2. `getenvOr("KEY", "<real-value>")` — the default is a hardcoded secret.
3. Secret passed as a command-line argument — visible in `ps aux` and shell history.
4. Deployment manifest has literal secret values — committed to the gitops repository.
5. Missing secret discovered only when the first request fails, not at startup.
