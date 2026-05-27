# B. Entry Points & Bootstrap

Expands rules B1\u2013B6.

## B1 \u2014 `main` delegates to `run` [MUST]

```go
func main() {
    if err := run(); err != nil {
        fmt.Fprintf(os.Stderr, "fatal: %v\n", err)
        os.Exit(1)
    }
}

func run() error {
    ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
    defer stop()

    cfg, err := config.Load()
    if err != nil {
        return fmt.Errorf("load config: %w", err)
    }
    // ... wire dependencies, start server ...
    return nil
}
```

Why: `run` returning an `error` is testable; a `main` full of logic and `log.Fatal` calls is not. `main` becomes a trivial three-line wrapper.

## B2 \u2014 Signal-aware root context [MUST]

`signal.NotifyContext` returns a context cancelled when the process receives `SIGINT` or `SIGTERM`. This context is the parent of every request and worker context, so a shutdown signal propagates everywhere.

## B3 \u2014 Graceful shutdown [MUST]

```go
srv := &http.Server{Addr: cfg.Addr, Handler: handler}

go func() {
    if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
        // record the error; trigger shutdown
    }
}()

<-ctx.Done() // signal received

shutdownCtx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
defer cancel()
if err := srv.Shutdown(shutdownCtx); err != nil {
    return fmt.Errorf("graceful shutdown: %w", err)
}
```

`srv.Shutdown` stops accepting new connections and waits for in-flight requests to finish, up to the timeout. For BFSI services this is not optional \u2014 a deploy or scale-down event must not abandon a transaction mid-commit.

Background workers follow the same shape: on `ctx.Done()`, stop pulling new work, finish or checkpoint what is held, then return.

## B4 \u2014 Explicit dependency construction [SHOULD]

The bootstrap constructs each dependency once and injects it:

```go
func run() error {
    // ... ctx, cfg ...
    logger := platform.NewLogger(cfg.LogLevel)
    db, err := platform.NewDBPool(ctx, cfg.DB)
    if err != nil {
        return fmt.Errorf("db pool: %w", err)
    }
    defer db.Close()

    paymentRepo := payment.NewRepository(db)
    paymentSvc  := payment.NewService(paymentRepo, logger)
    paymentH    := payment.NewHandler(paymentSvc, logger)

    mux := http.NewServeMux()
    paymentH.Register(mux)
    // ... start server with mux ...
}
```

The dependency graph is visible in one place. No package-level singletons, no hidden `init()` wiring.

## B5 \u2014 Fail fast on startup [SHOULD]

If the DB pool cannot be created, a required secret cannot be fetched, or required config is missing, `run` returns an error and the process exits non-zero. A BFSI service must not start in a half-working state where some transactions silently fail.

## B6 \u2014 Non-zero exit on failure [MUST]

`os.Exit(1)` (or another non-zero code) on fatal failure. Orchestrators (Kubernetes, systemd) rely on the exit code to detect a crash and restart or alert.

## Common findings

1. Logic and `log.Fatal` scattered in `main` instead of a testable `run() error`.
2. No graceful shutdown \u2014 `SIGTERM` kills in-flight requests.
3. Root context not signal-aware, so shutdown does not propagate.
4. Service starts even though the DB is unreachable, then fails every request.
5. Fatal failure exits zero, so the orchestrator thinks the process ended cleanly.
