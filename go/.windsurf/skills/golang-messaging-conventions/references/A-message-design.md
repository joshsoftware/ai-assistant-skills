# A. Message Design & Schema

Expands rules A1–A6.

## A1 + A6 — Versioned envelope [MUST/SHOULD]

```go
type Envelope struct {
    MessageID     string          `json:"message_id"`      // A3: crypto/rand, unique
    EventType     string          `json:"event_type"`      // e.g. "payment.created.v1"
    SchemaVersion int             `json:"schema_version"`  // A1: stable version
    SourceService string          `json:"source_service"`
    ProducedAt    time.Time       `json:"produced_at"`     // UTC
    Data          json.RawMessage `json:"data"`            // A2: self-contained payload
}

// Generate a unique message ID
func newMessageID() string {
    b := make([]byte, 16)
    if _, err := rand.Read(b); err != nil {
        panic(fmt.Sprintf("newMessageID: crypto/rand: %v", err))
    }
    return "msg_" + base64.RawURLEncoding.EncodeToString(b)
}
```

Consumers receiving an unknown `schema_version` forward to DLQ with a `UNKNOWN_SCHEMA` reason rather than panicking or silently discarding.

## A4 — No PII in payloads [MUST]

```go
// WRONG — full account number in message
type PaymentCreatedData struct {
    AccountNumber string `json:"account_number"` // plain PII
    Amount        int64  `json:"amount_minor"`
}

// RIGHT — tokenised reference
type PaymentCreatedData struct {
    AccountToken  string `json:"account_token"`  // resolves to account in the data store
    Amount        int64  `json:"amount_minor"`
    Currency      string `json:"currency"`
}
```

Message queues often have:
- Multiple consumers (some may not need PII).
- Logs of message content for debugging.
- Replayed messages to new consumers.
- Retention periods longer than the PII's intended lifetime.

---

# B. Producer Patterns

Expands rules B1–B3.

## B1 — Transactional outbox [MUST]

The outbox pattern ensures a message is published if and only if the DB transaction commits:

```go
// Within the same database transaction:
func (s *Service) CreatePayment(ctx context.Context, req Request) (Payment, error) {
    tx, _ := s.db.BeginTx(ctx, nil)
    defer tx.Rollback(ctx)

    p := Payment{ID: newID(), ...}
    if err := s.repo.Save(ctx, tx, p); err != nil {
        return Payment{}, err
    }

    // Write to outbox table — same transaction, same commit
    msg := newEnvelope("payment.created.v1", PaymentCreatedData{...})
    if err := s.outbox.Enqueue(ctx, tx, msg); err != nil {
        return Payment{}, err
    }

    tx.Commit(ctx)
    return p, nil
}
// A background worker reads from the outbox table and publishes to the broker
// Once published, it marks the outbox record as sent
```

Without the outbox: the payment is saved but the publish fails → downstream services never know. Or: the publish succeeds but the DB commit fails → downstream processes a payment that was rolled back.

## B3 — Context timeout on publish [MUST]

```go
publishCtx, cancel := context.WithTimeout(ctx, 5*time.Second)
defer cancel()
if err := s.publisher.Publish(publishCtx, msg); err != nil {
    return fmt.Errorf("publish: %w", err)
}
```

---

# C. Consumer Patterns

Expands rules C1–C5.

## C1 + C2 — Idempotent consumer with deduplication [MUST]

```go
func (c *Consumer) handlePaymentCreated(ctx context.Context, msg Envelope) error {
    // Deduplication: check if message_id was already processed
    processed, err := c.dedup.IsProcessed(ctx, msg.MessageID)
    if err != nil { return err } // transient DB error — will retry
    if processed {
        slog.InfoContext(ctx, "duplicate message skipped",
            slog.String("message_id", msg.MessageID))
        return nil // ack it — already done
    }

    var data PaymentCreatedData
    if err := json.Unmarshal(msg.Data, &data); err != nil {
        return fmt.Errorf("%w: %w", ErrPermanent, err) // D1: permanent failure → DLQ
    }

    if err := c.ledger.PostCredit(ctx, data); err != nil {
        return err // transient if ledger is down → retry
    }

    // Mark as processed — only after successful business logic
    return c.dedup.MarkProcessed(ctx, msg.MessageID, 24*time.Hour)
}
```

## C5 — Worker pool [SHOULD]

```go
type ConsumerPool struct {
    worker    int
    queue     chan Envelope
    wg        sync.WaitGroup
}

func (p *ConsumerPool) Run(ctx context.Context, handler HandlerFunc) {
    for i := 0; i < p.workers; i++ {
        p.wg.Add(1)
        go func() {
            defer p.wg.Done()
            for {
                select {
                case msg, ok := <-p.queue:
                    if !ok { return }
                    if err := handler(ctx, msg); err != nil {
                        // route to DLQ or retry — see D
                    }
                case <-ctx.Done():
                    return
                }
            }
        }()
    }
}
```

---

# D. Error Handling, Retries & DLQ

Expands rules D1–D5.

## D1 + D2 — Error classification [MUST]

```go
// Sentinel errors for classification
var (
    ErrPermanent  = errors.New("permanent failure")  // → DLQ immediately
    ErrTransient  = errors.New("transient failure")  // → retry with backoff
)

func handleMessage(ctx context.Context, msg Envelope) error {
    var data PaymentData
    if err := json.Unmarshal(msg.Data, &data); err != nil {
        // Malformed JSON will never succeed — permanent
        return fmt.Errorf("%w: unmarshal: %v", ErrPermanent, err)
    }
    if err := processPayment(ctx, data); err != nil {
        if isTimeoutOrUnavailable(err) {
            return fmt.Errorf("%w: %v", ErrTransient, err)
        }
        return fmt.Errorf("%w: %v", ErrPermanent, err)
    }
    return nil
}
```

## D5 — DLQ message with error metadata [SHOULD]

```go
type DLQEnvelope struct {
    Original   Envelope  // full original message
    ErrorReason string   // human-readable
    RetryCount  int
    FailedAt    time.Time
}
```

---

# E. Graceful Shutdown

Expands rules E1–E3.

## E1 + E2 — Stop pulling, drain in-flight [MUST]

```go
func (c *Consumer) Run(ctx context.Context) error {
    for {
        select {
        case <-ctx.Done():
            // Stop pulling — wait for in-flight handlers to complete
            c.wg.Wait()
            return nil
        default:
        }
        msgs, err := c.broker.Receive(ctx, maxBatch)
        if err != nil {
            if ctx.Err() != nil { return nil }
            continue
        }
        for _, msg := range msgs {
            c.wg.Add(1)
            go func(m Message) {
                defer c.wg.Done()
                defer m.Ack() // or Nack on error
                c.handle(ctx, m)
            }(msg)
        }
    }
}
```

On `SIGTERM`, the orchestrator cancels `ctx`. The consumer stops receiving, waits for in-flight handlers (bounded by a separate timeout), then exits. Kafka offsets are not committed after ctx cancellation (E3).

---

# F. Broker Abstraction

Expands rules F1–F3.

## F1 + F2 — Broker-agnostic interface [SHOULD]

```go
type Publisher interface {
    Publish(ctx context.Context, topic string, msg Envelope) error
}

type Consumer interface {
    Subscribe(ctx context.Context, topic string, handler HandlerFunc) error
}

type HandlerFunc func(ctx context.Context, msg Envelope) error
```

Unit tests supply a fake publisher that stores messages in a slice. Integration tests use a real broker. Business logic never imports Kafka or SQS packages — it depends on the interface.

## Common findings

1. No outbox pattern — publish happens outside the DB transaction, causing lost events on crash.
2. Consumer processes the message then acks — a crash after processing but before ack causes re-delivery of an already-processed message with no deduplication check.
3. Ack-before-processing — a crash after ack but before completion loses the message.
4. PII in message payload — account numbers, PAN in Kafka topics that multiple consumers read.
5. Unbounded goroutine-per-message — a spike in message volume exhausts goroutines.
6. DLQ not monitored — failed messages accumulate silently.
7. Consumer has no graceful shutdown — in-flight messages are abandoned on deploy.
