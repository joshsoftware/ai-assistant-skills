# A. Directory Layout

Expands rules A1\u2013A8. These are community conventions, not a Go standard.

## Reference layout (medium service)

```
myservice/
\u251c\u2500\u2500 cmd/
\u2502   \u251c\u2500\u2500 api/
\u2502   \u2502   \u2514\u2500\u2500 main.go          # API server entry point
\u2502   \u2514\u2500\u2500 worker/
\u2502       \u2514\u2500\u2500 main.go          # background worker entry point
\u251c\u2500\u2500 internal/
\u2502   \u251c\u2500\u2500 config/              # typed config struct + loader
\u2502   \u251c\u2500\u2500 platform/            # shared infra: db pool, logger, http client
\u2502   \u251c\u2500\u2500 payment/             # a domain
\u2502   \u2502   \u251c\u2500\u2500 handler.go       # HTTP/transport layer
\u2502   \u2502   \u251c\u2500\u2500 service.go       # business logic
\u2502   \u2502   \u251c\u2500\u2500 repository.go    # data access
\u2502   \u2502   \u2514\u2500\u2500 model.go         # domain types
\u2502   \u2514\u2500\u2500 ledger/              # another domain
\u2502       \u251c\u2500\u2500 handler.go
\u2502       \u251c\u2500\u2500 service.go
\u2502       \u251c\u2500\u2500 repository.go
\u2502       \u2514\u2500\u2500 model.go
\u251c\u2500\u2500 pkg/                     # only if something is genuinely reusable
\u2502   \u2514\u2500\u2500 money/               # e.g. a money type other repos may import
\u251c\u2500\u2500 api/                     # OpenAPI / protobuf contract files
\u251c\u2500\u2500 migrations/              # DB migration files
\u251c\u2500\u2500 configs/                 # config templates / examples
\u251c\u2500\u2500 scripts/                 # build / ops scripts
\u251c\u2500\u2500 deployments/             # deployment manifests
\u251c\u2500\u2500 go.mod
\u251c\u2500\u2500 go.sum
\u251c\u2500\u2500 Makefile
\u2514\u2500\u2500 README.md
```

## Reference layout (small service)

Do not over-structure. A small service is fine as:

```
myservice/
\u251c\u2500\u2500 main.go
\u251c\u2500\u2500 handler.go
\u251c\u2500\u2500 service.go
\u251c\u2500\u2500 store.go
\u251c\u2500\u2500 go.mod
\u251c\u2500\u2500 go.sum
\u2514\u2500\u2500 README.md
```

Add `cmd/`, `internal/`, and domain subdirectories only as the service grows and the root gets busy.

## A1 \u2014 `cmd/<binary>/` for executables [SHOULD]

Each executable gets its own subdirectory whose name is the binary name. The `main` package lives there. A repository producing an API server and a worker has `cmd/api/` and `cmd/worker/`.

## A2 \u2014 `internal/` for non-importable code [SHOULD]

Code under `internal/` cannot be imported by packages outside the parent of `internal/`. The Go toolchain enforces this at compile time. For a BFSI service, almost all business code belongs in `internal/` \u2014 it is not meant to be a public library.

## A3 \u2014 `pkg/` only when reuse is real [MAY]

`pkg/` signals "other repositories may import this." If nothing in the service is meant for external reuse, do not create `pkg/` \u2014 an empty or speculative `pkg/` is noise. A money type or a shared client SDK is a legitimate `pkg/` candidate.

## A4 \u2014 Group by domain, not by layer [SHOULD]

Prefer:
```
internal/payment/{handler,service,repository,model}.go
internal/ledger/{handler,service,repository,model}.go
```
Over:
```
internal/handlers/{payment,ledger}.go
internal/services/{payment,ledger}.go
internal/repositories/{payment,ledger}.go
```

Domain grouping keeps everything about "payment" in one place, makes ownership clear, and naturally limits how much one domain reaches into another. Layer grouping scatters a single feature across many directories.

## A5 \u2014 File split within a domain [SHOULD]

- `handler.go` \u2014 transport concerns: decode request, call service, encode response. No business logic.
- `service.go` \u2014 business logic. No HTTP, no SQL.
- `repository.go` / `store.go` \u2014 data access. No business logic.
- `model.go` \u2014 domain types.

This separation lets each layer be tested independently and lets the data layer be swapped.

## A6 \u2014 Thin `main.go` [MUST]

`main.go` wires dependencies and starts the process. Business logic in `main` cannot be unit-tested and cannot be reused. See `references/B-entrypoint-bootstrap.md`.

## A7 \u2014 Supporting directories [MAY]

`api/`, `configs/`, `migrations/`, `scripts/`, `deployments/` \u2014 add each only when there is content for it.

## A8 \u2014 Uncluttered root [SHOULD]

For a multi-package service, the root holds `go.mod`, `go.sum`, `README.md`, build config, and directories \u2014 not a pile of `.go` files. (For a genuinely small single-package service, A8 does not apply; the flat layout above is fine.)

## Common findings

1. Layer-first directories (`handlers/`, `services/`, `repositories/`) on a service large enough that domain grouping would be clearer.
2. Business logic in `main.go`.
3. Speculative empty `pkg/`.
4. A domain package mixing HTTP decoding and SQL in the same file.
5. Code that should be in `internal/` placed where other repos can import it.
