# C. Versioning & Compatibility

Expands rules C1\u2013C5.

## C1 \u2014 The API is versioned [MUST]

Every API carries a version from day one. Retrofitting versioning onto an unversioned API in production is painful. Two accepted styles:

- **URL-path versioning** (default convention here): `/v1/payments`. Visible, simple to route, easy to run two versions side by side.
- **Header versioning**: `Accept: application/vnd.acme.v1+json`. Keeps URLs clean but is less visible and easier for clients to get wrong.

Pick one and apply it uniformly. URL-path versioning is the default recommendation because it is the most operationally transparent.

## C2 \u2014 Whole-number major versions [SHOULD]

Use `v1`, `v2`, `v3`. Do not put minor or patch numbers in the URL (`v1.2`, `v2.1.3`). Minor, backward-compatible evolution happens within a major version without a URL change.

## C3 \u2014 Backward-compatible changes only within a version [MUST]

Within `v1`, these are safe:
- Adding a new optional response field.
- Adding a new endpoint.
- Adding a new optional request parameter with a sensible default.
- Adding a new value to an enum \u2014 *only if* clients are documented to tolerate unknown values.

These are breaking and forbidden within a version:
- Removing or renaming a field.
- Changing a field's type or format.
- Changing the meaning of an existing field.
- Making an optional request field required.
- Removing an endpoint or an enum value.
- Tightening validation so previously-valid requests now fail.

## C4 \u2014 Breaking change means a new major version [SHOULD]

A breaking change is introduced as `v2`. `v1` keeps running through a documented deprecation window so clients can migrate. Communicate the deprecation date and, where possible, emit a `Deprecation` / `Sunset` header on `v1` responses.

## C5 \u2014 Reject unknown request fields [SHOULD]

```go
dec := json.NewDecoder(r.Body)
dec.DisallowUnknownFields()
```

If a client sends a field the server does not recognise, fail the request rather than silently ignoring it. Silent ignoring hides client/server drift \u2014 the client believes it sent something meaningful; the server discarded it. For a financial API, that silence can mean a parameter the client thought constrained a transaction was never applied.

## Common findings

1. No version in the API at all \u2014 the first breaking change has nowhere to go.
2. Minor/patch versions in the URL (`/v1.3/...`).
3. A field renamed within `v1`, breaking existing clients.
4. Validation tightened within a version, breaking previously-valid requests.
5. Unknown request fields silently ignored, hiding drift.
