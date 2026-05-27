# B. README

Expands rules B1–B4.

## B2 — Required README sections [MUST]

```markdown
# payments-service

Payment initiation and reconciliation service for the ACME platform.
Processes UPI, IMPS, and NACH transactions. Regulated under RBI Master Direction
on Digital Payment Security Controls, 2021.

## Quick start

\`\`\`bash
cp .env.example .env       # fill in required secrets (see Environment variables)
go run ./cmd/api
\`\`\`

## Run tests

\`\`\`bash
go test ./...                  # unit tests
go test -race ./...            # with race detector (required in CI)
go test -tags=integration ./... # integration tests (requires Docker Compose)
\`\`\`

## Environment variables

| Variable | Required | Description |
|---|---|---|
| `DB_DSN` | Yes | PostgreSQL connection string (from Vault) |
| `JWT_SECRET` | Yes | HMAC signing key (from Vault) |
| `REDIS_ADDR` | Yes | Redis address |
| `HTTP_ADDR` | No | Listen address, default `:8443` |
| `LOG_LEVEL` | No | `debug`, `info`, `warn`, `error` — default `info` |

## Architecture

See [docs/decisions/](docs/decisions/) for ADRs.
See [api/openapi.yaml](api/openapi.yaml) for the API contract.

## Runbook

See [docs/runbook.md](docs/runbook.md) for deployment, rollback, and incident procedures.

## Regulatory scope

This service is covered by `bfsi-india-core` categories A, C, D, E, F, H, L, M.
CERT-In incident reporting: 6 hours from detection. Contact: oncall@acme.com.
```

The README is the entry point. It links to the runbook and ADRs rather than duplicating their content.

---

# C. OpenAPI / API Contract

Expands rules C1–C5.

## C1 + C2 — OpenAPI spec versioned with code [MUST]

```yaml
# api/openapi.yaml
openapi: "3.1.0"
info:
  title: Payments Service API
  version: "1.0.0"
  description: |
    Payment initiation and status API.
    Regulated under RBI Digital Payment Security Controls, 2021.
  contact:
    email: platform@acme.com

servers:
  - url: https://api.acme.com/v1

security:
  - BearerAuth: []

paths:
  /payments:
    post:
      summary: Initiate a payment
      operationId: createPayment
      parameters:
        - name: Idempotency-Key
          in: header
          required: true
          schema:
            type: string
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreatePaymentRequest'
      responses:
        '201':
          description: Payment created
          headers:
            Location:
              schema:
                type: string
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Payment'
        '400':
          $ref: '#/components/responses/BadRequest'
        '409':
          $ref: '#/components/responses/Conflict'
        '422':
          $ref: '#/components/responses/UnprocessableEntity'
```

## C3 — Document all error responses [MUST]

```yaml
components:
  responses:
    BadRequest:
      description: The request is malformed or missing required fields
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/ErrorResponse'
          example:
            error:
              code: VALIDATION_FAILED
              message: One or more fields are invalid
              correlation_id: req_abc123
              details:
                - field: amount
                  issue: must be greater than zero

  schemas:
    ErrorResponse:
      type: object
      required: [error]
      properties:
        error:
          type: object
          required: [code, message, correlation_id]
          properties:
            code:
              type: string
              enum: [VALIDATION_FAILED, PAYMENT_DECLINED, DUPLICATE_REQUEST,
                     LIMIT_EXCEEDED, NOT_FOUND, INTERNAL_ERROR]
            message:
              type: string
            correlation_id:
              type: string
```

Documenting the `code` enum in the spec means clients can generate typed error-handling code from it.

---

# D. Architecture Decision Records (ADRs)

Expands rules D1–D5.

## D3 — ADR template [SHOULD]

```markdown
# 0003 — Use Redis for session storage

**Status:** Accepted
**Date:** 2024-11-15

## Context

We need a session store for the authentication service. Sessions must survive
pod restarts (ruling out in-memory only), be shared across multiple API replicas,
and expire automatically after 15 minutes of inactivity (RBI requirement).

## Decision

Use Redis (AWS ElastiCache, single-shard, TLS enabled, AUTH required) as the
session store. Session keys are namespaced `payments:prod:session:<sid>` with a
TTL of 15 minutes, reset on each authenticated request.

## Alternatives considered

- **PostgreSQL**: durable but adds DB load for every request; no native TTL.
- **In-process sync.Map**: does not survive restarts or scale across replicas.

## Consequences

- Redis is now a critical dependency; a Redis outage affects all authenticated users.
- We must configure Redis TLS and AUTH per `golang-bfsi-bindings` rule A (session security).
- We accept the ~1ms added latency per authenticated request for the network hop.
```

---

# E. Runbooks

Expands rules E1–E4.

## E1 + E2 — Required runbook sections [MUST]

```markdown
# payments-service Runbook

## Deployment

1. Tag the release: `git tag v1.2.3 && git push --tags`
2. CI builds and pushes the signed image.
3. ArgoCD / Helm deployment: `helm upgrade payments ... --set image.tag=v1.2.3`
4. Verify health: `curl https://api.acme.com/v1/healthz`

## Rollback

1. `helm rollback payments` — reverts to the previous chart revision.
2. Confirm: check pod status, error rates in Grafana.

## Common failure modes

| Symptom | Likely cause | Resolution |
|---|---|---|
| `500` on all payment creates | DB connection pool exhausted | Check DB pool metrics; restart pod if pool is stuck |
| `401` on all requests | JWT secret rotation in progress | Wait for rotation window (5 min); check Vault lease |
| Redis connection refused | Redis restart / network partition | Cache falls through to DB; check Redis logs |

## Incident procedures (BFSI)

**CERT-In 6-hour report (mandatory):**
- Notify: cert-in@cert-in.org.in and security@acme.com
- Template: `docs/incidents/cert-in-report-template.md`
- On-call: +91-XXXXXXXXXX (primary), +91-XXXXXXXXXX (secondary)
- **Do not wait for root-cause analysis — report within 6 hours of detection.**

**RBI notification:**
- Contact: compliance@acme.com, who manages the RBI notification per the CCMP.

## Dashboards & logs

- Grafana: https://grafana.acme.com/d/payments
- Logs: https://logs.acme.com/payments
```

## Common findings

1. README with no instructions on how to run the service locally.
2. OpenAPI spec checked in once and never updated — diverges from the implementation.
3. Error responses in the spec are `{}` (empty schema) — clients cannot decode them.
4. No ADRs — "why was PostgreSQL chosen over MySQL?" has no answer.
5. Runbook exists but is linked nowhere — undiscoverable during an incident.
6. BFSI runbook missing the CERT-In 6-hour reporting procedure.
