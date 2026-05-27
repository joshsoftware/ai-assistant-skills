# Sources & Rationale

This skill is **engineering conventions, not a standard.**

## What this skill draws on

- **Enterprise Integration Patterns** (Hohpe & Woolf, 2003) — Idempotent Consumer, Dead Letter Channel, Message Store, Transactional Client. These are the foundational patterns; the skill applies them to Go.
- **Apache Kafka documentation** — consumer groups, offset management, at-least-once vs exactly-once, idempotent producers.
- **AWS SQS/SNS documentation** — visibility timeout, FIFO queues, DLQ configuration.
- **RabbitMQ documentation** — acknowledgement modes, dead-letter exchanges.
- **GCP Pub/Sub documentation** — ack deadline, ordering keys, dead-letter topics.
- **BFSI context** — PII masking in messages (A4) binds `bfsi-india-core` rule E4; data residency for message broker deployments binds rule M1.

## Where the community varies

- **Exactly-once delivery** — Kafka supports it with transactional producers and consumers, at significant operational cost. For most BFSI use cases, at-least-once + idempotent consumer is simpler and sufficient.
- **Outbox vs transactional publish** — some brokers (Kafka with a Kafka-native DB) allow transactional publish within a single transaction. For services using a relational DB and a separate broker (the common case), the outbox table is the standard pattern.
- **Broker choice** — Kafka for high-throughput streaming and replay; SQS for managed, low-operational-overhead decoupling; RabbitMQ for routing and fanout; NATS for low-latency. The skill is broker-agnostic; patterns apply to all.

## Exactly-once note

"Exactly-once" is frequently claimed but rarely achievable end-to-end across broker + consumer + downstream database. In practice: the broker may provide exactly-once delivery guarantees, but the consumer's database write may still fail after the ack, causing a re-delivery. The idempotent-consumer pattern (deduplication via `message_id`) is the practical solution for BFSI workloads where duplicate payment processing is unacceptable.
