# A. Resource Design & URLs

Expands rules A1\u2013A5. Community REST practice, adapted to Go.

## A1 \u2014 Plural nouns, not verbs [SHOULD]

```
GET    /payments            list payments
POST   /payments            create a payment
GET    /payments/{id}       fetch one payment
PATCH  /payments/{id}       update a payment
DELETE /payments/{id}       delete a payment
```

Not:
```
GET  /getPayments
POST /createPayment
POST /deletePaymentById
```

The HTTP method already carries the verb. The path names the thing.

## A2 \u2014 Shallow nesting [SHOULD]

One level of nesting expresses ownership clearly:
```
GET /accounts/{id}/transactions     transactions belonging to an account
```

Beyond one level the URL becomes hard to read and route:
```
GET /accounts/{id}/transactions/{txnId}/disputes/{dId}/messages   # too deep
```

Prefer a top-level resource with a filter:
```
GET /messages?dispute_id={dId}
GET /disputes/{dId}                 # dispute as its own top-level resource
```

## A3 \u2014 Consistent path style [SHOULD]

Pick one convention for multi-word resources and apply it everywhere. Common choice: lowercase with hyphens (`/payment-mandates`). The specific choice matters less than consistency across the whole API.

## A4 \u2014 Opaque identifiers [MUST]

A client treats an ID as an opaque token. For resources exposing customer or financial data, prefer non-sequential identifiers (UUIDs) so an attacker cannot enumerate (`/accounts/1001`, `/accounts/1002`, ...). Note: a non-sequential ID is defence in depth \u2014 the actual control is the server-side ownership check (`golang-bfsi-bindings` rule B5). Do both.

## A5 \u2014 Actions as sub-resources [SHOULD]

Some operations are not a clean CRUD verb on a resource. Model them as a sub-resource rather than an RPC verb in the path:

```
POST /payments/{id}/reversals       reverse a payment
POST /accounts/{id}/freezes         freeze an account
POST /loans/{id}/disbursements      disburse a loan
```

Not:
```
POST /reversePayment
POST /payments/{id}/doReversal
```

The sub-resource (`reversals`) is itself a thing that can be listed, fetched, and audited \u2014 which is exactly what a BFSI system wants.

## Common findings

1. Verb-in-path endpoints (`/getX`, `/createY`).
2. Deeply nested URLs that are awkward to route and read.
3. Inconsistent path styles across the API.
4. Sequential integer IDs on customer-data resources, enabling enumeration.
5. RPC-style action endpoints where a sub-resource would be cleaner and auditable.
