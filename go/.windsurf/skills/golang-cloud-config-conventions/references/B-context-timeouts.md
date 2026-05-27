# B. Context, Timeouts & Retries

Expands rules B1–B4.

## B1 + B2 — Per-operation context with timeout [MUST]

```go
func (s *StorageService) UploadDocument(ctx context.Context, key string, r io.Reader) error {
    // Derive an upload-specific timeout from the parent context
    uploadCtx, cancel := context.WithTimeout(ctx, 2*time.Minute)
    defer cancel()

    _, err := s.s3.PutObject(uploadCtx, &s3.PutObjectInput{
        Bucket:           aws.String(s.bucket),
        Key:              aws.String(key),
        Body:             r,
        ChecksumAlgorithm: types.ChecksumAlgorithmSha256, // C3
    })
    return err
}
```

The timeout is derived from the *parent* context so the caller's deadline still applies. If the parent is cancelled (e.g. the HTTP request was abandoned), the upload cancels too.

## B4 — SDK retry policy [SHOULD]

**AWS SDK v2 — built-in retry with configurable mode:**
```go
cfg, _ := config.LoadDefaultConfig(ctx,
    config.WithRegion("ap-south-1"),
    config.WithRetryer(func() aws.Retryer {
        return retry.NewStandard(func(o *retry.StandardOptions) {
            o.MaxAttempts = 5
            o.MaxBackoff   = 30 * time.Second
        })
    }),
)
```

**GCS — retry options per operation:**
```go
obj := client.Bucket(bucket).Object(key).
    Retryer(storage.WithBackoff(gax.Backoff{
        Initial:    500 * time.Millisecond,
        Max:        30 * time.Second,
        Multiplier: 2,
    }))
```

Do not add a manual retry loop around SDK calls — the SDK already retries retriable errors with backoff. A manual loop doubles the retry count and may retry non-idempotent writes.

---

# C. Object Operations

Expands rules C1–C6.

## C1 + C2 — Stream, don't buffer; always close [MUST]

```go
// WRONG — reads entire object into memory
resp, _ := s.s3.GetObject(ctx, &s3.GetObjectInput{Bucket: &s.bucket, Key: &key})
data, _ := io.ReadAll(resp.Body) // OOM risk for large files
resp.Body.Close()

// RIGHT — stream to writer, close body
resp, err := s.s3.GetObject(ctx, &s3.GetObjectInput{Bucket: &s.bucket, Key: &key})
if err != nil { return err }
defer resp.Body.Close()      // always close, even on error paths

if _, err := io.Copy(dst, resp.Body); err != nil {
    return fmt.Errorf("stream object: %w", err)
}
```

An unclosed S3/GCS response body holds the HTTP connection open. The connection is never returned to the pool. Under load this exhausts the pool. This is one of the most common Go SDK bugs in production.

## C3 — Integrity verification [MUST]

**S3 — SHA-256 checksum:**
```go
_, err := s.s3.PutObject(ctx, &s3.PutObjectInput{
    Bucket:            aws.String(s.bucket),
    Key:               aws.String(key),
    Body:              r,
    ChecksumAlgorithm: types.ChecksumAlgorithmSha256,
})
```

S3 computes and verifies the checksum server-side. If the upload is corrupted in transit, `PutObject` returns an error.

**GCS — CRC32C:**
```go
w := client.Bucket(bucket).Object(key).NewWriter(ctx)
w.SendCRC32C = true
w.CRC32C = crc32.Checksum(data, crc32.MakeTable(crc32.Castagnoli))
```

---

# D. Presigned URLs

Expands rules D1–D5.

## D1 + D3 — Short expiry, never logged [MUST]

```go
presignClient := s3.NewPresignClient(s.s3)

// Upload presigned URL — short expiry, specific key
url, err := presignClient.PresignPutObject(ctx, &s3.PutObjectInput{
    Bucket: aws.String(s.bucket),
    Key:    aws.String(key),           // server-generated, not from client input
}, func(o *s3.PresignOptions) {
    o.Expires = 10 * time.Minute       // D1: short expiry
})
if err != nil { return "", err }

// D3: log the key, never the URL
slog.InfoContext(ctx, "presigned upload URL issued",
    slog.String("key", key),
    slog.String("tenant", tenantID),
    // NOT: slog.String("url", url.URL) — never log the URL
)

return url.URL, nil
```

## D4 — Validate ownership before serving [MUST]

```go
func (h *Handler) GetDocumentURL(w http.ResponseWriter, r *http.Request) {
    docID := r.PathValue("id")
    caller := callerFromContext(r.Context())

    // Verify the caller owns the document before issuing a presigned URL
    doc, err := h.repo.FindByID(r.Context(), docID)
    if err != nil || !caller.CanAccessDocument(doc) {
        http.Error(w, "not found", http.StatusNotFound)
        return
    }
    url, _ := h.storage.PresignDownload(r.Context(), doc.StorageKey)
    // return URL to caller
}
```

Without the ownership check, any authenticated caller can request a presigned URL for any object key.

---

# E. Naming & Organisation

Expands rules E1–E4.

## E1 + E2 — Namespaced, server-generated keys [SHOULD/MUST]

```go
func objectKey(tenantID, entityType, objectID, ext string) string {
    now := time.Now().UTC()
    return fmt.Sprintf("%s/%s/%d/%02d/%s%s",
        tenantID, entityType, now.Year(), now.Month(), objectID, ext)
}
// → "acme-tenant/kyc/2025/03/doc_abc123.pdf"
```

Never:
```go
key := r.FormValue("filename") // path traversal: "../../../admin/config.yaml"
key := r.FormValue("key")      // arbitrary key injection
```

## E4 — Key design for lifecycle policies [SHOULD]

```
<tenantID>/kyc/...       → 7-year retention lifecycle rule
<tenantID>/statements/... → 10-year retention lifecycle rule
<tenantID>/temp/...      → 7-day expiry lifecycle rule
```

Lifecycle rules in S3/GCS are prefix-based. If all objects are under one flat namespace, you cannot apply different retention policies to different data classes.

---

# F. BFSI & Compliance Rules

Expands rules F1–F5.

## F1 — Region validation at startup [MUST]

```go
const requiredRegion = "ap-south-1" // AWS Mumbai — RBI data localisation

func validateStorageConfig(cfg StorageConfig) error {
    if cfg.Region != requiredRegion {
        return fmt.Errorf("storage region must be %s (RBI payment-data localisation), got %s",
            requiredRegion, cfg.Region)
    }
    return nil
}
```

Validate this at startup, not at upload time. An accidentally misconfigured region is caught before any data is written.

## F2 + F3 — Private bucket with CMK encryption [MUST]

These are infrastructure controls (Terraform/IaC), but the Go code must confirm them:

```go
// Verify bucket is not public on startup (AWS)
out, err := s.s3.GetBucketPolicyStatus(ctx, &s3.GetBucketPolicyStatusInput{Bucket: &s.bucket})
if err == nil && out.PolicyStatus != nil && out.PolicyStatus.IsPublic {
    return fmt.Errorf("bucket %s is publicly accessible — prohibited for regulated data", s.bucket)
}
```

## F4 — Versioning and deletion protection [MUST for document buckets]

Also an IaC/infrastructure concern, but the service should verify at startup:

```go
ver, err := s.s3.GetBucketVersioning(ctx, &s3.GetBucketVersioningInput{Bucket: &s.bucket})
if err != nil || ver.Status != types.BucketVersioningStatusEnabled {
    return fmt.Errorf("bucket %s must have versioning enabled for document retention", s.bucket)
}
```

## Common findings

1. KYC document bucket created in `us-east-1` — RBI data localisation violation.
2. Bucket ACL set to public-read during initial setup and never reverted.
3. No CMK encryption — objects encrypted with AWS-managed keys that the client cannot audit.
4. Presigned URL logged in application logs — full object access exposed to log consumers.
5. Object key built from `r.FormValue("filename")` — path traversal.
