# D. Module Hygiene

Expands rules D1\u2013D6.

## D1 \u2014 Commit go.mod and go.sum [MUST]

Both files are committed. `go.sum` holds cryptographic checksums of every module version the build uses; it is the supply-chain integrity record. A repository without `go.sum` cannot produce a verifiable build.

## D2 \u2014 Correct module path [MUST]

The `module` line in `go.mod` matches the repository's canonical import path, e.g.:

```
module github.com/acme/payments-service
```

A mismatch breaks imports for anyone consuming the module and confuses tooling.

## D3 \u2014 Supported Go version [SHOULD]

The `go` directive declares the minimum Go version:

```
go 1.23
```

Keep it on a supported release. Upgrade deliberately \u2014 a version bump can change build behaviour and is a reviewable change, not an incidental one.

## D4 \u2014 `replace` directives need review [SHOULD]

A `replace` redirects a dependency to a fork, a fixed version, or a local path:

```
replace github.com/some/dep => github.com/ourfork/dep v1.2.3-patched
```

This silently changes what code is compiled. Every `replace` in a production module must be reviewed and justified \u2014 it is a supply-chain decision. Local-path `replace` directives must never reach a release build.

## D5 \u2014 Run `go mod tidy` before merge [SHOULD]

`go mod tidy` removes unused dependencies and adds missing ones, so `go.mod`/`go.sum` reflect the actual import graph. Run it before merging so the module files stay accurate and reviewable.

## D6 \u2014 Single vs multiple modules [MAY]

A repository containing several services is usually simplest as one module \u2014 one `go.mod`, one dependency set, one version. Multiple modules in one repository add coordination overhead (cross-module versioning, `replace` directives during development). Choose multiple modules only with a concrete reason, e.g. genuinely independent release cadences.

## Common findings

1. `go.sum` missing or git-ignored \u2014 no integrity record.
2. Module path not matching the repository location.
3. `go` directive on an unsupported, very old version.
4. A local-path `replace` left in `go.mod` and shipped.
5. `go.mod` listing dependencies the code no longer imports because `go mod tidy` was never run.
