# G. BFSI-Specific Caching Rules

Expands rules G1–G4. Detailed patterns are in `references/F-cache-interface.md` (G1–G4 section). This file is the standalone reference for BFSI caching concerns.

## Summary

All four rules are covered with full patterns in `F-cache-interface.md` under the "G. BFSI-Specific Caching Rules" section. They are:

- **G1** — PII in cache values must be masked/tokenised (same rules as persistence).
- **G2** — Cached financial balances carry an `AsOf` timestamp; live balance used for authorisation.
- **G3** — Redis as session store requires TLS + authentication.
- **G4** — Audit records bypass cache entirely; write directly to the append-only store.

See `golang-bfsi-bindings` categories E (data/PII), A (session), and F (logging/audit) for the full regulatory basis of each rule.
