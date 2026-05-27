# F. Pagination, Filtering & Sorting

Expands rules F1\u2013F4.

## F1 \u2014 All collections are paginated [MUST]

A list endpoint with no pagination will, as data grows, eventually return a response large enough to exhaust memory or breach a size limit. Every collection endpoint paginates from day one \u2014 retrofitting it later is a breaking change.

## F2 \u2014 Cursor vs offset [SHOULD]

**Cursor-based** (recommended for large or changing datasets):
```
GET /payments?limit=50
\u2192 { "data": [...], "pagination": { "next_cursor": "eyJpZCI6...", "has_more": true } }

GET /payments?limit=50&cursor=eyJpZCI6...
```
The cursor encodes the position (e.g. the last seen sort key). It is stable under inserts and deletes \u2014 a new row arriving mid-pagination does not shift the window or cause skips/duplicates.

**Offset-based** (acceptable for small, stable datasets):
```
GET /payments?limit=50&offset=100
```
Simple, but on a large or changing dataset it drifts: rows inserted before the offset cause items to be skipped or repeated, and deep offsets are slow for the database.

For financial transaction lists \u2014 large, append-heavy, correctness-sensitive \u2014 prefer cursor-based.

## F3 \u2014 Server-enforced maximum page size [MUST]

```go
const (
    defaultPageSize = 25
    maxPageSize     = 100
)

func pageSize(raw string) int {
    n, err := strconv.Atoi(raw)
    if err != nil || n <= 0 {
        return defaultPageSize
    }
    if n > maxPageSize {
        return maxPageSize // cap, do not honour a larger request
    }
    return n
}
```

A client asking for `limit=100000` gets `maxPageSize`, not 100000. The cap protects the database and the response size regardless of what the client requests.

## F4 \u2014 Allow-listed filter and sort fields [SHOULD]

```go
var sortableFields = map[string]string{
    "created_at": "created_at",
    "amount":     "amount_minor",
    "status":     "status",
}

func resolveSort(raw string) (column string, err error) {
    col, ok := sortableFields[raw]
    if !ok {
        return "", ErrValidation // unknown sort field
    }
    return col, nil
}
```

Filter and sort parameters name *fields*, and those names are validated against a server-side allow-list before being used. The raw client string is never interpolated into a query \u2014 that is SQL injection via the "structural" part of the statement (see `golang-bfsi-bindings` rule C2). The allow-list also decouples the public field name (`created_at`) from the physical column (`created_at`), so the schema can change without breaking the API.

## Common findings

1. Unbounded list endpoint \u2014 no pagination at all.
2. Offset pagination on a large append-heavy table, causing skips/duplicates and slow deep pages.
3. Client-supplied `limit` honoured without a cap.
4. Sort/filter field names interpolated into SQL \u2014 injection.
5. Public sort names hard-wired to physical column names, so a schema change breaks the API.
