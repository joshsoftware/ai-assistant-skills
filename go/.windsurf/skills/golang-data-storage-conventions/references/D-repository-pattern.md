# D. Repository Pattern

Expands rules D1–D5.

## D1 + D2 — Repository layer and consumer-owned interface [SHOULD]

```go
// payment/service.go — the consumer defines only what it needs
type PaymentRepository interface {
    Save(ctx context.Context, p Payment) error
    FindByID(ctx context.Context, id string) (Payment, error)
    ListByAccount(ctx context.Context, accountID string, page Page) ([]Payment, error)
}

// The service depends on the interface, not the concrete type
type Service struct {
    repo   PaymentRepository
    logger *slog.Logger
}
```

```go
// payment/postgres_repository.go — concrete implementation
type postgresRepository struct {
    db DBTX
}

func NewPostgresRepository(db DBTX) *postgresRepository {
    return &postgresRepository{db: db}
}

func (r *postgresRepository) Save(ctx context.Context, p Payment) error {
    _, err := r.db.ExecContext(ctx,
        `INSERT INTO payments (id, amount_minor, currency, account_id, created_at)
         VALUES ($1, $2, $3, $4, $5)`,
        p.ID, p.Amount.Minor, p.Amount.Currency, p.AccountID, p.CreatedAt.UTC(),
    )
    return err
}
```

The interface lives in `payment/service.go` (the consumer), not in `postgres_repository.go` (the implementation). The implementation does not know about the interface; it just has the methods. A test provides a fake:

```go
// payment/service_test.go
type fakeRepo struct {
    payments map[string]Payment
}
func (f *fakeRepo) Save(ctx context.Context, p Payment) error {
    f.payments[p.ID] = p; return nil
}
// etc.
```

## D3 — Intent-based method names [SHOULD]

Name methods by what they do for the domain, not by the SQL behind them:

| ✓ | ✗ |
|---|---|
| `Save` | `Insert`, `Upsert` (exposes SQL verb) |
| `FindByID` | `SelectByID` |
| `ListByAccount` | `SelectWhereAccountID` |
| `Delete` | `DeleteRow` |

The repository is a domain concept; its surface should read as domain language. If the underlying SQL changes from INSERT to UPSERT, the interface stays the same.

## D4 — context.Context as first parameter [MUST]

```go
// Every repository method takes ctx as the first parameter
func (r *postgresRepository) FindByID(ctx context.Context, id string) (Payment, error) {
    var p Payment
    row := r.db.QueryRowContext(ctx,
        "SELECT id, amount_minor, currency, status FROM payments WHERE id = $1", id)
    if err := row.Scan(&p.ID, &p.Amount.Minor, &p.Amount.Currency, &p.Status); err != nil {
        if errors.Is(err, sql.ErrNoRows) {
            return Payment{}, ErrNotFound
        }
        return Payment{}, fmt.Errorf("scan payment: %w", err)
    }
    return p, nil
}
```

This is how the request context (with its cancellation and deadline) propagates down to the database call. A repository method without `ctx` is incompatible with the rest of the stack.

## D5 — ORM usage [MAY]

ORMs are acceptable. Key points when using one:

1. **Understand the generated SQL.** Log or print queries during development; production surprises from ORM-generated SQL are real.
2. **Parameterisation holds.** ORMs that let you pass a raw SQL string must still use parameterised values. "Raw" escape hatches are fine; interpolating user input into them is not.
3. **Context must thread through.** Prefer the ORM's context-aware query methods. A context-unaware ORM call is a problem; most modern Go ORMs support context.
4. **N+1 query awareness.** ORMs that load associations lazily can produce one query per row. Audit hot paths and use eager loading / joins where the N+1 pattern would be expensive.

## Common findings

1. SQL queries scattered in service and handler code — no repository boundary.
2. Interface defined in the repository package, forcing all consumers to import it.
3. Interface lists every method the repository has rather than only what the consumer needs — making test fakes needlessly large.
4. Repository methods without `ctx` — no cancellation or timeout propagation.
5. ORM's raw-query method used with string concatenation — ORM gives no protection here.
