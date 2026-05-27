# C. Dependency Wiring & Configuration

Expands rules C1\u2013C6.

## C1 + C2 + C3 \u2014 Typed config, loaded once, validated [MUST/SHOULD]

```go
package config

type Config struct {
    Addr      string        // HTTP listen address
    LogLevel  string
    DB        DBConfig
    Timeouts  TimeoutConfig
}

type DBConfig struct {
    DSN             string
    MaxOpenConns    int
    MaxIdleConns    int
    ConnMaxLifetime time.Duration
}

func Load() (*Config, error) {
    cfg := &Config{
        Addr:     getenv("HTTP_ADDR", ":8443"),
        LogLevel: getenv("LOG_LEVEL", "info"),
        DB: DBConfig{
            DSN:          os.Getenv("DB_DSN"), // required, no default
            MaxOpenConns: getenvInt("DB_MAX_OPEN_CONNS", 25),
        },
    }
    if err := cfg.validate(); err != nil {
        return nil, err
    }
    return cfg, nil
}

func (c *Config) validate() error {
    if c.DB.DSN == "" {
        return errors.New("DB_DSN is required")
    }
    if c.DB.MaxOpenConns <= 0 {
        return errors.New("DB_MAX_OPEN_CONNS must be positive")
    }
    return nil
}
```

Rules embodied here:
- Config comes from the environment (C1, Twelve-Factor). Note: a secret like `DB_DSN` should itself be delivered from a secret store \u2014 see the credential-management skill; do not commit a real default.
- It is validated at startup; a missing `DB_DSN` fails fast with a message naming the key (C2).
- It is loaded once into a typed struct (C3). The rest of the codebase receives the struct, never calls `os.Getenv` directly.

## C4 \u2014 Constructors over singletons [SHOULD]

```go
// payment/service.go
type Service struct {
    repo   Repository
    logger *slog.Logger
}

func NewService(repo Repository, logger *slog.Logger) *Service {
    return &Service{repo: repo, logger: logger}
}
```

A package-level `var DB *sql.DB` initialised in `init()` is hidden global state: it cannot be substituted in a test, and the dependency is invisible at the call site. An explicit constructor makes the dependency a parameter.

## C5 \u2014 Interfaces at the point of use [SHOULD]

Define the interface in the package that *consumes* it, listing only the methods that consumer needs:

```go
// payment/service.go \u2014 the consumer declares what it needs
type Repository interface {
    Save(ctx context.Context, p Payment) error
    FindByID(ctx context.Context, id string) (Payment, error)
}
```

The concrete implementation (e.g. a Postgres repository) does not need to know this interface exists; it just has the methods. A test supplies a fake that satisfies the same small interface. This is the Go idiom: "accept interfaces, return structs", with the interface owned by the consumer.

## C6 \u2014 DI containers [MAY]

For a large service with a deep dependency graph, a wiring tool or DI container is acceptable. For most services, manual wiring in `run()` (see B4) is clearer and has no magic. Do not reach for a container by default.

## Common findings

1. `os.Getenv` called in many packages instead of a single typed config loaded once.
2. Missing required config discovered only when the first request fails, not at startup.
3. Package-level `var DB *sql.DB` set in `init()` \u2014 untestable hidden global.
4. Interface declared in the implementation package, forcing the consumer to import it.
5. A heavyweight DI container on a small service that would be clearer with manual wiring.
