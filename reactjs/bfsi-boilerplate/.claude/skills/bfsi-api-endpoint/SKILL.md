---
name: bfsi-api-endpoint
description: Adds a typed TanStack Query API endpoint with interface-based request/response typing, audit hooks, and idempotency-key support. Use when the user types /bfsi-api-endpoint, asks to "add an API endpoint", "wire up GET /something", or "create a mutation".
disable-model-invocation: true
argument-hint: <Method> <Path> [--feature FeatureName] [--mutation]
allowed-tools: Read Write Edit Glob Grep
---

# BFSI API Endpoint

Adds a new endpoint to an existing feature module's `api.ts`.

## What it generates

```ts
// In src/features/<Feature>/api.ts
export const use<EndpointName> = createQuery({
  queryKey: [<TAG>],
  queryFn: async (arg) => {
    return http.<method><<EndpointName>Request, <EndpointName>Response>(<URL>, arg);
  },
  meta: { audit: '<auditEventName>' },
});
```

## Workflow

### Step 1: Locate the api.ts

Find the feature's `api.ts` (either via `--feature` flag or by inferring from the current file's path). If the user is editing `src/features/Foo/components/X.tsx`, target `src/features/Foo/api.ts`.

### Step 2: Generate request/response interfaces

If interfaces for the request/response don't exist in `types.ts`, add them. Default shape:

```ts
export interface <EndpointName>Request { /* infer from path params and method */ }
export interface <EndpointName>Response { /* placeholder — user fills in */ }
```

### Step 3: Add the endpoint

Use the template shown above.

### Step 4: Add the URL constant

In `constants.ts`:

```ts
export const <FEATURE>_URLS = {
  // ...existing
  <ENDPOINT_NAME>: '<path>',
} as const;
```

### Step 5: Mutations: idempotency-key

If method is `POST | PUT | PATCH | DELETE`, automatically include the Idempotency-Key header. Tell the user this is automatic.

### Step 6: Audit event

Pick a name following the convention: `<feature>.<entity>.<action>`. For GET endpoints, skip audit (reads are not audited by default; opt in via `--audit-reads`).

### Step 7: Verify

Run `npm run typecheck`. If the interfaces still have placeholder shapes, flag it: "I've added the endpoint but the request/response interfaces are placeholders. Open `types.ts` and define the network shapes."

## Conventions

- **No `any`** — every endpoint must have typed request + response interfaces.
- **Network shapes are interfaces** — responses are typed at compile time, not parsed through Zod.
- **All mutations get Idempotency-Key** — backend de-dupes accidental double-submit.
- **All errors throw typed `ApiError`** — handled by the global error boundary.
- **All caching uses tags**, never hard times. Invalidation is explicit.
